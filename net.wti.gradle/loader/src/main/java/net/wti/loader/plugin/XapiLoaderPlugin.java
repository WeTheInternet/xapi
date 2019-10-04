package net.wti.loader.plugin;

import net.wti.gradle.schema.parser.SchemaMetadata;
import net.wti.gradle.schema.parser.SchemaParser;
import net.wti.gradle.settings.ProjectDescriptorView;
import net.wti.gradle.system.service.GradleService;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;

import java.io.File;

import static net.wti.gradle.settings.ProjectDescriptorView.fromSettings;
import static net.wti.gradle.system.service.GradleService.buildOnce;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 30/07/19 @ 1:33 AM.
 */
public class XapiLoaderPlugin implements Plugin<Settings> {

    @Override
    public void apply(Settings settings) {
        final File schema = new File(settings.getRootDir(), "schema.xapi");
        if (schema.exists()) {
            final ProjectDescriptorView root = fromSettings(settings);
            final SchemaParser parser = ()->root;
            final SchemaMetadata metadata = parser.getSchema();
            // now, visit the schema, also loading any child projects w/ schema.xapi along the way.
        }
    }
}
