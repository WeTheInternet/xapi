package xapi.server.vertx;

import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import xapi.except.NoSuchItem;
import xapi.fu.Lazy;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;
import xapi.scope.X_Scope;
import xapi.scope.api.HasRequestContext;
import xapi.scope.spi.RequestContext;
import xapi.server.errors.RedirectionException;
import xapi.server.errors.RouteNotHandledException;
import xapi.server.vertx.scope.GlobalScopeVertx;
import xapi.server.vertx.scope.RequestScopeVertx;
import xapi.server.vertx.scope.SessionScopeVertx;
import xapi.util.X_String;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 8/12/18 @ 3:38 AM.
 */
public class VertxContext implements RequestContext<User, VertxRequest, VertxResponse, SessionScopeVertx, RequestScopeVertx>,
    HasRequestContext<VertxContext> {

    public static final Integer SUCCESS = 200;
    public static final Integer FAILURE = 500;
    public static final String CONTEXT_KEY = "_xvc_";

    private RoutingContext ctx;
    private final VertxRequest req;
    private final VertxResponse resp;
    private final GlobalScopeVertx global;
    private final Lazy<SessionScopeVertx> session;
    private final RequestScopeVertx scope;
    private volatile boolean finished;
    private volatile Throwable failures;

    public VertxContext(RoutingContext ctx) {
        this.ctx = ctx;
        this.req = new VertxRequest(ctx.request());
        this.resp = new VertxResponse(ctx.response());
        resp.setRerouter((route, updateUrl)->{
            if (updateUrl) {
//                String body = resp.prepareToClose();
                resp.setHeader("Location", route);
                // TODO: transfer any onFinish callbacks through a header + event bus handler.
                resp.finish(307);
            } else {
                ctx.reroute(route);
            }
        });

        GlobalScopeVertx[] pntr = {null};
        X_Scope.service().globalScope(global->{
            pntr[0] = (GlobalScopeVertx)global;
        });
        global = pntr[0];

        final Session rawSession = ctx.session();
        final String sessionId = rawSession.id();
        final SessionScopeVertx sess = global.findOrCreateSession(sessionId, id -> {
            final Object existing = ctx.session().data().get(id);
            final SessionScopeVertx scope;
            if (existing != null) {
                // Actually make the data we save to the vertx session some smaller,
                // serializable subset of local values... have a SerializableClassTo for localData,
                // and we'll just copy those into a new SessionScopeVertx instance.
                scope = (SessionScopeVertx) existing;
            } else {
                scope = new SessionScopeVertx();
            }
            scope.setContext(ctx);
            final MapLike mapProxy = X_Jdk.toMap(ctx.session().data());
            scope.setLocal(MapLike.class, mapProxy);
            return scope;
        });
        // TODO: actually use vert.x auth wiring, so we can populate ctx.user() correctly.
        // For now, we rely on our own login code to update the session w/ a user object.
//        sess.setUser(ctx.user());

        // We are only using the lazy here so we can check if it's been resolved by user or not.
        // This can allow us to fail-fast when considering whether to deal w/ session or not.
        session = Lazy.immutable1(sess);
        scope = (RequestScopeVertx) sess.getRequestScope(req,resp);
    }

    @Override
    public User getUser() {
        return ctx.user();
    }

    @Override
    public VertxRequest getRequest() {
        return req;
    }

    @Override
    public VertxResponse getResponse() {
        return resp;
    }

    public GlobalScopeVertx getGlobal() {
        return global;
    }

    @Override
    public SessionScopeVertx getSession() {
        final SessionScopeVertx s = session.out1();
        s.setContext(ctx);
        s.setLocal(MapLike.class, X_Jdk.toMap(ctx.session().data()));
        return s;
    }

    public boolean wasSessionRead() {
        return session.isResolved();
    }

    @Override
    public Integer finish(Throwable failures) {
        assert !this.finished : "Already finished!";
        this.finished = true;
        this.failures = failures;
        if (failures != null) {
            int code = 0;
            boolean autoclose = true;
            if (failures instanceof RouteNotHandledException) {
                ctx.next();
                autoclose = false;
            } else if (failures instanceof RedirectionException) {
                final RedirectionException redirect = (RedirectionException) failures;
                if (!redirect.getBody().isEmpty()) {
                    resp.clearResponseBody();
                    resp.buildHtmlResponse()
                        .getBody()
                        .append("Redirecting to ")
                        .makeAnchor()
                        .setHref(redirect.getLocation())
                        .append(redirect.getBody());
                }
                if (redirect.isUpdateUrl()) {
                    resp.reroute(redirect.getLocation(), true);
                    code = 307;
                    autoclose = !resp.isClosed();
                } else {
                    ctx.reroute(redirect.getLocation());
                    autoclose = false;
                }
            } else {
                if (failures instanceof NoSuchItem) {
                    resp.setStatusCode(code = 404);
                }
                ctx.fail(failures);
            }
            if (code == 0) {
                code = ctx.response().getStatusCode();
            }
            if (code == SUCCESS) {
                code = FAILURE;
            }
            ctx.response().setStatusCode(code);
            if (autoclose && !resp.isClosed()) {
                resp.finish(code);
            }
            return code;
        }
        String body = resp.prepareToClose();
        if (!X_String.isEmptyTrimmed(body)) {
            resp.getHttpResponse().write(body);
        }
        resp.finish(ctx.response().getStatusCode());
        // lets clean up after ourselves...
        ctx.data().remove(CONTEXT_KEY);
        return SUCCESS; // no failure
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public RequestScopeVertx getScope() {
        return scope;
    }

    public static VertxContext fromNative(RoutingContext ctx) {
        return (VertxContext) ctx.data().compute(CONTEXT_KEY, (key, existing)-> {
            if (existing == null) {
                return new VertxContext(ctx);
            } else {
                ((VertxContext)existing).updateContext(ctx);
                return existing;
            }
        });
    }

    private void updateContext(RoutingContext ctx) {
        req.setHttpRequest(ctx.request());
        resp.setHttpResponse(ctx.response());
        this.ctx = ctx;
        finished = false;
        failures = null;
        resp.reset();
        req.reset();
    }

    @Override
    public VertxContext getContext() {
        return this;
    }

    @Override
    public String toString() {
        return "VertxContext{" +
            ", scope=" + scope +
            ", finished=" + finished +
            ", failures=" + failures +
            '}';
    }
}
