package xapi.gradle.task;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.system.service.GradleService;
import net.wti.manifest.ManifestPlugin;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import xapi.fu.data.SetLike;
import xapi.fu.java.X_Jdk;
import xapi.gradle.config.ManifestGenerator;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * This task is used to generate build/xapi-paths/META-INF/xapi/$artifactId.xapi (and paths.xapi)
 * which is, itself, placed into the META-INF/xapi directory of our jars.
 *
 * We also generate #N META-INF/xapi/$platform/$archiveType.xapi files, for each archiveType and platform,
 * which are embedded into the correct jars; the root META-INF/paths.xapi file
 * will represent the "for my intended runtime" collection of settings.
 * So, a gwt jar would have META-INF/xapi/gwt/main.xapi === META-INF/xapi/paths.xapi
 *
 * Stub implementations can reference the resources at /META-INF/xapi/settings.xapi,
 * while platform specific implementations will load /META-INF/xapi/$platform/settings.xapi.
 *
 * <pre>
      This is some prototype psuedocode to guide us, for an imaginary setup w/ main, api, dev/main and dev/api:
{@code <xapi}
module = "xapi-lang"
type = "main"
sources = [
  "/opt/xapi/core/lang/src/main/java"
  ,  "/opt/xapi/core/lang/src/main/gen"
]
resources = [
  "/opt/xapi/core/lang/src/main/resources"
]
outputs = [
  "/opt/xapi/core/lang/build/classes/java/main"
  ,
  "/opt/xapi/core/lang/build/classes/groovy/main"
  ,
  "/opt/xapi/core/lang/build/resources/main"
]
// Any internally-created modules, if any, included in this module (loaded and absorbed)
includes = {
  api : "xapi-lang/api"
}
// Any internally-created modules, if any, that this module supercedes (expunged from other dependencies by our inclusion)
provides = {
  spi : "mod-name/spi" // for example, maybe we had to hack the spi across a release version
}

// List of external module paths.
inherits = [ 'xapi-fu', 'xapi-source/api' ]

compilePath = "/paths/to:all:outputs" // use classes dirs
runtimePath = "/added:to:$compilePath" // optional to use dirs (dev mode) or jars (prod mode)
devPath = "$runtimePath:plus:sources"
// optionals, when archive type is a DistType:
gwtPath = "$devPath:plus:gwt-only:stuff-here"

// List only our own dependencies (and those dependencies' transitive inheritances).
// The full classpath can be built by loading the includes/inherits as needed,
// or from the computed path string, above.  This enables a fallback scenario,
// where some file in the precomputed values is missing,
// and we need to get back the source dependency, and download / check cache for values to use.
// This list should be empty for dist-builds.
compileDependency = {
  "net.fu:external-dep:v1.2.3":"/path/to/file/if/exists.jar",
  "xapi-internal/name":"$root/path/to/manifest/file/if/exists.xapi",
}

runtimeDependency = [
  'net.fu:external-dep-impl:v1.2.3',
  ':xapi-internal-name-impl'
]
// The map of all platforms, if any, for this module.
// This can be ommitted if the value is platforms = { main: 'main' }
platforms = {
  // defines the root platform
  main: "main",
  api: "api",
  // defines any sub-platforms (which must inherit from us, not the other way around)
  dev: {
    main: "xapi-lang/main"
    api: "xapi-lang/api"
  }
  devTest: {
    main: "xapi-lang-test/main"
  }
}
// A list of all archives that we can expect to exist.
// Can be ommitted if `archives = { '$moduleName': 'main' }`
archives = {
    'xapi-lang': 'main',
    'xapi-lang-api': 'api',
    'xapi-lang-dev': 'dev/main',
    'xapi-lang-dev-api': 'dev/api',
  }
{@code />}

 * </pre>
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/12/18 @ 2:56 AM.
 */
public class XapiManifest extends DefaultTask {

    public static final String MANIFEST_TASK_NAME = "xapiManifest";
    public static final String DEFAULT_OUTPUT_DIR = "xapi-paths";
    private final ManifestGenerator builder;

    private final DirectoryProperty outputDir;
    private final Property<String> outputDirName;
    private final Set<ArchiveGraph> seenTypes;
    private final SetLike<File> allDirs;

    public XapiManifest() {
        final Project p = getProject();
        allDirs = X_Jdk.setLinkedSynchronized();

        outputDirName = p.getObjects().property(String.class);
        outputDirName.convention(DEFAULT_OUTPUT_DIR);

        outputDir = p.getObjects().directoryProperty();
        outputDir.set(
            p.getLayout().getBuildDirectory().dir(p.provider(getOutputDirName()::get))
        );

        setGroup("xapi");
        setDescription("Transfer gradle meta data into xapi manifest files");
        seenTypes = new LinkedHashSet<>();
        final ProjectView view = ProjectView.fromProject(getProject());
        builder = new ManifestGenerator(view);
    }

    public static TaskProvider<XapiManifest> fromView(ProjectView view) {
        view.getPlugins().apply(ManifestPlugin.class);
        return view.getTasks().named(XapiManifest.MANIFEST_TASK_NAME, XapiManifest.class);
    }

    @TaskAction
    public void buildManifest() {
        outputDir.finalizeValue();
        final File out = outputDir.get().getAsFile();
        final File file = new File(out, "META-INF" + File.separator + "xapi");

        final String main = writeSettings(file, builder);

        getLogger().info("Wrote xapi manifest {};\nuse trace logging to see manifest contents.", file.toURI());
        getLogger().trace("Manifest contents: {}", main);
    }

    protected String writeSettings(File dir, ManifestGenerator builder) {
        final String main = builder.printMain(dir);
        return main;
    }

    @OutputDirectory
    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    public Property<String> getOutputDirName() {
        return outputDirName;
    }

    public void setOutputDirName(Provider<String> outputDir) {
        this.outputDirName.set(outputDir);
    }

    public void setOutputDirName(CharSequence outputDir) {
        this.outputDirName.set(getProject().provider(outputDir::toString));
    }

    public void setOutputDirName(String outputDir) {
        this.outputDirName.set(outputDir);
    }

    public Callable<String> computeFreshness() {
        return builder::summary;
    }
}
