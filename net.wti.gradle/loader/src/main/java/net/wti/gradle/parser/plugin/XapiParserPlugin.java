package net.wti.gradle.parser.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.ArchiveRequest.ArchiveRequestType;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.*;
import net.wti.gradle.schema.internal.ArchiveConfigInternal;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.map.*;
import net.wti.gradle.schema.map.internal.SchemaDependency;
import net.wti.gradle.schema.api.SchemaModule;
import net.wti.gradle.schema.api.SchemaProject;
import net.wti.gradle.system.plugin.XapiBasePlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import xapi.fu.Maybe;
import xapi.fu.data.MultiList;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;

import java.io.File;

import static xapi.fu.itr.SingletonIterator.singleItem;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-09 @ 3:13 a.m..
 */
public class XapiParserPlugin implements Plugin<Project> {

    private boolean strict = true;

    @Override
    public void apply(Project proj) {
        if (proj != proj.getRootProject()) {
            proj.getRootProject().getPlugins().apply("xapi-parser");
        }
        Object strictProp = proj.findProperty("xapi.strict");
        switch (String.valueOf(strictProp)) {
            case "false":
            case "0":
            case "n":
            case "no":
            case "False":
                strict=false;
        }
        proj.getPlugins().apply(XapiBasePlugin.class);
        final ProjectView view = ProjectView.fromProject(proj.getRootProject());
        SchemaMap map = SchemaMap.fromView(view);
        boolean madeChanges = false;

        String gradleProjectVersion = String.valueOf(proj.getVersion());
        if ("unspecified".equals(gradleProjectVersion)) {
            madeChanges = true;
            proj.setVersion(map.getVersion());
        } else if (QualifiedModule.UNKNOWN_VALUE.equals(map.getVersion())) {
            map.setVersion(gradleProjectVersion);
        } else if (!gradleProjectVersion.equals(map.getVersion())) {
            File settingsFile = proj.getGradle().getStartParameter().getSettingsFile();
            if (settingsFile == null) {
                settingsFile = proj.getGradle().getStartParameter().getProjectDir();
                if (settingsFile == null) {
                    settingsFile = proj.getRootDir();
                }
            }
            if (!settingsFile.isFile()) {
                File parent = settingsFile.isDirectory() ? settingsFile : settingsFile.getAbsoluteFile().getParentFile();
                File child = new File(parent, "settings.gradle.kts");
                if (!child.isFile()) {
                    child = new File(parent, "settings.gradle"); // we want the default printed value to be settings.gradle
                }
                settingsFile = child;
            }
            throw new IllegalStateException("Gradle version and schema.xapi disagree about the version of the project"
             + proj.getPath() + "; gradle:'" + gradleProjectVersion + "', xapi:'" + map.getVersion() + "'\n" +
            "Please make the following files agree about versions:\n" +
            "file://" + settingsFile.getAbsolutePath() + "\n(rootProject.name = \"CorrectValue\")\n" +
            "file://" + map.getRootSchema().getSchemaFile() + "\n(<xapi-schema version = \"CorrectValue\")"
            ); // end new IllegalStateException
        }

        String gradleProjectGroup = String.valueOf(proj.getGroup());
        if (gradleProjectGroup.isEmpty()) {
            madeChanges = true;
            proj.setGroup(map.getGroup());
        } else if (QualifiedModule.UNKNOWN_VALUE.equals(map.getGroup()) || ".".equals(map.getGroup())) {
            map.setGroup(gradleProjectGroup);
        } else if (!gradleProjectGroup.equals(map.getGroup())) {
            throw new IllegalStateException("Gradle group and schema.xapi disagree about the group of the project "
             + proj.getPath() + ";\ngradle:'" + gradleProjectGroup + "', xapi:'" + map.getGroup() + "'\n" +
            "see file://" + proj.getRootDir() + " for details.");
        }

        if (madeChanges) {
            // only bother the user if we did anything.
            proj.getLogger().quiet("Configured '{}:{}:{}' as '{}:{}:{}'.", gradleProjectGroup, proj.getName(), gradleProjectVersion, proj.getGroup(), proj.getName(), proj.getVersion());
        }

        boolean[] foundMe = {false};
        map.getCallbacks().forProject(proj.getPath(), schemaProject -> {
            foundMe[0] = true;
            proj.getLogger().quiet("Initializing parser plugin for detected project " + proj.getPath());
            initializeProject(proj, map, schemaProject);
        });

        proj.afterEvaluate(ready -> {
            map.getCallbacks().flushCallbacks(map);
        });

        proj.getGradle().buildFinished(result -> {
            if (!foundMe[0]) {
                proj.getLogger().quiet("No schema project entry found for {} in {}; known projects: {}",
                    proj.getPath(),
                    map.getRootSchema().isExplicit() ? map.getRootSchema().getSchemaFile() : "virtual schema " + proj.getPath(),
                    map.getAllProjects().map(SchemaProject::getPathGradle).join(", ")
                );
            }
        });
    }

