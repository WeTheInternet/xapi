package net.wti.gradle.schema.map;

import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.schema.api.*;
import net.wti.gradle.schema.impl.SchemaCallbacksDefault;
import net.wti.gradle.schema.parser.SchemaMetadata;
import net.wti.gradle.schema.parser.SchemaParser;
import net.wti.gradle.system.service.GradleService;
import xapi.fu.*;
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
 * <p>
 * Starting at the root schema.xapi, we collect a complete graph of the root + all child {@link SchemaMetadata}.
 * Whereas SchemaMetadata exposes internal AST objects, the SchemaMap translates those into a typesafe, object-oriented graph.
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-02-06 @ 3:45 a.m..
 */
public class SchemaMap implements HasAllProjects {

    public static final String KEY_FOR_EXTENSIONS = "xapiSchemaMap";

    public static SchemaMap fromView(MinimalProjectView view) {
        return fromView(view, () -> view);
    }

    public static SchemaMap fromView(MinimalProjectView view, SchemaParser parser) {
        final SchemaMetadata rootMeta = parser.getSchema();
        return fromView(view, parser, rootMeta);
    }

    public static SchemaMap fromView(MinimalProjectView view, SchemaParser parser, SchemaMetadata rootMeta) {
        return GradleService.buildOnce(SchemaMap.class, view.findView(":"), KEY_FOR_EXTENSIONS, v -> {
            SchemaCallbacksDefault callbacks = new SchemaCallbacksDefault();
            SchemaMap map = new SchemaMap(view, parser, rootMeta, callbacks);
            view.whenSettingsReady(map.resolver.ignoreIn1().ignoreOutput()::in);
            return map;
        });
    }

    private final SchemaMetadata rootSchema;
    private final MinimalProjectView view;
    private final SetLike<SchemaMetadata> allMetadata;
    private final MapLike<String, SchemaMap> children;
    private final MapLike<String, SchemaProject> projects;
    private final SchemaModule rootModule;
    private final SchemaProject rootProject;
    private final Lazy<MinimalProjectView> resolver;
    private final SchemaCallbacks callbacks;
    private SchemaProject tailProject;

    public SchemaMap(
        MinimalProjectView view,
        SchemaParser parser,
        SchemaMetadata root,
        SchemaCallbacks callbacks
    ) {
        this.rootSchema = root;
        this.view = view;
        this.callbacks = callbacks;
        children = mapOrderedInsertion();
        projects = mapOrderedInsertion();
        allMetadata = setLinked();
        String rootName = root.getName();
        // consider changing rootModule published field to either be configurable, or default true (and publish an aggregator / pom-only)
        rootModule = new SchemaModule(rootName, X_Jdk.setLinked(), false, false);
        tailProject = rootProject = new SchemaProject(
            view,
            rootName,
            root.isMultiPlatform(),
            !new File(root.getSchemaDir(), "src").exists()
        );
        projects.put(tailProject.getPath(), tailProject);

        // Create a resolver, so we can forcibly finish our SchemaMap parse on-demand,
        // but still give time for user configuration from settings.gradle to be fully parsed
        resolver = Lazy.deferred1(()->{

            System.out.println("Settings are ready, loading children for " + view);
            loadChildren(getRootProject(), parser, root);
            callbacks.flushCallbacks(this);

            // if we're done loading the root-most build, time to invoke child callbacks!
            if (view.getGradle().getParent() == null) {
                // This ...doesn't really work yet... we'll likely need a specially-serializable set of metadata
                // that each project will write to disk as soon as information comes in...
                // obviating the notion of instances of SchemaMap that can ever "see each other" across distinct gradle builds.
                // Leaving this here moreso as a note that this should be fixed properly later.
                for (SchemaMap child : children.mappedValues()) {
                    child.getCallbacks().flushCallbacks(this);
                    // should probably be: child.close(map), which finalizes the list of projects,
                    // so any not-internally-realized multi-module projects that later become multi-module
                    // will easily know that they should create multiple sourcesets, rather than rely on multiple-projects.
                }
            } else {
                // this is probably where we'd want to serialize some kind of metadata to disk,
                // regarding whether each project is single-module, multi-platform or multi-module (implies multi-platform as well).
                // This way, all participating builds will know which platform/module pairs are present as top-level gradle projects,
                // vs. a sourceset glued into the "main" gradle project.
            }
            return view;
        });
    }

