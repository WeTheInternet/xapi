package net.wti.gradle.require.api;

import net.wti.gradle.schema.internal.XapiRegistration;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.DefaultNamedDomainObjectList;
import org.gradle.api.internal.collections.CollectionFilter;
import org.gradle.internal.reflect.Instantiator;

import static org.gradle.api.Named.Namer.forType;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/4/19 @ 10:45 PM.
 */
public class RequireProject extends DefaultNamedDomainObjectList<XapiRegistration> {
    public RequireProject(
        DefaultNamedDomainObjectList<? super XapiRegistration> objects,
        CollectionFilter<XapiRegistration> filter,
        Instantiator instantiator
    ) {
        super(objects, filter, instantiator, forType(XapiRegistration.class));
    }

    public RequireProject(
        Instantiator instantiator,
        CollectionCallbackActionDecorator decorator
    ) {
        super(XapiRegistration.class, instantiator, forType(XapiRegistration.class), decorator);
    }
}
