package xapi.jre.model;

import org.junit.Test;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelNotFoundException;
import xapi.model.api.ModelQuery;
import xapi.model.api.ModelQueryResult;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelKeyDefault;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Exercises the real provider, serialization, filesystem and actual deferred runnable bodies. */
public class ModelServiceTransactionGateTest {
  private static final class QueuedService extends ModelServiceJre {
    final List<Runnable> reads = new ArrayList<>();
    int decodes;
    @Override protected void dispatchFileRead(final Runnable operation) { reads.add(operation); }
    @Override public <M extends Model> M deserialize(final Class<M> type, final xapi.source.lex.CharIterator source,
        final xapi.model.impl.ModelSerializationHints hints) {
      decodes++; return super.deserialize(type, source, hints);
    }
    @Override public <M extends Model> M deserialize(final xapi.model.api.ModelManifest manifest,
        final xapi.source.lex.CharIterator source) {
      decodes++; return super.deserialize(manifest, source);
    }
    Runnable next() { return reads.remove(0); }
  }

  private static final class Fixture implements AutoCloseable {
    final Path root;
    final FileModelTransactionStore store;
    final QueuedService service;
    final ModelText a;
    final ModelText b;
    Fixture() throws Exception { this(Files.createTempDirectory("xapi-provider-gate-"), FileModelTransactionStore.Faults.NONE); }
    Fixture(final Path root, final FileModelTransactionStore.Faults faults) throws Exception {
      this.root = root;
      store = new FileModelTransactionStore(root, faults);
      service = new QueuedService();
      service.attachTransactionStore(store);
      a = model(service, "a", "old-a"); b = model(service, "b", "old-b");
    }
    void seed() throws Exception {
      final Result<ModelText> first = new Result<>(), second = new Result<>();
      service.persist(a, first); service.persist(b, second);
      first.success(); second.success();
    }
    ModelTransaction change(final String id, final long revision) {
      return transaction(service, id, revision, model(service, "a", "new-a"), model(service, "b", "new-b"));
    }
    @Override public void close() throws IOException { store.close(); }
  }

  private static ModelText model(final ModelServiceJre service, final String id, final String value) {
    final ModelText model = service.create(ModelText.class);
    model.setKey(new ModelKeyDefault("", "text", id)); model.setText(value); return model;
  }

  private static ModelStoreKey key(final ModelServiceJre service, final ModelText model) {
    return new ModelStoreKey(service.keyToString(model.getKey()), "text/" + model.getKey().getId());
  }

  private static ModelTransaction transaction(final ModelServiceJre service, final String id, final long revision,
      final ModelText a, final ModelText b) {
    return new ModelTransaction("fixture/1", "fixture-manifest", "parent", id, Arrays.asList(
        new ModelMutation(key(service, a), revision, service.serialize(ModelText.class, a).toSource().getBytes(StandardCharsets.UTF_8)),
        new ModelMutation(key(service, b), revision, service.serialize(ModelText.class, b).toSource().getBytes(StandardCharsets.UTF_8))));
  }

