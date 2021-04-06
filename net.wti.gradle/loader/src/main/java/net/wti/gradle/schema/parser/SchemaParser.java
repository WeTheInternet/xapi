package net.wti.gradle.schema.parser;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.require.api.DependencyKey;
import net.wti.gradle.require.api.DependencyMap;
import net.wti.gradle.require.api.DependencyType;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.*;
import net.wti.gradle.schema.impl.DefaultSchemaPlatform;
import net.wti.gradle.schema.map.*;
import net.wti.gradle.schema.spi.SchemaProperties;
import net.wti.gradle.system.service.GradleService;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import xapi.fu.*;
import xapi.fu.data.ListLike;
import xapi.fu.data.MultiList;
import xapi.fu.data.SetLike;
import xapi.fu.itr.*;
import xapi.fu.java.X_Jdk;
import xapi.fu.log.Log;
import xapi.fu.log.Log.LogLevel;
import xapi.gradle.fu.LazyString;
import xapi.util.X_String;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

import static com.github.javaparser.ast.visitor.ComposableXapiVisitor.whenMissingFail;

/**
 * Converts schema.xapi files into {@link DefaultSchemaMetadata} classes.
 *
 * Not really a parser as much as it is a visitor of parsed xapi,
 * which fills in an instance of SchemaMetadata from said file.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 29/07/19 @ 5:09 AM.
 */
public interface SchemaParser {

    MinimalProjectView getView();

    default DefaultSchemaMetadata getSchema() {
        return GradleService.buildOnce(getView().getRootProject(), DefaultSchemaMetadata.EXT_NAME, this::parseSchema);
    }

    /**
     * Find the schema.xapi file for a given project, and parse it.
     *
     * @param p - A MinimalProjectView, to be able to look up properties and locate a project directory.
     * @return A parsed {@link DefaultSchemaMetadata} file.
     */
    default DefaultSchemaMetadata parseSchema(MinimalProjectView p) {
        String schemaFile = GradleCoerce.unwrapStringOr(p.findProperty("xapiSchema"), p.getProjectDir() + File.separator + "schema.xapi");
        File f = new File(schemaFile);
        final DefaultSchemaMetadata metadata = new DefaultSchemaMetadata(null, f);
        if (f.exists()) {
            return parseSchemaFile(null, metadata, p.getProjectDir());
        }
        return metadata;
    }

    /**
     * Parse a supplied "schema.xapi" file.
     *
     * @return A parsed {@link DefaultSchemaMetadata} file.
     * @param parent Optional parent of the target metadata.
     * @param metadata A schema metadata instance for us to fill out.
     * @param relativeRoot In case the schemaFile is relative, where to try resolving it.
     * @return A parsed {@link DefaultSchemaMetadata} file.
     *
     * We will throw illegal argument exceptions if you provide a non-existent absolute-path-to-a-schema-file.
     * Relative paths will be more forgiving.
     */
    default DefaultSchemaMetadata parseSchemaFile(
        DefaultSchemaMetadata parent,
        DefaultSchemaMetadata metadata,
        File relativeRoot
    ) {
        File schemaFile = normalizeSchemaFile(relativeRoot, metadata.getSchemaFile());
        if (!schemaFile.exists()) {
            return new DefaultSchemaMetadata(parent, schemaFile);
        }
        final UiContainerExpr expr;
        try (
            FileInputStream fio = new FileInputStream(schemaFile)
        ) {
            expr = JavaParser.parseUiContainerMergeMany(schemaFile.getAbsolutePath(), fio, LogLevel.INFO);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to load file://" + schemaFile.getAbsolutePath(), e);
        } catch (ParseException e) {
            throw new GradleException("Invalid xapi source in file://" + schemaFile.getAbsolutePath(), e);
        }

        // hm, we're getting duplication of root schema as :name and :name:schema.xapi...
        // TODO: find out if above comment is even true, and do we even care?

        final DefaultSchemaMetadata result = parseSchemaElement(parent, metadata, expr);
        result.setSchemaLocation(schemaFile.getPath());
        return result;
    }

    default File normalizeSchemaFile(File relativeRoot, File schemaFile) {
        if (!schemaFile.exists()) {
            if (schemaFile.isAbsolute()) {
                throw new IllegalArgumentException("Non-existent absolute-path xapiSchema file " + schemaFile);
            }
            schemaFile = new File(relativeRoot, schemaFile.getPath());
        }
        if (schemaFile.isDirectory()) {
            schemaFile = new File(schemaFile, "schema.xapi");
        }
        return schemaFile;
    }

