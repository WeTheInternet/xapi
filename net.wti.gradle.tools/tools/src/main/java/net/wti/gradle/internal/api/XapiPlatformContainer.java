package net.wti.gradle.internal.api;

import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.reflect.Instantiator;

import static org.gradle.api.internal.CollectionCallbackActionDecorator.NOOP;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/27/18 @ 12:04 AM.
 */
public class XapiPlatformContainer extends AbstractNamedDomainObjectContainer<XapiPlatform> {

    private final ObjectFactory objects;

    protected XapiPlatformContainer(
        Instantiator instantiator,
        ObjectFactory objects
    ) {
        super(XapiPlatform.class, instantiator, NOOP);
        this.objects = objects;
    }

    @Override
    protected XapiPlatform doCreate(String name) {
        return new XapiPlatform(objects, name);
    }
}
