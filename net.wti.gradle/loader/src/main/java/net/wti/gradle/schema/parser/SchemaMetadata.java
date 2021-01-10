package net.wti.gradle.schema.parser;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.JsonPairExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import net.wti.gradle.require.api.DependencyType;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.QualifiedModule;
import xapi.fu.data.ListLike;
import xapi.fu.data.MultiList;
import xapi.fu.java.X_Jdk;
import xapi.fu.log.Log;

import java.io.File;

import static xapi.gradle.fu.LazyString.lazyToString;

/**
 * Example schema.xapi (edited snapshot of xapi project's main schema.xapi:
 * <pre>
 * <xapi-schema
 *
 *     defaultRepoUrl = jcenter()
 *
 *     schemaLocation = "schema/schema.gradle"
 *
 *     platforms = [
 *         <main />,
 *         <jre replace = "main" published = true/>,
 *         <gwt replace = "main" published = true/>,
 *     ]
 *
 *     modules = [
 *         <api />,
 *         <spi />,
 *         <main include = [ api, "spi" ] />,
 *     ]
 *
 *     requires = {
 *         project: common
 *     }
 *
 *     projects = {
 *         // the projects below all have gwt, jre and other platforms
 *         multiPlatform: [
 *             "collections",
 *             "common",
 *             "model",
 *         ]
 *
 *         // the projects below all have a single "main" platform (potentially w/ multiple modules like api and testTools though!)
 *         standalone: [
 *             "util"
 *         ]
 *
 *         // the projects below are effectively parents of multiple child projects.
 *         // it will be left to the schema.xapi of these projects to determine whether
 *         // the child modules are multiPlatform, standalone, or nested multiProject
 *         multiProject: {
 *             dist: ["gwt", "jre"],
 *             samples: ["demo"]
 *         }
 *     }
 *
 *     // declare any external dependencies here,
 *     // so we can handle pre-emptively syncing jars (and maybe source checkouts) to a local cache,
 *     // then just reference these "blessed artifacts" w/out versions anywhere;
 *     // leaving it up to the system to decide what version to use.
 *     external = [
 *         // preload elements will be downloaded once, on build startup, into a local filesystem repository (xapi.repo)
 *         <preload
 *             name = "gwt"
 *             url = "https://wti.net/repo"
 *             version = "2.8.0"
 *             // limits these artifacts to gwt platform, where they will be auto-available as versionless dependencies
 *             // this inheritance is also given to any platform replacing gwt platform.
 *             platforms = [ "gwt" ]
 *             module = [ main ] // optionally limit modules
 *             artifacts = {
 *                 "com.google.gwt" : [
 *                     "gwt-user",
 *                     "gwt-dev",
 *                     "gwt-codeserver",
 *                 ]
 *             }
 *         /preload>
 *         ,
 *     ]
 *
 * /xapi-schema>
 * </pre>
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 29/07/19 @ 5:09 AM.
 */
public class SchemaMetadata {
    public static final String EXT_NAME = "_xapi_schema";
    /**
     * The location where a backing schema.xapi file exists.
     * May or may not exist.
     */
    private final File schemaFile;

    /**
     * True if this schema metadata is backed by a user configuration file.
     * If one schema references a path where a schema.xapi does not exist,
     * we'll still create a configuration object which can react to parental customization.
     *
     * explicit == true == schemaFile.exists()
     * explicit == false == !schemaFile.exists()
     */
    private final boolean explicit;
    private final SchemaMetadata parent;

    private String defaultUrl;
    private String schemaLocation;
    private String name;
    private String group;
    private String version;
    private ListLike<UiContainerExpr> platforms, modules, external, projects;
    private MultiList<PlatformModule, Expression> depsProject, depsInternal, depsExternal;

    public SchemaMetadata(SchemaMetadata parent, File schemaFile) {
        this.parent = parent;
        this.schemaFile = schemaFile;
        this.explicit = schemaFile.exists();
        depsProject = X_Jdk.multiListOrderedInsertion();
        depsInternal = X_Jdk.multiListOrderedInsertion();
        depsExternal = X_Jdk.multiListOrderedInsertion();
        group = version = QualifiedModule.UNKNOWN_VALUE;
    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public SchemaMetadata setDefaultUrl(String defaultUrl) {
        this.defaultUrl = defaultUrl;
        return this;
    }

    public String getSchemaLocation() {
        if (schemaLocation == null) {
            final File parent = schemaFile.isFile() ? schemaFile.getParentFile() : schemaFile;
            return parent.getPath() + "/schema.gradle";
        }
        return schemaLocation;
    }

    public SchemaMetadata setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
        return this;
    }

    public ListLike<UiContainerExpr> getProjects() {
        return projects;
    }

    public SchemaMetadata setProjects(ListLike<UiContainerExpr> projects) {
        this.projects = projects;
        return this;
    }

    public ListLike<UiContainerExpr> getPlatforms() {
        return platforms;
    }

    public SchemaMetadata setPlatforms(ListLike<UiContainerExpr> platforms) {
        this.platforms = platforms;
        return this;
    }

    public ListLike<UiContainerExpr> getModules() {
        return modules;
    }

    public SchemaMetadata setModules(ListLike<UiContainerExpr> modules) {
        this.modules = modules;
        return this;
    }

    public ListLike<UiContainerExpr> getExternal() {
        return external;
    }

