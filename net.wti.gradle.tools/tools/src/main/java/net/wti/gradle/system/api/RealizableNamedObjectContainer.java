package net.wti.gradle.system.api;

import org.gradle.api.NamedDomainObjectContainer;

import java.util.Collection;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/14/19 @ 2:31 AM.
 */
public interface RealizableNamedObjectContainer <T> extends NamedDomainObjectContainer<T> {

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
}
