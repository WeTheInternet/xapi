package net.wti.gradle.parser.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.ArchiveConfigInternal;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.map.SchemaMap;
import net.wti.gradle.schema.map.SchemaModule;
import net.wti.gradle.schema.map.SchemaPlatform;
import net.wti.gradle.schema.map.SchemaProject;
import net.wti.gradle.system.plugin.XapiBasePlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import xapi.fu.Maybe;
import xapi.fu.itr.MappedIterable;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-09 @ 3:13 a.m..
 */
public class XapiParserPlugin implements Plugin<Project> {

    @Override
    public void apply(Project p) {
        if (p != p.getRootProject()) {
            p.getRootProject().getPlugins().apply("xapi-parser");
        }
        p.getPlugins().apply(XapiBasePlugin.class);
        final ProjectView view = ProjectView.fromProject(p.getRootProject());
        SchemaMap map = SchemaMap.fromView(view);
        Maybe<SchemaProject> schemaProject = map.findProject(p.getPath());
        if (schemaProject.isPresent()) {
            p.getLogger().trace("Initializing parser plugin for detected project " + p.getPath());
            initializeProject(p, map, schemaProject.get());
        } else {
            p.getLogger().quiet("No entry found for {} in {} ", p.getPath(),
                map.getRootSchema().isExplicit() ? map.getRootSchema().getSchemaFile() : "virtual schema");
        }
    }

    private void initializeProject(
        Project gradleProject,
        SchemaMap map,
        SchemaProject schemaProject
    ) {
        final ProjectView view = ProjectView.fromProject(gradleProject);
        final MappedIterable<? extends SchemaPlatform> platforms = schemaProject.getAllPlatforms();
        final MappedIterable<? extends SchemaModule> modules = schemaProject.getAllModules();
        final XapiSchema schema = view.getSchema();
        for (SchemaPlatform schemaPlatform : platforms) {
            schema.getPlatforms().maybeCreate(schemaPlatform.getName());
        }
        for (SchemaPlatform schemaPlatform : platforms) {
            final PlatformConfigInternal platform = schema.getPlatforms().maybeCreate(schemaPlatform.getName());
            if (schemaPlatform.getReplace() != null) {
                platform.replace(schemaPlatform.getReplace());
            }
            platform.setPublished(schemaPlatform.isPublished());
            platform.setTest(schemaPlatform.isTest());

            for (SchemaModule schemaModule : modules) {
                final ArchiveConfigInternal archive = platform.getArchives().maybeCreate(schemaModule.getName());
                archive.setPublished(schemaPlatform.isPublished() && schemaModule.isPublished());
                archive.setTest(schemaPlatform.isTest() || schemaModule.isTest());
                for (String require : schemaModule.getRequire()) {
//                    archive.require(isMainPlatform ? require : platform.getName() + GUtil.toCamelCase(require));
                    archive.require(require);
                }
            }

        }


    }
}
