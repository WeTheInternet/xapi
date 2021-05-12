package xapi.test.server.bdd;

import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.api.Classpath;
import xapi.fu.*;
import xapi.fu.In3.In3Unsafe;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.impl.AbstractModel;
import xapi.process.X_Process;
import xapi.scope.api.HasRequestContext;
import xapi.scope.api.Scope;
import xapi.scope.request.RequestScope;
import xapi.scope.request.SessionScope;
import xapi.scope.spi.RequestContext;
import xapi.server.X_Server;
import xapi.server.api.*;
import xapi.source.lex.CharIterator;
import xapi.source.lex.InputStreamCharIterator;
import xapi.test.server.bdd.TestSocketServer.SocketScope;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
class TestSocketServer extends AbstractModel implements WebApp, Rethrowable, XapiServer<SocketScope> {

    interface SocketScope extends RequestScope<SocketRequest, SocketResponse> {
        @Override
        default Class<? extends Scope> forScope() {
            return RequestScope.class;
        }
    }

    private final URL classpath;
    private final Lazy<Socket> io;
    private Thread enviro;
    //        private Server server;
    private ServerSocket socket;
    private int port = -1;
    private StringTo<Classpath> classpaths;
    private StringTo<ModelGwtc> gwtcs;
    private StringTo<Boolean> allowAbsolutes;
    private StringTo<Model> templates;
    private IntTo<Route> routes;
    private volatile boolean running;
    private String source;
    private boolean devMode;
    private String contentRoot;
    private String protocol;
    private String instanceId = "test";

