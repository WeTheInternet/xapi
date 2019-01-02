package net.wti.gradle.require.plugin;

import net.wti.gradle.require.api.XapiRequire;
import net.wti.gradle.schema.api.ArchiveConfig;
import net.wti.gradle.schema.api.XapiSchema;
import net.wti.gradle.schema.internal.PlatformConfigInternal;
import net.wti.gradle.schema.internal.XapiRegistration;
import net.wti.gradle.schema.plugin.XapiSchemaPlugin;
import net.wti.gradle.system.service.GradleService;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.provider.ListProperty;
import org.gradle.configuration.project.ProjectConfigurationActionContainer;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.util.GUtil;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

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
        final Project schemaRoot = XapiSchemaPlugin.schemaRootProject(project);
        XapiSchema schema = (XapiSchema) schemaRoot.getExtensions().getByName(XapiSchema.EXT_NAME);
        String xapiRegClass = (String) project.findProperty("xapi.register.class");
        final XapiRequire reg;
        if (xapiRegClass == null) {
            reg = project.getExtensions().create(
                XapiRequire.EXT_NAME,
                XapiRequire.class,
                schema,
                project.getObjects()
            );
        } else {
            final ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Class<? extends XapiRequire> xapiReg = (Class<? extends XapiRequire>) cl.loadClass(xapiRegClass);
                reg = project.getExtensions().create(
                    XapiRequire.class,
                    XapiRequire.EXT_NAME,
                    xapiReg,
                    schema,
                    project.getObjects()
                );
            } catch (ClassNotFoundException e) {
                throw new GradleException("Could not load " + xapiRegClass + " from " + cl, e);
            }
        }
        if (project != schemaRoot) {
            // TODO: remove the need for this by correctly using the BuildGraph.
            project.getPlugins().apply(XapiSchemaPlugin.class);
        }
        register(project, schema, reg);
    }

    private void register(Project project, XapiSchema schema, XapiRequire reg) {
        // Wire up listeners for XapiRequire to trigger lazy factories in schema/lib.
        actions.add(proj->{

            final ListProperty<XapiRegistration> regs = reg.getRegistrations();
            final List<XapiRegistration> items = regs.get();
            regs.set(Collections.emptyList());
            regs.finalizeValue();
            for (XapiRegistration include : items) {
                final String incProj = include.getProject();
                final String incPlat = include.getPlatform();
                final String incArch = include.getArchive();
                if (incArch == null) {
                    if (incPlat == null) {
                        // project-wide requirement; bind all
                        schema.getPlatforms().configureEach(plat ->
                            plat.getArchives().configureEach(arch ->
                                include(project, incProj, (PlatformConfigInternal) plat, arch)
                            ));
                    } else {
                        // platform-wide requirement. This will get sticky if we allow
                        // subprojects to customize archive types, as we don't want to
                        // mess around w/ evaluating the foreign project.
                        final PlatformConfigInternal plat = schema.getPlatforms().maybeCreate(incPlat);
                        plat.getArchives().configureEach(arch -> include(project, incProj, plat, arch));
                    }
                } else {
                    // a single project:platform:archive selector
                    final PlatformConfigInternal plat = schema.getPlatforms().maybeCreate(incPlat);
                    final ArchiveConfig arch = plat.getArchives().maybeCreate(incArch);
                    include(project, incProj, plat, arch);
                }
            }
        });
    }


    @SuppressWarnings("unchecked")
    private void include(
        Project project,
        String incProj,
        PlatformConfigInternal plat,
        ArchiveConfig arch
    ) {
        if (!incProj.startsWith(":")) {
            incProj = ":" + incProj;
        }

        final String name = plat.configurationName(arch);
        final DependencyHandler deps = project.getDependencies();
        final Dependency dep = deps.project(GUtil.map(
            "path", incProj,
            // This is an egregious hack for now.  Meant to correspond to gradle's apiElements configuration.
            "configuration", name + "Assembled"
        ));
        deps.add(name, dep);
    }
}
