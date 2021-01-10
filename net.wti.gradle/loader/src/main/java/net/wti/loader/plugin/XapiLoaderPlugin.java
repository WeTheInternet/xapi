package net.wti.loader.plugin;

import net.wti.gradle.schema.index.SchemaIndexerImpl;
import net.wti.gradle.schema.map.SchemaMap;
import net.wti.gradle.schema.parser.SchemaMetadata;
import net.wti.gradle.schema.parser.SchemaParser;
import net.wti.gradle.schema.spi.SchemaIndex;
import net.wti.gradle.settings.ProjectDescriptorView;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.ProjectDescriptor;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import xapi.fu.Out1;

import javax.inject.Inject;
import java.io.File;

import static net.wti.gradle.settings.ProjectDescriptorView.fromSettings;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 30/07/19 @ 1:33 AM.
 */
public class XapiLoaderPlugin implements Plugin<Settings> {

    private final Logger logger;

    @Inject
    public XapiLoaderPlugin() {
        logger = Logging.getLogger(XapiLoaderPlugin.class);
    }

    @Override
    public void apply(Settings settings) {
        // Read in the root schema.xapi file
        final File schema = new File(settings.getRootDir(), "schema.xapi");
        if (schema.exists()) {
            final ProjectDescriptorView root = fromSettings(settings);
            final SchemaParser parser = ()->root;
            final SchemaMetadata metadata = parser.getSchema();
            if ("".equals(settings.getRootProject().getName())) {
                logger.quiet("Configuring default root project name of file://{} to {}", settings.getRootDir(), metadata.getName());
                settings.getRootProject().setName(metadata.getName());
            }

            // Remove anything disabled by system property / env var
            String explicitPlatform = getPlatform(settings);
            if (explicitPlatform != null) {
                metadata.reducePlatformTo(explicitPlatform);
            }

            // Transverse the full */*/schema.xapi hierarchy
            final SchemaMap map = buildMap(settings, parser, metadata);

            // Write the index...
            SchemaIndexerImpl indexer = new SchemaIndexerImpl();
            // TODO: derive buildName from a configurable Property<String>
            final Out1<SchemaIndex> index = indexer.index(
                root,
                "test",
                root.getProjectDir()
            );

            // Setup callback for each project to add buildable / publishable component with multiple modules / platforms.
            prepareProjects(settings, map);

            root.whenReady(view  -> index.out1());

        }

    }

    protected SchemaMap buildMap(
        Settings settings,
        SchemaParser parser,
        SchemaMetadata metadata
    ) {
        //noinspection UnnecessaryLocalVariable (nice for debugging)
        SchemaMap map = SchemaMap.fromView(parser.getView(), parser, metadata);
        return map;
    }

    private void prepareProjects(Settings settings, SchemaMap map) {
        map.getCallbacks().perProject(project -> {
            logger.info("Processing schema project {}", project);

            if (project != map.getRootProject()) {
                File dir = new File(settings.getSettingsDir(), project.getSubPath());
                String gradlePath = ":" + project.getSubPath().replace('/', ':');
                settings.include(gradlePath);
                if (dir.isDirectory()) {
                    final ProjectDescriptor p = settings.project(gradlePath);
                    if (new File(dir, project.getName() + ".gradle").exists()) {
                        p.setProjectDir(dir);
                        p.setBuildFileName(project.getName() + ".gradle");
                    } else if (new File(dir, project.getName() + ".gradle.kts").exists()) {
                        p.setProjectDir(dir);
                        p.setBuildFileName(project.getName() + ".gradle.kts");
                    } else {
                        // TODO generate a useful "defaults" script that can be applied / used as default buildscript
                    }
                }
            }
        });
    }

    private static String getPlatform(Settings settings) {
        String explicitPlatform = settings.getStartParameter().getProjectProperties().get("xapi.platform");
        if (explicitPlatform == null) {
            explicitPlatform = settings.getStartParameter().getSystemPropertiesArgs().get("xapi.platform");
        }
        if (explicitPlatform == null) {
            explicitPlatform = System.getProperty("xapi.platform");
        }
        if (explicitPlatform == null) {
            explicitPlatform = System.getenv("XAPI_PLATFORM");
        }
        return explicitPlatform;
    }
}
