package net.wti.gradle.system.api;

import org.gradle.api.NamedDomainObjectContainer;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/14/19 @ 2:31 AM.
 */
public interface RealizableNamedObjectContainer <T> extends NamedDomainObjectContainer<T> {

    /**
     * This method raises the visibility of the same-sig protected method
     * found in DefaultNamedDomainObjectCollection.
     *
     * The presence of this method forces you to expose a public override.
     * Just `return super.hasWithName(name);`.
     *
     * @param name The key to check
     * @return true if the object is registered
     */
    boolean hasWithName(String name);

    default RealizableNamedObjectContainer<T> realize() {
        // This will call org.gradle.api.internal.DefaultDomainObjectCollection.addEagerAction
        // which is what will realize the objects in this container.  It's a shame this isn't public API.
        whenObjectAdded(ignored->{});
        return this;
    }

    default RealizableNamedObjectContainer<T> realizeInto(Collection<T> into) {
        // This will call org.gradle.api.internal.DefaultDomainObjectCollection.addEagerAction
        // which is what will realize the objects in this container.  It's a shame this isn't public API.
        whenObjectAdded(into::add);
        whenObjectRemoved(into::remove);
        return this;
    }

    default boolean anyMatch(Predicate<? super T> test) {
        realize();
        for (T item : this) {
            if (test.test(item)) {
                return true;
            }
        }
        return false;
    }

    default boolean allMatch(Predicate<? super T> test) {
        realize();
        for (T item : this) {
            if (!test.test(item)) {
                return false;
            }
        }
        return true;
    }
}
