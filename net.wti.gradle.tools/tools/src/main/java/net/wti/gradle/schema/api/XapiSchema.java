package net.wti.gradle.schema.api;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.impl.IntermediateJavaArtifact;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.DefaultUsageType;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.internal.*;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import net.wti.gradle.schema.tasks.XapiReport;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleCoerce;
import net.wti.gradle.system.tools.GradleMessages;
import org.gradle.api.Action;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.artifacts.ConfigurationPublications;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Usage;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.artifacts.ArtifactAttributes;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.internal.attributes.ImmutableAttributes;
import org.gradle.api.internal.plugins.DefaultArtifactPublicationSet;
import org.gradle.api.internal.tasks.TaskDependencyFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.plugins.ide.idea.IdeaPlugin;
import org.gradle.plugins.ide.idea.model.IdeaModule;
import org.gradle.util.ConfigureUtil;
import org.gradle.util.GUtil;
import xapi.gradle.fu.LazyString;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME;

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
    private final ProjectView view;
    private Object mainPlatform;

    public XapiSchema(ProjectView self) {
        this.view = self;
        final Instantiator instantiator = self.getInstantiator();
        // There's a nasty diamond problem around the main platform, and the schema containers
        // for platform and archive types.  We work around it by sending deferred "provide main platform" logic.
        Callable<PlatformConfigInternal> getMain = this::getMainPlatform;

        assert GradleMessages.noOpForAssertion(()->new DefaultArchiveConfigContainer(getMain, self));
        archives = instantiator.newInstance(DefaultArchiveConfigContainer.class, getMain, self);

        assert GradleMessages.noOpForAssertion(()->new DefaultPlatformConfigContainer(self, archives));
        platforms = instantiator.newInstance(DefaultPlatformConfigContainer.class, self, archives);

        final ProjectView root = XapiSchemaPlugin.schemaRootProject(self);
        final ProjectGraph rpg = root.getProjectGraph();
        final ProjectGraph spg = self.getProjectGraph();
        if (root == self) {
            // When we are the schema root, we must wait until we are evaluated before we let other XapiSchema instances
            // read from our values.
            rpg.whenReady(ReadyState.RUN_FINALLY + 1, ready->{
                platforms.configureEach(plat ->
                    archives.configureEach(arch-> {
                        rpg.whenReady(ReadyState.RUN_FINALLY + 2, allReady->{
                            final ArchiveConfig inst = plat.getArchive(arch.getName());
                            inst.require(arch.required());
                            inst.setPublishedProvider(self.lazyProvider(arch::isPublished));
                            inst.setSourceAllowed(arch.isSourceAllowed());
                            inst.setTest(arch.isTest());
                            self.getLogger().trace("root schema ({}~{}) declared {}", new LazyString(()-> GradleService.buildId(self.getGradle()).toString()), self.getPath(), inst);
                            final PlatformConfig rootPlat = plat.getRoot();
                        });

                    })
                );
            });
        } else {
            root.whenReady(evaluated->{
                // We must further wait for the schema root to be in a state of task-flushing before we try to read from it.
                final XapiSchema rootSchema = root.getSchema();
                rootSchema.platforms.configureEach(rooted->{
                    final PlatformConfigInternal myPlat = platforms.maybeCreate(rooted.getName());
                    myPlat.baseOn(rooted);
                    if (rooted.getParent() != null) {
                        final PlatformConfigInternal myParent = platforms.maybeCreate(rooted.getParent().getName());
                        myParent.baseOn(rooted.getParent());
                        myPlat.setParent(myParent);
                    }
                });
                rootSchema.archives.configureEach(rooted->{
                    // we want to create all archive types immediately, but we don't want to initialize them until scripts are evaluated.
                    //  ...well, it would actually be nice to do more eager evaluation, but the current state machine makes that impossible.
                    final ArchiveConfigInternal myArch = archives.maybeCreate(rooted.getName());
                    myArch.baseOn(rooted, true);
                });
                rpg.whenReady(ReadyState.BEFORE_CREATED, ready -> {
                    spg.whenReady(ReadyState.BEFORE_CREATED, alsoReady -> {
                        rootSchema.archives.configureEach(rooted->{
                            final ArchiveConfigInternal myArch = archives.maybeCreate(rooted.getName());
                            myArch.baseOn(rooted, false);
                        });

                        platforms.configureEach(plat -> {
                            if (plat.getParent() != null) {
                                archives.configureEach(arch -> {
                                    final ArchiveConfig local = plat.getArchive(arch.getName());
                                    ((ArchiveConfigInternal)local).baseOn(arch, false);
                                });
                            }
                        });
                        initialize(rootSchema);
                    });
                });
            });
        }
//        self.getBuildGraph().whenReady(ReadyState.AFTER_CREATED, p->{
        spg.whenReady(ReadyState.AFTER_CREATED, p->{
            platforms.configureEach(plat -> {
                plat.getArchives().configureEach(arch -> {
                    final ArchiveGraph archive = self.getProjectGraph()
                        .platform(plat.getName())
                        .archive(arch.getName());

                    configureArchive(self, plat, arch, archive);

                });
            });
        });

        self.getPlugins().withType(IdeaPlugin.class).all( plugin -> {
            platforms.configureEach(plat -> {
                plat.getArchives().configureEach(arch -> {
                    if (!arch.isTest()) {
                        return;
                    }
                    final ArchiveGraph archive = self.getProjectGraph()
                        .platform(plat.getName())
                        .archive(arch.getName());
                    if (!archive.srcExists()) {
                        view.getLogger().info("Skipping module {}; source set? {}", archive.getPath(), self.getSourceSets().findByName(archive.getSrcName()));
                        return;
                    }
//                            self.getGradle().buildFinished(res->{
                    final IdeaModule module = plugin.getModel().getModule();
                    final SourceSet srcSet = archive.getSource().getSrc();
                    addTestDirs(self, module, srcSet, true);
                    addTestDirs(self, module, srcSet, false);
//                            });
                        }
                    );
                });
        });
        self.getTasks().register(XapiReport.TASK_NAME, XapiReport.class, report ->
            report.record(self.getSchema())
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

            archGraph.importLocal(parentArch, DefaultUsageType.Api, Transitivity.api, false);
            archGraph.importLocal(parentArch, DefaultUsageType.Runtime, Transitivity.runtime, false);

        }

        final ConfigurationPublications exportedApi = archGraph.configExportedApi().getOutgoing();
        final ConfigurationPublications exportedRuntime = archGraph.configExportedRuntime().getOutgoing();
        view.getLogger().info("Configuring {}; realized? {}", archGraph.getPath(), archGraph.realized());
        if (archGraph.realized()) {
            final TaskDependencyFactory taskFactory = ((GradleInternal)view.getGradle()).getServices().get(TaskDependencyFactory.class);

            if (archConfig.isTest()) {
                archGraph.whenReady(ReadyState.AFTER_CREATED, done->{
                    if (!(
                            platConfig.getMainModuleName().equals(archConfig.getName())
                            &&
                            view.getSchema().getMainPlatformName().equals(platConfig.getName())
                    )) {
                        try {
                            // a non main:main test module _should_ have a Test task hooked up! ...but is not required.
                            final TaskProvider<Test> testTask = archGraph.getTasks().getTestTask();
                            view.getTasks().named(CHECK_TASK_NAME, check->
                                check.dependsOn(testTask));
                            testTask.get();
                        } catch (UnknownTaskException ignored) {}
                    }
                });
            }

            IntermediateJavaArtifact compiled = new IntermediateJavaArtifact(taskFactory, ArtifactTypeDefinition.JVM_CLASS_DIRECTORY, archGraph.getJavacTask()) {
                @Override
                public File getFile() {
                    return archGraph.getJavacTask().get().getDestinationDir();
                }
            };
            IntermediateJavaArtifact packaged = new IntermediateJavaArtifact(taskFactory, ArtifactTypeDefinition.JAR_TYPE, archGraph.getJarTask()) {
                @Override
                public File getFile() {
                    return archGraph.getJarTask().get().getArchiveFile().get().getAsFile();
                }
            };


            final LazyPublishArtifact artifact = new LazyPublishArtifact(archGraph.getJarTask(), archGraph.getVersion(), null, taskFactory);
            view.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(artifact);
            exportedRuntime.artifact(artifact);

            exportedRuntime.getAttributes().attribute(ArtifactAttributes.ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE);

            view.getExtensions().getByType(DefaultArtifactPublicationSet.class).addCandidate(artifact);

            exportedApi.variants(variants -> {
                variants.create("assembled", variant -> {
                    variant.artifact(compiled);
                    variant.attributes(attributes->{
                        archGraph.withAttributes(attributes);
                        attributes.attribute(Usage.USAGE_ATTRIBUTE, platGraph.project().usageApiClasses());
                    });
                });
                variants.create("packaged", variant -> {
                    variant.artifact(packaged);
                    variant.attributes(attributes->{
                        archGraph.withAttributes(attributes);
                        attributes.attribute(Usage.USAGE_ATTRIBUTE, platGraph.project().usageRuntime());
                    });
                });
            });

//            exportedRuntime.artifact(packaged);
            exportedRuntime.variants(variants -> {
                variants.create("assembled", variant -> {
                    variant.artifact(compiled);
                    variant.attributes(attributes->{
                        archGraph.withAttributes(attributes);
                        attributes.attribute(Usage.USAGE_ATTRIBUTE, platGraph.project().usageApiClasses());
                    });
                });
                variants.create("packaged", variant -> {
                    variant.artifact(packaged);
                    variant.attributes(attributes->{
                        archGraph.withAttributes(attributes);
                        attributes.attribute(Usage.USAGE_ATTRIBUTE, platGraph.project().usageRuntimeJars());
                    });
                });
            });

        }
        if (platConfig.isRequireSource() || archConfig.isSourceAllowed()) {
            // Instead of isSourceAllowed, we may want to allow platform source
            // requirements to trigger inclusion of super-platforms,
            // without forcing those super-platforms to transitively inherit them.
            // ...would need an intermediate configuration that is transitive only to the sub-platform,
            // (and included in the compileOnly / runtimeOnly configurations).
            archGraph.configTransitive().extendsFrom(archGraph.configSource()); // not depending on the exported variant,
                                                                                // as we want to avoid devolving into a jar, from source
        }

        platGraph.project().whenReady(ReadyState.READY, ready->{
//        platGraph.project().whenReady(ReadyState.BEFORE_READY + 0x40, ready->{

            // TODO: turn resolveNeeded into onNeeded(()->{});
            final Set<LazyString> needed = resolveNeeded(archConfig);
            view.getLogger().debug("{} requires {}", archGraph.getModuleName(), needed);

            for (LazyString needs : needed) {
                String need = needs.toString();
                boolean only = need.endsWith("*");
                if (only) {
                    need = need.substring(0, need.length()-1);
                }
                boolean lenient = need.endsWith("?");
                if (lenient) {
                    need = need.substring(0, need.length()-1);
                }
                PlatformConfigInternal plat = null;
                if (need.contains(":")) {
                    String[] bits = need.split(":");
                    assert bits.length == 2 : "Malformed module require: " + need + " can have at most one :";
                    plat = view.getSchema().findPlatform(bits[0]);
                    need = bits[1];
                }
                if (plat == null) {
                    plat = (PlatformConfigInternal) platConfig;
                }
                final String platName = plat.getName();
                if (!platName.equals(need)) {
                    need = GUtil.toLowerCamelCase(need.replace(platName, ""));
                }
                final PlatformGraph needPlat = platGraph.project().platform(platName);
                final ArchiveGraph neededModule = needPlat.archive(need);

                // neededModule.realized() checks if our module has source code, or is any transitive dependency of a module with source code.
                if (neededModule.realized()) {
                    archGraph.getView().getLogger().info("Realizing {}", new LazyString(archGraph::getPath));
                    archGraph.getSource();
                    archGraph.importLocal(neededModule, DefaultUsageType.Api, only ? Transitivity.compile_only : Transitivity.api, lenient);
                    archGraph.importLocal(neededModule, DefaultUsageType.Runtime, only ? Transitivity.runtime_only : Transitivity.runtime, lenient);
                } else {
                    // we need to depend on transitive dependencies of missing-src modules.
                    // idea / solution: move all archGraph.import* calls behind a ModuleRequest (ArchiveRequest),
                    // so we can register our intent to build a gradle dependency graph,
                    // and transverse through no-source modules, including their transitive dependencies
                    // until each branch either produces an inheritance point, or runs out of dependencies to consider.

                    // This will allow us to skip through a missing sourceSet (that we avoid paying to create),
                    // and directly inherit any existing transitive dependencies.

                    view.getLogger().trace("{}.{} ignoring no-source module {}", archGraph.getModuleName(), archGraph.getSrcName(), neededModule.getSrcName());
                }


            }
        });

    }

    private static void addTestDirs(ProjectView view, IdeaModule module, SourceSet srcSet, boolean sources) {

        Set<File> items =
            (sources ?
                srcSet.getAllJava().getSourceDirectories().getFiles() :
                srcSet.getAllSource().getSourceDirectories().minus(
                    srcSet.getAllJava().getSourceDirectories()
                ).getFiles()
            )
            .stream()
            .filter(File::exists)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        sources = true;
        if (!items.isEmpty()) {
            final Set<File> src = sources ? module.getTestSourceDirs() : module.getTestResourceDirs();
            if (src != null) {
                src.addAll(items);
                items = new LinkedHashSet<>(src);
            }
            if (sources) {
                module.setTestSourceDirs(items);
            } else {
                module.setTestResourceDirs(items);
            }

            final Stream<File> dirs = items.stream();
            view.getLogger().trace("Adding test {}source directories:\n\t{}",
                sources ? "" : "re",
                new LazyString(()->
                    dirs.map(File::getAbsolutePath).collect(Collectors.joining("\n\t" ))
                )
            );
        }
    }

    protected Set<LazyString> resolveNeeded(ArchiveConfig mod) {
        // TODO: memoize results, lock provider with memoized result.
        final SetProperty<LazyString> req = mod.required();
        req.finalizeValue();
        final Set<LazyString> mine = req.get();
        final Set<LazyString> all = new LinkedHashSet<>();
        all.addAll(mine);
        addTransitive(mod, all, mine);
        return all;
    }

    private void addTransitive(ArchiveConfig mod, Set<LazyString> all, Set<LazyString> mine) {
        for (LazyString required : mine) {
            String named = required.toString();
            final ArchiveConfig req = mod.getPlatform().getArchive(named);
            Set<LazyString> need = req.required().get();
            if (all.addAll(need)) {
                addTransitive(mod, all, need);
            }
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

    public PlatformConfigInternal findPlatform(final String orig) {
        String need = orig;
        PlatformConfig plat = platforms.findByName(need);
        while (plat == null) {
            int check = need.length();
            do {
                if (check == 0) {
                    return orig.isEmpty() ? getMainPlatform() : null;
                }
            }
            while (
                Character.isLowerCase(need.charAt(--check)) &&
                Character.isJavaIdentifierPart(need.charAt(check))
            );

            need = Character.toLowerCase(need.charAt(0)) + (
                need.length() == 1 ? "" :
                    need.substring(1, check)
            );
            plat = platforms.findByName(need);
        }
        return (PlatformConfigInternal) plat;
    }

    public void initialize(XapiSchema rootSchema) {
        archives.setWithClassifier(rootSchema.archives.isWithClassifier());
        archives.setWithCoordinate(rootSchema.archives.isWithCoordinate());
        archives.setWithSourceJar(rootSchema.archives.isWithSourceJar());

    }

    public PlatformConfigInternal getMainPlatform() {
        return platforms.maybeCreate("main");
    }

    public ImmutableAttributes getAttributes(String xapiCoords, String newGroup, String newName) {
        // TODO: consult a registry before relying on naming conventions...
        PlatformConfig plat = null;
        String left;
        if (xapiCoords != null) {
            String[] coords = xapiCoords.split(":");
            if (coords.length == 2) {
                plat = platforms.getByName(coords[0]);
                left = coords[1];
            } else if (coords.length == 1) {
                plat = platforms.getByName(getMainPlatformName());
                left = coords[0];
            } else {
                assert coords.length == 0 : "Invalid xapi coordinated " + xapiCoords + " for " + newGroup + ":" + newName;
                plat = platforms.getByName(getMainPlatformName());
                left = "main";
            }
        } else {
            int lastDot = newGroup.lastIndexOf('.');
            if (lastDot != -1) {
                String suffix = newGroup.substring(lastDot+1);
                if (platforms.hasWithName(suffix)) {
                    plat = platforms.getByName(suffix);
                }
            }
            String[] bits = newName.split("-");
            String suffix = bits[bits.length-1];
            if (plat == null) {
                plat = findPlatform(suffix);
                if (plat == null) {
                    plat = getMainPlatform();
                }
            }
            if (bits.length == 1) {
                // There is no suffix.  assume main.
                left = "main";
            } else {
                left = GUtil.toLowerCamelCase(suffix.replace(plat.getName(), ""));
                if (!plat.getArchives().hasWithName(left)) {
                    // Hm... should probably log or throw here, and enforce registrations...
                    left = "main";
                }
            }
        }
        final ArchiveConfig arch = plat.getArchive(left);
        return arch.getAttributes(view);
    }

    @Override
    public String toString() {
        return "XapiSchema{" +
            "platforms=" + platforms +
            '}';
    }

    public void whenReady(Action<? super XapiSchema> o) {
        view.getProjectGraph().whenReady(ReadyState.BEFORE_READY + 0x60, ready-> {
//        view.getProjectGraph().whenReady(ReadyState.AFTER_CREATED + 0x10, ready-> {
            o.execute(this);
        });
    }

    public ArchiveGraph module(Object platform, Object module) {
        return view.getProjectGraph().platform(GradleCoerce.unwrapStringOr(platform, ""))
            .archive(GradleCoerce.unwrapString(module));
    }

    public boolean shouldPublish(ArchiveConfigInternal module) {
        if (!module.getPlatform().isPublished()) {
            return false;
        }
        // Heuristic is that, by default, main, and anything required by main
        // should be published.
        final ArchiveConfigInternal main = module.getPlatform().getMainModule();
        return main.isOrRequires(module);
    }

    public String getMainPlatformName() {
        String s = GradleCoerce.unwrapStringNonNull(mainPlatform);
        return s.isEmpty() ? PlatformModule.defaultPlatform.call() : s;
    }

    public void setMainPlatform(Object mainPlatform) {
        this.mainPlatform = mainPlatform;
    }
}
