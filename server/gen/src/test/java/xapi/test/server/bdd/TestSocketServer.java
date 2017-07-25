package xapi.test.server.bdd;

import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.fu.Lazy;
import xapi.fu.Mutable;
import xapi.fu.Rethrowable;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.impl.AbstractModel;
import xapi.process.X_Process;
import xapi.scope.api.RequestScope;
import xapi.server.X_Server;
import xapi.server.api.Classpath;
import xapi.server.api.ModelGwtc;
import xapi.server.api.Route;
import xapi.server.api.WebApp;
import xapi.server.api.XapiEndpoint;
import xapi.server.api.XapiServer;
import xapi.source.api.CharIterator;
import xapi.source.impl.InputStreamCharIterator;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/23/16.
 */
class TestSocketServer extends AbstractModel implements WebApp, Rethrowable, XapiServer<SocketRequest, Socket> {

    private final URL classpath;
    private final Lazy<Socket> io;
    private Thread enviro;
    //        private Server server;
    private ServerSocket socket;
    private int port = -1;
    private StringTo<Classpath> classpaths;
    private StringTo<ModelGwtc> gwtcs;
    private StringTo<Model> templates;
    private IntTo<Route> routes;
    private volatile boolean running;
    private String source;
    private boolean devMode;
    private String contentRoot;
    private String instanceId = "test";

    public TestSocketServer(WebApp classpath) {
        try {
            this.classpath = new URL(classpath.getClasspaths().get("root").getPaths().at(0));
        } catch (MalformedURLException e) {
            throw rethrow(e);
        }
        setSource(classpath.getSource());
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
    public void serviceRequest(Socket socket, In2<SocketRequest, Boolean> callback) {

    }

    @Override
    public String getPath(SocketRequest req) {
        return req.getPath();
    }

    @Override
    public String getMethod(SocketRequest req) {
        return "SOCKET";
    }

    @Override
    public WebApp getWebApp() {
        return null;
    }

    @Override
    public IntTo<String> getParams(SocketRequest req, String param) {
        throw new UnsupportedOperationException("Socket server does not have params");
    }

    @Override
    public IntTo<String> getHeaders(SocketRequest req, String header) {
        throw new UnsupportedOperationException("Socket server does not have headers");
    }

    @Override
    public void writeText(RequestScope<SocketRequest> request, String payload, In1<SocketRequest> callback) {
        throw new UnsupportedOperationException("writeText not supported");
    }

    @Override
    public void writeFile(
        RequestScope<SocketRequest> request, String payload, In1<SocketRequest> callback
    ) {
        throw new UnsupportedOperationException("writeText not supported");
    }

    @Override
    public void writeGwtJs(RequestScope<SocketRequest> request, String payload, In1<SocketRequest> callback) {
        throw new UnsupportedOperationException("writeGwtJs not supported");
    }

    @Override
    public void writeTemplate(
        RequestScope<SocketRequest> request, String payload, In1<SocketRequest> callback
    ) {
        throw new UnsupportedOperationException("writeTemplate not supported");
    }

    @Override
    public void writeCallback(RequestScope<SocketRequest> request, String payload, In1<SocketRequest> callback) {
        throw new UnsupportedOperationException("writeCallback not supported");
    }

    @Override
    public void writeService(
        String path, RequestScope<SocketRequest> request, String payload, In1<SocketRequest> callback
    ) {
        throw new UnsupportedOperationException("writeService not supported");
    }

    @Override
    public void registerEndpoint(String name, XapiEndpoint<SocketRequest> endpoint) {
        throw new UnsupportedOperationException("registerEndpoint not supported");
    }

    @Override
    public void registerEndpointFactory(
        String name, boolean singleton, In1Out1<String, XapiEndpoint<SocketRequest>> endpoint
    ) {
        throw new UnsupportedOperationException("registerEndpoint not supported");
    }

    @Override
    public void inScope(Socket socket, In1Unsafe<RequestScope<SocketRequest>> callback) {

    }

    public void start() {
        URLClassLoader cl = new URLClassLoader(
            new URL[]{classpath},
            Thread.currentThread().getContextClassLoader()
        );
        //            server = new Server(port);
        enviro = new Thread(() -> {
            //                while (server.isRunning()) {
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
    public String getSource() {
        return source;
    }

    @Override
    public WebApp setSource(String source) {
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
    public boolean isRunning() {
        return running;
    }

    @Override
    public WebApp setRunning(boolean running) {
        this.running = running;
        return this;
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
