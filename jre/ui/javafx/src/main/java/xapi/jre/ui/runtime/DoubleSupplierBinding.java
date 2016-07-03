package xapi.jre.ui.runtime;

import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableDoubleValue;
import xapi.fu.Notifier;
import xapi.fu.Out1;
import xapi.fu.X_Fu;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/1/16.
 */
public class DoubleSupplierBinding implements ObservableDoubleValue {

    private final Out1<Double> value;
    private final Map<ChangeListener<? super Number>, Void> changeListeners;
    private final Map<InvalidationListener, Void> invalidationListeners;

    private DoubleSupplierBinding(Out1<Double> value) {
        this.value = value.mapIf(X_Fu::isNull, d->defaultValue());
        changeListeners = new IdentityHashMap<>();
        invalidationListeners = new IdentityHashMap<>();
    }

    protected double defaultValue() {
        return -1;
    }

    public static DoubleSupplierBinding valueOf(Out1<Double> value) {
        return new DoubleSupplierBinding(value);
    }

    public static DoubleSupplierBinding valueOf(IntSupplier value) {
        return new DoubleSupplierBinding(()->(double)value.getAsInt());
    }
    public static DoubleSupplierBinding valueOf(LongSupplier value) {
        return new DoubleSupplierBinding(()->(double)value.getAsLong());
    }

    public DoubleSupplierBinding bindNotifier(Notifier<? extends Number> notifier) {
        notifier.listen((oldV, newV)-> {
            // per source code in javafx ExpressionHelper class, we fire invalidations before changes.
            invalidationListeners.keySet()
                  .forEach(l->l.invalidated(DoubleSupplierBinding.this));
           changeListeners.keySet()
                 .forEach(l->l.changed(DoubleSupplierBinding.this, oldV, newV));
        }
        );
        return this;
    }

    @Override
    public double get() {
        return value.out1();
    }

    @Override
    public Double getValue() {
        return value.out1();
    }

    @Override
    public void addListener(InvalidationListener observer) {
        invalidationListeners.put(observer, null);
    }

    @Override
    public void addListener(ChangeListener<? super Number> listener) {
        changeListeners.put(listener, null);
    }

    @Override
    public void removeListener(InvalidationListener observer) {
        invalidationListeners.put(observer, null);
    }

    @Override
    public void removeListener(ChangeListener<? super Number> listener) {
        changeListeners.remove(listener);
    }

    @Override
    public int intValue() {
        return value.out1().intValue();
    }

    @Override
    public long longValue() {
        return value.out1().longValue();
    }

    @Override
    public float floatValue() {
        return value.out1().floatValue();
    }

    @Override
    public double doubleValue() {
        return value.out1();
    }
}
