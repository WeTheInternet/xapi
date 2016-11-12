package xapi.server.vertx;

import com.github.javaparser.ast.expr.UiContainerExpr;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.In2;
import xapi.fu.Lazy;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.impl.AbstractModel;
import xapi.scope.X_Scope;
import xapi.scope.api.RequestScope;
import xapi.server.X_Server;
import xapi.server.api.XapiServer;
import xapi.server.api.Classpath;
import xapi.server.api.Gwtc;
import xapi.server.api.Route;
import xapi.server.api.WebApp;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public class XapiVertxServer extends AbstractModel implements WebApp, XapiServer<VertxRequest, HttpServerRequest> {

    private Lazy<String[]> propertyNames = Lazy.deferred1(()->
        X_Model.create(WebApp.class).getPropertyNames()
    );
    private Lazy<StringTo<Class<?>>> propertyTypes = Lazy.deferred1(()-> {
        WebApp model = X_Model.create(WebApp.class);
        StringTo<Class<?>> classes = X_Collect.newStringMapInsertionOrdered(Class.class);
        for (String prop : getPropertyNames()) {
            classes.put(prop, model.getPropertyType(prop));
        }
        return classes;
    });
    private Lazy<String> type = Lazy.deferred1(()->
        X_Model.create(WebApp.class).getType()
    );

    @Override
    public void inScope(
        HttpServerRequest req, In1Unsafe<RequestScope<VertxRequest>> callback
    ) {
        X_Scope.service().runInNewScope(RequestScope.class, (scope, done)->{
            final RequestScope<VertxRequest> s = scope;
            VertxRequest forScope = VertxRequest.getOrMake(s.getRequest(), req);
            callback.in(scope);
            done.done();
        });
    }

    @Override
    public StringTo<Classpath> getClasspaths() {
        return getProperty("classpaths");
    }

    @Override
    public WebApp setClasspaths(StringTo<Classpath> classpaths) {
        return (WebApp) setProperty("classpaths", classpaths);
    }

    @Override
    public StringTo<Gwtc> getGwtModules() {
        return getProperty("gwtcs");
    }

    @Override
    public StringTo<Model> getTemplates() {
        return getProperty("templates");
    }

    @Override
    public IntTo<Route> getRoute() {
        return getProperty("route");
    }

    @Override
    public boolean isRunning() {
        return Boolean.TRUE.equals(getProperty("running"));
    }

    @Override
    public WebApp setRunning(boolean running) {
        return (WebApp) setProperty("running", true);
    }

    @Override
    public int getPort() {
        return getProperty("port", 0);
    }

    @Override
    public WebApp setPort(int port) {
        return (WebApp) setProperty("port", port);
    }

    @Override
    public String getSource() {
        return getProperty("source");
    }

    @Override
    public WebApp setSource(String source) {
        return (WebApp) setProperty("source", source);
    }

    @Override
    public void start() {
        WebApp.super.start();
        if (getPort() == 0) {
            X_Server.usePort(p->{
                setPort(p);
                start();
            });
            return;
        }
        final HttpServerOptions opts = new HttpServerOptions()
            .setHost("0.0.0.0")
            .setPort(getPort());

        Vertx.vertx().createHttpServer(opts)
            .requestHandler(this::handleRequest)
        .listen();
    }

    protected void handleRequest(HttpServerRequest req) {
        serviceRequest(req, (r1, r2)->{
            final HttpServerResponse resp = r1.getHttpRequest().response();
            if (!resp.ended()){
                resp.end();
            }
        });
    }

    private String resolveResponse(HttpServerRequest req, UiContainerExpr response) {
        return response.toSource();
    }

    @Override
    public void shutdown() {
        WebApp.super.shutdown();
    }

    @Override
    public String[] getPropertyNames() {
        return propertyNames.out1();
    }

    @Override
    public Class<?> getPropertyType(String key) {
        return propertyTypes.out1().get(key);
    }

    @Override
    public String getType() {
        return type.out1();
    }

    @Override
    public void serviceRequest(
        HttpServerRequest httpServerRequest, In2<VertxRequest, HttpServerRequest> callback
    ) {
    }

    @Override
    public String getPath(VertxRequest req) {
        return req.getHttpRequest().path();
    }

    @Override
    public IntTo<String> getParams(VertxRequest req, String param) {
        return new ListAdapter<>(req.getHttpRequest().params().getAll(param));
    }

    @Override
    public IntTo<String> getHeaders(VertxRequest req, String header) {
        return new ListAdapter<>(req.getHttpRequest().headers().getAll(header));
    }

}
