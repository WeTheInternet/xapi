package net.wti.gradle.schema.tasks;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.ArchiveConfigInternal;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.*;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.internal.artifacts.dependencies.DefaultSelfResolvingDependency;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.*;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.util.GFileUtils;
import xapi.gradle.java.Java;

import javax.inject.Inject;
import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/29/18 @ 12:35 AM.
 */
@SuppressWarnings("UnstableApiUsage")
public class XapiReport extends DefaultTask {

    private static final Pattern PATH_FINDER = Pattern.compile("[:;]");

    public static final String TASK_NAME = "xapiReport";

    private final Property<String> report;
    private final SetProperty<String> sourceSets;
    private final Property<Boolean> log;

    private final RegularFileProperty location;

    @Inject
    public XapiReport() {
        final ObjectFactory objects = getProject().getObjects();
        report = objects.property(String.class);
        log = objects.property(Boolean.class);
        sourceSets = objects.setProperty(String.class);
        location = objects.fileProperty();
        final String loc = getDefaultLocation();
        report.convention("Xapi Report " + getProject().getPath() + " -> <empty>");
        log.convention("true".equals(getProject().findProperty("xapi.debug")));
        location.convention(getProject().getLayout().getBuildDirectory().file(loc));

        whenSelected(selected-> {
            getProject().getTasks().withType(JavaCompile.class)
                .configureEach(javac-> {
                    javac.shouldRunAfter(this);
                });
            // generate() is expensive, so we only do it if the task is selected.
            // We do it during configuration phase, however, since we can
            // currently, sadly, run into deadlocks if we wait until configurations can be resolved in parallel
            // i.e. during task execution.
            generate();
        });


    }

    @TaskAction
    public void emitReport() {
        GFileUtils.writeFile(report.get(), location.get().getAsFile());
        if (log.get()) {
            getLogger().quiet(report.get());
        }
    }

    private String getDefaultLocation() {
        final Object prop = getProject().findProperty("xapi.report");
        return prop == null ? "xapiReport/report" : GradleCoerce.unwrapStrings(prop).get(0);
    }

    public void record(XapiSchema schema) {
        final ProjectGraph graph = ProjectView.fromProject(getProject()).getProjectGraph();
        schema.getPlatforms().configureEach(platformConfig->{
            platformConfig.getArchives().configureEach(archiveConfig -> {
                final ArchiveGraph module = ((ArchiveConfigInternal) archiveConfig).findGraph(graph);
                sourceSets.add(module.getSrcName());
            });
        });
    }

    public void generate() {
        StringBuilder b = new StringBuilder("\nXapi Report ");
        b.append(getProject().getPath()).append(" ->\n")
                .append(":\n\tAll Configurations\n\t\t")
                .append(getProject().getConfigurations().stream()
                    .map(c->c.getName() + " extends [" +
                            c.getExtendsFrom().stream().map(Configuration::getName).collect(Collectors.joining(","))
                    + "]" +
                        (
                            c.getOutgoing().getCapabilities().isEmpty() ? "" :
                                "\n\t\t\tCapabilities:\n\t\t\t\t" + c.getOutgoing().getCapabilities().stream()
                                .map(cap -> cap.getGroup() + ":" + cap.getName() + ":" + cap.getVersion())
                                .collect(Collectors.joining("\n\t\t\t\t"))
                        ) + (
                            c.getOutgoing().getVariants().isEmpty() ? "" :
                                "\n\t\t\tVariants:\n\t\t\t\t" + c.getOutgoing().getVariants().stream()
                                .map(variant -> variant.getName() + " -> " + variant.getArtifacts().getFiles().getAsPath())
                                .collect(Collectors.joining("\n\t\t\t\t"))
                        )
                    )
                    .collect(Collectors.joining("\n\t\t")));
        if (getProject().getConvention().findPlugin(JavaPluginConvention.class) == null) {
            b.append("\nSkipping sourcesets as ").append(getProject().getPath()).append(" does not have any valid java plugin");
        } else {
            final SourceSetContainer srcs = Java.sources(getProject());
            for (String platform : sourceSets.get()) {
                final SourceSet src = srcs.findByName(platform);
                b
                    .append("\nSource set ")
                    .append(platform);
                if (src == null) {
                    b.append(": <no source>\n\t\t");
                } else {
                    b
                        .append("\n\tCompile classpath of ")
                        .append(getProject().getPath())
                        .append(":")
                        .append(platform)
                        .append(" ->\n\t\t")
                        .append(fixPath(src.getCompileClasspath().getAsPath()))
                        .append("\n\tRuntime classpath of ")
                        .append(getProject().getPath())
                        .append(":")
                        .append(platform)
                        .append(" ->\n\t\t");
                    try {
                        b
                            .append(fixPath(src.getRuntimeClasspath().getAsPath()))
                        ;
                    } catch (Exception e) {
                        b.append(e.getClass().getName())
                                .append(" resolving classpath: ")
                                .append(e.getMessage())
                                .append("\n")
                         .append(Arrays.asList(e.getStackTrace()).stream().map(StackTraceElement::toString).collect(Collectors.joining("\n")));
                    }
                }
                b.append(":\n\tConfigurations of ")
                    .append(getProject().getPath())
                    .append(":\n\t\t");
                final ConfigurationContainer configs = getProject().getConfigurations();
                Set<String> seen = new HashSet<>();
                printConfigs(platform + "Assembled", "\t\t", configs, b, seen);
                printConfigs(platform + "CompileClasspath", "\t\t", configs, b, seen);

            }
        }
        String is = b.toString();
        if (report.isPresent()) {
            final String was = report.get();
            if (is.equals(was)) {
                return;
            }
            if (!was.contains("<empty>")) {
                is = was + "\n" + is;
            }
        }
        report.set(is);
    }

