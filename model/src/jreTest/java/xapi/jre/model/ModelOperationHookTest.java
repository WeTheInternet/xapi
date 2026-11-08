package xapi.jre.model;

import com.sun.net.httpserver.HttpServer;
import org.junit.Test;
import xapi.model.api.ModelModule;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelUtil;
import xapi.util.api.SuccessHandler;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * A domain adapter reuses the authorized HTTP transport, without accepting
 * arbitrary path segments or unregistered model types.
 */
public class ModelOperationHookTest {

  private static final class Client extends ModelServiceHttp {
    Client(final String endpoint, final ModelModule module) {
      super(endpoint, module, 2000);
    }

    void operation(final String path, final String type, final SuccessHandler<String> callback) {
      postModelOperation(path, type, "command", response -> {
        if (response.statusCode() != 200) throw new IllegalStateException("status");
        return response.body();
      }, callback, null);
    }
  }

  private static ModelModule module() {
    final ModelModule result = new ModelModule();
    result.setModuleName("hook-test");
    result.setUuid("hook-v1");
    result.addManifest(ModelUtil.createManifest(ModelText.class));
    return result;
  }

  private static void assertInvalid(final Client client, final String operation, final String type)
      throws Exception {
    final CompletableFuture<String> response = new CompletableFuture<>();
    client.operation(operation, type,
        SuccessHandler.handler(response::complete, response::completeExceptionally));
    try {
      response.get(1, TimeUnit.SECONDS);
      fail("Expected a rejected operation/type: " + operation + " / " + type);
    } catch (final ExecutionException expected) {
      assertTrue(expected.getCause().toString(),
          expected.getCause() instanceof IllegalArgumentException);
    }
  }

  @Test
  public void rejectsUnsafeOperationOrUnknownTypeBeforeTransport() throws Exception {
    final Client client = new Client("http://127.0.0.1:1/model", module());
    for (final String operation : new String[]{
        "../complete", "complete?x", "/complete", "complete/extra", "", "complete\n"
    }) {
      assertInvalid(client, operation, "text");
    }
    assertInvalid(client, "complete", "unregistered");
  }

  @Test
  public void literalDomainOperationUsesExistingModuleAndBodyTransport() throws Exception {
    final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    final AtomicInteger requests = new AtomicInteger();
    final AtomicReference<String> moduleHeader = new AtomicReference<>();
    final AtomicReference<String> modelTypeHeader = new AtomicReference<>();
    server.createContext("/model/complete", exchange -> {
      requests.incrementAndGet();
      moduleHeader.set(exchange.getRequestHeaders().getFirst("X-Gwt-Module"));
      modelTypeHeader.set(exchange.getRequestHeaders().getFirst("X-Model-Type"));
      final byte[] result = "canonical-ack".getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, result.length);
      try {
        exchange.getResponseBody().write(result);
      } finally {
        exchange.close();
      }
    });
    server.start();
    try {
      final Client client = new Client(
          "http://127.0.0.1:" + server.getAddress().getPort() + "/model", module());
      final CompletableFuture<String> response = new CompletableFuture<>();
      client.operation("complete", "text",
          SuccessHandler.handler(response::complete, response::completeExceptionally));

      assertEquals("canonical-ack", response.get(5, TimeUnit.SECONDS));
      assertEquals(1, requests.get());
      assertEquals("hook-test", moduleHeader.get());
      assertEquals("text", modelTypeHeader.get());
    } finally {
      server.stop(0);
    }
  }
}
