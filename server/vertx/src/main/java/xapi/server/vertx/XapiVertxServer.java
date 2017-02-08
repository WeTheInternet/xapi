package xapi.server.vertx;

import com.github.javaparser.ast.expr.UiContainerExpr;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import xapi.collect.api.IntTo;
import xapi.dev.gwtc.api.GwtcService;
import xapi.fu.Do;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.In2;
import xapi.fu.Mutable;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.ServerRecompiler;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.process.X_Process;
import xapi.scope.X_Scope;
import xapi.scope.api.RequestScope;
import xapi.server.X_Server;
import xapi.server.api.ModelGwtc;
import xapi.server.api.Route;
import xapi.server.api.WebApp;
import xapi.server.api.XapiServer;
import xapi.util.X_String;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.LockSupport;

import com.google.gwt.dev.codeserver.JobEvent.CompileStrategy;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
public class XapiVertxServer implements XapiServer<VertxRequest, HttpServerRequest> {

    private final WebApp webApp;
    private Vertx vertx;
    private In1<DoUnsafe> exe;

    public XapiVertxServer(WebApp webApp) {
        this.webApp = webApp;
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
        serviceRequest(req, (r1, r2)->{
            final HttpServerResponse resp = r1.getHttpRequest().response();
            if (r1.isAutoclose() && !resp.ended()){
                resp.end();
            }
        });
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
        HttpServerRequest req, In2<VertxRequest, HttpServerRequest> callback
    ) {
        inScope(req, vertx->{

            final String path = req.path();
            final WebApp app = getWebApp();
            Route best = null;
            ChainBuilder<Route> backups = Chain.startChain();
            double score = 0;
            for (Route route : app.getRoute().forEach()) {
                double match = route.matches(path);
                if (match > score) {
                    if (match == 1) {
                        // Try to server a perfect match immediately
                        if (route.serve(vertx, done->
                            callback.in(done, req)
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
                if (best.serve(vertx, done->
                    callback.in(done, req)
                )) {
                    return;
                } else {
                    X_Log.trace(getClass(), "Best was no good; ", best, "\nServing backups:", backups);
                    for (Route backup : backups) {
                        if (backup.serve(vertx, done->
                            callback.in(done, req)
                        )) {
                            return;
                        }
                    }
                }
            }

            final HttpServerResponse response = req.response();
            final String text = "<html><body>Hello World</body></html>";
            response.setChunked(true);

            response.end(text);

            callback.in(vertx.getRequest(), req);
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
    public void writeFile(RequestScope<VertxRequest> request, String payload, In1<VertxRequest> callback) {
        final HttpServerResponse response = request.getRequest().getResponse();
        File toServe = new File(webApp.getContentRoot());
        if (!toServe.exists()) {
            throw new IllegalStateException("Content root " + toServe + " does not exist!");
        }
        toServe = new File(toServe, payload);
        if (!toServe.exists()) {
            throw new IllegalStateException("Content file " + toServe + " does not exist!");
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
                Do onDone = () -> {
                    Path path = Paths.get(manifest.getCompiledWar());
                    final Path file = path.resolve(payload + File.separatorChar + payload + ".nocache.js");
                    response.sendFile(
                        file.normalize().toString()
                    );
                    callback.in(req);
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
                            final URL resource = recompiler.getResourceLoader().getResource(fileName);
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

}
