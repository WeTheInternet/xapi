package xapi.jre.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Opt-in Linux file transaction engine. It is not a ModelService provider: adapters must route
 * every participating read, query and write through the same gate before advertising atomicity.
 * A published redo decision rolls forward; no later commit starts until durable cleanup succeeds.
 * Receipts have no automatic expiry. This engine does not authenticate callers or decode models.
 */
public final class FileModelTransactionStore implements AutoCloseable {
  public static final String INTERNAL_DIRECTORY = ".xapi-transactions";
  // Technical pilot bounds, not domain/reward policy or a claim about an HTTP server's limits.
  public static final int MAX_MUTATIONS = 64;
  public static final int MAX_REQUEST_BYTES = 4 * 1024 * 1024;
  private static final Set<Path> OWNED_ROOTS = new HashSet<>();

  public enum Boundary {
    PREPARED, PUBLISHED, COMMITTED, RECORD_MATERIALIZED, METADATA_MATERIALIZED,
    IDENTITY_MATERIALIZED, SEQUENCE_MATERIALIZED, RECEIPT_MATERIALIZED, BEFORE_CLEANUP, CLEANUP_UNLINKED
  }

  /** Fault injection observes durable protocol boundaries; production uses NONE. */
  public interface Faults {
    void at(Boundary boundary, int child) throws IOException;
    Faults NONE = (boundary, child) -> {};
  }

  /** Synchronous file work only: never await an asynchronous callback while holding this scope. */
  public interface IOOperation<T> {
    T run() throws IOException;
  }

  public static final class Conflict extends IOException {
    public Conflict(final String message) { super(message); }
  }

  /** The commit decision may exist. Retry/recover this same operation; do not invent a new ID. */
  public static final class RecoveryRequired extends IOException {
    public RecoveryRequired(final Throwable cause) { super("Transaction outcome requires recovery", cause); }
  }

  private final Path root;
  private final Path internal;
  private final Path revisions;
  private final Path identities;
  private final Path receipts;
  private final Path pending;
  private final Path sequenceFile;
  private final Faults faults;
  private final AtomicModelFile atomicFiles = new AtomicModelFile();
  private FileChannel ownerChannel;
  private FileLock ownerLock;
  private boolean recoveryRequired;
  private boolean closed;
  private boolean rootReserved;
  private long sequence;
  private int operationDepth;

  public FileModelTransactionStore(final Path root) throws IOException {
    this(root, Faults.NONE);
  }

  /** Immutable canonical directory identity; the owner, not an attached provider, closes the store. */
  public Path directory() { return root; }

  /** Validates and reads under the same gate; callbacks belong outside this synchronous scope. */
  public synchronized <T> T readScope(final IOOperation<T> operation) throws IOException {
    ready();
    operationDepth++;
    try { return operation.run(); }
    finally { operationDepth--; }
  }

  /** Rejects legacy writes to protected records and performs the actual mutation under one gate. */
  public synchronized <T> T legacyWriteScope(final ModelStoreKey key, final IOOperation<T> operation) throws IOException {
    ready();
    mutationAllowed();
    if (isProtected(key)) throw new Conflict("Protected model requires a conditional transaction");
    operationDepth++;
    try { return operation.run(); }
    finally { operationDepth--; }
  }

