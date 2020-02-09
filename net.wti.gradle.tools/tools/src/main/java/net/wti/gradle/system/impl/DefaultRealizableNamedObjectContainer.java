package net.wti.gradle.system.impl;

import net.wti.gradle.system.api.RealizableNamedObjectContainer;
import org.gradle.api.Namer;
import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.internal.reflect.Instantiator;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/14/19 @ 2:40 AM.
 */
public abstract class DefaultRealizableNamedObjectContainer <T> extends AbstractNamedDomainObjectContainer<T>
    implements RealizableNamedObjectContainer<T> {

    protected DefaultRealizableNamedObjectContainer(
        Class<T> type,
        Instantiator instantiator,
        Namer<? super T> namer,
        CollectionCallbackActionDecorator callbackDecorator
    ) {
        super(type, instantiator, namer, callbackDecorator);
    }

    protected DefaultRealizableNamedObjectContainer(
        Class<T> type,
        Instantiator instantiator,
        CollectionCallbackActionDecorator callbackActionDecorator
    ) {
        super(type, instantiator, callbackActionDecorator);
    }

    @Override
    public boolean hasWithName(String name) {
        return super.hasWithName(name);
    }

}
