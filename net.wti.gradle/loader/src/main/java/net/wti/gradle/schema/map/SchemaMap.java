package net.wti.gradle.schema.map;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import net.wti.gradle.internal.api.ProjectView;
import net.wti.gradle.schema.parser.SchemaMetadata;
import net.wti.gradle.schema.parser.SchemaParser;
import net.wti.gradle.system.service.GradleService;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.fu.Maybe;
import xapi.fu.data.ListLike;
import xapi.fu.data.MapLike;
import xapi.fu.data.SetLike;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.fu.java.X_Jdk;

import java.io.File;

import static xapi.fu.java.X_Jdk.mapOrderedInsertion;
import static xapi.fu.java.X_Jdk.setLinked;

/**
* A complete mapping of a fully-parsed project tree.
*
* Starting at the root schema.xapi, we collect a complete graph of the root + all child {@link SchemaMetadata}.
* Whereas SchemaMetadata exposes internal AST objects, the SchemaMap translates those into a typesafe, object-oriented graph.
*
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-06 @ 3:45 a.m..
 */
public class SchemaMap {

    public static final String KEY_FOR_EXTENSIONS = "xapiSchemaMap";

    public static SchemaMap fromView(ProjectView view) {
        return GradleService.buildOnce(SchemaMap.class, view, KEY_FOR_EXTENSIONS, v->{
            final SchemaParser parser = ()->view;
            final SchemaMetadata rootMeta = parser.getSchema();
            //noinspection UnnecessaryLocalVariable (nice for debugging)
            SchemaMap map = new SchemaMap(parser, rootMeta);
            return map;
        });
    }

    private final SchemaMetadata rootSchema;
    private final SetLike<SchemaMetadata> allMetadata;
    private final MapLike<String, SchemaMap> children;
    private final SchemaModule rootModule;
    private SchemaProject rootProject;
    private SchemaProject tailProject;

    public SchemaMap(SchemaMetadata root) {
        this.rootSchema = root;
        children = mapOrderedInsertion();
        allMetadata = setLinked();
        String rootName = root.getName();
        rootModule = new SchemaModule(rootName, X_Jdk.setLinked(), false, false);
        tailProject = rootProject = new SchemaProject(rootName, root.isMultiPlatform(), !new File(root.getSchemaDir(), "src").exists());
    }
    public SchemaMap(SchemaParser parser, SchemaMetadata root) {
        this(root);
        loadChildren(parser, root);
    }

    public void addChild(String at, SchemaMap child) {
        if (allowChild(rootSchema, at, child)) {
            children.put(at, child);
        }
    }

    protected boolean allowChild(SchemaMetadata root, String at, SchemaMap child) {
        // TODO: allow platform limitation properties to prevent a child schema map from being remembered.
        return true;
    }

    public void loadChildren(SchemaParser parser, SchemaMetadata metadata) {
        allMetadata.add(metadata);
        if (metadata == rootSchema) {
            // The first one in!

        } else {
            // hm, we probably want to record more here, when adding child metadatas...
        }
        if (metadata.isExplicit()) {
            parser.loadModules(this, metadata);
        }
    }

