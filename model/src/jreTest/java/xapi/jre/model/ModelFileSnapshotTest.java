package xapi.jre.model;

import org.junit.Test;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelUnavailable;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelText;
import xapi.model.service.ModelServiceWithCreateIfAbsent;
import xapi.model.user.ModelUser;
import xapi.source.lex.CharIterator;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Snapshot data and read conditions must share exact bytes, without hidden IO during decoding. */
public class ModelFileSnapshotTest {

  private static final class Service extends ModelServiceJre {
    Runnable next;
    Runnable onDecode;
    int writes;
    int loads;

    @Override
    protected void dispatchFileOperation(final Runnable operation) {
      next = operation;
    }

    @Override
    protected <M extends Model> void doPersist(final String type, final M model,
        final SuccessHandler<M> callback) {
      writes++;
      super.doPersist(type, model, callback);
    }

    @Override
    public <M extends Model> void load(final Class<M> type, final ModelKey key,
        final SuccessHandler<M> callback) {
      loads++;
      throw new AssertionError("Implicit hydration during a snapshot decode");
    }

    @Override
    protected <M extends Model> M deserialize(final ModelManifest manifest,
        final CharIterator source, final boolean direction, final boolean hydrate) {
      assertFalse("Snapshots cannot hydrate referenced models", hydrate);
      if (onDecode != null) {
        final Runnable hook = onDecode;
        onDecode = null;
        hook.run();
      }
      return super.deserialize(manifest, source, direction, hydrate);
    }
  }

  private static class Result<T> implements SuccessHandler<T>, ErrorHandler<Throwable> {
    T value;
    Throwable error;
    int count;

    @Override
    public void onSuccess(final T result) {
      value = result;
      count++;
    }

    @Override
    public void onError(final Throwable failure) {
      error = failure;
      count++;
    }

    T success() {
      assertEquals("Exactly one terminal callback", 1, count);
      if (error != null) throw new AssertionError(error);
      return value;
    }
  }

  private static ModelText text(final Service service, final String value) {
    final ModelText model = service.create(ModelText.class);
    model.setKey(service.newKey("child", "day", "1").getChild(model.getType(), "brush"));
    model.setText(value);
    return model;
  }

  private static void raw(final FileModelTransactionStore store, final ModelStoreKey key,
      final byte[] image) throws Exception {
    store.legacyWriteScope(key, () -> {
      final Path file = store.directory().resolve(key.relativePath());
      Files.createDirectories(file.getParent());
      Files.write(file, image);
      return null;
    });
  }

  @Test
  public void snapshotCapturesKeyAndDecodesExactlyReadBytesOutsideGate() throws Exception {
    final Service service = new Service();
    try (FileModelTransactionStore store =
        new FileModelTransactionStore(Files.createTempDirectory("file-snapshot"))) {
      service.attachTransactionStore(store);
      final ModelText original = text(service, "OLD");
      final ModelManifest manifest = service.findManifest(ModelText.class);
      final byte[] old = service.encodeTransactionImage(manifest, original);
      final ModelStoreKey key = service.transactionKey(original.getKey());
      raw(store, key, old);
      final byte[] newer = service.encodeTransactionImage(manifest, text(service, "NEW"));

      final Result<ModelFileSnapshot<ModelText>> result = new Result<>();
      service.loadSnapshot(manifest, original.getKey(), result);
      original.getKey().setId("caller-mutated");
      service.onDecode = () -> {
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread concurrentWrite = new Thread(() -> {
          try { raw(store, key, newer); }
          catch (final Throwable error) { failure.set(error); }
        });
        concurrentWrite.start();
        try { concurrentWrite.join(3000); }
        catch (final InterruptedException interrupted) { throw new AssertionError(interrupted); }
        assertFalse("Decode must not hold the store gate", concurrentWrite.isAlive());
        assertNull(failure.get());
      };

      service.next.run();
      final ModelFileSnapshot<ModelText> snapshot = result.success();
      assertEquals("OLD", snapshot.model().getText());
      assertArrayEquals(old, snapshot.record().value());
      assertArrayEquals(newer, store.read(key).value());
      assertEquals(key.canonicalKey(), snapshot.storeKey().canonicalKey());
      assertEquals("brush", snapshot.model().getKey().getId());
    }
  }

