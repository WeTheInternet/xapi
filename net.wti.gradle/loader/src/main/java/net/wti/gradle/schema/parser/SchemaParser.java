package net.wti.gradle.schema.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import net.wti.gradle.internal.api.MinimalProjectView;
import net.wti.gradle.schema.map.SchemaMap;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.GradleException;
import xapi.fu.In2;
import xapi.fu.data.ListLike;
import xapi.fu.log.Log.LogLevel;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import static com.github.javaparser.ast.visitor.ComposableXapiVisitor.whenMissingFail;

/**
 * Converts schema.xapi files into {@link SchemaMetadata} classes.
 *
 * Not really a parser as much as it is a visitor of parsed xapi,
 * which fills in an instance of SchemaMetadata from said file.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 29/07/19 @ 5:09 AM.
 */
public interface SchemaParser {

    MinimalProjectView getView();

    default SchemaMetadata getSchema() {
        return GradleService.buildOnce(getView(), SchemaMetadata.EXT_NAME, this::parseSchema);
    }

    /**
     * Find the schema.xapi file for a given project, and parse it.
     *
     * @param p - A MinimalProjectView, to be able to look up properties and locate a project directory.
     * @return A parsed {@link SchemaMetadata} file.
     */
    default SchemaMetadata parseSchema(MinimalProjectView p) {
        String schemaFile = GradleCoerce.unwrapStringOr(p.findProperty("xapiSchema"), "schema.xapi");
        File f = new File(schemaFile);
        return parseSchemaFile(p.getProjectDir(), f);
    }

    /**
     * Parse a supplied "schema.xapi" file.
     *
     * @return A parsed {@link SchemaMetadata} file.
     * @param relativeRoot In case the schemaFile is relative, where to try resolving it.
     * @param schemaFile A relative-or-absolute path to a schema.xapi file (can be named anything)
     * @return A parsed {@link SchemaMetadata} file.
     *
     * We will throw illegal argument exceptions if you provide a non-existent absolute-path-to-a-schema-file.
     * Relative paths will be more forgiving.
     */
    default SchemaMetadata parseSchemaFile(File relativeRoot, File schemaFile) {
        if (!schemaFile.exists()) {
            if (schemaFile.isAbsolute()) {
                throw new IllegalArgumentException("Non-existent absolute-path xapiSchema file " + schemaFile);
            }
            schemaFile = new File(relativeRoot, schemaFile.getPath());
        }
        if (schemaFile.isDirectory()) {
            schemaFile = new File(schemaFile, "schema.xapi");
        }
        if (!schemaFile.exists()) {
            return new SchemaMetadata(schemaFile);
        }
        final UiContainerExpr expr;
        try (
            FileInputStream fio = new FileInputStream(schemaFile)
        ) {
            expr = JavaParser.parseUiContainer(schemaFile.getAbsolutePath(), fio, LogLevel.INFO);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load file:" + schemaFile.getAbsolutePath(), e);
        } catch (ParseException e) {
            throw new GradleException("Invalid xapi source in file:" + schemaFile.getAbsolutePath(), e);
        }
        return parseSchemaElement(schemaFile, expr);
    }

