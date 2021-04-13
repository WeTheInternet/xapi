package net.wti.gradle.schema.map;

import com.github.javaparser.ast.expr.JsonPairExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.require.api.DependencyType;
import net.wti.gradle.require.api.PlatformModule;
import net.wti.gradle.schema.api.*;
import net.wti.gradle.schema.impl.SchemaCallbacksDefault;
import net.wti.gradle.schema.parser.DefaultSchemaMetadata;
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
import xapi.util.X_String;

import java.io.File;

import static xapi.fu.java.X_Jdk.mapOrderedInsertion;
import static xapi.fu.java.X_Jdk.setLinked;

/**
 * A complete mapping of a fully-parsed project tree.
 * <p>
 * Starting at the root schema.xapi, we collect a complete graph of the root + all child {@link DefaultSchemaMetadata}.
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
        final DefaultSchemaMetadata rootMeta = parser.getSchema();
        return fromView(view, parser, rootMeta);
    }

    public static SchemaMap fromView(MinimalProjectView view, SchemaParser parser, DefaultSchemaMetadata rootMeta) {
        return GradleService.buildOnce(SchemaMap.class, view.findView(":"), KEY_FOR_EXTENSIONS, v -> {
            SchemaCallbacksDefault callbacks = new SchemaCallbacksDefault();
            SchemaMap map = new SchemaMap(view, parser, rootMeta, callbacks);
            view.whenSettingsReady(map.resolver.ignoreIn1().ignoreOutput()::in);
            return map;
        });
    }

    private final DefaultSchemaMetadata rootSchema;
    private final MinimalProjectView view;
    private final SetLike<DefaultSchemaMetadata> allMetadata;
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
        DefaultSchemaMetadata root,
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

            System.out.println(view.getClass().getSimpleName() + " is ready, loading children for file://" + view.getProjectDir());
            loadMetadata(getRootProject(), parser, root);
            callbacks.flushCallbacks(this);

            // if we're done loading the root-most build, time to invoke child callbacks!
            if (view.getGradle().getParent() == null) {
                // This ...doesn't really work yet... we'll likely need a specially-serializable set of metadata
                // that each project will write to disk as soon as information comes in...
                // obviating the notion of instances of SchemaMap that can ever "see each other" across distinct gradle builds.
                // Leaving this here moreso as a note that this should be fixed properly later.
                // Later on: XapiLoaderPlugin is doing this with with schema index (and generated gradle projects)
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

            fixMetadata();
            callbacks.flushCallbacks(this);

            return view;
        });
    }

    private void fixMetadata() {
        for (SchemaProject project : getAllProjects()) {
            project.forAllPlatformsAndModules((plat, mod) -> {
                    PlatformModule myPlatMod = new PlatformModule(plat.getName(), mod.getName());
                    String platReplace = plat.getReplace();
                    if (X_String.isNotEmpty(platReplace)) {
                        // Need to bind gwt:api to main:api or jre:main to main:main
                        PlatformModule intoPlatMod = myPlatMod.edit(plat.getReplace(), null);
                        final SchemaDependency dep = new SchemaDependency(DependencyType.internal, myPlatMod, getGroup(), getVersion(), intoPlatMod.toStringStrict());
                        project.getDependencies().get(myPlatMod).add(dep);
                    }
                    // bind, say, jre:main to jre:api, or main:test to main:main
                    for (String includes : mod.getInclude()) {
                        // build an { internal: "dependency" }
                        PlatformModule intoPlatMod = myPlatMod.edit(null, includes);
                        final SchemaDependency dep = new SchemaDependency(DependencyType.internal, myPlatMod, getGroup(), getVersion(), intoPlatMod.toStringStrict());
                        project.getDependencies().get(myPlatMod).add(dep);
                    }
            });
        }
    }

    public Lazy<MinimalProjectView> getResolver() {
        return resolver;
    }

    public MinimalProjectView getView() {
        return getResolver().out1();
    }

    @Override
    public SchemaCallbacks getCallbacks() {
        return callbacks;
    }

    public void addChild(String at, SchemaMap child) {
        if (allowChild(rootSchema, at, child)) {
            children.put(at, child);
        }
    }

    protected boolean allowChild(DefaultSchemaMetadata root, String at, SchemaMap child) {
        // TODO: allow platform limitation properties to prevent a child schema map from being remembered.
        return true;
    }

    public void loadMetadata(SchemaProject project, SchemaParser parser, DefaultSchemaMetadata metadata) {
        allMetadata.add(metadata);
        if (metadata == rootSchema) {
            // The first one in!

        } else {
            // hm, we probably want to record more here, when adding child metadatas...
        }
//        if (metadata.isExplicit()) {
            parser.loadMetadata(this, project, metadata, parser);
//        }

    }

    public void loadExternals(DefaultSchemaMetadata metadata, SchemaParser parser, ListLike<UiContainerExpr> externals) {

    }

    public DefaultSchemaMetadata getRootSchema() {
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

    @Override
    public SetLike<SchemaProject> getAllProjects() {
        final SetLike<SchemaProject> all = X_Jdk.setLinked();
        visitChildren(all, rootProject);
        return all;
    }

    private void visitChildren(final SetLike<SchemaProject> all, final SchemaProject project) {
        if (all.addIfMissing(project)) {
            for (SchemaProject child : project.getChildren()) {
                visitChildren(all, child);
            }
        }
    }

    public SetLike<DefaultSchemaMetadata> getAllMetadata() {
        final SetLike<DefaultSchemaMetadata> all = X_Jdk.setLinked();
        all.addNow(allMetadata);
        return all;
    }

    public MappedIterable<SchemaPreload> getAllPreloads() {
        return allMetadata.mapRemoveNull(DefaultSchemaMetadata::getExternal).map(external -> {
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

    public void loadProjects(SchemaProject sourceProject, SchemaParser parser, DefaultSchemaMetadata metadata) {
        final ListLike<UiContainerExpr> projects = metadata.getProjects();
        for (UiContainerExpr project : projects) {
            String name = project.getName();
            boolean multiplatform = project.getAttribute("multiplatform")
                .mapIfPresent(attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(metadata.isMultiPlatform());
            boolean virtual = project.getAttribute("virtual")
                .mapIfPresent(attr -> "true".equals(attr.getStringExpression(false)))
                .ifAbsentReturn(false);
            String parentPath = project.getAttribute("parentPath")
                .mapIfPresent(attr -> attr.getStringExpression(false))
                .ifAbsentReturn("");

            // TODO: load this project if it's UiElement

            final SchemaProject oldTail = tailProject;

            String viewPath = toViewPath(parentPath, name);
            MinimalProjectView projectView = parser.getView().findView(viewPath);
            if (projectView == null) {
                throw new IllegalArgumentException("No view found named " + name);
            }
            if (oldTail.hasProject(name)) {
                tailProject = oldTail.getProject(name);
                if (multiplatform) {
                    tailProject.setMultiplatform(true);
                }
                tailProject.setVirtual(virtual);
            } else {
                final MinimalProjectView targetView = parser.getView().findView(viewPath);
                tailProject = new SchemaProject(oldTail, targetView, name, multiplatform, virtual);
                tailProject.setParentGradlePath(parentPath);
            }
            this.projects.put(tailProject.getPath(), tailProject);
            insertChild(oldTail, parser, metadata, project, tailProject);
            tailProject = oldTail;
        }
    }

    private String toViewPath(final String parentPath, final String name) {
        return X_String.isEmptyTrimmed(parentPath) ? name
                : parentPath.startsWith(":") ? parentPath + ":" + name :
                ":" + parentPath + ":" + name;
    }

    protected void insertChild(SchemaProject into, SchemaParser parser, DefaultSchemaMetadata parent, final UiContainerExpr project, SchemaProject child) {
        for (SchemaModule module : into.getAllModules()) {
            child.addModule(module);
        }
        for (SchemaPlatform platform : into.getAllPlatforms()) {
            child.addPlatform(platform);
        }

        // TODO: have a property to be able to specify the project path, for cases when project name != directory name
        into.addProject(child);
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
        // the project may have additional fields, like `modules =` or `platforms =`
        DefaultSchemaMetadata parsedChild = projectToMeta.get(child);
        if (parsedChild == null) {
            parsedChild = new DefaultSchemaMetadata(parent, childSchema);
            projectToMeta.put(child, parsedChild);
            if (childSchema.exists()) {
                // we only want to parse this schema file once; it is a non-configurable search heuristic,
                // so no-need to keep parsing it just to add another <xapi-schema /> element.
                parsedChild = parser.parseSchemaFile(parent, parsedChild, parentDir);
            } else {
                parsedChild = parser.parseProjectElement(parent, parsedChild, project);
            }
        } else {
            assert parsedChild.getParent() == parent : "Existing " + parsedChild + " disagrees about who is parent:" +
                    "\n1:\n" + parsedChild.getParent() +" (previous value)" +
                    "\n2:\n" + parent + " (new value)";
            parser.parseProjectElement(parent, parsedChild, project);
        }
        loadMetadata(child, parser, parsedChild);
//        loadProjectElement(child, parsedChild, project, parser);

    }
    private final MapLike<SchemaProject, DefaultSchemaMetadata> projectToMeta = X_Jdk.mapIdentity();

    private void loadProjectElement(final SchemaProject project, final DefaultSchemaMetadata meta, final UiContainerExpr element, final SchemaParser parser) {
        ComposableXapiVisitor.whenMissingFail(SchemaMap.class, ()->"")
                .withUiContainerRecurse(In2.ignoreAll())
                .withNameTerminal(In2.ignoreAll())
                .withUiAttrTerminal((attr, ignore)->{
                    switch (attr.getNameString()) {
                        case "multiplatform":
                        case "virtual":
                            break;
                        case "platforms":
                        case "modules":
                            parser.loadModules(project, meta, parser);
                        case "dependencies":
                            break;
                        case "requires":
                        case "require":
                            break;
                        default:
                            throw new UnsupportedOperationException("projects = <element />, element " + element.getName() + " does not support attribute name " + attr.toSource());
                    }
                })
                .visit(element, null);
    }

    @Override
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

    @Override
    public String getVersion() {
        String version = getRootSchema().getVersion();
        if (X_String.isEmptyTrimmed(version)) {
            return QualifiedModule.UNKNOWN_VALUE;
        }
        return version;
//        final String schemaVersion = getRootSchema().getVersion();
//        if (QualifiedModule.UNKNOWN_VALUE.equals(schemaVersion)) {
//            return "current";
//        }
//        return schemaVersion;
    }
}
