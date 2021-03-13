package net.wti.gradle.classpath.tasks;

import net.wti.gradle.internal.api.ReadyState;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionSelector;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * XapiClasspathTask:
 * <p>
 * pass in g:n:v / project Dependencies, get back task output files of the resolved classpath.
 * <p>
 * <p> This task allows you to say "I want to resolve X, Y, Z dependencies before I doSomethingElse(tm)".
 * <p>
 * <p> This lets you declare your intention to resolve a set of dependencies, without actually blocking to resolve them.
 * <p>
 * <p> By default, the xapi framework will create and use classpath tasks to control java / etc compilations and runtimes.
 * <p>
 * <p> This allows you to do interesting things with the classpath as soon as it is ready,
 * <p> without waiting for compileJava or whatever else to finish.
 * <p>
 * <p> This also grants you finer access to cache invalidation ammunition:
 * <p> If you have some job you want to run whenever your dependencies change,
 * <p> but doesn't care at all about compiled classes changing,
 * <p> you can instead use the underlying XapiClasspathTask as task.inputs.
 * <p>
 * <p> Bonus points: if the tasks consuming the classpath is in a different gradle project,
 * <p> you may achieve greater runtime parallelization of task execution (more projects == more locks to shard work)
 * <p>
 * Created by james@wetheinter.net on Tue. Mar 09, 2021 @ 2:06 a.m.
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class XapiClasspathTask extends AbstractXapiClasspath {

    private final ConfigurableFileCollection classpath;
    // hmmmmm....  perhaps we should create a virtual output directory containing links to assembled classpath.

    public XapiClasspathTask() {

        classpath = getObjects().fileCollection();
        // add the detached configuration we'll use to resolve dependencies to our output classpath FileCollection
        getView().getProjectGraph().whenReady(ReadyState.BEFORE_FINISHED + 0x40, init -> {
            // using a callback to give user scripts time to configure us...
            // even if the readystate is well-beyond CREATED, this will defer changes in constructor until after we're called.
            // This gives you time to set things up before we try resolving settings on you
            Property<Configuration> resolver = getDetachedResolver();
            resolver.finalizeValue();
            classpath.from(resolver.get());
            getOutputs().files(classpath.filter(File::isFile));
            getOutputs().dirs(classpath.filter(File::isDirectory));
        });

        // force this task to always resolve
        getResolve().set(true);
        getResolve().finalizeValue();
        // strict is default to true, but not forced to true
        getStrict().set(true);
    }

    // note: no @Classpath annotation on our _output_ classpath, you'd use @Classpath on an _input_ field consuming this task(.outputs.files)
    // (the classpath task inputs are handled by supertype)
    @Internal
    public ConfigurableFileCollection getClasspath() {
        return classpath;
    }

    @Override
    protected void consumeClasspath(final Map<File, ResolvedArtifact> allResolved, final List<ModuleVersionSelector> allFailed) {
        // nothing to do here, really.  This task is meant to be referenced by other tasks which consume classpaths.
        // the only real meat is the callback registered in class constructor
    }
}
