package xapi.server.vertx;

import com.github.javaparser.ast.expr.UiContainerExpr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.shareddata.Counter;
import io.vertx.core.spi.cluster.ClusterManager;
import xapi.annotation.model.IsModel;
import xapi.bytecode.NotFoundException;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.InitMap;
import xapi.collect.api.IntTo.Many;
import xapi.collect.api.StringTo;
import xapi.collect.impl.InitMapDefault;
import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobManager;
import xapi.dev.gwtc.impl.GwtcJobManagerAbstract;
import xapi.dev.gwtc.api.GwtcService;
import xapi.except.MultiException;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.*;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In3.In3Unsafe;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.gwtc.api.GwtManifest;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.io.impl.IOCallbackDefault;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelNotFoundException;
import xapi.model.api.PrimitiveSerializer;
import xapi.process.X_Process;
import xapi.scope.X_Scope;
import xapi.scope.api.Scope;
import xapi.scope.request.SessionScope;
import xapi.server.X_Server;
import xapi.server.api.ModelGwtc;
import xapi.server.api.Route;
import xapi.server.api.WebApp;
import xapi.server.api.XapiEndpoint;
import xapi.server.api.XapiServer;
import xapi.server.model.ModelSession;
import xapi.source.template.MappedTemplate;
import xapi.time.X_Time;
import xapi.util.X_String;
import xapi.util.X_Util;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import static io.vertx.core.Future.succeededFuture;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public class XapiVertxServer implements XapiServer<RequestScopeVertx> {

    static {
        X_Model.register(ModelSession.class);
    }

    private static final String SESSION_KEY = "wtis";
    private static final String INSTANCE_ID_COUNTER = "iids";
    private static final String EXPIRED = "Thu, 01 Jan 1970 00:00:00 GMT";
    // This is only used in non-clustered mode, and would benefit from a persistent source
    // to ensure uniqueness and monotonicity across server starts.
    private static AtomicLong ts = new AtomicLong(System.nanoTime()/1024);
    private final WebApp webApp;
    private final PrimitiveSerializer primitives;
    private Vertx vertx;
    private In1<DoUnsafe> exe;
    private final InitMap<String, XapiEndpoint<?>> endpoints;
    /**
     * This instance id; by default, generated sequentially from static variable.
     * TODO: use a vert.x clustered counter
     */
    private final Lazy<String> iid;
    private final Lazy<SecureRandom> random;
    private final ChainBuilder<In2<Vertx, HttpServer>> onStarted;
    private final ChainBuilder<In2<Vertx, HttpServer>> onShutdown;
    private ClusterManager clusterManager;
    private final Lazy<Out1<String>> iidFactory;
    private Do onRelease;

    public XapiVertxServer(WebApp webApp) {
        this.webApp = webApp;
        endpoints = new InitMapDefault<>(
            X_Fu::identity, this::findEndpoint
        );
        onStarted = Chain.startChain();
        onShutdown = Chain.startChain();
        onStarted.add((v, s)->
            instanceIdFactory()
        );
        primitives = X_Inject.instance(PrimitiveSerializer.class);
        iidFactory = Lazy.deferred1(initializeIidFactory(webApp));
        iid = Lazy.deferBoth(webApp::getOrMakeInstanceId, iidFactory);
        random = Lazy.deferBoth(SecureRandom::new, ()->iid.out1().getBytes());
        onRelease = webApp.isDestroyable() ? webApp::destroy : Do.NOTHING;
    }

    protected Out1<Out1<String>> initializeIidFactory(WebApp webApp) {
        return ()->{
            if (clusterManager == null) {
                // no cluster, no problem... just create a simple shared counter
                return ()-> "iid" + Long.toString(ts.incrementAndGet(), 36);
            }

            AtomicLong result = new AtomicLong();
            final Do useDefaultId = ()->{
                final int newId = System.identityHashCode(new Object());
                while (!result.compareAndSet(0, newId)) {
                    LockSupport.parkNanos(10_000);
                }
                synchronized (result) {
                    result.notify();
                }
            };
            Do generateId = ()->{
                clusterManager.getCounter(INSTANCE_ID_COUNTER, fut->{
                    if (fut.failed()) {
                        X_Log.error(XapiVertxServer.class, "Unable to get instance id counter", fut.cause());
                        // Use fallback id
                        useDefaultId.done();
                    } else {
                        final Counter counter = fut.result();
                        counter.incrementAndGet(newId->{
                            if (newId.failed()) {
                                X_Log.error(XapiVertxServer.class, "Unable to increment instance id counter", fut.cause());
                                useDefaultId.done();
                            } else {
                                final Long id = newId.result();
                                while (!result.compareAndSet(0, id)) {
                                    LockSupport.parkNanos(10_000);
                                }
                                synchronized (result) {
                                    result.notify(); // only notify one at a time, since each request would have asked once
                                }
                            }
                        });
                    }
                });
            };
            // we'll prime the pump...
            X_Time.runLater(generateId.toRunnable());
            return ()-> {
                long winner = result.get();
                if (winner != 0) {
                    if (!result.compareAndSet(winner, 0)) {
                        // we did not win race
                        winner = 0;
                    }
                }
                if (winner == 0) {
                    generateId.done();
                    while ((winner = result.get()) == 0) {
                        synchronized (result) {
                            try {
                                result.wait(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw X_Util.rethrow(e);
                            }
                        }
                    }
                }
                return primitives.serializeLong(winner);
            };
        };
    }

    @Override
    public void registerEndpoint(String name, XapiEndpoint<RequestScopeVertx> endpoint) {
        endpoints.put(name, endpoint);
    }

    @Override
    public void registerEndpointFactory(String name, boolean singleton, In1Out1<String, XapiEndpoint<RequestScopeVertx>> endpoint) {
        synchronized (endpoints) {
            endpoints.put(name, (path, requestLike, payload, callback) -> {
                final XapiEndpoint realEndpoint = endpoint.io(name);
                if (singleton) {
                    endpoints.put(name, realEndpoint);
                }
                realEndpoint.initialize(requestLike, XapiVertxServer.this);
                realEndpoint.serviceRequest(path, requestLike, payload, callback);
            });
        }
    }

    @Override
    public void onRelease() {
        onRelease.done();
        onRelease = Do.unsafe(()->{
            throw new IllegalStateException("Server already released");
        });
    }


    protected XapiEndpoint<?> findEndpoint(String name) {
        // this is called during the init process of the underlying map,
        // but we made it protected so it could be overloaded, and that
        // means still want to route all endpoint loading through the caching map.
        if (endpoints.containsKey(name)) {
            return endpoints.get(name);
        }
        try {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            final Class<?> cls = cl.loadClass(name);
            // once we have the class, lets inject it...
            Object inst;
            try {
                inst = X_Inject.singleton(cls);
                if (inst == null) {
                    throw new NullPointerException();
                }
                assert XapiEndpoint.class.isInstance(inst) : "Injection result of " + name + ", " + inst.getClass() + " is not a XapiEndpoint" +
                    " (or there is something nefarious happening with your classloader)";
                final XapiEndpoint<?> endpoint = (XapiEndpoint<?>) inst;
                // we'll cache singletons
                endpoints.put(name, endpoint);
                return endpoint;
            } catch (RuntimeException ignored) {
                // no dice... can we inject an instance?
                try {
                    inst = X_Inject.instance(cls);
                    // huzzah!  we can create instances.  In this case, we'll return the one we just created,
                    // plus create a simple delegate which knows to inject a new endpoint per invocation.
                    final Class<XapiEndpoint<?>> c = Class.class.cast(cls);
                    endpoints.put(name, (path, requestLike, payload, callback) -> {
                        XapiEndpoint realInst = X_Inject.instance(c);
                        realInst.serviceRequest(path, requestLike, payload, callback);
                    });
                    assert XapiEndpoint.class.isInstance(inst) : "Injection result of " + name + ", " + inst.getClass() + " is not a XapiEndpoint" +
                        " (or there is something nefarious happening with your classloader)";
                    return (XapiEndpoint<?>) inst;
                } catch (RuntimeException stillIgnored) {
                    // STILL no dice... try service loader then give up.
                    for (Object result : ServiceLoader.load(cls, cl)) {
                        // if you loaded through service loader, you are going to be static, and can worry about
                        // state management yourself.
                        if (XapiEndpoint.class.isInstance(result)) {
                            final XapiEndpoint<?> endpoint = (XapiEndpoint<?>) result;
                            endpoints.put(name, endpoint);
                            return endpoint;
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new NotConfiguredCorrectly("Could not load endpoint named " + name);
        }
        return null;
    }

    @Override
    public WebApp getWebApp() {
        return webApp;
    }

    @Override
    public void inScope(
        In1Out1<SessionScope, RequestScopeVertx> scopeFactory, In3Unsafe<RequestScopeVertx, Throwable, Do> callback
    ) {
        X_Scope.service().runInNewScope(SessionScope.class, (session, done)->{

            final RequestScopeVertx scope = scopeFactory.io(session);
            final XapiServer was = scope.setLocal(XapiServer.class, this);
            final MapLike<String, String> cookies = scope.getRequest().getCookies();
            ModelSession model;
            final SuccessHandler<ModelSession> saved = new IOCallbackDefault<ModelSession>(){
                @Override
                public void onSuccess(ModelSession t) {
                    finish(null);
                }

                @Override
                public void onError(Throwable e) {
                    if (!(e instanceof ModelNotFoundException)) {
                        // not found is normal for a session that has been purged in the backend
                        X_Log.error(XapiVertxServer.class, "Error loading session", e);
                    }
                    scope.getResponse().addHeader("Set-Cookie", SESSION_KEY+"=;path=/;expires=" + EXPIRED + ";");
                    finish(e);
                }

                private void finish(Throwable e) {
                    callback
                        .in(scope, e, ()->{
                            if (was == null) {
                                scope.removeLocal(XapiServer.class);
                            } else {
                                scope.setLocal(XapiServer.class, was);
                            }
                            done.done();
                    });
                }
            };
            // TODO: if a request does not require a session, don't bother loading one.
            // for now, we're just going to pay every time for speedier development,
            // and will optimize once the platform matures
            if (cookies.has(SESSION_KEY)) {
                // There is a session key; let's ensure we're synced up
                final String cookie = cookies.get(SESSION_KEY);
                if (!cookie.trim().isEmpty()) {

                    final ModelKey key = ModelSession.SESSION_KEY_BUILDER.out1()
                        .withId(cookie).buildKey();
                    X_Model.mutate(ModelSession.class, key, modelSession->{
                        modelSession.setTouched(System.currentTimeMillis());
                        return modelSession;
                    }, saved);
                    return;
                }
            }
            // no session key... let's properly initialize our session, and send along a cookie to get session back
            String sessionKey = generateSessionKey();
            scope.getResponse().addHeader("Set-Cookie", SESSION_KEY+"="+X_String.encodeURIComponent(sessionKey)+";path=/;");
            model = ModelSession.SESSION_MODEL_BUILDER.out1().buildModel();
            model.getKey().setId(sessionKey);
            model.setTouched(System.currentTimeMillis());
            X_Model.persist(model, saved);
        });
    }

    protected String generateSessionKey() {
        final PrimitiveSerializer serializer = X_Model.getService().primitiveSerializer();
        String timestamp = serializer.serializeDouble(X_Time.nowPlusOne().millis());
        String iid = webApp.getOrMakeInstanceId(instanceIdFactory());
        return
            timestamp +
            primitives.serializeLong(random.out1().nextLong()) +
            primitives.serializeString(iid);
    }

    private Out1<String> instanceIdFactory() {
        return iidFactory.out1();
    }

    @Override
    public void start(Do onStart) {
        webApp.setRunning(true);
        if (webApp.getPort() == 0) {
            X_Server.usePort(p->{
                webApp.setPort(p);
                start(onStart);
            });
            return;
        }
        final HttpServerOptions opts = new HttpServerOptions()
            .setHost("0.0.0.0")
            .setPort(webApp.getPort());

        Mutable<HttpServer> connection = new Mutable<>();
        final VertxOptions vertxOptions = new VertxOptions();

        if (webApp.isDevMode()) {
            vertxOptions
                .setBlockedThreadCheckInterval(2000)
                .setQuorumSize(5)
                .setHAEnabled(true)
                .setMaxEventLoopExecuteTime(60_000_000_000L) // one minute for event loop when debugging
                .setMaxWorkerExecuteTime(200_000_000_000L);
        }
        exe = X_Process::runDeferred;
        final Handler<AsyncResult<Vertx>> startup = res->{
            if (res.succeeded()) {
                vertx = res.result();
                final Scope scope = X_Scope.service().currentScope();
                scope.setLocal(Vertx.class, vertx);
                clusterManager = ((VertxInternal) vertx).getClusterManager();
                if (clusterManager != null) {
                    scope.setLocal(ClusterManager.class, clusterManager);
                }
                scope.setLocal(WebApp.class, webApp);
                scope.setLocal(XapiServer.class, XapiVertxServer.this);
                vertx.createHttpServer(opts)
                    .requestHandler(this::handleRequest)
                    .listen(result->{
                        if (result.failed()) {
                            throw new IllegalStateException("Server failed to start ", result.cause());
                        }
                        onStarted(vertx, result.result());
                        X_Log.info(getClass(), "Server up and running at 0.0.0.0:" + webApp.getPort());
                        endpoints.forEach((key, endpoint)->
                            endpoint.initialize(scope, XapiVertxServer.this)
                        );
                        onStart.done();
                        // we'll set the mutable's value last,
                        // as we want the calling thread to block until the job is really complete
                        connection.in(result.result());
                    });

            } else {
                X_Log.error(XapiVertxServer.class, "Vert.x failed to start", res.cause());
                if (onStart instanceof ErrorHandler) {
                    ((ErrorHandler) onStart).onError(res.cause());
                }
            }
        };
        if (webApp.isClustered()) {
            Vertx.clusteredVertx(vertxOptions, startup);
        } else {
            final Vertx server = Vertx.vertx(vertxOptions);
            startup.handle(succeededFuture(server));
        }


        while (connection.isNull()) {
            LockSupport.parkNanos(100_000);
        }

    }

    public void onStarted(In2<Vertx, HttpServer> callback) {
        assert !onStarted.anyMatch(callback::equals) : "Duplicate onStarted callback added: " + callback;
        onStarted.add(callback);
    }

    protected void onStarted(Vertx vertx, HttpServer result) {
        onStarted.forAll(In2::in, vertx, result);
    }

    protected void handleRequest(HttpServerRequest req) {
        final VertxRequest request = new VertxRequest(req);
        final VertxResponse response = new VertxResponse(req.response());
        inScope(session->(RequestScopeVertx)session.getRequestScope(request, response), (scope, t, done)->{
            serviceRequest(scope, (r, error)->{
                if (error != null) {
                  handleError(req, response, error);
                }
                // finish response
                response.finish();
                // release scope
                done.done();
            });
        });
    }

    protected void handleError(HttpServerRequest req, VertxResponse resp, Throwable error) {
        if (!resp.getResponse().ended()) {
            final String body = "<!DOCTYPE html><html>" +
                "<body style=\"text-align: center; vertical-align: middle; display: block\">" +
                "<div style=\"white-space:pre-wrap\">" +
                "Our apologies, we cannot find anything to serve to:\n" +
                dump(req) +
                "</div>" +
                "</body>" +
                "</html>";
            resp.getResponse().headers().set("Content-Length", Integer.toString(body.length()));
            resp.getResponse().write(body);
            resp.finish(404);
        }
    }

    public static String dump(HttpServerRequest req) {return dump(req, "\n");}

    public static String dump(
        HttpServerRequest req,
        String suffix
    ) {
        StringBuilder b = new StringBuilder("Request{");
        b.append("Path: ").append(req.path())
            .append(suffix);
        b.append("Query: ").append(req.query())
            .append(suffix);
        b.append("Method: ").append(req.method())
            .append(suffix);
        for (Entry<String, String> header : req.headers().entries()) {
            b.append("Header[").append(header.getKey()).append("=").append(header.getValue()).append("]")
                .append(suffix);
        }
        b.append("URI: ").append(req.uri())
            .append(suffix);
        b.append("Absolute URI: ").append(req.absoluteURI())
            .append(suffix);

        return b.toString();
    }

    private String resolveResponse(HttpServerRequest req, UiContainerExpr response) {
        return response.toSource();
    }

    @Override
    public void shutdown(Do onDone) {
        onShutdown.add(onDone.ignores2());
        webApp.setRunning(false);
        onRelease = onRelease.doAfter(onDone);
    }

    @Override
    public void serviceRequest(
        RequestScopeVertx request, In2<RequestScopeVertx, Throwable> callback
    ) {
            try {

                final String path = request.getPath();
                final WebApp app = getWebApp();

                // TODO: compile routes into a trie which supports * and **
                // AKA: Finish PathTrie class...
                Iterator<Route> itr = app.getRoute().forEach().iterator();

                final Many<Route> routeScore = X_Collect.newIntMultiMap(Route.class, X_Collect.MUTABLE_KEY_ORDERED);

                while ( itr.hasNext() ) {
                    Route route = itr.next();
                    double match = route.matches(path);
                    assert match <= 1 : "Do not return match score greater than 1; you sent " + match;
                    if (match > 0) {
                        int normalized = -(int)((Integer.MAX_VALUE) * match);
                        routeScore.add(normalized, route);
                    }
                }
                final Iterator<Route> routes = routeScore
                    .flatten(X_Fu::<MappedIterable<Route>>identity)
                    .iterator();
                serveRoutes(request, routes, callback);
            } catch (Throwable t) {
                callback.in(request, t);
                throw t;
            }
    }

    private void serveRoutes(
        RequestScopeVertx request,
        Iterator<Route> routes,
        In2<RequestScopeVertx, Throwable> callback
    ) {
        if (routes.hasNext()) {
            final Route next = routes.next();
            serveRoute(next, request, routes, callback);
        } else {
            // Failed to route this request
            callback.in(request, new NotFoundException(request.toString()));
        }
    }

    private void serveRoute(
        Route next,
        RequestScopeVertx request,
        Iterator<Route> routes,
        In2<RequestScopeVertx, Throwable> callback
    ) {
        X_Time.runLater(()->{
            final XapiServer was = request.setLocal(XapiServer.class, this);
            // TODO cleanup server reference later...
            next.serve(request.getPath(), request, (s, t)->{
                if (t == null) {
                    // success
                    callback.in(request, null);
                } else {
                    if (routes.hasNext()){
                        // There are still backup routes to try...
                        serveRoutes(request, routes, (scope, previousT) -> {
                            // Spy on the final result; if we ultimately failed, get loud
                            if (previousT == null) {
                                callback.in(scope, null);
                                request.removeLocal(XapiServer.class);
                            } else {
                                X_Log.warn(XapiVertxServer.class, "Route reported error", next, t);
                                callback.in(scope, MultiException.mergedThrowable("Multiple routes matched and failed",
                                    t, previousT));
                            }
                        });
                    } else {
                        // Final route failed... get loud immediately.
                        X_Log.warn(XapiVertxServer.class, "Route reported error", next, t);
                        callback.in(request, t);
                        request.removeLocal(XapiServer.class);
                    }
                }
            });
        });
    }

    @Override
    public void writeText(
        RequestScopeVertx request, String payload, In2<RequestScopeVertx, Throwable> callback
    ) {
        try {
            request.getResponse().buildRawResponse().append(payload);
        } catch (Throwable e) {
            callback.in(request, e);
            return;
        }
        callback.in(request, null);
    }

    @Override
    public void writeTemplate(
        RequestScopeVertx request, String payload, In2<RequestScopeVertx, Throwable> callback
    ) {
        try {

            if (payload.trim().startsWith("<")) {
                // We have a xapi template to parse and run...
            } else if (payload.contains("$")) {
                // Swap out any $named variables, if any, from our scope
                final ClassTo<Object> vars = X_Collect.newClassMap(Object.class);
                request.loadMap(vars.asMap());

                MappedTemplate t = new MappedTemplate(payload);


                StringTo<Object> replaceables = X_Collect.newStringMap(Object.class);

                for (Class<?> key : vars.keys()) {
                    String name = computeTemplateKey(key);
                    if (name != null) {
                        if (payload.contains("$" + name)) {
                            // Payload has a potential match for this value
                        }
                        if (payload.contains("${" + name)) {
                            // Payload might have a nested value for this type... computation of nesting will be nasty.
                            // consider outsourcing this to a dependency...
                        }
                    }
                }

            } else {
            }
        } catch (Throwable t) {
            callback.in(request, t);
        }

       writeText(request, payload, callback);
    }

    private String computeTemplateKey(Class<?> key) {
        final IsModel model = key.getAnnotation(IsModel.class);
        if (model != null) {
            return model.modelType();
        }
        if (Model.class.isAssignableFrom(key)) {
            // A model w/out an IsModel annotation...
            // Turn the simplename into a model type
            return X_String.firstCharToLowercase(key.getSimpleName().replace("Model", ""));
        }
        return null;
    }

    @Override
    public void writeFile(
        RequestScopeVertx request, String payload, In2<RequestScopeVertx, Throwable> callback
    ) {
        final HttpServerResponse response = request.getRequest().getResponse();
        File toServe = new File(webApp.getContentRoot());
        if (!toServe.exists()) {
            throw new IllegalStateException("Content root " + toServe + " does not exist!");
        }
        toServe = new File(webApp.getContentRoot(), payload);
        if (!toServe.exists()) {
            toServe = new File(payload.startsWith("/") ? payload.substring(1) : payload);
            if (!toServe.exists()) {
                throw new IllegalStateException("Content file " + new File(webApp.getContentRoot(), payload) + " does not exist!");
            }
        }
        final String path = toServe.getAbsolutePath();
        request.getResponse().onFinish(resp-> {
                resp.prepareToClose();
                response.sendFile(path);
            }
        );
        callback.in(request, null);
    }

    @Override
    public void writeGwtJs(
        RequestScopeVertx request, String payload, In2<RequestScopeVertx, Throwable> callback
    ) {
        final ModelGwtc module = getWebApp().getOrCreateGwtModules().get(payload);
        if (module == null) {
            throw new IllegalArgumentException("No gwt app registered for id " + payload);
        }
        writeGwtJs(request, payload, module, callback);
    }

    private void writeGwtJs(RequestScopeVertx scope, String payload, ModelGwtc module, In2<RequestScopeVertx, Throwable> callback) {
        final VertxRequest req = scope.getRequest();
        final VertxResponse resp = scope.getResponse();
        String path = req.getPath();
        final GwtcService service = module.getOrCreateService();
        // request was for the nocache file; compile every time (for now)
        assert !resp.getResponse().headWritten() : "Head already written!";
        X_Process.runDeferred(()->{
                final GwtManifest manifest = module.getOrCreateManifest();
                final GwtcJobManager manager = service.getJobManager();
                final GwtcJob job = manager.getJob(manifest.getModuleName());

                assert !resp.getResponse().headWritten() : "Head already written!!";

                Duration maxTime = gwtMaxCompileTime();

                boolean rootLoad = path.contains(".nocache.js");
                service.doCompile(
                    !rootLoad,
                    manifest, maxTime.toMillis(), TimeUnit.MILLISECONDS, (result, err)->{
                    if (err == null) {
                        manifest.setCompileDirectory(result);

                        boolean done = false;

                        assert !resp.getResponse().headWritten() : "Head already written!!!";

                        if (rootLoad) {

                            Path file = Paths.get(manifest.getCompiledWar());
                            file = file.resolve(payload + File.separatorChar + payload + ".nocache.js");
                            done = true;
                            resp.prepareToClose();
                            resp.getResponse().sendFile(
                                file.normalize().toString()
                            );
                            callback.in(scope, null);
                        } else {
                            // request was for a file from the compile result;
                            // serve it up...
                            String newUrl = path.startsWith("/") ? path.substring(1) : path;
                            Path dir = Paths.get(newUrl.startsWith("sourcemaps") ? manifest.getCompileDirectory().getSourceMapDir() : manifest.getCompiledWar());
                            Path file = dir.resolve(newUrl);
                            if (Files.exists(file)) {
                                final String normalized = file.normalize().toString();
                                resp.prepareToClose();
                                resp.getResponse().sendFile(normalized);
                            } else if (newUrl.startsWith("sourcemaps")){
                                if (newUrl.endsWith("json")) {
                                    final String fileName = X_String.chopEndOrReturnEmpty(newUrl, "/")
                                        // not really sure why super dev mode does this...
                                        .replace("_sourcemap.json", "_sourceMap0.json");
                                    file = dir.resolve(fileName);
                                    if (Files.exists(file)) {
                                        final String normalized = file.normalize().toString();
                                        resp.prepareToClose();
                                        resp.getResponse().sendFile(normalized);
                                    } else {
                                        X_Log.warn(XapiVertxServer.class, "No file to serve for ", file, "from url", newUrl);
                                    }
                                } else {
                                    // This url is for an actual source file.
                                    int ind = newUrl.indexOf("$sourceroot_goes_here$");
                                    if (ind == -1) {
                                        ind = newUrl.indexOf('/');
                                        ind = newUrl.indexOf('/', ind+1);
                                    }
                                    ind = newUrl.indexOf('/', ind+1);
                                    String fileName = newUrl.substring(ind+1);
                                    done = true;
                                    job.requestResource(fileName, (url, fail)->{
                                        if (fail instanceof NotFoundException) {
                                            if (fileName.startsWith("gen/")) {
                                                Path genPath = Paths.get(manifest.getCompileDirectory().getGenDir());
                                                final Path genFile = genPath.resolve(fileName.substring(4));
                                                if (Files.exists(genFile)) {
                                                    resp.prepareToClose();
                                                    resp.getResponse().sendFile(genFile.toString());
                                                    callback.in(scope, null);
                                                    return;
                                                } else {
                                                    X_Log.warn(XapiVertxServer.class, "No file found for path", genFile);
                                                }
                                            }
                                        } else if (fail == null){
                                            // In case this is a url into a jar,
                                            // we save ourselves some headache, and just stream from whatever URL we get
                                            try (
                                                final InputStream in = url.openStream()
                                            ) {
                                                final byte[] bytes = X_IO.toByteArray(in);
                                                resp.prepareToClose();
                                                resp.getResponse().end(Buffer.buffer(bytes));
                                                callback.in(scope, null);
                                                return;
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        } else {
                                            X_Log.error(XapiVertxServer.class, "Failed sending source", fileName);
                                            callback.in(scope, fail);
                                        }
                                    });
                                }
                            } else {
                                X_Log.warn(XapiVertxServer.class, "No file to serve for ", file, "from url", newUrl);
                            }
                            if (!done && !resp.getResponse().headWritten()) {
                                callback.in(scope, null);
                            }
                        }


                    } else {
                        X_Log.error(XapiVertxServer.class, "Gwt compilation failed", err);
                        throw new IllegalStateException("Gwt compile failed for manifest " + manifest, err);
                    }
                });
            });
    }

    protected Duration gwtMaxCompileTime() {
        return Duration.of(5, ChronoUnit.MINUTES); // hopefully optimistic...
    }

    @Override
    public void writeCallback(
        RequestScopeVertx request, String payload, In2<RequestScopeVertx, Throwable> callback
    ) {
        callback.in(request, null);
    }

    @Override
    public void writeService(
        String path, RequestScopeVertx request, String payload, In2<RequestScopeVertx, Throwable> callback
    ) {
        final XapiEndpoint endpoint = findEndpoint(payload);
        if (endpoint == null) {
            X_Log.warn(XapiVertxServer.class, "No endpoint found for", payload);
        } else {
            endpoint.serviceRequest(path, request, payload, callback);
        }
    }
}
