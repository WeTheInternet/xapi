package net.wti.gradle.settings.api;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.internal.ProjectViewInternal;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import org.gradle.internal.reflect.Instantiator;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.data.ListLike;
import xapi.fu.data.MapLike;
import xapi.fu.data.MultiList;
import xapi.fu.data.SetLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.X_Jdk;
import xapi.fu.log.Log;
import xapi.string.X_String;

/**
 * Abstraction layer over a project descriptor, like <project-name virtual=true multiplatform=false />
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-06 @ 4:39 a.m..
 */
public class SchemaProject implements Named, HasPath {

    private static final Log LOG = Log.loggerFor(SchemaProject.class);
    private final String name;
    private final MapLike<String, SchemaProject> children;
    private final NamedDomainObjectSet<SchemaModule> modules;
    private final NamedDomainObjectSet<SchemaPlatform> platforms;
    private final MultiList<PlatformModule, SchemaDependency> dependencies;
    private Boolean multiplatform, virtual, inherit;
    private final SchemaProject parent;
    private final MinimalProjectView view;
    private boolean loaded;
    private String parentGradlePath;
    private boolean force;

    public SchemaProject(
            MinimalProjectView view,
            String name,
            Boolean multiplatform,
            Boolean virtual,
            Boolean inherit
    ) {
        this(null, view, name, multiplatform, virtual, inherit);
    }

    public SchemaProject(SchemaProject parent, MinimalProjectView view, String name, Boolean multiplatform, Boolean virtual, final Boolean inherit) {
        this.view = view;
        this.name = name;
        this.parent = parent;
        this.multiplatform = multiplatform;
        this.virtual = virtual;
        this.inherit = inherit;
        children = X_Jdk.mapOrderedInsertion();
        final Instantiator instantiator = ProjectViewInternal.findInstantiator(view);
        final CollectionCallbackActionDecorator decorator = ProjectViewInternal.findDecorator(view);
        modules = new DefaultNamedDomainObjectSet<>(SchemaModule.class, instantiator, decorator);
        platforms = new DefaultNamedDomainObjectSet<>(SchemaPlatform.class, instantiator, decorator);
        dependencies = X_Jdk.multiListOrderedInsertion();
    }

    public String getName() {
        return name;
    }

    public String getPublishedName() {
        return name; // for now just return name; we'll wire this up properly when we bother to make it configurable
    }

    public SizedIterable<SchemaProject> getChildren() {
        return children.mappedValues();
    }

    public boolean isMultiplatform() {
        return Boolean.TRUE.equals(multiplatform);
    }

    public boolean isVirtual() {
        return Boolean.TRUE.equals(virtual);
    }

    public boolean isInherit() {
        return Boolean.TRUE.equals(inherit);
    }

    public SchemaModule addModule(SchemaModule module) {
        final SchemaModule result;
        if (modules.contains(module)) {
            final SchemaModule existing = modules.getByName(module.getName());
            modules.remove(existing);
            result = existing.update(module);
            modules.add(result);
        } else {
            modules.add((result = module));
        }
        for (SchemaProject child : children.mappedValues()) {
            child.addModule(module);
        }
        return result;
    }

    public SchemaPlatform addPlatform(SchemaPlatform platform) {
        SchemaPlatform result;
        if (platforms.contains(platform)) {
            final SchemaPlatform existing = platforms.getByName(platform.getName());
            platforms.remove(platform);
            result = existing.update(platform);
            platforms.add(result);
        } else {
            platforms.add((result = platform));
        }
        for (SchemaProject child : children.mappedValues()) {
            child.addPlatform(platform);
        }
        return result;
    }

    @Override
    public String getPath() {
        if (parent == null) {
            return "";
        }
        String parentPath = parent.getPath();
        if (parentPath.isEmpty()) {
            return getName();
        }
        return parentPath + "/" + getName();
    }

    public MultiList<PlatformModule, SchemaDependency> getDependencies() {
        return dependencies;
    }

    public String getPathGradle() {
        if (parent == null) {
            return ":";
        }
        StringBuilder path = new StringBuilder(parent.getPathGradle());
        if (path.charAt(path.length()-1) != ':') {
            path.append(':');
        }
        path.append(getName());
        return path.toString();
    }

    public String getPathIndex() {
        String pathIndex = calcPathIndex();
        if ("_".equals(pathIndex)) {
            return "_" + getName();
        }
        return pathIndex;
    }

    private String calcPathIndex() {
        final String name = getName();
        if (parent == null) {
            return "_";
        }
        StringBuilder path = new StringBuilder(parent.calcPathIndex());
        if (path.length() > 1) {
            path.append("_");
        }
        path.append(name);
        return path.toString();
    }

    public SchemaProject getRoot() {
        return parent == null ? this : parent.getRoot();
    }

    public String getSubPath() {
        return getPath().replace(getRoot() + "/", "");
    }

    public SchemaProject getParent() {
        return parent;
    }

    @Override
    public String toString() {
        return "proj{" +
                "path='" + getPath() + "'" +
                (isMultiplatform() ? "+multi" : "") +
                (isVirtual() ? "+virt" : "") +
                (isInherit() ? "+inherit" : "") +
                (children.isEmpty() ? "" : "[" + children.keys().join(" , ") + "]" ) +
                '}';
    }

    public MappedIterable<? extends SchemaModule> getAllModules() {
        final SetLike<SchemaModule> mods = X_Jdk.setLinked();
        if (inherit && parent != null) {
            mods.addNow(parent.getAllModules());
            mods.removeAllEquality(X_Jdk.toSet(modules));
        }
        mods.addNow(modules);
        return mods;
    }

