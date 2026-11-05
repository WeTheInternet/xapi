package xapi.jre.model;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xapi.dev.source.CharBuffer;
import xapi.fu.In2;
import xapi.model.api.Model;
import xapi.model.api.ModelDeserializationException;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelNotFoundException;
import xapi.model.api.ModelQuery;
import xapi.model.api.ModelQueryResult;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelKeyDefault;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.io.IOException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class ModelFilePersistenceTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private ModelServiceJre service() {
    ModelServiceJre service = new ModelServiceJre();
    service.setRootDir(folder.getRoot());
    return service;
  }

  private ModelText model(ModelServiceJre service, String id) {
    ModelText model = service.create(ModelText.class);
    model.setKey(new ModelKeyDefault("", "text", id));
    model.setText("preserved UTF-8 — data");
    return model;
  }

  @Test public void realModelSerializationAndLoadKeepExistingFormatAndKey() throws Exception {
    ModelServiceJre service = service();
    ModelText model = model(service, "existing");
    String serialized = service.serialize(ModelText.class, model).toSource();
    Result<ModelText> save = new Result<>();
    service.persist(model, save);
    assertSame(model, save.success());
    assertEquals(serialized, new String(Files.readAllBytes(folder.getRoot().toPath()
        .resolve("text/existing")), StandardCharsets.UTF_8));
    Result<ModelText> load = new Result<>();
    service.load(ModelText.class, model.getKey(), load);
    assertEquals(model.getText(), load.success().getText());
    assertSame(model.getKey(), load.value.getKey());
  }

  @Test public void serializationFailureDoesNotTouchExistingModel() throws Exception {
    ModelServiceJre service = new ModelServiceJre() {
      @Override protected <M extends Model> void serialize(String type, M model, In2<CharBuffer, Throwable> callback) {
        callback.in(null, new IOException("injected serializer"));
      }
    };
    service.setRootDir(folder.getRoot());
    Path path = folder.getRoot().toPath().resolve("text/existing");
    Files.createDirectories(path.getParent());
    Files.write(path, "old".getBytes(StandardCharsets.UTF_8));
    Result<ModelText> save = new Result<>();
    service.persist(model(service, "existing"), save);
    assertTrue(save.failure() instanceof IOException);
    assertEquals("old", new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
  }

  @Test public void generatedIdSerializationFailureLeavesNoVisibleEmptyRecord() throws Exception {
    ModelServiceJre service = new ModelServiceJre() {
      @Override protected <M extends Model> void serialize(String type, M model, In2<CharBuffer, Throwable> callback) {
        callback.in(null, new IOException("injected serializer"));
      }
    };
    service.setRootDir(folder.getRoot());
    ModelText model = model(service, null);
    Result<ModelText> save = new Result<>();
    service.persist(model, save);
    assertTrue(save.failure() instanceof IOException);
    assertNull("rejected provisional ID must not become a retry overwrite", model.getKey().getId());
    assertFalse(Files.exists(folder.getRoot().toPath().resolve("text")));
  }

  @Test public void rootAliasesCannotReserveTheSameGeneratedIdWhileSerializationIsDeferred() throws Exception {
    class DeferredService extends ModelServiceJre {
      final List<Runnable> pending = new ArrayList<>();
      @Override protected <M extends Model> void serialize(String type, M model, In2<CharBuffer, Throwable> callback) {
        CharBuffer buffer = super.serialize(type, model);
        pending.add(() -> callback.in(buffer, null));
      }
    }
    DeferredService firstService = new DeferredService();
    DeferredService secondService = new DeferredService();
    firstService.setRootDir(folder.getRoot());
    secondService.setRootDir(new File(folder.getRoot(), "."));
    ModelText first = model(firstService, null);
    ModelText second = model(secondService, null);
    first.setText("first record");
    second.setText("second record");
    Result<ModelText> firstSave = new Result<>();
    Result<ModelText> secondSave = new Result<>();
    firstService.persist(first, firstSave);
    secondService.persist(second, secondSave);
    assertNotEquals(first.getKey().getId(), second.getKey().getId());
    assertFalse(Files.exists(folder.getRoot().toPath().resolve("text")));
    secondService.pending.get(0).run();
    firstService.pending.get(0).run();
    firstSave.success();
    secondSave.success();
    Result<ModelText> firstLoad = new Result<>();
    Result<ModelText> secondLoad = new Result<>();
    firstService.load(ModelText.class, first.getKey(), firstLoad);
    secondService.load(ModelText.class, second.getKey(), secondLoad);
    assertEquals("first record", firstLoad.success().getText());
    assertEquals("second record", secondLoad.success().getText());
  }

  @Test public void corruptionIsNotReportedAsMissingOrOverwritten() throws Exception {
    ModelServiceJre service = service();
    ModelText model = model(service, "corrupt");
    Path path = folder.getRoot().toPath().resolve("text/corrupt");
    Files.createDirectories(path.getParent());
    byte[] corrupt = "not a model".getBytes(StandardCharsets.UTF_8);
    Files.write(path, corrupt);
    Result<ModelText> load = new Result<>();
    service.load(ModelText.class, model.getKey(), load);
    assertTrue(load.failure() instanceof ModelDeserializationException);
    assertFalse(load.error instanceof ModelNotFoundException);
    assertArrayEquals(corrupt, Files.readAllBytes(path));
    assertFalse(load.error.getMessage().contains("not a model"));
  }

  @Test public void genuinelyAbsentRecordIsMissingWithoutStorageCause() throws Exception {
    ModelServiceJre service = service();
    Result<ModelText> load = new Result<>();
    service.load(ModelText.class, new ModelKeyDefault("", "text", "absent"), load);
    assertTrue(load.failure() instanceof ModelNotFoundException);
    assertNull(load.error.getCause());
  }

  @Test public void directoryInPlaceOfFileIsStorageFailureNotMissing() throws Exception {
    ModelServiceJre service = service();
    Files.createDirectories(folder.getRoot().toPath().resolve("text/directory"));
    Result<ModelText> load = new Result<>();
    service.load(ModelText.class, new ModelKeyDefault("", "text", "directory"), load);
    assertTrue(load.failure() instanceof IOException);
    assertFalse(load.error instanceof ModelNotFoundException);
  }

  @Test public void typedAndUntypedQueriesIgnoreAbandonedTempsAndNestedDirectories() throws Exception {
    ModelServiceJre service = service();
    Result<ModelText> save = new Result<>();
    service.persist(model(service, "valid"), save);
    save.success();
    Path kind = folder.getRoot().toPath().resolve("text");
    Files.write(kind.resolve(AtomicModelFile.TEMP_PREFIX + "interrupted.tmp"), new byte[] {1, 2});
    Files.createDirectories(kind.resolve("ancestor"));
    ModelQuery<ModelText> query = new ModelQuery<>();
    Result<ModelQueryResult<ModelText>> typed = new Result<>();
    service.query(ModelText.class, query, typed);
    assertEquals(1, count(typed.success().getModels()));
    Result<ModelQueryResult<Model>> untyped = new Result<>();
    service.query(new ModelQuery<Model>(), untyped);
    assertEquals(1, count(untyped.success().getModels()));
  }

  private static int count(Iterable<?> values) {
    int count = 0;
    for (Object ignored : values) count++;
    return count;
  }

  private static final class Result<T> implements SuccessHandler<T>, ErrorHandler<Throwable> {
    private final CountDownLatch done = new CountDownLatch(1);
    T value;
    Throwable error;
    @Override public void onSuccess(T value) { this.value = value; done.countDown(); }
    @Override public void onError(Throwable error) { this.error = error; done.countDown(); }
    T success() throws Exception {
      assertTrue("callback timeout", done.await(10, TimeUnit.SECONDS));
      if (error != null) throw new AssertionError("unexpected storage failure", error);
      return value;
    }
    Throwable failure() throws Exception {
      assertTrue("callback timeout", done.await(10, TimeUnit.SECONDS));
      assertNotNull("expected storage failure", error);
      return error;
    }
  }
}
