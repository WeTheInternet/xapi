package xapi.fu.api;

import xapi.fu.Rethrowable;
import xapi.fu.ReturnSelf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Idea for an interface contract regarding runtime type composition.
 *
 * The Mixin interface itself adds a {@code "T add(Class<T>)"} method, which takes a class,
 * and returns an instance of that class derived from it's self(),
 * or looks for a static method with signature `Object mixin(Class, Object)`
 * on the class sent to add().
 *
 * You are encouraged to override add(), and really only call Mixin.super.add if you have to give up.
 *
 * The flagship implementations for mixins will be composable functional interfaces,
 * where you can add features that you want your resulting object to have,
 * like Serializability, argument/result caching/spying, error handlers, facades, etc,
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 8/26/18 @ 4:37 AM.
 */
public interface Mixin <Self extends ReturnSelf> extends ReturnSelf<Self>, Rethrowable {

    /**
     * Add a given type to your self().
     *
     * Unless this method is overridden, the object returned by self()
     * must implement the class you send, or the class you send must have a
     * <pre>
     * static <A, E extends A> A mixin(Class<E> type, Object self) {
     *     // do the work to "return some A that wraps / is implemented by `self`"
     * }
     * </pre>
     *
     * It is up to the implementors of both Mixin.add and AddedType.mixin which
     * determine what happens when this method is called (overrider first, obviously).
     *
     * By default, if self is an instance of the type, you get the type back,
     * otherwise we call out to AddedType.mixin static method,
     * where you might create new instances of the type,
     * or you may simply add the class to some builder,
     * or (if you are in an appropriate vm) create a proxy object
     *  (TODO: a jvm-only module to create smart proxy mixins).
     *
     *
     * @param type The type which must be added / returned from self().
     *             By default, if type.isInstance(self()), we will return self() directly,
     *             but overriding code may not want to / remember to do this check.
     *
     *             For example, it is perfectly valid to want to make a "factory factory",
     *             so a "factory mixin" would want to override this method and expressly never call super.
     *
     * @param <Add> The base type of the thing you want (lower bound of what you expect to get back
     * @param <E> An extension of Add that exists merely to allow you to get complete type parameters
     *           from an arbitrary class literal:
     *           List<String> added = add(List.class); // example only, plz don't expect to work for free
     * @return An instance of Add which should be derived from self().
     *
     *         Note that we use self() instead of `this`,
     *         because we may want to transform a type into a mutable form / BuilderOfType
     *         whenever anything is add()ed, where we want a single "other form" for self(),
     *         which can get more complex by creating a new delegate, and having all other self() references
     *         pointing to the updated copy, instead of some arbitrary `this`.
     */
    @SuppressWarnings({
        // We cheat; if assertions are enabled, we may give back way longer error messages.
        // Always run with assertions on all the time, except in production
        // (unless you really, really, REALLY, *REALLY* *NEED* to).
        "AssertWithSideEffects", "ConstantConditions",
        // we actually did check and throw, so this is valid to suppress
        "unchecked"})
    default <Add, E extends Add> Add add(Class<E> type) {
        final Self self = self();
        if (type.isInstance(self)) {
            return type.cast(self);
        }
        try {
            final Method method = type.getMethod("mixin", Class.class, Object.class);
            try {
                final Object result = method.invoke(null, type, self);
                if (!type.isInstance(result)) {
                    String msg = method + " returned " + result + " which is not an instance of " + type;

                    // we cheat, and give more details when assertions are compiled in.
                    assert null != (msg +=
                        result == null ? "null is unacceptable" :
                        result.getClass().getCanonicalName() + " should " +
                            (type.isInterface() ? "implement" : "extend") + " " + type.getCanonicalName()
                        // TODO: add sample code here once we figure it out in tests.
                    );

                    throw rethrow(new IllegalArgumentException(msg));
                }
                return (Add) result;
            } catch (IllegalAccessException e) {
                // let our rethrow method see our failures...
                throw rethrow(new IllegalArgumentException(method + " must be public"));
            } catch (InvocationTargetException e) {
                throw rethrowCause(e);
            }
        } catch (NoSuchMethodException e) {
            // consider being lenient?
            // maybe looking in a super type, or for some other builder-y type in self()?
            String msg = "Cannot find " + type + ".mixin; add -ea for details";

            // we cheat... this assert is always true, and we use it to give a better message (as promised in production error message)
            assert null != (msg = "In order to use " + type + " as a mixin target, you must implement a\n" +
                "public static Object mixin(Class type, " + self.getClass().getCanonicalName() + " type){} method " +
                "where you must return 'your self() with the given interface mixed in'.\n" +
                "In most cases, your self() will be a builder that adds the class to it's own factory mechanations.\n\n" +
            // need to be able to assert on gwt runtime here, to reduce -ea spam...
                "Gwt users will also need to make sure that your mixin method is retained:\n" +
                "static {\n" +
                "  " + type.getCanonicalName() + ".class.getMethod(\"mixin\", Class.class, " + self.getClass().getSimpleName() + ".class);" +
                "\n} // retains runtime reflection whenever class-you-put-this-in is loaded");

            throw rethrow(new IllegalArgumentException(msg));
        }
    }

}
