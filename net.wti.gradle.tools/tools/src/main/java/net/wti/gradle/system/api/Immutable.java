package net.wti.gradle.system.api;

import java.util.concurrent.Callable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2/1/19 @ 12:03 AM.
 */
public class Immutable <T> implements Callable<T> {
    private T value;

    public Immutable(T value) {
        this.value = value;
    }

    @Override
    public T call() {
        return value;
    }
}