  @Test public void ownershipAttachesBeforeSeedAndPinsRootEvenAfterClose() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.service.attachTransactionStore(fixture.store);
      fixture.service.setRootDir(fixture.root.resolve(".").toFile());
      assertTrue(fixture.service.hasTransactionStore());
      fixture.seed();
      expect(IllegalArgumentException.class, () -> fixture.service.setRootDir(Files.createTempDirectory("other-model-root-").toFile()));
      final ModelServiceJre late = new ModelServiceJre();
      final Path original = Files.createTempDirectory("original-provider-root-");
      late.setRootDir(original.toFile());
      late.getRoot(new Result<Model>());
      expect(IllegalStateException.class, () -> late.attachTransactionStore(fixture.store));
      assertEquals(original.toFile(), late.getRoot(new Result<Model>()));
      assertFalse(late.hasTransactionStore());
      fixture.store.close();
      expect(IllegalArgumentException.class, () -> fixture.service.setRootDir(Files.createTempDirectory("other-model-root-").toFile()));
      final Result<ModelText> load = new Result<>();
      fixture.service.load(ModelText.class, fixture.a.getKey(), load); fixture.service.next().run();
      assertTrue(load.failure() instanceof IOException);
      final Result<ModelText> save = new Result<>(); fixture.service.persist(fixture.a, save);
      assertTrue(save.failure() instanceof IOException);
      assertTrue(fixture.service.hasTransactionStore());
    }
  }

  @Test public void deferredLoadCapturesIdentityAndReadsCommittedStateAtExecution() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.seed();
      final ModelKey request = new ModelKeyDefault("", "text", "a");
      final Result<ModelText> load = new Result<>(); fixture.service.load(ModelText.class, request, load);
      request.setId("b");
      fixture.store.commit(fixture.change("op", 0));
      fixture.service.next().run();
      assertEquals("new-a", load.success().getText()); assertEquals("a", load.value.getKey().getId());
    }
  }

  @Test public void allThreeQueryFormsIgnoreInternalFilesAndSnapshotMutableQueryInput() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.seed(); fixture.store.commit(fixture.change("op", 0));
      final Path bucket = fixture.root.resolve("text");
      Files.write(bucket.resolve(".xapi-write-abandoned.tmp"), new byte[]{1});
      final ModelQuery<ModelText> query = new ModelQuery<>();
      final Result<ModelQueryResult<ModelText>> typed = new Result<>(), manifest = new Result<>();
      final Result<ModelQueryResult<Model>> untyped = new Result<>();
      fixture.service.query(ModelText.class, query, typed);
      query.setNamespace("unrelated");
      fixture.service.query(fixture.service.findManifest(ModelText.class), new ModelQuery<ModelText>(), manifest);
      fixture.service.query(new ModelQuery<Model>(), untyped);
      while (!fixture.service.reads.isEmpty()) fixture.service.next().run();
      assertEquals(2, typed.success().getSize()); assertEquals(2, manifest.success().getSize()); assertEquals(2, untyped.success().getSize());
      final Result<ModelQueryResult<ModelText>> reserved = new Result<>();
      fixture.service.query(ModelText.class, new ModelQuery<ModelText>().setNamespace(FileModelTransactionStore.INTERNAL_DIRECTORY), reserved);
      fixture.service.next().run(); assertNotNull(reserved.failure());
    }
  }

  @Test public void protectedRecordsRejectLegacyPersistAndDeleteIncludingAfterRestart() throws Exception {
    final Path root;
    try (Fixture fixture = new Fixture()) {
      root = fixture.root; fixture.seed(); fixture.store.commit(fixture.change("op", 0));
      final Result<ModelText> save = new Result<>(); fixture.service.persist(fixture.a, save);
      assertTrue(save.failure() instanceof FileModelTransactionStore.Conflict);
      final Result<Boolean> remove = new Result<>(); fixture.service.delete(fixture.b.getKey(), remove);
      assertTrue(remove.failure() instanceof FileModelTransactionStore.Conflict);
    }
    try (Fixture fixture = new Fixture(root, FileModelTransactionStore.Faults.NONE)) {
      final Result<ModelText> save = new Result<>(); fixture.service.persist(fixture.a, save);
      assertTrue(save.failure() instanceof FileModelTransactionStore.Conflict);
      final Result<Boolean> remove = new Result<>(); fixture.service.delete(fixture.b.getKey(), remove);
      assertTrue(remove.failure() instanceof FileModelTransactionStore.Conflict);
      assertEquals(1, fixture.store.read(key(fixture.service, fixture.a)).revision());
    }
  }

  @Test public void missingProtectedRecordIsStorageFailureAndCannotBeReseeded() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.seed(); fixture.store.commit(fixture.change("op", 0));
      Files.delete(fixture.root.resolve("text/a"));
      final Result<ModelText> load = new Result<>(); fixture.service.load(ModelText.class, fixture.a.getKey(), load);
      fixture.service.next().run();
      assertTrue(load.failure() instanceof IOException); assertFalse(load.error instanceof ModelNotFoundException);
      final Result<ModelText> save = new Result<>(); fixture.service.persist(fixture.a, save);
      assertTrue(save.failure() instanceof IOException); assertFalse(Files.exists(fixture.root.resolve("text/a")));
    }
  }

  @Test public void validationAndEveryActualReadWaitForWholeMaterialization() throws Exception {
    final CountDownLatch half = new CountDownLatch(1), release = new CountDownLatch(1), validated = new CountDownLatch(1);
    final AtomicReference<Throwable> failure = new AtomicReference<>();
    try (Fixture fixture = new Fixture(Files.createTempDirectory("xapi-provider-gate-"), (at, child) -> {
      if (at == FileModelTransactionStore.Boundary.RECORD_MATERIALIZED && child == 0) {
        half.countDown(); await(release);
      }
    })) {
      fixture.seed();
      final Thread writer = thread(failure, () -> fixture.store.commit(fixture.change("op", 0))); writer.start();
      assertTrue(half.await(5, TimeUnit.SECONDS));
      final Result<ModelText> load = new Result<>(); fixture.service.load(ModelText.class, fixture.b.getKey(), load);
      final Result<ModelQueryResult<ModelText>> typed = new Result<>(), manifest = new Result<>();
      final Result<ModelQueryResult<Model>> untyped = new Result<>();
      fixture.service.query(ModelText.class, new ModelQuery<ModelText>(), typed);
      fixture.service.query(fixture.service.findManifest(ModelText.class), new ModelQuery<ModelText>(), captured -> validated.countDown(), manifest);
      fixture.service.query(new ModelQuery<Model>(), untyped);
      final List<Thread> readers = new ArrayList<>();
      while (!fixture.service.reads.isEmpty()) { final Runnable read = fixture.service.next(); final Thread reader = thread(failure, read::run); readers.add(reader); reader.start(); }
      assertFalse(validated.await(50, TimeUnit.MILLISECONDS)); assertFalse(load.done.await(20, TimeUnit.MILLISECONDS));
      release.countDown(); writer.join(5000); for (Thread reader : readers) reader.join(5000);
      if (failure.get() != null) throw new AssertionError(failure.get());
      assertEquals("new-b", load.success().getText()); assertEquals(0, validated.getCount());
      for (final ModelQueryResult<?> result : Arrays.asList(typed.success(), manifest.success(), untyped.success())) {
        assertEquals(2, result.getSize());
        for (Model model : result.getModelList()) assertTrue(((ModelText) model).getText().startsWith("new-"));
      }
    } finally { release.countDown(); }
  }

  @Test public void legacyMutationsWaitForTransactionThenRejectInsteadOfOverwriting() throws Exception {
    final CountDownLatch half = new CountDownLatch(1), release = new CountDownLatch(1);
    final AtomicReference<Throwable> failure = new AtomicReference<>();
    try (Fixture fixture = new Fixture(Files.createTempDirectory("xapi-provider-gate-"), (at, child) -> {
      if (at == FileModelTransactionStore.Boundary.RECORD_MATERIALIZED && child == 0) { half.countDown(); await(release); }
    })) {
      fixture.seed();
      final Thread writer = thread(failure, () -> fixture.store.commit(fixture.change("op", 0))); writer.start();
      assertTrue(half.await(5, TimeUnit.SECONDS));
      final Result<ModelText> save = new Result<>(); final Result<Boolean> remove = new Result<>();
      final Thread saver = thread(failure, () -> fixture.service.persist(fixture.a, save));
      final Thread deleter = thread(failure, () -> fixture.service.delete(fixture.b.getKey(), remove));
      saver.start(); deleter.start(); assertFalse(save.done.await(30, TimeUnit.MILLISECONDS)); assertFalse(remove.done.await(30, TimeUnit.MILLISECONDS));
      release.countDown(); writer.join(5000); saver.join(5000); deleter.join(5000);
      if (failure.get() != null) throw new AssertionError(failure.get());
      assertTrue(save.failure() instanceof FileModelTransactionStore.Conflict);
      assertTrue(remove.failure() instanceof FileModelTransactionStore.Conflict);
      assertTrue(new String(fixture.store.read(key(fixture.service, fixture.a)).value(), StandardCharsets.UTF_8).contains("new-a"));
    } finally { release.countDown(); }
  }

  @Test public void validatedQueryScopeAlsoBlocksAnUnprotectedLegacyWriteUntilSnapshotIsBuffered() throws Exception {
    final CountDownLatch validating = new CountDownLatch(1), release = new CountDownLatch(1);
    final AtomicReference<Throwable> failure = new AtomicReference<>();
    try (Fixture fixture = new Fixture()) {
      fixture.seed();
      final Result<ModelQueryResult<ModelText>> query = new Result<>();
      fixture.service.query(fixture.service.findManifest(ModelText.class), new ModelQuery<ModelText>(), captured -> {
        validating.countDown(); try { await(release); } catch (IOException error) { throw new IllegalStateException(error); }
      }, query);
      final Thread reader = thread(failure, fixture.service.next()::run); reader.start();
      assertTrue(validating.await(5, TimeUnit.SECONDS));
      final Result<ModelText> save = new Result<>();
      final Thread writer = thread(failure, () -> fixture.service.persist(model(fixture.service, "a", "later"), save)); writer.start();
      assertFalse(save.done.await(50, TimeUnit.MILLISECONDS)); release.countDown();
      reader.join(5000); writer.join(5000);
      if (failure.get() != null) throw new AssertionError(failure.get());
      save.success();
      for (ModelText value : query.success().getModelList()) assertTrue(value.getText().startsWith("old-"));
      final Result<ModelText> load = new Result<>(); fixture.service.load(ModelText.class, fixture.a.getKey(), load); fixture.service.next().run();
      assertEquals("later", load.success().getText());
    } finally { release.countDown(); }
  }

  @Test public void validatorFailureCallbackIsOutsideScopeAndSymlinksDoNotBecomeEmptyResults() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.seed(); final Result<ModelQueryResult<ModelText>> rejected = new Result<>();
      class Callback implements SuccessHandler<ModelQueryResult<ModelText>>, ErrorHandler<Throwable> {
        @Override public void onSuccess(final ModelQueryResult<ModelText> ignored) { rejected.onError(new AssertionError("unexpected success")); }
        @Override public void onError(final Throwable error) {
          try { fixture.store.commit(fixture.change("error-callback", 0)); rejected.onError(error); }
          catch (Throwable failure) { rejected.onError(new AssertionError(failure)); }
        }
      }
      fixture.service.query(fixture.service.findManifest(ModelText.class), new ModelQuery<ModelText>(),
          captured -> { throw new IllegalArgumentException("validator"); }, new Callback());
      fixture.service.next().run();
      assertTrue(rejected.failure() instanceof xapi.model.api.ModelQueryFailureException);
      assertEquals(1, fixture.store.receipt("parent", "error-callback").revision());
      final Path outside = Files.createTempFile("xapi-provider-outside-", ".txt");
      Files.write(outside, "outside remains".getBytes(StandardCharsets.UTF_8));
      Files.createSymbolicLink(fixture.root.resolve("text/linked"), outside);
      final Result<ModelQueryResult<ModelText>> linked = new Result<>();
      fixture.service.query(ModelText.class, new ModelQuery<ModelText>(), linked); fixture.service.next().run();
      assertTrue(linked.failure() instanceof xapi.model.api.ModelQueryFailureException);
      assertEquals("outside remains", new String(Files.readAllBytes(outside), StandardCharsets.UTF_8));
    }
  }

  @Test public void readValidationRejectsReentrantMutationsButConsumerCallbackCanCommit() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.seed();
      final Result<ModelQueryResult<ModelText>> result = new Result<>();
      fixture.service.query(fixture.service.findManifest(ModelText.class), new ModelQuery<ModelText>(), captured -> {
        try {
          expect(IOException.class, () -> fixture.store.commit(fixture.change("bad", 0)));
          expect(IOException.class, () -> fixture.store.legacyWriteScope(key(fixture.service, fixture.a), () -> null));
          expect(IOException.class, fixture.store::recover); expect(IOException.class, fixture.store::close);
        } catch (Exception failure) { throw new AssertionError(failure); }
      }, new SuccessHandler<ModelQueryResult<ModelText>>() {
        @Override public void onSuccess(final ModelQueryResult<ModelText> value) {
          try { assertEquals(1, fixture.store.commit(fixture.change("good", 0)).revision()); result.onSuccess(value); }
          catch (Throwable error) { result.onError(error); }
        }
      });
      fixture.service.next().run(); assertEquals(2, result.success().getSize());
    }
  }

  @Test public void callbacksAreOutsideWriteAndReadScopesAndConsumerFailureIsNotStorageFailure() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.seed(); final int[] errors = {0};
      class Throwing<T> implements SuccessHandler<T>, ErrorHandler<Throwable> {
        @Override public void onSuccess(final T value) { throw new IllegalStateException("consumer"); }
        @Override public void onError(final Throwable error) { errors[0]++; }
      }
      expect(IllegalStateException.class, () -> fixture.service.persist(fixture.a, new Throwing<ModelText>()));
      fixture.service.load(ModelText.class, fixture.a.getKey(), new Throwing<ModelText>());
      expect(IllegalStateException.class, fixture.service.next()::run);
      fixture.service.query(ModelText.class, new ModelQuery<ModelText>(), new Throwing<ModelQueryResult<ModelText>>());
      expect(IllegalStateException.class, fixture.service.next()::run);
      assertEquals(0, errors[0]);
      final Result<ModelText> save = new Result<>();
      fixture.service.persist(fixture.a, new SuccessHandler<ModelText>() {
        @Override public void onSuccess(final ModelText value) {
          try { assertEquals(1, fixture.store.commit(fixture.change("callback", 0)).revision()); save.onSuccess(value); }
          catch (Throwable error) { save.onError(error); }
        }
      });
      save.success();
    }
  }

  @Test public void startupRecoveryCompletesCrashBeforeAttachedProviderServesOrSeeds() throws Exception {
    final Path root;
    try (Fixture fixture = new Fixture()) { root = fixture.root; fixture.seed(); }
    final Path log = root.resolveSibling(root.getFileName() + ".crash.log");
    final Process child = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-cp", System.getProperty("java.class.path"),
        CrashWriter.class.getName(), root.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
    if (!child.waitFor(15, TimeUnit.SECONDS)) { child.destroyForcibly(); fail("Crash fixture timeout"); }
    assertEquals("Crash fixture failed: " + log, 73, child.exitValue());
    try (Fixture fixture = new Fixture(root, FileModelTransactionStore.Faults.NONE)) {
      final Result<ModelText> load = new Result<>(); fixture.service.load(ModelText.class, fixture.a.getKey(), load); fixture.service.next().run();
      assertEquals("new-a", load.success().getText());
      final Result<ModelText> reseed = new Result<>(); fixture.service.persist(fixture.a, reseed);
      assertTrue(reseed.failure() instanceof FileModelTransactionStore.Conflict);
      assertEquals(1, fixture.store.receipt("parent", "crash").revision());
    }
  }

  @Test public void oversizedAttachedLegacyWriteRejectsBeforeReplacingBytesAndReleasesGeneratedId() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.seed();
      final byte[] original = Files.readAllBytes(fixture.root.resolve("text/a"));
      final char[] large = new char[FileModelTransactionStore.MAX_REQUEST_BYTES + 1]; Arrays.fill(large, 'x');
      fixture.a.setText(new String(large));
      final Result<ModelText> save = new Result<>(); fixture.service.persist(fixture.a, save);
      assertTrue(save.failure() instanceof IOException);
      assertFalse(save.error instanceof FileModelTransactionStore.RecoveryRequired);
      assertArrayEquals(original, Files.readAllBytes(fixture.root.resolve("text/a")));
      final ModelText generated = model(fixture.service, null, new String(large));
      final Result<ModelText> fresh = new Result<>(); fixture.service.persist(generated, fresh);
      assertTrue(fresh.failure() instanceof IOException); assertNull(generated.getKey().getId());
      try (java.nio.file.DirectoryStream<Path> files = Files.newDirectoryStream(fixture.root.resolve("text"))) {
        int count = 0; for (Path ignored : files) count++; assertEquals(2, count);
      }
    }
  }

  @Test public void oversizedQueryRecordFailsBeforeDecoderAndDoesNotBecomeAnEmptyOrMissingResult() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final Path file = fixture.root.resolve("text/oversized"); Files.createDirectories(file.getParent());
      try (java.io.RandomAccessFile sparse = new java.io.RandomAccessFile(file.toFile(), "rw")) {
        sparse.setLength(FileModelTransactionStore.MAX_REQUEST_BYTES + 1L);
      }
      final Result<ModelQueryResult<ModelText>> query = new Result<>();
      fixture.service.query(ModelText.class, new ModelQuery<ModelText>(), query); fixture.service.next().run();
      assertTrue(query.failure() instanceof xapi.model.api.ModelQueryFailureException);
      assertFalse(query.error instanceof ModelNotFoundException); assertEquals(0, fixture.service.decodes);
      assertEquals(FileModelTransactionStore.MAX_REQUEST_BYTES + 1L, Files.size(file));
    }
  }

  @Test public void originalParentedIdentityIsValidatedBeforeCaptureWithoutNormalizingItsOwner() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelKey parent = new ModelKeyDefault("ownerB", "day", "7").setKeyType(ModelKey.KEY_TYPE_LONG);
      final ModelKey valid = new ModelKeyDefault("ownerB", "text", "item").setKeyType(-17).setParent(parent);
      final ModelText record = model(fixture.service, "item", "B-record"); record.setKey(valid);
      final Result<ModelText> saved = new Result<>(); fixture.service.persist(record, saved); saved.success();
      for (final String wrongOwner : Arrays.asList("ownerA", "")) {
        final ModelKey malformed = new ModelKeyDefault(wrongOwner, "text", "item").setKeyType(-17).setParent(parent);
        final Result<ModelText> denied = new Result<>(); fixture.service.load(ModelText.class, malformed, denied);
        assertTrue(denied.failure() instanceof IOException); assertTrue(fixture.service.reads.isEmpty()); assertEquals(0, fixture.service.decodes);
      }
      final ModelKey cycle = new ModelKeyDefault("ownerB", "text", "cycle"); cycle.setParent(cycle);
      final Result<ModelText> cyclic = new Result<>(); fixture.service.load(ModelText.class, cycle, cyclic);
      assertTrue(cyclic.failure() instanceof IOException); assertTrue(fixture.service.reads.isEmpty());
      final Result<ModelText> loaded = new Result<>(); fixture.service.load(ModelText.class, valid, loaded);
      parent.setId("changed-after-dispatch"); fixture.service.next().run();
      assertEquals("B-record", loaded.success().getText()); assertEquals("ownerB", loaded.value.getKey().getNamespace());
      assertEquals(-17, loaded.value.getKey().getKeyType()); assertEquals("7", loaded.value.getKey().getParent().getId());
      assertEquals(ModelKey.KEY_TYPE_LONG, loaded.value.getKey().getParent().getKeyType());
    }
  }

  @Test public void validatorAndEnumerationUseTheSameCapturedNamespaceAncestorAndCursor() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelKey parentA = new ModelKeyDefault("ownerA", "day", "7").setKeyType(ModelKey.KEY_TYPE_LONG);
      final ModelKey parentB = new ModelKeyDefault("ownerB", "day", "8").setKeyType(ModelKey.KEY_TYPE_LONG);
      final ModelText a = model(fixture.service, "second", "A-record");
      a.setKey(new ModelKeyDefault("ownerA", "text", "second").setParent(parentA));
      final Result<ModelText> saved = new Result<>(); fixture.service.persist(a, saved); saved.success();
      final ModelQuery<ModelText> original = new ModelQuery<ModelText>().setNamespace("ownerA").setAncestor(parentA).setCursor("second");
      final Result<ModelQueryResult<ModelText>> result = new Result<>();
      fixture.service.query(fixture.service.findManifest(ModelText.class), original, captured -> {
        assertNotSame(original, captured); assertEquals("ownerA", captured.getNamespace());
        assertEquals("7", captured.getAncestor().getId()); assertEquals("second", captured.getCursor());
      }, result);
      original.setNamespace("ownerB").setAncestor(parentB).setCursor("unrelated"); fixture.service.next().run();
      assertEquals(1, result.success().getSize()); assertEquals("A-record", result.value.getModelList().iterator().next().getText());
      assertTrue("manifest decoder instrumentation must observe the successful control", fixture.service.decodes > 0);
      fixture.service.decodes = 0;
      original.setNamespace("ownerA").setAncestor(parentA).setCursor("second");
      final Result<ModelQueryResult<ModelText>> denied = new Result<>();
      fixture.service.query(fixture.service.findManifest(ModelText.class), original, captured -> {
        if ("ownerA".equals(captured.getNamespace())) throw new IllegalArgumentException("captured scope denied");
      }, denied);
      original.setNamespace("ownerB").setAncestor(parentB).setCursor("unrelated"); fixture.service.next().run();
      assertTrue(denied.failure() instanceof xapi.model.api.ModelQueryFailureException); assertEquals(0, fixture.service.decodes);
      final Result<ModelQueryResult<ModelText>> changed = new Result<>();
      fixture.service.query(fixture.service.findManifest(ModelText.class), new ModelQuery<ModelText>(),
          captured -> captured.setNamespace("retargeted"), changed);
      fixture.service.next().run(); assertTrue(changed.failure() instanceof xapi.model.api.ModelQueryFailureException);
      assertEquals(0, fixture.service.decodes);
    }
  }

  @Test public void inconsistentOriginalQueryAncestorRejectsBeforeValidationDispatchOrDecode() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelKey parent = new ModelKeyDefault("ownerB", "day", "7");
      final ModelKey inconsistent = new ModelKeyDefault("ownerA", "text", "item").setParent(parent);
      final ModelQuery<ModelText> query = new ModelQuery<ModelText>().setNamespace("ownerA").setAncestor(inconsistent);
      final int[] validated = {0}; final Result<ModelQueryResult<ModelText>> denied = new Result<>();
      fixture.service.query(fixture.service.findManifest(ModelText.class), query, captured -> validated[0]++, denied);
      assertTrue(denied.failure() instanceof IOException); assertEquals(0, validated[0]);
      assertTrue(fixture.service.reads.isEmpty()); assertEquals(0, fixture.service.decodes);
      final Result<ModelQueryResult<ModelText>> mismatched = new Result<>(); query.setAncestor(parent);
      fixture.service.query(fixture.service.findManifest(ModelText.class), query, captured -> validated[0]++, mismatched);
      assertTrue(mismatched.failure() instanceof IOException); assertEquals(0, validated[0]); assertTrue(fixture.service.reads.isEmpty());
    }
  }

  public static final class CrashWriter {
    public static void main(final String[] args) throws Exception {
      try (Fixture fixture = new Fixture(java.nio.file.Paths.get(args[0]), (at, child) -> {
        if (at == FileModelTransactionStore.Boundary.RECORD_MATERIALIZED && child == 0) Runtime.getRuntime().halt(73);
      })) { fixture.store.commit(fixture.change("crash", 0)); }
      throw new AssertionError("Crash boundary not reached");
    }
  }

  private static void await(final CountDownLatch latch) throws IOException {
    try { if (!latch.await(5, TimeUnit.SECONDS)) throw new IOException("Barrier timeout"); }
    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException(interrupted); }
  }
  private interface Action { void run() throws Exception; }
  private static Thread thread(final AtomicReference<Throwable> failure, final Action action) {
    return new Thread(() -> { try { action.run(); } catch (Throwable error) { failure.compareAndSet(null, error); } });
  }
  private static <T extends Throwable> T expect(final Class<T> type, final Action action) throws Exception {
    try { action.run(); } catch (Throwable failure) { if (type.isInstance(failure)) return type.cast(failure); throw new AssertionError(failure); }
    throw new AssertionError("Expected " + type.getSimpleName());
  }
  private static final class Result<T> implements SuccessHandler<T>, ErrorHandler<Throwable> {
    final CountDownLatch done = new CountDownLatch(1); T value; Throwable error;
    @Override public void onSuccess(final T value) { this.value = value; done.countDown(); }
    @Override public void onError(final Throwable error) { this.error = error; done.countDown(); }
    T success() throws Exception { assertTrue(done.await(5, TimeUnit.SECONDS)); if (error != null) throw new AssertionError(error); return value; }
    Throwable failure() throws Exception { assertTrue(done.await(5, TimeUnit.SECONDS)); assertNotNull(error); return error; }
  }
}