  @Test
  public void absenceRetainsConditionAndCallbacksRunOutsideGateOnce() throws Exception {
    final Service service = new Service();
    try (FileModelTransactionStore store =
        new FileModelTransactionStore(Files.createTempDirectory("snapshot-absence"))) {
      service.attachTransactionStore(store);
      final ModelText model = text(service, "unused");
      final ModelManifest manifest = service.findManifest(ModelText.class);
      final Result<ModelFileSnapshot<ModelText>> result = new Result<>();
      service.loadSnapshot(manifest, model.getKey(), result);
      service.next.run();
      assertNull(result.success().model());
      assertFalse(result.value.record().exists());

      final Result<ModelFileSnapshot<ModelText>> throwing = new Result<ModelFileSnapshot<ModelText>>() {
        @Override
        public void onSuccess(final ModelFileSnapshot<ModelText> snapshot) {
          super.onSuccess(snapshot);
          final AtomicReference<Throwable> failure = new AtomicReference<>();
          final Thread worker = new Thread(() -> {
            try { store.read(service.transactionKey(model.getKey())); }
            catch (final Throwable error) { failure.set(error); }
          });
          worker.start();
          try { worker.join(3000); }
          catch (final InterruptedException interrupted) { throw new AssertionError(interrupted); }
          assertFalse("Observer should run outside the store gate", worker.isAlive());
          assertNull(failure.get());
          throw new IllegalStateException("observer");
        }
      };
      service.loadSnapshot(manifest, model.getKey(), throwing);
      try {
        service.next.run();
        fail("Observer exception must escape, without a second callback");
      } catch (final IllegalStateException expected) {
        assertEquals("observer", expected.getMessage());
      }
      assertEquals(1, throwing.count);
      assertNull(throwing.error);
    }
  }

  @Test
  public void codecDoesNotAutosaveOrHydrateAndRejectsCorruptIdentity() throws Exception {
    final Service service = new Service();
    try (FileModelTransactionStore store =
        new FileModelTransactionStore(Files.createTempDirectory("snapshot-codec"))) {
      service.attachTransactionStore(store);
      final ModelContent root = service.create(ModelContent.class);
      root.setKey(service.newKey("child", root.getType(), "root"));
      final ModelUser author = service.create(ModelUser.class);
      author.setKey(service.newKey("child", author.getType(), "author"));
      root.setAuthor(author);
      final ModelContent reference = service.create(ModelContent.class);
      reference.setKey(service.newKey("child", reference.getType(), "ref"));
      root.related().add(reference);

      final ModelManifest manifest = service.findManifest(ModelContent.class);
      final byte[] image = service.encodeTransactionImage(manifest, root);
      final ModelContent decoded = service.decodeSnapshotImage(manifest, root.getKey(), image);
      assertEquals("author", decoded.getAuthor().getKey().getId());
      assertEquals(0, service.loads);
      assertEquals(0, service.writes);
      assertFalse(Files.exists(store.directory().resolve("child/content/ref")));

      try {
        service.decodeSnapshotImage(manifest,
            service.newKey("child", root.getType(), "other"), image);
        fail("A different complete key must not match the captured image");
      } catch (final IOException expected) {
        // Snapshot identities must match both type and exact key.
      }
      try {
        service.decodeSnapshotImage(manifest, root.getKey(), new byte[]{(byte) 0xff});
        fail("Malformed UTF-8 must not become canonical model state");
      } catch (final IOException expected) {
        // Corrupt bytes cannot be decoded as an acknowledged snapshot.
      }
    }
  }

  @Test
  public void attachedClosedStoreHasTypedUnavailableWithoutChangingBorrowIdentity() throws Exception {
    final Service service = new Service();
    final FileModelTransactionStore store =
        new FileModelTransactionStore(Files.createTempDirectory("snapshot-closed"));
    service.attachTransactionStore(store);
    final ModelText model = text(service, "x");
    store.close();
    assertSame(store, service.transactionStore());

    final Result<ModelFileSnapshot<ModelText>> result = new Result<>();
    service.loadSnapshot(service.findManifest(ModelText.class), model.getKey(), result);
    service.next.run();
    assertTrue("Shared API classifies provider unavailability",
        result.error instanceof ModelUnavailable);
    assertTrue("Existing JRE exception subtype remains compatible",
        result.error instanceof FileModelTransactionStore.Unavailable);
    assertEquals(1, result.count);
  }

  @Test
  public void oversizedSnapshotImageFailsWithBoundedIOException() throws Exception {
    final Service service = new Service();
    try (FileModelTransactionStore store =
        new FileModelTransactionStore(Files.createTempDirectory("snapshot-image-limit"))) {
      service.attachTransactionStore(store);
      final ModelText model = text(service, "unused");
      final byte[] oversized = new byte[ModelServiceWithCreateIfAbsent.MAX_MODEL_BYTES + 1];
      try {
        service.decodeSnapshotImage(service.findManifest(ModelText.class), model.getKey(), oversized);
        fail("The image byte bound must be checked before cloning or decoding");
      } catch (final IOException expected) {
        assertTrue(expected.getMessage(), expected.getMessage().contains("byte limit"));
      }
    }
  }
}
