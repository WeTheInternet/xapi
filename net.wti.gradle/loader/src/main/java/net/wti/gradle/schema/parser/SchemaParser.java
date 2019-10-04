package net.wti.gradle.schema.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import net.wti.gradle.internal.api.MinimalProjectView;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.GradleException;
import xapi.fu.In2;
import xapi.fu.log.Log.LogLevel;

import java.io.*;

import static com.github.javaparser.ast.visitor.ComposableXapiVisitor.onMissingFail;
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
     * TODO: take in a _much_ more limited subset of ProjectView,
     *  so we can reuse this code for a ProjectDesriptor as well.
     *
     * @param p
     * @return
     * @throws ParseException
     * @throws IOException
     */
    default SchemaMetadata parseSchema(MinimalProjectView p) {
        String schemaFile = GradleCoerce.unwrapStringOr(p.findProperty("xapiSchema"), "schema.xapi");
        File f = new File(schemaFile);
        if (!f.exists()) {
            if (f.isAbsolute()) {
                throw new IllegalArgumentException("Non-existent absolute-path xapiSchema file " + schemaFile);
            }
            f = new File(p.getProjectDir(), schemaFile);
        }
        if (!f.exists()) {
            return new SchemaMetadata();
        }
        final UiContainerExpr expr;
        try (
            FileInputStream fio = new FileInputStream(f)
        ) {
            expr = JavaParser.parseUiContainer(f.getAbsolutePath(), fio, LogLevel.INFO);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load file:" + f.getAbsolutePath(), e);
        } catch (ParseException e) {
            throw new GradleException("Invalid xapi source in file:" + f.getAbsolutePath(), e);
        }
        return parseSchema(expr);
    }

    default SchemaMetadata parseSchema(UiContainerExpr schema) {
        final SchemaMetadata metadata = new SchemaMetadata();
        ComposableXapiVisitor.<SchemaMetadata>whenMissingFail(SchemaParser.class)
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
                    .withUiAttrRecurse(In2.ignoreAll())
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
    }
    default void addProjects(SchemaMetadata meta, Expression expression) {
        /*
    modules = [
        <main require = [ api, spi ] />,
        <sample require = "main" published = true />,
        <testTools require = "main" published = true />,
        <test require = ["sample", testTools ] />, // purposely mixing strings and name references, to ensure visitor is robust
    ]
        */
    }

    default void addExternal(SchemaMetadata meta, Expression expression) {
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
            , null);
    }

    default void addSchemaLocation(SchemaMetadata meta, Expression expression) {
        // define where to produce a generated schema.  default is schema/build.gradle
        expression.accept(
            whenMissingFail(SchemaParser.class)
                .nameOrString(meta::setSchemaLocation)
            , null);
    }

}
