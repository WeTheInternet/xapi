package net.wti.gradle.schema.api;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.schema.internal.*;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import net.wti.gradle.schema.tasks.XapiReport;
import net.wti.gradle.system.api.LazyFileCollection;
import net.wti.gradle.system.impl.DefaultLazyFileCollection;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationPublications;
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.util.ConfigureUtil;
import org.gradle.util.GUtil;

import java.io.File;
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
        platforms.configureEach(plat -> {
            plat.getAllArchives().addCollection(archives);
        });
        if (root != pv) {
            initialize(root.getSchema());
        }
        pv.getBuildGraph().whenReady(ReadyState.AFTER_CREATED, p->{
            platforms.configureEach(plat -> {
                plat.getAllArchives().configureEach(arch -> {
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

    protected void configureArchive(
        ProjectView view,
        PlatformConfig platConfig,
        ArchiveConfig archConfig,
        ArchiveGraph archGraph
    ) {

        final PlatformGraph platGraph = archGraph.platform();

        if (!platConfig.isRoot()) {
            final PlatformGraph parent = platGraph.project().platform(platConfig.getParent().getName());
            platGraph.setParent(parent);
            final ArchiveGraph parentArch = parent.archive(archGraph.getName());
            // Here is where we want to add inheritance points from child to parent platforms.

            archGraph.importLocal(parentArch, false, false);

            platGraph.project().whenReady(ReadyState.BEFORE_READY, ready->{
                // Need to rebase our .required names.
                ((ArchiveConfigInternal)archConfig).fixRequires(platConfig);
            });

        }

        if (archGraph.srcExists()) {
            SourceMeta meta = archGraph.getSource();

            // The assembled configuration is where we'll put the sourceset outputs.
            // We should consider making this a jar-variant instead of dependency...
            final Configuration assembled = archGraph.configAssembled();
            final ConfigurationPublications publications = assembled.getOutgoing();
            final Property<SourceSetOutput> output = view.getObjects().property(SourceSetOutput.class);
            output.convention(view.lazyProvider(()-> meta.getSrc().getOutput()));
            final SourceSetOutput out = output.get();
            final String name = archGraph.getModuleName() + "-assembled";
            final Provider<Set<File>> files = view.lazyProvider(out::getFiles);
            final Provider<TaskDependency> tasks = view.lazyProvider(out::getBuildDependencies);
            LazyFileCollection lazyFiles = new DefaultLazyFileCollection(view, name, files, tasks);
            // TODO: subclass our own dependency type, so it can be detected later.
            assembled.getDependencies().add(new DefaultSelfResolvingDependency(archGraph.getComponentId(name), lazyFiles));
            // experiment to see if we can safely defer this further, by using .withDependencies:
//            assembled.withDependencies(deps->{});

            view.getPlugins().withType(JavaPlugin.class).configureEach(makesArchives->{
                // Register build-java tasks if the java plugin is applied.
                archGraph.getJarTask();
                archGraph.getJavacTask();
            });

        }
        if (platConfig.isRequireSource() || archConfig.isSourceAllowed()) {
            // Instead of isSourceAllowed, we may want to allow platform source
            // requirements to trigger inclusion of super-platforms,
            // without forcing those super-platforms to transitively inherit them.
            // ...would need an intermediate configuration that is transitive only to the sub-platform,
            // (and included in the compileOnly / runtimeOnly configurations).
            archGraph.configTransitive().extendsFrom(archGraph.configSource());
        }

        final Set<String> needed = archConfig.required();
        // We should actually defer this until later...
        for (String need : needed) {
            boolean only = need.endsWith("*");
            if (only) {
                need = need.substring(0, need.length()-1);
            }
            boolean lenient = need.endsWith("?");
            if (lenient) {
                need = need.substring(0, need.length()-1);
            }
            PlatformConfigInternal conf = view.getSchema().findPlatform(need);
            if (conf == null) {
                conf = (PlatformConfigInternal) platConfig;//view.getSchema().getMainPlatform();
            }
            need = GUtil.toLowerCamelCase(need.replace(conf.getName(), ""));
            final PlatformGraph needPlat = platGraph.project().platform(conf.getName());
            final ArchiveGraph neededArchive = needPlat.archive(need);

            // Hm... perhaps we should put this in a BEFORE_READY callback?
            archGraph.importLocal(neededArchive, only, lenient);

        }

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
            need = need.substring(0, check);
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
