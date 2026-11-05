package xapi.jre.model;

import org.junit.Test;
import xapi.model.api.*;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelKeyDefault;
import xapi.model.impl.ModelUtil;
import xapi.model.service.ModelServiceWithCreateIfAbsent;
import xapi.model.test.api.ModelTestDirections;
import xapi.model.test.api.ModelTestDirectionsEmbedded;
import xapi.source.lex.CharIterator;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Real owned file-provider conditional-create races, without household state or a graph transaction. */
public class ModelCreateIfAbsentTest {
  private static class Service extends ModelServiceJre {
    final List<Runnable> operations = new ArrayList<>();
    int ordinaryWrites, decoded;
    boolean rejectAcknowledgment;
    Runnable acknowledgmentDecode;
    @Override protected void dispatchFileOperation(final Runnable operation) { operations.add(operation); }
    @Override protected <M extends Model> void doPersist(final String type, final M model, final SuccessHandler<M> callback) {
      ordinaryWrites++; super.doPersist(type, model, callback);
    }
    @Override protected <M extends Model> M deserialize(final ModelManifest manifest, final CharIterator source,
        final boolean direction, final boolean hydrate) {
      decoded++; final M value = super.deserialize(manifest, source, direction, hydrate);
      if (decoded == 2 && acknowledgmentDecode != null) acknowledgmentDecode.run();
      if (rejectAcknowledgment && decoded == 2) throw new IllegalStateException("injected acknowledgment decode failure");
      return value;
    }
    void drain() { while (!operations.isEmpty()) operations.remove(0).run(); }
  }
  private static final class Fixture implements AutoCloseable {
    final Path root = Files.createTempDirectory("xapi-create-");
    final FileModelTransactionStore store = new FileModelTransactionStore(root);
    final Service service;
    Fixture() throws Exception { this(new Service()); }
    Fixture(final Service service) throws Exception { this.service = service; service.attachTransactionStore(store); service.create(ModelText.class); }
    @Override public void close() throws IOException { store.close(); }
  }
  private static ModelText progress(final Service service, final String state) {
    final ModelText model = service.create(ModelText.class);
    final ModelKey day = new ModelKeyDefault("child-a", "day", "1").setKeyType(ModelKey.KEY_TYPE_LONG);
    model.setKey(new ModelKeyDefault("child-a", "text", "brush-teeth").setKeyType(73).setParent(day));
    model.setText(state); model.setTime("FINISHED".equals(state) ? 200 : 100); return model;
  }
  private static ModelStoreKey key(final Service service, final Model model) {
    return new ModelStoreKey(service.keyToString(model.getKey()), "child-a/text/day/1/brush-teeth");
  }
  private static void save(final Service service, final ModelText model) {
    final Result<ModelText> saved = new Result<>(); service.persist(model, saved); saved.success();
  }
  private static final class Result<T> implements SuccessHandler<T>, ErrorHandler<Throwable> {
    T value; Throwable error; int calls; final CountDownLatch done = new CountDownLatch(1);
    @Override public void onSuccess(final T value) { this.value = value; calls++; done.countDown(); }
    @Override public void onError(final Throwable error) { this.error = error; calls++; done.countDown(); }
    T success() { assertEquals(1, calls); if (error != null) throw new AssertionError(error); return value; }
    Throwable failure() { assertEquals(1, calls); assertNotNull(error); return error; }
  }

