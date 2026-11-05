package xapi.jre.model;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Real isolated files and subprocess interruption; no household store or remote server. */
public class FileModelTransactionStoreTest {
  private static final ModelStoreKey A = new ModelStoreKey("owner:lv:day/1:a:long", "owner/lv/day/1/a");
  private static final ModelStoreKey B = new ModelStoreKey("owner:lv:day/1:b:long", "owner/lv/day/1/b");

  private static byte[] bytes(final String value) { return value.getBytes(StandardCharsets.UTF_8); }
  private static String value(final ModelStoreRecord record) { return new String(record.value(), StandardCharsets.UTF_8); }

  private static ModelTransaction transaction(final String id, final long revision, final String a, final String b) {
    return new ModelTransaction("model-txn/1", "fixture-manifest", "fixture-parent", id, Arrays.asList(
        new ModelMutation(A, revision, bytes(a)), new ModelMutation(B, revision, bytes(b))));
  }

  private static Path legacy() throws IOException {
    final Path root = Files.createTempDirectory("xapi-txn-test-");
    Files.createDirectories(root.resolve(A.relativePath()).getParent());
    Files.write(root.resolve(A.relativePath()), bytes("old-a"));
    Files.write(root.resolve(B.relativePath()), bytes("old-b"));
    return root;
  }

