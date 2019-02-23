package net.wti.gradle.schema.internal;

import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.ArchiveConfigContainer;
import net.wti.gradle.schema.api.PlatformConfig;
import net.wti.gradle.schema.api.PlatformConfigContainer;
import org.gradle.api.internal.CompositeDomainObjectSet;
import org.gradle.internal.reflect.Instantiator;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 2:33 PM.
 */
public class DefaultPlatformConfig implements PlatformConfigInternal {

    private static final Pattern NEED_SOURCES = Pattern.compile("gwt|j2cl|j2objc", Pattern.CASE_INSENSITIVE);

    private final String name;
    private final DefaultArchiveConfigContainer archives;
    private final CompositeDomainObjectSet<ArchiveConfig> allArchives;

    private final PlatformConfigContainer container;
    private PlatformConfigInternal parent;
    private Map<PlatformConfig, PlatformConfig> children = new IdentityHashMap<>();
    private boolean requireSource;
    private boolean test;

    public DefaultPlatformConfig(
        String name,
        PlatformConfigContainer container,
        Instantiator instantiator,
        ArchiveConfigContainer schemaArchives
    ) {
        this(null, name, container, instantiator, schemaArchives);
    }

    protected DefaultPlatformConfig(
        PlatformConfigInternal parent,
        String name,
        PlatformConfigContainer container,
        Instantiator instantiator,
        ArchiveConfigContainer schemaArchives
    ) {
        this.name = name;
        // TODO: A smart delegate where we can check the platform for archives first,
        // then default to the schema itself.
        archives = new DefaultArchiveConfigContainer(()->this, instantiator);
        allArchives = CompositeDomainObjectSet.create(ArchiveConfig.class, archives);
        allArchives.addCollection(archives);

        schemaArchives.configureEach(item ->{

            if (item.getPlatform().getName().equals(name)) {
                archives.add(item);
            }
        });
        this.parent = parent;
        this.container = container;
        requireSource = NEED_SOURCES.matcher(name).find();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PlatformConfigInternal getParent() {
        return parent;
    }

    @Override
    public ArchiveConfigInternal getMainArchive() {
        return archives.maybeCreate(getName());
    }

    @Override
    public boolean isRequireSource() {
        return requireSource;
    }

    @Override
    public ArchiveConfigContainerInternal getArchives() {
        return archives;
    }

    @Override
    public CompositeDomainObjectSet<ArchiveConfig> getAllArchives() {
        return allArchives;
    }

    @Override
    public void setRequireSource(boolean requires) {
        this.requireSource = requires;
    }

    @Override
    public void replace(CharSequence named) {
        replace(container.getByName(String.valueOf(named)));
    }

    @Override
    public void replace(PlatformConfig named) {
        assert named instanceof PlatformConfigInternal : named.getClass() + " must implement PlatformConfigInternal";
        this.parent = (PlatformConfigInternal) named;
        this.parent.addChild(this);
    }

    @Override
    public void addChild(PlatformConfig platform) {
        assert this != platform;
        children.put(platform, platform);
    }

    @Override
    public boolean isTest() {
        return test;
    }

    @Override
    public void setTest(boolean test) {
        this.test = test;
    }

    @Override
    public String toString() {
        return "DefaultPlatformConfig{" +
            "name='" + name + '\'' +
            ", archives=" + archives +
            ", parent=" + (parent == null ? "null" : parent.getName()) +
            ", children=" + children +
            ", requireSource=" + requireSource +
            ", test=" + test +
            '}';
    }
}
