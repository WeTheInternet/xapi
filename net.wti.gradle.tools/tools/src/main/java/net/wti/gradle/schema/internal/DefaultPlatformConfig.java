package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.ArchiveConfigContainer;
import net.wti.gradle.schema.api.PlatformConfig;
import net.wti.gradle.schema.api.PlatformConfigContainer;
import net.wti.gradle.system.tools.GradleCoerce;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static net.wti.gradle.system.tools.GradleCoerce.unwrapBoolean;

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
    private Object requireSource;
    private Object published;
    private Object test;
    private Object mainModule;

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
                view.getProjectGraph().whenReady(ReadyState.BEFORE_CREATED + 0x10, p-> {
                    archives.maybeCreate(item.getName()).baseOn(item, false);
                });
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
    public ArchiveConfigInternal getMainModule() {
        return archives.maybeCreate(getMainModuleName());
    }

    @Override
    public void setMainModule(Object mainModule) {
        this.mainModule = mainModule;
    }

    @Override
    public String getMainModuleName() {
        String s = GradleCoerce.unwrapStringNonNull(mainModule);
        return s.isEmpty() ? PlatformConfigInternal.super.getMainModuleName() : s;
    }

    @Override
    public boolean isRequireSource() {
        return unwrapBoolean(requireSource);
    }

    @Override
    public ArchiveConfigContainerInternal getArchives() {
        return archives;
    }

    @Override
    public void setRequireSource(Object requires) {
        this.requireSource = requires;
    }

    @Override
    public void setPublished(Object published) {
        this.published = published;
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
        // TODO Gradle Property API instead of this mess....
        if (rooted instanceof DefaultPlatformConfig) {
            final DefaultPlatformConfig r = (DefaultPlatformConfig) rooted;
            if (r.requireSource != null) {
                this.requireSource = r.requireSource;
            }
            if (r.test != null) {
                this.test = r.test;
            }
            if (r.published != null) {
                this.published = r.published;
            }
        } else {
            this.requireSource = rooted.isRequireSource();
            this.test = rooted.isTest();
            this.published = rooted.isPublished();
        }
    }

    @Override
    public boolean isTest() {
        return test == null ? "test".equals(name) : unwrapBoolean(test);
    }

    @Override
    public boolean isPublished() {
        // TODO: check if this platform is disabled for current build, and return false if true.
        if (published == null) {
            // if published is not set, the heuristic is to only publish "root",
            //  or anything that root replaces (for cases when you manually set root to a derived platform)
            final PlatformConfigInternal root = getRoot();
            if (!PlatformModule.defaultPlatform.toString().equals(root.getName())) {
                return false;
            }
            return root.isOrReplaces(this);
        }
        return unwrapBoolean(published);
    }

    @Override
    public boolean isOrReplaces(PlatformConfigInternal argPlat) {
        String argName = argPlat.getName();
        if (argName.equals(getName())) {
            return true;
        } else {
            // when platforms don't match, we need to quit,
            // unless our platform replaces the argument platform
            PlatformConfigInternal candidate = getParent();
            while (candidate != null) {
                if (argName.equals(candidate.getName())) {
                    // winner winner.  Our platform "replaces" the argument platform, we may continue to check module inheritance
                    // TODO: discern between "replaces" and "extends"(does not exist yet).
                    //  A true replacement should FAIL this test, while and extends should BREAK, as we do below.
                    // For now, we only do "extends", using `.replaces = ...`;
                    // but extends is a bad keyword to use, so we should instead come up with a new name
                    // when we bother to wire up the differentiation.
                    return true;
                }
                candidate = candidate.getParent();
            }
        }
        // out platform is neither the argument platform, nor does it replace the argument platform.  Check failed.
        return false;
    }

    @Override
    public void setTest(Object test) {
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
