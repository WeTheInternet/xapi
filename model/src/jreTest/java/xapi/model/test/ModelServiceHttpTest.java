package xapi.model.test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xapi.io.X_IO;
import xapi.io.api.RequestFailed;
import xapi.jre.model.ModelServiceHttp;
import xapi.jre.model.ModelServiceJre;
import xapi.jre.io.HttpBearerToken;
import xapi.model.api.*;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelUtil;
import xapi.model.test.api.ModelTestDirections;
import xapi.model.tools.ModelSerializerDefault;
import xapi.source.lex.StringCharIterator;
import xapi.util.api.SuccessHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/** Real HTTP protocol fixture; does not claim to test a deployed production endpoint. */
public class ModelServiceHttpTest {

    @Rule public final TemporaryFolder temporary = new TemporaryFolder();
    private HttpServer http;
    private Store server;
    private ModelServiceHttp client;
    private String endpoint;
    private String moduleArtifact;
    private String kind;
    private Path storage;
    private volatile int forcedStatus;
    private volatile String forcedBody;
    private volatile String lastBody;
    private volatile String lastAuthorization;
    private volatile String forcedLocation;
    private volatile ModelQuery<?> lastQuery;
    private volatile CountDownLatch holdResponse;
    private final CountDownLatch requestEntered = new CountDownLatch(1);
    private final AtomicInteger requests = new AtomicInteger();

    private static class Store extends ModelServiceJre {
        private final ModelModule module;

        Store(final ModelModule module) {
            this.module = module;
            for (final ModelManifest manifest : module.getManifests()) {
                register(manifest.getModelType());
                registerManifest(manifest);
            }
        }

        @Override
        public ModelModule getModelModule() {
            return module;
        }
    }

    @Before
    public void start() throws Exception {
        final ModelModule authored = new ModelModule();
        authored.setModuleName("http-model-test");
        authored.setUuid("http-model-test-v1");
        authored.addManifest(ModelUtil.createManifest(ModelText.class));
        moduleArtifact = ModelSerializerDefault.serialize(authored);
        // The two peers independently load ONE artifact, never reflect separate schemas.
        server = new Store(ModelSerializerDefault.deserializeModule(moduleArtifact));
        storage = temporary.newFolder("models").toPath();
        server.setRootDir(storage.toFile());
        kind = server.register(ModelText.class);
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/model/", this::serve);
        http.start();
        endpoint = "http://127.0.0.1:" + http.getAddress().getPort() + "/model";
        client = new ModelServiceHttp(endpoint, ModelSerializerDefault.deserializeModule(moduleArtifact), 2000);
    }

    @After
    public void stop() {
        if (http != null) {
            http.stop(0);
        }
    }

