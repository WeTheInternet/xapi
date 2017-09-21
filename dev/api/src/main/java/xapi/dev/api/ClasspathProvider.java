package xapi.dev.api;

import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.Out1;
import xapi.fu.ReturnSelf;
import xapi.scope.X_Scope;
import xapi.scope.api.Scope;
import xapi.time.X_Time;

/**
 * A provider of {@link Classpath} objects, which stashes itself onto a Scope object,
 * so we can cache potentially expensive dependency resolution results.
 *
 * Each instance of this class should be a distinct type (anonymous class, lambda, etc),
 * or override {@link ClasspathProvider#selfType()} to return a stable class key.
 *
 * Beware that lambdas and method references create a new dynamic type whenever
 * the method reference expression is evaluated (even if it's the same line of
 * code executed more than once).  So, with great power comes great responsibility.
 *
 * ClasspathProvider cp = scope->Lazy.deferred1(this::thingThatReturnsClasspath);
 * cp.preloadClasspath(X_Scope.currentScope(), X_Time::runLater);
 * // later
 * for (String path : cp.provideClasspath(scope).getPaths()) {
 *     // add path to something
 * }
 *
 * While this looks nice, it will do nasty things to you.
 * Compare it to this:
 *
 * Lazy<Classpath> provider = Lazy.deferred1(this::thingThatReturnsClasspath);
 * ClasspathProvider cp = scope->provider;
 * // ... do stuff
 *
 *
 * For most cases, it is best to just create a named type (even within a method):
 *
 * public Class<? extends ClasspathProvider> registerClasspath(Out1<Classpath> provider){
 *
 *     Lazy<Classpath> provider = Lazy.deferred1(provider);
 *     final class MyCp implements ClasspathProvider<MyCp>{
 *         public Lazy<Classpath> loadClasspath(Scope s) {
 *           return provider;
 *         }
 *     }
 *     new MyCp().preloadClasspath(scope, Do::done, classpath->doSomething(classpath));
 *     return MyCp.class;
 * }
 *
 * better:
 *
 * public final class CPMeaningfulName implements ClasspathProvider<CPMeaningfulName> {
 *     private static final CPMeaningfulName INSTANCE = new CPMeaningfulName();
 *     private final Lazy<Classpath> provider;
 *     private CPMeaningfulName() {
 *         provider = Lazy.deferred1(this::createClasspath);
 *     }
 *     private Classpath createClasspath() {
 *       // your logic here...
 *
 *     }
 * }
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/10/17.
 */
public interface ClasspathProvider <Self extends ClasspathProvider<Self>> extends ReturnSelf<Self>{

    boolean AUTO_PRELOAD = "true".equals(System.getProperty("xapi.classpath.preload", "true"));

    /**
     * This allows custom classes to statically preload their payloads (opt-in, by calling this method);
     * assumes, of course, that GlobalScope is used, or that you are initializing some custom root-like Scope
     * before this type can possibly be loaded.
     *
     * TODO: Register partially-evaluated classpaths to start initializing in the global scope,
     * and then finish them off in the local scope.  First pass preloads anything it can,
     * so that we use local scope when the Classpath is fully resolved.
     *
     * Tracking dynamic versus static nodes remaining in a classpath variable (or any expression)
     * would allow us to know when we're down to just hardcoded strings we can emit with optimal efficiently.
     *
     *
     */
    static void preload(ClasspathProvider<?> preload) {
        if (AUTO_PRELOAD) {
            preload.preloadClasspath(X_Scope.currentScope(), X_Time::doLater);
        }
    }

    default Classpath provideClasspath(Scope s) {
        Self instance = s.getOrCreate(selfType(), cls->self());
        final Out1<Classpath> provider = instance.loadClasspath(s);
        return provider.out1();
    }

    /**
     * Preloads the lazy classpath provider, using the executor you send.
     *
     * // async preload
     * myClasspath.preloadClasspath(X_Scope.currentScope(), X_Time::runLater);
     * // sync preload
     * myClasspath.preloadClasspath(X_Scope.currentScope(), Do::done);
     *
     * @param s
     * @param executor
     */
    default void preloadClasspath(Scope s, In1<Do> executor) {
        loadClasspath(s, executor, In1.ignored());
    }

    default void loadClasspath(Scope s, In1<Do> executor, In1<Classpath> callback) {
        // Yes, if there is an older instance of `this`, use it...
        Self instance = s.getOrCreate(selfType(), cls->self());
        // TODO if (instance != this) { // meaningful debug message }
        final Out1<Classpath> provider = instance.loadClasspath(s);
        // does not execute yet
        final Do todo = provider.spy1(callback).ignoreOut1();
        // let executor decide whether to be sync or async
        executor.in(todo);
    }

    default Class<Self> selfType() {
        return Class.class.cast(self().getClass());
    }

    /**
     * You get a scope and must create a Classpath instance from it.
     *
     * It is HIGHLY RECOMMENDED for you to return an instance of Lazy,
     * to ensure caching occurs (we can't Lazy-wrap from here).
     *
     * If you explicitly don't want caching, any attempts to preload
     * will just waste even more CPU time, unless your own code has some caching involved.
     *
     * You should not have to store anything on the Scope, provided you are not
     * creating reusable ClasspathProvider classes...  Each instance should be
     * a local / lambda / private class which represents a single classpath.
     *
     * The key returned by #selfType(), by default, is the class of `this` instance,
     * which is used as the key to store the instance in your state object.
     *
     * This can be dangerous if you use lambdas/method references, and expect them to have the stable classes.
     * Each instance of a dynamic (lambda) type has it's own class instance,
     * so keys for them are not stable.
     *
     * This maybe be what you want when you don't have namespace to create concrete, named types,
     * and are careful to either:
     *
     * A) store single, reused instances of your lambda:
     * final ClasspathProvider myFactory = scope->someLazyInstance;
     *
     * or B) collect up the classes from your lambdas to reuse later:
     * final ClasspathProvider myFactory = scope->someLazyInstance;
     * Class<? extends ClasspathProvider> key = myFactory.getClass();
     * // later
     * Classpath cp = scope.get(key).provideClasspath(scope);
     *
     * See class javadoc for more info.
     *
     */
    Out1<Classpath> loadClasspath(Scope s);

    default Out1<? extends Iterable<String>> getPaths(Scope scope) {
        return ()->loadClasspath(scope).out1().getOrCreatePaths();
    }
}
