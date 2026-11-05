package xapi.jre.model;

import org.junit.Test;
import xapi.model.api.*;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelKeyDefault;
import xapi.model.user.ModelUser;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

/** Scoped PR-review regressions using only programmatic models and private temporary stores. */
public class ModelReviewRegressionTest {
  private static final class Service extends ModelServiceJre {
    final List<Runnable> reads = new ArrayList<>();
    @Override protected void dispatchFileRead(final Runnable operation) { reads.add(operation); }
    void drain() { while (!reads.isEmpty()) reads.remove(0).run(); }
  }
  private static final class Result<T> implements SuccessHandler<T>, ErrorHandler<Throwable> {
    T value; Throwable error; int callbacks;
    @Override public void onSuccess(final T value) { this.value = value; callbacks++; }
    @Override public void onError(final Throwable error) { this.error = error; callbacks++; }
    T success() { assertEquals(1, callbacks); if (error != null) throw new AssertionError(error); return value; }
    Throwable failed() { assertEquals(1, callbacks); assertNotNull(error); return error; }
  }
  private static ModelText text(final Service service, final String namespace, final String id) {
    final ModelText model = service.create(ModelText.class); model.setKey(new ModelKeyDefault(namespace, "text", id)); model.setText(id); return model;
  }
  private static <M extends Model> void save(final Service service, final M model) {
    final Result<M> result = new Result<>(); service.persist(model, result); result.success();
  }
  private static ModelStoreKey key(final Service service, final Model model, final String path) {
    return new ModelStoreKey(service.keyToString(model.getKey()), path);
  }
  private static void protect(final Service service, final FileModelTransactionStore store,
      final Model model, final String path, final String operation) throws Exception {
    store.commit(new ModelTransaction("review/1", "fixture", "parent", operation, Collections.singletonList(
        new ModelMutation(key(service, model, path), 0, service.serialize((Class)model.getClass().getInterfaces()[0], model).toSource().getBytes(StandardCharsets.UTF_8)))));
  }