    public void loadModules(
        SchemaMetadata from,
        ListLike<UiContainerExpr> modules
    ) {
        for (UiContainerExpr module : modules) {
            String name = module.getName();
            boolean published = module.getAttribute("published")
                .mapIfPresent( attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(false);
            boolean test = module.getAttribute("test")
                .mapIfPresent( attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(false);
            final SetLike<String> require = X_Jdk.setLinked();
            final Maybe<UiAttrExpr> requireAttr = module.getAttribute("require");
            if (requireAttr.isPresent()) {
                final ComposableXapiVisitor<Object> addRequire = ComposableXapiVisitor.whenMissingFail(SchemaParser.class);
                addRequire
                    .withJsonContainerRecurse(In2.ignoreAll())
                    .withJsonPairTerminal((json, meta) ->
                        json.getValueExpr().accept(addRequire,meta))
                    .nameOrString(require::add);
                requireAttr.get().getExpression().accept(addRequire, from);
            }

            final SchemaModule tailModule = new SchemaModule(name, require, published, test);
            tailProject.addModule(tailModule);
        }

    }

    public void loadPlatforms(
        SchemaMetadata from,
        ListLike<UiContainerExpr> platforms
    ) {
        for (UiContainerExpr platform : platforms) {
            String name = platform.getName();
            boolean published = platform.getAttribute("published")
                .mapIfPresent( attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(false);
            boolean test = platform.getAttribute("test")
                .mapIfPresent( attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(false);
            final Maybe<UiAttrExpr> replaceAttr = platform.getAttribute("replace");
            final SetLike<String> replace = X_Jdk.setLinked();
            if (replaceAttr.isPresent()) {
                final ComposableXapiVisitor<Object> addRequire = ComposableXapiVisitor.whenMissingFail(SchemaParser.class);
                addRequire.nameOrString(replace::add);
                replaceAttr.get().getExpression().accept(addRequire, from);
            }

            assert replace.isEmpty() || replace.size() == 1 : "Cannot replace more than one other platform; " + name + " tries to replace " + replace;

            final SchemaPlatform tailPlatform = new SchemaPlatform(name, replace.isEmpty() ? null : replace.first(), published, test);
            tailProject.addPlatform(tailPlatform);
        }

    }

    public void loadExternals(SchemaMetadata metadata, SchemaParser parser, ListLike<UiContainerExpr> externals) {

    }

    public void loadProjects(
        SchemaMetadata from,
        SchemaParser parser,
        ListLike<UiContainerExpr> projects
    ) {
        for (UiContainerExpr project : projects) {
            String name = project.getName();
            boolean multiplatform = project.getAttribute("multiplatform")
                .mapIfPresent( attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(from.isMultiPlatform());
            boolean virtual = project.getAttribute("virtual")
                .mapIfPresent( attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(false);

            final SchemaProject oldTail = tailProject;
            tailProject = new SchemaProject(oldTail, name, multiplatform, virtual);
            oldTail.addChild(this, parser, from, tailProject);
            tailProject = oldTail;
        }

    }

    public SchemaMetadata getRootSchema() {
        return rootSchema;
    }

    public SchemaProject getRootProject() {
        return rootProject;
    }

    @Override
    public String toString() {
        return "SchemaMap{" +
            "\nrootSchema=" + rootSchema +
            ",\nchildren=" + children +
            ",\nrootModule=" + rootModule +
            ",\nrootProject=" + rootProject +
            ",\ntailProject=" + tailProject +
            "\n} /*end SchemaMap*/";
    }

    public SetLike<SchemaProject> getAllProjects() {
        final SetLike<SchemaProject> all = X_Jdk.setLinked();
        all.add(this.rootProject);
        all.addNow(this.rootProject.getChildren());
        this.rootProject.getChildren().flatten(SchemaProject::getChildren).forAll(all::add);
        return all;
    }

    public SetLike<SchemaMetadata> getAllMetadata() {
        final SetLike<SchemaMetadata> all = X_Jdk.setLinked();
        all.addNow(allMetadata);
        return all;
    }

    public MappedIterable<SchemaPreload> getAllPreloads() {
        return allMetadata.mapRemoveNull(SchemaMetadata::getExternal).map(external -> {
            ChainBuilder<SchemaPreload> preloads = Chain.startChain();
            ComposableXapiVisitor<UiContainerExpr> v = ComposableXapiVisitor.whenMissingFail(SchemaMap.class);
            v
                .withJsonArrayRecurse(In2.ignoreAll())
                .withUiContainerTerminal( (preload, ex) ->
                    preloads.add(SchemaPreload.fromAst(preload))
                )
            ;
            for (UiContainerExpr ex : external) {
                ex.accept(v, ex);
            }
            return preloads;
        }).flatten(In1Out1.identity());
    }

    public Maybe<SchemaProject> findProject(String path) {
        final SizedIterable<SchemaProject> results = getAllProjects().filter(proj -> proj.getPathGradle().equals(path)).counted();
        if (results.isEmpty()) {
            return Maybe.not();
        }
        assert results.size() == 1 : "Multiple SchemaProject match " + path;
        return Maybe.immutable(results.first());
    }
}
