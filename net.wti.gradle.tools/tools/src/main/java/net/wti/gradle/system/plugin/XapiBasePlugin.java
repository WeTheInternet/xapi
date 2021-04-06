package net.wti.gradle.system.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.spi.GradleServiceFinder;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository.MetadataSources;
import org.gradle.api.attributes.AttributesSchema;
import org.gradle.api.plugins.*;
import xapi.gradle.fu.LazyString;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

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
                self.whenReady(ready->applyPlatformPlugin(self));
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

        self.whenReady(ready->validateProperties(self));

        // Whenever we apply any plugins, we should, as soon as possible, parse our schema.xapi / SchemaMap,
        // so other processes can rely on finding / visiting the SchemaMap to construct project graphs.
        // Unfortunately, the parser we need for that is published with these tools,
        // so we're going to do this relatively terrible thing: try to apply a plugin existing in a downstream, optional dependency
        try {
            plugins.apply("xapi-parser");
        } catch (UnknownPluginException e) {
            if (!"true".equals(self.findProperty("xapi.ignore.missing.xapi-parser"))) {
                project.getLogger().warn("Unable to find xapi-parser plugin on classpath");
            }
        }

    }

    private void validateProperties(final ProjectView self) {
        final String skipProp = "xapiSkipPropValidation";
        try {
            if (!"true".equals(self.findProperty(skipProp))) {
                doValidateProperties(self);
            }
        } catch (RuntimeException e) {
            String oldMessage = e.getMessage();
            throw new IllegalStateException("Failed gradle property validation; if you wish to suppress this check (not recommended), set " +
                    skipProp + "=true in file://" + self.getRootProject().getProjectDir() + "/gradle.properties.\nError: " + e.getMessage(), e);
        }
    }
    private void doValidateProperties(final ProjectView self) {
        // we want to make sure our various properties all agree.
        // in order to get a group and a version from
        String projectGroup = self.getGroup();
        String projectVersion = self.getVersion();
        Supplier<Properties>[] getProps = new Supplier[1];
        getProps[0] = () ->{
            Properties props = new Properties();
            File propFile;
            propFile = new File(self.getRootProject().getProjectDir(), "gradle.properties");
            if (propFile.exists()) {
                try {
                    props.load(new FileInputStream(propFile));
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to read propFile " + propFile.getAbsolutePath(), e);
                }
            }
            propFile = new File(self.getProjectDir(), "gradle.properties");
            if (propFile.exists()) {
                try {
                    props.load(new FileInputStream(propFile));
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to read propFile " + propFile.getAbsolutePath(), e);
                }
            }
            getProps[0] = ()->props;
            return props;
        };
        String propGroupId = self.getGradle().getStartParameter().getProjectProperties().get("xapiGroupId");
        if (propGroupId == null) {
            propGroupId = (String)getProps[0].get().get("xapiGroupId");
        }
        String propVersion = self.getGradle().getStartParameter().getProjectProperties().get("xapiVersion");
        if (propVersion == null) {
            propVersion = (String)getProps[0].get().get("xapiVersion");
        }
        Object propProjectGroupId = self.findProperty("xapiGroupId");
        Object propVersionFile = self.findProperty("xapiVersionFile");
        Object propProjectVersion = self.findProperty("xapiVersion");
        if (propVersionFile != null) {
            // yay, there is a version file! Make this version file the source of truth
            File versionFile = self.file(String.valueOf(propVersionFile));
            if (!versionFile.exists()) {
                versionFile = self.getRootProject().file(String.valueOf(propVersionFile));
            }
            if (!versionFile.exists()) {
                throw new IllegalStateException("Version file " + propVersionFile + " cannot be found by " + self.getDebugPath());
            }
            // TODO: actually hook up the xapiVersionFile.  There is a huge mess of specifying xapiVersion and gradle version and schema.xapi version...
            //  replace all of it with a definitive, single-file source of truth: xapiVersionFile
        }
        if (propGroupId == null) {
            // if there's no xapiGroupId, then make the root project name match group.
            if (!projectGroup.equals(self.getRootProject().getName())) {
                throw new IllegalStateException("gradle root project name (" + self.getRootProject().getName() + ") illegally disagrees with gradle-configured group (" + projectGroup + ").\n" +
                        "To allow these values to differ, please add to file://" + self.getRootProject().getProjectDir() + "/gradle.properties xapiGroupId=" + projectGroup);
            }
            if (propProjectGroupId != null) {
                throw new IllegalStateException("Value of xapiGroupId must be set in root gradle.properties: file://" + self.getRootProject().getProjectDir() + "/gradle.properties not in " + self.getPath() + " or on command line");
            }
        } else {
            if (!projectGroup.equals(propGroupId)) {
                throw new IllegalStateException("gradle property xapiGroupId (" + propGroupId + ") illegally disagrees with gradle-configured group (" + projectGroup + ") in file://" + self.getProjectDir());
            }
            if (!propGroupId.equals(propProjectGroupId)) {
                throw new IllegalStateException("root gradle.properties value xapiGroupId (" + propGroupId + ") illegally disagrees with project-specific group (" + propProjectGroupId + ") in file://" + self.getProjectDir());
            }
        }
        if (propVersion == null) {
            if (propProjectVersion != null) {
                throw new IllegalStateException("Value of xapiVersion must be set in root gradle.properties: file://" + self.getRootProject().getProjectDir() + "/gradle.properties not in " + self.getPath() + " or on command line");
            }
        } else {
            if (!projectVersion.equals(propVersion)) {
                throw new IllegalStateException("gradle property xapiVersion (" + propVersion + ") illegally disagrees with gradle-configured version (" + projectVersion + ") in file://" + self.getProjectDir());
            }
            if (!propVersion.equals(propProjectVersion)) {
                throw new IllegalStateException("root gradle.properties value xapiVersion (" + propVersion + ") illegally disagrees with project-specific version (" + propProjectVersion + ") in file://" + self.getProjectDir());
            }
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