    private void serve(final HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
        lastBody = X_IO.toStringUtf8(exchange.getRequestBody());
        requestEntered.countDown();
        if (holdResponse != null) {
            try {
                holdResponse.await(5, TimeUnit.SECONDS);
            } catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                exchange.close();
                return;
            }
        }
        if (forcedStatus != 0) {
            if (forcedLocation != null) exchange.getResponseHeaders().set("Location", forcedLocation);
            reply(exchange, forcedStatus, forcedBody == null ? "forced failure" : forcedBody);
            return;
        }
        if (!"POST".equals(exchange.getRequestMethod())
                || !"http-model-test".equals(exchange.getRequestHeaders().getFirst("X-Gwt-Module"))
                || !kind.equals(exchange.getRequestHeaders().getFirst("X-Model-Type"))
                || !"text/plain;charset=UTF-8".equals(exchange.getRequestHeaders().getFirst("Content-Type"))) {
            reply(exchange, 400, "Unknown module/type or request format");
            return;
        }
        try {
            final String operation = exchange.getRequestURI().getPath().substring("/model/".length());
            final String response;
            switch (operation) {
                case "persist": {
                    final ModelText incoming = server.deserialize(ModelText.class, new StringCharIterator(lastBody));
                    final ModelText saved = await(callback -> server.persist(incoming, callback));
                    response = server.serialize(ModelText.class, saved).toSource();
                    break;
                }
                case "load": {
                    final ModelKey key = server.keyFromString(lastBody);
                    final ModelText loaded = await(callback -> server.load(ModelText.class, key, callback));
                    response = server.serialize(ModelText.class, loaded).toSource();
                    break;
                }
                case "query": {
                    final ModelQuery<ModelText> query = ModelQuery.deserialize(server,
                            server.primitiveSerializer(), new StringCharIterator(lastBody));
                    lastQuery = query;
                    final ModelQueryResult<ModelText> result = await(callback -> server.query(ModelText.class, query, callback));
                    response = result.serialize(server, server.primitiveSerializer());
                    break;
                }
                default:
                    reply(exchange, 400, "Unsupported operation");
                    return;
            }
            reply(exchange, 200, response);
        } catch (final Exception failure) {
            final Throwable cause = failure instanceof ExecutionException ? failure.getCause() : failure;
            final boolean missing = cause instanceof ModelNotFoundException && cause.getCause() == null;
            reply(exchange, missing ? 404 : 500, cause.toString());
        }
    }

    private static void reply(final HttpExchange exchange, final int status, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain;charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private ModelText text(final ModelKey key, final String text) {
        final ModelText model = client.create(ModelText.class);
        model.setKey(key);
        model.setText(text);
        model.setTime(1234);
        return model;
    }

    private ModelKey key(final String namespace, final String day) {
        return client.newKey(namespace, "day", day).setKeyType(7).getChild(kind, "same +%?# id").setKeyType(9);
    }

    private static <T> T await(final Consumer<SuccessHandler<T>> operation) throws Exception {
        final CompletableFuture<T> result = new CompletableFuture<>();
        operation.accept(SuccessHandler.handler(result::complete, result::completeExceptionally));
        return result.get(5, TimeUnit.SECONDS);
    }

    private static Throwable failure(final Consumer<SuccessHandler<ModelText>> operation) throws Exception {
        try {
            await(operation);
            throw new AssertionError("Expected failure");
        } catch (final ExecutionException expected) {
            return expected.getCause();
        }
    }

    @Test
    public void roundTripsFullKeysNamespacesAndExactTextUsingSharedManifest() throws Exception {
        final String body = "caf\u00e9 \u65e5\u672c\u8a9e \ud83c\udf31\r\nline two\n\nlast\r";
        final ModelKey first = key("owner +%?\u00e9", "2026-10-31");
        final ModelKey second = key("owner +%?\u00e9", "2026-11-01");
        final ModelKey otherOwner = key("someone else", "2026-10-31");
        for (final ModelKey key : new ModelKey[]{first, second, otherOwner}) {
            final ModelText original = text(key, body + key.getParent().getId() + key.getNamespace());
            final ModelText saved = await(callback -> client.persist(original, callback));
            assertNotSame(original, saved);
            assertEquals(client.keyToString(key), client.keyToString(saved.getKey()));
            assertEquals(9, saved.getKey().getKeyType());
            assertEquals(7, saved.getKey().getParent().getKeyType());
        }
        // Fresh client/store objects prove this isn't a client-side in-memory cache.
        client = new ModelServiceHttp(endpoint, ModelSerializerDefault.deserializeModule(moduleArtifact));
        server = new Store(ModelSerializerDefault.deserializeModule(moduleArtifact));
        server.setRootDir(storage.toFile());
        for (final ModelKey key : new ModelKey[]{first, second, otherOwner}) {
            final ModelText loaded = await(callback -> client.load(ModelText.class, key, callback));
            assertEquals(client.keyToString(key), lastBody);
            assertEquals(client.keyToString(key), client.keyToString(loaded.getKey()));
            assertEquals(body + key.getParent().getId() + key.getNamespace(), loaded.getText());
        }
    }

    @Test
    public void typedQueryCarriesFullAncestorAndNamespace() throws Exception {
        final ModelKey key = key("owner", "2026-10-31");
        ModelServiceHttpTest.<ModelText>await(callback -> client.persist(text(key, "first day"), callback));
        ModelServiceHttpTest.<ModelText>await(callback -> client.persist(text(key("owner", "2026-11-01"), "second day"), callback));
        final ModelQuery<ModelText> query = new ModelQuery<ModelText>().setNamespace("owner")
                .setAncestor(key.getParent()).setPageSize(10);
        final ModelQueryResult<ModelText> result = await(callback -> client.query(ModelText.class, query, callback));
        assertEquals("owner", lastQuery.getNamespace());
        assertEquals(client.keyToString(key.getParent()), client.keyToString(lastQuery.getAncestor()));
        assertEquals(1, result.getSize());
        assertEquals(client.keyToString(key), client.keyToString(result.getModelList().get(0).getKey()));
    }

    @Test
    public void onlyLoad404IsMissingAndFailuresNeverReplaceCallerData() throws Exception {
        final ModelKey key = key("owner", "today");
        final ModelText original = text(key, "keep this");
        final String originalKey = client.keyToString(key);
        assertTrue(failure(callback -> client.load(ModelText.class, key, callback)) instanceof ModelNotFoundException);
        for (final int status : new int[]{400, 500, 503}) {
            forcedStatus = status;
            final Throwable failedLoad = failure(callback -> client.load(ModelText.class, key, callback));
            assertTrue(failedLoad.toString(), failedLoad instanceof RequestFailed);
            assertEquals(status, ((RequestFailed) failedLoad).getResponse().statusCode());
        }
        forcedStatus = 404;
        assertTrue(failure(callback -> client.persist(original, callback)) instanceof RequestFailed);
        forcedStatus = 500;
        final AtomicInteger errors = new AtomicInteger();
        final CompletableFuture<Throwable> result = new CompletableFuture<>();
        client.persist(original, SuccessHandler.handler(saved -> result.completeExceptionally(new AssertionError()), error -> {
            errors.incrementAndGet();
            result.complete(error);
        }));
        assertTrue(result.get(5, TimeUnit.SECONDS) instanceof RequestFailed);
        assertEquals(1, errors.get());
        assertEquals(originalKey, client.keyToString(original.getKey()));
        assertEquals("keep this", original.getText());
        try (java.util.stream.Stream<Path> files = Files.walk(storage)) {
            assertEquals(0, files.filter(Files::isRegularFile).count());
        }
    }

    @Test
    public void rejectsMalformedAndWrongParentResponsesWithoutReplacingCallerModel() throws Exception {
        final ModelKey key = key("owner", "today");
        forcedStatus = 200;
        forcedBody = "not a serialized model";
        assertFalse(failure(callback -> client.load(ModelText.class, key, callback)) instanceof ModelNotFoundException);
        forcedBody = server.serialize(ModelText.class, text(key("owner", "other day"), "wrong")).toSource();
        final Throwable wrongKey = failure(callback -> client.load(ModelText.class, key, callback));
        assertTrue(wrongKey.toString(), wrongKey instanceof IllegalStateException);
        forcedBody += "trailing garbage";
        assertTrue(failure(callback -> client.load(ModelText.class, key, callback)) instanceof IllegalStateException);
    }

    @Test
    public void corruptSavedModelIsServerFailureAndUnreachableIsNotMissing() throws Exception {
        final ModelKey key = key("owner", "today");
        ModelServiceHttpTest.<ModelText>await(callback -> client.persist(text(key, "saved"), callback));
        final Path persisted;
        try (java.util.stream.Stream<Path> files = Files.walk(storage)) {
            persisted = files.filter(Files::isRegularFile).findFirst().get();
        }
        Files.write(persisted, "corrupt".getBytes(StandardCharsets.UTF_8));
        final Throwable corrupt = failure(callback -> client.load(ModelText.class, key, callback));
        assertTrue(corrupt.toString(), corrupt instanceof RequestFailed);
        assertEquals(500, ((RequestFailed) corrupt).getResponse().statusCode());
        assertEquals("corrupt", new String(Files.readAllBytes(persisted), StandardCharsets.UTF_8));
        http.stop(0);
        assertFalse(failure(callback -> client.load(ModelText.class, key, callback)) instanceof ModelNotFoundException);
    }

    @Test
    public void rejectsAmbiguousKeyNamespaceBeforeSending() throws Exception {
        final ModelKey key = client.newKey("", kind, "id").setParent(client.newKey("owner", "day", "today"));
        assertTrue(failure(callback -> client.load(ModelText.class, key, callback)) instanceof IllegalArgumentException);
        assertEquals(0, requests.get());
    }

    @Test
    public void timesOutAsynchronouslyWithoutTreatingTimeoutAsAbsenceOrRetrying() throws Exception {
        holdResponse = new CountDownLatch(1);
        client = new ModelServiceHttp(endpoint, ModelSerializerDefault.deserializeModule(moduleArtifact), 500);
        final CompletableFuture<Throwable> result = new CompletableFuture<>();
        final AtomicReference<Thread> completionThread = new AtomicReference<>();
        final Thread caller = Thread.currentThread();
        try {
            client.load(ModelText.class, key("owner", "today"), SuccessHandler.handler(
                    model -> result.completeExceptionally(new AssertionError("Unexpected success")), error -> {
                        completionThread.set(Thread.currentThread());
                        result.complete(error);
                    }));
            assertTrue(requestEntered.await(2, TimeUnit.SECONDS));
            assertFalse("IO must return while the server is still waiting", result.isDone());
            assertTrue(result.get(5, TimeUnit.SECONDS) instanceof java.net.SocketTimeoutException);
            assertNotSame(caller, completionThread.get());
            assertEquals(1, requests.get());
        } finally {
            holdResponse.countDown();
        }
    }

    @Test
    public void credentialsRotatePerRequestAndMissingCredentialNeverSends() throws Exception {
        final AtomicReference<String> token = new AtomicReference<>("first-test-token");
        client = new ModelServiceHttp(endpoint, ModelSerializerDefault.deserializeModule(moduleArtifact), 2000,
                HttpBearerToken.forLoopbackDevelopment(token::get));
        forcedStatus = 401;
        final ModelKey key = key("owner", "today");
        Throwable rejected = failure(callback -> client.load(ModelText.class, key, callback));
        assertTrue(rejected instanceof RequestFailed);
        assertEquals(401, ((RequestFailed) rejected).getResponse().statusCode());
        assertEquals("Bearer first-test-token", lastAuthorization);
        token.set("second-test-token");
        forcedStatus = 403;
        rejected = failure(callback -> client.load(ModelText.class, key, callback));
        assertTrue(rejected instanceof RequestFailed);
        assertEquals(403, ((RequestFailed) rejected).getResponse().statusCode());
        assertEquals("Bearer second-test-token", lastAuthorization);
        final int sent = requests.get();
        token.set(null);
        assertTrue(failure(callback -> client.load(ModelText.class, key, callback)) instanceof IllegalStateException);
        token.set("invalid\r\ncredential");
        assertTrue(failure(callback -> client.load(ModelText.class, key, callback)) instanceof IllegalArgumentException);
        assertEquals(sent, requests.get());
    }

    @Test
    public void redirectsNeverReceiveCredentialsOrModelPayloads() throws Exception {
        final AtomicInteger forwarded = new AtomicInteger();
        http.createContext("/capture", exchange -> { forwarded.incrementAndGet(); reply(exchange, 200, "unexpected"); });
        forcedLocation = "http://127.0.0.1:" + http.getAddress().getPort() + "/capture";
        client = new ModelServiceHttp(endpoint, ModelSerializerDefault.deserializeModule(moduleArtifact), 2000,
                HttpBearerToken.forLoopbackDevelopment(() -> "test-token"));
        for (final int status : new int[]{301, 302, 303, 307, 308}) {
            forcedStatus = status;
            assertFalse(failure(callback -> client.persist(text(key("owner", "today"), "private model"), callback))
                    instanceof ModelNotFoundException);
        }
        assertEquals(5, requests.get());
        assertEquals(0, forwarded.get());
        assertEquals("Bearer test-token", lastAuthorization);
    }

    @Test
    public void sharedManifestUsesClientToServerAndServerToClientPolicies() throws Exception {
        final ModelModule authored = new ModelModule();
        authored.setModuleName("http-model-test");
        authored.setUuid("directions-v1");
        authored.addManifest(ModelUtil.createManifest(ModelTestDirections.class));
        final String artifact = ModelSerializerDefault.serialize(authored);
        final Store remote = new Store(ModelSerializerDefault.deserializeModule(artifact));
        final ModelServiceHttp local = new ModelServiceHttp(endpoint, ModelSerializerDefault.deserializeModule(artifact));
        final ModelTestDirections original = local.create(ModelTestDirections.class);
        original.setKey(local.newKey("owner", ModelTestDirections.MODEL_TEST_DIRECTIONS, "one"));
        original.setClientToServer("outgoing");
        original.setServerToClient("must not upload");
        original.setBothEnabled("both");
        original.setBothDisabled("private");
        final ModelTestDirections response = remote.create(ModelTestDirections.class);
        response.setKey(original.getKey());
        response.setClientToServer("must not download");
        response.setServerToClient("incoming");
        response.setBothEnabled("acknowledged");
        forcedStatus = 200;
        forcedBody = remote.serialize(ModelTestDirections.class, response).toSource();
        final ModelTestDirections saved = await(callback -> local.persist(original, callback));
        final ModelTestDirections received = remote.deserialize(ModelTestDirections.class, new StringCharIterator(lastBody));
        assertEquals("outgoing", received.getClientToServer());
        assertNull(received.getServerToClient());
        assertEquals("both", received.getBothEnabled());
        assertNull(received.getBothDisabled());
        assertEquals("incoming", saved.getServerToClient());
        assertNull(saved.getClientToServer());
        assertEquals("acknowledged", saved.getBothEnabled());
        assertNull(saved.getBothDisabled());
        assertEquals("must not upload", original.getServerToClient());
    }
}
