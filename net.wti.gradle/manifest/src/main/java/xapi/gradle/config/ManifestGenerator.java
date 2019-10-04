package xapi.gradle.config;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.schema.api.XapiSchema;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.util.GFileUtils;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Out2;
import xapi.fu.data.ListLike;
import xapi.fu.data.MapLike;
import xapi.fu.data.SetLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.X_Jdk;
import xapi.gradle.paths.AllPaths;

import java.io.File;

import static xapi.fu.itr.ArrayIterable.iterate;
import static xapi.fu.java.X_Jdk.list;
import static xapi.source.X_Source.javaQuote;

/**
 * A container for metadata about a given project's build.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/26/18 @ 4:54 AM.
 */
public class ManifestGenerator {

    private final XapiSchema schema;
    private final Lazy<ArchiveGraph> main;
    private final MapLike<ArchiveGraph, AllPaths> paths;
    private final AllPaths mainPaths, otherPaths;
    private final ProjectView view;

    public ManifestGenerator(ProjectView view) {
        this.view = view;
        this.schema = view.getSchema();
        mainPaths = new AllPaths();
        otherPaths = new AllPaths();
        paths = X_Jdk.mapOrderedInsertion();
        main = Lazy.deferred1(()->
            view.getProjectGraph().mainModule()
        );

        view.projectGraph().configure(graph ->
            // only "print" any realized platforms.
            graph.realizedPlatforms(this::addPlatform)
        );
    }

    public void addPlatform(PlatformGraph config) {
        final ArchiveGraph type = config.getMainModule();
        AllPaths ap = paths.computeIfAbsent(type, AllPaths::new);
        (isInherited(type) ? mainPaths : otherPaths)
            .absorb(ap);
        addSources(ap, type, this::isGenDir);
        config.archives().configureEach(this::addSources);
    }

    private void addSources(AllPaths ap, ArchiveGraph config, In1Out1<File, Boolean> genDirCheck) {
        if (!config.srcExists()) {
            return;
        }
        final SourceSet src = config.getSource().getSrc();
        if (!ap.isMutateOnAbsorb()) {
            // We may undo this if we see fit, but it's appropriate for now.
            throw new IllegalStateException("Only a child AllPaths may add sources; call parent.absorb(child) on " + this);
        }
        ap.mutate();
        for (File srcDir : src.getJava().getSrcDirs()) {
            ap._sources().add(srcDir);
            if (genDirCheck.io(srcDir)) {
                ap._generated().add(srcDir);
            }
        }
        for (File srcDir : src.getResources().getSrcDirs()) {
            ap._resources().add(srcDir);
            if (genDirCheck.io(srcDir)) {
                ap._generated().add(srcDir);
            }
        }
        final SourceSetOutput outs = src.getOutput();
        for (File dir : MappedIterable.mapped(outs.getDirs())
            .plus(outs.getClassesDirs())
            .plus(outs.getResourcesDir())
        ) {
            ap._outputs().add(dir);
            if (genDirCheck.io(dir)) {
                ap._generated().add(dir);
            }
        }
    }

    public void addSources(ArchiveGraph config) {
        AllPaths ap = paths.computeIfAbsent(config, AllPaths::new);
        (isInherited(config) ? mainPaths : otherPaths)
            .absorb(ap);
        addSources(ap, config, this::isGenDir);
    }

    public boolean isInherited(ArchiveGraph type) {
        return main.out1().config().isOrRequires(type.config());
    }

    public boolean isGenDir(File dir) {
        // good enough for now...
        final String path = dir.getAbsolutePath();
        if (path.contains(File.separator + "gen")) {
            String[] bySrc = path.split(File.separator + "src" + File.separator);
            if (bySrc.length == 2) {
                return bySrc[1].contains("gen");
            }
            String[] byBuild = path.split(File.separator + "build" + File.separator);
            if (byBuild.length == 2) {
                return byBuild[1].contains("gen");
            }
        }
        return false;
    }

    public String printMain(File dir) {

        // If you add more data sources here, be sure to update summary()

        ArchiveGraph mod = view.getProjectGraph().mainModule();
        final String module = mod.getNameCore();
        final File subDir = new File(dir, module);
        subDir.mkdirs();
        final ArchiveGraph type = getArchiveType();

        // Write out each individual archive type, @ META-INF/xapi/module-name/archiveType.xapi
        for (Out2<ArchiveGraph, AllPaths> item : paths.forEachItem()) {
            // Printing these will also ensure that the META-INF/xapi/module-name/main.xapi is _clean_ of inherited dependencies;
            // the parent mainPaths will absorb included children into paths.
            printArchive(subDir, module, item.out1(), item.out2());
        }

        // Now, write out the main archive type's dependencies into the META-INF/xapi/module-name.xapi
        // And a copy in META-INF/xapi/settings.xapi
        final String mainSource = printArchive(new File(dir, module + ".xapi"), module, type, mainPaths);
        return mainSource;
    }

