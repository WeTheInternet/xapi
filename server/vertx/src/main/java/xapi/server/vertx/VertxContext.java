package xapi.server.vertx;

import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import xapi.fu.Lazy;
import xapi.scope.X_Scope;
import xapi.scope.api.RouteNotHandledException;
import xapi.scope.spi.RequestContext;
import xapi.server.vertx.scope.GlobalScopeVertx;
import xapi.server.vertx.scope.RequestScopeVertx;
import xapi.server.vertx.scope.SessionScopeVertx;
import xapi.util.X_String;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 8/12/18 @ 3:38 AM.
 */
public class VertxContext implements RequestContext<User, VertxRequest, VertxResponse, SessionScopeVertx, RequestScopeVertx> {

    public static final Integer SUCCESS = 200;
    private static final String CONTEXT_KEY = "_xvc_";

    private final RoutingContext ctx;
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
            scope.setUser(ctx.user());
            scope.setContext(ctx);
            return scope;
        });
        sess.setUser(ctx.user());
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
        String body = resp.prepareToClose();
        if (failures != null) {
            if (failures instanceof RouteNotHandledException) {
                ctx.next();
            } else {
                ctx.fail(failures);
            }
            int code = ctx.response().getStatusCode();
            resp.finish(code);
            return code;
        }
        if (!X_String.isEmptyTrimmed(body)) {
            resp.getResponse().write(body);
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
        return (VertxContext) ctx.data().computeIfAbsent(CONTEXT_KEY, ignored->new VertxContext(ctx));
    }
}
