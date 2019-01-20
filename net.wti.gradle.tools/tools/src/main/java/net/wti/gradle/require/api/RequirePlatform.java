package net.wti.gradle.require.api;

import net.wti.gradle.schema.internal.XapiRegistration;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.DefaultNamedDomainObjectList;
import org.gradle.api.internal.collections.CollectionFilter;
import org.gradle.internal.reflect.Instantiator;

import static org.gradle.api.Named.Namer.forType;

/**
 * A xapiRequire DSL for a specific platform:
 * xapiRequire {
 *     // The gwt{} block here is a RequirePlatform; xapiRequire itself is a container for RequirePlatforms
 *     gwt {
 *         // applies to _all_ gwt platform archives
 *         external 'a:b:c'
 *         main {
 *             // applies only to gwt:main archive.
 *             external 'x.y.z'
 *         }
 *     }
 * }
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/4/19 @ 10:45 PM.
 */
public class RequirePlatform extends DefaultNamedDomainObjectList<XapiRegistration> {
    public RequirePlatform(
        DefaultNamedDomainObjectList<? super XapiRegistration> objects,
        CollectionFilter<XapiRegistration> filter,
        Instantiator instantiator
    ) {
        super(objects, filter, instantiator, forType(XapiRegistration.class));
    }

    public RequirePlatform(
        Instantiator instantiator,
        CollectionCallbackActionDecorator decorator
    ) {
        super(XapiRegistration.class, instantiator, forType(XapiRegistration.class), decorator);
    }
}
