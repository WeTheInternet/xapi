package xapi.server.socket;

import xapi.fu.iterate.Chain;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;

import java.nio.ByteBuffer;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/16/16.
 */
public class SocketMessageQueue {

    private final Lazy<ByteBuffer> buffer = Lazy.deferred1(()->ByteBuffer.allocate(1024));

    private Chain<ByteBuffer> buffers;
    private In1Out1<ByteBuffer, Boolean> listener;

    public void useBuffer(In1Unsafe<ByteBuffer> callback) {
        synchronized (buffer) {
            callback.in(buffer.out1());
        }
    }
}