    default DefaultSchemaMetadata parseSchemaElement(
        DefaultSchemaMetadata parent,
        final DefaultSchemaMetadata metadata,
        UiContainerExpr schema
    ) {
        File schemaFile = metadata.getSchemaFile();
        ComposableXapiVisitor.<DefaultSchemaMetadata>whenMissingFail(SchemaParser.class)
            .withUiContainerRecurse(In2.ignoreAll())
            .withNameExpr((ctr, meta)-> {
                if (!ctr.getName().equals("xapi-schema")) {
                    throw new IllegalArgumentException("Bad schema file, " + (schemaFile == null ? "<virtual>" : schemaFile.getAbsolutePath()) + "; " +
                        "does not start with <xapi-schema; instead, starts with: " + ctr.getName() + ":\n" + ctr.toSource());
                }
                return false;
            })
            .withUiAttrTerminal(readProjectAttributes())
            .visit(schema, metadata);
        schema.addExtra("xapi-schema-meta", metadata);
        return metadata;
    }

    default DefaultSchemaMetadata parseProjectElement(
        DefaultSchemaMetadata parent,
        final DefaultSchemaMetadata metadata,
        UiContainerExpr schema
    ) {
        ComposableXapiVisitor.<DefaultSchemaMetadata>whenMissingFail(SchemaParser.class)
            .withUiContainerRecurse(In2.ignoreAll())
            .withNameTerminal((ctr, meta)-> {
                // the only name we visit is the <opening-element attrName=not-visited /opening-element>
                metadata.setName(ctr.getName());
            })
            .withUiAttrTerminal(readProjectAttributes())
            .visit(schema, metadata);
        schema.addExtra("xapi-schema-meta", metadata);
        return metadata;
    }

    default In2<UiAttrExpr, DefaultSchemaMetadata> readProjectAttributes() {
        return (attr, meta)->{
            switch (attr.getNameString()) {
                case "multiplatform":
                case "virtual":
                    // ignored, only apply to xapi-schema. Should likely be a static set checked in `default:`
                    break;
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
                case "name":
                    // the name of the root project, for use it paths
                    meta.setName(attr.getStringExpression(false, true));
                    break;
                case "group":
                    // the group of the project, for use in publishing / dependency references
                    meta.setGroup(attr.getStringExpression(false, true));
                    break;
                case "version":
                    // the group of the project, for use in publishing / dependency references
                    meta.setVersion(attr.getStringExpression(false, true));
                    break;
                case "require":
                case "requires":
                    addRequires(meta, PlatformModule.UNKNOWN, attr.getExpression());
                    break;
                default:
                    throw new UnsupportedOperationException("Attributes named " + attr.getNameString() + " are not (yet) supported");
            }
        };
    }

    default void addRequires(final DefaultSchemaMetadata meta, final PlatformModule platMod, Expression expression) {

        ComposableXapiVisitor<SchemaParser> v = ComposableXapiVisitor.onMissingFail(SchemaParser.class);
        v
            .withJsonContainerRecurse(In2.ignoreAll())
            .withJsonPairTerminal((pair, me)->{
                insertDependencyRaw(meta, pair, platMod);
            })
        ;
        expression.accept(v, this);
        /*
    requires = {
        project: projName
        internal: [buildName,] projName [,'platName'?:'main'] [,'modName'?:'main']
        external: gId:aId:v:classifier [,'platName'?:'main'] [,'modName'?:'main']
    }
        */
    }

    default void insertDependencyRaw(DefaultSchemaMetadata meta, JsonPairExpr pair, PlatformModule platMod) {
        meta.addDependency(pair, platMod);
    }

