package xapi.jre.model;

import org.junit.Test;
import xapi.model.api.Model;
import xapi.model.api.ModelCreateResult;
import xapi.model.api.ModelDeserializationContext;
import xapi.model.api.ModelList;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelModule;
import xapi.model.api.ModelNotFoundException;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelUtil;
import xapi.model.tools.ModelSerializerDefault;
import xapi.model.tools.PrimitiveSerializerDefault;
import xapi.source.lex.StringCharIterator;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/** Invalid numeric fields and collection counts must fail with ordinary JVM assertions disabled. */
public class ModelScalarFramingTest {
  @Test
  public void rejectsTruncatedScalarsAndAcknowledgmentsWithoutAssertions() throws Exception {
    final Path log = Files.createTempFile("xapi-scalar-framing-", ".log");
    final List<String> command = new ArrayList<>(Arrays.asList(
        System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
        "-da", "-Xmx128m", "-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir")));
    for (final String property : new String[]{"xapi.injector.cache", "xapi.mvn.repo"}) {
      final String value = System.getProperty(property);
      if (value != null) command.add("-D" + property + "=" + value);
    }
    command.addAll(Arrays.asList("-cp", runtimeClasspath(), AssertionsDisabled.class.getName()));
    final Process child = new ProcessBuilder(command)
        .redirectErrorStream(true).redirectOutput(log.toFile()).start();
    try {
      if (!child.waitFor(45, TimeUnit.SECONDS)) {
        child.destroyForcibly();
        child.waitFor(5, TimeUnit.SECONDS);
        fail("Assertion-disabled scalar probe timed out; output: " + log);
      }
      assertEquals("Assertion-disabled scalar probe failed; output: " + log + "\n"
          + new String(Files.readAllBytes(log), StandardCharsets.UTF_8), 0, child.exitValue());
    } finally {
      if (child.isAlive()) {
        child.destroyForcibly();
        child.waitFor(5, TimeUnit.SECONDS);
      }
    }
  }

  private static String runtimeClasspath() throws Exception {
    final Set<String> entries = new LinkedHashSet<>();
    // Gradle workers may keep test dependencies in a loader instead of java.class.path.
    for (ClassLoader loader = ModelScalarFramingTest.class.getClassLoader(); loader != null;
        loader = loader.getParent()) {
      if (loader instanceof URLClassLoader) {
        for (final URL location : ((URLClassLoader) loader).getURLs()) {
          if (!"file".equals(location.getProtocol())) {
            throw new IllegalStateException("Unsupported test classpath URL: " + location);
          }
          entries.add(new File(location.toURI()).getAbsolutePath());
        }
      }
    }
    for (final String entry : System.getProperty("java.class.path", "").split(Pattern.quote(File.pathSeparator), -1)) {
      entries.add(new File(entry.isEmpty() ? "." : entry).getAbsolutePath());
    }
    return String.join(File.pathSeparator, entries);
  }

  public static final class AssertionsDisabled {
    public static void main(final String[] args) {
      try {
        assertTrue("Huge-header probes require a bounded child heap", Runtime.getRuntime().maxMemory() <= 128L * 1024 * 1024);
        assertFalse("Probe must run with -da", ModelScalarFramingTest.class.desiredAssertionStatus());
        assertFalse("Numeric decoder must run with -da", PrimitiveSerializerDefault.class.desiredAssertionStatus());
        assertFalse("Model decoder must run with -da", ModelSerializerDefault.class.desiredAssertionStatus());
        assertFalse("Acknowledgment decoder must run with -da", ModelCreateRequest.class.desiredAssertionStatus());
        validNumericEncodingsRemainUnchanged();
        missingAndUnterminatedScalarsFail();
        invalidNumericAlphabetBoundsAndSentinelsFail();
        malformedHttpAcknowledgmentsFailWithoutFallback();
        corruptExistingFilesFailWithoutReplacement();
        negativeListCountsFailHttpAcknowledgmentsWithoutFallback();
        negativeListCountsFailExistingFilesWithoutReplacement();
        hugeArrayHeadersRejectMissingPayloads();
        hugeArrayAcknowledgmentsFailWithoutFallback();
        hugeArrayExistingFilesFailWithoutReplacement();
        System.out.println("Assertion-disabled scalar, collection-count and bounded-array HTTP/file checks passed.");
      } catch (final Throwable failure) {
        failure.printStackTrace();
        System.exit(1);
      }
      System.exit(0);
    }
  }

  private static void validNumericEncodingsRemainUnchanged() {
    final PrimitiveSerializerDefault codec = new PrimitiveSerializerDefault();
    final int[] values = {0, 1, -1, 31, -31, 32, -32, 33, -33, 1024, Integer.MIN_VALUE};
    final String[] wire = {"E", "T", "1", "\\", "\t", "eT", "e1", "tT", "t1", "eeT", "\0"};
    for (int i = 0; i < values.length; i++) {
      assertEquals(wire[i], codec.serializeInt(values[i]));
      assertEquals(values[i], codec.deserializeInt(new StringCharIterator(wire[i])));
      assertEquals(wire[i], codec.serializeLong(values[i] == Integer.MIN_VALUE ? Long.MIN_VALUE : values[i]));
      assertEquals(values[i] == Integer.MIN_VALUE ? Long.MIN_VALUE : values[i],
          codec.deserializeLong(new StringCharIterator(wire[i])));
    }
    final long[] boundaries = {Long.MIN_VALUE, Long.MIN_VALUE + 1, Integer.MIN_VALUE,
        -1, 0, 1, Integer.MAX_VALUE, Long.MAX_VALUE};
    for (final long value : boundaries) {
      final String encoded = codec.serializeLong(value);
      assertEquals(value, codec.deserializeLong(new StringCharIterator(encoded)));
    }
    for (final int value : new int[]{-Integer.MAX_VALUE, Integer.MAX_VALUE}) {
      assertEquals(value, codec.deserializeInt(new StringCharIterator(codec.serializeInt(value))));
    }
    assertEquals(0.0, codec.deserializeDouble(new StringCharIterator(codec.serializeDouble(0.0))), 0.0);
    assertEquals(200.0, codec.deserializeDouble(new StringCharIterator(codec.serializeDouble(200.0))), 0.0);
    assertEquals(-17.5f, codec.deserializeFloat(new StringCharIterator(codec.serializeFloat(-17.5f))), 0.0f);
    for (final float value : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -0.0f}) {
      assertEquals(Float.floatToIntBits(value),
          Float.floatToIntBits(codec.deserializeFloat(new StringCharIterator(codec.serializeFloat(value)))));
    }
    for (final double value : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.0}) {
      assertEquals(Double.doubleToLongBits(value),
          Double.doubleToLongBits(codec.deserializeDouble(new StringCharIterator(codec.serializeDouble(value)))));
    }
    // Nonminimal forms within the numeric width retain their existing meaning.
    assertEquals(1, codec.deserializeInt(new StringCharIterator("tE")));
    assertEquals(1L, codec.deserializeLong(new StringCharIterator("tE")));
  }

  private static void missingAndUnterminatedScalarsFail() {
    final PrimitiveSerializerDefault codec = new PrimitiveSerializerDefault();
    for (final String wire : new String[]{"", "e", "t", "ttt"}) {
      expectMalformed(() -> codec.deserializeInt(new StringCharIterator(wire)));
      expectMalformed(() -> codec.deserializeLong(new StringCharIterator(wire)));
      expectMalformed(() -> codec.deserializeByte(new StringCharIterator(wire)));
      expectMalformed(() -> codec.deserializeShort(new StringCharIterator(wire)));
      expectMalformed(() -> codec.deserializeChar(new StringCharIterator(wire)));
      expectMalformed(() -> codec.deserializeFloat(new StringCharIterator(wire)));
      expectMalformed(() -> codec.deserializeDouble(new StringCharIterator(wire)));
      expectMalformed(() -> codec.deserializeString(new StringCharIterator(wire)));
    }
    // Existing nonnumeric framing must also reject a missing scalar/payload without -ea.
    try {
      codec.deserializeBoolean(new StringCharIterator(""));
      fail("Missing boolean was accepted");
    } catch (final IndexOutOfBoundsException expected) {
      // StringCharIterator.next() already checks bounds independently of assertions.
    }
    try {
      codec.deserializeString(new StringCharIterator("T"));
      fail("Truncated string payload was accepted");
    } catch (final IllegalStateException expected) {
      // StringCharIterator.consume() already checks the declared payload length.
    }
  }

  private static void invalidNumericAlphabetBoundsAndSentinelsFail() {
    final PrimitiveSerializerDefault codec = new PrimitiveSerializerDefault();
    for (final char invalid : new char[]{1, 8, ' ', '\n', '\r', 31, 127, 128, 0xd800}) {
      for (final String wire : new String[]{Character.toString(invalid), "t" + invalid}) {
        expectMalformed(() -> codec.deserializeInt(new StringCharIterator(wire)));
        expectMalformed(() -> codec.deserializeLong(new StringCharIterator(wire)));
        expectMalformed(() -> codec.deserializeFloat(new StringCharIterator(wire)));
        expectMalformed(() -> codec.deserializeDouble(new StringCharIterator(wire)));
      }
    }
    for (final String wire : new String[]{"e\0", "t\0"}) {
      expectMalformed(() -> codec.deserializeInt(new StringCharIterator(wire)));
      expectMalformed(() -> codec.deserializeLong(new StringCharIterator(wire)));
    }
    for (final String wire : new String[]{zeros(6) + "A", zeros(6) + "2", zeros(7) + "E"}) {
      expectMalformed(() -> codec.deserializeInt(new StringCharIterator(wire)));
      expectMalformed(() -> codec.deserializeFloat(new StringCharIterator(wire)));
    }
    for (final String wire : new String[]{zeros(12) + "R", zeros(12) + "8", zeros(13) + "E"}) {
      expectMalformed(() -> codec.deserializeLong(new StringCharIterator(wire)));
      expectMalformed(() -> codec.deserializeDouble(new StringCharIterator(wire)));
    }
  }

  private static String zeros(final int count) {
    final char[] digits = new char[count];
    Arrays.fill(digits, 'e');
    return new String(digits);
  }

  private static void expectMalformed(final Runnable decode) {
    try {
      decode.run();
      fail("Malformed numeric scalar was accepted");
    } catch (final IllegalArgumentException expected) {
      assertTrue(causedByMalformedScalar(expected));
    }
  }

  private static void malformedHttpAcknowledgmentsFailWithoutFallback() throws Exception {
    final ModelCreateIfAbsentHttpTest fixture = new ModelCreateIfAbsentHttpTest();
    try {
      fixture.start();
      final ModelText acknowledged = fixture.text("FINISHED");
      final String full = fixture.codec.serialize(ModelText.class, acknowledged).toSource();
      final String[] malformed = malformedTimes(fixture.codec, acknowledged, full);
      for (final int status : new int[]{200, 201}) {
        fixture.status = status;
        for (final String body : malformed) {
          fixture.response = body;
          final Throwable failure = fixture.failure(fixture.text("ACTIVE"));
          assertTrue("Expected malformed numeric scalar: " + failure, causedByMalformedScalar(failure));
          assertFalse(failure instanceof ModelNotFoundException);
        }
      }
      final int failedRequests = 2 * malformed.length;
      assertEquals("No ordinary-persist fallback or extra request", failedRequests, fixture.requests.get());
      for (final int status : new int[]{200, 201}) {
        fixture.status = status;
        fixture.response = full;
        final ModelCreateResult<ModelText> result = ModelCreateIfAbsentHttpTest.await(
            callback -> fixture.client.createIfAbsent(fixture.text("ACTIVE"), callback));
        assertEquals(status == 201, result.isCreated());
        assertEquals("FINISHED", result.getModel().getText());
        assertEquals(200.0, result.getModel().getTime(), 0.0);
      }
      assertEquals(failedRequests + 2, fixture.requests.get());
    } finally {
      fixture.stop();
    }
  }

  private static String[] malformedTimes(final ModelServiceJre service, final ModelText model, final String full) {
    final String time = service.primitiveSerializer().serializeDouble(model.getTime());
    assertTrue("Fixture requires a multi-character final time scalar", time.length() > 1);
    assertTrue("Fixture must truncate time, not another field", full.endsWith(time));
    final String withoutTime = full.substring(0, full.length() - time.length());
    return new String[]{withoutTime, full.substring(0, full.length() - 1), withoutTime + " ", withoutTime + "\n"};
  }

  private static boolean causedByMalformedScalar(final Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof IllegalArgumentException
          && current.getMessage() != null && (current.getMessage().startsWith("Unterminated encoded ")
          || current.getMessage().startsWith("Invalid encoded "))) return true;
    }
    return false;
  }

  private static final class InlineService extends ModelServiceJre {
    int ordinaryWrites;
    @Override protected void dispatchFileOperation(final Runnable operation) { operation.run(); }
    @Override protected <M extends Model> void doPersist(final String type, final M model,
        final SuccessHandler<M> callback) {
      ordinaryWrites++;
      super.doPersist(type, model, callback);
    }
  }

  private static final class Result<M extends Model> implements SuccessHandler<ModelCreateResult<M>>, ErrorHandler<Throwable> {
    ModelCreateResult<M> value;
    Throwable error;
    int calls;
    @Override public void onSuccess(final ModelCreateResult<M> result) { value = result; calls++; }
    @Override public void onError(final Throwable failure) { error = failure; calls++; }
  }

  private static void corruptExistingFilesFailWithoutReplacement() throws Exception {
    final Path root = Files.createTempDirectory("xapi-scalar-file-");
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      final InlineService service = new InlineService();
      service.attachTransactionStore(store);
      final ModelText existing = service.create(ModelText.class);
      existing.setKey(service.newKey("scalar", existing.getType(), "existing"));
      existing.setText("FINISHED"); existing.setTime(200);
      final String full = service.serialize(ModelText.class, existing).toSource();
      final Path file = root.resolve("scalar").resolve(existing.getType()).resolve("existing");
      Files.createDirectories(file.getParent());
      final ModelText candidate = service.create(ModelText.class);
      candidate.setKey(service.keyFromString(service.keyToString(existing.getKey())));
      candidate.setText("ACTIVE"); candidate.setTime(100);
      for (final String malformed : malformedTimes(service, existing, full)) {
        final byte[] corrupt = malformed.getBytes(StandardCharsets.UTF_8);
        Files.write(file, corrupt);
        final Result<ModelText> failed = new Result<>();
        service.createIfAbsent(candidate, failed);
        assertEquals(1, failed.calls); assertNull(failed.value);
        assertTrue("Corrupt existing scalar must fail", causedByMalformedScalar(failed.error));
        assertFalse(failed.error instanceof ModelNotFoundException);
        assertArrayEquals("Corruption must not be replaced by the candidate", corrupt, Files.readAllBytes(file));

        final byte[] canonical = full.getBytes(StandardCharsets.UTF_8);
        Files.write(file, canonical);
        final Result<ModelText> valid = new Result<>();
        service.createIfAbsent(candidate, valid);
        assertEquals(1, valid.calls); assertNull(valid.error); assertNotNull(valid.value);
        assertFalse(valid.value.isCreated());
        assertEquals("FINISHED", valid.value.getModel().getText());
        assertEquals(200.0, valid.value.getModel().getTime(), 0.0);
        assertArrayEquals(canonical, Files.readAllBytes(file));
      }
      candidate.setKey(service.newKey("scalar", candidate.getType(), "new"));
      final Result<ModelText> fresh = new Result<>();
      service.createIfAbsent(candidate, fresh);
      assertEquals(1, fresh.calls); assertNull(fresh.error); assertNotNull(fresh.value);
      assertTrue(fresh.value.isCreated());
      assertEquals(100.0, fresh.value.getModel().getTime(), 0.0);
      assertEquals("No ordinary-persist fallback", 0, service.ordinaryWrites);
    }
  }

  @xapi.annotation.model.IsModel(modelType = "listCount")
  public interface ListHolder extends Model {
    ModelList<ModelText> getItems();
    void setItems(ModelList<ModelText> items);
  }

  private static ModelModule listModule() {
    final ModelModule module = new ModelModule();
    module.setModuleName("list-count-fixture"); module.setUuid("list-count-fixture-v1");
    module.addManifest(ModelUtil.createManifest(ModelList.class));
    module.addManifest(ModelUtil.createManifest(ModelText.class));
    module.addManifest(ModelUtil.createManifest(ListHolder.class));
    return module;
  }

  private static void registerListModule(final ModelServiceJre service, final ModelModule module) {
    for (final ModelManifest manifest : module.getManifests()) {
      service.register(manifest.getModelType());
      service.registerManifest(manifest);
    }
  }

  private static ListHolder listHolder(final AbstractJreModelService service, final String id, final int size) {
    final ListHolder result = service.create(ListHolder.class);
    result.setKey(service.newKey("list-count", result.getType(), id));
    if (size >= 0) {
      final ModelList<ModelText> items = service.create(ModelList.class);
      items.setModelType(ModelText.class);
      for (int i = 0; i < size; i++) {
        final ModelText child = service.create(ModelText.class);
        child.setKey(service.newKey("list-count", child.getType(), "child-" + i));
        child.setText("child-" + i); child.setTime(200);
        items.add(child);
      }
      result.setItems(items);
    }
    return result;
  }

  private static void assertListSize(final ListHolder result, final int size) {
    if (size < 0) {
      assertNull(result.getItems());
    } else {
      assertNotNull(result.getItems());
      assertEquals(size, result.getItems().size());
      if (size > 0) assertEquals("child-0", result.getItems().iterator().next().getText());
    }
  }

  private static String[] negativeListCounts(final ModelServiceJre service, final ListHolder empty) {
    final String full = service.serialize(ListHolder.class, empty).toSource();
    final String zero = service.primitiveSerializer().serializeInt(0);
    final String header = zero + service.primitiveSerializer().serializeClass(ModelText.class) + zero;
    assertTrue("Fixture must end in the nonnull empty ModelList header and count", full.endsWith(header));
    final String prefix = full.substring(0, full.length() - zero.length());
    return new String[]{prefix + service.primitiveSerializer().serializeInt(-1),
        prefix + service.primitiveSerializer().serializeInt(-2),
        prefix + service.primitiveSerializer().serializeInt(Integer.MIN_VALUE)};
  }

  private static boolean causedByInvalidListCount(final Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof IllegalArgumentException && current.getMessage() != null
          && current.getMessage().startsWith("Invalid encoded ModelList count:")) return true;
    }
    return false;
  }

  private static void negativeListCountsFailHttpAcknowledgmentsWithoutFallback() throws Exception {
    final ModelCreateIfAbsentHttpTest fixture = new ModelCreateIfAbsentHttpTest();
    try {
      fixture.start();
      final ModelModule module = listModule();
      fixture.codec = new ModelServiceJre(); registerListModule(fixture.codec, module);
      fixture.client = new ModelServiceHttp("http://127.0.0.1:" + fixture.server.getAddress().getPort() + "/model",
          ModelSerializerDefault.deserializeModule(ModelSerializerDefault.serialize(module)), 2000);
      final ListHolder candidate = listHolder(fixture.client, "existing", 0);
      final String[] malformed = negativeListCounts(fixture.codec, listHolder(fixture.codec, "existing", 0));
      final List<AtomicInteger> failureCallbacks = new ArrayList<>();
      for (final int status : new int[]{200, 201}) {
        fixture.status = status;
        for (final String body : malformed) {
          fixture.response = body;
          final AtomicInteger callbacks = new AtomicInteger(); failureCallbacks.add(callbacks);
          final CompletableFuture<ModelCreateResult<ListHolder>> future = new CompletableFuture<>();
          fixture.client.createIfAbsent(candidate, SuccessHandler.handler(
              result -> { callbacks.incrementAndGet(); future.complete(result); },
              error -> { callbacks.incrementAndGet(); future.completeExceptionally(error); }));
          try {
            future.get(5, TimeUnit.SECONDS);
            fail("Negative ModelList count was accepted as an HTTP acknowledgment");
          } catch (final ExecutionException failure) {
            assertTrue("Expected malformed ModelList count: " + failure, causedByInvalidListCount(failure));
            assertFalse(failure.getCause() instanceof ModelNotFoundException);
          }
          assertEquals("One terminal callback for each malformed acknowledgment", 1, callbacks.get());
        }
      }
      final int failedRequests = 2 * malformed.length;
      assertEquals("No ordinary-persist fallback or extra request", failedRequests, fixture.requests.get());
      for (final int status : new int[]{200, 201}) {
        fixture.status = status;
        for (final int size : new int[]{-1, 0, 1}) {
          fixture.response = fixture.codec.serialize(ListHolder.class, listHolder(fixture.codec, "existing", size)).toSource();
          final ModelCreateResult<ListHolder> result = ModelCreateIfAbsentHttpTest.await(
              callback -> fixture.client.createIfAbsent(candidate, callback));
          assertEquals(status == 201, result.isCreated());
          assertListSize(result.getModel(), size);
        }
      }
      assertListSize(candidate, 0);
      for (final AtomicInteger callbacks : failureCallbacks) assertEquals(1, callbacks.get());
      assertEquals(failedRequests + 6, fixture.requests.get());
    } finally {
      fixture.stop();
    }
  }

  private static void negativeListCountsFailExistingFilesWithoutReplacement() throws Exception {
    final Path root = Files.createTempDirectory("xapi-list-count-file-");
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      final InlineService service = new InlineService();
      registerListModule(service, listModule()); service.attachTransactionStore(store);
      final ListHolder existing = listHolder(service, "existing", 0);
      final ListHolder candidate = listHolder(service, "existing", 0);
      final Path file = root.resolve("list-count").resolve(existing.getType()).resolve("existing");
      Files.createDirectories(file.getParent());
      for (final String malformed : negativeListCounts(service, existing)) {
        final byte[] corrupt = malformed.getBytes(StandardCharsets.UTF_8);
        Files.write(file, corrupt);
        final Result<ListHolder> failed = new Result<>();
        service.createIfAbsent(candidate, failed);
        assertEquals(1, failed.calls); assertNull(failed.value);
        assertTrue("Corrupt existing list count must fail", causedByInvalidListCount(failed.error));
        assertFalse(failed.error instanceof ModelNotFoundException);
        assertArrayEquals("Corrupt list must not be replaced by the candidate", corrupt, Files.readAllBytes(file));
      }
      for (final int size : new int[]{-1, 0, 1}) {
        final byte[] canonical = service.serialize(ListHolder.class, listHolder(service, "existing", size))
            .toSource().getBytes(StandardCharsets.UTF_8);
        Files.write(file, canonical);
        final Result<ListHolder> valid = new Result<>();
        service.createIfAbsent(candidate, valid);
        assertEquals(1, valid.calls); assertNull(valid.error); assertNotNull(valid.value);
        assertFalse(valid.value.isCreated()); assertListSize(valid.value.getModel(), size);
        assertArrayEquals(canonical, Files.readAllBytes(file));
      }
      final Result<ListHolder> fresh = new Result<>();
      service.createIfAbsent(listHolder(service, "new", 1), fresh);
      assertEquals(1, fresh.calls); assertNull(fresh.error); assertNotNull(fresh.value);
      assertTrue(fresh.value.isCreated()); assertListSize(fresh.value.getModel(), 1);
      assertListSize(candidate, 0);
      assertEquals("No ordinary-persist fallback", 0, service.ordinaryWrites);
    }
  }

  private static final class ArrayFrameCodec extends ModelSerializerDefault<Model> {
    final PrimitiveSerializerDefault primitives = new PrimitiveSerializerDefault();
    final ModelServiceJre service = new ModelServiceJre();

    Object decode(final Class<?> type, final String wire) {
      final StringCharIterator input = new StringCharIterator(wire);
      final ModelDeserializationContext context = new ModelDeserializationContext(null, service, null);
      final Object result = readObject(type, "payload", input, primitives, context);
      assertFalse("Array decoder left trailing input", input.hasNext());
      return result;
    }
  }

  public enum ArrayEnum { VALUE }

  private static void hugeArrayHeadersRejectMissingPayloads() {
    final ArrayFrameCodec codec = new ArrayFrameCodec();
    final String huge = codec.primitives.serializeInt(Integer.MAX_VALUE);
    assertTrue("The oversized declaration must remain a tiny input", huge.length() <= 7);
    for (final String payload : new String[]{"", codec.primitives.serializeInt(1)}) {
      expectInvalidArrayPayload("Invalid encoded array length:", () -> codec.decode(byte[].class, huge + payload));
      expectInvalidArrayPayload("Invalid encoded array length:", () -> codec.decode(long[].class, huge + payload));
    }
    for (final String payload : new String[]{"", codec.primitives.serializeString("one")}) {
      expectInvalidArrayPayload("Invalid encoded array length:", () -> codec.decode(String[].class, huge + payload));
    }
    for (final String mask : new String[]{"", codec.primitives.serializeInt(0)}) {
      expectInvalidArrayPayload("Invalid encoded boolean array:", () ->
          codec.primitives.deserializeBooleanArray(new StringCharIterator(huge + mask)));
      expectInvalidArrayPayload("Invalid encoded boolean array:", () ->
          codec.decode(boolean[].class, huge + huge + mask));
    }
    final String enumType = codec.primitives.serializeClass(ArrayEnum.class);
    for (final String item : new String[]{"", codec.primitives.serializeInt(0)}) {
      expectInvalidArrayPayload("Invalid encoded enum set count:", () -> codec.decode(EnumSet.class, huge + enumType + item));
    }
  }

  private static void expectInvalidArrayPayload(final String prefix, final Runnable decode) {
    try {
      decode.run();
      fail("Oversized declaration with missing payload was accepted");
    } catch (final IllegalArgumentException expected) {
      assertTrue("Expected payload validation failure: " + expected,
          expected.getMessage() != null && expected.getMessage().startsWith(prefix));
    }
  }

  @xapi.annotation.model.IsModel(modelType = "arrayFrame")
  public interface ArrayHolder extends Model {
    byte[] getPayload();
    void setPayload(byte[] payload);
  }

  private static ModelModule arrayModule() {
    final ModelModule module = new ModelModule();
    module.setModuleName("array-frame-fixture"); module.setUuid("array-frame-fixture-v1");
    module.addManifest(ModelUtil.createManifest(ArrayHolder.class));
    return module;
  }

  private static ArrayHolder arrayHolder(final AbstractJreModelService service, final String id, final byte[] payload) {
    final ArrayHolder result = service.create(ArrayHolder.class);
    result.setKey(service.newKey("array-frame", result.getType(), id));
    result.setPayload(payload);
    return result;
  }

  private static String hugeArrayImage(final ModelServiceJre service, final ArrayHolder empty) {
    assertArrayEquals(new String[]{"payload"}, service.getOrMakeModelManifest(ArrayHolder.class).getPropertyNames());
    final String full = service.serialize(ArrayHolder.class, empty).toSource();
    final String zero = service.primitiveSerializer().serializeInt(0);
    assertTrue("Fixture must end in the sole empty-array length", full.endsWith(zero));
    return full.substring(0, full.length() - zero.length()) + service.primitiveSerializer().serializeInt(Integer.MAX_VALUE);
  }

  private static boolean causedByInvalidArrayLength(final Throwable failure) {
    for (Throwable current = failure; current != null; current = current.getCause()) {
      if (current instanceof IllegalArgumentException && current.getMessage() != null
          && current.getMessage().startsWith("Invalid encoded array length:")) return true;
    }
    return false;
  }

  private static void hugeArrayAcknowledgmentsFailWithoutFallback() throws Exception {
    final ModelCreateIfAbsentHttpTest fixture = new ModelCreateIfAbsentHttpTest();
    try {
      fixture.start();
      final ModelModule module = arrayModule();
      fixture.codec = new ModelServiceJre(); registerListModule(fixture.codec, module);
      fixture.client = new ModelServiceHttp("http://127.0.0.1:" + fixture.server.getAddress().getPort() + "/model",
          ModelSerializerDefault.deserializeModule(ModelSerializerDefault.serialize(module)), 2000);
      final ArrayHolder candidate = arrayHolder(fixture.client, "existing", new byte[0]);
      fixture.response = hugeArrayImage(fixture.codec, arrayHolder(fixture.codec, "existing", new byte[0]));
      final List<AtomicInteger> failureCallbacks = new ArrayList<>();
      for (final int status : new int[]{200, 201}) {
        fixture.status = status;
        final AtomicInteger callbacks = new AtomicInteger(); failureCallbacks.add(callbacks);
        final CompletableFuture<ModelCreateResult<ArrayHolder>> future = new CompletableFuture<>();
        fixture.client.createIfAbsent(candidate, SuccessHandler.handler(
            result -> { callbacks.incrementAndGet(); future.complete(result); },
            error -> { callbacks.incrementAndGet(); future.completeExceptionally(error); }));
        try {
          future.get(5, TimeUnit.SECONDS);
          fail("Huge array length was accepted as an HTTP acknowledgment");
        } catch (final ExecutionException failure) {
          assertTrue("Expected array payload validation: " + failure, causedByInvalidArrayLength(failure));
          assertFalse(failure.getCause() instanceof ModelNotFoundException);
        }
        assertEquals("One terminal callback for each malformed acknowledgment", 1, callbacks.get());
      }
      assertEquals("No ordinary-persist fallback or extra request", 2, fixture.requests.get());
      for (final int status : new int[]{200, 201}) {
        fixture.status = status;
        for (final byte[] payload : new byte[][]{null, new byte[0], new byte[]{0, 1, -1, Byte.MIN_VALUE, Byte.MAX_VALUE}}) {
          fixture.response = fixture.codec.serialize(ArrayHolder.class, arrayHolder(fixture.codec, "existing", payload)).toSource();
          final ModelCreateResult<ArrayHolder> result = ModelCreateIfAbsentHttpTest.await(
              callback -> fixture.client.createIfAbsent(candidate, callback));
          assertEquals(status == 201, result.isCreated());
          // Model proxies materialize an empty default for a null array property.
          assertArrayEquals(payload == null ? new byte[0] : payload, result.getModel().getPayload());
        }
      }
      assertArrayEquals(new byte[0], candidate.getPayload());
      for (final AtomicInteger callbacks : failureCallbacks) assertEquals(1, callbacks.get());
      assertEquals(8, fixture.requests.get());
    } finally {
      fixture.stop();
    }
  }

  private static void hugeArrayExistingFilesFailWithoutReplacement() throws Exception {
    final Path root = Files.createTempDirectory("xapi-array-frame-file-");
    try (FileModelTransactionStore store = new FileModelTransactionStore(root)) {
      final InlineService service = new InlineService();
      registerListModule(service, arrayModule()); service.attachTransactionStore(store);
      final ArrayHolder existing = arrayHolder(service, "existing", new byte[0]);
      final ArrayHolder candidate = arrayHolder(service, "existing", new byte[0]);
      final Path file = root.resolve("array-frame").resolve(existing.getType()).resolve("existing");
      Files.createDirectories(file.getParent());
      final byte[] corrupt = hugeArrayImage(service, existing).getBytes(StandardCharsets.UTF_8);
      Files.write(file, corrupt);
      final Result<ArrayHolder> failed = new Result<>();
      service.createIfAbsent(candidate, failed);
      assertEquals(1, failed.calls); assertNull(failed.value);
      assertTrue("Corrupt existing array must fail validation", causedByInvalidArrayLength(failed.error));
      assertFalse(failed.error instanceof ModelNotFoundException);
      assertArrayEquals("Corrupt array must not be replaced by the candidate", corrupt, Files.readAllBytes(file));
      for (final byte[] payload : new byte[][]{null, new byte[0], new byte[]{0, 1, -1, Byte.MIN_VALUE, Byte.MAX_VALUE}}) {
        final byte[] canonical = service.serialize(ArrayHolder.class, arrayHolder(service, "existing", payload))
            .toSource().getBytes(StandardCharsets.UTF_8);
        Files.write(file, canonical);
        final Result<ArrayHolder> valid = new Result<>();
        service.createIfAbsent(candidate, valid);
        assertEquals(1, valid.calls); assertNull(valid.error); assertNotNull(valid.value);
        assertFalse(valid.value.isCreated());
        // Model proxies materialize an empty default for a null array property.
        assertArrayEquals(payload == null ? new byte[0] : payload, valid.value.getModel().getPayload());
        assertArrayEquals(canonical, Files.readAllBytes(file));
      }
      final Result<ArrayHolder> fresh = new Result<>();
      service.createIfAbsent(arrayHolder(service, "new", new byte[]{7}), fresh);
      assertEquals(1, fresh.calls); assertNull(fresh.error); assertNotNull(fresh.value);
      assertTrue(fresh.value.isCreated()); assertArrayEquals(new byte[]{7}, fresh.value.getModel().getPayload());
      assertArrayEquals(new byte[0], candidate.getPayload());
      assertEquals("No ordinary-persist fallback", 0, service.ordinaryWrites);
    }
  }
}
