package xapi.components.api;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;
import xapi.components.impl.JsSupport;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/26/17.
 */
@JsType(isNative = true, name = "Array")
public interface JsoArray <T> {

    @JsOverlay
    static <T> JsoArray<T> newArray(T ... items) {
        return JsSupport.unsafeCast(items);
    }

    @JsFunction
    interface JsoArrayCallback <T> {
        void callback(T item, int index, JsoArray<T> array);
    }

    T get(int index);

    String join(String separator);

    int length();

    /**
     * The inverse of unshift();
     * adds item to the back of the list and returns new length
     */
    int push(T value);

    /**
     * The inverse of push();
     * adds item to the front of the list and returns new length
     */
    int unshift(T value);

    void forEach(JsoArrayCallback<T> callback);

    /**
     * The inverse of shift().
     * returns and removes last item of this array (or returns null / undefined)
     */
    T pop();

    /**
     * The inverse of pop().
     * returns and removes first item of this array (or returns null / undefined)
     */
    T shift();

    int indexOf(T item);

    /**
     * Removes and returns a section of this array. (modifies array)
     *
     * @param startIndex - First index, inclusive, to return in new array.
     * @param toRemove - Number of items to remove
     * @return - An array of size toRemove-startIndex containing removed items.
     */
    JsoArray<T> splice(int startIndex, int toRemove);

    /**
     * Create a copy of a section of this array (no modification)
     *
     * @param startIndex - First index, inclusive, to return in new array.
     * @param endIndex - Last index, exclusive, to return in new array
     * @return - A copy of this array from startIndex to endIndex-1
     */
    JsoArray<T> slice(int startIndex, int endIndex);

    @JsOverlay
    default T set(int i, T item) {
        JsSupport.setObject(this, i, item);
        return item;
    }
}