    @Internal
    public ArchiveGraph getArchiveType() {
        return main.out1();
    }

    protected String printArchive(File dir, String module, ArchiveGraph main, AllPaths paths) {

        DomBuffer out = new DomBuffer("xapi", false);
        out.allowAbbreviation(true).setAttributeNewline(true);
        out.indent();

        // If you add more data sources here, be sure to update summary()
        out.setAttribute("module", module);
        out.setAttribute("type", main.platform().getName());

        writeClasspath(out, "sources", paths.getSources());
        writeClasspath(out, "resources", paths.getResources());
        writeClasspath(out, "outputs", paths.getOutputs());
        writeClasspath(out, "generated", paths.getGenerated());

        /* Print includes:
        // Any internally-created modules, if any, included in this module (loaded and absorbed)
        includes = {
          api : "xapi-lang/api"
        }
        */
        SetLike<ArchiveGraph> used = X_Jdk.set();
        for (PlatformGraph realized: main.platform().project().realizedPlatforms()) {
            for (ArchiveGraph arch : realized.realizedArchives()) {
                if (this.paths.has(arch)) {
                    used.add(arch);
                }
            }

        }
        if (used.isNotEmpty()) {
            final String[][] items = used.map(type -> new String[]{type.getSrcName(), type.project().getName() + "/" + type.platform().getName() + "/" + type.getName()})
                .toArray(String[][]::new);
            writeJson(out, "includes", true, items);
        }

        out.outdent();
        String src = out.toSource();
        if (dir.isDirectory()) {
            // If it's a directory, we're writing a submodule.
            dir = new File(dir, main.getSrcName() + ".xapi");
            GFileUtils.writeFile(src, dir);
        } else {
            // It's a main module, write its qualified named file and (for now, until we can make a better tool,
            // a META-INF/xapi/paths.xapi file, which will be present in all archives)
            GFileUtils.writeFile(src, dir);
            GFileUtils.writeFile(src, new File(dir.getParentFile(), "paths.xapi"));

        }
        return src;

        /*TODO: inherits
        // List of external module paths.
        inherits = [ 'xapi-fu', 'xapi-source/api' ]
        */

        /*TODO: provides
        // Any internally-created modules, if any, that this module supercedes (expunged from other dependencies by our inclusion)
        provides = {
          spi : "mod-name/spi" // for example, maybe we had to hack the spi across a release version
        }
        */

        /*TODO: paths
        compilePath = "/paths/to:all:outputs" // use classes dirs
        runtimePath = "/added:to:$compilePath" // optional to use dirs (dev mode) or jars (prod mode)
        devPath = "$runtimePath:plus:sources"
        // optionals, when archive type is a DistType:
        gwtPath = "$devPath:plus:gwt-only:stuff-here"
        */

        /* TODO: formal dependency analysis
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
        */

        /* TODO: platform mapping
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
        */
    }

    protected PrintBuffer writeJson(DomBuffer out, String attrName, boolean quoted, String[] ... sources) {
        final PrintBuffer attr = out.attribute(attrName);
        attr.print("{").indent();
        final PrintBuffer contents = attr.makeChild();
        boolean inline = sources.length < 2;

        final String src = iterate(sources).map(source ->
                                source[0] + ": " + (quoted ? javaQuote(source[1]) : source[1])
                            )
                            .join("," + (inline ? " " : "\n"));

        if (src.contains("\n")) {
            contents.printlns(src);
        } else {
            contents.print(src);
        }

        attr.outdent().println("}");
        return contents;
    }
    protected PrintBuffer writeClasspath(DomBuffer out, String attrName, SetLike<File> sources) {
        // hm.  json arrays are desirable for pre-processing-free iteration,
        // but classpath:notation:is:hard:to:beat...  Perhaps a specialized path element might be nice;
        // though we'd likely want to force : and forget ;
        // for now, we'll write json for individual elements that we'd want to visit,
        // and just append a single "classpath:string:of:everything" elsewhere, for convenience.
        final PrintBuffer attr = out.attribute(attrName);
        final SizedIterable<String> items = sources.filterUnsafe(File::exists).map(File::getAbsolutePath).counted();
        boolean inline = items.size() < 3;
        attr.indent().setIndentNeeded(false).append("[");
        if (!inline) {
            attr.println();
        }
        final PrintBuffer contents = attr.makeChild();
        String joiner = inline ? ", " : ",\n";
        String src = items.join("\"", "\"" + joiner +"\"", "\"");

        if (!"\"\"".equals(src)) {
            if (src.contains("\n")) {
                contents.printlns(src);
            } else {
                contents.append(src);
            }
        }

        attr.outdent().println("]");
        return contents;
    }

    public String summary() {
        ListLike<String> values = list();
        for (Out2<ArchiveGraph, AllPaths> item : paths.forEachItem()) {
            values.add(item.out1().getModuleName());
            values.add(item.out2().summary());
        }
        return values.join("{", "~","}");
    }
}
