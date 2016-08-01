package xapi.util.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.util.api.HasId;
import xapi.util.api.IdGenerator;

import java.util.concurrent.atomic.AtomicInteger;

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
            synchronized (ids) {
                try {
                    if (from != recursionSickness) {
                        recursionSickness = from;
                        return ((HasId)from).getId();
                    }
                } finally {
                    recursionSickness = null;
                }
            }
        }
        int newId = ids.getAndIncrement();
        return Integer.toString(newId, 36);
    }
}
