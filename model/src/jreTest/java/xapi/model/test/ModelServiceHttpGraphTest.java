package xapi.model.test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xapi.io.X_IO;
import xapi.jre.io.HttpBearerToken;
import xapi.jre.model.ModelServiceHttp;
import xapi.jre.model.ModelServiceJre;
import xapi.model.api.*;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelUtil;
import xapi.model.test.api.ModelTestDirections;
import xapi.model.tools.ModelSerializerDefault;
import xapi.model.user.ModelUser;
import xapi.source.lex.StringCharIterator;
import xapi.util.api.SuccessHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/** Real HTTP boundary fixture for outgoing graph validation, not application atomicity. */
public class ModelServiceHttpGraphTest {
    private HttpServer http;
    private ModelServiceHttp client;
    private Codec codec;
    private String endpoint;
    private final AtomicInteger credentialReads = new AtomicInteger();
    private final AtomicInteger requests = new AtomicInteger();
    private final Map<String, ModelContent> received = new ConcurrentHashMap<>();
    private volatile CountDownLatch responsesSent = new CountDownLatch(0);

    private static final class Codec extends ModelServiceJre {
        private final ModelModule module;
        Codec(final ModelModule module) {
            this.module = module;
            for (final ModelManifest manifest : module.getManifests()) {
                register(manifest.getModelType());
                registerManifest(manifest);
            }
        }
        @Override public ModelModule getModelModule() { return module; }
        @Override protected boolean isKeyOnlyHydrationEnabled() { return false; }
    }

    @Before public void start() throws Exception {
        http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        http.createContext("/model/", this::serve);
        http.start();
        endpoint = "http://127.0.0.1:" + http.getAddress().getPort() + "/model";
        configure(false);
    }

    private void configure(final boolean excludeRelatedUpload) throws Exception {
        final ModelModule authored = new ModelModule();
        authored.setModuleName("http-graph-test");
        authored.setUuid("http-graph-test-v1");
        for (final Class<? extends Model> type : Arrays.asList(
                ModelContent.class, ModelList.class, ModelText.class, ModelUser.class)) {
            authored.addManifest(ModelUtil.createManifest(type));
        }
        final ModelManifest content = authored.getManifest("content");
        // Exercise both serializer autosave paths with one shared, explicit schema.
        content.getMethodData("author").addAnnotations(ModelContent.class.getMethod("getRelated").getAnnotations());
        if (excludeRelatedUpload) {
            content.getMethodData("related").addAnnotations(
                    ModelTestDirections.class.getMethod("getServerToClient").getAnnotations());
        }
        final String artifact = ModelSerializerDefault.serialize(authored);
        codec = new Codec(ModelSerializerDefault.deserializeModule(artifact));
        client = new ModelServiceHttp(endpoint, ModelSerializerDefault.deserializeModule(artifact), 2000,
                HttpBearerToken.forLoopbackDevelopment(() -> {
                    credentialReads.incrementAndGet();
                    return "fixture-token";
                }));
    }

    @After public void stop() {
        if (http != null) http.stop(0);
    }

    private void serve(final HttpExchange exchange) throws IOException {
        requests.incrementAndGet();
        final String body = X_IO.toStringUtf8(exchange.getRequestBody());
        try {
            if (!"POST".equals(exchange.getRequestMethod())
                    || !"/model/persist".equals(exchange.getRequestURI().getPath())
                    || !"http-graph-test".equals(exchange.getRequestHeaders().getFirst("X-Gwt-Module"))
                    || !"content".equals(exchange.getRequestHeaders().getFirst("X-Model-Type"))) {
                reply(exchange, 400, "Unexpected fixture request");
                return;
            }
            final StringCharIterator input = new StringCharIterator(body);
            final ModelContent incoming = codec.deserialize(ModelContent.class, input);
            if (input.hasNext()) throw new IllegalStateException("Trailing request data");
            if (incoming.getKey() == null) incoming.setKey(codec.newKey("owner", "content", "assigned"));
            if (incoming.getKey().isIncomplete()) incoming.getKey().setId("assigned");
            received.put(incoming.getKey().getId(), incoming);
            // A separate acknowledged object with no references avoids background hydration
            // and keeps this fixture about outgoing requests and caller immutability.
            final ModelContent saved = codec.create(ModelContent.class);
            saved.setKey(codec.keyFromString(codec.keyToString(incoming.getKey())));
            saved.setText("ack:" + incoming.getText());
            saved.setPermaLink(incoming.getPermaLink());
            reply(exchange, 200, codec.serialize(ModelContent.class, saved).toSource());
        } catch (final Throwable failure) {
            reply(exchange, 500, failure.toString());
        } finally {
            responsesSent.countDown();
        }
    }

