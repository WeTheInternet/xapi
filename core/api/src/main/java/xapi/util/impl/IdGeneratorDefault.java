package xapi.util.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.fu.Lazy;
import xapi.util.api.HasId;
import xapi.util.api.IdGenerator;

import java.util.concurrent.atomic.AtomicInteger;

import static xapi.fu.Out1.out1Deferred;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/29/16.
 */
@InstanceDefault(implFor = IdGenerator.class)
public class IdGeneratorDefault <T> implements IdGenerator<T> {

    private final AtomicInteger ids = new AtomicInteger();
    T recursionSickness;
    @Override
    public String generateId(T from) {
        if (from instanceof HasId) {
            // prevent HasId instances which defer to this IdGenerator
            // from causing recursion sickness; if they have a known
            // id, we want to use it, otherwise, we want to prevent
            // infinite recursion by storing a reference to the object,
            // so we can detect and avoid recursion.
            final T was;
            synchronized (ids) {
                was = recursionSickness;
                try {
                    if (from != was) {
                        recursionSickness = from;
                        return ((HasId)from).getId();
                    }
                } finally {
                    recursionSickness = was;
                }
            }
        }
        int newId = ids.getAndIncrement();
        return Integer.toString(newId, 36);
    }

    @Override
    public Lazy<String> lazyId(T from) {
        return Lazy.deferred1(()->{
            recursionSickness = from;
            try {
                return generateId(from);
            } finally {
                recursionSickness = null;
            }
        });
    }

}