  @Test public void missingProtectedRecordFailsEveryPageAndAllTypedQueryForms() throws Exception {
    final Path root = Files.createTempDirectory("xapi-review-");
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      final Service service = new Service(); service.attachTransactionStore(store);
      final ModelText a = text(service, "A", "a"), b = text(service, "A", "b"); save(service, a); save(service, b);
      protect(service, store, a, "A/text/a", "protect-a");
      Files.delete(root.resolve("A/text/a"));
      for (int form = 0; form < 2; form++) {
        final Result<ModelQueryResult<ModelText>> result = new Result<>();
        final ModelQuery<ModelText> query = new ModelQuery<ModelText>().setNamespace("A").setCursor("b").setPageSize(1);
        if (form == 0) service.query(ModelText.class, query, result);
        else service.query(service.findManifest(ModelText.class), query, result);
        service.drain(); assertTrue(result.failed() instanceof ModelQueryFailureException);
      }
      final Result<ModelQueryResult<ModelText>> emptyScope = new Result<>();
      service.query(ModelText.class, new ModelQuery<ModelText>().setNamespace("unrelated"), emptyScope); service.drain();
      assertEquals(0, emptyScope.success().getSize());
    }
  }

  @Test public void entireProtectedBucketAndNamespaceLossCannotBecomeAnEmptyUntypedQuery() throws Exception {
    for (boolean removeNamespace : Arrays.asList(false, true)) {
      final Path root = Files.createTempDirectory("xapi-review-");
      try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
        final Service service = new Service(); service.attachTransactionStore(store);
        final ModelText a = text(service, "A", "a"), unrelated = text(service, "B", "b"); save(service, a); save(service, unrelated);
        protect(service, store, a, "A/text/a", "protect");
        Files.delete(root.resolve("A/text/a")); Files.delete(root.resolve("A/text"));
        if (removeNamespace) Files.delete(root.resolve("A"));
        final Result<ModelQueryResult<Model>> missing = new Result<>();
        service.query(new ModelQuery<Model>().setNamespace("A"), missing); service.drain();
        assertTrue(missing.failed() instanceof ModelQueryFailureException);
        final Result<ModelQueryResult<ModelText>> typed = new Result<>(), manifest = new Result<>();
        service.query(ModelText.class, new ModelQuery<ModelText>().setNamespace("A"), typed);
        service.query(service.findManifest(ModelText.class), new ModelQuery<ModelText>().setNamespace("A"), manifest);
        service.drain(); assertTrue(typed.failed() instanceof ModelQueryFailureException); assertTrue(manifest.failed() instanceof ModelQueryFailureException);
        final Result<ModelQueryResult<Model>> other = new Result<>();
        service.query(new ModelQuery<Model>().setNamespace("B"), other); service.drain(); assertEquals(1, other.success().getSize());
      }
    }
  }

  @Test public void missingProtectedChildOnlyInvalidatesItsExactAncestorBucket() throws Exception {
    final Path root = Files.createTempDirectory("xapi-review-");
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      final Service service = new Service(); service.attachTransactionStore(store);
      final ModelKey day7 = new ModelKeyDefault("A", "day", "7"), day8 = new ModelKeyDefault("A", "day", "8");
      final ModelText missing = text(service, "A", "item"), healthy = text(service, "A", "item");
      missing.getKey().setParent(day7); healthy.getKey().setParent(day8); save(service, missing); save(service, healthy);
      protect(service, store, missing, "A/text/day/7/item", "protect-child"); Files.delete(root.resolve("A/text/day/7/item"));
      final Result<ModelQueryResult<ModelText>> selected = new Result<>(), unrelated = new Result<>(), top = new Result<>();
      service.query(ModelText.class, new ModelQuery<ModelText>().setNamespace("A").setAncestor(day7).setCursor("zzz"), selected);
      service.query(ModelText.class, new ModelQuery<ModelText>().setNamespace("A").setAncestor(day8), unrelated);
      service.query(ModelText.class, new ModelQuery<ModelText>().setNamespace("A"), top); service.drain();
      assertTrue(selected.failed() instanceof ModelQueryFailureException); assertEquals(1, unrelated.success().getSize()); assertEquals(0, top.success().getSize());
    }
  }

  @Test public void metadataBasenameIsAnOrdinaryRecordExceptAtTheReservedRootDirectory() throws Exception {
    for (boolean attached : Arrays.asList(false, true)) {
      final Path root = Files.createTempDirectory("xapi-review-");
      try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
        final Service service = new Service();
        if (attached) service.attachTransactionStore(store); else service.setRootDir(root.toFile());
        final ModelText model = text(service, "", FileModelTransactionStore.INTERNAL_DIRECTORY); save(service, model);
        final Result<ModelText> loaded = new Result<>(); service.load(ModelText.class, model.getKey(), loaded); service.drain();
        assertEquals(model.getText(), loaded.success().getText());
        final Result<ModelQueryResult<ModelText>> typed = new Result<>(); service.query(ModelText.class, new ModelQuery<ModelText>(), typed);
        final Result<ModelQueryResult<Model>> untyped = new Result<>(); service.query(new ModelQuery<Model>(), untyped); service.drain();
        assertEquals(1, typed.success().getSize()); assertEquals(1, untyped.success().getSize());
      }
    }
  }

  @Test public void validatedCapabilityRejectsAnUnownedProviderButOrdinaryManifestQueriesStillWork() throws Exception {
    final Service service = new Service(); service.setRootDir(Files.createTempDirectory("xapi-review-").toFile());
    save(service, text(service, "", "a")); final int[] validated = {0};
    final Result<ModelQueryResult<ModelText>> rejected = new Result<>();
    service.query(service.findManifest(ModelText.class), new ModelQuery<ModelText>(), captured -> validated[0]++, rejected);
    assertTrue(rejected.failed() instanceof IllegalStateException); assertEquals(0, validated[0]); assertTrue(service.reads.isEmpty());
    final Result<ModelQueryResult<ModelText>> ordinary = new Result<>();
    service.query(service.findManifest(ModelText.class), new ModelQuery<ModelText>(), ordinary); service.drain(); assertEquals(1, ordinary.success().getSize());
  }

  @Test public void unownedDeferredQueriesCaptureNamespaceAncestorAndCursorBeforeDispatch() throws Exception {
    final Service service = new Service(); service.setRootDir(Files.createTempDirectory("xapi-review-").toFile());
    final ModelKey parent = new ModelKeyDefault("A", "day", "7");
    final ModelText a = text(service, "A", "second"); a.getKey().setParent(parent); save(service, a);
    for (int form = 0; form < 2; form++) {
      final ModelQuery<ModelText> query = new ModelQuery<ModelText>().setNamespace("A").setAncestor(parent).setCursor("second");
      final Result<ModelQueryResult<ModelText>> result = new Result<>();
      if (form == 0) service.query(ModelText.class, query, result); else service.query(service.findManifest(ModelText.class), query, result);
      query.setNamespace("B").setAncestor(new ModelKeyDefault("B", "day", "99")).setCursor("zzz");
      service.drain(); assertEquals(1, result.success().getSize());
    }
    save(service, text(service, "A", "flat"));
    final ModelQuery<Model> query = new ModelQuery<Model>().setNamespace("A").setCursor(UntypedModelCursor.encode("text", "flat"));
    final Result<ModelQueryResult<Model>> result = new Result<>(); service.query(query, result);
    query.setNamespace("B").setCursor("zzz"); service.drain(); assertEquals(1, result.success().getSize());
  }

  @Test public void attachedSnapshotsKeepKeyOnlyReferencesStableWhileInlineChildrenRemainAvailable() throws Exception {
    final Path root = Files.createTempDirectory("xapi-review-");
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      final Service service = new Service(); service.attachTransactionStore(store);
      final ModelUser author = service.create(ModelUser.class); author.setKey(new ModelKeyDefault("", "user", "u")); author.setDisplayName("old-author"); save(service, author);
      final ModelContent parent = service.create(ModelContent.class); parent.setKey(new ModelKeyDefault("", "content", "p")); parent.setAuthor(author); parent.setText("parent");
      final ModelContent child = service.create(ModelContent.class); child.setKey(parent.getKey().getChild("content", "child")); child.setText("inline-child"); child.setAuthor(author);
      final ModelList<ModelContent> children = service.create(ModelList.class); children.setModelType(ModelContent.class); children.add(child); parent.setChildren(children); save(service, parent);
      final Result<ModelContent> load = new Result<>(); service.load(ModelContent.class, parent.getKey(), load);
      service.reads.remove(0).run(); final ModelContent snapshot = load.success();
      assertTrue("key-only references must not queue hidden loads", service.reads.isEmpty());
      assertNotNull(snapshot.getAuthor()); assertNull(snapshot.getAuthor().getDisplayName());
      assertEquals(service.keyToString(author.getKey()), service.keyToString(snapshot.getAuthor().getKey()));
      assertEquals("inline-child", snapshot.getChildren().first().getText()); assertNull(snapshot.getChildren().first().getAuthor().getDisplayName());
      author.setDisplayName("new-author"); protect(service, store, author, "user/u", "author-change"); service.drain();
      assertNull(snapshot.getAuthor().getDisplayName()); assertNull(snapshot.getChildren().first().getAuthor().getDisplayName());
      final Result<ModelQueryResult<ModelContent>> query = new Result<>(); service.query(service.findManifest(ModelContent.class), new ModelQuery<ModelContent>(), query); service.drain();
      assertEquals(1, query.success().getSize()); assertNull(query.value.getModelList().iterator().next().getAuthor().getDisplayName()); assertTrue(service.reads.isEmpty());
    }
  }

  @Test public void unownedLegacyKeyOnlyHydrationStillRunsAfterOuterCallback() throws Exception {
    final Service service = new Service(); service.setRootDir(Files.createTempDirectory("xapi-review-").toFile());
    final ModelUser author = service.create(ModelUser.class); author.setKey(new ModelKeyDefault("", "user", "u")); author.setDisplayName("hydrated-author"); save(service, author);
    final ModelContent parent = service.create(ModelContent.class); parent.setKey(new ModelKeyDefault("", "content", "p")); parent.setAuthor(author); save(service, parent);
    final Result<ModelContent> load = new Result<>(); service.load(ModelContent.class, parent.getKey(), load); service.reads.remove(0).run();
    final ModelContent snapshot = load.success(); assertNull(snapshot.getAuthor().getDisplayName()); assertFalse(service.reads.isEmpty());
    service.drain(); assertEquals("hydrated-author", snapshot.getAuthor().getDisplayName());
  }
}
