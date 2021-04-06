package net.wti.gradle.schema.spi;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.schema.api.SchemaMetadata;
import xapi.fu.data.MapLike;
import xapi.fu.java.X_Jdk;
import xapi.util.X_Namespace;
import xapi.util.X_String;

import static net.wti.gradle.schema.spi.DefaultSchemaProperties.keyMap;
import static xapi.util.X_String.firstNotEmpty;
import static xapi.util.X_String.isNotEmpty;

/**
 * SchemaProperties:
 * <p>
 * <p> This class exposes the various configurable values that are used when handling xapi schemas.
 * <p>
 * <p> The search order is as follows: If provided, check MinimalProjectView for gradle properties (use root gradle.properties)
 * <p> Next, system properties, then environment variables, then fallback to default values.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 16/03/2021 @ 2:07 a.m..
 */
public interface SchemaProperties {

    static SchemaProperties getInstance() {
        return DefaultSchemaProperties.INSTANCE;
    }

    String defaultValue(String key);

    default String getIndexLocation(MinimalProjectView view) {
        return getProperty(X_Namespace.PROPERTY_INDEX_PATH, view);
    }

    default String getPublishGroupPattern(MinimalProjectView view, final String platformName) {
        final String configured = getProperty(X_Namespace.PROPERTY_PUBLISH_GROUP_PATTERN, view);
        if (X_String.isEmptyTrimmed(configured)) {
            return "main".equals(platformName) ? "$group" : "$group.$platform";
        }
        return configured;
    }

    default String getPublishNamePattern(MinimalProjectView view, final String moduleName) {
        final String configured = getProperty(X_Namespace.PROPERTY_PUBLISH_NAME_PATTERN, view);
        if (X_String.isEmptyTrimmed(configured)) {
            return "main".equals(moduleName) ? "$name" : "$name-$module";
        }

        return configured;
    }

    default String getProperty(String key, MinimalProjectView view) {
        if (DefaultSchemaProperties.recursion > 0) {
            throw new IllegalStateException("SchemaProperties " + this + " is recursing into itself");
        }
        String value = searchProperty(key, view);
        if (isNotEmpty(value)) {
            return value;
        }
        return defaultValue(value);
    }

    static String searchProperty(String key, MinimalProjectView view) {
        try {
            if (DefaultSchemaProperties.recursion++ > 1) {
                throw new IllegalStateException("SchemaProperties.searchProperty()  is recursing into itself");
            }
            String maybe = null;
            if (view != null) {
                Object candidate = view.findProperty(key);
                if (candidate != null) {
                    maybe = String.valueOf(candidate);
                }
            }
            if (isNotEmpty(maybe)) {
                return maybe;
            }
            maybe = System.getProperty(key);
            if (isNotEmpty(maybe)) {
                return maybe;
            }
            String envKey = keyMap.computeIfAbsent(key, ()->
                    // handle camelCase, snake-case and dot.case.
                    key.replaceAll("([A-Z])", "_$1").replaceAll("[.-]", "_").toUpperCase()
            );
            maybe = System.getenv(envKey);
            if (isNotEmpty(maybe)) {
                return maybe;
            }
            return null;
        } finally {
            DefaultSchemaProperties.recursion--;
        }
    }

    default String getBuildName(MinimalProjectView root, SchemaMetadata metadata) {
        String name = root.getBuildName();
        if (X_String.isEmpty(name) || ":".equals(name)) {
            return "_";//metadata.getName();
        }
        return name;
    }

    default String resolvePattern(String pattern, SchemaIndex index, String projectName, String platform, String module) {
        return resolvePattern(pattern, index.getBuildName(), projectName, index.getGroupIdNotNull(), index.getVersion().toString(), platform, module);
    }
    default String resolvePattern(String pattern, String buildName, String projectName, String groupId, String version, String platform, String module) {
        return pattern
                .replaceAll("[$]build", firstNotEmpty(buildName, ":"))
                .replaceAll("[$]name", projectName)
                .replaceAll("[$]group", groupId)
                .replaceAll("[$]version", version)
                .replaceAll("[$]platform", platform)
                .replaceAll("[$]module", module)
                ;
    }
}
final class DefaultSchemaProperties implements SchemaProperties{
    static int recursion;
    static final DefaultSchemaProperties INSTANCE = new DefaultSchemaProperties();
    static final MapLike<String, String> keyMap = X_Jdk.mapHashConcurrent();
    private DefaultSchemaProperties(){}

    @Override
    public String defaultValue(final String key) {
        return null;
    }
}