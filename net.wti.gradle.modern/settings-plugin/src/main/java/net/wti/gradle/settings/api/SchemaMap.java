package net.wti.gradle.settings.api;

import net.wti.gradle.api.MinimalProjectView;
import net.wti.gradle.settings.XapiSchemaParser;
import net.wti.gradle.settings.impl.SchemaCallbacksDefault;
import net.wti.gradle.settings.index.IndexNodePool;
import net.wti.gradle.settings.index.IndexingFailedException;
import net.wti.gradle.settings.index.SchemaIndex;
import net.wti.gradle.settings.index.SchemaIndexerImpl;
import net.wti.gradle.settings.model.SchemaPreload;
import net.wti.gradle.settings.plugin.XapiSettingsPlugin;
import net.wti.gradle.settings.schema.DefaultSchemaMetadata;
import net.wti.gradle.tools.HasAllProjects;
import net.wti.gradle.tools.InternalGradleCache;
import net.wti.javaparser.ast.expr.UiContainerExpr;
import net.wti.javaparser.ast.visitor.ComposableXapiVisitor;
import org.gradle.api.UnknownDomainObjectException;
import xapi.fu.*;
import xapi.fu.data.ListLike;
import xapi.fu.data.MapLike;
import xapi.fu.data.SetLike;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.MappedIterable;
import xapi.fu.java.X_Jdk;
import xapi.fu.log.Log;
import xapi.string.X_String;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static xapi.fu.java.X_Jdk.mapOrderedInsertion;
import static xapi.fu.java.X_Jdk.setLinked;
import static xapi.string.X_String.isNotEmpty;

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
    private final IndexNodePool nodePool;
    private Lazy<SchemaIndex> indexProvider;

