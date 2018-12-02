package xapi.fu.lazy;

import xapi.fu.*;
import xapi.fu.has.HasReset;

import static xapi.fu.Out1.immutable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/9/17.
 */
public class ResettableLazy <T> extends Lazy<T> implements HasReset {

    private volatile Out1<Out1<T>> resetter;
    private volatile Do onReset = Do.NOTHING;
    // TODO: maybe push this down into Lazy, or up, into SettableLazy?
    private volatile In1<T> onSet = In1.ignored();

    public ResettableLazy(Out1<T> supplier) {
        super(supplier);
        setResetter(()->supplier);
    }

    public void bind(ResettableLazy<T> spy) {
        onReset(spy::reset);
    }

    public final synchronized void onReset(Do spy) {
        assert spy != null : "Don't send null callbacks!";
        this.onReset = this.onReset.doAfter(spy);
    }

    public final synchronized void onSet(In1<T> spy) {
        assert spy != null : "Don't send null callbacks!";
        this.onSet = this.onSet.useAfterMe(spy);
        if (isResolved()) {
            final T val = out1();
            if (valueAcceptable(val)) {
                spy.in(val);
            }
        }
    }

    @Override
    public synchronized final void reset() {
        onReset.done();
        final Out1<T> factory = resetter.out1();
        proxy = complexProxy(factory, val->onSet.in(val), false);
    }

    public synchronized ResettableLazy<T> setResetter(Out1<Out1<T>> resetter) {
        this.resetter = resetter;
        return this;
    }

    // TODO: push this up into a new subclass, SettableLazy?
    public final void set(Out1<T> proxy) {
        setResetter(immutable(proxy));
        reset();
    }

    public final void set(T value) {
        set(Immutable.immutable1(value));
    }
}
