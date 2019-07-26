package net.wti.gradle.system.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import net.wti.gradle.system.spi.GradleServiceFinder;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository.MetadataSources;
import org.gradle.api.attributes.AttributesSchema;
import org.gradle.api.plugins.*;

import java.io.File;

/**
 * This plugin is responsible for making sure basic schema wiring is setup,
 * and wires in the xapiReport task, which downstream plugins will augment with useful information.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/28/19 @ 3:37 AM.
 */
@SuppressWarnings("UnstableApiUsage")
public class XapiBasePlugin implements Plugin<Project> {

    public static final String XAPI_LOCAL = "xapiLocal";

    public static final String PROP_SCHEMA_APPLIES_JAVA = "xapi.apply.java";
    public static final String PROP_SCHEMA_APPLIES_JAVA_LIBRARY = "xapi.apply.java-library";
    public static final String PROP_SCHEMA_APPLIES_JAVA_PLATFORM = "xapi.apply.java-platform";

    private boolean withJavaPlugin, withJavaLibraryPlugin, withJavaPlatformPlugin;
    private String repo;

    @Override
    public void apply(Project project) {
        final PluginContainer plugins = project.getPlugins();
        plugins.apply(JavaBasePlugin.class);

        configureRepo(project.getRepositories(), project);

        final AttributesSchema attrSchema = project.getDependencies().getAttributesSchema();
        // Hm... should probably merge both of these into a single compositable value?
        // This could be necessary when disambiguation comes into play...
        attrSchema.attribute(XapiSchemaPlugin.ATTR_PLATFORM_TYPE, strat->{
        });
        attrSchema.attribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE, strat->{
        });

        final ProjectView self = ProjectView.fromProject(project);
        final ProjectView root = XapiSchemaPlugin.schemaRootProject(self);

