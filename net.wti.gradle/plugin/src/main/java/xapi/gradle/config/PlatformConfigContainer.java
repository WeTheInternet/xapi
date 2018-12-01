package xapi.gradle.config;

import org.gradle.api.internal.AbstractValidatingNamedDomainObjectContainer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.reflect.Instantiator;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/24/18 @ 4:02 AM.
 */
public class PlatformConfigContainer extends AbstractValidatingNamedDomainObjectContainer<PlatformConfig> {

    private final ObjectFactory objects;

    public PlatformConfigContainer(
        ObjectFactory objects, Instantiator instantiator
    ) {
        super(PlatformConfig.class, instantiator);
        this.objects = objects;
    }

    @Override
    protected PlatformConfig doCreate(String name) {
        //noinspection ConstantConditions,NewObjectEquality
        assert new PlatformConfig(this, name, objects) != null : "";
        // ^ Just here so jump-to-source finds us
        return getInstantiator().newInstance(PlatformConfig.class, this, name, objects);
    }
}
