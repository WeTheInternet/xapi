package net.wti.gradle.schema.api;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.schema.internal.*;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import net.wti.gradle.schema.tasks.XapiReport;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ConfigurationPublications;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.jvm.tasks.Jar;
import org.gradle.util.ConfigureUtil;
import org.gradle.util.GUtil;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 *
 * The dsl below is meant to be a roadmap of sorts, to elicit the most concise syntax to accurately represent a xapi schema.
 *
 * Don't expect the code below to actually work; we'll build the real thing next...
 *
 * xapiSchema {
 *     archives {
 *         sourceJar() // A built-in method that adds sources for all artifacts
 *         javadocJar() // Add javadoc as well
 *         withClassifier = false // deploys jars as a classifier of the source artifact
 *         withCoordinate = true // deploys jars as their own artifactId / component, with their own transitive dependency graphs.
 *         api {
 *             // requires should probably go in a xapiRequire {} block instead...
 *             // requireProject(':collect')
 *             // requireRemote('com.foo:some-jar')
 *         }
 *         spi {
 *             // requireProject(':inject')
 *         }
 *         main {
 *             require [api, spi]
 *         }
 *         stub {
 *             require [main]
 *             // TODO: add injection transformation here?
 *         }
 *         test {
 *             require [main]
 *         }
 *         testStaging {
 *             require [stub]
 *         }
 *     }
 *
 *     platforms {
 *         j2js {
 *              replace main
 *              requireSource true
 *         }
 *         gwt { replace j2js }
 *         gwtDev {
 *             replace(gwt).when { findProperty('gwtDev') == 'true' }
 *         }
 *         j2cl {
 *              archives {
 *                  jszip {
 *                      extension 'jszip.zip'
 *                  }
 *                  externs {
 *                      extension 'externs.zip'
 *                  }
 *              }
 *              replace j2js
 *              transform {
 *                  input {
 *                      gwtIncompat {
 *                          // input transformations are applied to all required dependencies,
 *                          // in order to create a "staging artifact set".
 *                          sourceIn = true
 *                          classesIn = true
 *                          sourcesOut = true
 *                          classesOut = false
 *                          main 'com.google.j2cl.tools.gwtincompatible.Stripper'
 *                          require 'com.google.j2cl:gwt-incompatible-stripper'
 *                      }
 *                  }
 *                  output {
 *                      j2clCompile {
 *                          sourceIn = true
 *                          classesIn = true
 *                          classesOut = true
 *                          sourcesOut = false
 *                          task(JavaCompile) {
 *                              // configure any extra stuff here
 *                          }
 *                      }
 *
 *                      j2clTranspile {
 *                          input j2clCompile
 *                          input externs
 *                          task(J2clTranspile)
 *                          extension 'jszip.zip'
 *                      }
 *                  }
 *              }
 *         }
 *
 *     }
 *
 *     // The platforms {} code above effectively stamps out the schema into prefix-named usage contexts:
 *     // gwtApi {
 *     //     require api
 *     // }
 *     // gwtSpi {
 *     //     require spi
 *     // }
 *     // gwt {
 *     //     replace main
 *     //     require [gwtApi, gwtSpi]
 *     // }
 *
 * }
 *
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/26/18 @ 2:52 AM.
 */
@SuppressWarnings("UnstableApiUsage")
public class XapiSchema {

    public static final String EXT_NAME = "xapiSchema";
    private final PlatformConfigContainerInternal platforms;
    private final ArchiveConfigContainerInternal archives;

    public XapiSchema(ProjectView pv) {
        final Instantiator instantiator = pv.getInstantiator();
        // There's a nasty diamond problem around the main platform, and the schema containers
        // for platform and archive types.  We work around it by sending deferred "provide main platform" logic.
        Callable<PlatformConfigInternal> getMain = this::getMainPlatform;
        archives = instantiator.newInstance(DefaultArchiveConfigContainer.class, getMain, instantiator);
        platforms = instantiator.newInstance(DefaultPlatformConfigContainer.class, instantiator, archives);
        final ProjectView root = XapiSchemaPlugin.schemaRootProject(pv);
        if (root != pv) {
            initialize(root.getSchema());
        }
        pv.getGradle().projectsEvaluated(gradle->{

            platforms.configureEach(plat -> {

                plat.getArchives().addAll(archives);
                plat.getArchives().configureEach(arch -> {
                    final ArchiveGraph archive = pv.getProjectGraph()
                        .platform(plat.getName())
                        .archive(arch.getName());

                    configureArchive(pv, plat, arch, archive);

                });
            });
        });
        pv.getTasks().register(XapiReport.TASK_NAME, XapiReport.class, report ->
            report.record(pv.getSchema())
        );
    }