  /**
   * Reconcile a query's entire logical bucket with protected bindings before pagination.
   * A flat namespace query includes direct records under its immediate type buckets only.
   * This intentionally scans all binding metadata; no persistent query index is claimed.
   */
  synchronized void validateQueryInventory(final Path directory, final boolean flatNamespace) throws IOException {
    ready();
    final Path scope = directory.toAbsolutePath();
    if (!scope.equals(scope.normalize()) || !scope.startsWith(root) || scope.startsWith(internal)) {
      throw new IOException("Invalid query inventory scope");
    }
    try (java.nio.file.DirectoryStream<Path> entries = Files.newDirectoryStream(revisions)) {
      for (final Path entry : entries) {
        if (AtomicModelFile.isTemporary(entry.toFile())) continue;
        final DataInputStream fields = new DataInputStream(new ByteArrayInputStream(unchecked(readBounded(entry))));
        final ModelStoreKey key;
        try { key = new ModelStoreKey(ModelTransactionCodec.text(fields, 32768), ModelTransactionCodec.text(fields, 16384)); }
        catch (IllegalArgumentException invalid) { throw new IOException("Corrupt protected model inventory", invalid); }
        final long revision = fields.readLong();
        if (revision <= 0 || revision > sequence || fields.available() != 0 || !metadata(key).equals(entry)) {
          throw new IOException("Corrupt protected model inventory");
        }
        final Path file = root.resolve(key.relativePath());
        final Path bucket = file.getParent();
        if (!(flatNamespace ? bucket != null && scope.equals(bucket.getParent()) : scope.equals(bucket))) continue;
        if (revision(key) != revision || !exists(destination(key))) {
          throw new IOException("Protected query model is missing or inconsistent");
        }
      }
    }
  }

