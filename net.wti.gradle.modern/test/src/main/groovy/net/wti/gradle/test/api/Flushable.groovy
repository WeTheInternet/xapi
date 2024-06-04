package net.wti.gradle.test.api

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/15/19 @ 1:37 AM.
 */
trait Flushable {

    Boolean hasWorkRemaining() {
        return false
    }

    void doWork() {

    }

    void flush() {
        doFlush()
    }

    void doFlush() {
        while(hasWorkRemaining()) {
            doWork()
        }
    }
}
