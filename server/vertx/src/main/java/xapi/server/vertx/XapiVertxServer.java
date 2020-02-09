package xapi.server.vertx;

import com.github.javaparser.ast.expr.UiContainerExpr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.shareddata.Counter;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.ClusteredSessionStore;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import xapi.annotation.model.IsModel;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.collect.api.IntTo.Many;
import xapi.collect.api.StringTo;
import xapi.dev.gwtc.api.GwtcJob;
import xapi.dev.gwtc.api.GwtcJobManager;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.gwtc.api.JobCanceledException;
import xapi.dev.source.PrintBuffer;
import xapi.except.MultiException;
import xapi.except.NoSuchItem;
import xapi.fu.*;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.In3.In3Unsafe;
import xapi.fu.data.MapLike;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.SizedIterator;
import xapi.fu.itr.MappedIterable;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.io.impl.IOCallbackDefault;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelNotFoundException;
import xapi.model.api.PrimitiveSerializer;
import xapi.process.X_Process;
import xapi.scope.X_Scope;
import xapi.scope.api.HasRequestContext;
import xapi.scope.api.Scope;
import xapi.scope.request.SessionScope;
import xapi.scope.service.ScopeService;
import xapi.scope.spi.RequestContext;
import xapi.server.X_Server;
import xapi.server.api.*;
import xapi.server.api.Route.RouteType;
import xapi.server.model.ModelSession;
import xapi.server.vertx.scope.RequestScopeVertx;
import xapi.server.vertx.scope.ScopeServiceVertx;
import xapi.server.vertx.scope.SessionScopeVertx;
import xapi.source.template.MappedTemplate;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Properties;
import xapi.util.X_String;
import xapi.util.X_Util;
import xapi.util.api.ErrorHandler;
import xapi.util.api.SuccessHandler;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import static io.vertx.core.Future.succeededFuture;
import static xapi.util.X_String.ensureEndsWith;
import static xapi.util.X_String.ensureStartsWith;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public class XapiVertxServer extends AbstractXapiServer<RequestScopeVertx> {

    static {
        X_Model.register(ModelSession.class);
    }

    private static final String SESSION_KEY = X_Properties.getProperty("xapi.session.key", "xaz");
    private static final String INSTANCE_ID_COUNTER = "iids";
    private static final String EXPIRED = "Thu, 01 Jan 1970 00:00:00 GMT";
    // This is only used in non-clustered mode, and would benefit from a persistent source
    // to ensure uniqueness and monotonicity across server starts.
    private static AtomicLong ts = new AtomicLong(System.nanoTime()/1024);
    private final WebApp webApp;
    private final PrimitiveSerializer primitives;
    private Vertx vertx;
    private In1<DoUnsafe> exe;
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
    private Router router;

    public XapiVertxServer(WebApp webApp) {
        this.webApp = webApp;
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
    public void onRelease() {
        onRelease.done();
        onRelease = Do.unsafe(()->{
            throw new IllegalStateException("Server already released");
        });
    }

    @Override
    public WebApp getWebApp() {
        return webApp;
    }

    @Override
    // This terrrrible type parameter can go away once we rewire the Request driven model to use RequestContext
    public <C extends RequestContext> void inContext(
        C ctx, In1Out1<C, RequestScopeVertx> factory, In3Unsafe<RequestScopeVertx, Throwable, In1<Throwable>> callback
    ) {
        final ScopeServiceVertx scopes = getScopeService();
        final Scope current = scopes.currentScope();
        // TODO: check if this is already a request scope,
        // and instead skip ahead to req.initialize()

        // MAYBE: Test what happens if we remove this global scope wrapper (most likely sessions will go away...
        // should do that anyway, and then just make session access handle ensuring the global scope...)
        scopes.globalScopeVertx(global->{
            final RequestScopeVertx req = factory.io(ctx);
            scopes.runInScope(req, (session, done)->{
                Throwable initError = null;
                final XapiServer was = req.setLocal(XapiServer.class, this);
                VertxContext vc = (VertxContext) ctx;
                try {
                    // this goes away when we change the type parameter of XapiVertxServer
                    req.initialize(vc.getRequest(), vc.getResponse());
                } catch (Throwable t) {
                    initError = t;
                }
                callback.in(req, initError, error->{
                    if (was == null) {
                        req.removeLocal(XapiServer.class);
                    } else {
                        req.setLocal(XapiServer.class, was);
                    }
                    if (error != null) {
                        handleError(req.getRequest().getHttpRequest(), vc.getResponse(), error);
                    }
                    // finish response
                    Integer code = vc.finish(error);
                    final Out1<String> dump = ()->XapiVertxServer.dump(req.getRequest().getHttpRequest());
                    final Out1<String> user = ()->ctx.getUser() == null ? "anon user" : ctx.getUser().toString();
                    if (VertxContext.SUCCESS.equals(code)) {
                        X_Log.debug(XapiVertxServer.class, user, ", request succeeded for ", req.getPath());
                    } else if (code == VertxContext.CANCELLED) {
                        X_Log.trace(XapiVertxServer.class, user, ", cancelled request for ", req.getPath());
                    } else if (code == 307) {
                        X_Log.trace(XapiVertxServer.class, user, ", request redirected to", ctx.getResponse().getHeaders().get("Location"), ctx, "\n", dump);
                    } else if (code == 404) {
                        X_Log.trace(XapiVertxServer.class, user, ", nothing to serve for ", dump);
                    } else {
                        X_Log.warn(XapiVertxServer.class, user, ", request failed for ", ctx, "\n", dump, " had error:\n", error);
                    }
                    // release scope
                    done.done();
                });
            });
        });
    }

    @Override
    public void inScope(
        String sessionId, In1Out1<SessionScope, RequestScopeVertx> scopeFactory, In3Unsafe<RequestScopeVertx, Throwable, Do> callback
    ) {
        final ScopeServiceVertx scopes = getScopeService();
        scopes.globalScopeVertx(global->{
            final In2Unsafe<SessionScopeVertx, Do> doWork = (session, done)->{

                final RequestScopeVertx req = scopeFactory.io(session);
                final XapiServer was = req.setLocal(XapiServer.class, this);
                final VertxRequest nativeReq = req.getRequest();
                final MapLike<String, String> cookies = nativeReq.getCookies();
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
                        req.getResponse().addHeader("Set-Cookie", expireCookie(SESSION_KEY));
                        finish(e);
                    }

                    private void finish(Throwable e) {
                        callback
                            .in(req, e, ()->{
                                if (was == null) {
                                    req.removeLocal(XapiServer.class);
                                } else {
                                    req.setLocal(XapiServer.class, was);
                                }
                                done.done();
                            });
                    }
                };
                // TODO: if a request does not require a session, don't bother loading one.
                // for now, we're just going to pay every time for speedier development,
                // and will optimize once the platform matures
                final SessionHandler nativeSession = req.get(SessionHandler.class);
                nativeSession.setSessionCookieName(SESSION_KEY);

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
                req.getResponse().addHeader("Set-Cookie", SESSION_KEY+"="+X_String.encodeURIComponent(sessionKey)+";path=/;");
                model = ModelSession.SESSION_MODEL_BUILDER.out1().buildModel();
                model.getKey().setId(sessionKey);
                model.setTouched(System.currentTimeMillis());
                session.setModel(model);
                X_Model.persist(model, saved);
            };
            final SessionScopeVertx session =  global.findOrCreateSession(sessionId, id->{
                final SessionScope<User, VertxRequest, VertxResponse> s = X_Inject.instance(SessionScope.class);
                return (SessionScopeVertx) s;
            });
            scopes.runInScope(session, doWork);
        });
    }

    public static String expireCookie(String cookieKey) {
        return cookieKey + "=;path=/;expires=" + EXPIRED + ";";
    }

    protected ScopeServiceVertx getScopeService() {
        final ScopeService service = X_Scope.service();
        assert service instanceof ScopeServiceVertx : "The scope service was not a ScopeServiceVertx; you have some other " +
            "scope wiring that isn't going to work; where you extend XapiVertxServer, override getScopeService() and " +
            "return your own singleton ScopeServiceVertx; you have: " +
            (service == null ? "" : service.getClass()) + " : " + service;
        return (ScopeServiceVertx) service;
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
                .setBlockedThreadCheckInterval(600_000)
//                .setBlockedThreadCheckInterval(2000)
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
                SessionStore sessions;
                if (clusterManager == null) {
                    sessions = LocalSessionStore.create(vertx);
                } else {
                    scope.setLocal(ClusterManager.class, clusterManager);
                    sessions = ClusteredSessionStore.create(vertx);
                }
                scope.setLocal(WebApp.class, webApp);
                scope.setLocal(XapiServer.class, XapiVertxServer.this);

                router = Router.router(vertx);
                // TODO: provide alternate cookie handler, which does not use cookies,
                // and instead uses an alternate means of identifying user connection
                // (for example, add a sequence of header ids that are all one-time use keys,
                // computed from some entropy given from the server).
                router.route().handler(CookieHandler.create());
                SessionHandler sessionHandler = SessionHandler.create(sessions);
                router.route().handler(sessionHandler);

                installRoutesOverride(router);

                // reconcile usage of * so we can do this only for selected endpoints...
                router.route().method(HttpMethod.POST).method(HttpMethod.PUT).handler(BodyHandler.create());

                // Go over our routes, and pick out the ones we can wire up direct routing for,
                // as vertx should be a much faster lookup than going through abstraction layer.
                for (
                    final SizedIterator<Route> itr = webApp.getRoute().iterator();
                    itr.hasNext();
                    ) {
                    final Route route = itr.next();
                    // Move as much brains as possible here, as router.route() calls.
                    // while still leaving behind the manual implementation
                    // so that containers w/out routing infrastructure
                    // can still leverage the manual routing we are doing today.
                    if (route.getPath().contains("*")) {
                        // xapi uses ant-style path matcher, * and **; vertx allows regex
                        continue;
                    }
                    final String path = route.getPath();
                    final io.vertx.ext.web.Route r = router.route(path);

                    final IntTo<String> methods = route.getMethods();
                    if (methods != null && methods.isNotEmpty()) {
                        for (String method : methods) {
                            final HttpMethod m = HttpMethod.valueOf(method.toUpperCase());
                            r.method(m);
                            switch (m) {
                                case POST:
                                case PUT:
                                    // TODO: include uploadDir from WtiServerConfig
                                    r.handler(BodyHandler.create());
                            }
                        }
                    }
                    final RouteType type = route.getRouteType();
                    In1Out1<Handler<RoutingContext>, io.vertx.ext.web.Route> target =
                        type.isBlocking() ? r::blockingHandler : r::handler;
                    target.io(rc->{
                        final VertxContext vc = VertxContext.fromNative(rc);
                        switch (route.getRouteType()) {
                            case Reroute:
                                X_Log.trace(XapiVertxServer.class, "Rerouting", path, " to ", route.getPayload());
                                if (route.getPayload().contains("$")) {
                                    // There may be a template key in the redirection, we'll handle it in xapi server,
                                    // which will be forced to issue a 307 redirect, instead of internal vertx rerouting.
                                    inContext(vc, VertxContext::getScope, (req, t, done)->{
                                        if (t != null) {
                                            done.in(t);
                                            return;
                                        }
                                        // TODO: move everybody into here, and make HasRequestContext the default "thing to pass around"
                                        route.serveWithContext(path, vc, (s, err)-> done.in(err));
                                    });
                                } else {
                                    rc.reroute(route.getPayload());
                                }
                                break;
                            case Gwt:
                            case Text:
                            case Callback:
                            case File:
                            case Directory:
                            case Template:
                            case Service:
                                inContext(vc, VertxContext::getScope, (req, t, done)->{
                                    if (t != null) {
                                        done.in(t);
                                        return;
                                    }
                                    route.serve(path, req, (s, err)-> done.in(err));
                                });
                                break;
                            default:
                                assert false : route.getRouteType() + " is not implemented by " + getClass();
                        }
                    });
                    // remove this route from the mapping, as we've chosen to handle it here.
                    itr.remove();

                } // end for-loop

                // Now, if the above routes did not complete, delegate to standard xapi infrastructure,
                // for features that vertx either doesn't support, or is not (currently) worth the effort to delegate to.
                initializeEndpoints(scope);

                router.route().handler(this::onRequest);

                router.route().failureHandler(rc->{
                    final VertxContext vc = VertxContext.fromNative(rc);
                    final Throwable failure = rc.failure();
                    final VertxResponse resp = vc.getResponse();
                    assert resp.getHttpResponse() == rc.response() : "Stale context " + vc + " has incorrect response";
                    final int code = resp.getStatusCode() == VertxContext.SUCCESS ? VertxContext.FAILURE : resp.getStatusCode();
                    resp.setStatusCode(code);

                    final String returnedBody = resp.clearResponseBody();
                    final PrintBuffer out = resp.buildErrorResponse();
                    String msg = failure.getMessage();
                    if (msg == null || msg.isEmpty()) {
                        msg = failure.toString();
                    } else if (msg.length() < 10) {
                        msg = failure + " : " + msg;
                    }
                    if (expectedFailure(resp, failure)) {
                        X_Log.trace(XapiVertxServer.class, "Request failed", vc, failure);
                    } else {
                        X_Log.warn(XapiVertxServer.class, "Request failed", vc, failure);
                    }
                    out.clear().append(msg);
                    if (X_String.isNotEmptyTrimmed(returnedBody)) {
                        // TODO: make this configurably hidden, or maybe non-visible to non-developers.
                        out
                            .append("<pre>Partial Response:\n")
                            .append(returnedBody)
                            .append("</pre>");
                    }

                    resp.finish(code);

                    // lets clean up after ourselves...
                    rc.data().remove(VertxContext.CONTEXT_KEY);
            });

                installRoutesBackup(router);

                vertx.createHttpServer(opts)
                    .requestHandler(router::accept)
                    .listen(result->{
                        if (result.failed()) {
                            throw new IllegalStateException("Server failed to start ", result.cause());
                        }
                        onStarted(vertx, result.result());


                        X_Log.info(XapiVertxServer.class, "Server up and running at 0.0.0.0:" + webApp.getPort());

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

    private boolean expectedFailure(VertxResponse resp, Throwable failure) {
        return resp.getStatusCode() == 404;
    }

    @Override
    protected void initializeEndpoint(
        String path, XapiEndpoint<RequestScopeVertx> endpoint, Scope scope
    ) {
        // add us to the router as a high priority call.
        // TODO: have http methods and content type matching from the endpoint done here.
        final String pathRegex;
        final Handler<RoutingContext> handler = rc -> {
            final VertxContext vc = VertxContext.fromNative(rc);
            endpoint.serviceRequest(rc.normalisedPath(), vc.getScope(), path, (finalScope, fail) -> {
                try {
                    if (fail == null) {
                        endpoint.onSuccess(finalScope);
                        vc.finish(null);
                    } else {
                        // give endpoint a chance to recover...
                        Throwable error = endpoint.onFail(finalScope, fail);
                        vc.finish(error);
                    }
                } catch (Throwable t) {
                    X_Log.error(XapiVertxServer.class, "Fatal error in endpoint/controller code", t,
                        "\nEndpoint: ", path, ", context: ", vc);
                }
            });
        };
        if (endpoint.isContextPath()) {
            pathRegex = ensureEndsWith(ensureStartsWith(path, "/"), "/.*");
            router.routeWithRegex(pathRegex).handler(handler);
        } else {
            pathRegex = ensureStartsWith(path, "/");
            router.route(pathRegex).handler(handler);
        }

        super.initializeEndpoint(path, endpoint, scope);
    }

    protected void installRoutesOverride(Router router) {
    }

    protected void installRoutesBackup(Router router) {
    }

    public void onStarted(In2<Vertx, HttpServer> callback) {
        assert !onStarted.anyMatch(callback::equals) : "Duplicate onStarted callback added: " + callback;
        onStarted.add(callback);
    }

    protected void onStarted(Vertx vertx, HttpServer result) {
        onStarted.forAll(In2::in, vertx, result);
    }

    protected void onRequest(RoutingContext ctx) {
        VertxContext vc = VertxContext.fromNative(ctx);
        inContext(vc, VertxContext::getScope, (scope, t, done) -> {
            if (t == null) {
                serviceRequest(scope, (r, error)-> done.in(error));
            } else {
                ctx.fail(t);
            }
        });
    }

    protected void handleError(HttpServerRequest req, VertxResponse resp, Throwable error) {
            final String body = "<!DOCTYPE html><html>" +
                "<body style=\"text-align: center; vertical-align: middle; display: block\">" +
                "<div style=\"white-space:pre-wrap\">" +
                (error instanceof NoSuchItem ?
                "Our apologies, we cannot find anything to serve to:" :
                "Request encountered an error: " + error) +
                "\n" +
                dump(req) +
                "</div>" +
                "<pre>" +
                error +
                "</pre>" +
                "</body>" +
                "</html>";
            resp.buildErrorResponse()
                .print(body);
            resp.setStatusCode(404);
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
            callback.in(request, new NoSuchItem(request.getRequest().getPath()));
        }
    }

    private void serveRoute(
        Route next,
        RequestScopeVertx request,
        Iterator<Route> routes,
        In2<RequestScopeVertx, Throwable> callback
    ) {
        final Do restore = request.captureScope();
        X_Time.runLater(()->{
            restore.done();
            final XapiServer was = request.setLocal(XapiServer.class, this);
            // we'll cleanup server reference later...
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
                        // TODO: "catch routes"
                        if (t instanceof JobCanceledException) {
                            X_Log.trace(XapiVertxServer.class, "Request canceled for ", request.getPath());
                        } else {
                            X_Log.warn(XapiVertxServer.class, "Route reported error", next, t);
                        }
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

    protected void writeFileSystem(
        boolean allowDirListing, RequestScopeVertx request, String payload, In2<RequestScopeVertx, Throwable> callback
    ) {
        final HttpServerResponse response = request.getRequest().getResponse();
        File toServe = new File(webApp.getContentRoot());
        if (!toServe.exists()) {
            throw new IllegalStateException("Content root " + toServe + " does not exist!");
        }
        toServe = new File(webApp.getContentRoot(), payload);
        if (!toServe.exists()) {
            boolean allowAbsolute = webApp.allowAbsolute(request.getPath());
            toServe = new File(!allowAbsolute && payload.startsWith("/") ? payload.substring(1) : payload);
            if (!toServe.exists()) {
                throw new IllegalStateException("Content file " + (allowAbsolute ? toServe : new File(webApp.getContentRoot(), payload)) + " does not exist!");
            }
        }
        final File finalPath = toServe;
        request.getResponse().onFinish(resp-> {
                resp.prepareToClose();
                if (finalPath.isDirectory()) {
                    if (allowDirListing) {
                        response.setChunked(true);
                        response.write("<html><body>");
                        for (File file : Objects.requireNonNull(finalPath.listFiles())) {
                            response.write("<a href=\"" +
                                request.getPath() + (request.getPath().endsWith("/") ? "" : "/") + file.getName() +
                             "\">" + file.getName() + " </a><br/>");

                        }
                        response.end("</body></html>");
                    } else {
                        X_Log.error(XapiVertxServer.class, "Disallowed viewing of ", finalPath);
                        response.setStatusCode(503).end("Not allowed to view " + finalPath.toString());
                    }
                } else {
                    response.sendFile(finalPath.getAbsolutePath());
                }
            }
        );
        callback.in(request, null);
    }

    @Override
    public void writeFile(
        RequestScopeVertx request, String payload, In2<RequestScopeVertx, Throwable> callback
    ) {
        X_Log.error("Writing file request", payload);
        writeFileSystem(false, request, payload, callback);
    }
    @Override
    public void writeDirectory(
        RequestScopeVertx request, String payload, In2<RequestScopeVertx, Throwable> callback
    ) {
        X_Log.error("Writing directory request", payload);
        writeFileSystem(true, request, payload, callback);
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
        final Moment start = X_Time.now();
        final VertxRequest req = scope.getRequest();
        final VertxResponse resp = scope.getResponse();
        String path = req.getPath();

        final GwtcService service = module.getOrCreateService();
        // request was for the nocache file; compile every time (for now)
        assert !resp.getHttpResponse().headWritten() : "Head already written!";

        final In2<CompiledDirectory, Throwable> writeResponse = (result, err)->{

            final GwtManifest manifest = module.getOrCreateManifest();
            final GwtcJobManager manager = service.getJobManager();
            boolean rootLoad = path.contains(".nocache.js");

            X_Log.log(XapiVertxServer.class,
                path.contains("nocache.js") ? LogLevel.INFO : LogLevel.TRACE,
                "Gwtc request", path, "serviced in ", X_Time.difference(start));
            if(result != null) {
                manifest.setCompileDirectory(result);
            }
            if (err != null) {
                X_Log.error(XapiVertxServer.class, "Gwt compilation failed", err);
                callback.in(scope, err);
                return;
            }
            if (resp.getHttpResponse().closed()) {
                // user already closed response / gave up; underlying connection is dead...
                if (!resp.isClosed()) {
                    // if we didn't close our scope, we should do so now... (may need to short circuit some handling here...)
                    callback.in(scope, new JobCanceledException());
                }
                return;
            }
            boolean done = false;

            assert !resp.getHttpResponse().headWritten() : "Head already written!!!";

            if (rootLoad) {

                Path file = Paths.get(manifest.getCompiledWar());
                file = file.resolve(payload + File.separatorChar + payload + ".nocache.js");
                done = true;
                resp.prepareToClose();
                resp.getHttpResponse().sendFile(
                    file.normalize().toString()
                );
            } else {
                // request was for a file from the compile result;
                // serve it up...
                String newUrl = path.startsWith("/") ? path.substring(1) : path;
                Path dir = Paths.get(newUrl.startsWith("sourcemaps") ? manifest.getCompileDirectory().getSourceMapDir() : manifest.getCompiledWar());
                Path file = dir.resolve(newUrl);
                if (Files.exists(file)) {
                    final String normalized = file.normalize().toString();
                    resp.prepareToClose();
                    resp.getHttpResponse().sendFile(normalized);
                } else if (newUrl.startsWith("sourcemaps")){
                    if (newUrl.endsWith("json")) {
                        final String fileName = X_String.chopEndOrReturnEmpty(newUrl, "/")
                            // not really sure why super dev mode does this...
                            .replace("_sourcemap.json", "_sourceMap0.json");
                        file = dir.resolve(fileName);
                        if (Files.exists(file)) {
                            final String normalized = file.normalize().toString();
                            resp.prepareToClose();
                            resp.getHttpResponse().sendFile(normalized);
                        } else {
                            X_Log.warn(XapiVertxServer.class, "No file to serve for ", file, "from url", newUrl);
                            callback.in(scope, new NoSuchItem(file));
                            return;
                        }
                    } else {
                        final GwtcJob job = manager.getJob(manifest.getModuleName());

                        if (job == null) {
                            callback.in(scope, new IllegalStateException("No job for " + manifest.getModuleName()));
                            return;
                        }
                        // This url is for an actual source file.
                        int ind = newUrl.indexOf("$sourceroot_goes_here$");
                        if (ind == -1) {
                            ind = newUrl.indexOf('/');
                            ind = newUrl.indexOf('/', ind+1);
                        }
                        ind = newUrl.indexOf('/', ind+1);
                        String fileName = newUrl.substring(ind+1);
                        done = true;
                        final DoUnsafe blockOnRequest = job.requestResource(fileName, (url, fail) -> {
                            if (fail instanceof NoSuchItem) {
                                if (fileName.startsWith("gen/")) {
                                    Path genPath = Paths.get(manifest.getCompileDirectory().getGenDir());
                                    final Path genFile = genPath.resolve(fileName.substring(4));
                                    if (Files.exists(genFile)) {
                                        resp.prepareToClose();
                                        resp.getHttpResponse().sendFile(genFile.toString());
                                        callback.in(scope, null);
                                    } else {
                                        X_Log.warn(
                                            XapiVertxServer.class,
                                            "No file found for path",
                                            genFile
                                        );
                                        callback.in(scope, new NoSuchItem(genFile));
                                    }
                                } else {
                                    callback.in(scope, fail);
                                }
                            } else if (fail == null) {
                                // In case this is a url into a jar,
                                // we save ourselves some headache, and just stream from whatever URL we get
                                boolean calledBack = false;
                                try (
                                    final InputStream in = url.openStream()
                                ) {
                                    final byte[] bytes = X_IO.toByteArray(in);
                                    resp.prepareToClose();
                                    resp.getHttpResponse().end(Buffer.buffer(bytes));
                                    calledBack = true;
                                    callback.in(scope, null);
                                } catch (Exception e) {
                                    if (calledBack) {
                                        X_Log.error(XapiVertxServer.class, "Failed sending source", fileName, e);
                                    } else {
                                        callback.in(scope, e);
                                    }
                                }
                            } else {
                                X_Log.error(XapiVertxServer.class, "Failed sending source", fileName);
                                callback.in(scope, fail);
                            }
                        });
                        X_Process.runDeferred(blockOnRequest);
                    }
                } else {
                    X_Log.warn(XapiVertxServer.class, "No file to serve for ", file, "from url", newUrl);
                    done = true;
                    callback.in(scope, new NoSuchItem(file));
                }
            }
            if (!done) {
                callback.in(scope, null);
            }

        }; // end callback


        if (module.getPrecompileLocation() != null) {
            // There is a precompile location... use it.
            // Because this location is set, we are guaranteed to take the fast path in this method,
            // so we don't have to worry about getting off server event loop thread.
            module.getCompiledDirectory(writeResponse);
        } else {
            // We may do a full/recompile, so lets get off thread where it's safe to block.
            X_Process.runDeferred(()->{
                assert !resp.getHttpResponse().headWritten() : "Head already written!!";

                Duration maxTime = gwtMaxCompileTime();

                final GwtManifest manifest = module.getOrCreateManifest();
                boolean rootLoad = path.contains(".nocache.js");
                X_Log.debug(XapiVertxServer.class, "Prepare thread took", X_Time.difference(start));
                service.doCompile(
                        !rootLoad,
                        manifest, maxTime.toMillis(), TimeUnit.MILLISECONDS, writeResponse.doAfterMe((dir,err)->{
                            if (err == null) {
                                // Now that a successful recompile has occurred, fixup the precompile location,
                                // so everybody asks the service for latest code.  In production, this probably
                                // should not happen (i.e. disable recompiler altogether).
                                // However, we leave it hooked up so that we can use secret hooks to force a recompile,
                                // and then force all user traffic to pick up the latest precompileLocation.
                                if (module.isRecompileAllowed()) {
                                    // forces everyone to keep asking the service for a freshness check.
                                    module.setPrecompileLocation(null);
                                } else {
                                    // TODO: check that this is correct
                                    module.setPrecompileLocation(dir.getWarDir());
                                }
                            }
                    }));
                });
        }

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
            callback.in(request, new IllegalArgumentException("No endpoint: " + path));
        } else {
            endpoint.serviceRequest(path, request, payload, callback);
        }
    }

    @Override
    public <Req extends HasRequestContext> void reroute(
        Req request, String payload, In2<Req, Throwable> callback
    ) {
        // This dirty hack can go away later...
        VertxContext ctx = (VertxContext) request.getContext();
        final VertxResponse resp = ctx.getResponse();
        // lazy... we should probably template the payload to apply any properties in scope
        resp.addHeader("Location", payload);
        resp.setStatusCode(307);
    }
}
