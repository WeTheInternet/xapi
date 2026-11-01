package xapi.jre.model;

import com.sun.net.httpserver.*;
import org.junit.*;
import xapi.model.api.*;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelUtil;
import xapi.model.tools.ModelSerializerDefault;
import xapi.util.api.SuccessHandler;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import static org.junit.Assert.*;

/** Real loopback transport, strict acknowledgments, no server deployment or fallback writes. */
public class ModelCreateIfAbsentHttpTest {
  HttpServer server; ModelServiceHttp client; ModelServiceJre codec;
  volatile int status = 201; volatile String response; volatile String received;
  volatile CountDownLatch release; final CountDownLatch entered = new CountDownLatch(1);
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
      byte[] body = (response == null ? received : response).getBytes(StandardCharsets.UTF_8);
      if (!"/model/create".equals(exchange.getRequestURI().getPath())) { exchange.sendResponseHeaders(500, -1); exchange.close(); return; }
      exchange.sendResponseHeaders(status, body.length); exchange.getResponseBody().write(body); exchange.close();
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

}