    public void configureArchive(
        ProjectView pv,
        PlatformConfig platConfig,
        ArchiveConfig archConfig,
        ArchiveGraph archGraph
    ) {

        final Configuration
            transitive = archGraph.configTransitive()
//            intransitive = archGraph.configIntransitive(),
//            impl = archGraph.configImpl(),
//            compile = archGraph.configCompile(),
//            runtime = archGraph.configRuntime(),
//            assembled = archGraph.configAssembled()
        ;

        final PlatformGraph platGraph = archGraph.platform();

        final DependencyHandler deps = pv.getDependencies();

        if (!platConfig.isRoot()) {
            final PlatformGraph parent = platGraph.project().platform(platConfig.getParent().getName());
            platGraph.setParent(parent);
            final ArchiveGraph parentArch = parent.archive(archGraph.getName());
            deps.add(
                transitive.getName(),
                parentArch.configRuntime()
            );
        }

        if (archGraph.srcExists()) {
            SourceMeta meta = archGraph.getSource();

            deps.add(
                archGraph.configAssembled().getName(),
                // The assembled configuration is where we'll grab all sourceset outputs.
                meta.getSrc().getOutput()
            );


        }
        if (platConfig.isRequireSource() || archConfig.isSourceAllowed()) {
            // Instead of isSourceAllowed, we may want to allow platform source
            // requirements to trigger source creation of super-platforms.
            final Configuration sourceConfig = addSourceConfiguration(pv, archGraph);
            transitive.extendsFrom(sourceConfig);
        }

        final Set<String> needed = archConfig.required();
        // We should actually defer this until later...
        for (String need : needed) {
            boolean only = need.endsWith("*");
            if (only) {
                need = need.substring(0, need.length()-1);
            }
            PlatformConfigInternal conf = pv.getSchema().findPlatform(need);
            if (conf == null) {
                conf = pv.getSchema().getMainPlatform();
            }
            need = GUtil.toLowerCamelCase(need.replace(conf.getName(), ""));
            final PlatformGraph needPlat = platGraph.project().platform(conf.getName());
            final ArchiveGraph neededArchive = needPlat.archive(need);

            Dependency dep = pv.dependencyFor(neededArchive.configAssembled());

            deps.add(
                only ? archGraph.configIntransitive().getName(): transitive.getName()
                , dep);
        }

    }

    private Configuration addSourceConfiguration(
        ProjectView project,
        ArchiveGraph archive
    ) {
        XapiSchema schema = project.getSchema();
        String name = archive.getSrcName() + "Source";
        ConfigurationContainer configs = project.getConfigurations();
        Configuration config = configs.findByName(name);
        if (config != null) {
            return config;
        }

        String taskName = name + "Jar";
        final TaskContainer tasks = project.getTasks();
        config = configs.maybeCreate(name);

        boolean hasSrc = archive.srcExists();

        final TaskProvider<Jar> jarTask = tasks.register(taskName, Jar.class, jar -> {
            if (hasSrc) {
                SourceMeta meta = archive.getSource();
                jar.from(meta.getSrc().getAllSource());
                jar.getExtensions().add(SourceMeta.EXT_NAME, meta);
            }
            if (schema.getArchives().isWithCoordinate()) {
                assert !schema.getArchives().isWithClassifier() : "Archive container cannot use both coordinate and classifier: " + schema.getArchives();
                jar.getArchiveAppendix().set("sources");
            } else {
                jar.getArchiveClassifier().set("sources");
            }
        });

        final ConfigurationPublications outgoing = config.getOutgoing();

//        outgoing.variants(variants->{
//            final ConfigurationVariant variant = variants.maybeCreate("sources");
//            variant.attributes(attrs->
//                attrs.attribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE, "sources")
//            );
////        if (hasSrc) {
////            for (File srcDir : archive.allSourceDirs().getSourceDirectories()) {
////                final Provider<File> provider = providers.provider(() -> srcDir);
////                LazyPublishArtifact src = new LazyPublishArtifact(provider);
////                final Directory proj = project.getLayout().getProjectDirectory();
////                final String seg = srcDir.getAbsolutePath().replace(proj.getAsFile().getAbsolutePath(), "").substring(1);
////                final Directory asDir = proj.dir(seg);
////                outgoing.artifact(new FileSystemPublishArtifact(
////                    asDir, project.getVersion()
////                ));
//////                outgoing.artifact(src);
////            }
////        }
//        });

        archive.configAssembled().extendsFrom(config);

        if (archive.srcExists()) {
            project.getDependencies().add(
                name,
                archive.allSourceDirs().getSourceDirectories()
            );
        }

        return config;
    }

    public PlatformConfigContainerInternal getPlatforms() {
        return platforms;
    }

    public ArchiveConfigContainerInternal getArchives() {
        return archives;
    }

    public void archives(@DelegatesTo(ArchiveConfigContainer.class) Closure configure) {
        archives(ConfigureUtil.configureUsing(configure));
    }

    public void archives(Action<? super ArchiveConfigContainer> configure) {
        configure.execute(archives);
    }

    public void platforms(@DelegatesTo(PlatformConfigContainer.class) Closure configure) {
        platforms(ConfigureUtil.configureUsing(configure));
    }

    public void platforms(Action<? super PlatformConfigContainer> configure) {
        configure.execute(platforms);
    }

    public PlatformConfigInternal findPlatform(String need) {
        PlatformConfig plat = platforms.findByName(need);
        while (plat == null) {
            int check = need.length();
            while (check > 0 && Character.isLowerCase(need.charAt(--check))) {

            }
            if (check == 0) {
                return null;
            }
            need = need.substring(0, --check);
            plat = platforms.findByName(need);
        }
        return (PlatformConfigInternal) plat;
    }

    public void initialize(XapiSchema rootSchema) {
        rootSchema.platforms.configureEach(platforms::add);
        rootSchema.archives.configureEach(archives::add);
        archives.setWithClassifier(rootSchema.archives.isWithClassifier());
        archives.setWithCoordinate(rootSchema.archives.isWithCoordinate());
        archives.setWithSourceJar(rootSchema.archives.isWithSourceJar());

    }

    public PlatformConfigInternal getMainPlatform() {
        return platforms.maybeCreate("main");
    }
}
