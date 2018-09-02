package xapi.fu.lazy;

import xapi.fu.*;
import xapi.fu.has.HasReset;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/9/17.
 */
public class ResettableLazy <T> extends Lazy<T> implements HasReset {

    private volatile Out1<Out1<T>> resetter;

    public ResettableLazy(Out1<T> supplier) {
        super(supplier);
        resetter = ()->simpleProxy(supplier);
    }

    public ResettableLazy(Out1<T> supplier, In1<T> spy, boolean spyBeforeUnlock) {
        super(supplier, spy, spyBeforeUnlock);
        resetter = ()-> complexProxy(supplier, spy, spyBeforeUnlock);
    }

    public void bind(ResettableLazy<T> spy) {
        onReset(spy::reset);
    }

    public final synchronized void onReset(Do spy) {
        assert spy != null : "Don't send null callbacks!";
        this.resetter = this.resetter.spy1(spy.ignores1());
    }

    @Override
    public final void reset() {
        proxy = resetter.out1();
    }

    public ResettableLazy<T> setResetter(Out1<Out1<T>> resetter) {
        this.resetter = resetter;
        return this;
    }

    public final void set(Out1<T> proxy) {
        setResetter(Immutable.immutable1(proxy));
        reset();
    }

    public final void set(T value) {
        set(Immutable.immutable1(value));
    }
}
