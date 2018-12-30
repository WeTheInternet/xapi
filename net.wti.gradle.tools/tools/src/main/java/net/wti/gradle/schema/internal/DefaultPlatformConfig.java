package net.wti.gradle.schema.internal;

import net.wti.gradle.schema.api.*;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.util.GUtil;

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
//    private final SourceSetContainer srcs;
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
        archives = new DefaultArchiveConfigContainer(instantiator);
        schemaArchives.configureEach(archives::add);
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
    public ArchiveConfigContainer getArchives() {
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
    public String sourceName(String archive) {
        return isRoot() ? archive :
            "main".equals(archive) || getName().equals(archive) ? getName() :
            getName() + GUtil.toCamelCase(archive);
    }

    @Override
    public boolean isTest() {
        return test;
    }

    @Override
    public SourceMeta sourceFor(SourceSetContainer srcs, ArchiveConfig archive) {
//        assert archives.contains(archive) : "Archive " + archive + " does not belong to " + this;
        final String srcName = sourceName(archive);
        SourceSet src = srcs.findByName(srcName);
        SourceMeta meta = null;
        if (src == null) {
            src = srcs.create(srcName);
        } else {
            meta = (SourceMeta) src.getExtensions().findByName(SourceMeta.EXT_NAME);
        }
        if (meta == null) {
            meta = new SourceMeta(this, archive, src);
            src.getExtensions().add(SourceMeta.EXT_NAME, meta);
        }
        return meta;
    }

    @Override
    public String configurationName(ArchiveConfig archive) {
        String n = archive.getName();
        return getName().equals(n) || "main".equals(n) ? getName() : getName() + GUtil.toCamelCase(n);
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