        if (self == root) {
            // When we are the schema root project,
            // we may want to detect and interact with the java-platform component.
            // Specifically, we want to make it simple to have the schema read and expose
            // the java-platform's dependency information to all xapiRequire instances.
            // Preferably, we also generate code to give typesafe "suggestions from the platform".
            // Making this a read-write relationship, it should be possible to declare the same
            // information in dependencies {} as xapiRequire {}, and both generate the same bom / behavior / artifacts.
            if ("true".equals(project.findProperty(XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA_PLATFORM))) {
                withJavaPlatformPlugin = true;
                self.whenReady(this::applyPlatformPlugin);
            } else {
                plugins.withType(JavaPlatformPlugin.class).configureEach(addedLater -> {
                    throw new IllegalStateException("You must apply the java-platform plugin before xapi-base, or set gradle property -P" + XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA_PLATFORM +"=true");
                });
                if (project.file("src").exists()) {
                    plugins.apply(JavaPlugin.class);
                    withJavaPlugin = true;
                }
            }
        } else {
            // If we're going to interact w/ java and java-library, they need to go first,
            // since they don't always maybeCreate() something we want to interop w/.
            if ("true".equals(project.findProperty(XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA_PLATFORM))) {
                plugins.apply(JavaPlatformPlugin.class);
                withJavaPlatformPlugin = true;
            } else if ("true".equals(project.findProperty(XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA_LIBRARY))) {
                plugins.apply(JavaLibraryPlugin.class);
                removeJavaComponent(project);
                withJavaLibraryPlugin = true;
            } else if ("true".equals(project.findProperty(XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA))) {
                plugins.apply(JavaPlugin.class);
                removeJavaComponent(project);
                withJavaPlugin = true;
            } else {
                // user did not explicitly request either java of java-library integration.  Setup boobytraps:
                boolean[] locked = {false};
                boolean has = !plugins.withType(JavaPlugin.class, p->{
                    if (locked[0]) {
                        throw new IllegalStateException("You must apply the java plugin before xapi-base, or set gradle property -P" + XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA +"=true");
                    }
                    // Need to remove the java plugin's component, so it doesn't stomp our own,
                    // which provides the same functionality and more.
                    removeJavaComponent(project);
                    withJavaPlugin = true;
                }).isEmpty();
                has |= !plugins.withType(JavaLibraryPlugin.class, p->{
                    if (locked[0]) {
                        throw new IllegalStateException("You must apply the java-library plugin before xapi-base, or set gradle property -P" + XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA_LIBRARY +"=true");
                    }
                    withJavaLibraryPlugin = true;
                    // also hookup `api` to transitive...
                }).isEmpty();
                has |= !plugins.withType(JavaPlatformPlugin.class, p->{
                    if (locked[0]) {
                        throw new IllegalStateException("You must apply the java-platform plugin before xapi-base, or set gradle property -P" + XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA_PLATFORM +"=true");
                    }
                    withJavaPlatformPlugin = true;
                }).isEmpty();
                if (!has) {
                    // User has not added any java plugin yet.
                    // In the future, we'll likely replace the guts of JavaPlugin here.
                    // For now, we'll just apply it ourselves.
//                    plugins.apply(JavaPlugin.class);
                    plugins.apply(XapiJavaPlugin.class);
                }
                locked[0] = true;
            }
            // TODO: add disambiguation rules to avoid api <-> runtime confusion
            //  for now, we'll just hide configurations which get in the way (dirty!)
            self.getBuildGraph().whenReady(ReadyState.RUN_FINALLY+1, ready-> {
                if (withJavaPlugin) {
                    self.getConfigurations().getByName(JavaPlugin.API_ELEMENTS_CONFIGURATION_NAME).setCanBeConsumed(false);
                    self.getConfigurations().getByName(JavaPlugin.RUNTIME_ELEMENTS_CONFIGURATION_NAME).setCanBeConsumed(false);
                }
            });
        }

    }

    private void removeJavaComponent(Project project) {
        if (!"false".equals(project.findProperty("skip.java.plugin"))) {
            project.getComponents().removeIf(comp -> "java".equals(comp.getName()));
        }
    }

    public void configureRepo(RepositoryHandler repos, Project project) {
        final boolean addRepo = repos.stream().noneMatch(r->XAPI_LOCAL.equals(r.getName()));
        if (!addRepo) {
            project.getLogger().warn("xapiLocal already found in " + repos);
            return;
        }

        if (repo == null) {
            repo = (String) project.findProperty("xapi.mvn.repo");
        }
        if (repo == null) {
            String xapiHome = (String) project.findProperty("xapi.home");
            if (xapiHome == null) {
                xapiHome = GradleServiceFinder.getService(project).findXapiHome();
            }
            if (xapiHome == null) {
                project.getLogger().quiet("No xapi.home found; setting xapiLocal repo to file:{}/repo", project.getRootDir());
                xapiHome = project.getRootDir().getAbsolutePath();
            } else {
                project.getLogger().trace("Using repo from xapi.home {}/repo", xapiHome);
            }
            repo = new File(xapiHome , "repo").toString();
        }

        repos.maven(mvn -> {
            mvn.setUrl(repo);
            mvn.setName("xapiLocal");
            if ("true".equals(System.getProperty("no.metadata")) || "true".equals(project.findProperty("no.metadata"))) {
                project.getLogger().trace("Adding xapiLocal {} w/ maven|gradle metadata support in {}", repo, project.getPath());
                mvn.metadataSources(MetadataSources::mavenPom);
            } else {
                project.getLogger().trace("Adding xapiLocal {} w/ only gradle metadata in {}", repo, project.getPath());
                mvn.metadataSources(MetadataSources::gradleMetadata);
            }
        });

    }

    private void applyPlatformPlugin(ProjectView view) {
        view.getPlugins().apply(JavaPlatformPlugin.class);

        JavaPlatformExtension ext = (JavaPlatformExtension) view.getExtensions().getByName("javaPlatform");
        // TODO: expose and honor the allowedDependencies() user-configuration, IF they applied the platform plugin.
        //  otherwise, make the java-platform plugin honor our configuration of "schema project allows dependencies".
    }
}
