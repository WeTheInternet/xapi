package net.wti.gradle.require.plugin;

import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.internal.api.ReadyState;
import net.wti.gradle.internal.require.api.ArchiveGraph;
import net.wti.gradle.internal.require.api.BuildGraph;
import net.wti.gradle.internal.require.api.PlatformGraph;
import net.wti.gradle.internal.require.api.ProjectGraph;
import net.wti.gradle.require.api.XapiRequire;
import net.wti.gradle.schema.api.Transitivity;
import net.wti.gradle.schema.internal.XapiRegistration;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
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
        final ProjectGraph proj = graph.project(view.getPath()).get();
        graph.whenReady(ReadyState.FINISHED, done->{

            reg.getRegistrations().configureEach(include -> {
                final String incProj = include.getProject();
                final String incPlat = include.getPlatform();
                final String incArch = include.getArchive();
                final Transitivity trans = include.getTransitivity();
                final Boolean lenient = include.getLenient();

                if (incArch == null) {
                    if (incPlat == null) {
                        // project-wide requirement; bind every platform + archive pair
                        proj.platforms().all(plat->
                            plat.archives().all(arch-> {
                                // hm.  another place where we should filter out modules which don't exist, to avoid creating them
                                include(view, incProj, arch, include, trans, lenient == null ? true : lenient);
                            }
                        ));
                    } else {
                        // platform-wide requirement. This will get sticky if we allow
                        // subprojects to customize archive types, as we don't want to
                        // mess around w/ evaluating the foreign project.
                        final PlatformGraph plat = proj.platform(incPlat);
                        plat.archives().all(arch -> include(view, incProj, arch, include, trans, lenient == null ? true : lenient));
                    }
                } else {
                    // a single project:platform:archive selector.
                    final PlatformGraph plat = proj.platform(incPlat);
                    final ArchiveGraph arch = plat.archive(incArch);
                    include(view, incProj, arch, include, trans, lenient == null ? false : lenient);
                }

            });
        });
    }

    private void include(
        ProjectView self,
        String projId,
        ArchiveGraph arch,
        XapiRegistration include,
        Transitivity trans,
        boolean lenient
    ) {
        self.getProjectGraph().whenReady(ReadyState.FINISHED + 0x100, done->{
            switch (include.getMode()) {
                case project:
                    includeProject(self, projId, include, arch, trans, lenient);
                    return;
                case internal:
                    includeInternal(self, projId, arch, trans, lenient);
                    return;
                case external:
                    includeExternal(self, projId, include, arch, trans, lenient);
                    return;
                default:
                    throw new UnsupportedOperationException(include.getMode() + " was not handled");
            }
        });
    }

    private void includeProject(
        ProjectView self,
        String projId,
        XapiRegistration reg,
        ArchiveGraph arch,
        Transitivity transitivity,
        boolean lenient
    ) {

        final ProjectGraph incProject = self.getBuildGraph().getProject(projId);
        incProject.whenReady(ReadyState.BEFORE_READY, other->{

            final String projName = incProject.getName();
            final String coord = GradleCoerce.unwrapStringNonNull(reg.getFrom());
            String[] coords = coord.split(":");
            assert coords.length <3 : "xapi-coordinate paths cannot have more than two:segments. You sent " + coord;

            self.getLogger().info("Including project {} into arch {} with coordinates {}", projName, arch.getPath(), coord);
            // If there is 1 or fewer requested coordinates, the platform is default platform ("main")...
            String platName = coords.length < 2 ? arch.getDefaultPlatform() : coords[0];
            // If there are 0 coordinates, the module is default module ("main"), otherwise, module is the last item
            String modName = coords.length == 0 ? arch.getDefaultModule() : coords[coords.length-1];

            arch.importProject(projName, platName, modName, transitivity, lenient);
        });
    }

    private void includeInternal(
        ProjectView self,
        String projId,
        ArchiveGraph arch,
        Transitivity transitivity,
        boolean lenient
    ) {
        String[] coords = projId.split(":");
        assert ":".equals(projId) || !projId.endsWith(":") : "Invalid projId " + projId + " cannot end with :";
        final ProjectGraph graph;
        final String platName;
        final String archName;
        final BuildGraph bg = self.getBuildGraph();
        if (coords.length < 2) {
            graph = self.getProjectGraph();
            platName = arch.getDefaultPlatform();
            archName = projId.isEmpty() ? arch.getDefaultModule() : coords[coords.length - 1];
        } else if (coords.length == 2) {
            final String plat = coords[coords.length - 2];
            archName = coords[coords.length - 1];
            if (bg.hasProject(plat)) {
                platName = arch.getDefaultPlatform();
                graph = bg.getProject(plat);
            } else {
                platName = plat;
                graph = self.getProjectGraph();
            }
        } else {
            // more than 2 coordinates; treat this as projectId:plat:mod

            platName = coords[coords.length - 2];
            archName = coords[coords.length - 1];
            String proj = projId.substring(0, projId.length() - platName.length() - archName.length() - 2);
            self.getLogger().warn("{} is using `Requirable.internal '{}'` instead of `Requirable.project '{}', '{}:{}'` ", self.getPath(), projId, proj, platName, archName);
            graph = bg.getProject(proj);
        }
        graph.whenReady(ReadyState.BEFORE_FINISHED, ready->{
            final PlatformGraph reqPlatform = graph.platform(platName);
            final ArchiveGraph reqModule = reqPlatform.archive(archName);

            arch.importLocal(reqModule, transitivity, lenient);
        });
    }

    private void includeExternal(
        ProjectView self,
        String projId,
        XapiRegistration reg,
        ArchiveGraph arch,
        Transitivity only,
        boolean lenient
    ) {

        String requestedGroup = reg.getPlatform();
        String requestedName = reg.getArchive();

        final Dependency dep;
        // The projId here is a g:n[:v] module identifier.
        int lastColon = projId.lastIndexOf(':');
        if (projId.indexOf(':') == lastColon || lastColon == projId.length()-1) {
            // either no version supplied, or `empty-string:for-version:`
            // TODO: this should be looked up from a cache / service somewhere...
            String was = projId;
            projId += ":" + self.getVersion();
            self.getLogger().info("Replacing {} with {}", was, projId);
        }
        final String[] items = projId.split(":");
        assert items.length == 3 : "Malformed path " + projId + "; expected three segments, got " + Arrays.asList(items);
        if ("null".equals(items[2])) {
            self.getLogger().quiet("Replacing \"null\" with {}", projId);
            items[2] = self.getVersion();
        }
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

        arch.importExternal(dep, reg, newGroup, newName, only, lenient);
    }

}