    default SchemaMetadata parseSchemaElement(File schemaFile, UiContainerExpr schema) {
        final SchemaMetadata metadata = new SchemaMetadata(schemaFile);
        ComposableXapiVisitor.<SchemaMetadata>whenMissingFail(SchemaParser.class)
            .withUiContainerRecurse(In2.ignoreAll())
            .withNameExpr((ctr, meta)-> {
                if (!ctr.getName().equals("xapi-schema")) {
                    throw new IllegalArgumentException("Bad schema file, " + schemaFile.getAbsolutePath() + "; " +
                        "does not start with <xapi-schema; instead, starts with: " + ctr.getName() + ":\n" + ctr.toSource());
                }
                return false;
            })
            .withUiAttrTerminal((attr, meta)->{
                switch (attr.getNameString()) {
                    case "platforms":
                        // definition of platforms
                        addPlatforms(meta, attr.getExpression());
                        break;
                    case "modules":
                        // definition of modules per platform
                        addModules(meta, attr.getExpression());
                        break;
                    case "projects":
                        // definition of child projects
                        addProjects(meta, attr.getExpression());
                        break;
                    case "external":
                        // definition of external dependencies (move to settings.xapi?)
                        addExternal(meta, attr.getExpression());
                        break;
                    case "defaultRepoUrl":
                        // definition of the default repo url to use for external dependency resolution
                        addDefaultRepoUrl(meta, attr.getExpression());
                        break;
                    case "schemaLocation":
                        // location of a generated schema script. If it's a directory, a file named build.gradle will be created.
                        addSchemaLocation(meta, attr.getExpression());
                        break;
                    case "rootName":
                        // the name of the root project, for use it paths
                        meta.setName(attr.getStringExpression(false, true));
                        break;
                    default:
                        throw new UnsupportedOperationException("Attributes named " + attr.getNameString() + " are not (yet) supported");
                }
            })
            .visit(schema, metadata);
        return metadata;
    }

    default void addPlatforms(SchemaMetadata meta, Expression expression) {
        /*
    platforms = [
        <main />,
        <jre replace = "main" />,
        <gwt replace = "main" />,
    ]
        */
        final ComposableXapiVisitor<?> visitor = whenMissingFail(SchemaParser.class)
            .withUiContainerTerminal((el, val) -> {
                String platName = el.getName();
                meta.addPlatform(platName, el);
            })
            .nameOrString(platName ->
                meta.addPlatform(platName, new UiContainerExpr(platName))
            );
        if (expression instanceof JsonContainerExpr) {
            final JsonContainerExpr json = (JsonContainerExpr) expression;
            if (json.isArray()) {
                // an array of elements:
                // [ <main />, <gwt replace="main" />, <jre replace=main />, ... ]
                visitor
                    .withJsonArrayRecurse(In2.ignoreAll(), false)
                    .withJsonPairRecurse(In2.ignoreAll())
                    .withUiAttrRecurse(In2.ignoreAll())
                    .withIntegerLiteralTerminal(In2.ignoreAll())
                    .visit(json, null);
            } else {
                // a map from name to replace= values:
                // { main: "", gwt: "main", jre: "main", etc: "..." }
                whenMissingFail(SchemaParser.class)
                    .withUiAttrTerminal((attr, val) -> {
                        String platName = attr.getNameString();
                        attr.accept(
                            whenMissingFail(SchemaParser.class)
                            .nameOrString(replace -> {
                                final UiContainerExpr expr = new UiContainerExpr(platName);
                                if (!replace.isEmpty()) {
                                    expr.addAttribute("replace", NameExpr.of(replace));
                                }
                                meta.addPlatform(platName, expr);
                            })
                        , val);
                    })
                    .visit(json, null);
            }
        } else if (expression instanceof UiContainerExpr) {
            expression.accept(visitor, null);
        } else {
            throw new GradleException("Invalid platforms expression " + expression.getClass() +" :\n" + expression.toSource());
        }
    }

