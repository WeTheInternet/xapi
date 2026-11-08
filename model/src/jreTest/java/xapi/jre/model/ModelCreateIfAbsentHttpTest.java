package xapi.jre.model;

import com.sun.net.httpserver.*;
import org.junit.*;
import xapi.model.api.*;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelUtil;
import xapi.model.tools.ModelSerializerDefault;
import xapi.model.service.ModelServiceWithCreateIfAbsent;
import xapi.util.api.SuccessHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import static org.junit.Assert.*;

/** Real loopback transport, strict acknowledgments, no server deployment or fallback writes. */
public class ModelCreateIfAbsentHttpTest {
  HttpServer server; ModelServiceHttp client; ModelServiceJre codec;
  volatile int status = 201; volatile String response; volatile String received; volatile byte[] rawResponse;
  volatile CountDownLatch release; final CountDownLatch entered = new CountDownLatch(1);
  volatile ResponseProducer responseProducer; volatile boolean chunkedResponse;
  volatile String expectedPath = "/model/create";
  interface ResponseProducer { void write(HttpExchange exchange) throws IOException; }
  final AtomicInteger requests = new AtomicInteger();
  @Before public void start() throws Exception {
    ModelModule authored = new ModelModule(); authored.setModuleName("create-fixture"); authored.setUuid("create-fixture-v1");
    authored.addManifest(ModelUtil.createManifest(ModelText.class));
    final String artifact = ModelSerializerDefault.serialize(authored);
    codec = new ModelServiceJre(); codec.register(ModelText.class);
    for (ModelManifest manifest : authored.getManifests()) codec.registerManifest(manifest);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/model/", exchange -> {
      requests.incrementAndGet(); received = new String(read(exchange), StandardCharsets.UTF_8); entered.countDown();
      try { if (release != null && !release.await(5, TimeUnit.SECONDS)) throw new AssertionError("response release timeout"); }
      catch (InterruptedException failure) { Thread.currentThread().interrupt(); exchange.close(); return; }
      if (!expectedPath.equals(exchange.getRequestURI().getPath())) { exchange.sendResponseHeaders(500, -1); exchange.close(); return; }
      final ResponseProducer producer = responseProducer;
      if (producer != null) {
        try { producer.write(exchange); } finally { exchange.close(); }
        return;
      }
      byte[] body = rawResponse == null ? (response == null ? received : response).getBytes(StandardCharsets.UTF_8) : rawResponse;
      exchange.sendResponseHeaders(status, chunkedResponse ? 0 : body.length); exchange.getResponseBody().write(body); exchange.close();
    }); server.start();
    client = new ModelServiceHttp("http://127.0.0.1:" + server.getAddress().getPort() + "/model",
        ModelSerializerDefault.deserializeModule(artifact), 2000);
  }
  static byte[] read(HttpExchange e) throws java.io.IOException { java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(); byte[] b = new byte[4096]; int n; while ((n=e.getRequestBody().read(b)) != -1) out.write(b,0,n); return out.toByteArray(); }
  @After public void stop() { if (release != null) release.countDown(); if (server != null) server.stop(0); }
  ModelText text(String value) { ModelText m=client.create(ModelText.class); m.setKey(client.newKey("child-a", "day", "2026-11-01").setKeyType(7).getChild(m.getType(), "brush").setKeyType(73)); m.setText(value); m.setTime("FINISHED".equals(value)?200:100); return m; }
  static <T> T await(Consumer<SuccessHandler<T>> action) throws Exception { CompletableFuture<T> f=new CompletableFuture<>(); action.accept(SuccessHandler.handler(f::complete,f::completeExceptionally)); return f.get(5,TimeUnit.SECONDS); }
  Throwable failure(ModelText m) throws Exception { try { ModelCreateIfAbsentHttpTest.<ModelCreateResult<ModelText>>await(cb -> client.createIfAbsent(m,cb)); throw new AssertionError("expected failure"); } catch (ExecutionException e) { return e.getCause(); } }
  @Test public void strictCreatedAndExistingAcknowledgmentsPreserveFinishedValue() throws Exception {
    ModelCreateResult<ModelText> created=await(cb->client.createIfAbsent(text("ACTIVE"),cb)); assertTrue(created.isCreated());
    status=200; response=codec.serialize(ModelText.class,text("FINISHED")).toSource();
    ModelCreateResult<ModelText> existing=await(cb->client.createIfAbsent(text("ACTIVE"),cb));
    assertFalse(existing.isCreated()); assertEquals("FINISHED",existing.getModel().getText()); assertEquals(200,existing.getModel().getTime(),0); assertEquals(2,requests.get());
  }
  @Test public void failedAndUnexpectedStatusesNeverFallbackToPersistOrMissing() throws Exception {
    for (int code : new int[]{404,500,503,202}) { status=code; assertNotNull(failure(text("ACTIVE"))); }
    assertEquals(4,requests.get());
  }
  @Test public void malformedRawUtf8AcknowledgmentsFailBeforeReplacementDecoding() throws Exception {
    final String wire = codec.serialize(ModelText.class, text("markerX")).toSource();
    final int marker = wire.indexOf("markerX"); assertTrue(marker >= 0);
    final byte[] bytes = wire.getBytes(StandardCharsets.UTF_8);
    final int byteOffset = wire.substring(0, marker).getBytes(StandardCharsets.UTF_8).length;
    bytes[byteOffset + "markerX".length() - 1] = (byte) 0xff;
    rawResponse = bytes;
    for (int code : new int[]{201, 200}) {
      status = code;
      final Throwable error = failure(text("ACTIVE"));
      assertTrue("Expected raw UTF-8 decoding failure: " + error, error instanceof CharacterCodingException);
    }
    assertEquals(2, requests.get());
  }
  @Test public void validUtf8AcknowledgmentsPreserveUnicodeReplacementCharacterAndLineEndings() throws Exception {
    final String value = "caf\u00e9 \u65e5\u672c\u8a9e \ud83c\udf31 \ufffd\r\nsecond\n";
    rawResponse = codec.serialize(ModelText.class, text(value)).toSource().getBytes(StandardCharsets.UTF_8);
    for (int code : new int[]{201, 200}) {
      status = code;
      final ModelCreateResult<ModelText> result = await(cb -> client.createIfAbsent(text("ACTIVE"), cb));
      assertEquals(code == 201, result.isCreated()); assertEquals(value, result.getModel().getText());
    }
    assertEquals(2, requests.get());
  }
  @Test public void malformedOrDifferentFullIdentityFailsWithoutFallback() throws Exception {
    response="malformed"; assertNotNull(failure(text("ACTIVE")));
    ModelText wrong=text("FINISHED"); wrong.getKey().getParent().setId("2026-11-02"); response=codec.serialize(ModelText.class,wrong).toSource();
    assertNotNull(failure(text("ACTIVE"))); assertEquals(2,requests.get());
  }
  @Test public void capturesCallerIdentityAndImageWhileTransportWaits() throws Exception {
    release=new CountDownLatch(1); ModelText m=text("ACTIVE"); String expected=client.keyToString(m.getKey());
    CompletableFuture<ModelCreateResult<ModelText>> future=new CompletableFuture<>(); client.createIfAbsent(m,SuccessHandler.handler(future::complete,future::completeExceptionally));
    assertTrue(entered.await(5,TimeUnit.SECONDS)); m.setText("MUTATED"); m.getKey().setId("different"); release.countDown();
    ModelText actual=future.get(5,TimeUnit.SECONDS).getModel(); assertEquals("ACTIVE",actual.getText()); assertEquals(expected,client.keyToString(actual.getKey())); assertEquals(1,requests.get());
  }
  @xapi.annotation.model.IsModel(modelType="createDirectional")
  public interface Directional extends Model {
    @xapi.annotation.model.Serializable(clientToServer=@xapi.annotation.model.ClientToServer(enabled=true), serverToClient=@xapi.annotation.model.ServerToClient(enabled=false))
    String getUpload(); void setUpload(String value);
  }
  @Test public void unequalDirectionalFieldCountsValidateCandidateAsReceivingPeer() throws Exception {
    ModelModule authored=new ModelModule(); authored.setModuleName("create-asymmetric"); authored.setUuid("create-asymmetric-v1"); authored.addManifest(ModelUtil.createManifest(Directional.class));
    ModelServiceHttp local=new ModelServiceHttp("http://127.0.0.1:"+server.getAddress().getPort()+"/model",ModelSerializerDefault.deserializeModule(ModelSerializerDefault.serialize(authored)),2000);
    ModelServiceJre remote=new ModelServiceJre(); remote.register(Directional.class); for(ModelManifest m:authored.getManifests()) remote.registerManifest(m);
    Directional input=local.create(Directional.class); input.setKey(local.newKey("child-a",input.getType(),"routine")); input.setUpload("outgoing");
    Directional reply=remote.create(Directional.class); reply.setKey(input.getKey()); response=remote.serialize(Directional.class,reply).toSource();
    ModelCreateResult<Directional> result=await(cb->local.createIfAbsent(input,cb)); assertTrue(result.isCreated()); assertNull(result.getModel().getUpload());
    Directional uploaded=remote.deserialize(Directional.class,new xapi.source.lex.StringCharIterator(received)); assertEquals("outgoing",uploaded.getUpload()); assertEquals(1,requests.get());
  }
  @Test public void conditionalCreateUsesOppositePeerForCandidatePreflightAndClientForAcknowledgment() throws Exception {
    ModelModule authored=new ModelModule(); authored.setModuleName("create-directions"); authored.setUuid("create-directions-v1");
    authored.addManifest(ModelUtil.createManifest(xapi.model.test.api.ModelTestDirections.class));
    String artifact=ModelSerializerDefault.serialize(authored);
    ModelServiceJre remote=new ModelServiceJre(); remote.register(xapi.model.test.api.ModelTestDirections.class);
    for(ModelManifest m:authored.getManifests()) remote.registerManifest(m);
    ModelServiceHttp local=new ModelServiceHttp("http://127.0.0.1:"+server.getAddress().getPort()+"/model",ModelSerializerDefault.deserializeModule(artifact),2000);
    xapi.model.test.api.ModelTestDirections input=local.create(xapi.model.test.api.ModelTestDirections.class);
    input.setKey(local.newKey("child-a",input.getType(),"routine")); input.setClientToServer("outgoing"); input.setServerToClient("private outgoing"); input.setBothEnabled("both");
    xapi.model.test.api.ModelTestDirections reply=remote.create(xapi.model.test.api.ModelTestDirections.class); reply.setKey(input.getKey()); reply.setServerToClient("incoming"); reply.setBothEnabled("ack");
    response=remote.serialize(xapi.model.test.api.ModelTestDirections.class,reply).toSource();
    ModelCreateResult<xapi.model.test.api.ModelTestDirections> result=await(cb->local.createIfAbsent(input,cb));
    assertTrue(result.isCreated()); assertEquals("incoming",result.getModel().getServerToClient()); assertNull(result.getModel().getClientToServer()); assertEquals("ack",result.getModel().getBothEnabled());
    xapi.model.test.api.ModelTestDirections uploaded=remote.deserialize(xapi.model.test.api.ModelTestDirections.class,new xapi.source.lex.StringCharIterator(received));
    assertEquals("outgoing",uploaded.getClientToServer()); assertNull(uploaded.getServerToClient()); assertEquals("both",uploaded.getBothEnabled()); assertEquals(1,requests.get());
  }

  @Test public void createRetainsKeyOnlyReferencesAndInlineChildrenWithoutImplicitHttpIo() throws Exception {
    ModelModule authored=new ModelModule(); authored.setModuleName("create-references"); authored.setUuid("create-references-v1");
    authored.addManifest(ModelUtil.createManifest(ModelList.class)); authored.addManifest(ModelUtil.createManifest(xapi.model.content.ModelContent.class)); authored.addManifest(ModelUtil.createManifest(xapi.model.user.ModelUser.class));
    ModelServiceHttp local=new ModelServiceHttp("http://127.0.0.1:"+server.getAddress().getPort()+"/model",ModelSerializerDefault.deserializeModule(ModelSerializerDefault.serialize(authored)),2000);
    xapi.model.content.ModelContent input=local.create(xapi.model.content.ModelContent.class); input.setKey(local.newKey("child-a",input.getType(),"routine"));
    xapi.model.user.ModelUser author=local.create(xapi.model.user.ModelUser.class); author.setKey(local.newKey("child-a",author.getType(),"parent")); input.setAuthor(author);
    xapi.model.content.ModelContent child=local.create(xapi.model.content.ModelContent.class); child.setKey(local.newKey("child-a",child.getType(),"inline")); child.setText("inline checklist data"); input.children().add(child);
    xapi.model.content.ModelContent related=local.create(xapi.model.content.ModelContent.class); related.setKey(local.newKey("child-a",related.getType(),"related")); input.related().add(related);
    ModelCreateResult<xapi.model.content.ModelContent> result=await(cb->local.createIfAbsent(input,cb));
    assertEquals("parent",result.getModel().getAuthor().getKey().getId()); assertEquals(1,result.getModel().getChildren().size());
    assertEquals("inline checklist data",result.getModel().getChildren().iterator().next().getText()); assertEquals(1,requests.get());
  }

  private static final class CountingResponse extends InputStream {
    private final int length;
    int readBytes;
    boolean closed;
    CountingResponse(final int length) { this.length = length; }
    @Override public int read() {
      if (readBytes == length) return -1;
      readBytes++; return 'x';
    }
    @Override public int read(final byte[] buffer, final int offset, final int count) {
      if (count == 0) return 0;
      if (readBytes == length) return -1;
      final int available = Math.min(count, length - readBytes);
      Arrays.fill(buffer, offset, offset + available, (byte) 'x');
      readBytes += available;
      return available;
    }
    @Override public void close() { closed = true; }
  }

  private static boolean isResponseLimit(final Throwable error) {
    for (Throwable current = error; current != null; current = current.getCause()) {
      if (current instanceof IOException
          && "Conditional-create response exceeds byte limit".equals(current.getMessage())) return true;
    }
    return false;
  }

  @Test public void conditionalResponseStreamBoundsBulkAndSingleByteReads() throws Exception {
    final int limit = ModelServiceWithCreateIfAbsent.MAX_MODEL_BYTES;
    final byte[] buffer = new byte[4096];
    final CountingResponse exact = new CountingResponse(limit);
    try (InputStream input = new ModelServiceHttp.ConditionalResponseInputStream(exact)) {
      int total = 0;
      while (total < limit - 1) {
        final int count = input.read(buffer, 0, Math.min(buffer.length, limit - 1 - total));
        assertTrue(count > 0); total += count;
      }
      assertEquals('x', input.read());
      assertEquals(0, input.read(buffer, 0, 0));
      assertEquals(-1, input.read());
      assertEquals(limit, exact.readBytes);
    }
    assertTrue(exact.closed);

    for (final boolean singleByte : new boolean[]{false, true}) {
      final CountingResponse oversized = new CountingResponse(limit + 64);
      try (InputStream input = new ModelServiceHttp.ConditionalResponseInputStream(oversized)) {
        if (singleByte) {
          int total = 0;
          while (total < limit) {
            final int count = input.read(buffer, 0, Math.min(buffer.length, limit - total));
            assertTrue(count > 0); total += count;
          }
        }
        try {
          if (singleByte) input.read();
          else while (input.read(buffer) != -1) {}
          fail("The first excess byte must fail before buffering the remaining response");
        } catch (final IOException expected) {
          assertTrue(isResponseLimit(expected));
        }
        assertEquals(limit + 1, oversized.readBytes);
        assertEquals(0, input.read(buffer, 0, 0));
        try { input.read(); fail("Overflow must remain terminal"); }
        catch (final IOException expected) { assertTrue(isResponseLimit(expected)); }
        assertEquals("A failed response must not read more bytes", limit + 1, oversized.readBytes);
      }
      assertTrue("Failure cleanup must close the underlying stream", oversized.closed);
    }
  }

  private static void writeRepeated(final OutputStream out, final byte[] chunk, final int count) throws IOException {
    int remaining = count;
    while (remaining > 0) {
      final int next = Math.min(remaining, chunk.length);
      out.write(chunk, 0, next); remaining -= next;
    }
  }

  @Test public void oversizedFixedAndChunkedResponsesFailBeforeTheirFiniteTail() throws Exception {
    final int limit = ModelServiceWithCreateIfAbsent.MAX_MODEL_BYTES;
    // status, chunked transfer, multibyte UTF-8
    final int[][] cases = {{200, 0, 0}, {201, 1, 0}, {200, 1, 1}, {201, 0, 1}, {404, 1, 1}};
    final List<AtomicInteger> callbackCounts = new ArrayList<>();
    final ModelText candidate = text("ACTIVE");
    for (final int[] scenario : cases) {
      final int code = scenario[0];
      final boolean chunked = scenario[1] != 0;
      final boolean unicode = scenario[2] != 0;
      final CountDownLatch tail = new CountDownLatch(1);
      final CountDownLatch producerDone = new CountDownLatch(1);
      responseProducer = exchange -> {
        final int planned = limit + 64;
        exchange.sendResponseHeaders(code, chunked ? 0 : planned);
        final byte[] chunk = new byte[4096];
        for (int i = 0; i < chunk.length; i++) {
          chunk[i] = unicode ? (byte) (i % 2 == 0 ? 0xc3 : 0xa9) : (byte) 'x';
        }
        try (OutputStream out = exchange.getResponseBody()) {
          final int prefix = limit + (unicode ? 2 : 1);
          writeRepeated(out, chunk, prefix);
          out.flush();
          try {
            if (!tail.await(5, TimeUnit.SECONDS)) throw new IOException("response tail release timeout");
          } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt(); throw new IOException(interrupted);
          }
          writeRepeated(out, chunk, planned - prefix);
        } catch (final IOException closedByClient) {
          // Early client closure is expected; the client must report the precise size failure below.
        } finally {
          producerDone.countDown();
        }
      };
      final AtomicInteger callbacks = new AtomicInteger(); callbackCounts.add(callbacks);
      final CompletableFuture<ModelCreateResult<ModelText>> future = new CompletableFuture<>();
      try {
        client.createIfAbsent(candidate, SuccessHandler.handler(
            result -> { callbacks.incrementAndGet(); future.complete(result); },
            error -> { callbacks.incrementAndGet(); future.completeExceptionally(error); }));
        try { future.get(5, TimeUnit.SECONDS); fail("Oversized response was accepted"); }
        catch (final ExecutionException expected) {
          assertTrue("Expected early byte-limit failure: " + expected, isResponseLimit(expected));
          assertFalse(expected.getCause() instanceof ModelNotFoundException);
        }
        assertEquals("Failure must arrive before the producer's remaining body is released", 1L, tail.getCount());
        assertEquals(1, callbacks.get());
      } finally {
        tail.countDown();
        assertTrue("Finite response producer must close", producerDone.await(5, TimeUnit.SECONDS));
        responseProducer = null;
      }
    }
    response = codec.serialize(ModelText.class, text("FINISHED")).toSource();
    final ModelCreateResult<ModelText> valid = await(callback -> client.createIfAbsent(candidate, callback));
    assertEquals("FINISHED", valid.getModel().getText());
    assertEquals("ACTIVE", candidate.getText());
    for (final AtomicInteger callbacks : callbackCounts) assertEquals(1, callbacks.get());
    assertEquals("No retry or ordinary-persist fallback", cases.length + 1, requests.get());
  }

  private String responseWithByteSize(final int targetBytes, final boolean unicode) {
    final int emptyBytes = codec.serialize(ModelText.class, text("")).toSource().getBytes(StandardCharsets.UTF_8).length;
    final int fixedBytes = emptyBytes - codec.primitiveSerializer().serializeInt(0).length();
    int characters = targetBytes - fixedBytes;
    int payloadBytes = 0;
    for (int attempt = 0; attempt < 8; attempt++) {
      payloadBytes = targetBytes - fixedBytes - codec.primitiveSerializer().serializeInt(characters).length();
      final int nextCharacters = unicode ? payloadBytes / 2 + payloadBytes % 2 : payloadBytes;
      if (nextCharacters == characters) break;
      characters = nextCharacters;
    }
    final char[] value = new char[characters];
    Arrays.fill(value, unicode ? '\u00e9' : 'a');
    if (unicode && payloadBytes % 2 != 0) value[value.length - 1] = 'a';
    final String expected = new String(value);
    rawResponse = codec.serialize(ModelText.class, text(expected)).toSource().getBytes(StandardCharsets.UTF_8);
    assertEquals("Fixture must use the exact UTF-8 byte size", targetBytes, rawResponse.length);
    return expected;
  }

  @Test public void exactByteLimitAcceptsAsciiAndMultibyteAcknowledgments() throws Exception {
    for (final boolean unicode : new boolean[]{false, true}) {
      status = unicode ? 200 : 201;
      chunkedResponse = unicode;
      final String expected = responseWithByteSize(ModelServiceWithCreateIfAbsent.MAX_MODEL_BYTES, unicode);
      final ModelCreateResult<ModelText> result = await(callback -> client.createIfAbsent(text("ACTIVE"), callback));
      assertEquals(status == 201, result.isCreated());
      assertEquals(expected, result.getModel().getText());
    }
    assertEquals(2, requests.get());
  }

  @Test public void ordinaryLoadRetainsItsExistingResponseSizePolicy() throws Exception {
    expectedPath = "/model/load";
    status = 200;
    final String expected = responseWithByteSize(ModelServiceWithCreateIfAbsent.MAX_MODEL_BYTES + 1, true);
    final ModelText result = await(callback -> client.load(ModelText.class, text("ACTIVE").getKey(), callback));
    assertEquals(expected, result.getText());
    assertEquals(1, requests.get());
  }

}