  public FileModelTransactionStore(final Path directory, final Faults faults) throws IOException {
    root = directory.toAbsolutePath().normalize();
    this.faults = java.util.Objects.requireNonNull(faults, "faults");
    internal = root.resolve(INTERNAL_DIRECTORY);
    revisions = internal.resolve("revisions");
    identities = internal.resolve("identities");
    receipts = internal.resolve("receipts");
    pending = internal.resolve("pending");
    sequenceFile = internal.resolve("sequence");
    try {
      ensureDirectory(root);
      ensureDirectory(internal);
      final Path lockFile = internal.resolve("owner.lock");
      rejectLink(lockFile);
      if (exists(lockFile) && !Files.isRegularFile(lockFile, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("Model ownership lock is not a regular file");
      }
      synchronized (OWNED_ROOTS) {
        if (!OWNED_ROOTS.add(root)) throw new IOException("Model store already has a writer");
        rootReserved = true;
      }
      // On Linux closing *another* descriptor for this inode can release process-scoped
      // fcntl locks. Reject same-JVM ownership before opening a second lock descriptor.
      ownerChannel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
      try {
        ownerLock = ownerChannel.tryLock();
      } catch (OverlappingFileLockException owned) {
        throw new IOException("Model store already has a writer", owned);
      }
      if (ownerLock == null) throw new IOException("Model store already has a writer");
      ensureDirectory(revisions);
      ensureDirectory(identities);
      ensureDirectory(receipts);
      recover();
    } catch (IOException | RuntimeException failure) {
      try { close(); } catch (IOException cleanup) { failure.addSuppressed(cleanup); }
      throw failure;
    }
  }

  /** All returned records are from one gate-held snapshot; arbitrary separate calls are not. */
  public synchronized Map<String, ModelStoreRecord> read(final List<ModelStoreKey> keys) throws IOException {
    ready();
    if (keys.size() > MAX_MUTATIONS) throw new IOException("Read snapshot too large");
    final Map<String, ModelStoreRecord> result = new LinkedHashMap<>();
    for (final ModelStoreKey key : keys) {
      if (result.containsKey(key.canonicalKey())) throw new IOException("Duplicate snapshot key");
      result.put(key.canonicalKey(), record(key));
    }
    return Collections.unmodifiableMap(result);
  }

  public synchronized ModelStoreRecord read(final ModelStoreKey key) throws IOException {
    ready();
    return record(key);
  }

  /** Legacy adapters must reject unconditional writes/deletes to these records. */
  public synchronized boolean isProtected(final ModelStoreKey key) throws IOException {
    ready();
    record(key); // Validate binding, not merely whether the hash-named metadata exists.
    return exists(metadata(key));
  }

  /** Receipt lookup must be authorized by an adapter before disclosing actor-scoped results. */
  public synchronized ModelTransactionReceipt receipt(final String actorScope, final String operationId) throws IOException {
    ready();
    final Path path = receiptPath(actorScope, operationId);
    if (!exists(path)) return null;
    final ModelTransactionReceipt result = ModelTransactionCodec.receipt(readBounded(path));
    if (!actorScope.equals(result.transaction().actorScope()) || !operationId.equals(result.transaction().operationId())) {
      throw new IOException("Receipt identity mismatch");
    }
    return result;
  }

  public synchronized ModelTransactionReceipt commit(final ModelTransaction transaction) throws IOException {
    ready();
    mutationAllowed();
    final byte[] request = ModelTransactionCodec.request(transaction);
    final String fingerprint = ModelTransactionCodec.hash(request);
    final ModelTransactionReceipt previous = receipt(transaction.actorScope(), transaction.operationId());
    if (previous != null) {
      if (!fingerprint.equals(previous.fingerprint())) throw new Conflict("Operation ID already has a different payload");
      return previous;
    }
    validate(transaction);
    if (sequence == Long.MAX_VALUE) throw new IOException("Store revision exhausted");
    final byte[] journal = ModelTransactionCodec.receipt(transaction, sequence + 1);
    final boolean[] published = {false};
    boolean completed = false;
    try {
      new AtomicModelFile() {
        @Override void beforeReplace(final Path temporary, final Path target) throws IOException {
          faults.at(Boundary.PREPARED, -1);
        }
        @Override void atomicReplace(final Path temporary, final Path target) throws IOException {
          super.atomicReplace(temporary, target);
          published[0] = true;
          faults.at(Boundary.PUBLISHED, -1);
        }
      }.replace(pending, journal);
      faults.at(Boundary.COMMITTED, -1);
      final ModelTransactionReceipt result = ModelTransactionCodec.receipt(journal);
      materialize(result, journal);
      completed = true;
      return result;
    } catch (IOException | RuntimeException failure) {
      if (published[0] || failure instanceof ModelPersistenceException) {
        recoveryRequired = true;
        throw new RecoveryRequired(failure);
      }
      throw failure;
    } finally {
      // Even an Error must not leave the instance serving a partially materialized decision.
      if (published[0] && !completed) recoveryRequired = true;
    }
  }

  /** Startup and explicit retry recovery finish an existing decision before reads are enabled. */
  public synchronized void recover() throws IOException {
    owned();
    mutationAllowed();
    recoveryRequired = true;
    try {
      sequence = readSequence();
      if (exists(pending)) {
        final byte[] journal = readBounded(pending);
        final ModelTransactionReceipt committed = ModelTransactionCodec.receipt(journal);
        // Sequence may already have been materialized before the process stopped.
        if (committed.revision() != sequence
            && (sequence == Long.MAX_VALUE || committed.revision() != sequence + 1)) {
          throw new IOException("Invalid pending transaction order");
        }
        validatePaths(committed.transaction());
        materialize(committed, journal);
      }
      recoveryRequired = false;
    } catch (IOException | RuntimeException failure) {
      throw new RecoveryRequired(failure);
    }
  }

  private void materialize(final ModelTransactionReceipt receipt, final byte[] journal) throws IOException {
    int index = 0;
    for (final ModelMutation mutation : receipt.transaction().mutations()) {
      atomicWrite(destination(mutation.key()), mutation.value());
      faults.at(Boundary.RECORD_MATERIALIZED, index);
      atomicWrite(metadata(mutation.key()), metadataBytes(mutation.key(), receipt.revision()));
      faults.at(Boundary.METADATA_MATERIALIZED, index);
      atomicWrite(identity(mutation.key()), metadataBytes(mutation.key(), receipt.revision()));
      faults.at(Boundary.IDENTITY_MATERIALIZED, index++);
    }
    atomicWrite(sequenceFile, checked(ByteBuffer.allocate(8).putLong(receipt.revision()).array()));
    faults.at(Boundary.SEQUENCE_MATERIALIZED, -1);
    final Path receiptFile = receiptPath(receipt.transaction().actorScope(), receipt.transaction().operationId());
    if (exists(receiptFile) && !Arrays.equals(journal, readBounded(receiptFile))) {
      throw new IOException("Conflicting committed receipt");
    }
    atomicWrite(receiptFile, journal);
    faults.at(Boundary.RECEIPT_MATERIALIZED, -1);
    faults.at(Boundary.BEFORE_CLEANUP, -1);
    Files.delete(pending);
    faults.at(Boundary.CLEANUP_UNLINKED, -1);
    forceDirectory(internal);
    sequence = receipt.revision();
  }

  private void validate(final ModelTransaction transaction) throws IOException {
    validatePaths(transaction);
    for (final ModelMutation mutation : transaction.mutations()) {
      final ModelStoreRecord previous = record(mutation.key());
      if (mutation.expectedRevision() == ModelMutation.MUST_NOT_EXIST) {
        if (previous.exists()) throw new Conflict("Model already exists");
      } else if (!previous.exists() || previous.revision() != mutation.expectedRevision()) {
        throw new Conflict("Model revision changed");
      }
    }
  }

  private void validatePaths(final ModelTransaction transaction) throws IOException {
    final Set<String> paths = new HashSet<>();
    final Set<String> keys = new HashSet<>();
    for (final ModelMutation mutation : transaction.mutations()) {
      destination(mutation.key());
      if (!paths.add(mutation.key().relativePath()) || !keys.add(mutation.key().canonicalKey())) {
        throw new IOException("Duplicate transaction destination or identity");
      }
    }
    for (final String left : paths) {
      for (final String right : paths) {
        if (right.startsWith(left + "/")) throw new IOException("Overlapping transaction destinations");
      }
    }
  }

  private ModelStoreRecord record(final ModelStoreKey key) throws IOException {
    final Path destination = destination(key);
    final long revision = revision(key);
    if (!exists(destination)) {
      if (revision != 0) throw new IOException("Protected model file is missing");
      return new ModelStoreRecord(null, 0);
    }
    return new ModelStoreRecord(readBounded(destination), revision);
  }

  private long revision(final ModelStoreKey key) throws IOException {
    final Path metadata = metadata(key);
    long revision = 0;
    if (exists(metadata)) {
      final DataInputStream in = new DataInputStream(new ByteArrayInputStream(unchecked(readBounded(metadata))));
      final String boundKey = ModelTransactionCodec.text(in, 32768);
      final String boundPath = ModelTransactionCodec.text(in, 16384);
      revision = in.readLong();
      if (!key.canonicalKey().equals(boundKey) || !key.relativePath().equals(boundPath)
          || revision <= 0 || revision > sequence || in.available() != 0) throw new IOException("Model revision metadata mismatch");
    }
    final Path identity = identity(key);
    if (revision == 0 ? exists(identity) : !exists(identity)
        || !Arrays.equals(metadataBytes(key, revision), readBounded(identity))) {
      throw new IOException("Canonical model identity binding mismatch");
    }
    return revision;
  }

  private byte[] metadataBytes(final ModelStoreKey key, final long revision) throws IOException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    final DataOutputStream out = new DataOutputStream(bytes);
    ModelTransactionCodec.text(out, key.canonicalKey());
    ModelTransactionCodec.text(out, key.relativePath());
    out.writeLong(revision);
    out.close();
    return checked(bytes.toByteArray());
  }

