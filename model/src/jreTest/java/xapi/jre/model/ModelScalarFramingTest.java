package xapi.jre.model;

import org.junit.Test;
import xapi.model.api.Model;
import xapi.model.api.ModelCreateResult;
import xapi.model.api.ModelNotFoundException;
import xapi.model.content.ModelText;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/** Invalid numeric fields must fail even when ordinary JVM assertions are disabled. */
public class ModelScalarFramingTest {
  @Test
  public void rejectsTruncatedScalarsAndAcknowledgmentsWithoutAssertions() throws Exception {
    final Path log = Files.createTempFile("xapi-scalar-framing-", ".log");
    final List<String> command = new ArrayList<>(Arrays.asList(
        System.getProperty("java.home") + File.separator + "bin" + File.separator + "java",
        "-da", "-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir")));
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
        assertFalse("Probe must run with -da", ModelScalarFramingTest.class.desiredAssertionStatus());
        assertFalse("Numeric decoder must run with -da", PrimitiveSerializerDefault.class.desiredAssertionStatus());
        assertFalse("Acknowledgment decoder must run with -da", ModelCreateRequest.class.desiredAssertionStatus());
        validNumericEncodingsRemainUnchanged();
        missingAndUnterminatedScalarsFail();
        invalidNumericAlphabetBoundsAndSentinelsFail();
        malformedHttpAcknowledgmentsFailWithoutFallback();
        corruptExistingFilesFailWithoutReplacement();
        System.out.println("Assertion-disabled scalar alphabet/bounds, HTTP 200/201 and existing-file checks passed.");
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

  private static final class Result implements SuccessHandler<ModelCreateResult<ModelText>>, ErrorHandler<Throwable> {
    ModelCreateResult<ModelText> value;
    Throwable error;
    int calls;
    @Override public void onSuccess(final ModelCreateResult<ModelText> result) { value = result; calls++; }
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
        final Result failed = new Result();
        service.createIfAbsent(candidate, failed);
        assertEquals(1, failed.calls); assertNull(failed.value);
        assertTrue("Corrupt existing scalar must fail", causedByMalformedScalar(failed.error));
        assertFalse(failed.error instanceof ModelNotFoundException);
        assertArrayEquals("Corruption must not be replaced by the candidate", corrupt, Files.readAllBytes(file));

        final byte[] canonical = full.getBytes(StandardCharsets.UTF_8);
        Files.write(file, canonical);
        final Result valid = new Result();
        service.createIfAbsent(candidate, valid);
        assertEquals(1, valid.calls); assertNull(valid.error); assertNotNull(valid.value);
        assertFalse(valid.value.isCreated());
        assertEquals("FINISHED", valid.value.getModel().getText());
        assertEquals(200.0, valid.value.getModel().getTime(), 0.0);
        assertArrayEquals(canonical, Files.readAllBytes(file));
      }
      candidate.setKey(service.newKey("scalar", candidate.getType(), "new"));
      final Result fresh = new Result();
      service.createIfAbsent(candidate, fresh);
      assertEquals(1, fresh.calls); assertNull(fresh.error); assertNotNull(fresh.value);
      assertTrue(fresh.value.isCreated());
      assertEquals(100.0, fresh.value.getModel().getTime(), 0.0);
      assertEquals("No ordinary-persist fallback", 0, service.ordinaryWrites);
    }
  }
}
