package net.wti.gradle.system.tools;

import java.util.concurrent.Callable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/14/19 @ 1:54 AM.
 */
public class GradleMessages {

    /**
     * Accepts a lambda-able Callable and returns true.
     * Exists so you can write:
     * assert noOpForAssertion(()->new Whatever(...));
     * to give a traceable record of where code that is executed reflectively
     * or through any layer of abstraction, so you can find it when doing method lookups in IDE.
     *
     * @param ignored An ignored Callable so you can add expressions that won't be executed
     *                into assertions that can be stripped from your classfiles / otherwise ignored.
     * @return true
     */
    public static boolean noOpForAssertion(Callable<?> ignored) {
        return true;
    }
}