    public SchemaMetadata setExternal(ListLike<UiContainerExpr> external) {
        this.external = external;
        return this;
    }

    public MultiList<PlatformModule, Expression> getDepsProject() {
        return depsProject;
    }

    public MultiList<PlatformModule, Expression> getDepsInternal() {
        return depsInternal;
    }

    public MultiList<PlatformModule, Expression> getDepsExternal() {
        return depsExternal;
    }

    public String getGroup() {
        return group;
    }

    public SchemaMetadata setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public SchemaMetadata setVersion(String version) {
        this.version = version;
        return this;
    }

    public void addPlatform(UiContainerExpr el) {
        if (platforms == null) {
            platforms = X_Jdk.listArrayConcurrent();
        }
        platforms.add(el);
    }

    public void addModule(String moduleName, UiContainerExpr el) {
        if (modules == null) {
            modules = X_Jdk.listArrayConcurrent();
        }
        modules.add(el);
    }

    public void addExternal(UiContainerExpr el) {
        if (external == null) {
            external = X_Jdk.listArrayConcurrent();
        }
        external.add(el);
    }

    public void reducePlatformTo(String explicitPlatform) {
        // TODO: tentatively disable certain platforms
    }

    public File getSchemaFile() {
        return schemaFile;
    }

    public File getSchemaDir() {
        return schemaFile.isFile() ? schemaFile.getAbsoluteFile().getParentFile() : schemaFile.isDirectory() ? schemaFile : new File(".").getAbsoluteFile();
    }

    public boolean isExplicit() {
        return explicit;
    }

    public void addProject(Expression expr) {

    }

    public boolean isMultiPlatform() {
        // LATER: take into account parent schemas + platform disablement properties
        return platforms != null && platforms.isNotEmpty();
    }

    @Override
    public String toString() {
        return "SchemaMetadata{" +
            "path=" + getPath() +
            ", parent=" + (parent == null ? "''" : parent) +
            ", schemaFile=" + schemaFile +
            ", explicit=" + explicit +
            ", defaultUrl='" + defaultUrl + '\'' +
            ", schemaLocation='" + schemaLocation + '\'' +
            '}';
    }

    public String getName() {
        return name == null ? (name = getSchemaDir().getName()) : name;
    }

    public boolean hasName() {
        return this.name != null;
    }

    public void setName(String name) {
        if (this.name != null && !this.name.equals(name)) {
            throw new IllegalArgumentException("Cannot change " + this.name + " to " + name);
        }
        this.name = name;
    }

    public void addDependency(DependencyType type, PlatformModule into, JsonPairExpr from) {
        Log.firstLog(into, this).log(SchemaMetadata.class,
            lazyToString(this::getPath), "adding dependency ", type, " into ", into + " : " + from);
        // hm... we should just be storing ast for the requires= of a top-level xapi-schema.
        // all the other requires= elements will also be stored in other ast nodes, and visited as appropriate by SchemaParser.
        switch (type) {
            case unknown:
            case project:
                // hm... we need to actually be mutating a dependency graph.
                // for now, we'll create a SchemaDependency object, to simply describe the source request;
                // we'll handle turning it into a real dependency graph later.
                (depsProject == null ? (depsProject = X_Jdk.multiListOrderedInsertion()) : depsProject)
                    .get(into).add(from);
                break;
            case internal:
                // internal will be
                (depsInternal == null ? (depsInternal = X_Jdk.multiListOrderedInsertion()) : depsInternal)
                    .get(into).add(from);
                break;
            case external:
                (depsExternal == null ? (depsExternal = X_Jdk.multiListOrderedInsertion()) : depsExternal)
                    .get(into).add(from);
                break;
            default:
                throw new UnsupportedOperationException("Dependency type " + type + " not (yet?) supported");
        }
    }

    public final SchemaMetadata getParent() {
        return parent;
    }

    public void addDependency(JsonPairExpr pair, PlatformModule platMod) {
        if (pair.getKeyExpr() instanceof IntegerLiteralExpr) {
            // requires is in "array form":
            // requires = [ 'a', 'b' ]
            // which implies, by default, requires = { project: [ 'a', 'b' ] }
            // TODO: add optional structure if transitivity / other values are desired.  requires = [ { name: 'a', transitivity: 'api' }, ... ]
            addDependency(DependencyType.project, platMod, pair);
        } else {
            String type = pair.getKeyString();
            DependencyType t = DependencyType.unknown;
            String coord = null;
            for (DependencyType value : DependencyType.values()) {
                if (type.startsWith(value.name())){
                    if (value.name().equals(type)) {
                        t = value;
                        break;
                    }
                    coord = type.substring(value.name().length());
                    if (coord.startsWith("_")) {
                        coord = coord.substring(1);
                    } else if (Character.isUpperCase(coord.charAt(0))) {
                        coord = Character.toLowerCase(coord.charAt(0)) +
                            (coord.length() == 1 ? "" : coord.substring(1));
                    }
                    break;
                }
            }
            addDependency(t, platMod.edit(null, coord), pair);
        }
    }

    public String getPath() {
        String me = getName();
        if (parent == null) {
            if (":".equals(me)) {
                return ":";
            }
            return ":" + me;
        }
        String parentPath = parent.getPath();
        return parentPath + (parentPath.endsWith(":") ? "" : ":") + me;
    }
}
