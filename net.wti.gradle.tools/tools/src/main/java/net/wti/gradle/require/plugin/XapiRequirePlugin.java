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
import org.gradle.api.plugins.JavaBasePlugin;
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
        project.getPlugins().apply(JavaBasePlugin.class);
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

            switch (include.getMode()) {
                case internal:
                    if (incArch == null) {
                        if (incPlat == null) {
                            // project-wide requirement; bind every platform + archive pair
                            proj.platforms().all(plat->
                                plat.archives().all(arch->
                                    includeInternal(view, incProj,  plat, arch)
                            ));
                        } else {
                            // platform-wide requirement. This will get sticky if we allow
                            // subprojects to customize archive types, as we don't want to
                            // mess around w/ evaluating the foreign project.
                            final PlatformGraph plat = proj.platform(incPlat);
                            plat.archives().all(arch -> includeInternal(view, incProj, plat, arch));
                        }
                    } else {
                        // a single project:platform:archive selector
                        final PlatformGraph plat = proj.platform(incPlat);
                        final ArchiveGraph arch = plat.archive(incArch);
                        includeInternal(view, incProj, plat, arch);
                    }
                    break;
                case external:
                    // For an external dependency, we may have metadata already available
                    // about what project/platform/module it maps to.  If not, we can also
                    // use lenient configurations across all modules, and add the correct naming convention to each.

                    // For now, we're just going to use the lenient configuration,
                    // and wire in the differentiation of to/from later (this complexity should be hidden from user).
                    proj.platforms().all(plat->
                        plat.archives().all(arch->
                            includeExternal(view, incProj, incPlat, incArch,  plat, arch)
                    ));

                    break;
                default:
                        throw new UnsupportedOperationException(include.getMode() + " was not handled");
            }
        });
    }

    private void includeInternal(
        ProjectView self,
        String projId,
        PlatformGraph plat,
        ArchiveGraph arch
    ) {

        final Configuration target;
        final DependencyHandler deps = self.getDependencies();
        final Dependency dep;
        final ProjectGraph incProject = self.getBuildGraph().getProject(projId);
        final String projName = incProject.getName();
        assert !projName.equals(self.getPath());
        dep = self.dependencyFor(projName, arch.configAssembled());
        target = arch.configTransitive();
        deps.add(target.getName(), dep);
    }

    private void includeExternal(
        ProjectView self,
        String projId,
        String requestedGroup,
        String requestedName,
        PlatformGraph plat,
        ArchiveGraph arch
    ) {

        final Configuration target;
        final DependencyHandler deps = self.getDependencies();
        final Dependency dep;
        // The projId here is a g:n[:v] module identifier.
        if (projId.indexOf(':') == projId.lastIndexOf(':')) {
            // TODO: this should be looked up from a cache somewhere...
            projId += ":" + self.getVersion();
        }
        dep = self.getDependencies().create(projId);
        // transform the dependency to match our platform/archive
        String newGroup =
            dep.getGroup();
//            arch.asGroup(requestedGroup);
        String newName =
            dep.getName();
//            arch.asModuleName(requestedName);
        // Note: it's actually better to just add the plain g:n:v than it is to add the module-specific name.
        // If you have `com.foo:thing:1`, gradle will know it is variant-mapped and use our import configuration to select correctly.
        // If you have `com.foo:thing-api:1`, gradle will look for it in your local repo, and will fail if it is not found.


        // Hm...  the transitivity here should be controllable...
        // It's already ~too complex for a prototype, so transitive-it-is, for now.
        target = arch.configTransitive();

        // we need per-variant, on-demand inputs to the lenient config.
        Configuration dest = arch.configImportApi(target);

        // Now, make it lenient...  This will copy the attributes set from configImportApi.
        dest = arch.configLenient(dest);

        deps.add(dest.getName(), deps.create(newGroup + ":" + newName + ":" + dep.getVersion()));
    }

}
