package net.wti.gradle.test.api;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/15/19 @ 1:37 AM.
 */
public interface HasWork {
    default Boolean hasWorkRemaining() {
        return false;
    }

    default void doWork() {

    }

    default void flush() {
        doFlush();
    }

    default void doFlush() {
        while(hasWorkRemaining()) {
            doWork();
        }
    }
}
