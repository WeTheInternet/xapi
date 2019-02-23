package net.wti.gradle.require.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.require.api.XapiRequire;
import net.wti.gradle.schema.internal.XapiRegistration;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.configuration.project.ProjectConfigurationActionContainer;

import javax.inject.Inject;
import java.util.Arrays;

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
        project.getPlugins().apply(XapiSchemaPlugin.class);
        ProjectView view = ProjectView.fromProject(project);

        // We let you specify a classname override of XapiRequire, which we can generate type-safe, project-specific helpers.
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
            final boolean only = !include.getTransitive();
            final Boolean lenient = include.getLenient();

            if (incArch == null) {
                if (incPlat == null) {
                    // project-wide requirement; bind every platform + archive pair
                    proj.platforms().all(plat->
                        plat.archives().all(arch-> {
                            include(view, incProj, arch, include, only, lenient == null ? true : lenient);
                        }
                    ));
                } else {
                    // platform-wide requirement. This will get sticky if we allow
                    // subprojects to customize archive types, as we don't want to
                    // mess around w/ evaluating the foreign project.
                    final PlatformGraph plat = proj.platform(incPlat);
                    plat.archives().all(arch -> include(view, incProj, arch, include, only, lenient == null ? true : lenient));
                }
            } else {
                // a single project:platform:archive selector.
                final PlatformGraph plat = proj.platform(incPlat);
                final ArchiveGraph arch = plat.archive(incArch);
                include(view, incProj, arch, include, only, lenient == null ? false : lenient);
            }

        });
    }

    private void include(
        ProjectView self,
        String projId,
        ArchiveGraph arch,
        XapiRegistration include,
        boolean only,
        boolean lenient
    ) {
        switch (include.getMode()) {
            case project:
                includeProject(self, projId, arch, only, lenient);
                return;
            case internal:
                includeInternal(self, projId, arch, only, lenient);
                return;
            case external:
                includeExternal(self, projId, include.getPlatform(), include.getArchive(), arch, only, lenient);
                return;
            default:
                throw new UnsupportedOperationException(include.getMode() + " was not handled");
        }
    }

    private void includeProject(
        ProjectView self,
        String projId,
        ArchiveGraph arch,
        boolean only,
        boolean lenient
    ) {

        final ProjectGraph incProject = self.getBuildGraph().getProject(projId);
        incProject.whenReady(ReadyState.BEFORE_READY, other->{
            final String projName = incProject.getName();
            arch.importGlobal(projName, only, lenient);
        });
    }

    private void includeInternal(
        ProjectView self,
        String projId,
        ArchiveGraph arch,
        boolean only,
        boolean lenient
    ) {
        String[] coords = projId.split(":");
        assert coords.length == 2 : "Invalid coordinates " + coords + " expected `platform:module` pair";
        final ProjectGraph graph = self.getProjectGraph();
        graph.whenReady(ReadyState.BEFORE_READY, ready->{
            final PlatformGraph reqPlatform = graph.platform(coords[0]);
            final ArchiveGraph reqModule = reqPlatform.archive(coords[1]);

            arch.importLocal(reqModule, only, lenient);
        });
    }

    private void includeExternal(
        ProjectView self,
        String projId,
        String requestedGroup,
        String requestedName,
        ArchiveGraph arch,
        boolean only,
        boolean lenient
    ) {

        final DependencyHandler deps = self.getDependencies();
        final Dependency dep;
        // The projId here is a g:n[:v] module identifier.
        if (projId.indexOf(':') == projId.lastIndexOf(':')) {
            // TODO: this should be looked up from a cache / service somewhere...
            projId += ":" + self.getVersion();
        }
        final String[] items = projId.split(":");
        assert items.length == 3 : "Malformed path " + projId + "; expected three segments, got " + Arrays.asList(items);
        dep = self.getDependencies().create(projId);
        // transform the dependency to match our platform/archive
        String newGroup =
//            dep.getGroup();
            arch.asGroup(requestedGroup == null ? items[0] : requestedGroup);
        String newName =
//            dep.getName();
            arch.asModuleName(requestedName == null ? items[1] : requestedName);
        // Note: it's actually better to just add the plain g:n:v than it is to add the module-specific name.
        // If you have `com.foo:thing:1`, gradle will know it is variant-mapped and use our import configuration to select correctly.
        // If you have `com.foo:thing-api:1`, gradle will look for it in your local repo, and will fail if it is not found.

        arch.importExternal(dep, newGroup, newName, only, lenient);
    }

}
