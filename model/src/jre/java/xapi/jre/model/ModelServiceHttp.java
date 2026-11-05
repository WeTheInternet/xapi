package xapi.jre.model;

import xapi.collect.X_Collect;
import xapi.collect.api.StringDictionary;
import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.io.api.DelegatingIOCallback;
import xapi.io.api.IOMessage;
import xapi.io.api.RequestFailed;
import xapi.io.service.IOService;
import xapi.jre.io.IOServiceDefault;
import xapi.jre.io.HttpBearerToken;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelList;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelModule;
import xapi.model.api.ModelNotFoundException;
import xapi.model.api.ModelQuery;
import xapi.model.api.ModelQueryResult;
import xapi.model.api.ModelCreateResult;
import xapi.model.service.ModelServiceWithCreateIfAbsent;
import xapi.source.lex.StringCharIterator;
import xapi.util.api.SuccessHandler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Opt-in JRE/Android client for the X_Model POST protocol. The endpoint is the
 * complete model base URL, for example {@code https://example.test/model}.
 * Load sends {@link #keyToString(ModelKey)}; persist and typed query use the existing
 * model/query serialization. Every request supplies X-Model-Type and X-Gwt-Module;
 * the latter is a historical metadata name and does not require a GWT runtime.
 *
 * <p>The module must be the exact manifest artifact used by the server (for example
 * loaded with ModelSerializerDefault.deserializeModule), including nested models.
 * This service is deliberately not an injection default: application startup must
 * explicitly select/configure it before accessing X_Model. It never uses local files.
 *
 * <p>Callbacks run on the IO completion thread, not an Android/UI thread. Success
 * returns a newly deserialized, server-acknowledged model; the caller's model is not
 * changed. Only a load HTTP 404 means absence. Timeouts and other failures do not
 * imply that a write failed to reach the server; callers must reconcile uncertain
 * writes using their stable key rather than create replacement records.
 *
 * <p>Autosave references must already have complete keys. Persist new referenced
 * models first and use their returned saved models before persisting the owner.
 * The entire outgoing autosave graph is checked before any request. Do not mutate
 * that graph during the synchronous serialization phase. ModelList is supported
 * as a nested property; this protocol does not carry a root ModelList's own key.
 */
public class ModelServiceHttp extends AbstractJreModelService implements ModelServiceWithCreateIfAbsent {

    private final String endpoint;
    private final ModelModule module;
    private final IOService io;
    private final URI endpointUri;
    private final HttpBearerToken authorization;
    private final ThreadLocal<Set<Model>> autosavePreflight = new ThreadLocal<>();

    public ModelServiceHttp(final String endpoint, final ModelModule module) {
        this(endpoint, module, 15_000);
    }

    /** Connect and read timeouts are each bounded by {@code timeoutMillis}. */
    public ModelServiceHttp(final String endpoint, final ModelModule module, final int timeoutMillis) {
        this(endpoint, module, timeoutMillis, null);
    }

    /**
     * Optional bearer credentials are resolved once per request, never stored in a
     * model or URL. A missing credential fails before IO. Redirects are rejected
     * rather than forwarding credentials or serialized models to another endpoint.
     */
    public ModelServiceHttp(final String endpoint, final ModelModule module, final int timeoutMillis,
                            final HttpBearerToken authorization) {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("A positive HTTP timeout is required");
        }
        final URI uri = URI.create(Objects.requireNonNull(endpoint, "endpoint"));
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getQuery() != null || uri.getFragment() != null
                || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("Expected an absolute HTTP model endpoint without query, fragment or credentials");
        }
        String base = endpoint;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        this.endpoint = base;
        endpointUri = uri;
        this.authorization = authorization;
        if (authorization != null) authorization.validateEndpoint(uri);
        this.module = Objects.requireNonNull(module, "module");
        final String moduleName = module.getModuleName();
        if (moduleName == null || moduleName.trim().isEmpty() || moduleName.contains("\r") || moduleName.contains("\n")) {
            throw new IllegalArgumentException("The shared module must have a valid name");
        }
        for (final ModelManifest manifest : module.getManifests()) {
            register(Objects.requireNonNull(manifest.getModelType(), "manifest model class"));
            registerManifest(manifest);
        }
        io = new IOServiceDefault() {
            @Override
            protected Reader createResponseReader(final InputStream in) {
                return new InputStreamReader(in, StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT));
            }
            @Override
            protected void applySettings(final URLConnection connection, final int modifiers) {
                connection.setConnectTimeout(timeoutMillis);
                connection.setReadTimeout(timeoutMillis);
                if (connection instanceof HttpURLConnection) {
                    ((HttpURLConnection) connection).setInstanceFollowRedirects(false);
                }
            }
        };
    }

    @Override
    public ModelModule getModelModule() {
        return module;
    }

    @Override
    protected boolean isAsync() {
        return false; // Serialization is immediate; IO itself is asynchronous.
    }

    @Override
    protected boolean isClientToServer() {
        return true;
    }

    @Override public boolean supportsCreateIfAbsent() { return true; }

    @Override public <M extends Model> void createIfAbsent(final M model,
            final SuccessHandler<ModelCreateResult<M>> callback) {
        final ModelManifest manifest;
        try { manifest = model == null ? null : requireManifest(model.getType()); }
        catch (Throwable failure) { fail(callback, failure); return; }
        createIfAbsent(manifest, model, callback);
    }

    @Override public <M extends Model> void createIfAbsent(final ModelManifest manifest, final M model,
            final SuccessHandler<ModelCreateResult<M>> callback) {
        final ModelCreateRequest request;
        try {
            request = new ModelCreateRequest(this, model == null ? null : requireManifest(model.getType()),
                    manifest, model, isClientToServer());
        } catch (Throwable failure) { fail(callback, failure); return; }
        postModelOperation("create", request.type, request.source, response -> {
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new IllegalStateException("Unexpected conditional-create status " + response.statusCode());
            }
            final M acknowledged;
            try { acknowledged = request.decode(this, response.body().getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
            catch (java.io.IOException invalid) { throw new IllegalStateException("Invalid conditional-create response", invalid); }
            return new ModelCreateResult<>(response.statusCode() == 201, acknowledged);
        }, callback, null); // A create 404 is failure, never missing state or permission to persist.
    }

    @Override
    protected <M extends Model> void doPersist(final String type, final M model, final SuccessHandler<M> callback) {
        final String body;
        final ModelKey expected;
        try {
            requireRootManifest(type);
            rejectRootList(model);
            validateKey(model.getKey(), false);
            if (model.getKey() != null && !type.equals(model.getKey().getKind())) {
                throw new IllegalArgumentException("Model kind does not match its key");
            }
            // Snapshot identity, since the caller may keep editing while IO is pending.
            expected = keyFromString(keyToString(model.getKey()));
            preflightAutosaves(type, model);
            body = serialize(type, model).toSource();
        } catch (final Throwable failure) {
            fail(callback, failure);
            return;
        }
        postModelOperation("persist", type, body, response -> {
            final M saved = readModel(type, response.body());
            verifyIdentity(expected, saved.getKey());
            return saved;
        }, callback, null);
    }

    private void preflightAutosaves(final String type, final Model model) {
        if (autosavePreflight.get() != null) {
            throw new IllegalStateException("Reentrant persistence during HTTP autosave preflight");
        }
        final Set<Model> active = Collections.newSetFromMap(new IdentityHashMap<Model, Boolean>());
        active.add(model);
        autosavePreflight.set(active);
        try {
            // Use the real directional serializer to visit exactly the outgoing references.
            // Its doPersistBlocking calls are intercepted below, with no network effects.
            serialize(type, model);
        } finally {
            autosavePreflight.remove();
        }
    }

    @Override
    public <M extends Model> Out1<M> doPersistBlocking(final String type, final M model, final long milliTimeout) {
        final Set<Model> active = autosavePreflight.get();
        if (active == null) {
            return super.doPersistBlocking(type, model, milliTimeout);
        }
        requireRootManifest(type);
        rejectRootList(model);
        // Reject before returning to the serializer: it would otherwise await an
        // incomplete child's saved result and absorb properties without its new key.
        validateKey(model.getKey(), true);
        if (!type.equals(model.getKey().getKind())) {
            throw new IllegalArgumentException("Autosaved model kind does not match its key");
        }
        if (!active.add(model)) {
            throw new IllegalArgumentException("Cyclic HTTP autosave references are unsupported");
        }
        try {
            serialize(type, model);
        } finally {
            active.remove(model);
        }
        // Complete keys never need an autosave result while serializing. Do not
        // return the caller object: if a key changes mid-pass, fail before absorb
        // can copy a model onto itself and clear aliased collection properties.
        return () -> {
            throw new IllegalStateException("An autosave key changed during HTTP preflight");
        };
    }

    @Override
    public <M extends Model> void load(final Class<M> type, final ModelKey key, final SuccessHandler<M> callback) {
        final String kind;
        final String body;
        final ModelKey expected;
        try {
            kind = getTypeName(type);
            requireRootManifest(kind);
            validateKey(key, true);
            if (!kind.equals(key.getKind())) {
                throw new IllegalArgumentException("Requested model class does not match key kind");
            }
            body = keyToString(key);
            expected = keyFromString(body);
        } catch (final Throwable failure) {
            fail(callback, failure);
            return;
        }
        postModelOperation("load", kind, body, response -> {
            final M loaded = readModel(kind, response.body());
            verifyIdentity(expected, loaded.getKey());
            return loaded;
        }, callback, expected);
    }

    @Override
    public <M extends Model> void query(final Class<M> type, final ModelQuery<M> query,
                                       final SuccessHandler<ModelQueryResult<M>> callback) {
        final String kind;
        final String body;
        final String namespace;
        final String ancestor;
        try {
            kind = getTypeName(type);
            requireRootManifest(kind);
            if (query.getAncestor() != null) {
                validateKey(query.getAncestor(), true);
            }
            body = query.serialize(this, primitiveSerializer());
            namespace = query.getNamespace();
            ancestor = query.getAncestor() == null ? null : keyToString(query.getAncestor());
        } catch (final Throwable failure) {
            fail(callback, failure);
            return;
        }
        postModelOperation("query", kind, body, response -> {
            final StringCharIterator chars = new StringCharIterator(response.body());
            final ModelQueryResult<M> result = new ModelQueryResult<>(type);
            result.setCursor(primitiveSerializer().deserializeString(chars));
            final int count = primitiveSerializer().deserializeInt(chars);
            if (count < 0) {
                throw new IllegalStateException("Negative query result count");
            }
            for (int i = 0; i < count; i++) {
                final M model = deserialize(type, chars);
                validateKey(model.getKey(), true);
                if (!kind.equals(model.getKey().getKind())
                        || !Objects.equals(namespace, model.getKey().getNamespace())
                        || !hasAncestor(model.getKey(), ancestor)) {
                    throw new IllegalStateException("Query returned a model outside its requested scope");
                }
                result.addModel(model);
            }
            if (chars.hasNext()) {
                throw new IllegalStateException("Unexpected trailing query response data");
            }
            return result;
        }, callback, null);
    }

    @Override
    public void query(final ModelQuery<Model> query, final SuccessHandler<ModelQueryResult<Model>> callback) {
        fail(callback, new UnsupportedOperationException("This HTTP protocol requires a typed query"));
    }

    @Override
    public void delete(final ModelKey key, final SuccessHandler<Boolean> callback) {
        fail(callback, new UnsupportedOperationException("This HTTP protocol does not define delete"));
    }

    private ModelManifest requireRootManifest(final String kind) {
        final ModelManifest manifest = requireManifest(kind);
        if (ModelList.class.isAssignableFrom(manifest.getModelType())) {
            throw new UnsupportedOperationException("The HTTP protocol does not carry a root ModelList's identity");
        }
        return manifest;
    }

    private void rejectRootList(final Model model) {
        if (model instanceof ModelList) {
            throw new UnsupportedOperationException("The HTTP protocol does not carry a root ModelList's identity");
        }
    }

    private ModelManifest requireManifest(final String kind) {
        final ModelManifest manifest = module.getManifest(kind);
        if (manifest == null) {
            throw new IllegalArgumentException("Model kind is not in the shared module: " + kind);
        }
        return manifest;
    }

    private <M extends Model> M readModel(final String type, final String body) {
        final StringCharIterator chars = new StringCharIterator(body);
        final M model = deserialize(requireManifest(type), chars);
        if (model == null || chars.hasNext() || !type.equals(model.getType())
                || model.getKey() == null || !type.equals(model.getKey().getKind())) {
            throw new IllegalStateException("Invalid model response");
        }
        return model;
    }

    private void verifyIdentity(final ModelKey expected, final ModelKey actual) {
        validateKey(actual, true);
        if (expected == null) {
            return; // Server may assign the first key for a new model.
        }
        if (expected.isComplete()) {
            if (!keyToString(expected).equals(keyToString(actual))) {
                throw new IllegalStateException("Server response changed model identity");
            }
        } else if (!Objects.equals(expected.getNamespace(), actual.getNamespace())
                || !Objects.equals(expected.getKind(), actual.getKind())
                || expected.getKeyType() != actual.getKeyType()
                || !keyToString(expected.getParent()).equals(keyToString(actual.getParent()))) {
            throw new IllegalStateException("Server response changed the new model's key scope");
        }
    }

    private boolean hasAncestor(final ModelKey key, final String ancestor) {
        if (ancestor == null) {
            return true;
        }
        for (ModelKey parent = key.getParent(); parent != null; parent = parent.getParent()) {
            if (ancestor.equals(keyToString(parent))) {
                return true;
            }
        }
        return false;
    }

    private void validateKey(final ModelKey key, final boolean complete) {
        if (key == null) {
            if (complete) {
                throw new IllegalArgumentException("A complete model key is required");
            }
            return;
        }
        final Set<ModelKey> visited = Collections.newSetFromMap(new IdentityHashMap<ModelKey, Boolean>());
        for (ModelKey part = key; part != null; part = part.getParent()) {
            if (!visited.add(part) || part.getKind() == null || part.getKind().isEmpty()
                    || (complete || part != key) && !part.isComplete()
                    || !Objects.equals(key.getNamespace(), part.getNamespace())) {
                throw new IllegalArgumentException("Keys must have complete parents and one consistent namespace");
            }
        }
    }

    protected final <T> void postModelOperation(final String operation, final String type, final String body,
                          final In1Out1<IOMessage<String>, T> decode, final SuccessHandler<T> callback,
                          final ModelKey missingKey) {
        try {
            if (operation == null || !operation.matches("[A-Za-z][A-Za-z0-9_-]{0,63}")) {
                throw new IllegalArgumentException("Invalid model operation identifier");
            }
            requireManifest(type);
            Objects.requireNonNull(body, "body");
            Objects.requireNonNull(decode, "decode");
        } catch (Throwable failure) { fail(callback, failure); return; }
        final AtomicBoolean terminal = new AtomicBoolean();
        final StringDictionary<String> headers = X_Collect.newDictionary();
        headers.setValue("Content-Type", "text/plain;charset=UTF-8");
        headers.setValue("Accept", "text/plain;charset=UTF-8");
        headers.setValue("X-Model-Type", type);
        headers.setValue("X-Gwt-Module", module.getModuleName());
        final DelegatingIOCallback<IOMessage<String>> handler = new DelegatingIOCallback<>(response -> {
            final T value;
            try {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IllegalStateException("Unexpected HTTP status " + response.statusCode());
                }
                value = decode.io(response);
            } catch (final Throwable failure) {
                if (terminal.compareAndSet(false, true)) {
                    fail(callback, failure);
                }
                return;
            }
            if (terminal.compareAndSet(false, true)) {
                callback.onSuccess(value);
            }
        }, failure -> {
            if (terminal.compareAndSet(false, true)) {
                if (missingKey != null && failure instanceof RequestFailed
                        && ((RequestFailed) failure).getResponse().statusCode() == 404) {
                    fail(callback, new ModelNotFoundException(missingKey));
                } else {
                    fail(callback, failure);
                }
            }
        });
        try {
            if (authorization != null) {
                headers.setValue("Authorization", authorization.authorizationHeader(endpointUri));
            }
            io.post(endpoint + "/" + operation, body, headers, handler);
        } catch (final Throwable failure) {
            handler.onError(failure);
        }
    }

    private static void fail(final SuccessHandler<?> callback, final Throwable failure) {
        DelegatingIOCallback.failHandler(callback).onError(failure);
    }
}
