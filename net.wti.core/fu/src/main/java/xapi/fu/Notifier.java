package xapi.fu;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/2/16.
 */
public class Notifier<T> {

    In2[] listeners = new In2[0];

    public Notifier<T> listen(In2<T, T> listener) {
        listeners = X_Fu.push(listeners, listener);
        return this;
    }
    public Notifier<T> notifyListeners(T oldValue, T newValue) {
        for (In2 listener : listeners) {
            listener.in(oldValue, newValue);
        }
        return this;
    }

}