  @Test public void staleStartReturnsFinishedProgressWithoutOverwriteAndCreationStaysUnprotected() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelText active = progress(fixture.service, "ACTIVE"); final Result<ModelCreateResult<ModelText>> first = new Result<>();
      fixture.service.createIfAbsent(active, first); assertEquals(0, first.calls); fixture.service.drain();
      assertTrue(first.success().isCreated()); assertNotSame(active, first.value.getModel()); assertFalse(fixture.store.isProtected(key(fixture.service, active)));
      save(fixture.service, progress(fixture.service, "FINISHED"));
      final byte[] finished = Files.readAllBytes(fixture.root.resolve("child-a/text/day/1/brush-teeth"));
      final Result<ModelCreateResult<ModelText>> stale = new Result<>(); fixture.service.createIfAbsent(active, stale); fixture.service.drain();
      assertFalse(stale.success().isCreated()); assertEquals("FINISHED", stale.value.getModel().getText()); assertEquals(200, stale.value.getModel().getTime(), 0);
      assertArrayEquals(finished, Files.readAllBytes(fixture.root.resolve("child-a/text/day/1/brush-teeth")));
      assertEquals(1, fixture.service.ordinaryWrites); // Only the explicit completion, never a create fallback.
    }
  }

  @Test public void twoRacingCreatesHaveOneWinnerAndOneDetachedExistingAcknowledgment() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final Result<ModelCreateResult<ModelText>> a = new Result<>(), b = new Result<>();
      fixture.service.createIfAbsent(progress(fixture.service, "ACTIVE-a"), a); fixture.service.createIfAbsent(progress(fixture.service, "ACTIVE-b"), b);
      final Runnable first = fixture.service.operations.remove(0), second = fixture.service.operations.remove(0);
      final Thread one = new Thread(first), two = new Thread(second); one.start(); two.start(); one.join(5000); two.join(5000);
      assertFalse(one.isAlive()); assertFalse(two.isAlive());
      assertTrue(a.success().isCreated() ^ b.success().isCreated()); assertEquals(a.value.getModel().getText(), b.value.getModel().getText());
      assertNotSame(a.value.getModel(), b.value.getModel()); assertEquals(0, fixture.service.ordinaryWrites);
    }
  }

  @Test public void capturesImageFullHierarchicalIdentityPolicyAndCallerMutationBeforeDispatch() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelText model = progress(fixture.service, "ACTIVE"); final String expected = fixture.service.keyToString(model.getKey());
      final ModelManifest selected = fixture.service.findManifest(ModelText.class);
      final Result<ModelCreateResult<ModelText>> result = new Result<>(); fixture.service.createIfAbsent(selected, model, result);
      model.setText("caller changed"); model.setTime(999); model.getKey().setId("different"); model.getKey().getParent().setId("2");
      Collections.reverse(Arrays.asList(selected.getPropertyNames())); fixture.service.drain();
      assertEquals("ACTIVE", result.success().getModel().getText()); assertEquals(100, result.value.getModel().getTime(), 0);
      assertEquals(expected, fixture.service.keyToString(result.value.getModel().getKey())); assertEquals(73, result.value.getModel().getKey().getKeyType());
      assertEquals(ModelKey.KEY_TYPE_LONG, result.value.getModel().getKey().getParent().getKeyType());
      assertTrue(Files.exists(fixture.root.resolve("child-a/text/day/1/brush-teeth"))); assertFalse(Files.exists(fixture.root.resolve("child-a/text/day/2/different")));
    }
  }

  @Test public void protectedExistingIsReturnedWithoutChangingAnyProtectionButMissingProtectedFails() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelText finished = progress(fixture.service, "FINISHED"); save(fixture.service, finished);
      fixture.store.commit(new ModelTransaction("fixture/1", "fixture", "parent", "protect", Collections.singletonList(
          new ModelMutation(key(fixture.service, finished), 0, fixture.service.serialize(ModelText.class, finished).toSource().getBytes(StandardCharsets.UTF_8)))));
      final Map<String, byte[]> before = new TreeMap<>();
      try (java.util.stream.Stream<Path> paths = Files.walk(fixture.root.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY))) {
        paths.filter(Files::isRegularFile).forEach(p -> { try { before.put(fixture.root.relativize(p).toString(), Files.readAllBytes(p)); } catch (IOException e) { throw new IllegalStateException(e); } });
      }
      final Result<ModelCreateResult<ModelText>> existing = new Result<>(); fixture.service.createIfAbsent(progress(fixture.service, "ACTIVE"), existing); fixture.service.drain();
      assertFalse(existing.success().isCreated()); assertEquals("FINISHED", existing.value.getModel().getText()); assertTrue(fixture.store.isProtected(key(fixture.service, finished)));
      for (Map.Entry<String, byte[]> entry : before.entrySet()) assertArrayEquals(entry.getValue(), Files.readAllBytes(fixture.root.resolve(entry.getKey())));
      Files.delete(fixture.root.resolve("child-a/text/day/1/brush-teeth"));
      final Result<ModelCreateResult<ModelText>> missing = new Result<>(); fixture.service.createIfAbsent(progress(fixture.service, "ACTIVE"), missing); fixture.service.drain();
      assertTrue(missing.failure() instanceof IOException); assertFalse(missing.error instanceof ModelNotFoundException); assertFalse(Files.exists(fixture.root.resolve("child-a/text/day/1/brush-teeth")));
    }
  }

  @Test public void corruptExistingImageFailsWithoutReplacingBytes() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final Path file = fixture.root.resolve("child-a/text/day/1/brush-teeth"); Files.createDirectories(file.getParent()); final byte[] corrupt = "not a model".getBytes(StandardCharsets.UTF_8); Files.write(file, corrupt);
      final Result<ModelCreateResult<ModelText>> result = new Result<>(); fixture.service.createIfAbsent(progress(fixture.service, "ACTIVE"), result); fixture.service.drain();
      assertNotNull(result.failure()); assertFalse(result.error instanceof ModelNotFoundException); assertArrayEquals(corrupt, Files.readAllBytes(file));
    }
  }

  @Test public void rejectsUnavailableIncompleteAndWrongManifestBeforeAnyStoreWrite() throws Exception {
    final Service unattached = new Service(); final Result<ModelCreateResult<ModelText>> unavailable = new Result<>();
    unattached.createIfAbsent(progress(unattached, "ACTIVE"), unavailable); assertFalse(unattached.supportsCreateIfAbsent()); assertTrue(unavailable.failure() instanceof UnsupportedOperationException);
    assertTrue(unattached.operations.isEmpty());
    try (Fixture fixture = new Fixture()) {
      final ModelText model = progress(fixture.service, "ACTIVE"); model.getKey().setId(null);
      final Result<ModelCreateResult<ModelText>> missingKey = new Result<>(); fixture.service.createIfAbsent(model, missingKey); assertTrue(missingKey.failure() instanceof IOException); assertNull(model.getKey().getId());
      model.setKey(progress(fixture.service, "ACTIVE").getKey());
      final Result<ModelCreateResult<ModelText>> wrong = new Result<>(); fixture.service.createIfAbsent(ModelUtil.createManifest(ModelText.class), model, wrong); assertTrue(wrong.failure() instanceof IllegalArgumentException);
      assertTrue(fixture.service.operations.isEmpty()); assertFalse(Files.exists(fixture.root.resolve("child-a")));
    }
  }

  @Test public void uncertainAcknowledgmentFailureCanRetrySameKeyWithoutReplacingCommittedState() throws Exception {
    try (Fixture fixture = new Fixture()) {
      fixture.service.rejectAcknowledgment = true;
      final Result<ModelCreateResult<ModelText>> uncertain = new Result<>(); fixture.service.createIfAbsent(progress(fixture.service, "ACTIVE"), uncertain); fixture.service.drain();
      assertTrue(uncertain.failure() instanceof IllegalStateException); assertTrue(Files.exists(fixture.root.resolve("child-a/text/day/1/brush-teeth")));
      fixture.service.rejectAcknowledgment = false; save(fixture.service, progress(fixture.service, "FINISHED"));
      final Result<ModelCreateResult<ModelText>> retry = new Result<>(); fixture.service.createIfAbsent(progress(fixture.service, "ACTIVE"), retry); fixture.service.drain();
      assertFalse(retry.success().isCreated()); assertEquals("FINISHED", retry.value.getModel().getText());
    }
  }

  @Test public void acknowledgmentDecodeAndObserverAreOutsideGateAndObserverThrowsDoNotDoubleNotify() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final Result<ModelCreateResult<ModelText>> done = new Result<>();
      final ModelText expectedModel = progress(fixture.service, "ACTIVE");
      fixture.service.acknowledgmentDecode = () -> assertOtherThreadCanRead(fixture, expectedModel);
      fixture.service.createIfAbsent(progress(fixture.service, "ACTIVE"), new SuccessHandler<ModelCreateResult<ModelText>>() {
        @Override public void onSuccess(final ModelCreateResult<ModelText> value) {
          assertOtherThreadCanRead(fixture, value.getModel());
          done.onSuccess(value);
        }
      }); fixture.service.drain(); done.success();
      final int[] errors = {0};
      class Throwing implements SuccessHandler<ModelCreateResult<ModelText>>, ErrorHandler<Throwable> {
        @Override public void onSuccess(final ModelCreateResult<ModelText> value) { throw new IllegalStateException("observer"); }
        @Override public void onError(final Throwable failure) { errors[0]++; }
      }
      fixture.service.createIfAbsent(progress(fixture.service, "ACTIVE"), new Throwing());
      try { fixture.service.drain(); fail("observer must throw"); } catch (IllegalStateException expected) { assertEquals("observer", expected.getMessage()); }
      assertEquals(0, errors[0]);
    }
  }

  private static void assertOtherThreadCanRead(final Fixture fixture, final Model model) {
    final CountDownLatch read = new CountDownLatch(1); final AtomicReference<Throwable> failure = new AtomicReference<>();
    final Thread other = new Thread(() -> { try { fixture.store.read(key(fixture.service, model)); } catch (Throwable error) { failure.set(error); } finally { read.countDown(); } }); other.start();
    try { assertTrue("decode/callback must not retain the owned gate", read.await(3, TimeUnit.SECONDS)); other.join(3000); }
    catch (InterruptedException e) { throw new AssertionError(e); }
    if (failure.get() != null) throw new AssertionError(failure.get());
  }

  @Test public void captureDoesNotAutoSaveReferencesAndRootModelListIsRejected() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelContent parent = fixture.service.create(ModelContent.class); parent.setKey(new ModelKeyDefault("child-a", "content", "root"));
      final ModelContent referenced = fixture.service.create(ModelContent.class); referenced.setKey(new ModelKeyDefault("child-a", "content", "reference"));
      final ModelList<ModelContent> related = fixture.service.create(ModelList.class); related.setModelType(ModelContent.class); related.add(referenced); parent.setRelated(related);
      final Result<ModelCreateResult<ModelContent>> result = new Result<>(); fixture.service.createIfAbsent(parent, result); assertEquals(0, fixture.service.ordinaryWrites); fixture.service.drain(); result.success();
      assertFalse(Files.exists(fixture.root.resolve("child-a/content/reference"))); assertEquals(0, fixture.service.ordinaryWrites); assertTrue(fixture.service.operations.isEmpty());
      related.setKey(new ModelKeyDefault("child-a", "list", "list-root")); final Result<ModelCreateResult<ModelList<ModelContent>>> root = new Result<>(); fixture.service.createIfAbsent(related, root); assertTrue(root.failure() instanceof UnsupportedOperationException);
    }
  }

  @Test public void ordinaryPersistenceRemainsCompatibleWithConditionalExistingReferences() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelContent parent = fixture.service.create(ModelContent.class);
      parent.setKey(fixture.service.newKey("child-a", "content", "root"));
      parent.setText("canonical-parent");
      final ModelContent reference = fixture.service.create(ModelContent.class);
      reference.setKey(fixture.service.newKey("child-a", "content", "reference"));
      reference.setText("canonical-reference");
      parent.related().add(reference);
      assertTrue(fixture.service.findManifest(ModelContent.class).isKeyOnly("related"));
      assertTrue(fixture.service.findManifest(ModelContent.class).isAutoSave("related"));
      save(fixture.service, parent);
      final Path parentFile = fixture.root.resolve("child-a/content/root");
      final Path referenceFile = fixture.root.resolve("child-a/content/reference");
      final byte[] parentBytes = Files.readAllBytes(parentFile);
      final byte[] referenceBytes = Files.readAllBytes(referenceFile);
      final int writes = fixture.service.ordinaryWrites;

      parent.setText("stale-parent"); reference.setText("stale-reference");
      final Result<ModelCreateResult<ModelContent>> existing = new Result<>();
      fixture.service.createIfAbsent(parent, existing); fixture.service.drain();
      final ModelContent acknowledged = existing.success().getModel();
      assertFalse(existing.value.isCreated());
      assertEquals("canonical-parent", acknowledged.getText());
      assertEquals(fixture.service.keyToString(reference.getKey()),
          fixture.service.keyToString(acknowledged.getRelated().first().getKey()));
      assertNull("Conditional acknowledgments must not hydrate references",
          acknowledged.getRelated().first().getText());
      assertEquals(writes, fixture.service.ordinaryWrites);
      assertArrayEquals(parentBytes, Files.readAllBytes(parentFile));
      assertArrayEquals(referenceBytes, Files.readAllBytes(referenceFile));
      assertTrue(fixture.service.operations.isEmpty());
    }
  }

  @Test public void conditionalCreationRemainsCompatibleWithOrdinaryReferenceLoading() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelContent reference = fixture.service.create(ModelContent.class);
      reference.setKey(fixture.service.newKey("child-a", "content", "reference"));
      reference.setText("stored-reference");
      save(fixture.service, reference);
      final ModelContent parent = fixture.service.create(ModelContent.class);
      parent.setKey(fixture.service.newKey("child-a", "content", "root"));
      parent.setText("created-parent");
      reference.setText("unsaved-caller-reference");
      parent.related().add(reference);
      final ModelManifest selected = fixture.service.findManifest(ModelContent.class);
      final Class<?>[] componentTypes = selected.getMethodData("related").getTypeParams();
      final Class<?> componentType = componentTypes[0];
      final Result<ModelCreateResult<ModelContent>> created = new Result<>();
      fixture.service.createIfAbsent(selected, parent, created);
      componentTypes[0] = ModelText.class;
      try {
        fixture.service.drain();
        assertTrue(created.success().isCreated());
        final ModelContent acknowledgedReference = created.value.getModel().getRelated().first();
        assertEquals(fixture.service.keyToString(reference.getKey()),
            fixture.service.keyToString(acknowledgedReference.getKey()));
        assertNull(acknowledgedReference.getText());
      } finally {
        componentTypes[0] = componentType;
      }

      final Result<ModelContent> loaded = new Result<>();
      fixture.service.load(ModelContent.class, parent.getKey(), loaded); fixture.service.drain();
      assertEquals("created-parent", loaded.success().getText());
      final ModelContent loadedReference = loaded.value.getRelated().first();
      assertNull("Owned-store loads must not hydrate references", loadedReference.getText());
      assertEquals(fixture.service.keyToString(reference.getKey()),
          fixture.service.keyToString(loadedReference.getKey()));
      final Result<ModelContent> explicitlyLoadedReference = new Result<>();
      fixture.service.load(ModelContent.class, loadedReference.getKey(), explicitlyLoadedReference);
      fixture.service.drain();
      assertEquals("stored-reference", explicitlyLoadedReference.success().getText());
      assertEquals(1, fixture.service.ordinaryWrites);
      assertTrue(fixture.service.operations.isEmpty());
    }
  }

  @Test public void byteLimitCountsUtf8RatherThanCharacters() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelText model = progress(fixture.service, "ACTIVE"); final char[] chars = new char[ModelServiceWithCreateIfAbsent.MAX_MODEL_BYTES / 2]; Arrays.fill(chars, '\u20ac'); model.setText(new String(chars));
      final Result<ModelCreateResult<ModelText>> tooLarge = new Result<>(); fixture.service.createIfAbsent(model, tooLarge);
      assertTrue(tooLarge.failure() instanceof IOException); assertTrue(fixture.service.operations.isEmpty()); assertFalse(Files.exists(fixture.root.resolve("child-a")));
    }
  }
  @Test public void fileAcknowledgmentsPreserveServerOutboundFieldsForCreatedAndExistingImages() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final xapi.model.test.api.ModelTestDirections candidate=fixture.service.create(xapi.model.test.api.ModelTestDirections.class);
      candidate.setKey(new ModelKeyDefault("child-a",candidate.getType(),"directional")); candidate.setServerToClient("canonical-server-field"); candidate.setClientToServer("not-stored"); candidate.setBothEnabled("both");
      final Result<ModelCreateResult<xapi.model.test.api.ModelTestDirections>> first=new Result<>(); fixture.service.createIfAbsent(candidate,first); fixture.service.drain();
      assertEquals("canonical-server-field",first.success().getModel().getServerToClient()); assertEquals("both",first.value.getModel().getBothEnabled()); assertNull(first.value.getModel().getClientToServer());
      candidate.setServerToClient("stale-caller"); final Result<ModelCreateResult<xapi.model.test.api.ModelTestDirections>> second=new Result<>(); fixture.service.createIfAbsent(candidate,second); fixture.service.drain();
      assertFalse(second.success().isCreated()); assertEquals("canonical-server-field",second.value.getModel().getServerToClient());
    }
  }
  @Test public void fileAcknowledgmentsPreserveDirectionalListItemsForCreatedAndExistingImages() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelTestDirectionsEmbedded candidate = fixture.service.create(ModelTestDirectionsEmbedded.class);
      candidate.setKey(new ModelKeyDefault("child-a", candidate.getType(), "directional-list"));
      final ModelTestDirections item = fixture.service.create(ModelTestDirections.class);
      item.setKey(candidate.getKey().getChild(item.getType(), "item"));
      item.setServerToClient("canonical-server-field");
      item.setClientToServer("not-stored");
      item.setBothEnabled("canonical-shared-field");
      item.setBothDisabled("not-on-wire");
      final ModelList<ModelTestDirections> list = fixture.service.create(ModelList.class);
      list.setModelType(ModelTestDirections.class);
      list.add(item);
      candidate.setList(list);

      final Result<ModelCreateResult<ModelTestDirectionsEmbedded>> first = new Result<>();
      fixture.service.createIfAbsent(candidate, first); fixture.service.drain();
      assertTrue(first.success().isCreated());
      final ModelTestDirections createdItem = first.value.getModel().getList().first();
      assertNotSame(item, createdItem);
      assertEquals("canonical-server-field", createdItem.getServerToClient());
      assertEquals("canonical-shared-field", createdItem.getBothEnabled());
      assertNull(createdItem.getClientToServer()); assertNull(createdItem.getBothDisabled());

      item.setServerToClient("stale-caller"); item.setBothEnabled("stale-shared-field");
      final Result<ModelCreateResult<ModelTestDirectionsEmbedded>> second = new Result<>();
      fixture.service.createIfAbsent(candidate, second); fixture.service.drain();
      assertFalse(second.success().isCreated());
      final ModelTestDirections existingItem = second.value.getModel().getList().first();
      assertNotSame(item, existingItem);
      assertEquals("canonical-server-field", existingItem.getServerToClient());
      assertEquals("canonical-shared-field", existingItem.getBothEnabled());
      assertNull(existingItem.getClientToServer()); assertNull(existingItem.getBothDisabled());
    }
  }
  @Test public void malformedUtf8ExistingImageFailsClosedWithoutChangingStoredBytes() throws Exception {
    try (Fixture fixture = new Fixture()) {
      final ModelText candidate=progress(fixture.service,"markerX"); final Result<ModelCreateResult<ModelText>> first=new Result<>(); fixture.service.createIfAbsent(candidate,first); fixture.service.drain(); first.success();
      final Path path=fixture.root.resolve("child-a/text/day/1/brush-teeth"); final byte[] bytes=Files.readAllBytes(path);
      final byte[] marker="markerX".getBytes(StandardCharsets.UTF_8); int found=-1;
      for(int i=0;i<=bytes.length-marker.length;i++) { boolean same=true; for(int j=0;j<marker.length;j++)if(bytes[i+j]!=marker[j])same=false; if(same){found=i;break;} }
      assertTrue(found>=0); bytes[found+marker.length-1]=(byte)0xff; Files.write(path,bytes);
      final Result<ModelCreateResult<ModelText>> retry=new Result<>(); fixture.service.createIfAbsent(candidate,retry); fixture.service.drain();
      assertTrue(retry.failure() instanceof java.io.IOException); assertArrayEquals(bytes,Files.readAllBytes(path));
    }
  }

}
