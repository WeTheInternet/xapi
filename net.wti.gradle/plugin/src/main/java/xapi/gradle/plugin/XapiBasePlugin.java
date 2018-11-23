package xapi.gradle.plugin;

import org.gradle.BuildAdapter;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.publish.maven.tasks.GenerateMavenPom;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.gradle.util.GFileUtils;
import xapi.fu.java.X_Jdk;
import xapi.gradle.X_Gradle;
import xapi.gradle.publish.Publish;
import xapi.gradle.task.XapiManifest;
import xapi.gradle.tools.Depend;
import xapi.gradle.tools.Ensure;

import javax.inject.Inject;
import java.io.File;

/**
 * The base xapi plugin ensures all the common infrastructure for other plugins is in place.
 *
 * Adds the XapiExtension, `xapi { ... }` to the project,
 * then waits until evaluation is complete to setup additional build tasks.
 *
 * If you use cross-project configuration (allprojects/subprojects),
 * you should probably not apply the basic xapi plugin until the project is evaluated,
 * to give that project time to install a more specific plugin, which might add an extended xapi { } configuration before us.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/4/18 @ 1:04 AM.
 */
public class XapiBasePlugin implements Plugin<Project> {

    private final Instantiator instantiator;
    private final ObjectFactory objectFactory;

    @Inject
    public XapiBasePlugin(Instantiator instantiator, ObjectFactory objectFactory) {
        this.instantiator = instantiator;
        this.objectFactory = objectFactory;
    }


    @Override
    public void apply(Project project) {

        project.getPlugins().apply(JavaLibraryPlugin.class);
        project.getPlugins().apply(MavenPublishPlugin.class);
        final ExtensionContainer ext = project.getExtensions();
        final XapiExtension config, existing = ext.findByType(XapiExtension.class);

        ext.create("depend", Depend.class, project);
        X_Gradle.init(project);
        if (existing == null) {
            config = X_Gradle.createConfig(project, instantiator);
        } else {
            config = existing;
        }

        // TODO: set xapi.maven.repo system property / env var for all tests,
        // based on whether there is a known checkout of xapi being used.

        Ensure.projectEvaluated(project, p-> {
            project.getLogger().trace("Project {} initializing config {}", p, config);
            config.initialize(project);
            final TaskProvider<XapiManifest> provider = prepareSourceSets(project, config);

            project.getGradle().projectsEvaluated(g->
                provider.configure(man ->
                    man.getInputs().property("paths", man.computeFreshness())
                )
            );
        });

        PublishingExtension publishing = (PublishingExtension) project.getExtensions().getByName(PublishingExtension.NAME);

        Publish.addPublishing(project);
        project.getGradle().projectsEvaluated(gradle -> {
            project.getLogger().trace("Preparing config {}", config);
            config.prepare(project);

            project.getTasks().withType(GenerateMavenPom.class).all(
            pom ->{
                final MavenPublication main = (MavenPublication) publishing.getPublications().findByName("mavenJava");
                if (main != null) {
                    project.getTasks().named(JavaPlugin.JAR_TASK_NAME, t->{
                        Jar jar = (Jar) t;
                        jar.from(pom.getDestination(), spec->{
                            spec.into("META-INF/maven/" + main.getGroupId() + "/" + main.getArtifactId());
                            spec.rename(".*", "pom.xml");
                        });
                        // TODO: consider pom.properties as well?
                        // We won't be relying on maven properties for gradle,
                        // so we'll only use them if it makes sense to pick them
                        // back out later in code like X_Maven.
                    });
                }
                if ("true".equals(project.findProperty("dumpPom"))) {
                    pom.doLast(t->{
                        t.getLogger().quiet("Pom for {}/{} created at {}", project.getGroup(), project.getName(), pom.getDestination().toURI());
                        t.getLogger().quiet(GFileUtils.readFileQuietly(pom.getDestination()));
                    });
                }
            });

        });

        project.getGradle().addBuildListener(new BuildAdapter() {
            @Override
            public void projectsEvaluated(Gradle gradle) {
            // The latest possible callback.
            project.getLogger().trace("Finishing config {}", config);
            config.finish(project);
            }
        });

        project.getPlugins().withType(IdeaPlugin.class, idea->{
            final IdeaModule module = idea.getModel().getModule();
            final File root = module.getContentRoot();

        });
    }

    protected TaskProvider<XapiManifest> prepareSourceSets(Project project, XapiExtension config) {
        TaskProvider<XapiManifest> manifest = project.getTasks().register(XapiManifest.MANIFEST_TASK_NAME, XapiManifest.class,
            man -> {
                man.setOutputDir(config.outputMeta());
                project.getDependencies().add(JavaPlugin.API_CONFIGURATION_NAME, man.getOutputs().getFiles());
            });
        project.getTasks().named(JavaPlugin.PROCESS_RESOURCES_TASK_NAME, task->{
            ProcessResources process = (ProcessResources) task;
            final XapiManifest man = manifest.get();
            process.dependsOn(man);
            process.from(man.getOutputs().getFiles());
            man.finalizedBy(process);
        });
        project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME, task->{
            task.dependsOn(manifest);

            JavaCompile compile = (JavaCompile) task;
            compile.setClasspath(
                compile.getClasspath().plus(manifest.get().getOutputs().getFiles())
            );
        });
        return manifest;
    }
}
