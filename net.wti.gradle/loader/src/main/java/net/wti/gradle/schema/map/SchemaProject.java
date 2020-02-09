package net.wti.gradle.schema.map;

import net.wti.gradle.schema.parser.SchemaMetadata;
import net.wti.gradle.schema.parser.SchemaParser;
import xapi.fu.data.SetLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.java.X_Jdk;

import java.io.File;

/**
 * Abstraction layer over a project descriptor, like <project-name virtual=true multiplatform=false />
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-06 @ 4:39 a.m..
 */
public class SchemaProject {
    private final String name;
    private final SetLike<SchemaProject> children;
    private final SetLike<SchemaModule> modules;
    private final SetLike<SchemaPlatform> platforms;
    private final boolean multiplatform, virtual;
    private final SchemaProject parent;

    public SchemaProject(String name, boolean multiplatform, boolean virtual) {
        this(null, name, multiplatform, virtual);
    }

    public SchemaProject(SchemaProject parent, String name, boolean multiplatform, boolean virtual) {
        this.name = name;
        this.parent = parent;
        this.multiplatform = multiplatform;
        this.virtual = virtual;
        children = X_Jdk.setLinked();
        modules = X_Jdk.setLinked();
        platforms = X_Jdk.setLinked();
    }

    public void addChild(
        SchemaMap map,
        SchemaParser parser,
        SchemaMetadata parent,
        SchemaProject child
    ) {
        for (SchemaModule module : modules) {
            child.addModule(module);
        }
        for (SchemaPlatform platform : platforms) {
            child.addPlatform(platform);
        }

        children.add(child);
        File childSchema = new File(parent.getSchemaDir(), child.name);
        if (!childSchema.exists()) {
            childSchema = new File(childSchema.getParentFile(), child.name + ".xapi");
        }
        if (!childSchema.exists()) {
            childSchema = new File(childSchema.getParentFile(), child.name + "/schema.xapi");
        }
        if (!childSchema.exists()) {
            // still nothing... give up and make a contrived relative root
            String relativeRoot = parent.getSchemaDir().getAbsolutePath().replaceFirst(
                map.getRootSchema().getSchemaDir().getAbsolutePath() + "[/\\\\]*", "");
            childSchema = relativeRoot.isEmpty() ? new File(child.name) : new File(relativeRoot, child.name);
        }
        final SchemaMetadata parsedChild = parser.parseSchemaFile(parent.getSchemaDir(), childSchema);
        map.loadChildren(parser, parsedChild);
    }

    public String getName() {
        return name;
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
            final SchemaModule existing = modules.removeAndReturn(module);
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
            final SchemaPlatform existing = platforms.removeAndReturn(platform);
            platforms.add(existing.update(platform));
        } else {
            platforms.add(platform);
        }
        for (SchemaProject child : children) {
            child.addPlatform(platform);
        }
    }

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
        mods.removeAllEquality(modules);
        mods.addNow(modules);
        return mods;
    }

    public MappedIterable<? extends SchemaPlatform> getAllPlatforms() {
        final SetLike<SchemaPlatform> mods = X_Jdk.setLinked();
        if (parent != null) {
            mods.addNow(parent.getAllPlatforms());
        }
        mods.removeAllEquality(platforms);
        mods.addNow(platforms);
        return mods;
    }
}
