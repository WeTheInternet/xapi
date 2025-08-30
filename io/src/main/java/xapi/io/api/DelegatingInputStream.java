package xapi.io.api;

import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.Out2;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/19/17.
 */
public class DelegatingInputStream extends InputStream implements StreamDelegate {

    private final InputStream delegate;
    private final Do done;
    private In1<Integer> spy;

    public DelegatingInputStream(InputStream source) {
        this(source, In1.ignored());
    }

    public DelegatingInputStream(InputStream delegate, In1<Integer> spy) {
        this(delegate, spy, Do.NOTHING);
    }

    public DelegatingInputStream(InputStream delegate, Out2<In1<Integer>, Do> callbacks) {
        this(delegate, callbacks.out1(), callbacks.out2());
    }
    public DelegatingInputStream(InputStream delegate, In1<Integer> spy, Do done) {
        this.delegate = delegate;
        this.spy = spy;
        this.done = done;
    }

    @Override
    public int read() throws IOException {
        int i = delegate.read();
        spy(i);
        return i;
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