    private void printConfigs(
        String name,
        String indent,
        ConfigurationContainer configs,
        StringBuilder b,
        Set<String> seen
    ) {
        b.append(name).append(": ");
        if (!seen.add(name)) {
            b.append("(*)\n").append(indent);
            return;
        }
        final Configuration config = configs.findByName(name);
        if (config == null) {
            b.append("<null>");
        } else {
            final DependencySet deps = config.getDependencies();
            b.append(deps.size()).append(" dependencies");
            if (!deps.isEmpty()) {
                appendDeps(b, deps, indent);
            }
            b.append("\n").append(indent);

            final Set<Configuration> parents = config.getExtendsFrom();
            if (!parents.isEmpty()) {
                b.append("Extends From:\n\t")
                 .append(indent);
                for (Configuration parent : parents) {
                    printConfigs(parent.getName(), indent + "\t", configs, b, seen);
                }

            }
        }
        b.append("\n").append(indent);

    }

    private void appendDeps(StringBuilder b, Collection<? extends Dependency> deps, String indent) {
        b.append("(");
        boolean nl = deps.size() > 2;
        for (Dependency dep : deps) {
            b.append(nl ? "\n" + indent + "\t" : " ");
            if (dep instanceof ProjectDependency) {
                b.append("project(path: '")
                    .append(((ProjectDependency) dep).getDependencyProject().getPath())
                    .append("', configuration: '")
                    .append(((ProjectDependency) dep).getTargetConfiguration())
                    .append("'");
            }
            else if (dep instanceof DefaultSelfResolvingDependency) {
                final TaskDependency buildDep = ((DefaultSelfResolvingDependency) dep).getBuildDependencies();
                final FileCollection resolved = ((DefaultSelfResolvingDependency) dep).getFiles();
                b.append("self resolving: ").append(buildDep);
                if (resolved.isEmpty()) {
                    b.append(" <empty>");
                } else {
                    b.append(": ");
                    final Set<File> all = resolved.getFiles();
                    for (File file : all) {
                        b.append(file).append( " and ")
                            .append(all.size()-1).append(" more");
                        break;
                    }

                }
            } else if (dep.getGroup() == null) {
                b.append(dep);
            } else {
                b
                    .append(dep.getGroup())
                    .append(":")
                    .append(dep.getName())
                    .append(":")
                    .append(dep.getVersion())
                    .append(" ")
                    .append(dep)
                ;
                if (dep instanceof ClientModule) {
                    b.append(dep.getReason())
                     .append(" ->\n")
                        .append(indent);
                    appendDeps(b, ((ClientModule) dep).getDependencies(), indent + "\t");
                }
            }
        }
        b.append(" )");
    }

    private String fixPath(String src) {
        return PATH_FINDER.matcher(src).replaceAll("\n\t\t");
    }

    @Input
    public Property<String> getReport() {
        return report;
    }

    public void setReport(String report) {
        this.report.set(report);
    }

    @OutputFile
    public RegularFileProperty getLocation() {
        return location;
    }

    public void setLocation(File location) {
        this.location.set(location);
    }
}
