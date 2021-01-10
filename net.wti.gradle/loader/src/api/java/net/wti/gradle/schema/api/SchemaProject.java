package net.wti.gradle.schema.api;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.map.internal.SchemaDependency;
import org.gradle.api.Named;
import org.gradle.api.NamedDomainObjectSet;
import org.gradle.api.internal.DefaultNamedDomainObjectSet;
import xapi.fu.In1;
import xapi.fu.data.ListLike;
import xapi.fu.data.MultiList;
import xapi.fu.data.SetLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.java.X_Jdk;

/**
 * Abstraction layer over a project descriptor, like <project-name virtual=true multiplatform=false />
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-06 @ 4:39 a.m..
 */
public class SchemaProject implements Named, HasPath {
    private final String name;
    private final SetLike<SchemaProject> children;
    private final NamedDomainObjectSet<SchemaModule> modules;
    private final NamedDomainObjectSet<SchemaPlatform> platforms;
    private final MultiList<PlatformModule, SchemaDependency> dependencies;
    private final boolean multiplatform, virtual;
    private final SchemaProject parent;
    private final MinimalProjectView view;

    public SchemaProject(
        MinimalProjectView view,
        String name,
        boolean multiplatform,
        boolean virtual
    ) {
        this(null, view, name, multiplatform, virtual);
    }

    public SchemaProject(SchemaProject parent, MinimalProjectView view, String name, boolean multiplatform, boolean virtual) {
        this.view = view;
        this.name = name;
        this.parent = parent;
        this.multiplatform = multiplatform;
        this.virtual = virtual;
        children = X_Jdk.setLinked();
        modules = new DefaultNamedDomainObjectSet<>(SchemaModule.class, view.getInstantiator(), view.getDecorator());
        platforms = new DefaultNamedDomainObjectSet<>(SchemaPlatform.class, view.getInstantiator(), view.getDecorator());
        dependencies = X_Jdk.multiListOrderedInsertion();
    }

    public String getName() {
        return name;
    }

    public String getPublishedName() {
        return name; // for now just return name; we'll wire this up properly when we bother to make it configurable
    }

    public SetLike<SchemaProject> getChildren() {
        return children;
    }

    public boolean isMultiplatform() {
        return multiplatform;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void addModule(SchemaModule module) {
        if (modules.contains(module)) {
            final SchemaModule existing = modules.getByName(module.getName());
            modules.remove(existing);
            modules.add(existing.update(module));
        } else {
            modules.add(module);
        }
        for (SchemaProject child : children) {
            child.addModule(module);
        }
    }

    public void addPlatform(SchemaPlatform platform) {
        if (platforms.contains(platform)) {
            final SchemaPlatform existing = platforms.getByName(platform.getName());
            platforms.remove(platform);
            platforms.add(existing.update(platform));
        } else {
            platforms.add(platform);
        }
        for (SchemaProject child : children) {
            child.addPlatform(platform);
        }
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
        return "SchemaProject{" +
            "path='" + getPath() + '\'' +
            ", multiplatform=" + multiplatform +
            ", virtual=" + virtual +
            ", children=[" + children.map(SchemaProject::getName).join(" , ") + "]" +
            '}';
    }

    public MappedIterable<? extends SchemaModule> getAllModules() {
        final SetLike<SchemaModule> mods = X_Jdk.setLinked();
        if (parent != null) {
            mods.addNow(parent.getAllModules());
        }
        mods.removeAllEquality(X_Jdk.toSet(modules));
        mods.addNow(modules);
        return mods;
    }

    public MappedIterable<? extends SchemaPlatform> getAllPlatforms() {
        final SetLike<SchemaPlatform> mods = X_Jdk.setLinked();
        if (parent != null) {
            mods.addNow(parent.getAllPlatforms());
        }
        mods.removeAllEquality(X_Jdk.toSet(platforms));
        mods.addNow(platforms);
        return mods;
    }

    public MinimalProjectView getView() {
        return view;
    }

    public void forAllPlatforms(In1<SchemaPlatform> callback) {
        platforms.all(callback::in);
    }

    public void forAllModules(In1<SchemaModule> callback) {
        modules.all(callback::in);
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
}
