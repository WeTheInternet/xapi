package xapi.io.api;

import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.Chain;
import xapi.fu.itr.SingletonIterator;

/**
 * A delegating LineReader to allow you to wrap and combine other LineReaders,
 * or just add lambdas to the various callbacks
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/19/17.
 */
public class ComposableLineReader implements LineReader {

    private final MappedIterable<LineReader> delegate;

    public ComposableLineReader(LineReader delegate) {
        this.delegate = SingletonIterator.singleItem(delegate);
    }

    public ComposableLineReader(LineReader delegate, LineReader ... others) {
        this.delegate = Chain.toChain(delegate).addAll(others);
    }

    public ComposableLineReader(MappedIterable<LineReader> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onStart() {
        this.delegate.forEach(LineReader::onStart);
    }

    @Override
    public void onLine(String line) {
        this.delegate.forAll(LineReader::onLine, line);
    }

    @Override
    public void onEnd() {
        this.delegate.forEach(LineReader::onEnd);
    }
}
