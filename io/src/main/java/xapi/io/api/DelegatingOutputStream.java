package xapi.io.api;

import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.Out2;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/19/17.
 */
public class DelegatingOutputStream extends OutputStream {

    private final OutputStream delegate;
    private final Do done;
    private In1<Integer> spy;

    public DelegatingOutputStream(OutputStream source) {
        this(source, In1.ignored());
    }

    public DelegatingOutputStream(OutputStream delegate, In1<Integer> spy) {
        this(delegate, spy, Do.NOTHING);
    }

    public DelegatingOutputStream(OutputStream delegate, Out2<In1<Integer>, Do> callbacks) {
        this(delegate, callbacks.out1(), callbacks.out2());
    }
    public DelegatingOutputStream(OutputStream delegate, In1<Integer> spy, Do done) {
        this.delegate = delegate;
        this.spy = spy;
        this.done = done;
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
        spy(b);
    }

    @Override
    public void close() throws IOException {
        super.close();
        done.done();
    }

    private void spy(int i) {
        spy.in(i);
    }

}