    private void initializeProject(
        Project gradleProject,
        SchemaMap map,
        SchemaProject schemaProject
    ) {
        final ProjectView view = ProjectView.fromProject(gradleProject);
        final MappedIterable<? extends SchemaPlatform> platforms = schemaProject.getAllPlatforms();
        final MappedIterable<? extends SchemaModule> modules = schemaProject.getAllModules();
        final MultiList<PlatformModule, SchemaDependency> dependencies = schemaProject.getDependencies();
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
                for (String include : schemaModule.getInclude()) {
//                    archive.require(isMainPlatform ? require : platform.getName() + GUtil.toCamelCase(require));
                    // we can use above commented line when we remove hideous "fixRequires" hack from xapi production build
                    archive.require(include);
                }
            }

        }

        dependencies.forEachPair((modKey, dep)->{
            SizedIterable<PlatformConfig> plats;
            if (modKey.getPlatform() == null) {
                // null means "for all platforms"
                plats = SizedIterable.of(
                    schema.getPlatforms().size(),
                    schema.getPlatforms()
                );
            } else {
                plats = singleItem(schema.findPlatform(modKey.getPlatform()));
                if (plats.first() == null) {
                    if (strict) {
                        throw new IllegalArgumentException("No platform found named " + modKey.getPlatform() + " in " + schema.getPlatforms() + "\n" +
                            "to prevent this warning, set gradle property -Pxapi.strict=false");
                    }
                    plats = singleItem(schema.getMainPlatform());
                }
            }
            for (PlatformConfig plat : plats) {
                SizedIterable<ArchiveConfig> mods;
                if (modKey.getModule() == null) {
                    // null means "for all modules"
                    mods = SizedIterable.of(
                        plat.getArchives().size(),
                        plat.getArchives()
                    );
                } else {
                    mods = singleItem(plat.findArchive(modKey.getModule()));
                    if (mods.first() == null) {
                        if (strict) {
                            throw new IllegalArgumentException("No module found named " + modKey.getPlatform() + ":" + modKey.getModule() + " in " + schema.getPlatforms() + "\n" +
                                "to prevent this warning, set gradle property -Pxapi.strict=false");
                        }
                        mods = mods.map(nul-> plat.getMainModule());
                    }
                }
                for (ArchiveConfig mod : mods) {
                    // alright!  Add a Dependency for the given platform:module pair.
                    addDependency(view, mod, map, schemaProject, dep);
                }

            }
        });


    }

    private void addDependency(
        ProjectView view,
        ArchiveConfig mod,
        SchemaMap map,
        SchemaProject schemaProject,
        SchemaDependency dep
    ) {
        // dirty... we _probably_ shouldn't be resolving these so eagerly....
        final ArchiveGraph owner = view.getProjectGraph().platform(
            mod.getPlatform().getName()).archive(mod.getName());
        switch(dep.getType()) {
            case unknown:
                // for unknown types, we should probably log a warning, or try for multiple sources...

            case project:
                // project: this dependency is an intra-build project reference.
                String name = dep.getName();
                final Maybe<SchemaProject> result = map.findProject(name);
                if (result.isPresent()) {
                    view.getLogger().info("Adding {} {} {}", result.get(), " to ", mod.getPath());
                    SchemaProject toRequire = result.get();
                    PlatformModule requiredPlatform = dep.getCoords();
                    if (requiredPlatform.getPlatform() == null) {
                        // all platforms
                        if (requiredPlatform.getModule() == null) {
                            // all modules
                            schemaProject.forAllPlatforms(platform-> {
                                // add dependency from all platform/module combinations
                                schemaProject.forAllModules(module-> {
                                    // this may be cutting to the chase too quickly;
                                    // we should instead probably be recording complete dependency metadata
                                    // (and doing so when processing settings, not projects, like we are doing here)
                                    owner.importProject(name, platform.getName(), module.getName(), Transitivity.api, false);

                                });
                            });
                        } else {
                            // just one module, but do all compatible platforms
                            map.getCallbacks().forModule(requiredPlatform.getModule(), requiredMod-> {
                                    schemaProject.forAllPlatforms(platform -> {
                                        // setup a dependency for platform:requiredMod
                                        final SchemaPlatform childPlatform = toRequire.findPlatform(platform.getName());
                                        if (childPlatform == null) {
                                            // TODO: if there are parent platforms, we _may_ want to add them...
                                            //  though, they should be transitive based on intra-module dependencies already.
                                        } else {
                                            // this should actually, most likely be something that just records metadata of the dependency...
                                            // i.e. a complete graph where each node identifies a project:platform:module,
                                            // with all other coordinates that node requires and is required by.
                                            owner.importProject(name, platform.getName(), requiredMod.getName(), Transitivity.api, false);
                                        }
                                    });
                            });
                        }
                    } else {
                        // There is a platform requested
                        if (requiredPlatform.getModule() == null) {
                            // all modules
                            schemaProject.forAllModules(module->{
                                owner.importProject(name, requiredPlatform.getPlatform(), module.getName(), Transitivity.api, false);
                            });
                        } else {
                            // just one module, just one platform
                            owner.importProject(name, requiredPlatform.getPlatform(), requiredPlatform.getModule(), Transitivity.api, false);
                        }

                    }
                } else {
                    view.getLogger().quiet("Could not find project ", name, " in ", map);
                }
                break;
            case internal:
                // internal: this dependency is intra-project, like gwt.main -> main.api.source

            case external:
                // external: this dependency is a "fully:qualified:dependency:string"
                // here is where we'll need to depend on a at-settings-time index of the world to be pre-built...

        }
        mod.request(null, ArchiveRequestType.COMPILE);
    }
}
