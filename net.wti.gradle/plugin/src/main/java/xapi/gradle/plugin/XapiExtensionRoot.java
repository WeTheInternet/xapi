package xapi.gradle.plugin;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.file.Directory;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.internal.reflect.Instantiator;
import xapi.fu.In1Out1;

import static java.lang.String.valueOf;

/**
 * TODO: move this type into the tools package, so we can use it globally (in pre-plugin projects).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/22/18 @ 10:29 PM.
 */
public class XapiExtensionRoot extends XapiExtension {

    public static final String EXT_NAME = "xapiRoot";

    private final ListProperty<ModuleDependency> localCache;
    private final Property<Boolean> strictCaching;
    private final Property<Boolean> repoTransient;
    private final Property<String> xapiRepo;
    private final In1Out1<String, Dependency> resolver;

    public XapiExtensionRoot(Project project, Instantiator instantiator) {
        super(project, instantiator);
        localCache = project.getObjects().listProperty(ModuleDependency.class);
        xapiRepo = project.getObjects().property(String.class);
        strictCaching = project.getObjects().property(Boolean.class);
        repoTransient = project.getObjects().property(Boolean.class);

        xapiRepo.set(project.provider(()->{
            Object prop = project.findProperty("xapi.mvn.repo");
            if (prop == null) {
                repoTransient.finalizeValue();
                final ProjectLayout root = project.getRootProject().getLayout();
                final Directory dir = repoTransient.get() ? root.getBuildDirectory().get() : root.getProjectDirectory();
                prop = dir.dir("repo").getAsFile().getAbsolutePath();
                project.setProperty("xapi.mvn.repo", prop);
                project.getLogger().quiet("No -Pxapi.mvn.repo passed in or found in gradle.properties; using guessed location {}", prop);
            }
            return String.valueOf(prop);
        }));
        repoTransient.set(project.provider(()->{
            final Object prop = project.findProperty("xapi.repo.transient");
            Object quick = project.findProperty("xapi.quick");
            if (quick == null) {
                quick = project.findProperty("xq");
            }
            boolean val = "true".startsWith(valueOf(
                prop == null ? quick == null ? getDefaultRepoTransient() : quick : prop
            ));
            if (!val) {
                project.getTasks().named("clean", cln->{
                    cln.getProject().getGradle().buildFinished(res->{
                        cln.getLogger().quiet("You ran clean, but -Pxapi.repo.transient != true;");
                    });
                });
            }
            return val;
        }));
        resolver = defaultResolver(project);
    }

    protected In1Out1<String, Dependency> defaultResolver(Project project) {
        return project.getDependencies()::create;
    }

    protected CharSequence getDefaultRepoTransient() {
        // by default, we'll keep the repo beyond a clean.
        return "false";
    }

    public Property<Boolean> getRepoTransient() {
        return repoTransient;
    }

    public Property<String> getXapiRepo() {
        return xapiRepo;
    }

    public ListProperty<ModuleDependency> getLocalCache() {
        return localCache;
    }

    public Property<Boolean> getStrictCaching() {
        return strictCaching;
    }

    protected boolean isStrict() {
        return strictCaching.isPresent() ? strictCaching.get() : false;
    }

    public void preload(String notation) {
        final Dependency dependency = resolver.io(notation);
        if (!(dependency instanceof ModuleDependency)) {
            throw new GradleException("Invalid notation " + notation +"; only module:identifiers:expected");
        }
        getLocalCache().add((ModuleDependency) dependency);
    }
}