    private static void reply(final HttpExchange exchange, final int status, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain;charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private ModelContent content(final String id) {
        final ModelContent model = client.create(ModelContent.class);
        model.setKey(client.newKey("owner", "content", id));
        model.setText("original:" + id);
        model.setPermaLink("link:" + id);
        return model;
    }

    @SuppressWarnings("unchecked")
    private ModelList<ModelContent> list(final ModelContent... children) {
        final ModelList<ModelContent> list = client.create(ModelList.class);
        list.setModelType(ModelContent.class);
        // The model map is insertion ordered. Do not use add(), which may ensure keys.
        for (final ModelContent child : children) list.models().put(child.getKey(), child);
        return list;
    }

    private static <T> T await(final Consumer<SuccessHandler<T>> operation) throws Exception {
        final CompletableFuture<T> result = new CompletableFuture<>();
        operation.accept(SuccessHandler.handler(result::complete, result::completeExceptionally));
        return result.get(5, TimeUnit.SECONDS);
    }

    private static <T> Throwable rejected(final Consumer<SuccessHandler<T>> operation) throws Exception {
        try {
            await(operation);
            throw new AssertionError("Expected rejection");
        } catch (final ExecutionException failure) {
            return failure.getCause();
        }
    }

    private void assertNoRequestAttempt() {
        // Credential resolution is synchronous and directly precedes IO submission.
        // This assertion cannot race with an already submitted background HTTP request.
        assertEquals(0, credentialReads.get());
        assertEquals(0, requests.get());
    }

    @Test public void rejectsIncompleteSingleAutosaveReferenceWithoutChangingCaller() throws Exception {
        for (final boolean missing : new boolean[]{false, true}) {
            final ModelContent parent = content("parent");
            final ModelUser child = client.create(ModelUser.class);
            final ModelKey originalKey = missing ? null : client.newKey("owner", child.getType(), null);
            child.setKey(originalKey);
            parent.setAuthor(child);
            final Throwable failure = ModelServiceHttpGraphTest.<ModelContent>rejected(
                    callback -> client.persist(parent, callback));
            assertTrue(failure.toString(), failure instanceof IllegalArgumentException);
            assertSame(child, parent.getAuthor());
            assertSame(originalKey, child.getKey());
            assertEquals("original:parent", parent.getText());
            assertNoRequestAttempt();
        }
    }

    @Test public void validatesAutosaveAncestorsNamespaceAndKindBeforeIO() throws Exception {
        final ModelUser child = client.create(ModelUser.class);
        final String kind = child.getType();
        final ModelKey cyclic = client.newKey("owner", kind, "user");
        cyclic.setParent(cyclic);
        final ModelKey[] invalidKeys = {
                client.newKey("owner", kind, "user").setParent(client.newKey("owner", "group", null)),
                client.newKey("owner", kind, "user").setParent(client.newKey("someone-else", "group", "group")),
                client.newKey("owner", "wrong-kind", "user"),
                cyclic
        };
        for (final ModelKey invalid : invalidKeys) {
            final ModelContent parent = content("parent");
            child.setKey(invalid);
            parent.setAuthor(child);
            final Throwable failure = ModelServiceHttpGraphTest.<ModelContent>rejected(
                    callback -> client.persist(parent, callback));
            assertTrue(failure.toString(), failure instanceof IllegalArgumentException);
            assertSame(invalid, child.getKey());
            assertSame(child, parent.getAuthor());
            assertNoRequestAttempt();
        }
    }

    @Test public void rejectsLaterIncompleteListChildBeforeSavingEarlierCompleteSibling() throws Exception {
        final ModelContent parent = content("parent");
        final ModelContent complete = content("complete");
        final ModelContent leaf = content("inline-leaf");
        final ModelList<ModelContent> children = list(leaf);
        complete.setChildren(children);
        final ModelContent incomplete = content(null);
        final ModelList<ModelContent> related = list(complete, incomplete);
        parent.setRelated(related);
        assertSame(complete, related.iterator().next());
        final Throwable failure = ModelServiceHttpGraphTest.<ModelContent>rejected(
                callback -> client.persist(parent, callback));
        assertTrue(failure.toString(), failure instanceof IllegalArgumentException);
        assertSame(related, parent.getRelated());
        assertEquals(2, related.size());
        assertSame(children, complete.getChildren());
        assertEquals(1, children.size());
        assertSame(leaf, children.iterator().next());
        assertEquals("original:inline-leaf", leaf.getText());
        assertTrue(incomplete.getKey().isIncomplete());
        assertNoRequestAttempt();
    }

    @Test public void rejectsIncompleteGrandchildAcrossAutosaveAndEmbeddedParents() throws Exception {
        for (final boolean embedded : new boolean[]{false, true}) {
            final ModelContent parent = content("parent");
            final ModelContent child = content("child");
            final ModelContent incomplete = content(null);
            final ModelList<ModelContent> grandchildren = list(incomplete);
            child.setRelated(grandchildren);
            final ModelList<ModelContent> children = list(child);
            if (embedded) parent.setChildren(children); else parent.setRelated(children);
            final Throwable failure = ModelServiceHttpGraphTest.<ModelContent>rejected(
                    callback -> client.persist(parent, callback));
            assertTrue(failure.toString(), failure instanceof IllegalArgumentException);
            assertSame(grandchildren, child.getRelated());
            assertEquals(1, grandchildren.size());
            assertSame(incomplete, grandchildren.iterator().next());
            assertEquals(1, children.size());
            assertNoRequestAttempt();
        }
    }

    @Test public void rejectsCyclicAutosaveBeforeAnyRequest() throws Exception {
        final ModelContent parent = content("parent");
        parent.setRelated(list(parent));
        final Throwable failure = ModelServiceHttpGraphTest.<ModelContent>rejected(
                callback -> client.persist(parent, callback));
        assertTrue(failure.toString(), failure instanceof IllegalArgumentException);
        assertSame(parent, parent.getRelated().iterator().next());
        assertNoRequestAttempt();
    }

    @Test public void completeAutosaveReferencesAndInlineListsPreserveCallerCollections() throws Exception {
        final ModelContent parent = content("parent");
        final ModelContent child = content("child");
        // An embedded model can remain incomplete; only independently autosaved references
        // require complete identities. This still exercises nested ModelList framing.
        final ModelContent inline = content(null);
        final ModelList<ModelContent> children = list(inline);
        child.setChildren(children);
        final ModelList<ModelContent> related = list(child);
        parent.setRelated(related);
        responsesSent = new CountDownLatch(2);
        final ModelContent saved = await(callback -> client.persist(parent, callback));
        assertTrue(responsesSent.await(5, TimeUnit.SECONDS));
        assertEquals(2, credentialReads.get());
        assertEquals(2, requests.get());
        assertNotSame(parent, saved);
        assertEquals("ack:original:parent", saved.getText());
        assertEquals("original:parent", parent.getText());
        assertSame(related, parent.getRelated());
        assertEquals(1, related.size());
        assertSame(child, related.iterator().next());
        assertSame(children, child.getChildren());
        assertEquals(1, children.size());
        assertSame(inline, children.iterator().next());
        assertTrue(inline.getKey().isIncomplete());
        assertEquals("child", received.get("parent").getRelated().iterator().next().getKey().getId());
        assertEquals("original:null", received.get("child").getChildren().iterator().next().getText());
    }

    @Test public void excludedAutosavePropertyIsNotTraversedOrSent() throws Exception {
        configure(true);
        final ModelContent parent = content("parent");
        final ModelContent incomplete = content(null);
        final ModelList<ModelContent> related = list(incomplete);
        parent.setRelated(related);
        final ModelContent saved = await(callback -> client.persist(parent, callback));
        assertEquals("ack:original:parent", saved.getText());
        assertEquals(1, credentialReads.get());
        assertEquals(1, requests.get());
        assertNull(received.get("parent").getRelated());
        assertSame(related, parent.getRelated());
        assertTrue(incomplete.getKey().isIncomplete());
    }

    @Test public void rejectsRootModelListPersistLoadAndTypedQueryBeforeIO() throws Exception {
        final ModelList<ModelContent> root = list(content("child"));
        root.setKey(client.newKey("owner", root.getType(), "root-list"));
        assertTrue(ModelServiceHttpGraphTest.<ModelList<ModelContent>>rejected(
                callback -> client.persist(root, callback)) instanceof UnsupportedOperationException);
        assertTrue(ModelServiceHttpGraphTest.<ModelList>rejected(
                callback -> client.load(ModelList.class, root.getKey(), callback)) instanceof UnsupportedOperationException);
        assertTrue(ModelServiceHttpGraphTest.<ModelQueryResult<ModelList>>rejected(
                callback -> client.query(ModelList.class, new ModelQuery<ModelList>().setNamespace("owner"), callback))
                instanceof UnsupportedOperationException);
        assertEquals(1, root.size());
        assertEquals("root-list", root.getKey().getId());
        assertNoRequestAttempt();
    }
}
