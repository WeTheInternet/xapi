package xapi.server.socket;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;
import xapi.dev.source.CharBuffer;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.fu.data.MapLike;
import xapi.fu.Mutable;
import xapi.fu.Out1.Out1Unsafe;
import xapi.fu.Rethrowable;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.service.ModelService;
import xapi.model.user.ModelUser;
import xapi.process.X_Process;
import xapi.server.X_Server;
import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;
import xapi.util.X_Debug;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.LockSupport;

/**
 * A (currently toy) implementation of pure socket-based {@link xapi.model.api.Model} passing.
 * <p>
 * Created by James X. Nelson (james @wetheinter.net) on 10/16/16.
 */
public class XapiSocketServer implements Rethrowable {

    private final ModelService modelService;
    private Selector selector;
    private final Mutable<Integer> port = new Mutable<>();
    private MapLike<SocketChannel, SocketMessageQueue> connections =
        X_Collect.newMap(SocketChannel.class, SocketMessageQueue.class,
            CollectionOptions.asConcurrent(true).build()).asMap();

    public XapiSocketServer() {
        this(X_Inject.singleton(ModelService.class));
    }

    public XapiSocketServer(ModelService modelService) {
        this.modelService = modelService;
    }

    public void startServer() throws IOException {

        this.selector = Selector.open();

        ServerSocketChannel serverChannel = ServerSocketChannel.open();

        serverChannel.configureBlocking(false);

        // retrieve server socket and bind to port

        X_Server.usePort(
            this.port.useBeforeMeUnsafe(randomPort ->
                serverChannel.socket().bind(new InetSocketAddress(hostName(), randomPort))
            ));
        X_Log.trace(getClass(), "Port bound to: " + this.port.out1());
        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

        X_Log.trace(getClass(), "Server started...");

        while (this.selector.isOpen()) {

            // wait for events

            int found = this.selector.select();

            if (found == 0) {
                return;
            }

            //work on selected keys

            final Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = keys.next();
                // this is necessary to prevent the same key from coming up
                // again the next time around.
                keys.remove();
                if (!key.isValid()) {
                    X_Log.warn(getClass(), "Invalid key: ", key);
                    continue;
                }

                if (key.isAcceptable()) {
                    this.accept(key);
                } else if (key.isReadable()) {
                    this.read(key);
                } else {
                    X_Log.warn(getClass(), "Unhandled key: ", key);
                }

            }

        }

    }

    public In1Out1Unsafe<Model, Out1Unsafe<Model>> createClient() {
        In1Out1Unsafe<Model, Out1Unsafe<Model>> function;
        function = model -> {

            InetSocketAddress hostAddress = new InetSocketAddress(hostName(), getPort());
            Mutable<InetSocketAddress> clientAddress = new Mutable<>();
            X_Server.usePort(port->
                clientAddress.in(new InetSocketAddress(hostName(), port))
            );

            final AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(Executors.newCachedThreadPool());
            AsynchronousSocketChannel connection = AsynchronousSocketChannel.open(group);
            final AsynchronousSocketChannel channel = connection.bind(clientAddress.out1());

            final Class<Model> modelClass = modelService.typeToClass(model.getType());

            final String typeMessage = modelService.primitiveSerializer().serializeClass(modelClass);
            final CharBuffer modelMessage = modelService.serialize(modelClass, model);
            byte[] message = (typeMessage + modelMessage).getBytes();
            ByteBuffer buffer = ByteBuffer.wrap(message);

            final Future<Void> future = channel.connect(hostAddress);
            Mutable<Integer> reply = new Mutable<>();
            X_Process.resolve(future, v->
                channel.write(buffer, null, new CompletionHandler<Integer, Object>() {
                    @Override
                    public void completed(Integer result, Object attachment) {
                        System.out.println(result + " : " + attachment);
                        reply.in(result);
                        synchronized (reply) {
                            reply.notifyAll();
                        }
                    }

                    @Override
                    public void failed(Throwable exc, Object attachment) {
                        X_Log.error(getClass(), "Send failure:", exc);
                    }
                }),
                X_Debug::rethrow
            );
            return () -> {
                // listen for a reply.
                if (reply.out1() == null) {
                    synchronized (reply) {
                        reply.wait();
                    }
                }
                final ByteBuffer dst = ByteBuffer.allocate(reply.out1());
                final Future<Integer> serverReply = channel.read(dst);
                Mutable<Integer> serverSize = new Mutable<>();
                 X_Process.resolve(serverReply, i->{
                     synchronized (dst) {
                         serverSize.in(i);
                         dst.notify();
                     }
                 }, X_Debug::rethrow);
                if (serverSize.out1() == null) {
                    synchronized (dst) {
                        dst.wait();
                    }
                }
                StringCharIterator chars = new StringCharIterator(
                    new String(dst.array())
                );
                final Class<Model> cls = modelService.primitiveSerializer().deserializeClass(chars);
                return modelService.deserialize(cls, chars);
            };
        };
        return function.<Model>mapIn(modelIn->{
            while (selector == null || !selector.isOpen()) {
                LockSupport.parkNanos(100_000);
            }
            return modelIn;
        }).unsafeIn1Out1();
    }

    public int getPort() {
        return port.out1();
    }

    //accept a connection made to this channel's socket

    private void accept(SelectionKey key) throws IOException {

        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);

        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        X_Log.trace(getClass(), "Connected to: ", remoteAddr);

        // register channel with selector for further IO
        connections.put(channel, new SocketMessageQueue());

        channel.register(this.selector, SelectionKey.OP_READ);

    }

    //read from the socket channel

    private void read(SelectionKey key) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();

        final SocketMessageQueue queue = connections.get(channel);

        queue.useBuffer(buffer->{

            int numRead = -1;

            numRead = channel.read(buffer);

            if (numRead == -1) {
                connections.remove(channel);

                Socket socket = channel.socket();
                SocketAddress remoteAddr = socket.getRemoteSocketAddress();

                X_Log.trace(getClass(), "Connection closed by client: ", remoteAddr);

                channel.close();
                key.cancel();
                buffer.clear();
            }
            buffer.flip();
            final String chars = new String(buffer.array());
            final StringCharIterator itr = CharIterator.forString(chars);
            System.out.println(itr.consumeAll());
            final Class<? extends Model> cls = modelService.primitiveSerializer().deserializeClass(itr);
            final Model model = modelService.deserialize(cls, itr);
            System.out.println("Received: " + model);
        });




    }

    protected InetAddress hostName() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw rethrow(e);
        }
    }

    private void shutdown() {
        try {
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String... args) throws IOException {

        X_Log.logLevel(LogLevel.INFO);
        final XapiSocketServer server = new XapiSocketServer();
        Thread serverEnv = new Thread(() -> {
            try {
                server.startServer();

                while (server.selector.isOpen()) {
                    LockSupport.parkNanos(100_000);
                }
                System.out.println("Server done...");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, "server");
        serverEnv.start();


        Thread clientEnv = new Thread(() -> {
            final In1Out1Unsafe<Model, Out1Unsafe<Model>> client = server.createClient();
            final ModelUser user = X_Model.create(ModelUser.class)
                .setId("myId")
                .setDisplayName("My name")
                .setFirstName("My name");
            Model reply = client.io(user).out1();

            System.out.println("My reply: " + reply);

            server.shutdown();
        });
        clientEnv.setDaemon(true);
        clientEnv.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Socket open? " + server.selector.isOpen());
        }));

    }
}
