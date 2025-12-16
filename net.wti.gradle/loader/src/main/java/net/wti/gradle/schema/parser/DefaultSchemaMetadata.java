package net.wti.gradle.schema.parser;

import net.wti.lang.parser.ast.expr.Expression;
import net.wti.lang.parser.ast.expr.JsonPairExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.gradle.require.api.DependencyType;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.QualifiedModule;
import net.wti.gradle.schema.api.SchemaMetadata;
import xapi.fu.data.ListLike;
import xapi.fu.data.MultiList;
import xapi.fu.java.X_Jdk;
import xapi.fu.log.Log;
import xapi.gradle.fu.LazyString;

import java.io.File;

import static xapi.gradle.fu.LazyString.lazyToString;

/**
 * Example schema.xapi (edited snapshot of xapi project's main schema.xapi:
 * <pre>
 * <xapi-schema
 *
 *     defaultRepoUrl = mavenCentral()
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
 *         multiplatform: [
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
 *         multiproject: {
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
public class DefaultSchemaMetadata implements SchemaMetadata {
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
    private final DefaultSchemaMetadata parent;

    private String defaultUrl;
    private String schemaLocation;
    private String name;
    private String group;
    private String version;
    private ListLike<UiContainerExpr> platforms, modules, external, projects, repositories;
    private MultiList<PlatformModule, Expression> depsProject, depsInternal, depsExternal;
    private Boolean explicitMultiplatform;

    public DefaultSchemaMetadata(DefaultSchemaMetadata parent, File schemaFile) {
        this.parent = parent;
        this.schemaFile = schemaFile;
        this.explicit = schemaFile != null && schemaFile.exists();
        depsProject = X_Jdk.multiListOrderedInsertion();
        depsInternal = X_Jdk.multiListOrderedInsertion();
        depsExternal = X_Jdk.multiListOrderedInsertion();
        group = version = QualifiedModule.UNKNOWN_VALUE;
    }

    public String getDefaultUrl() {
        return defaultUrl;
    }

    public DefaultSchemaMetadata setDefaultUrl(String defaultUrl) {
        this.defaultUrl = defaultUrl;
        return this;
    }

    public String getSchemaLocation() {
        if (schemaLocation == null) {
            if (schemaFile == null) {
                if (!explicit) {
                    return "<virtual>";
                }
                throw new IllegalStateException("Must set either schemaFile or schemaLocation to non-null in " + this);
            }
            final File parent = schemaFile.isFile() ? schemaFile.getParentFile() : schemaFile;
            return parent.getPath() + "/schema.xapi";
        }
        return schemaLocation;
    }

    public DefaultSchemaMetadata setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
        return this;
    }

    public ListLike<UiContainerExpr> getProjects() {
        return projects;
    }

    public DefaultSchemaMetadata setProjects(ListLike<UiContainerExpr> projects) {
        this.projects = projects;
        return this;
    }

    public ListLike<UiContainerExpr> getPlatforms() {
        return platforms;
    }

    public DefaultSchemaMetadata setPlatforms(ListLike<UiContainerExpr> platforms) {
        this.platforms = platforms;
        return this;
    }

    public ListLike<UiContainerExpr> getModules() {
        return modules;
    }

    public DefaultSchemaMetadata setModules(ListLike<UiContainerExpr> modules) {
        this.modules = modules;
        return this;
    }

    public ListLike<UiContainerExpr> getExternal() {
        return external;
    }

    public DefaultSchemaMetadata setExternal(ListLike<UiContainerExpr> external) {
        this.external = external;
        return this;
    }

    public ListLike<UiContainerExpr> getRepositories() {
        return repositories;
    }

    public void setRepositories(final ListLike<UiContainerExpr> repositories) {
        this.repositories = repositories;
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

    public DefaultSchemaMetadata setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public DefaultSchemaMetadata setVersion(String version) {
        this.version = version;
        return this;
    }

    public void addPlatform(UiContainerExpr el) {
        if (platforms == null) {
            platforms = X_Jdk.listArrayConcurrent();
        }
        platforms.add(el);
    }

    public void addRepositories(UiContainerExpr el) {
        if (repositories == null) {
            repositories = X_Jdk.listArrayConcurrent();
        }
        repositories.add(el);
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
        return schemaFile != null && schemaFile.isFile() ?
               schemaFile.getAbsoluteFile().getParentFile() :
               schemaFile != null && schemaFile.isDirectory() ?
               schemaFile : new File(".").getAbsoluteFile();
    }

    public boolean isExplicit() {
        return explicit;
    }

    public void addProject(Expression expr) {

    }

    public boolean isMultiPlatform() {
        // LATER: take into account parent schemas + platform disablement properties (getAvailablePlatforms() would do it)
        if (Boolean.TRUE.equals(explicitMultiplatform)) {
            return true;
        }
        return platforms != null && platforms.isNotEmpty();
    }

    public void setExplicitMultiplatform(Boolean explicit) {
        this.explicitMultiplatform = explicit;
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

    @Override
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
        Log.firstLog(into, this).log(Log.LogLevel.TRACE, DefaultSchemaMetadata.class,
            lazyToString(this::getPath), "adding dependency ", type, " into ", into, " : ",
                        lazyToString(from::toSource).map(s->new LazyString(s.toString().replace("\n", " ")))
        );

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
                // internal will be references to other modules within a single project graph
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

    public final DefaultSchemaMetadata getParent() {
        return parent;
    }

    public final DefaultSchemaMetadata getRoot() {
        return parent == null ? this : parent.getRoot();
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
