package xapi.gradle.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GFileUtils;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.fu.Lazy;
import xapi.fu.data.SetLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.X_Jdk;
import xapi.gradle.api.ArchiveType;
import xapi.gradle.api.DefaultArchiveTypes;
import xapi.gradle.plugin.XapiExtension;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import static xapi.fu.itr.MappedIterable.mapped;

/**
 * This task is used to generate build/xapi-meta/settings.xapi
 * which is, itself, placed into META-INF/xapi/settings.xapi.
 *
 * In the future, we will also have support for META-INF/xapi/$platform/settings.xapi,
 * which are embedded into the correct jars; the root META-INF/settings.xapi file
 * will represent the "for my intended runtime" collection of settings.
 * So, a gwt jar would have META-INF/xapi/gwt/settings.xapi === META-INF/xapi/settings.xapi,
 * but would also have META-INF/xapi/main/settings.xapi, and .../xapi/api/...
 * so the gwt manifest can simply `include = [main, api]` to melt in inherited settings,
 * and it will behave sanely whether it was loaded from the platform specific location,
 * or the default location.
 *
 * Stub implementations can reference the resources at /META-INF/xapi/settings.xapi,
 * while platform specific implementations will load /META-INF/xapi/$platform/settings.xapi.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/12/18 @ 2:56 AM.
 */
public class XapiManifest extends DefaultTask {

    public static final String MANIFEST_TASK_NAME = "xapiManifest";
    private static final String DEFAULT_OUTPUT_DIR = "xapi-paths";

    private DirectoryProperty outputDir;
    private final Set<ArchiveType> seenTypes;
    private final SetLike<File> allDirs;


    public XapiManifest() {
        final Project p = getProject();
        allDirs = X_Jdk.setLinkedSynchronized();
        outputDir = p.getObjects().directoryProperty();
        outputDir.set(
            p.getLayout().getBuildDirectory().dir(DEFAULT_OUTPUT_DIR)
        );
        setGroup("xapi");
        setDescription("Transfer gradle meta data into xapi manifest files");
        seenTypes = new LinkedHashSet<>();
    }

    @TaskAction
    public void buildManifest() {
        outputDir.finalizeValue();
        // For now, we'll just build a default settings.xapi
        // which lists everything known about the current assembly.
        final File out = outputDir.get().getAsFile();
        final File file = new File(out, "META-INF" + File.separator + "xapi");
        final File dest = writeSettings(file, DefaultArchiveTypes.MAIN);
        getLogger().info("Wrote xapi manifest {}", dest.toURI());
    }

    protected File writeSettings(File out, ArchiveType type) {
        File local = new File(out, type.name() + File.separator + "settings.xapi");
        File main = new File(out, "settings.xapi");
        XapiExtension ext = XapiExtension.from(getProject());
        String settings = ext.exportSettings(this, type);
        boolean isMain = ext.getJars().get().getMainType().equals(type);
        GFileUtils.writeFile(settings, local);
        if (isMain) {
            // Also write to the root settings.xapi file.
            GFileUtils.writeFile(settings, main);
        }
        return local;
    }

    @OutputDirectory
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(Provider<Directory> outputDir) {
        this.outputDir.set(outputDir);
    }

    public void setOutputDir(Directory outputDir) {
        this.outputDir.set(outputDir);
    }

    public void setOutputDir(File outputDir) {
        this.outputDir.set(outputDir);
    }

    public void printArchive(DomBuffer out, XapiManifest manifest, ArchiveType type) {
        seenTypes.add(type);
        final PrintBuffer sources = out.attribute("sources");
        final PrintBuffer resources = out.attribute("resources");
        final PrintBuffer outputs = out.attribute("outputs");
        sources.println("[").indent();
        resources.println("[").indent();
        outputs.println("[").indent();
        final JavaPluginConvention java = manifest.getProject().getConvention().getPlugin(JavaPluginConvention.class);

        final SourceSet source = java.getSourceSets().getByName(type.name().toLowerCase());

        // If you add more data sources here, be sure to update computeFreshness()
        printSources(sources, source.getAllJava().getSrcDirs());
        printSources(resources, source.getResources().getSrcDirs());
        printSources(outputs,
            mapped(source.getOutput().getDirs())
            .plus(source.getOutput().getClassesDirs())
            .plus(source.getOutput().getResourcesDir())
        );
        // If you add more data sources here, be sure to update computeFreshness()

        //        // Now, also print all inherited dependencies! yikes... let's wait until we have magic sourceSets to bother...
        //        for (String item : source.getRuntimeClasspath().getAsPath().split(File.pathSeparator)) {
        //
        //        }

        // TODO: also compute additional links to supported platforms

        sources.outdent().println("]");
        resources.outdent().println("]");
        outputs.outdent().println("]");

    }

    private void printSources(PrintBuffer sources, Iterable<File> src) {
        SizedIterable<File> srcDirs = mapped(src).cached(); // cached iterables are sized.
        final String ws = srcDirs.size() > 2 ? "\n" : " ";
        String prefix = "\"";
        for (File srcDir : srcDirs) {
            try {
                allDirs.add(srcDir);
                sources.printlns(prefix + srcDir.getCanonicalPath() + "\"");
                prefix = ", " + ws + "\"";
            } catch (IOException e) {
                throw new GradleException("Could not resolve " + srcDir, e);
            }
        }

    }

    public XapiExtension getXapi() {
        return (XapiExtension) getProject().getExtensions().getByName(XapiExtension.EXT_NAME);
    }

    public Callable<String> computeFreshness() {
        return ()->allDirs.join(":");
    }
}
