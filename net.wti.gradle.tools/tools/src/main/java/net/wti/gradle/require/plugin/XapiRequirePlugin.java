package net.wti.gradle.require.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.require.api.XapiRequire;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.configuration.project.ProjectConfigurationActionContainer;

import javax.inject.Inject;

/**
 * A plugin which adds the xapiRequire {} dsl object to your build scripts.
 *
 * This gives you a concise, convenient syntax for declaring dependencies and build operations
 * between various platforms and artifacts.
 *
 * When using xapiRequire, you must apply plugin: 'xapi-schema' to your root build script,
 * or:<pre>
 *    to a projectName which you `evaluationDependsOn: ':projectName'`,
 *    and
 *    set sub-project-local gradle.properties
 * </pre>
 *
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/31/18 @ 3:11 AM.
 */
public class XapiRequirePlugin implements Plugin<Project> {

    private final ProjectConfigurationActionContainer actions;

    @Inject
    public XapiRequirePlugin(ProjectConfigurationActionContainer actions){
        this.actions = actions;
    }
    @Override
    @SuppressWarnings("unchecked")
    public void apply(Project project) {
        ProjectView view = ProjectView.fromProject(project);
        String xapiRegClass = (String) project.findProperty("xapi.register.class");
        final XapiRequire reg;
        if (xapiRegClass == null) {
            reg = project.getExtensions().create(
                XapiRequire.EXT_NAME,
                XapiRequire.class,
                view
            );
        } else {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Class<? extends XapiRequire> xapiReg = (Class<? extends XapiRequire>) cl.loadClass(xapiRegClass);
                reg = project.getExtensions().create(
                    XapiRequire.class,
                    XapiRequire.EXT_NAME,
                    xapiReg,
                    view
                );
            } catch (ClassNotFoundException e) {
                throw new GradleException("Could not load " + xapiRegClass + " from " + cl, e);
            }
        }
        register(view, reg);
    }

    private void register(ProjectView view, XapiRequire reg) {
        // Wire up listeners for XapiRequire to trigger lazy factories in schema/lib.
        final BuildGraph graph = view.getBuildGraph();
        reg.getRegistrations().configureEach(include -> {
            final ProjectGraph proj = graph.project(view.getPath()).get();
            final String incProj = include.getProject();
            final String incPlat = include.getPlatform();
            final String incArch = include.getArchive();
            final ProjectGraph incProject = graph.getProject(incProj);
            if (incArch == null) {
                if (incPlat == null) {
                    // project-wide requirement; bind every platform + archive pair
                    proj.platforms().all(plat->{
                        plat.archives().all(arch-> {
                            include(view, incProject,  plat, arch);
                        });
                    });
                } else {
                    // platform-wide requirement. This will get sticky if we allow
                    // subprojects to customize archive types, as we don't want to
                    // mess around w/ evaluating the foreign project.
                    final PlatformGraph plat = proj.platform(incPlat);
                    plat.archives().all(arch -> include(view, incProject, plat, arch));
                }
            } else {
                // a single project:platform:archive selector
                final PlatformGraph plat = proj.platform(incPlat);
                final ArchiveGraph arch = plat.archive(incArch);
                include(view, incProject, plat, arch);
            }
        });
    }

    private void include(
        ProjectView self,
        ProjectGraph proj,
        PlatformGraph plat,
        ArchiveGraph arch
    ) {
        final String projName = proj.getName();
        assert !projName.equals(self.getPath());

        final DependencyHandler deps = self.getDependencies();
        final Dependency dep = self.dependencyFor(projName, arch.configAssembled());
        final Configuration transitive = arch.configTransitive();
        deps.add(transitive.getName(), dep);
    }

}
