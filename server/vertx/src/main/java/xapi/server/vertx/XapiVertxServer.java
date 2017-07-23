package xapi.server.vertx;

import com.github.javaparser.ast.expr.UiContainerExpr;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import xapi.annotation.model.IsModel;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.InitMap;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.collect.impl.InitMapDefault;
import xapi.dev.gwtc.api.GwtcService;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.Do;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.fu.Mutable;
import xapi.fu.X_Fu;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.ServerRecompiler;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.process.X_Process;
import xapi.scope.X_Scope;
import xapi.scope.api.RequestScope;
import xapi.server.X_Server;
import xapi.server.api.ModelGwtc;
import xapi.server.api.Route;
import xapi.server.api.WebApp;
import xapi.server.api.XapiEndpoint;
import xapi.server.api.XapiServer;
import xapi.source.template.MappedTemplate;
import xapi.util.X_String;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.concurrent.locks.LockSupport;

import com.google.gwt.dev.codeserver.CompileStrategy;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public class XapiVertxServer implements XapiServer<VertxRequest, HttpServerRequest> {

    private final WebApp webApp;
    private Vertx vertx;
    private In1<DoUnsafe> exe;
    private final InitMap<String, XapiEndpoint<?>> endpoints;

    public XapiVertxServer(WebApp webApp) {
        this.webApp = webApp;
        endpoints = new InitMapDefault<>(
            X_Fu::identity, this::findEndpoint
        );
    }

    public void registerEndpoint(String name, XapiEndpoint<?> endpoint) {
        endpoints.put(name, endpoint);
    }

    public void registerEndpointFactory(String name, In1Out1<String, XapiEndpoint<?>> endpoint) {
        endpoints.put(name, (path, requestLike, payload, callback) -> {
            final XapiEndpoint realEndpoint = endpoint.io(name);
            realEndpoint.serviceRequest(path, requestLike, payload, callback);
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
        HttpServerRequest req, In1Unsafe<RequestScope<VertxRequest>> callback
    ) {
        X_Scope.service().runInNewScope(RequestScope.class, (scope, done)->{
            final XapiServer was = scope.setLocal(XapiServer.class, this);
            try {
                final RequestScopeVertx s = (RequestScopeVertx) scope;
                VertxRequest forScope = VertxRequest.getOrMake(s.getRequest(), req);
                s.initialize(forScope);
                callback.in(s);
                done.done();
            } finally {
                if (was == null) {
                    scope.removeLocal(XapiServer.class);
                } else {
                    scope.setLocal(XapiServer.class, was);
                }
            }
        });
    }

    @Override
    public void start() {
        webApp.setRunning(true);
        if (webApp.getPort() == 0) {
            X_Server.usePort(p->{
                webApp.setPort(p);
                start();
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
        vertx = Vertx.vertx(vertxOptions);

        exe = X_Process::runDeferred;
        final HttpServer server = vertx.createHttpServer(opts)
            .requestHandler(this::handleRequest)
            .listen(result->{
                if (result.failed()) {
                    throw new IllegalStateException("Server failed to start ", result.cause());
                }
                X_Log.info(getClass(), "Server up and running at 0.0.0.0:" + webApp.getPort());
                connection.in(result.result());
            });

        while (connection.isNull()) {
            LockSupport.parkNanos(100_000);
        }

    }

    protected void handleRequest(HttpServerRequest req) {
        serviceRequest(req, (r, success)->{
            final HttpServerResponse resp = r.getHttpRequest().response();
            if (r.isAutoclose() && !resp.ended()){
                resp.end();
            }
            if (!Boolean.TRUE.equals(success)) {
              final HttpServerResponse response = req.response();
              on404(req, response);
            }
        });
    }

    protected void on404(HttpServerRequest req, HttpServerResponse resp) {
        if (!resp.ended()) {
            resp.setStatusCode(404);
            resp.end("<!DOCTYPE html><html>" +
                "<body style=\"text-align: center; vertical-align: middle; display: block\">" +
                "<div style=\"white-space:pre-wrap\">" +
                "Our apologies, we cannot find anything to serve to:" +
                dump(req) +
                "</div>" +
                "</body>" +
                "</html>");
        }
    }

    protected String dump(HttpServerRequest req) {return dump(req, "\n");}

    protected String dump(
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
    public void shutdown() {
        webApp.setRunning(false);
    }

    @Override
    public void serviceRequest(
        HttpServerRequest req, In2<VertxRequest, Boolean> callback
    ) {
        inScope(req, vertx->{

            try {

                final String path = req.path();
                final WebApp app = getWebApp();
                Route best = null;
                ChainBuilder<Route> backups = Chain.startChain();
                double score = 0;
                for (Route route : app.getRoute().forEach()) {
                    double match = route.matches(path);
                    if (match > score) {
                        if (match == 1) {
                            // Try to serve a perfect match immediately
                            if (route.serve(path, vertx, done->
                                callback.in(done, true)
                            )) {
                                return;
                            }
                        }
                        if (best != null) {
                            backups.add(best);
                        }
                        score = match;
                        best = route;
                    } else if (match > 0) {
                        if (best != null) {
                            backups.add(route);
                        }
                    }
                }
                if (best != null) {
                    if (best.serve(path, vertx, done->
                        callback.in(done, true)
                    )) {
                        return;
                    } else {
                        X_Log.trace(getClass(), "Best was no good; ", best, "\nServing backups:", backups);
                        for (Route backup : backups) {
                            if (backup.serve(path, vertx, done->
                                callback.in(done, true)
                            )) {
                                return;
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                // failure... TODO error mapping
                callback.in(vertx.getRequest(), false);
                throw t;
            }

            // If you didn't return, you failed and should return failure to the client
            callback.in(vertx.getRequest(), false);
        });
    }

    @Override
    public String getPath(VertxRequest req) {
        return req.getHttpRequest().path();
    }

    @Override
    public String getMethod(VertxRequest req) {
        return req.getHttpRequest().method().name();
    }

    @Override
    public IntTo<String> getParams(VertxRequest req, String param) {
        return new ListAdapter<>(req.getHttpRequest().params().getAll(param));
    }

    @Override
    public IntTo<String> getHeaders(VertxRequest req, String header) {
        return new ListAdapter<>(req.getHttpRequest().headers().getAll(header));
    }

    @Override
    public void writeText(RequestScope<VertxRequest> request, String payload, In1<VertxRequest> callback) {
        final HttpServerResponse response = request.getRequest().getResponse();
        response.end(payload);
        callback.in(request.getRequest());
    }

    @Override
    public void writeTemplate(
        RequestScope<VertxRequest> request, String payload, In1<VertxRequest> callback
    ) {
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
    public void writeFile(RequestScope<VertxRequest> request, String payload, In1<VertxRequest> callback) {
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
        response.sendFile(toServe.getAbsolutePath());
        callback.in(request.getRequest());
    }

    @Override
    public void writeGwtJs(RequestScope<VertxRequest> request, String payload, In1<VertxRequest> callback) {
        final ModelGwtc module = getWebApp().getOrCreateGwtModules().get(payload);
        if (module == null) {
            throw new IllegalArgumentException("No gwt app registered for id " + payload);
        }
        writeGwtJs(request.getRequest(), payload, module, callback);
    }

    private void writeGwtJs(VertxRequest req, String payload, ModelGwtc module, In1<VertxRequest> callback) {
        final HttpServerResponse response = req.getResponse();
        String url = req.getPath();
        if (url.contains(".nocache.js")) {
            // request was for the nocache file; compile every time (for now)
            final GwtManifest manifest = module.getOrCreateManifest();
            X_Process.runDeferred(()->{
                boolean[] done = {false};
                Do onDone = () -> {
                    Path path = Paths.get(manifest.getCompiledWar());
                    final Path file = path.resolve(payload + File.separatorChar + payload + ".nocache.js");
                    if (!done[0]) {
                        done[0] = true;
                        response.sendFile(
                            file.normalize().toString()
                        );
                        callback.in(req);
                    }
                };
                final GwtcService service = module.getOrCreateService();
                if (manifest.isRecompile()) {
                    final ServerRecompiler compiler = module.getRecompiler();
                    if (compiler != null) {
                        compiler.useServer(comp->{
                            final CompiledDirectory result = comp.recompile();
                            if (result.getStrategy() != CompileStrategy.SKIPPED) {
                                // only update the directory to serve if we did not skip the compile
                                manifest.setCompileDirectory(result);
                            }
                            onDone.done();
                        });
                    } else {
                        service.recompile(manifest, (recompiler, error)->{
                            if (error != null) {
                                module.setRecompiler(null);
                                onDone.done();
                                throw new IllegalStateException("Gwt compile failed for manifest " + manifest, error);
                            }
                            module.setRecompiler(recompiler);
                            onDone.done();
                        });
                    }
                } else {
                    final int result = service.compile(manifest);
                    if (result != 0) {
                        throw new IllegalStateException("Gwt compile failed for manifest " + manifest);
                    }
                    onDone.done();
                }
            });
        } else {
            // request was for a file from the compile result;
            // serve it up...
            final GwtManifest manifest = module.getOrCreateManifest();
            url = url.startsWith("/") ? url.substring(1) : url;
            Path dir = Paths.get(url.startsWith("sourcemaps") ? manifest.getCompileDirectory().getSourceMapDir() : manifest.getCompiledWar());
            Path file = dir.resolve(url);
            Mutable<Boolean> done = new Mutable<>(false);
            if (Files.exists(file)) {
                final String normalized = file.normalize().toString();
                response.sendFile(normalized);
            } else if (url.startsWith("sourcemaps")){
                if (url.endsWith("json")) {
                    final String fileName = X_String.chopEndOrReturnEmpty(url, "/")
                        // not really sure why super dev mode does this...
                        .replace("_sourcemap.json", "_sourceMap0.json");
                    file = dir.resolve(fileName);
                    if (Files.exists(file)) {
                        final String normalized = file.normalize().toString();
                        response.sendFile(normalized);
                    } else {
                        X_Log.warn(getClass(), "No file to serve for ", file, "from url", url);
                    }
                } else {
                    // This url is for an actual source file.
                    int ind = url.indexOf("$sourceroot_goes_here$");
                    if (ind == -1) {
                        ind = url.indexOf('/');
                        ind = url.indexOf('/', ind+1);
                    }
                    ind = url.indexOf('/', ind+1);
                    String fileName = url.substring(ind+1);

                    // Lets check in obvious file locations
                    if (module.getRecompiler() != null) {
                        req.setAutoclose(false);
                        done.in(true);
                        module.getRecompiler().useServer(recompiler->{
                            final URL resource = recompiler.getResource(fileName);
                            if (resource == null) {
                                if (fileName.startsWith("gen/")) {
                                    Path genPath = Paths.get(manifest.getCompileDirectory().getGenDir());
                                    final Path genFile = genPath.resolve(fileName.substring(4));
                                    if (Files.exists(genFile)) {
                                        response.sendFile(genFile.toString());
                                        callback.in(req);
                                        return;
                                    } else {
                                        X_Log.warn(getClass(), "No file found for path", genFile);
                                    }
                                }
                            } else {
                                // In case this is a url into a jar,
                                // we save ourselves some headache, and just stream from whatever URL we get

                                try (
                                    final InputStream in = resource.openStream()
                                ) {
                                    final byte[] bytes = X_IO.toByteArray(in);
                                    response.end(Buffer.buffer(bytes));
                                    callback.in(req);
                                    return;
//                                    AsyncInputStream input = new AsyncInputStream(vertx, exe, in);
//                                    input.exceptionHandler(t->{
//                                        X_Log.error(getClass(), "Failed sending source", resource, t);
//                                        callback.in(req);
//                                        assert false : "Failure sending source " + resource + " : " + t;
//                                    });
//                                    input.endHandler(v->
//                                        callback.in(req)
//                                    );
//                                    final Pump pump = Pump.pump(input, response);
//                                    pump.start();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                // prevent system from auto-closing the response.
                                // we are now responsible for ensuring finally-closed semantics
                            }

                            X_Log.error(getClass(), "Failed sending source", fileName);
                            callback.in(req);
                        });
                    }
                }
            } else {
                X_Log.warn(getClass(), "No file to serve for ", file, "from url", url);
            }
            if (!done.out1()) {
                callback.in(req);
            }
        }
    }

    @Override
    public void writeCallback(RequestScope<VertxRequest> request, String payload, In1<VertxRequest> callback) {
        callback.in(request.getRequest());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeService(
        String path, RequestScope<VertxRequest> request, String payload, In1<VertxRequest> callback
    ) {
        final XapiEndpoint endpoint = findEndpoint(payload);
        if (endpoint == null) {
            X_Log.warn(XapiVertxServer.class, "No endpoint found for", payload);
        } else {
            endpoint.serviceRequest(path, request, payload, callback);
        }
    }
}