    public TestSocketServer(WebApp classpath) {
        try {
            this.classpath = new URL(classpath.getClasspaths().get("root").getPaths().at(0));
        } catch (MalformedURLException e) {
            throw rethrow(e);
        }
        setBaseSource(classpath.getBaseSource());
        setPort(classpath.getPort());
        setClasspaths(classpath.getClasspaths());
        final Mutable<Socket> sock = new Mutable<>();
        io = Lazy.deferred1(sock);
        port = X_Server.usePort(p -> {
            try {
                socket = new ServerSocket(getPort() == 0 || getPort() == -1 ? p : getPort());
                socket.setReuseAddress(true);
//                X_Process.runDeferredUnsafe(() ->
//                    sock.in(socket.accept())
//                );
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void shutdown() {
        WebApp.super.shutdown();
        try {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
            }
            if (io != null) {

            }
            if (enviro != null) {
                enviro.join();
                enviro = null;
            }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
        }
    }

    @Override
    public void serviceRequest(
        SocketScope socketScope, In2<SocketScope, Throwable> callback
    ) {
        callback.in(socketScope, null);
    }

    @Override
    public WebApp getWebApp() {
        return null;
    }

    @Override
    public void writeText(
        SocketScope scope, String payload, In2<SocketScope, Throwable> callback
    ) {
        throw new UnsupportedOperationException("writeText not supported");
    }

    @Override
    public void writeFile(
        SocketScope scope, String payload, In2<SocketScope, Throwable> callback
    ) {
        throw new UnsupportedOperationException("writeFile not supported");
    }

    @Override
    public void writeDirectory(
        SocketScope socketScope, String payload, In2<SocketScope, Throwable> callback
    ) {
        throw new UnsupportedOperationException("writeDirectory not supported");
    }

    @Override
    public void writeGwtJs(
        SocketScope scope, String payload, In2<SocketScope, Throwable> callback
    ) {
        throw new UnsupportedOperationException("writeGwtJs not supported");
    }

    @Override
    public void writeTemplate(
        SocketScope scope, String payload, In2<SocketScope, Throwable> callback
    ) {
        throw new UnsupportedOperationException("writeTemplate not supported");
    }

    @Override
    public void writeCallback(
        SocketScope scope, String payload, In2<SocketScope, Throwable> callback
    ) {
        throw new UnsupportedOperationException("writeCallback not supported");
    }

    @Override
    public void writeService(
        String path,
        SocketScope scope,
        String payload,
        In2<SocketScope, Throwable> callback
    ) {
        throw new UnsupportedOperationException("writeService not supported");
    }

    @Override
    public void registerEndpoint(String name, XapiEndpoint<SocketScope> endpoint) {
        throw new UnsupportedOperationException("registerEndpoint not supported");
    }

    @Override
    public void registerEndpointFactory(
        String name, boolean singleton, In1Out1<String, XapiEndpoint<SocketScope>> endpoint
    ) {
        throw new UnsupportedOperationException("registerEndpoint not supported");
    }

    @Override
    public <Req extends HasRequestContext> void reroute(
        Req request, String payload, In2<Req, Throwable> callback
    ) {
        throw new UnsupportedOperationException("reroute not supported");
    }

    @Override
    public void onRelease() {

    }

    @Override
    public <C extends RequestContext> void inContext(
        C ctx, In1Out1<C, SocketScope> factory, In3Unsafe<SocketScope, Throwable, In1<Throwable>> callback
    ) {

        final SocketScope s = factory.io(ctx);
        callback.in(s, null, In1.ignored()); // terrible...
    }

    @Override
    public void inScope(
        String sessionId, In1Out1<SessionScope, SocketScope> scopeFactory, In3Unsafe<SocketScope, Throwable, Do> callback
    ) {
    }

    @Override
    public void shutdown(Do onDone) {

    }

    public void start(Do onStart) {
        URLClassLoader cl = new URLClassLoader(
            new URL[]{classpath},
            Thread.currentThread().getContextClassLoader()
        );
        //            server = new Server(port);
        enviro = new Thread(() -> {
            onStart.done();
            while (isRunning()) {
                try {
                    serviceRequests();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                // Meter ourselves at least 1/10 of a milli
                LockSupport.parkNanos(100_000);
            }
        });
        enviro.setContextClassLoader(cl);
        enviro.setDaemon(true);
        WebApp.super.start();
        enviro.start();
    }

    @Override
    public void setContentRoot(String root) {
        this.contentRoot = root;
        X_Log.info(TestSocketServer.class, "Content root: ", contentRoot);
    }

    @Override
    public String getContentRoot() {
        return contentRoot;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }


    @Override
    public StringTo<Boolean> getAbsolutes() {
        return allowAbsolutes;
    }

    @Override
    public WebApp setAbsolutes(StringTo<Boolean> allowAbsolutes) {
        this.allowAbsolutes = allowAbsolutes;
        return this;
    }

    private void serviceRequests() throws IOException {
        CountDownLatch latch = new CountDownLatch(1);
        X_Process.runWhenReadyUnsafe(io, sock -> {
            final InputStream in = sock.getInputStream();
            while (in.available() > 0) {
                PrimitiveSerializer serializer = X_Inject.instance(PrimitiveSerializer.class);
                final CharIterator chars = new InputStreamCharIterator(in);
                String path = chars.readLine().toString();
                List<String> headers = new ArrayList<>();
                for (CharSequence header; !(header = chars.readLine()).toString().trim().isEmpty(); ) {
                    headers.add(header.toString());
                    System.out.println(chars.toString());
                }
                System.out.println(path);
                System.out.println(headers);
                if (!chars.hasNext()) {
                    // This is just a GET request
                    X_IO.drain(
                        sock.getOutputStream(),
                        X_IO.toStreamUtf8("HTTP/1.1 200 Success\r\n\r\nHello World")
                    );
                    sock.getOutputStream().flush();
                    sock.getOutputStream().close();
                    sock.close();

                } else {

                }
            }
            latch.countDown();
        });
        try {
            latch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw rethrow(e);
        }
    }

    public int getPort() {
        return port;
    }

    @Override
    public WebApp setPort(int port) {
        assert this.port == -1 || this.port == port: "Port already assigned; your application is probably leaking sockets (check localhost:" + port + ")";
        this.port = port;
        return this;
    }

    @Override
    public String getBaseSource() {
        return source;
    }

    @Override
    public WebApp setBaseSource(String source) {
        this.source = source;
        return this;
    }

    @Override
    public StringTo<Classpath> getClasspaths() {
        return classpaths;
    }

    @Override
    public WebApp setClasspaths(StringTo<Classpath> classpaths) {
        this.classpaths = classpaths;
        return this;
    }

    @Override
    public StringTo<ModelGwtc> getGwtModules() {
        return gwtcs;
    }

    @Override
    public WebApp setGwtModules(StringTo<ModelGwtc> modules) {
        this.gwtcs = modules;
        return this;
    }

    @Override
    public StringTo<Model> getTemplates() {
        return templates;
    }

    @Override
    public IntTo<Route> getRoute() {
        return routes;
    }

    @Override
    public boolean isClustered() {
        return false;
    }

    @Override
    public WebApp setClustered(boolean clustered) {
        return null;
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    @Override
    public WebApp setDestroyed(boolean destroyed) {
        return null;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public TestSocketServer setProtocol(final String protocol) {
        this.protocol = protocol;
        return this;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public WebApp setRunning(boolean running) {
        this.running = running;
        return this;
    }

    @Override
    public boolean isDestroyable() {
        return false;
    }

    @Override
    public WebApp setDestroyable(boolean destroyable) {
        return null;
    }

    @Override
    public WebApp setDevMode(boolean devMode) {
        this.devMode = devMode;
        return this;
    }

    @Override
    public boolean isDevMode() {
        return devMode;
    }

}
