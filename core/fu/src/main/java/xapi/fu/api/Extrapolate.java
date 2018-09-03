package xapi.fu.api;

import java.lang.annotation.*;

/**
 * Use this on method parameters to generate extrapolations (overloads) for a functional expression.
 *
 * This will create a block of generated code that duplicates the source method signature,
 * with extra / fewer / changed arguments / method names, to expose "useful helper method wiring"
 * without having to manually wire stuff up yourself.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 8/23/18 @ 2:26 AM.
 */
@Documented
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Extrapolates.class)
public @interface Extrapolate {

    @interface Stretch {
        int n() default -1; // -1 means "take from container @Extrapolate" (so you can apply defaults once)

        /**
         * Add an overload that accepts #n more arguments,
         * and include raw types in the added signatures:
         *
         * void work(@Extrapolate(STRETCH) Do stuff) {}
         *
         * final void <T> work1(In1<T> stuff, T raw1) { work(stuff.provide(raw1)); }
         *
         * Default to true.
         *
         */
        boolean addRaw() default true;

        /**
         * Add an overload that accepts #n more arguments,
         * and include deferred Out1 types in the added signatures:
         *
         * void work(@Extrapolate(STRETCH) Do stuff) {}
         *
         * final void <T> work1Defer(In1<T> stuff, Out1<T> raw1) { work(stuff.provideDeferred(raw1)); }
         *
         * Defaults to true.
         */
        boolean addDefer() default true;

        /**
         * Similar to {@link #addDefer()}, except we'll wrap your Out1 in a Lazy, so it's only ever called once.
         *
         * Defaults to false.
         */
        boolean addLazy() default false;

        /**
         * Similar to {@link #addDefer()}, except we'll unwrap your Out1 immediately,
         * instead of waiting until the "real" function is executed.
         *
         * void work(@Extrapolate(STRETCH) Do stuff) {}
         *
         * final void <T> work1Defer(In1<T> stuff, Out1<T> raw1) { work(stuff.provide(raw1.out1())); }
         *
         * Defaults to false.
         */
        boolean addImmediate() default false;
    }

    /**
     * Used to patterns we want to reuse a lot.
     * like adding utility methods to:
     * Stretch: In1 -> In3,
     * Shrink: In3 -> In1,
     * Unwrap: In1<T> -> T
     * Async: T doStuff(A arg) -> void doStuff(A arg, In2<T, Throwable> callback).
     *
     */
    enum BuiltinExtrapolations {
        /**
         * Effectively `stretch=@Stretch()`,
         * which will default to adding 2 layers of arity to functional parameters:
         *
         * void work(@Extrapolate(STRETCH) Do stuff) {}
         *
         * final void <T1> work1(In1<T1> stuff, T1 raw1) { work(stuff.supply(raw1)); }
         * final void <T1> work1Deferred(In1<T1> stuff, Out1<T1> raw1) { work(stuff.supply(raw1)); }
         * final void <T1, T2> work2(In2<T1, T2> stuff, T1 raw1, T2 raw2) { ... }
         *
         *
         */
        STRETCH,

        SHRINK,
        SUPPLY,
        /**
         * Add a spy to an argument; the overloaded signature will be the same, except an additional
         * callback will be added that is called whenever the SPYd element is invoked.
         * If you Spy on an In, your spy will receive input before the user-supplied callback.
         * If you Spy on an Out, your spy will receive input when the Out is invoked.
         */
        SPY,
        /**
         * For methods that take Out1 or Maybe as arguments,
         * add overloads that just take the raw parameter types.
         *
         * For collection types, add varargs.
         */
        UNWRAP,
        /**
         *
         */
        ASYNC,
        /**
         * Generate the infamous, annoying, "overloads for primitives",
         * so you can avoid boxing (at the cost of compiling more code over and over).
         * (Keep in mind, runtime performance is noticeable to users, while build time only to developers).
         */
        PRIMITIVIZE
    }

    BuiltinExtrapolations[] value() default {};

    /**
     * By default, stretching or shrinking arity will be done with up to two steps.
     *
     * We use 2 because there's already combinatorial explosion w/ raw + defer stretches being default on.
     */
    int n() default 2;

    /**
     * When multiple types of expansions are requested, should those expansions be permuted together?
     *
     * For example, @Extrapolate({STRETCH, PRIMITIVIZE}) will, by default, create 32 new method;
     * 2 permutations for raw@n=2
     * 2 permutations for defer@n=2
     * 8 permutations for primitives.
     *
     * If you don't want this code-spam, use permute=false.
     */
    boolean permute() default true;

    Stretch[] stretch() default {};

}