  @Test public void commitsTwoConditionalRecordsAndReturnsOriginalReceiptAfterRestart() throws Exception {
    final Path root = legacy();
    final ModelTransaction request = transaction("op", 0, "new-a", "new-b");
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      assertEquals(0, store.read(A).revision());
      final ModelTransactionReceipt receipt = store.commit(request);
      assertEquals(1, receipt.revision());
      assertEquals(receipt.fingerprint(), store.commit(request).fingerprint());
      assertTrue(store.isProtected(A));
      final Map<String, ModelStoreRecord> snapshot = store.read(Arrays.asList(A, B));
      assertEquals("new-a", value(snapshot.get(A.canonicalKey())));
      assertEquals("new-b", value(snapshot.get(B.canonicalKey())));
      assertEquals(1, snapshot.get(B.canonicalKey()).revision());
    }
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      assertEquals(1, store.commit(request).revision());
      assertEquals(1, store.receipt("fixture-parent", "op").revision());
      assertEquals(2, store.commit(transaction("next", 1, "a2", "b2")).revision());
      assertEquals(1, store.commit(request).revision());
      assertEquals("a2", value(store.read(A)));
    }
  }

  @Test public void rejectsNativePathBudgetsBeforePublishingUnderMissingBuckets() throws Exception {
    final Path root = legacy();
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      final String[] invalidPaths = {
          "absent/" + repeated("x", 256),
          "absent/" + repeated("é", 128),
          "absent/" + repeated("界", 86) + "/nested/record",
          // Every component fits; the total native pathname exceeds Linux PATH_MAX.
          repeated(repeated("é", 20) + "/", 100) + "record",
          // The final path fits, but not a full legal sibling temporary basename.
          repeated(repeated("x", 50) + "/", 76) + "record"
      };
      for (int i = 0; i < invalidPaths.length; i++) {
        final String operation = "path-budget-" + i;
        final ModelStoreKey invalid = new ModelStoreKey("invalid-" + i, invalidPaths[i]);
        final ModelTransaction request = new ModelTransaction("p", "m", "actor", operation, Arrays.asList(
            new ModelMutation(A, 0, bytes("must-not-change")),
            new ModelMutation(invalid, ModelMutation.MUST_NOT_EXIST, bytes("new"))));
        final IOException failure = expect(IOException.class, () -> store.commit(request));
        assertFalse(failure instanceof FileModelTransactionStore.RecoveryRequired);
        assertNull(store.receipt("actor", operation));
        assertFalse(Files.exists(root.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("pending")));
        assertEquals("old-a", value(store.read(A)));
        assertEquals(0, store.read(A).revision());
      }
      // Rejection must leave the engine usable, not merely without visible child files.
      assertEquals(1, store.commit(transaction("after-path-rejections", 0, "a", "b")).revision());
    }
  }

  @Test public void acceptsMaximumNativeComponentsAndChecksAllAtomicDestinations() throws Exception {
    final Path root = Files.createTempDirectory("xapi-path-budget-");
    final ModelStoreKey ascii = new ModelStoreKey("ascii", "a/" + repeated("x", 255));
    final ModelStoreKey unicode = new ModelStoreKey("unicode", "b/" + repeated("界", 85));
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      store.commit(new ModelTransaction("p", "m", "actor", "limits", Arrays.asList(
          new ModelMutation(ascii, ModelMutation.MUST_NOT_EXIST, bytes("ascii")),
          new ModelMutation(unicode, ModelMutation.MUST_NOT_EXIST, bytes("unicode")))));
      assertEquals("ascii", value(store.read(ascii)));
      assertEquals("unicode", value(store.read(unicode)));
    }
    // Validate an absent deep directory without creating it. Fixed-size journal/receipt
    // and hash filenames also need their own parent-plus-temporary budget.
    final Path deep = root.resolve(repeated(repeated("x", 50) + "/", 75));
    final Path[] generated = {
        deep.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("pending"),
        deep.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("sequence"),
        deep.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("receipts").resolve(repeated("a", 64)),
        deep.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("revisions").resolve(repeated("b", 64)),
        deep.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("identities").resolve(repeated("c", 64))
    };
    for (final Path target : generated) expect(IOException.class, () -> ModelFilePathBudget.validateWrite(target));
  }

  private static String repeated(final String value, final int count) {
    final StringBuilder result = new StringBuilder(value.length() * count);
    for (int i = 0; i < count; i++) result.append(value);
    return result.toString();
  }

  @Test public void rejectsOneStaleChildWithoutChangingEitherRecord() throws Exception {
    final Path root = legacy();
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      final ModelTransaction request = new ModelTransaction("p", "m", "actor", "op", Arrays.asList(
          new ModelMutation(A, 0, bytes("changed")), new ModelMutation(B, 9, bytes("changed"))));
      expect(FileModelTransactionStore.Conflict.class, () -> store.commit(request));
      assertEquals("old-a", value(store.read(A)));
      assertEquals("old-b", value(store.read(B)));
      assertNull(store.receipt("actor", "op"));
      assertFalse(Files.exists(root.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("pending")));
    }
  }

  @Test public void distinguishesMissingFromRevisionZeroAndRejectsChangedOperationMeaning() throws Exception {
    final Path root = Files.createTempDirectory("xapi-txn-test-");
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      assertFalse(store.read(A).exists());
      expect(FileModelTransactionStore.Conflict.class, () -> store.commit(transaction("wrong", 0, "a", "b")));
      store.commit(transaction("create", -1, "a", "b"));
      expect(FileModelTransactionStore.Conflict.class, () -> store.commit(transaction("other", -1, "a", "b")));
      expect(FileModelTransactionStore.Conflict.class, () -> store.commit(transaction("create", -1, "different", "b")));
      assertEquals("a", value(store.read(A)));
    }
  }

  @Test public void snapshotsInputBytesAndRetainsMonotonicRevisionAcrossABA() throws Exception {
    final Path root = legacy();
    final byte[] mutable = bytes("a");
    final ModelMutation mutation = new ModelMutation(A, 0, mutable);
    mutable[0] = 'z';
    mutation.value()[0] = 'z';
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      store.commit(new ModelTransaction("p", "m", "actor", "op", Collections.singletonList(mutation)));
      assertEquals("a", value(store.read(A)));
      store.commit(new ModelTransaction("p", "m", "actor", "op2", Collections.singletonList(new ModelMutation(A, 1, bytes("b")))));
      store.commit(new ModelTransaction("p", "m", "actor", "op3", Collections.singletonList(new ModelMutation(A, 2, bytes("a")))));
      expect(FileModelTransactionStore.Conflict.class, () -> store.commit(new ModelTransaction("p", "m", "actor", "stale",
          Collections.singletonList(new ModelMutation(A, 1, bytes("c"))))));
      assertEquals(3, store.read(A).revision());
    }
  }

  @Test public void rejectsDuplicatesAliasesOverlapsSymlinksAndOversizedPayloadBeforeCommit() throws Exception {
    final Path root = legacy();
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      final List<ModelMutation> duplicate = Arrays.asList(new ModelMutation(A, 0, bytes("a")), new ModelMutation(A, 0, bytes("b")));
      expect(IOException.class, () -> store.commit(new ModelTransaction("p", "m", "actor", "dupe", duplicate)));
      final ModelStoreKey child = new ModelStoreKey("child", A.relativePath() + "/child");
      expect(IOException.class, () -> store.commit(new ModelTransaction("p", "m", "actor", "overlap", Arrays.asList(
          new ModelMutation(A, 0, bytes("a")), new ModelMutation(child, -1, bytes("c"))))));
      expect(IOException.class, () -> store.commit(new ModelTransaction("p", "m", "actor", "blocked-parent",
          Collections.singletonList(new ModelMutation(child, -1, bytes("c"))))));
      expect(IOException.class, () -> store.commit(new ModelTransaction("p", "m", "actor", "large",
          Collections.singletonList(new ModelMutation(A, 0, new byte[FileModelTransactionStore.MAX_REQUEST_BYTES])))));
      final Path external = Files.createTempFile("xapi-txn-outside-", ".txt");
      Files.createSymbolicLink(root.resolve("linked"), external);
      expect(IOException.class, () -> store.read(new ModelStoreKey("link", "linked")));
      store.commit(transaction("bind", 0, "a", "b"));
      expect(IOException.class, () -> store.read(new ModelStoreKey("different-key-type", A.relativePath())));
      expect(IOException.class, () -> store.read(new ModelStoreKey(A.canonicalKey(), "different/location")));
      assertEquals("a", value(store.read(A)));
    }
    expect(IllegalArgumentException.class, () -> new ModelStoreKey("key", "../outside"));
    expect(IllegalArgumentException.class, () -> new ModelStoreKey("key", FileModelTransactionStore.INTERNAL_DIRECTORY + "/sequence"));
    expect(IllegalArgumentException.class, () -> new ModelStoreKey("key", "owner/.xapi-write-model.tmp"));
    expect(IllegalArgumentException.class, () -> new ModelStoreKey("\uD800", "valid"));
  }

  @Test public void excludesSecondWriterAndRetainsCorruptionAsFailure() throws Exception {
    final Path invalidLock = Files.createTempDirectory("xapi-txn-test-");
    Files.createDirectories(invalidLock.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("owner.lock"));
    expect(IOException.class, () -> new FileModelTransactionStore(invalidLock));
    final Path root = legacy();
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      expect(IOException.class, () -> new FileModelTransactionStore(root));
      store.commit(transaction("op", 0, "a", "b"));
    }
    Files.write(root.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("sequence"), bytes("broken"));
    expect(FileModelTransactionStore.RecoveryRequired.class, () -> new FileModelTransactionStore(root));
    assertEquals("broken", new String(Files.readAllBytes(root.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("sequence")), StandardCharsets.UTF_8));
  }

  @Test public void injectedBoundaryFailuresAreRejectedBeforePublicationOrRequireRecoveryAfterward() throws Exception {
    for (final FileModelTransactionStore.Boundary boundary : FileModelTransactionStore.Boundary.values()) {
      final Path root = legacy();
      final boolean[] fired = {false};
      try (FileModelTransactionStore store = new FileModelTransactionStore(root, (at, child) -> {
        if (!fired[0] && at == boundary) { fired[0] = true; throw new IOException("injected " + at); }
      })) {
        final ModelTransaction request = transaction("op", 0, "new-a", "new-b");
        final IOException failure = expect(IOException.class, () -> store.commit(request));
        if (boundary == FileModelTransactionStore.Boundary.PREPARED) {
          assertFalse(failure instanceof FileModelTransactionStore.RecoveryRequired);
          assertEquals("old-a", value(store.read(A)));
          assertNull(store.receipt("fixture-parent", "op"));
        } else {
          assertTrue(failure instanceof FileModelTransactionStore.RecoveryRequired);
          expect(FileModelTransactionStore.RecoveryRequired.class, () -> store.read(A));
          store.recover();
          assertEquals("new-a", value(store.read(A)));
          assertEquals("new-b", value(store.read(B)));
          assertEquals(1, store.commit(request).revision());
          assertEquals(2, store.commit(transaction("next", 1, "a2", "b2")).revision());
        }
      }
    }
  }

  @Test public void gatePreventsHalfMaterializationFromBeingRead() throws Exception {
    final Path root = legacy();
    final CountDownLatch half = new CountDownLatch(1), release = new CountDownLatch(1), readDone = new CountDownLatch(1);
    final AtomicReference<Throwable> failure = new AtomicReference<>();
    try (FileModelTransactionStore store = new FileModelTransactionStore(root, (at, child) -> {
      if (at == FileModelTransactionStore.Boundary.RECORD_MATERIALIZED && child == 0) {
        half.countDown();
        try { if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("barrier timeout"); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException(interrupted); }
      }
    })) {
      final Thread writer = new Thread(() -> { try { store.commit(transaction("op", 0, "new-a", "new-b")); } catch (Throwable error) { failure.set(error); } });
      writer.start();
      assertTrue(half.await(5, TimeUnit.SECONDS));
      final Thread reader = new Thread(() -> {
        try {
          final Map<String, ModelStoreRecord> snapshot = store.read(Arrays.asList(A, B));
          assertEquals("new-a", value(snapshot.get(A.canonicalKey())));
          assertEquals("new-b", value(snapshot.get(B.canonicalKey())));
        } catch (Throwable error) { failure.set(error); }
        finally { readDone.countDown(); }
      });
      reader.start();
      assertFalse(readDone.await(50, TimeUnit.MILLISECONDS));
      release.countDown();
      writer.join(5000); reader.join(5000);
      assertFalse(writer.isAlive()); assertFalse(reader.isAlive());
      if (failure.get() != null) throw new AssertionError(failure.get());
    } finally { release.countDown(); }
  }

  @Test public void corruptCommittedJournalCannotBeMistakenForAnAbsentOperation() throws Exception {
    final Path root = legacy();
    try (FileModelTransactionStore store = new FileModelTransactionStore(root, (at, child) -> {
      if (at == FileModelTransactionStore.Boundary.COMMITTED) throw new IOException("stop after commit");
    })) {
      expect(FileModelTransactionStore.RecoveryRequired.class, () -> store.commit(transaction("op", 0, "a", "b")));
    }
    final Path pending = root.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("pending");
    final byte[] corrupt = Files.readAllBytes(pending);
    corrupt[11] ^= 1; // A revision-bit change must fail the full journal checksum, not become another commit.
    Files.write(pending, corrupt);
    expect(FileModelTransactionStore.RecoveryRequired.class, () -> new FileModelTransactionStore(root));
    assertArrayEquals(corrupt, Files.readAllBytes(pending));
    assertEquals("old-a", new String(Files.readAllBytes(root.resolve(A.relativePath())), StandardCharsets.UTF_8));
  }

  @Test public void missingSequenceAfterCommitCannotReinitializeRevisionForAnotherKey() throws Exception {
    final Path root = legacy();
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      store.commit(transaction("op", 0, "a", "b"));
    }
    final Path sequence = root.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY).resolve("sequence");
    Files.delete(sequence);
    expect(FileModelTransactionStore.RecoveryRequired.class, () -> new FileModelTransactionStore(root));
    assertFalse(Files.exists(sequence));
    assertEquals("a", new String(Files.readAllBytes(root.resolve(A.relativePath())), StandardCharsets.UTF_8));
  }

  @Test public void unexpectedErrorAfterCommitStillClosesTheReadGate() throws Exception {
    final Path root = legacy();
    final boolean[] fired = {false};
    try (FileModelTransactionStore store = new FileModelTransactionStore(root, (at, child) -> {
      if (!fired[0] && at == FileModelTransactionStore.Boundary.RECORD_MATERIALIZED) {
        fired[0] = true;
        throw new AssertionError("unexpected application failure");
      }
    })) {
      expect(AssertionError.class, () -> store.commit(transaction("op", 0, "a", "b")));
      expect(FileModelTransactionStore.RecoveryRequired.class, () -> store.read(A));
      store.recover();
      assertEquals("a", value(store.read(A)));
      assertEquals("b", value(store.read(B)));
    }
  }

  @Test public void secondProcessCannotAcquireOwnershipAndExistingPermissionsRemain() throws Exception {
    final Path root = legacy();
    final java.util.Set<java.nio.file.attribute.PosixFilePermission> permissions =
        java.nio.file.attribute.PosixFilePermissions.fromString("rw-r-----");
    Files.setPosixFilePermissions(root.resolve(A.relativePath()), permissions);
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      expect(IOException.class, () -> new FileModelTransactionStore(root));
      final Path log = root.resolveSibling(root.getFileName() + ".lock.log");
      final Process child = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-cp",
          System.getProperty("java.class.path"), LockProbe.class.getName(), root.toString())
          .redirectErrorStream(true).redirectOutput(log.toFile()).start();
      if (!child.waitFor(15, TimeUnit.SECONDS)) { child.destroyForcibly(); fail("Lock probe timeout"); }
      assertEquals("Lock probe failed: " + log, 0, child.exitValue());
      store.commit(transaction("op", 0, "a", "b"));
      assertEquals(permissions, Files.getPosixFilePermissions(root.resolve(A.relativePath())));
    }
  }

  public static final class LockProbe {
    public static void main(final String[] args) throws Exception {
      try (FileModelTransactionStore unexpected = new FileModelTransactionStore(java.nio.file.Paths.get(args[0]))) {
        throw new AssertionError("Second process acquired the store");
      } catch (IOException expected) {
        if (!expected.getMessage().contains("already has a writer")) throw expected;
      }
    }
  }

  @Test public void subprocessHaltAtEveryBoundaryRecoversOneCompleteOutcome() throws Exception {
    for (final FileModelTransactionStore.Boundary boundary : FileModelTransactionStore.Boundary.values()) {
      final int children = boundary == FileModelTransactionStore.Boundary.RECORD_MATERIALIZED
          || boundary == FileModelTransactionStore.Boundary.METADATA_MATERIALIZED
          || boundary == FileModelTransactionStore.Boundary.IDENTITY_MATERIALIZED ? 2 : 1;
      for (int childIndex = 0; childIndex < children; childIndex++) {
      final Path root = legacy();
      final Path log = root.resolveSibling(root.getFileName() + ".process.log");
      final Process child = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-cp",
          System.getProperty("java.class.path"), CrashWriter.class.getName(), root.toString(), boundary.name(), Integer.toString(childIndex))
          .redirectErrorStream(true).redirectOutput(log.toFile()).start();
      if (!child.waitFor(15, TimeUnit.SECONDS)) { child.destroyForcibly(); fail("Crash fixture timeout"); }
      assertEquals("Crash fixture failed: " + log, 73, child.exitValue());
      try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
        if (boundary == FileModelTransactionStore.Boundary.PREPARED) {
          assertEquals("old-a", value(store.read(A))); assertEquals("old-b", value(store.read(B)));
          assertNull(store.receipt("fixture-parent", "op"));
        } else {
          assertEquals("new-a", value(store.read(A))); assertEquals("new-b", value(store.read(B)));
          assertEquals(1, store.commit(transaction("op", 0, "new-a", "new-b")).revision());
          assertEquals(2, store.commit(transaction("next", 1, "a2", "b2")).revision());
        }
      }
      try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
        if (boundary != FileModelTransactionStore.Boundary.PREPARED) assertEquals("a2", value(store.read(A)));
      }
      }
    }
  }

  public static final class CrashWriter {
    public static void main(final String[] args) throws Exception {
      final FileModelTransactionStore.Boundary boundary = FileModelTransactionStore.Boundary.valueOf(args[1]);
      final int childIndex = Integer.parseInt(args[2]);
      try (FileModelTransactionStore store = new FileModelTransactionStore(java.nio.file.Paths.get(args[0]), (at, child) -> {
        if (at == boundary && (child < 0 || child == childIndex)) Runtime.getRuntime().halt(73);
      })) { store.commit(transaction("op", 0, "new-a", "new-b")); }
      throw new AssertionError("Boundary was not exercised");
    }
  }

  private interface Action { void run() throws Exception; }
  private static <T extends Throwable> T expect(final Class<T> type, final Action action) throws Exception {
    try { action.run(); }
    catch (Throwable failure) { if (type.isInstance(failure)) return type.cast(failure); throw new AssertionError(failure); }
    throw new AssertionError("Expected " + type.getSimpleName());
  }
}
