package xapi.gradle.config;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Named;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.SourceSet;
import xapi.fu.Maybe;
import xapi.fu.X_Fu;
import xapi.fu.itr.MappedIterable;
import xapi.gradle.api.*;

import static xapi.fu.itr.EmptyIterator.none;
import static xapi.fu.itr.MappedIterable.mapped;
import static xapi.fu.itr.SingletonIterator.singleItem;

/**
 * A descriptor object for a give xapi platform.
 *
 * xapi {
 *     platform {
 *         dev {
 *             // adds all appropriate dependencies to all appropriate configurations
 *             withProject 'xapi-inject'
 *             withModule 'com.foo:my-xapi-module'
 *         }
 *         myAppDev {
 *             // add the dependencies (and outputs) of other platforms
 *             extend 'dev'
 *             // explicit archive list, to auto-generate projects / source folders for us.
 *             archive 'MAIN', 'API', 'SPI', 'STUB'
 *         }
 *         api {
 *             // not configuring anything,
 *             // but the mere presence here will add default source sets / wiring,
 *             // which can trigger the IDE to auto-generate source directories for you.
 *         }
 *     }
 * }
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/24/18 @ 3:54 AM.
 */
public class PlatformConfig implements Named  {

    private final ListProperty<ArchiveType> archives;
    private final ListProperty<PlatformConfig> extend;
    private final PlatformConfigContainer container;
    private final ArchiveType type;
    private String name;
    private SourceConfig sources;

    public PlatformConfig(
        PlatformConfigContainer container,
        String name,
        ObjectFactory objects
    ) {
        this.name = name;
        this.type = PlatformType.find(name)
            .getOrThrow(()->
                new IllegalStateException("You must call PlatformType.register(name, new ConfigurableArchiveType(name)) as early as possible in root build script")
            );
        this.archives = objects.listProperty(ArchiveType.class);
        this.extend = objects.listProperty(PlatformConfig.class);
        archives.set(singleItem(DefaultArchiveType.MAIN));
        extend.set(none());
        this.container = container;
    }

    @Override
    public String getName() {
        return name;
    }

    public PlatformConfig withProject(String projectName) {
        throw new UnsupportedOperationException("withProject not supported yet");
//        return this;
    }
    public PlatformConfig withModule(String moduleName) {
        throw new UnsupportedOperationException("withModule not supported yet");
//        return this;
    }
    public PlatformConfig extend(String otherPlatform) {
        final PlatformConfig other = container.findByName(otherPlatform);
        if (other == null) {
            throw new InvalidUserDataException(otherPlatform + " does not exist yet");
        }
        extend(other);
        return this;
    }
    public PlatformConfig extend(PlatformConfig otherPlatform) {
        extend.add(otherPlatform);
        return this;
    }
    public PlatformConfig archive(Object ... types) {
        archives.addAll(
            mapped(types).map(ArchiveType::coerceArchiveType)
        );
        return this;
    }

    public ListProperty<ArchiveType> getArchives() {
        return archives;
    }

    public MappedIterable<ArchiveType> archiveTypes() {
        return MappedIterable.mapped(archives.get());
    }

    public ArchiveType getType() {
        return type;
    }

    public void setSources(SourceConfig dev) {
        this.sources = dev;
    }

    public SourceConfig getSources() {
        return sources;
    }
}