//    public static HasAllProjects fromView(MinimalProjectView view) {
//        return fromView(view, () -> view);
//    }
//
//    public static HasAllProjects fromView(MinimalProjectView view, XapiSchemaParser parser) {
//        SchemaProperties props = parser.getProperties();
//        final DefaultSchemaMetadata rootMeta = parser.getSchema(explicitPlatform);
//        SchemaMap map = fromView(view, parser, rootMeta, props);
//        return map;
//    }

    public void setIndexProvider(final Out1<SchemaIndex> deferred) {
        this.indexProvider = Lazy.deferred1(deferred);
    }

    @Override
    public Lazy<SchemaIndex> getIndexProvider() {
        return indexProvider;
    }

    public static SchemaMap fromView(MinimalProjectView srcView, XapiSchemaParser parser, DefaultSchemaMetadata rootMeta, SchemaProperties props) {
        RootProjectView view = RootProjectView.rootView(srcView);
        IndexNodePool nodes = IndexNodePool.fromSettings(view.getSettings());
        return InternalGradleCache.buildOnce(SchemaMap.class, view, KEY_FOR_EXTENSIONS, v -> {
            SchemaCallbacksDefault callbacks = new SchemaCallbacksDefault();
            SchemaMap map = new SchemaMap(view, nodes, parser, rootMeta, callbacks);

            String indexProp = props.getIndexIdProp(view);
            String indexState = System.getProperty(indexProp);
//            if ("true".equals(indexState)) {
//                // yay, the index is already run. Return a lightweight HasAllProjects...
//                final Out1<SchemaIndex> indexProvider = Lazy.deferred1(()->{
//                    final String buildName = props.getBuildName(view);
//                    final CharSequence version = new LazyString(map::getVersion);
//                    final CharSequence group = new LazyString(map::getGroup);
//                    final SchemaIndexReader reader = props.createReader(view, version);
//                    return new SchemaIndexImmutable(buildName, group, version, reader);
//                });
////            SchemaCallbacksDefault callbacks = new SchemaCallbacksDefault();
////            IndexBackedSchemaMap map = new IndexBackedSchemaMap(view, callbacks, props, indexProvider);
////            // TODO: we need to kick off a job to read SchemaProject structure from index.
////                 we need to recreate / minimize the api surface of SchemaProject, such that it can be wholly read from disk.
////
////            return map;
//                // until above works, we always work with a "do all the work over again" SchemaMap...
//                map.setIndexProvider(indexProvider);
//            } else {
            // yikes! index hasn't run yet. Start it now!
//                view.whenReady(ready ->{
            SchemaIndexerImpl indexer = InternalGradleCache.buildOnce(
                    SchemaIndexerImpl.class,
                    view,
                    view.getBuildName() + "_index", ignored-> {
                        final SchemaIndexerImpl index = new SchemaIndexerImpl(props, map.getNodePool());
                        Log.loggerFor(SchemaMap.class, srcView)
                                        .log(Log.LogLevel.INFO, "Index has not yet run; starting one for SchemaMap");
                        return index;
                    }
            );
            final String bn = props.getBuildName(view);
            final Out1<SchemaIndex> deferred = indexer.index(view, bn, map);
            map.setIndexProvider(deferred);
//                });

//            }
            return map;
        });
    }

    public IndexNodePool getNodePool() {
        return nodePool;
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
    private volatile Do onResolve;

    public SchemaMap(
            final MinimalProjectView view,
            final IndexNodePool nodes,
            final XapiSchemaParser parser,
            final DefaultSchemaMetadata root,
            final SchemaCallbacks callbacks
    ) {
        onResolve = Do.NOTHING;
        this.rootSchema = root;
        this.view = view;
        this.nodePool = nodes;
        this.callbacks = callbacks;
        children = mapOrderedInsertion();
        projects = mapOrderedInsertion();
        allMetadata = setLinked();
        String rootName = root.getName();
        // consider changing rootModule published field to either be configurable, or default true (and publish an aggregator / pom-only)
        rootModule = new SchemaModule(rootName, X_Jdk.setLinked(), false, false, false);
        tailProject = rootProject = new SchemaProject(
                view,
                rootName,
                null,
                null,
                true
        );
        projects.put(tailProject.getPathGradle(), tailProject);

        // Create a resolver, so we can forcibly finish our SchemaMap parse on-demand,
        // but still give time for user configuration from settings.gradle to be fully parsed
        resolver = Lazy.withSpy(()->{
            Log.loggerFor(SchemaMap.class, view)
                            .log(Log.LogLevel.INFO, view.getClass().getSimpleName() + " is ready for project " + view.getProjectDir());
            loadMetadata(getRootProject(), parser, root);
            callbacks.flushCallbacks(this);

            // Commenting out this attempt to handle SchemaMap from child builds in memory here somehow...
            // because it's silly, since we shouldn't really be interacting w/ objects from other builds.
//
//            // if we're done loading the root-most build, time to invoke child callbacks!
//            if (view.getGradle().getParent() == null) {
//                // This ...doesn't really work yet... we'll likely need a specially-serializable set of metadata
//                // that each project will write to disk as soon as information comes in...
//                // obviating the notion of instances of SchemaMap that can ever "see each other" across distinct gradle builds.
//                // Leaving this here moreso as a note that this should be fixed properly later.
//                // Later on: XapiSettingsPlugin is doing this with schema index (and generated gradle projects)
//                for (SchemaMap child : children.mappedValues()) {
//                    child.getCallbacks().flushCallbacks(this);
//                    // should probably be: child.close(map), which finalizes the list of projects,
//                    // so any not-internally-realized multi-module projects that later become multi-module
//                    // will easily know that they should create multiple sourcesets, rather than rely on multiple-projects.
//                }
//            } else {
//                // this is probably where we'd want to serialize some kind of metadata to disk,
//                // regarding whether each project is single-module, multi-platform or multi-module (implies multi-platform as well).
//                // This way, all participating builds will know which platform/module pairs are present as top-level gradle projects,
//                // vs. a sourceset glued into the "main" gradle project.
//            }

            fixMetadata();
            callbacks.flushCallbacks(this);
            return view;
        }, doAfter -> {
            // This "doAfter" spy is invoked _after_ the lazy finishes resolving.
            // Because user can pass any callbacks they like to whenResolved(),
            // we need to guard against deadlock; if user callback launches a thread that tries
            // to also resolve this schema map, the whole thing locks up. Using a spy prevents this,
            // by returning our lock before processing user-supplied callbacks.
            drainCallbacks();
        });
    }

    private void drainCallbacks() {
        while (onResolve != Do.NOTHING) {
            final Do myRes;
            synchronized (allMetadata) {
                // before we invoke the user-supplied callbacks, lets reset to blank.
                myRes = onResolve;
                onResolve = Do.NOTHING;
            }
            myRes.done();
            callbacks.flushCallbacks(this);
        }
    }

    private void fixMetadata() {
        for (SchemaProject project : getAllProjects()) {
            final Set<String> once = new HashSet<>();
            project.forAllPlatformsAndModules((plat, mod) -> {
                if (plat.isDisabled()) {
                    // don't process disabled platforms
                    return;
                }
                PlatformModule myPlatMod = new PlatformModule(plat.getName(), mod.getName());
                String platReplace = plat.getReplace();
                if (isNotEmpty(platReplace)) {
                    // Need to bind gwt:api to main:api or jre:main to main:main
                    PlatformModule intoPlatMod = myPlatMod.edit(platReplace, null);
                    final SchemaDependency dep = new SchemaDependency(DependencyType.internal, myPlatMod, getGroup(), getVersion(), intoPlatMod.toStringStrict());
                    project.getDependencies().get(myPlatMod).add(dep);
                }
                // bind, say, jre:main to jre:api, or main:test to main:main
                for (String includes : mod.getInclude()) {
                    if (project.hasModule(includes)) {
                        // build an { internal: "dependency" }
                        PlatformModule intoPlatMod = myPlatMod.edit(null, includes);
                        final SchemaDependency dep = new SchemaDependency(DependencyType.internal, myPlatMod, getGroup(), getVersion(), intoPlatMod.toStringStrict());
                        project.getDependencies().get(myPlatMod).add(dep);
                        if (mod.isPublished()) {
                            ensureChildrenPublished(project, mod, includes, once);
                        }
                    }
                }
            });
        }
    }

    private void ensureChildrenPublished(final SchemaProject project, final SchemaModule mod, final String includes, final Set<String> once) {
        if (once.add(includes)) {
            final SchemaModule included;
            try {
                included = project.getModule(includes);
            } catch (UnknownDomainObjectException missing) {
                throw new IndexingFailedException("Could not find module " + includes + " in project " + project.getPathGradle() + " " +
                        "(known modules: " + project.getAllModules().map(SchemaModule::getName).join(", ") + ")");
            }

            if (!included.isPublished()) {
                included.updatePublished(true);
                for (String childInclude : included.getInclude()) {
                    ensureChildrenPublished(project, included, childInclude, once);
                }
            }
        }
    }

    public Lazy<MinimalProjectView> getResolver() {
        return resolver;
    }

    @Override
    public void resolve() {
        getResolver().out1();
        drainCallbacks();
    }

    public void whenResolved(Do job) {
        if (resolver.isResolved()) {
            job.done();
        } else {
            synchronized (allMetadata) {
                onResolve = onResolve.doAfter(job);
            }
        }
    }

    public MinimalProjectView getView() {
        return view;
    }

    @Override
    public SchemaCallbacks getCallbacks() {
        return callbacks;
    }

//    public void addChild(String at, SchemaMap child) {
//        if (allowChild(rootSchema, at, child)) {
//            children.put(at, child);
//        }
//    }

    protected boolean allowChild(DefaultSchemaMetadata root, String at, SchemaMap child) {
        // TODO: allow platform limitation properties to prevent a child schema map from being remembered.
        return true;
    }

    public void loadMetadata(SchemaProject project, XapiSchemaParser parser, DefaultSchemaMetadata metadata) {
        allMetadata.add(metadata);
        if (metadata == rootSchema) {
            // The first one in!

        } else {
            // hm, we probably want to record more here, when adding child metadatas...
        }
        String explicitPlatform = parser.getProperties().getProperty(project.getView(), "xapi.platform");
//        if (metadata.isExplicit()) {
        parser.loadMetadata(this, project, metadata, parser, explicitPlatform);
//        }

    }

    public void loadExternals(DefaultSchemaMetadata metadata, XapiSchemaParser parser, ListLike<UiContainerExpr> externals) {

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

    public void loadProjects(SchemaProject sourceProject, XapiSchemaParser parser, DefaultSchemaMetadata metadata, final String explicitPlatform) {
        final ListLike<UiContainerExpr> projects = metadata.getProjects();
        for (UiContainerExpr project : projects) {
            String name = project.getName();

            boolean inherit = project.getAttribute("inherit")
                    .mapIfPresent(attr -> "true".equals(attr.getStringExpression(false)))
                    .ifAbsentReturn(metadata.isInherit());
            boolean multiplatform = project.getAttribute("multiplatform")
                    .mapIfPresent(attr -> "true".equals(attr.getStringExpression(false)))
                    .ifAbsentReturn(inherit && metadata.isMultiPlatform());
            boolean virtual = project.getAttribute("virtual")
                    .mapIfPresent(attr -> "true".equals(attr.getStringExpression(false)))
                    .ifAbsentReturn(false);
            String parentPath = project.getAttribute("parentPath")
                    .mapIfPresent(attr -> attr.getStringExpression(false))
                    .ifAbsentReturn("");

            // TODO: load this project if it's UiElement

            final SchemaProject oldTail = tailProject;

            String viewPath = toViewPath(parentPath, name);
            // need to fake a project view using a mostly-delegate
            final File fixedDir = new File(parser.getView().getProjectDir(), viewPath.replaceAll(":", "/"));
            MinimalProjectView projectView = new SchemaProjectView(parser.getView(), fixedDir);
            if (oldTail.hasProject(name)) {
                tailProject = oldTail.getProject(name);
                if (multiplatform) {
                    tailProject.setMultiplatform(true);
                }
                if (virtual) {
                    tailProject.setVirtual(true);
                }
            } else {
                tailProject = new SchemaProject(oldTail, projectView, name, multiplatform, virtual, inherit);
                tailProject.setParentGradlePath(parentPath);
            }
            this.projects.put(tailProject.getPathGradle(), tailProject);
            insertChild(oldTail, parser, metadata, project, tailProject, explicitPlatform);
            tailProject = oldTail;
        }
    }

    private String toViewPath(final String parentPath, final String name) {
        return X_String.isEmptyTrimmed(parentPath) ? name
                : parentPath.startsWith(":") ? parentPath + ":" + name :
                ":" + parentPath + ":" + name;
    }

    protected void insertChild(SchemaProject into, XapiSchemaParser parser, DefaultSchemaMetadata parent, final UiContainerExpr project, SchemaProject child, final String explicitPlatform) {

        // TODO: have a property to be able to specify the project path, for cases when project name != directory name
        into.addProject(child);
        File childSchema = new File(parent.getSchemaDir(), child.getName());
        File maybe = new File(childSchema, child.getName() + ".xapi");
        if (maybe.isFile()) {
            childSchema = maybe;
        }
        maybe = new File(childSchema, "schema.xapi");
        if (maybe.isFile()) {
            childSchema = maybe;
        }
        if (!childSchema.isFile()) {
            childSchema = new File(childSchema.getParentFile(), child.getName() + ".xapi");
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
            if (childSchema.isFile()) {
                // we only want to parse this schema file once; it is a non-configurable search heuristic,
                // so no-need to keep parsing it just to add another <xapi-schema /> element.
                parsedChild = parser.parseSchemaFile(parent, parsedChild, parentDir, explicitPlatform);
            } else {
                parsedChild = parser.parseProjectElement(parent, parsedChild, project, explicitPlatform);
            }
        } else {
            assert parsedChild.getParent() == parent : "Existing " + parsedChild + " disagrees about who is parent:" +
                    "\n1:\n" + parsedChild.getParent() +" (previous value)" +
                    "\n2:\n" + parent + " (new value)";
            parser.parseProjectElement(parent, parsedChild, project, explicitPlatform);
        }
        loadMetadata(child, parser, parsedChild);
        if (parsedChild.isInherit()) {
            for (SchemaModule module : into.getAllModules()) {
                child.addModule(module);
            }
            for (SchemaPlatform platform : into.getAllPlatforms()) {
                child.addPlatform(platform);
            }
        } else {
            child.addModule(into.getDefaultModule());
            child.addPlatform(into.getDefaultPlatform());
        }
//        loadProjectElement(child, parsedChild, project, parser);

    }
    private final MapLike<SchemaProject, DefaultSchemaMetadata> projectToMeta = X_Jdk.mapIdentity();

//    private void loadProjectElement(final SchemaProject project, final DefaultSchemaMetadata meta, final UiContainerExpr element, final XapiSchemaParser parser) {
//        ComposableXapiVisitor.whenMissingFail(SchemaMap.class, ()->"")
//                .withUiContainerRecurse(In2.ignoreAll())
//                .withNameTerminal(In2.ignoreAll())
//                .withUiAttrTerminal((attr, ignore)->{
//                    switch (attr.getNameString()) {
//                        case "multiplatform":
//                        case "virtual":
//                            break;
//                        case "platforms":
//                        case "modules":
//                            parser.loadModules(project, meta, parser);
//                        case "dependencies":
//                            break;
//                        case "requires":
//                        case "require":
//                            break;
//                        default:
//                            throw new UnsupportedOperationException("projects = <element />, element " + element.getName() + " does not support attribute name " + attr.toSource());
//                    }
//                })
//                .visit(element, null);
//    }

    @Override
    public String getGroup() {
        final String schemaGroup = getRootSchema().getGroup();
        if (QualifiedModule.UNKNOWN_VALUE.equals(schemaGroup)) {
            return getRootSchema().getName();
        }
        return schemaGroup;
    }

    @Override
    public void setGroup(String group) {
        getRootSchema().setGroup(group);
    }

    @Override
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

    @Override
    public File getRootSchemaFile() {
        return rootSchema.getSchemaFile();
    }

    public boolean hasGradleProject(final String path) {
        if (XapiSettingsPlugin.hasRememberedProject(view.getSettings(), path)) {
            return true;
        }
        for (SchemaProject value : projects.mappedValues()) {
            if (value.getPathGradle().equals(path)) {
                return true;
            }
            if (":".equals(value.getPathGradle())) {
                // don't do any startWith checks on ":" -- everything is a match
                continue;
            }
            if (path.equals(value.getPathGradle())) {
                    return true;
            }
            if (value.isMultiplatform()) {
                if (value.hasProject(path.replaceFirst(":", ""))) {
                    return true;
                }
            }
        }
        return false;
    }

    public SchemaProject getProject(final String projectPath) {
        final String path = projectPath.startsWith(":") ? projectPath : ":" + projectPath;
        return projects.getMaybe(path).getOrThrow(()->new IllegalArgumentException(
                "No such project: " + path + "; available projects: " + projects.keys().join(", ")
        ));
    }
}