    default void addModules(SchemaMetadata meta, Expression expression) {
        /*
    modules = [
        <main require = [ api, spi ] />,
        <sample require = "main" published = true />,
        <testTools require = "main" published = true />,
        <test require = ["sample", testTools ] />, // purposely mixing strings and name references, to ensure visitor is robust
    ]
        */
        final ComposableXapiVisitor<?> visitor = whenMissingFail(SchemaParser.class)
            .withUiContainerTerminal((el, val) -> {
                String modName = el.getName();
                meta.addModule(modName, el);
            })
            .nameOrString(modName ->
                meta.addModule(modName, new UiContainerExpr(modName))
            );
        if (expression instanceof JsonContainerExpr) {
            final JsonContainerExpr json = (JsonContainerExpr) expression;
            if (json.isArray()) {
                // an array of elements:
                // [ <main />, <gwt require="main" />, <jre require=main />, ... ]
                visitor
                    .withJsonArrayRecurse(In2.ignoreAll(), false)
                    .withJsonPairRecurse(In2.ignoreAll())
                    .withUiAttrRecurse(In2.ignoreAll())
                    .withIntegerLiteralTerminal(In2.ignoreAll())
                    .visit(json, null);
            } else {
                // a map from name to require= values:
                // { main: "", gwt: "main", jre: "main", etc: "..." }
                whenMissingFail(SchemaParser.class)
                    .withUiAttrTerminal((attr, val) -> {
                        String platName = attr.getNameString();
                        attr.accept(
                            whenMissingFail(SchemaParser.class)
                                .nameOrString(require -> {
                                    final UiContainerExpr expr = new UiContainerExpr(platName);
                                    if (!require.isEmpty()) {
                                        expr.addAttribute("require", NameExpr.of(require));
                                    }
                                    meta.addModule(platName, expr);
                                })
                            , val);
                    })
                    .visit(json, null);
            }
        } else if (expression instanceof UiContainerExpr) {
            expression.accept(visitor, null);
        } else {
            throw new GradleException("Invalid platforms expression " + expression.getClass() +" :\n" + expression.toSource());
        }

    }
    default void addProjects(SchemaMetadata meta, Expression expression) {
        meta.addProject(expression);
    }

    default void addExternal(SchemaMetadata meta, Expression expression) {
        ComposableXapiVisitor<SchemaMetadata> v = ComposableXapiVisitor.whenMissingFail(SchemaParser.class);
        v  .withJsonArrayRecurse(In2.ignoreAll())
           .withUiContainerTerminal(meta::addExternal);
        expression.accept(v, meta);
        /*

    // declare any external dependencies here,
    // so we can handle pre-emptively syncing jars (and maybe source checkouts) to a local cache,
    // then just reference these "blessed artifacts" w/out versions anywhere;
    // leaving it up to the system
    external = [
        // preload elements will be downloaded once, on build startup, into a local filesystem repository (xapi.repo)
        <preload
            name = "gwt"
            url = "https://wti.net/repo" // optional if defaultRepoUrl is set.
            version = "2.8.0" // optional; required in artifacts children if not present
            platforms = [ "gwt" ] // optional; limit by platform
            modules = null // optional; limit by module
            inherited = true // optional; default is true: derivation of platform/modules inherit these artifacts
            artifacts = { // map of groupId : artifactId [ : version ] (version required if not set in preload tag).
                "com.google.gwt" : [ // if version is set, an array, otherwise, a json map
                    "gwt-user",
                    "gwt-dev",
                    "gwt-codeserver",
                ]
            }
        /preload>
        ,
        */
    }

    default void addDefaultRepoUrl(SchemaMetadata meta, Expression expression) {
        // add a string describing the default repo url
        expression.accept(
            whenMissingFail(SchemaParser.class)
                // consider supporting method calls and the like here too...
                .nameOrString(meta::setDefaultUrl)
                .withMethodCallExpr((expr, ignored) -> {
                    if (!expr.getArgs().isEmpty()) {
                        throw new IllegalArgumentException("defaultRepoUrl methods cannot take arguments! you sent " + expr.toSource());
                    }
                    return true;
                })
            , null);
    }

    default void addSchemaLocation(SchemaMetadata meta, Expression expression) {
        // define where to produce a generated schema.  default is $meta.schemaFile.parentDir/schema.gradle
        expression.accept(
            whenMissingFail(SchemaParser.class)
                .nameOrString(meta::setSchemaLocation)
            , null);
    }

    default void loadModules(SchemaMap map, SchemaMetadata metadata) {
        final ListLike<UiContainerExpr> platforms = metadata.getPlatforms();
        if (platforms != null) {
            map.loadPlatforms(metadata, platforms);
        }
        final ListLike<UiContainerExpr> modules = metadata.getModules();
        if (modules != null) {
            map.loadModules(metadata, modules);
        }
        final ListLike<UiContainerExpr> externals = metadata.getExternal();
        if (externals != null) {
            map.loadExternals(metadata, this, externals);
        }
        final ListLike<UiContainerExpr> projects = metadata.getProjects();
        if (projects != null) {
            map.loadProjects(metadata, this, projects);
        }
    }
}
