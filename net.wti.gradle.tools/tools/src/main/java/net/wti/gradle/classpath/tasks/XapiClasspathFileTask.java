package net.wti.gradle.classpath.tasks;

import org.gradle.api.artifacts.*;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.util.GFileUtils;

import java.io.File;
import java.util.*;

/**
 * A task to extract a .classpath file from a configured gradle source module.
 *
 * The input coordinates describe a given consumer of a classpath as:
 * :project:path, platform:module
 *
 * If :project:path is not supplied, the default value will be the project where the task is declared.
 * If platform is not specified, the default platform (usually "main") will be used.
 * If module is not specified, the default module (usually "main") will be used.
 *
 * If nothing is supplied, this task will produce the main module classpath of the gradle project where it was defined.
 *
 * Hm... maybe make it possible to select if we want consumer classpath, or producer
 * (consumer == all dependencies needed to create output,
 * producer == all output and all transitive dependencies).
 *
 * For now, we are only doing consumer classpaths; if you want to see output plus transitives,
 * you should have a module w/ a dependency on what you want to resolve...
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class XapiClasspathFileTask extends AbstractXapiClasspath {

    private final RegularFileProperty classpathFile;

    public XapiClasspathFileTask() {
        classpathFile = getObjects().fileProperty();
        classpathFile.convention(getProviders().provider(
                ()-> {
                    String outputFile = "classpaths/" + getName() + ".classpath";
                    return getProject().getLayout().getBuildDirectory().file(outputFile).get();
                }
        ));
    }

    @OutputFile
    public RegularFileProperty getClasspathFile() {
        return classpathFile;
    }

    @Override
    protected void consumeClasspath(final Map<File, ResolvedArtifact> allResolved, final List<ModuleVersionSelector> allFailed) {

        File outputFile = classpathFile.get().getAsFile();
        File outputDir = outputFile.getParentFile();
        if (!outputDir.isDirectory() && !outputDir.mkdirs()) {
            throw new IllegalStateException(outputDir + " does not exist and cannot be created. Check directory permissions and disk usage");
        }

        // alright, we got a classpath.  Let's write it to disk.
        if (outputFile.isFile()) {
            // TODO: capture existing state, to decide if any work was done or not, so we can participate in incremental builds
            outputFile.delete();
        }
        // next, generate the classpath.
        String classpath = getObjects().fileCollection().from(allResolved.keySet()).getAsPath();
        // write to disk and call it a day
        GFileUtils.writeFile(classpath, outputFile, "UTF-8");
    }

}
