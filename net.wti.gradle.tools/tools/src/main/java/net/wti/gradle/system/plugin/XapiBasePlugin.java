package net.wti.gradle.system.plugin;

import net.wti.gradle.internal.api.ProjectView;
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
                withJavaLibraryPlugin = true;
            } else if ("true".equals(project.findProperty(XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA))) {
                plugins.apply(JavaPlugin.class);
                withJavaPlugin = true;
            } else {
                // user did not explicitly request either java of java-library integration.  Setup boobytraps:
                boolean[] locked = {false};
                boolean has = !plugins.withType(JavaPlugin.class, p->{
                    if (locked[0]) {
                        throw new IllegalStateException("You must apply the java plugin before xapi-base, or set gradle property -P" + XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA +"=true");
                    }
                    withJavaPlugin = true;
                }).isEmpty();
                has |= !plugins.withType(JavaLibraryPlugin.class, p->{
                    if (locked[0]) {
                        throw new IllegalStateException("You must apply the java-library plugin before xapi-base, or set gradle property -P" + XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA_LIBRARY +"=true");
                    }
                    withJavaLibraryPlugin = true;
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
        }

    }

    public void configureRepo(RepositoryHandler repos, Project view) {
        final boolean addRepo = repos.stream().noneMatch(r->XAPI_LOCAL.equals(r.getName()));
        if (!addRepo) {
            view.getLogger().warn("xapiLocal already found in " + repos);
            return;
        }

        if (repo == null) {
            repo = (String) view.findProperty("xapi.mvn.repo");
        }
        if (repo == null) {
            String xapiHome = (String) view.findProperty("xapi.home");
            if (xapiHome == null) {
                xapiHome = GradleServiceFinder.getService(view).findXapiHome();
            }
            if (xapiHome == null) {
                view.getLogger().quiet("No xapi.home found; setting xapiLocal repo to {}/repo", view.getRootDir());
                xapiHome = view.getRootDir().getAbsolutePath();
            } else {
                view.getLogger().trace("Using repo from xapi.home {}/repo", xapiHome);
            }
            repo = new File(xapiHome , "repo").toURI().toString();
        }

        repos.maven(mvn -> {
            mvn.setUrl(repo);
            mvn.setName("xapiLocal");
            if (!"true".equals(System.getProperty("no.metadata"))) {
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
