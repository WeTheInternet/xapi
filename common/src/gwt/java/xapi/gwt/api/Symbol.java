package xapi.gwt.api;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;

import javax.annotation.Nullable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/16/17.
 */
@JsType(isNative = true, name = "Symbol", namespace = JsPackage.GLOBAL)
public class Symbol {

    @JsMethod(name = "for")
    public static native Symbol fromString(String s);

    /**
     * @param s - The symbol from which to extract the string name.
     * @return - The string name of the symbol,
     * or null (undefined) if the Symbol is one of the global / static
     * Symbols like Symbol.hasInstance or Symbol.toStringTag,
     * which do not have a name ( Symbol.hasInstance != Symbol.for('hasInstance') ).
     */
    @JsMethod(name = "keyFor")
    public static native @Nullable String toString(Symbol s);

    @JsProperty(name="toStringTag")
    public static native Symbol toStringTag();

    /**
     * @return The symbol to use to override the behavior of instanceof.
     *
     * Note that this symbol is a magic, static symbol;
     * Symbol.hasInstance != Symbol.for('hasInstance'),
     * as such, you must use this instance for your nefarious purposes.
     *
     * Also note that this, and all other static Symbols, do not have a name.
     * They have an accessor (this static method), which you can use to do == checks,
     * but they will not resolve to a string, as all strings are potential symbols.
     *
     *
     * From https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Symbol/hasInstance
     * <code>
     * class MyArray {
     *   static [Symbol.hasInstance](instance) {
     *     return Array.isArray(instance);
     *   }
     * }
     * console.log([] instanceof MyArray); // true
     *
     * </code>
     */
    @JsProperty(name = "hasInstance")
    public static native Symbol hasInstance();

    @JsOverlay
    public static boolean isSymbol(Object name) {
        return "symbol".equals(Js.typeof(name));
    }

}
