package xapi.gradle.api;

import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import org.gradle.internal.reflect.Instantiator;

/**
 * An object to configure / expose the various source directories of a xapi module.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/15/18 @ 12:28 AM.
 */
public class SourceConfigContainer extends DefaultNamedDomainObjectSet<SourceConfig> {

    public SourceConfigContainer(Instantiator instantiator) {
        super(SourceConfig.class, instantiator);
    }
}
