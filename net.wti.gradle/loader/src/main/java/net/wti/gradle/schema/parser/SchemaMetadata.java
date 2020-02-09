package net.wti.gradle.schema.parser;

import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import xapi.fu.In2;
import xapi.fu.data.ListLike;
import xapi.fu.java.X_Jdk;

import java.io.File;

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
 *         <main require = [ api, "spi" ] />,
 *     ]
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

    private String defaultUrl;
    private String schemaLocation;
    private String name;
    private ListLike<UiContainerExpr> platforms, modules, external, projects;

    public SchemaMetadata(File schemaFile) {
        this.schemaFile = schemaFile;
        this.explicit = schemaFile.exists();
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

    public void addPlatform(String platName, UiContainerExpr el) {
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
        return schemaFile.isFile() ? schemaFile.getParentFile() == null ? new File(".") : schemaFile.getParentFile() : schemaFile;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public void addProject(Expression expr) {
        final ComposableXapiVisitor<SchemaMetadata> v = ComposableXapiVisitor.whenMissingIgnore(SchemaParser.class);
        boolean[] multiplatform = {isMultiPlatform()}; // default to multi-platform if this schema is multiplatform
        boolean[] virtual = {false};

        final In2<UiContainerExpr, SchemaMetadata> addProject = (module, parent)-> {
            final ListLike<UiContainerExpr> platform = getProjects() == null ? (projects = X_Jdk.listArrayConcurrent()) : getProjects();
            if (!module.hasAttribute("multiplatform")) {
                module.addAttribute("multiplatform", BooleanLiteralExpr.boolLiteral(multiplatform[0]));
            }
            if (!module.hasAttribute("virtual")) {
                module.addAttribute("virtual", BooleanLiteralExpr.boolLiteral(virtual[0]));
            }
            platform.add(module);
        };

        v   .withJsonContainerRecurse(In2.ignoreAll())
            .withJsonPairTerminal((type, meta) -> {
                if (type.getKeyExpr() instanceof IntegerLiteralExpr) {
                    type.getValueExpr().accept(
                    ComposableXapiVisitor.onMissingFail(SchemaMetadata.class)
                        .withNameOrString((name, m) -> {
                            // we have a project to add...
                            UiContainerExpr newProject = new UiContainerExpr(name);
                            addProject.in(newProject, m);
                        })
                        , meta);
                    return; // source is an array, just carry on.
                }
                String key = type.getKeyString().toLowerCase();
                switch (key) {
                    case "multiplatform":
                    case "multi":
                        // children are default-multi-platform.
                        multiplatform[0] = true;
                        virtual[0] = false;
                        break;
                    case "standalone":
                    case "single":
                    case "singleplatform":
                        multiplatform[0] = false;
                        virtual[0] = false;
                        break;
                    case "virtual":
                        multiplatform[0] = false;
                        virtual[0] = true;
                        break;
                    default:
                        throw new IllegalArgumentException("Cannot understand json key " + type.getKeyString() + " of:\n" + type.toSource());
                }
                type.getValueExpr().accept(v, meta);
            })
            .withUiContainerTerminal(addProject)
        // TODO allow converting json { list: ..., of: ..., elements: ...} into array [ <list />, <of />, <elements /> ] form.
        // This should probably be a method in ComposableXapiVistitor.
        ;
        expr.accept(v, this);

    }

    public boolean isMultiPlatform() {
        // LATER: take into account parent schemas + platform disablement properties
        return platforms != null && platforms.isNotEmpty();
    }

    @Override
    public String toString() {
        return "SchemaMetadata{" +
            "schemaFile=" + schemaFile +
            ", explicit=" + explicit +
            ", defaultUrl='" + defaultUrl + '\'' +
            ", schemaLocation='" + schemaLocation + '\'' +
            '}';
    }

    public String getName() {
        return name == null ? getSchemaDir().getName() : name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
