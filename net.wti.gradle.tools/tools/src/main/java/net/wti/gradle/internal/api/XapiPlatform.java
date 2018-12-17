package net.wti.gradle.internal.api;

import org.gradle.api.DomainObjectSet;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.DefaultDomainObjectSet;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import java.util.Set;

import static org.gradle.api.internal.CollectionCallbackActionDecorator.NOOP;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/10/18 @ 2:53 AM.
 */
public class XapiPlatform implements XapiVariant {

    private final String name;
    private final Property<DomainObjectSet<XapiUsageContext>> usages;

    public XapiPlatform(ObjectFactory objects, String name) {
        this.name = name;
        this.usages = objects.property(Class.class.cast(DomainObjectSet.class));
        usages.set(new DefaultDomainObjectSet<>(XapiUsageContext.class, NOOP));
    }

    @Override
    public DomainObjectSet<XapiUsageContext> getUsages() {
        return usages.get();
    }

    @Override
    public String getName() {
        return name;
    }
}
