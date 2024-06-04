package net.wti.gradle.settings.api;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.system.tools.GradleCoerce;
import xapi.constants.X_Namespace;
import xapi.string.X_String;

import java.io.File;

import static xapi.string.X_String.isNotEmpty;

/**
 * SchemaProperties:
 * <p>
 * <p> This class exposes the various configurable values that are used when handling xapi schemas.
 * <p>
 * <p> The search order is as follows: If provided, check SimpleProjectView for gradle properties (use root gradle.properties)
 * <p> Next, system properties, then environment variables, then fallback to default values.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 16/03/2021 @ 2:07 a.m..
 */
public interface SchemaProperties extends SchemaPatternResolver {

    static SchemaProperties getInstance() {
        return DefaultSchemaProperties.INSTANCE;
    }

    static String getIndexLocation(MinimalProjectView view, SchemaProperties properties) {
        Object prop = properties.getIndexLocation(view);
        if (prop == null && view != null) {
            prop = view.getRootProject().getProjectDir();
            if (prop != null) {
                prop = new File((File)prop, "build/index");
            }
        }
        assert prop != null || view == null: view + " has root project with null projectDir";
        return prop == null ? new File("./build/index").getAbsolutePath() : String.valueOf(prop);
    }

    String defaultValue(String key);

    default String getIndexLocation(MinimalProjectView view) {
        final String indexLoc = getProperty(X_Namespace.PROPERTY_INDEX_PATH, view);
        return indexLoc;
    }

    default String getIndexIdProp(MinimalProjectView view) {
        // hm.  we need something a little better than this
        return getBuildName(view) + ".indexed." + view.getBuildName();
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
        String value = MinimalProjectView.searchProperty(key, view);
        if (isNotEmpty(value)) {
            return value;
        }
        return defaultValue(value);
    }

    default String getBuildName(MinimalProjectView root) {
        String name = root.getBuildName();
        if (X_String.isEmpty(name) || ":".equals(name)) {
            return "_";//metadata.getName();
        }
        return name;
    }

    default String getRootSchemaLocation(MinimalProjectView p) {
        return GradleCoerce.unwrapStringOr(p.findProperty("xapiSchema"), p.getProjectDir() + File.separator + "schema.xapi");
    }

    default File getRootSchemaFile(MinimalProjectView p) {
        return new File(getRootSchemaLocation(p));
    }

    default SchemaIndexReader createReader(MinimalProjectView view, CharSequence version) {
        String indexDir = getIndexLocation(view, this);
        return new SchemaIndexReader(indexDir, version, this);
    }

    default void markDone(String indexProp, MinimalProjectView view, String debugInfo) {
        markStatus("true", indexProp, view, debugInfo);
    }
    default void markFailed(String indexProp, MinimalProjectView view, String debugInfo) {
        markStatus("false", indexProp, view, debugInfo);
    }
    default void markStatus(String value, String indexProp, MinimalProjectView view, String debugInfo) {
        if (!"true".equals(System.getProperty(indexProp))) {
            // if we are running in strict mode, lets fail.
            if ("true".equals(getProperty("xapi.strict", view))) {
                throw new IllegalStateException(
                        "More than one " + debugInfo + "running for " + indexProp + " is illegal; please limit operation to once per gradle build.\n" +
                                "Set xapi.strict=false to suppress this failure"
                );
            } else {
                view.getLogger().info("More than one " + debugInfo + " running for " + indexProp + "; consider integrating with index from xapi-loader plugin");
            }
        }
        try {
            System.setProperty(indexProp, value);
        } catch (Exception e) {
            view.getLogger().info("Unable to set indexProp {} to {}:\n{}", indexProp, value, e);
        }
    }
}
final class DefaultSchemaProperties implements SchemaProperties{
    static final DefaultSchemaProperties INSTANCE = new DefaultSchemaProperties();
    private DefaultSchemaProperties(){}

    @Override
    public String defaultValue(final String key) {
        return null;
    }

}