    public MappedIterable<? extends SchemaPlatform> getAllPlatforms() {
        final SetLike<SchemaPlatform> mods = X_Jdk.setLinked();
        if (inherit && parent != null) {
            mods.addNow(parent.getAllPlatforms());
            mods.removeAllEquality(X_Jdk.toSet(platforms));
        }
        mods.addNow(platforms);
        return mods;
    }

    public MinimalProjectView getView() {
        return view;
    }

    public void forAllPlatforms(In1<SchemaPlatform> callback) {
        getAllPlatforms().forAll(callback::in);
    }

    public void forAllModules(In1<SchemaModule> callback) {
        getAllModules().forAll(callback::in);
    }

    public void forAllPlatformsAndModules(In2<SchemaPlatform, SchemaModule> callback) {
        forAllPlatforms(plat -> forAllModules(mod -> callback.in(plat, mod)));
    }



    public SchemaPlatform getPlatform(String platform) {
        return platforms.getByName(platform);
    }

    public SchemaModule getModule(String module) {
        return modules.getByName(module);
    }

    public SchemaPlatform findPlatform(String platform) {
        return platforms.findByName(platform);
    }

    public SchemaModule findModule(String module) {
        return modules.findByName(module);
    }

    public ListLike<SchemaDependency> getDependenciesOf(SchemaPlatform platform, SchemaModule module) {
        ListLike<SchemaDependency> result = X_Jdk.list();
        getDependencies().forEachPair((platMod, dep) -> {

            if (platMod.getPlatform() == null) {
                if (platMod.getModule() == null) {
                    result.add(dep);
                } else if (platMod.getModule().equals(module.getName())){
                    result.add(dep);
                }
            } else if (platMod.getPlatform().equals(platform.getName())) {
                if (platMod.getModule() == null) {
                    result.add(dep);
                } else if (platMod.getModule().equals(module.getName())){
                    result.add(dep);
                }
            }
        });
        return result;
    }

    public boolean hasProject(final String name) {
        return children.has(name);
    }

    public SchemaProject getProject(final String name) {
        return children.get(name);
    }

    public void addProject(final SchemaProject child) {
        final SchemaProject result = children.put(child.name, child);
        if (result != null && result != child) {
            throw new IllegalArgumentException("Cannot overwrite project " + child.getName() + "; you should instead get + mutate existing children!");
        }
    }

    public void setLoaded(final boolean loaded) {
        this.loaded = loaded;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setMultiplatform(final boolean multiplatform) {
        this.multiplatform = multiplatform;
    }

    public void setVirtual(final boolean virtual) {
        this.virtual = virtual;
    }

    public void setParentGradlePath(final String parentGradlePath) {
        this.parentGradlePath = parentGradlePath;
    }

    public String getParentGradlePath() {
        return parentGradlePath;
    }

    public boolean isForce() {
        return force;
    }

    public void setForce(final boolean force) {
        this.force = force;
    }

    public void trimPlatforms(final String explicit) {
        assert X_String.isNotEmpty(explicit) : "Don't try to trim non-explicit platform";
        SchemaPlatform[] all = platforms.toArray(new SchemaPlatform[0]);
        SetLike<SchemaPlatform> winners = X_Jdk.setHashIdentity();
        for (String allowed : explicit.split(",")) {
            SchemaPlatform winner = findPlatform(allowed);
            if (winner == null) {
                LOG.log(SchemaProject.class, Log.LogLevel.WARN,
                        "Project " + getPathGradle() + " did not have platform " + allowed);
                if (winners.isEmpty() && !explicit.contains(",")) {
                    LOG.log(SchemaProject.class, Log.LogLevel.WARN,
                            "Project " + getPathGradle() + " will instead retain platform 'main'");
                    // no winning platform... only leave main alive
                    for (SchemaPlatform platform : all) {
                        if ("main".equals(platform.getName())) {
                            winner = platform;
                            break;
                        }
                    }
                }
            }
            if (winner != null) {
                // there is a winning platform. It and everything it replaces lives. Everything else dies.
                winners.add(winner);
                while (winner != null) {
                    final String replace = winner.getReplace();
                    if (X_String.isEmpty(replace)) {
                        winner = null;
                    } else {
                        SchemaPlatform newWinner = findPlatform(replace);
                        assert newWinner != null : "Bad platform " + winner.getName() +
                                " tries to replace platform " + replace + "; but no such platform exists" +
                                " in '" + platforms.getNames() + "'";
                        winner = newWinner;
                        winners.add(winner);
                    }
                }

            }
        }

        for (SchemaPlatform platform : all) {
            if (!winners.contains(platform)) {
                // prune this non-live platform!
                LOG.log(SchemaProject.class, Log.LogLevel.INFO,
                        "Project " + getPathGradle() + " pruning non-live platform " + platform.getName());
                platforms.remove(platform);
                platform.setDisabled(true);
            }
        }

    }

    public boolean hasExplicitDependencies(final PlatformModule module) {
        return dependencies.getMaybe(module).mapIfPresent(deps-> {
            for (SchemaDependency dep : deps) {
                if (dep.getType() != DependencyType.internal) {
                    return true;
                }
            }
            return false;
        }).ifAbsentReturn(false);
    }

    public SchemaModule getDefaultModule() {
        return modules.getByName("main");
    }

    public SchemaPlatform getDefaultPlatform() {
        return platforms.getByName("main");
    }

    public boolean hasModule(final String includes) {
        return modules.getNames().contains(includes);
    }
}

