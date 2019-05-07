package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.schema.api.ArchiveConfigContainer;
import net.wti.gradle.schema.api.PlatformConfig;
import net.wti.gradle.schema.api.PlatformConfigContainer;

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

    private final PlatformConfigContainer container;
    private PlatformConfigInternal parent;
    private Map<PlatformConfig, PlatformConfig> children = new IdentityHashMap<>();
    private boolean requireSource;
    private boolean test;

    public DefaultPlatformConfig(
        String name,
        PlatformConfigContainer container,
        ProjectView view,
        ArchiveConfigContainer schemaArchives
    ) {
        this(null, name, container, view, schemaArchives);
    }

    protected DefaultPlatformConfig(
        PlatformConfigInternal parent,
        String name,
        PlatformConfigContainer container,
        ProjectView view,
        ArchiveConfigContainer schemaArchives
    ) {
        this.name = name;
        // TODO: A smart delegate where we can check the platform for archives first,
        // then default to the schema itself.
        archives = new DefaultArchiveConfigContainer(()->this, view);

        schemaArchives.configureEach(item ->{
            if (item.getPlatform().getName().equals(name)) {
                archives.maybeCreate(item.getName()).baseOn(item);
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
    public void setParent(PlatformConfigInternal parent) {
        this.parent = parent;
    }

    @Override
    public void baseOn(PlatformConfig rooted) {
        this.requireSource = rooted.isRequireSource();
        this.test = rooted.isTest();
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
            ", children=" + children.keySet() +
            ", requireSource=" + requireSource +
            ", test=" + test +
            '}';
    }
}
