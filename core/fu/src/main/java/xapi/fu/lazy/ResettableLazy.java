package xapi.fu.lazy;

import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
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
    public void reset() {
        proxy = resetter.out1();
    }
}