    default void addPlatforms(DefaultSchemaMetadata meta, Expression expression) {
        /*
    platforms = [
        <main />,
        <jre replace = "main" />,
        <gwt replace = "main" />,
    ]
        */
        final ComposableXapiVisitor<?> visitor = whenMissingFail(SchemaParser.class)
            .withUiContainerTerminal((el, val) -> {
                meta.addPlatform(el);
            })
            .nameOrString(platName ->
                meta.addPlatform(new UiContainerExpr(platName))
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
                                meta.addPlatform(expr);
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

    default void addModules(DefaultSchemaMetadata meta, Expression expression) {
        addModules(meta, expression, In1.ignored());
    }
    default void addModules(DefaultSchemaMetadata meta, Expression expression, In1<UiContainerExpr> massager) {
        /*
    modules can be a single big list of all elements:
    modules = [
        <main require = [ api, spi ] />,
        <sample require = "main" published = true />,
        // purposely mixing strings and name references, to ensure visitor is robust
        <testTools require = main published = true />,
        <test require = ["sample", testTools ] />,
    ]

    or simply, the default:
    modules = main

    as well as simply listing many modules=attributes:
    <xapi-schema
    modules = <main require = [api, spi ] />
    modules = <api />
    modules = spi
    modules = [ <test require = testFixtures />, <testFixtures require = main /> ]
    /xapi-schema>

    TODO: conditional modules, perhaps w/ generator-time macros to statically analyze methods like fileExists("path"):
    modules = fileExists("src/api") ?  [ <api ... />, <main requires=api /> ] : <main />
    // just got to make sure that re-declaring a module is additive, rather than destructive
        */
        final ComposableXapiVisitor<?> visitor = whenMissingFail(SchemaParser.class, ()->"Expected <elements/> or \"strings\" or names")
            .withUiContainerTerminal((el, val) -> {
                String modName = el.getName();
                massager.in(el);
                meta.addModule(modName, el);
            })
            .nameOrString(modName -> {
                final UiContainerExpr el = new UiContainerExpr(modName);
                massager.in(el);
                meta.addModule(modName, el);
            });
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
                whenMissingFail(SchemaParser.class, ()->"At least one attributes=required in json container " + expression.toSource())
                    .withUiAttrTerminal((attr, val) -> {
                        String platName = attr.getNameString();
                        attr.accept(
                            whenMissingFail(SchemaParser.class, ()->"Attribute " + platName + " may only =\"string\" or =name")
                                .nameOrString(require -> {
                                    final UiContainerExpr expr = new UiContainerExpr(platName);
                                    if (!require.isEmpty()) {
                                        expr.addAttribute("requires", NameExpr.of(require));
                                    }
                                    massager.in(expr);
                                    meta.addModule(platName, expr);
                                })
                            , val);
                    })
                    .visit(json, null);
            }
        } else if (expression instanceof UiContainerExpr) {
            expression.accept(visitor, null);
        } else {
            throw new GradleException("Invalid modules expression " + expression.getClass() +" :\n" + expression.toSource());
        }

    }
    default void addProjects(DefaultSchemaMetadata meta, Expression expression) {
        final ComposableXapiVisitor<DefaultSchemaMetadata> v = ComposableXapiVisitor.whenMissingIgnore(SchemaParser.class);
        boolean[] multiplatform = {meta.isMultiPlatform()}; // default to multi-platform if this schema is multiplatform
        boolean[] virtual = {false};

        final In2<UiContainerExpr, DefaultSchemaMetadata> addProject = (module, parent)-> {
            final ListLike<UiContainerExpr> projects;
            if (meta.getProjects() == null) {
                projects = X_Jdk.listArrayConcurrent();
                meta.setProjects(projects);
            } else {
                projects = meta.getProjects();
            }
            if (!module.hasAttribute("multiplatform")) {
                module.addAttribute("multiplatform", BooleanLiteralExpr.boolLiteral(multiplatform[0]));
            }
            if (!module.hasAttribute("virtual")) {
                module.addAttribute("virtual", BooleanLiteralExpr.boolLiteral(virtual[0]));
            }
            projects.add(module);
        };

        v   .withJsonContainerRecurse(In2.ignoreAll())
            .withJsonPairTerminal((type, meta1) -> {
                if (type.getKeyExpr() instanceof IntegerLiteralExpr) {
                    type.getValueExpr().accept(
                        ComposableXapiVisitor.<DefaultSchemaMetadata>whenMissingFail(SchemaParser.class)
                            .withUiContainerTerminal(addProject)
                            .withNameOrString((name, meta2) -> {
                                // we have a project to add...
                                UiContainerExpr newProject = new UiContainerExpr(name);
                                addProject.in(newProject, meta2);
                            })
                        , meta1);
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
        expression.accept(v, meta);

    }

    default void addExternal(DefaultSchemaMetadata meta, Expression expression) {
        ComposableXapiVisitor<DefaultSchemaMetadata> v = whenMissingFail(SchemaParser.class);
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

    default void addDefaultRepoUrl(DefaultSchemaMetadata meta, Expression expression) {
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

    default void addSchemaLocation(DefaultSchemaMetadata meta, Expression expression) {
        // define where to produce a generated schema.  default is $meta.schemaFile.parentDir/schema.gradle
        expression.accept(
            whenMissingFail(SchemaParser.class)
                .nameOrString(meta::setSchemaLocation)
            , null);
    }

    default void loadMetadata(SchemaMap map, SchemaProject project, DefaultSchemaMetadata metadata, final SchemaParser parser) {
        loadPlatforms(project, metadata, parser);
        loadModules(project, metadata, parser);
        loadExternals(project, metadata);
        final ListLike<UiContainerExpr> projects = metadata.getProjects();
        if (projects != null) {
            map.loadProjects(project, this, metadata);
        }
        loadDependencies(map, project, metadata);
    }

    default void loadDependencies(SchemaMap map, SchemaProject project, DefaultSchemaMetadata metadata) {

        DependencyMap<CharSequence> depMap = new DependencyMap<>();
        depMap.put(DependencyKey.group, LazyString.nullableString(metadata::getGroup));
        depMap.put(DependencyKey.version, LazyString.nullableString(metadata::getVersion));

        for (DependencyType type : DependencyType.values()) {
            final MultiList<PlatformModule, Expression> deps;
            switch (type) {
                case unknown:
                    // TODO: allow unknowns to search multiple locations?
                    continue;
                case internal:
                    deps = metadata.getDepsInternal();
                    break;
                case project:
                    deps = metadata.getDepsProject();
                    break;
                case external:
                    deps = metadata.getDepsExternal();
                    break;
                default:
                    throw new UnsupportedOperationException("DependencyType " + type + " not supported yet");
            }
            if (deps == null || deps.isEmpty()) {
                continue;
            }

            Log.tryLog(SchemaParser.class, this,
                project.getPath(), " adding ", type, " dependencies: ", deps.map(o->o.join("->").replace("\n", " ")));

            for (Out2<PlatformModule, ListLike<Expression>> entry : deps.forEachItem()) {
                final PlatformModule mod = entry.out1();
                // TODO: also account for structured dependencies w/ name, transitivity, etc.

                DependencyMap<CharSequence> localCopy = new DependencyMap<>(depMap);
                localCopy.put(DependencyKey.platformModule, mod);
                localCopy.put(DependencyKey.category, type);
                SizedIterable<SchemaDependency> extracted = entry.out2().map(expr ->
                    extractDependencies(localCopy, expr)
                ).flatten(In1Out1.identity()).cached();
                Log.tryLog(SchemaParser.class, this, metadata.getPath(), " adding deps ", extracted, " to platform ", mod);
                for (SchemaDependency dep : extracted) {
                    insertDependencies(project, metadata, mod, dep);
                }
            }
            // do not insert new code here w/o considering the early continue; above
        }

    }

    default SizedIterable<String> extractName(Expression expr) {
        return ComposableXapiVisitor.onMissingFail(SchemaParser.class)
            // hm... we probably need to accept <elements /> as well as "names".
            .extractNames(expr, this);
    }

    default SizedIterable<SchemaDependency> extractDependencies(DependencyMap<CharSequence> deps, Expression expr) {
        ChainBuilder<SchemaDependency> dependencies = Chain.startChain();
        final ComposableXapiVisitor<SchemaParser> vis = ComposableXapiVisitor.onMissingFail(SchemaParser.class);
        expr.accept(vis
            .withJsonPairTerminal((pair, parser) -> {
                // we made it here... look for annotations to apply to deps map.
                List<AnnotationExpr> annos = pair.getAnnotations();
                absorbAnnotations(deps, annos);

                String type = pair.getKeyString();
                final CharSequence is;
                final DependencyKey depType;
                final MappedIterable<Expression> exprs;
                switch (type) {
                    case "internal":
                    case "project":
                    case "external":
                    case "unknown":
                        is = DependencyType.valueOf(type);
                        depType = DependencyKey.category;
                        exprs = SingletonIterator.singleItem(pair.getValueExpr());
                        deps.put(DependencyKey.name, pair.getKeyString());
                        break;
                    case "module":
                    case "platform":
                        throw new IllegalArgumentException(type + " is not yet supported in SchemaParser.extractDependencies()");
//                        break;
                    default:
                        // if there is a string, it may be a key to a known module name.
                        // in this case, we are applying the dependency in the value element to a particular resolved module name:
                        // i.e.: gwtApi : { project: { something : gwtMain } }
                        // will put :something:gwtMain on the classpath of our project:gwtApi configurations.
                        // arguable, we should maybe require `module: { gwtApi : { project: { actual : deps } } }`,
                        // but it's already too {}-y already.
                        // platform: { gwt : { module : { api : { project: { actual : deps } } } }
                        // really ugly on one line, but nice and declarative, at least.
                        final Expression value = pair.getValueExpr();
                        if (!(value instanceof JsonContainerExpr)) {
                            throw new IllegalArgumentException("platformModule-specific dependency " + type + " may only have json [array] or {map:children}. Debugging: " + expr.toSource());
                        }
                        JsonContainerExpr json = (JsonContainerExpr) value;
                        depType = DependencyKey.platformModule;
                        exprs = json.getValues();
                        is = pair.getKeyString();
                }
                final CharSequence was = deps.put(depType, is);
                try {
                    exprs.forAll(Expression::accept, vis, parser);
                } finally {
                    if (was == null) {
                        deps.remove(depType);
                    } else if (is != was) {
                        deps.put(depType, was);
                    }
                }
            })
            .withJsonContainerTerminal((container, parser) -> {
                // expect either a list of "use sane defaults" for optional values, or a map of specific values.
                if (container.isArray()) {
                    // just a list, add the items up!
                    container.getValues().forAll(e->e.accept(vis, parser));
                } else {
                    // expect a map from name to plat:mod
                    for (JsonPairExpr pair : container.getPairs()) {
                        String name = pair.getKeyString();
                        for (String value : whenMissingFail(SchemaParser.class)
                                .extractNames(pair.getValueExpr(), parser)) {
                            PlatformModule platMod = PlatformModule.parse(value);
                            CharSequence was = deps.put(DependencyKey.platformModule, platMod);
                            // delegate actually building a dependency and adding it to returned list
                            StringLiteralExpr.stringLiteral(name).accept(vis, parser);
                            if (was == null) {
                                deps.remove(DependencyKey.platformModule);
                            } else {
                                deps.put(DependencyKey.platformModule, was);
                            }
                        }

                    }
                }
            })
            .withNameOrString((name, parser) -> {
                DependencyMap<CharSequence> localCopy = new DependencyMap<>(deps);
                localCopy.put(DependencyKey.name, name);
                CharSequence typeStr = deps.get(DependencyKey.category);
                if (typeStr == null) {
                    typeStr = DependencyType.unknown;
                }
                DependencyType type = DependencyType.valueOf(typeStr.toString());

                CharSequence platModStr = deps.get(DependencyKey.platformModule);
                if (platModStr == null || platModStr.length() == 0) {
                    platModStr = PlatformModule.defaultPlatform;
                }

                PlatformModule platMod;
                if (platModStr instanceof PlatformModule) {
                    platMod = (PlatformModule) platModStr;
                } else {
                    platMod = PlatformModule.parse(platModStr);
                }

                CharSequence gId = deps.getOrDefault(DependencyKey.group, QualifiedModule.UNKNOWN_VALUE);
                CharSequence version = deps.getOrDefault(DependencyKey.version, QualifiedModule.UNKNOWN_VALUE);

                switch (type) {
                    case internal:
                        platMod = platMod.edit(null, name);
                        break;
                    case external:
                        // extract gId and version from name, reducing name to a simple(r) string
                        String[] bits = name.split(":");
                        switch (bits.length) {
                            case 3:
                                // g:n:v
                                version = bits[2];
                                // fallthrough
                            case 2:
                                // g:n
                                gId = bits[0];
                                name = bits[1];
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Invalid external dependencies, has " + bits.length + " colons, only 2 or 3 expected in " + name
                                        + "\nfrom expression:\n" + expr.toSource());
                        }
                        break;
                }
                // todo: extract corrected group/version for local projects as well
                SchemaDependency dep = new SchemaDependency(type, platMod, gId.toString(), version.toString(), name);
                dependencies.add(dep);

            }), null);
        return dependencies.counted();
    }

    default void absorbAnnotations(DependencyMap<CharSequence> deps, List<AnnotationExpr> annos) {
        if (annos == null) {
            return;
        }
        for (AnnotationExpr anno : annos) {
            for (MemberValuePair member : anno.getMembers()) {
                for (String value : whenMissingFail(SchemaParser.class)
                        .extractNames(member.getValue(), deps)) {
                    // despite the many loops above, this will, in practice, all be simple @Group("value") constructs.
                    final DependencyKey key;
                    try {
                        key = DependencyKey.valueOf(member.getName());
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                    switch (key) {
                        case platformModule:
                            deps.put(key, PlatformModule.parse(value));
                            break;
                        case category:
                            deps.put(key, DependencyType.valueOf(value));
                            break;
                        default:
                            deps.put(key, value);
                    }
                }
            }

        }

    }

    default void insertDependencies(
        SchemaProject project,
        DefaultSchemaMetadata metadata,
        PlatformModule mod,
        SchemaDependency dep
    ) {
        project.getDependencies().get(mod).add(dep);
    }

    default void loadModules(SchemaProject project, DefaultSchemaMetadata metadata, final SchemaParser parser) {
        final ListLike<UiContainerExpr> modules = metadata.getModules();
        if (modules == null) {
            return;
        }
        final ListLike<SchemaModule> added = X_Jdk.listArray();
        for (UiContainerExpr module : modules) {
            String name = module.getName();
            boolean published = module.getAttribute("published")
                .mapIfPresent( attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn("main".equals(name)); // TODO: have a previously-configured "default publishing" list of module names
            boolean test = module.getAttribute("test")
                .mapIfPresent( attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(false);
            final SetLike<String> include = X_Jdk.setLinked();

            final String publishNamePattern = parser.getProperties().getPublishNamePattern(project.getView(), name);

            In1<Expression> processRequire = requireExpr -> {
                requireExpr.accept(
                        whenMissingFail(SchemaParser.class)
                                .withJsonContainerRecurse(In2.ignoreAll())
                                .withJsonPairTerminal((pair, ctx) -> {
                                    PlatformModule[] platMod = { new PlatformModule("main", name) };
                                    String typeStr = pair.getKeyString();
                                    if ("platform".equals(typeStr)) {
                                        // when a platform dependency is defined, expect value to be a json container
                                        if (pair.getValueExpr() instanceof JsonContainerExpr) {
                                            JsonContainerExpr platformDeps = (JsonContainerExpr) pair.getValueExpr();
                                            whenMissingFail(SchemaParser.class)
                                                    .withJsonContainerRecurse(In2.ignoreAll())
                                                    .withJsonPairTerminal((platform, ignored) -> {
                                                        if (platform.getKeyExpr() instanceof IntegerLiteralExpr) {
                                                            throw new IllegalArgumentException("All platform : {} dependencies MUST be a map; you sent list item: " + platform.getKeyString() + " : " + platform.getValueExpr());
                                                        }
                                                        PlatformModule specificPlatform = platMod[0].edit(platform.getKeyString(), null);
                                                        if (platform.getValueExpr() instanceof JsonContainerExpr) {
                                                            platform.getValueExpr().accept(
                                                                    whenMissingFail(SchemaParser.class)
                                                                            .withJsonContainerRecurse(In2.ignoreAll())
                                                                            .withJsonPairTerminal((dep, ignore) -> {
                                                                                insertDependencyRaw(metadata, dep, specificPlatform);
                                                                            }), ctx);
                                                        } else {
                                                            throw new IllegalArgumentException("Children of a platform : {} dependencies must have maps for values!, you sent " + platform.getKeyString() + " : " + platform.getValueExpr());
                                                        }
                                                    }).visit(platformDeps, ctx);
                                        } else {
                                            throw new IllegalArgumentException("All platform : {} dependencies MUST use a map for a value, you sent: " + pair.getValueExpr());
                                        }
                                    } else {
                                        // if it's not a platform dependency, then the key better be of type DependencyType
//                                final DependencyType type;
//                                try {
//                                    type = DependencyType.valueOf(typeStr);
//                                } catch (IllegalArgumentException e) {
//                                    throw new IllegalArgumentException("Illegal key " + typeStr + " is not 'platform' or any DependencyType enum;" +
//                                            " associated value:\n" + pair.getValueExpr() + "\n" +
//                                            e.getMessage(), e.getCause());
//                                }
                                        // next, apply this dependency to all platforms...
                                        final Maybe<UiAttrExpr> forPlatform = module.getAttribute("forPlatform");
                                        if (forPlatform.isPresent()) {
                                            final ComposableXapiVisitor<Object> vis = whenMissingFail(SchemaParser.class);
                                            vis.nameOrString(platName -> {
                                                PlatformModule specificPlatform = platMod[0].edit(platName, null);
                                                insertDependencyRaw(metadata, pair, specificPlatform);
                                            });
                                            forPlatform.get().getExpression().accept(vis, null);
                                        } else {
                                            project.forAllPlatforms(perPlat -> {
                                                PlatformModule specificPlatform = platMod[0].edit(perPlat.getName(), null);
                                                insertDependencyRaw(metadata, pair, specificPlatform);
                                            });
                                        }

                                    }

                                    // example module-with-requires:
                                    //    modules = [
                                    //        <main
                                    //            requires = {
                                    //                // A top-level external / internal / project dependency applies to all platforms.
                                    //                // internal dependencies are other modules which, per platform, the current module (main in this example) depends on
                                    //                internal: [ "api", "impl", ], // so, jre8:main depends on jre8:api and jre8:impl.
                                    //                // For all platforms, module $moduleName depends on $perPlatform:$moduleName of the listed projects:
                                    //                project: [ "a", "b", "c" ],
                                    //                // A list of external dependencies assumes either a hit in schema index, or a plain, non-xapi dependency string
                                    //                external: [ "tld.ext:art:1.0" ],
                                    //                // Prefer maps for selecting specific platform:module coordinates.
                                    //                external: { "tld.ext:ifact" : "main:spi" },
                                    //                // for platform-specific dependencies, specify platform, then use a map structure:
                                    //                platform: {
                                    //                    jre8 : {
                                    //                        // external dependencies as arrays will select main:main, unless this g:n:v is present in schema index.
                                    //                        external : [ "tld.ext:something:1.0", "tld.ext:something-else:1.1", ],
                                    //                        // it's best to always use a map and just select what you want specifically.
                                    //                        external : {
                                    //                            "tld.ext:ifact:1.0" : "main:spi",
                                    //                        },
                                    //                        // project lists take the "current platform" (jre8 in this example) over listed project names.
                                    //                        project : [ "x", "y", "z", ],
                                    //                        // use a map to select specific platform / module combinations
                                    //                        project : {
                                    //                              a: "main:spi", b: "main", c: "jre:api", ],
                                    //                        },
                                    //                        // internal is only ever a list of project-local module names
                                    //                        internal : [ "main:api", "impl" ],  // impl == jre8:impl, if not :, assumes current platform, or main, if outside platform:{}
                                    //                    },
                                    //                    jre11: {
                                    //                          // experimental: use annotation to specify defaults.
                                    //                          @Version("2.0")
                                    //                          @Group("tld.ext")
                                    //                          external: [ "name-api", "name-spi", "etc", ],
                                    //                    }
                                    //                }
                                    //            }
                                    //        /main> ]

                                })
                        , metadata);
            };
            // annoying to support both require= and requires=, but it's easy to forget which is which...
            module.getAttribute("requires").mapIfPresent(UiAttrExpr::getExpression).readIfPresent(processRequire);
            module.getAttribute("require").mapIfPresent(UiAttrExpr::getExpression).readIfPresent(processRequire);

            SchemaModule schemaMod = new SchemaModule(name, publishNamePattern, include, published, test);

            project.forAllPlatforms(plat -> {
                // an include = [ list, "of", modules ] is an automatic internal dependency on said module.
                // we also put these into the SchemaModule.includes list, which is a "very transitive" part of the model.
                // an internal dependency can be scoped down to compile_only, but an includes is the "official" transitive view of model.
                final In1<UiAttrExpr> insertInclude = In4.in4(this::insertModuleIncludes)
                        .provide1(schemaMod)
                        .provide1(plat.getName())
                        .provide1(metadata)
                        .useAfterMe(attr-> {
                            attr.getExpression().accept(
                                whenMissingFail(SchemaParser.class).extractNames(include::add)
                                , metadata);

                        });
                module.getAttribute("include").readIfPresent(insertInclude);
                module.getAttribute("includes").readIfPresent(insertInclude);
            });

            final SchemaModule result = insertModule(project, metadata, schemaMod, module);
            added.add(result);
        }
        // make all required-by-published modules also published.
        final Set<String> once = new HashSet<>();
        for (SchemaModule module : added) {
            if (module.isPublished()) {
                publishChildren(project, module, once);
            }
        }

    }

    default void publishChildren(SchemaProject project, SchemaModule module, Set<String> once) {
        for (String include : module.getInclude()) {
            if (once.add(include)) {
                final SchemaModule toInclude = project.getModule(include);
                if (!toInclude.isPublished()) {
                    toInclude.updatePublished(true);
                    publishChildren(project, toInclude, once);
                }
            }
        }
    }

    default SchemaModule insertModule(SchemaProject project, DefaultSchemaMetadata metadata, SchemaModule module, UiContainerExpr source) {
        return project.addModule(module);
    }

    default void loadPlatforms(SchemaProject project, DefaultSchemaMetadata metadata, final SchemaParser parser) {
        final ListLike<UiContainerExpr> platforms = metadata.getPlatforms();
        if (platforms == null) {
            return;
        }
        final ListLike<SchemaPlatform> added = X_Jdk.listArray();
        final Logger LOG = Logging.getLogger(SchemaParser.class);
        for (UiContainerExpr platform : platforms) {
            String name = platform.getName();
            boolean published = platform.getAttribute("published")
                .mapIfPresent( attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn("main".equals(name));
            boolean test = platform.getAttribute("test")
                .mapIfPresent( attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(false);
            final Maybe<UiAttrExpr> replaceAttr = platform.getAttribute("replace");
            final SetLike<String> replace = X_Jdk.setLinked();
            if (replaceAttr.isPresent()) {
                final ComposableXapiVisitor<Object> addRequire = whenMissingFail(SchemaParser.class);
                addRequire.nameOrString(replace::add);
                replaceAttr.get().getExpression().accept(addRequire, metadata);
            }
            final String groupPattern = parser.getProperties().getPublishGroupPattern(project.getView(), name);

            assert replace.isEmpty() || replace.size() == 1 : "Cannot replace more than one other platform; " + name + " tries to replace " + replace;

            // hm, this should be get-or-create semantics, to allow re-declaring a platform in an additive manner.
            SchemaPlatform tailPlatform = new DefaultSchemaPlatform(name,
                    groupPattern,
                    replace.isEmpty() ? null : replace.first(),
                    published,
                    test);
            final SchemaPlatform result = insertPlatform(project, metadata, tailPlatform, platform);
            added.add(result);

            final In1<UiAttrExpr> insertModule = In4.in4(this::insertModuleRequires)
                    .provide1(name)
                    .provide1(metadata)
                    .provide2(project::forAllModules);

            platform.getAttribute("requires").readIfPresent(insertModule);
            platform.getAttribute("require").readIfPresent(insertModule);
            // allow each platform to configure individual modules as well.
            Maybe<UiAttrExpr> modules = platform.getAttribute("modules");
            if (modules.isPresent()) {
                // we need to stash something that says "this module configuration applies only to our scoped platform".
                ListLike<UiContainerExpr> extracted = extractModuleForPlatform(platform.getName(), metadata, modules.get());
            }
        }
        // now, go through all published platforms, and make sure all predecessors are also published.
        final Set<String> once = new HashSet<>();
        for (SchemaPlatform platform : added) {
            if (platform.isPublished()) {
                publishChildren(project, platform, once);
            }
        }
    }

    default void insertModuleIncludes(SchemaModule module, String platform, DefaultSchemaMetadata metadata, UiAttrExpr attr) {
        // easiest way to construct internal dependency
        // is to just build some ast.  We resolved down to a string of module name,
        // now, build:
        // requires = { internal : [ expr ] }
        // and send that to insertModule
        final UiAttrExpr copy = (UiAttrExpr) attr.clone();
        copy.setName(new NameExpr("requires"));

        JsonPairExpr synth = new JsonPairExpr("internal", copy.getExpression());
        JsonContainerExpr ctr = new JsonContainerExpr(false, Collections.singletonList(synth));
        copy.setExpression(ctr);
        insertModuleRequires(platform, metadata, copy, In1.receiver(module));

    }
    default void insertModuleRequires(String platform, DefaultSchemaMetadata metadata, UiAttrExpr attr, In1<In1<SchemaModule>> moduleSource) {
        Expression toRequire = attr.getExpression();
        Pointer<String> platValue = Pointer.pointerTo(platform);
        Pointer<String> modValue = Pointer.pointer();
        Out1<PlatformModule> keyBuilder = () -> new PlatformModule(platValue.out1(), modValue.out1());
        final In1<SchemaModule> insertModule = mod -> {
            modValue.in(mod.getName());
            ComposableXapiVisitor<Object> vis = whenMissingFail(SchemaParser.class, () -> "Illegal contents for a require=... attribute");
            toRequire.accept(vis
                            .withJsonContainerRecurse(In2.ignoreAll())
                            .withJsonPairTerminal((pair, ctx)->{
                                switch (pair.getKeyString()) {
                                    case "unknown":
                                    case "project":
                                        metadata.getDepsProject()
                                                .get(keyBuilder.out1())
                                                .add(pair.getValueExpr());
                                        break;
                                    case "internal":
                                        metadata.getDepsInternal()
                                                .get(keyBuilder.out1())
                                                .add(pair.getValueExpr());
                                        break;
                                    case "external":
                                        metadata.getDepsExternal()
                                                .get(keyBuilder.out1())
                                                .add(pair.getValueExpr());
                                        break;
                                    case "platform":
                                        // expect map-only children
                                        ComposableXapiVisitor<Object> descender = whenMissingFail(SchemaParser.class)
                                                .withJsonMapOnly((json, ignore) -> {
                                                    for (JsonPairExpr childPair : json.getPairs()) {
                                                        try (Do lease = platValue.borrow(childPair.getKeyString())) {
                                                            childPair.getValueExpr().accept(vis, null);
                                                        }
                                                    }
                                                    // do not recurse, we sent the children to main visitor (vis) already.
                                                    return false;
                                                });
                                        pair.getValueExpr().accept(descender, null);
                                        return;
                                    case "module":
                                        // platform / module is handled like: platform { gwt: { module : { api : [ ... ] } } }
                                        // expect map-only children
                                        descender = whenMissingFail(SchemaParser.class)
                                                .withJsonMapOnly((json, ignore) -> {
                                                    for (JsonPairExpr childPair : json.getPairs()) {
                                                        try (Do lease = modValue.borrow(childPair.getKeyString())) {
                                                            childPair.getValueExpr().accept(vis, null);
                                                        }
                                                    }
                                                    // do not recurse, we sent the children to main visitor (vis) already.
                                                    return false;
                                                });
                                        pair.getValueExpr().accept(descender, null);
                                        return;
                                    default:
                                        try {
                                            Integer.parseInt(pair.getKeyString());
                                            metadata.getDepsInternal()
                                                    .get(keyBuilder.out1())
                                                    .add(pair.getValueExpr());
                                        } catch (NumberFormatException failed) {
                                            throw new UnsupportedOperationException(pair.getKeyString() + " is not a valid requires = {} key");
                                        }
                                }
                            })
                    , null); // end toRequire.accept
        };
        moduleSource.in(insertModule);
    }


    default ListLike<UiContainerExpr> extractModuleForPlatform(final String platformName, final DefaultSchemaMetadata metadata, UiAttrExpr uiAttrExpr) {
        final ListLike<UiContainerExpr> list = X_Jdk.list();

        addModules(metadata, uiAttrExpr.getExpression(), mod->{
            mod.addAttribute("forPlatform", StringLiteralExpr.stringLiteral(platformName));
        });

        return list;
    }

    default SchemaProperties getProperties() {
        return SchemaProperties.getInstance();
    }

    default void publishChildren(SchemaProject project, SchemaPlatform platform, Set<String> once) {
        String include = platform.getReplace();
        if (X_String.isNotEmpty(include)) {
            if (once.add(include)) {
                final SchemaPlatform toInclude = project.getPlatform(include);
                if (!toInclude.isPublished()) {
                    toInclude.setPublished(true);
                    publishChildren(project, toInclude, once);
                }
            }
        }
    }

    default SchemaPlatform insertPlatform(SchemaProject project, DefaultSchemaMetadata metadata, SchemaPlatform platform, UiContainerExpr source) {
        return project.addPlatform(platform);
    }

    default void loadExternals(SchemaProject project, DefaultSchemaMetadata metadata) {
        final ListLike<UiContainerExpr> externals = metadata.getExternal();
        if (externals == null) {
            return;
        }
        for (UiContainerExpr external : externals) {
            switch (external.getName()) {
                case "preload":
                    // A preload element is special, we will prepare download-to-local-repo tasks for this dependency.
                    // TODO: actually handle preloads
                    continue;
            }

        }

    }

}
