package xapi.gradle.api;

import org.gradle.api.Named;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import xapi.fu.itr.EmptyIterator;
import xapi.gradle.plugin.XapiExtension;

import java.util.Set;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/26/18 @ 6:03 AM.
 */
public class SourceConfig implements Named {

    public static final String EXT_NAME = "xapi";
    private final SourceSet sources;
    private final Configuration configuration;
    private final ArchiveType archiveType;
    private final SetProperty<ArchivePath> inherit;
    private final SetProperty<ArchivePath> provide;
    private TaskProvider<Jar> jar;
    private TaskProvider<JavaCompile> javac;
    private TaskProvider<ProcessResources> resources;
    private PublishArtifact publish;
    private String name;

    // TODO: add a _map_ of test sourcesets

    public SourceConfig(Project project, Configuration con, ArchiveType archiveType, SourceSet sources) {
        this.sources = sources;
        this.configuration = con;
        this.archiveType = archiveType;
        this.name = sources.getName();

        inherit = project.getObjects().setProperty(ArchivePath.class);
        provide = project.getObjects().setProperty(ArchivePath.class);
        inherit.set(EmptyIterator.none());
        provide.set(EmptyIterator.none());

    }

    public void init(
        TaskProvider<Jar> jar,
        TaskProvider<JavaCompile> javaCompile,
        TaskProvider<ProcessResources> processResources,
        PublishArtifact publish
    ) {
        this.jar = jar;
        this.javac = javaCompile;
        this.resources = processResources;
        this.publish = publish;
    }

    public SourceSet getSources() {
        return sources;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public TaskProvider<Jar> getJar() {
        return jar;
    }

    public TaskProvider<JavaCompile> getJavac() {
        return javac;
    }

    public TaskProvider<ProcessResources> getResources() {
        return resources;
    }

    public PublishArtifact getPublish() {
        return publish;
    }

    public SetProperty<ArchivePath> getInherit() {
        return inherit;
    }

    public SetProperty<ArchivePath> getProvide() {
        return provide;
    }

    public Set<ArchivePath> inherit() {
        inherit.finalizeValue();
        return inherit.get();
    }

    public Set<ArchivePath> provide() {
        provide.finalizeValue();
        return provide.get();
    }

    @Override
    public String getName() {
        return name;
    }

    public ArchiveType getArchiveType() {
        return archiveType;
    }
}
