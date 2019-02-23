package net.wti.gradle.schema.internal;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.PlatformConfig;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.internal.attributes.ImmutableAttributesFactory;
import org.gradle.util.GUtil;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/28/18 @ 1:45 PM.
 */
public class DefaultArchiveConfig implements ArchiveConfigInternal {

    private final String name;
    private final Set<Object> required;
    private final PlatformConfigInternal platform;
    private boolean sourceAllowed;

    public DefaultArchiveConfig(PlatformConfigInternal platform, String name) {
        this.name = name;
        this.platform = platform;
        required = new LinkedHashSet<>();
        sourceAllowed = true;

    }

    @Override
    public void require(Object ... units) {
        if (units != null && units.length > 0) {
            required.addAll(Arrays.asList(units));
        }
    }

    @Override
    public Set<String> required() {
        final LinkedHashSet<String> result = new LinkedHashSet<>();
        if (required.isEmpty()) {
            // Nothing set?  apply convention.
            switch (name) {
                case "main":
                    result.add("api");
                    result.add("spi");
                    break;
                case "mainSource":
                    result.add("apiSource");
                    result.add("spiSource");
                    break;
                case "stub":
                    result.add("main");
                    break;
                case "stubSource":
                    result.add("mainSource");
                    break;
                case "impl":
                    result.add("main");
                    result.add("stub*");
                    break;
                case "implSource":
                    result.add("mainSource");
                    result.add("stubSource*");
                    break;
            }
        } else {
            for (Object require : required) {
                result.addAll(GradleCoerce.unwrapStrings(require));
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "DefaultArchiveConfig{" +
            "name='" + name + '\'' +
            ", required=" + required +
            ", path=" + getPath() +
            '}';
    }

    protected String getPath() {
        return platform.getName() + ":" + getName();
    }

    @Override
    public boolean isSourceAllowed() {
        return sourceAllowed;
    }

    @Override
    public void setSourceAllowed(boolean sourceAllowed) {
        this.sourceAllowed = sourceAllowed;
    }

    @Override
    public ImmutableAttributes getAttributes(ProjectView view) {
        final ImmutableAttributesFactory factory = view.getAttributesFactory();
        return factory.concat(
            factory.of(XapiSchemaPlugin.ATTR_PLATFORM_TYPE, getPlatform().getName()),
            factory.of(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE, getName())
        );
    }

    public PlatformConfigInternal getPlatform() {
        return platform;
    }

    @Override
    public void fixRequires(PlatformConfig platConfig) {
        required.clear();
        final ArchiveConfig target = platConfig.getRoot().getArchive(getName());
        for (String require : target.required()) {
            required.add(platConfig.getName() + GUtil.toCamelCase(require));
        }

    }
}