    public Lazy<MinimalProjectView> getResolver() {
        return resolver;
    }

    public MinimalProjectView getView() {
        return view;
    }

    public SchemaCallbacks getCallbacks() {
        return callbacks;
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

    public void loadChildren(SchemaProject project, SchemaParser parser, SchemaMetadata metadata) {
        allMetadata.add(metadata);
        if (metadata == rootSchema) {
            // The first one in!

        } else {
            // hm, we probably want to record more here, when adding child metadatas...
        }
        if (metadata.isExplicit()) {
            parser.loadMetadata(this, project, metadata);
        }

    }

    public void loadExternals(SchemaMetadata metadata, SchemaParser parser, ListLike<UiContainerExpr> externals) {

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
                .withUiContainerTerminal((preload, ex) ->
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
        final String gradlePath = path.startsWith(":") ? path : ":" + path;
        final SizedIterable<SchemaProject> results = getAllProjects().filter(proj -> proj.getPathGradle().equals(
            gradlePath)).counted();
        if (results.isEmpty()) {
            return Maybe.not();
        }
        assert results.size() == 1 : "Multiple SchemaProject match " + path;
        return Maybe.immutable(results.first());
    }

    public void loadProjects(SchemaProject sourceProject, SchemaParser parser, SchemaMetadata metadata) {
        final ListLike<UiContainerExpr> projects = metadata.getProjects();
        for (UiContainerExpr project : projects) {
            String name = project.getName();
            boolean multiplatform = project.getAttribute("multiplatform")
                .mapIfPresent(attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(metadata.isMultiPlatform());
            boolean virtual = project.getAttribute("virtual")
                .mapIfPresent(attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(false);

            final SchemaProject oldTail = tailProject;
            tailProject = new SchemaProject(oldTail, parser.getView().findView(name), name, multiplatform, virtual);
            this.projects.put(tailProject.getPath(), tailProject);
            insertChild(oldTail, parser, metadata, tailProject);
            tailProject = oldTail;
        }
    }

    protected void insertChild(SchemaProject into, SchemaParser parser, SchemaMetadata parent, SchemaProject child) {
        for (SchemaModule module : into.getAllModules()) {
            child.addModule(module);
        }
        for (SchemaPlatform platform : into.getAllPlatforms()) {
            child.addPlatform(platform);
        }

        into.getChildren().add(child);
        File childSchema = new File(parent.getSchemaDir(), child.getName());
        if (!childSchema.exists()) {
            childSchema = new File(childSchema.getParentFile(), child.getName() + ".xapi");
        }
        if (!childSchema.exists()) {
            childSchema = new File(childSchema.getParentFile(), child.getName() + "/schema.xapi");
        }
        File parentDir = parent.getSchemaDir();
        if (!childSchema.exists()) {
            // still nothing... give up and make a contrived relative root
            String relativeRoot = parent.getSchemaDir().getAbsolutePath().replaceFirst(
                getRootSchema().getSchemaDir().getAbsolutePath() + "[/\\\\]*", "");
            childSchema = relativeRoot.isEmpty() ? new File(child.getName()) : new File(relativeRoot, child.getName());
            parentDir = getRootSchema().getSchemaDir();
        }
        final SchemaMetadata parsedChild = parser.parseSchemaFile(parent, parentDir, childSchema);
        loadChildren(child, parser, parsedChild);

    }

    public String getGroup() {
        final String schemaGroup = getRootSchema().getGroup();
        if (QualifiedModule.UNKNOWN_VALUE.equals(schemaGroup)) {
            return getRootSchema().getName();
        }
        return schemaGroup;
    }

    public void setGroup(String group) {
        getRootSchema().setGroup(group);
    }

    public void setVersion(String version) {
        getRootSchema().setVersion(version);
    }

    public String getVersion() {
        return getRootSchema().getVersion();
//        final String schemaVersion = getRootSchema().getVersion();
//        if (QualifiedModule.UNKNOWN_VALUE.equals(schemaVersion)) {
//            return "current";
//        }
//        return schemaVersion;
    }
}
