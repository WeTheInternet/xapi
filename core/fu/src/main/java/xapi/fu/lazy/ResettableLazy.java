package xapi.fu.lazy;

import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.has.HasReset;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/9/17.
 */
public class ResettableLazy <T> extends Lazy<T> implements HasReset {

    private final Out1<T> source;

    public ResettableLazy(Out1<T> supplier) {
        super(supplier);
        this.source = supplier;
    }

    @Override
    public void reset() {
        proxy = proxySupplier(source);
    }
}