  private Path destination(final ModelStoreKey key) throws IOException {
    final Path path = root.resolve(key.relativePath()).normalize();
    if (!path.startsWith(root) || path.startsWith(internal)) throw new IOException("Model path escapes store");
    rejectLinkChain(path);
    for (Path parent = path.getParent(); parent != null && parent.startsWith(root); parent = parent.getParent()) {
      if (exists(parent) && !Files.isDirectory(parent, LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("Model parent is not a directory");
      }
    }
    if (exists(path) && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
      throw new IOException("Model destination is not a regular file");
    }
    return path;
  }

  private Path metadata(final ModelStoreKey key) {
    return revisions.resolve(ModelTransactionCodec.hash(key.relativePath().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  private Path identity(final ModelStoreKey key) {
    return identities.resolve(ModelTransactionCodec.hash(key.canonicalKey().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  private Path receiptPath(final String actor, final String operation) {
    ModelStoreKey.text(actor, 1024, "actor scope");
    ModelStoreKey.text(operation, 256, "operation ID");
    return receipts.resolve(ModelTransactionCodec.hash((actor + '\0' + operation).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  private long readSequence() throws IOException {
    if (!exists(sequenceFile)) {
      if (exists(pending) || hasEntries(revisions) || hasEntries(identities) || hasEntries(receipts)) {
        throw new IOException("Store sequence is missing from an initialized transaction store");
      }
      // Establish revision zero durably before the first redo publication. A missing sequence
      // after any later interruption is therefore corruption, never permission to start over.
      atomicWrite(sequenceFile, checked(ByteBuffer.allocate(8).putLong(0).array()));
    }
    final byte[] bytes = unchecked(readBounded(sequenceFile));
    if (bytes.length != 8) throw new IOException("Corrupt store sequence");
    final long revision = ByteBuffer.wrap(bytes).getLong();
    if (revision < 0) throw new IOException("Invalid store sequence");
    return revision;
  }

  private boolean hasEntries(final Path directory) throws IOException {
    try (java.nio.file.DirectoryStream<Path> entries = Files.newDirectoryStream(directory)) {
      for (final Path entry : entries) {
        if (!AtomicModelFile.isTemporary(entry.toFile())) return true;
      }
    }
    return false;
  }

  private void ready() throws IOException {
    owned();
    if (exists(pending)) recoveryRequired = true;
    if (recoveryRequired) throw new RecoveryRequired(null);
  }

  private void mutationAllowed() throws IOException {
    if (operationDepth != 0) throw new IOException("Cannot mutate or close the store from a synchronous read/write scope");
  }

  private void owned() throws IOException {
    if (closed || ownerLock == null || !ownerLock.isValid()) throw new IOException("Model store is closed or unowned");
  }

  private boolean exists(final Path path) throws IOException {
    rejectLinkChain(path);
    // Reading attributes distinguishes absence from permission and other IO failures.
    try { Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS); return true; }
    catch (java.nio.file.NoSuchFileException absent) { return false; }
  }

  private byte[] readBounded(final Path path) throws IOException {
    rejectLinkChain(path);
    if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Not a regular store file");
    final long size = Files.size(path);
    if (size > MAX_REQUEST_BYTES + 256) throw new IOException("Store file exceeds supported bound");
    return Files.readAllBytes(path);
  }

  private byte[] checked(final byte[] bytes) {
    final byte[] checksum = ModelTransactionCodec.hash(bytes).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    final byte[] result = Arrays.copyOf(bytes, bytes.length + checksum.length);
    System.arraycopy(checksum, 0, result, bytes.length, checksum.length);
    return result;
  }

  private byte[] unchecked(final byte[] bytes) throws IOException {
    if (bytes.length < 64) throw new IOException("Corrupt store metadata");
    final byte[] content = Arrays.copyOf(bytes, bytes.length - 64);
    if (!Arrays.equals(bytes, checked(content))) throw new IOException("Corrupt store metadata checksum");
    return content;
  }

  private void atomicWrite(final Path path, final byte[] bytes) throws IOException {
    ensureDirectory(path.getParent());
    rejectLinkChain(path);
    atomicFiles.replace(path, bytes);
  }

  private void ensureDirectory(final Path path) throws IOException {
    if (path == null) return;
    rejectLink(path);
    if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
      if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Store parent is not a directory");
      rejectLinkChain(path);
      return;
    }
    ensureDirectory(path.getParent());
    Files.createDirectory(path);
    forceDirectory(path);
    forceDirectory(path.getParent());
  }

  private void rejectLinkChain(final Path path) throws IOException {
    Path current = path.toAbsolutePath();
    while (current != null) { rejectLink(current); current = current.getParent(); }
  }

  private void rejectLink(final Path path) throws IOException {
    if (Files.isSymbolicLink(path)) throw new IOException("Store paths must not use symbolic links");
  }

  private void forceDirectory(final Path directory) throws IOException {
    atomicFiles.forceDirectory(directory);
  }

  @Override
  public synchronized void close() throws IOException {
    if (closed) return;
    mutationAllowed();
    closed = true;
    try { if (ownerLock != null) ownerLock.release(); }
    finally {
      try { if (ownerChannel != null) ownerChannel.close(); }
      finally {
        if (rootReserved) {
          synchronized (OWNED_ROOTS) { OWNED_ROOTS.remove(root); }
          rootReserved = false;
        }
      }
    }
  }
}
