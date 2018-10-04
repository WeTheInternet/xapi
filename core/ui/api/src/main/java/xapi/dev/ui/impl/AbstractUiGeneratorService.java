package xapi.dev.ui.impl;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.plugin.NodeTransformer;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.visitor.ConcreteModifierVisitor;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.gen.SourceHelper;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.InterestingNodeFinder.InterestingNodeResults;
import xapi.fu.*;
import xapi.fu.itr.*;
import xapi.fu.itr.CachingIterator.ReplayableIterable;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.source.X_Source;
import xapi.source.read.JavaModel.IsQualified;
import xapi.ui.api.PhaseMap;
import xapi.ui.api.PhaseMap.PhaseNode;
import xapi.ui.api.UiPhase;
import xapi.ui.api.UiPhase.*;
import xapi.util.X_Debug;
import xapi.util.X_Properties;
import xapi.util.X_String;
import xapi.util.X_Util;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.*;

import static xapi.collect.X_Collect.newSet;
import static xapi.log.X_Log.warn;

/**
 * Created by james on 6/17/16.
 */
public abstract class AbstractUiGeneratorService <Raw, Ctx extends ApiGeneratorContext<Ctx>> extends UiGeneratorTools <Ctx> implements UiGeneratorService <Raw> {

    protected String phase;
    protected final IntTo.Many<Do> onDone;
    protected SourceHelper<Raw> service;
    protected final Lazy<MappedIterable<UiImplementationGenerator>> impls;
    protected final In1Out1<Ctx, StringTo<GeneratedUiDefinition>> definitions;
    protected final StringTo<ComponentBuffer> allComponents;
    protected final IntTo<String> recommendedImports;

    protected static class GeneratorState {

        protected Lazy<MetadataRoot> metadata;
        protected In2Out1<UiContainerExpr, ContainerMetadata, UiComponentGenerator> componentFactory;
        protected In2Out1<UiAttrExpr, UiComponentGenerator, UiFeatureGenerator> featureFactory;

        public In2Out1<UiAttrExpr, UiComponentGenerator, UiFeatureGenerator> getFeatureFactory() {
            return featureFactory;
        }

        public In2Out1<UiContainerExpr, ContainerMetadata, UiComponentGenerator> getComponentFactory() {
            return componentFactory;
        }

        public Lazy<MetadataRoot> getMetadata() {
            return metadata;
        }
    }

    protected volatile GeneratorState state;
    private boolean uiComponent;
    private boolean strict;
    private LogLevel level;

    public AbstractUiGeneratorService() {
        uiComponent = "true".equals(System.getProperty("xapi.ui.generator", "true"));
        onDone = X_Collect.newIntMultiMap(Do.class);
        impls = Lazy.deferred1(()->
            CachingIterator.cachingIterable(getImplementations().iterator())
        );
        recommendedImports = X_Collect.newSet(String.class, X_Collect.MUTABLE_INSERTION_ORDERED_SET);
        allComponents = X_Collect.newStringMap(ComponentBuffer.class);
        StringTo<GeneratedUiDefinition>[] map = new StringTo[1];
        definitions = In1Out1.of(this::loadClasspathDefinitions)
                            .lazy((ctx, factory)-> {
                                if (map[0] == null) {
                                    map[0] = factory.io(ctx);
                                }
                                return map[0];
                            });

        resetState();
    }

    private StringTo<GeneratedUiDefinition> loadClasspathDefinitions(Ctx ctx) {
        final StringTo<GeneratedUiDefinition> map = X_Collect.newStringMapInsertionOrdered(GeneratedUiDefinition.class);
        try {
            final Enumeration<URL> settings = Thread.currentThread().getContextClassLoader().getResources(
                "settings.xapi");
            final Enumeration<URL> generatedSettings = Thread.currentThread().getContextClassLoader().getResources(
                "generated-settings.xapi");
            loadSettings(ctx, map, settings);
            loadSettings(ctx, map, generatedSettings);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return map;
    }

    private void loadSettings(Ctx ctx, StringTo<GeneratedUiDefinition> map, Enumeration<URL> settings) throws IOException {
        while (settings.hasMoreElements()) {
            final URL settingsFile = settings.nextElement();
            try {
                loadSettings(ctx, map, settingsFile);
            } catch (ParseException e) {
                X_Log.error(AbstractUiGeneratorService.class,
                    "Bad settings file", settingsFile, e);
            }
        }
    }

    private void loadSettings(Ctx ctx, StringTo<GeneratedUiDefinition> map, URL settingsFile) throws IOException,
                                                                                            ParseException {
        // TODO have a settings service to do this once globally per classloader
        String source = X_IO.toStringUtf8(settingsFile.openStream());
        final UiContainerExpr container = JavaParser.parseUiContainer(source);
        for (UiAttrExpr component : container.getAttributesMatching(attr -> attr.getNameString().equals("components"))) {
            if (!(component.getExpression() instanceof JsonContainerExpr)) {
                throw new IllegalArgumentException("Components tag must be a [json, array]; you sent " + component);
            }
            JsonContainerExpr items = (JsonContainerExpr) component.getExpression();
            if (!items.isArray()) {
                throw new IllegalArgumentException("Components tag must be a [json, array]; you sent " + component);
            }
            items.getValues()
                 .forAll(e->{
                     final GeneratedUiDefinition definition = GeneratedUiDefinition.fromSettings(
                         tools(),
                         ctx,
                         (UiContainerExpr) e
                     );
                     map.put(definition.getTagName(), definition);
                     map.put(definition.getTypeName(), definition);
                     map.put(definition.getQualifiedName(), definition);
                 });
        }
    }

    @Override
    public ComponentBuffer initialize(
        SourceHelper<Raw> service, IsQualified type, UiContainerExpr container
    ) {
        this.service = service;
        if (container.getName().startsWith("define-api")) {
            return null;
        }
        final String pkgName = type.getPackage();
        final String simpleName = X_Source.enclosedNameFlattened(pkgName, type.getQualifiedName());
        final MetadataRoot root = state.metadata.out1();
        final ContainerMetadata metadata = createMetadata(root, container);
        final Ctx ctx = contextFor(type, container);
        final GeneratedUiComponent component = newComponent(pkgName, simpleName, metadata);
        component.setUiComponent(isUiComponent());
        final ComponentBuffer buffer = new ComponentBuffer(component, metadata);
        if (container.getName().equals("define-tag")) {
            // TODO: handle define-tags by NOT storing the whole buffer.
            // A Buffer is going to need to handle multiple tag definitions,
            // and that work does not add enough value at this time to warrant attention.
            String tagName = resolveString(ctx, container.getAttributeNotNull("tagName").getExpression());
            allComponents.put(tagName, buffer);
            buffer.setTagName(tagName);
            component.setTagName(tagName);
        }
        root.setGeneratedComponent(component);
        buffer.getRoot().setContext(ctx);
        buffer.setElement(type);
        metadata.setControllerType(pkgName, simpleName);
        impls.out1().forEach(impl->impl.spyOnNewComponent(buffer));
        allComponents.put(type.getQualifiedName(), buffer);
        allComponents.put(type.getSimpleName(), buffer);

        return buffer;
    }

    protected Ctx contextFor(IsQualified type, UiContainerExpr container) {
        // If this fails, you need to override this method to return the context type you want to use.
        return (Ctx) new ApiGeneratorContext();
    }

    @Override
    public UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata) {
        return state.componentFactory.io(container, metadata);
    }

    protected boolean ignoreMissingFeatures() {
        return true;
    }

    protected boolean ignoreMissingComponents() {
        return true;
    }

    @Override
    public UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator) {
        return state.featureFactory.io(container, componentGenerator);
    }

    @Override
    public ContainerMetadata createMetadata(MetadataRoot root, UiContainerExpr n) {
        final ContainerMetadata component = new ContainerMetadata(n);
        if (root == null) {
            root = createMetadataRoot();
        }
        // add default imports to metadata root
        root.addRecommendedImports(recommendedImports);

        component.setRoot(root);
        return component;
    }

    protected MetadataRoot createMetadataRoot() {
        return new MetadataRoot();
    }

    /**
     *
     * @param component - the component to run a phase a phase for
     * @param id - the id of the component
     * @return The component, for chaining
     *
     * Note that id is the second parameter, to enable easier method references;
     * this allows us to use this::runPhase as an
     * In2Out1<Component, String, Component>
     * which can then be easily piped into a loop.
     */
    @Override
    public void runPhase(ComponentBuffer component, String id) {
        try(
            @SuppressWarnings("unused") // autoclosed
            Do ondone = tools().startRound(id, component.getGeneratedComponent())
        ) {
            this.phase = id;
            final MappedIterable<UiImplementationGenerator> impls = this.impls.out1();
            switch (id) {
                case PhasePreprocess.PHASE_PREPROCESS:
                    preprocessComponent(component, impls);
                    return;
                case PhaseSupertype.PHASE_SUPERTYPE:
                    createSupertype(component);
                    return;
                case PhaseImplementation.PHASE_IMPLEMENTATION:
                    createImplementation(component, impls);
                    return;
                case PhaseIntegration.PHASE_INTEGRATION:
                    peekIntegration(component);
                    return;
                case PhaseBinding.PHASE_BINDING:
                    runBinding(component);
                    return;
                default:
                    runCustomPhase(id, component);
                    return;
            }
        }
    }


    protected ComponentBuffer preprocessComponent(ComponentBuffer component, Iterable<UiImplementationGenerator> impls) {
        // Find all refs, datanodes and other interesting bits of data.
        final ContainerMetadata metadata = component.getRoot();
        UiContainerExpr container = metadata.getUi();
        // replace all <import /> tags with imported resources
        container = resolveImports(service, component.getElement(), container, getHints());
        final Maybe<UiAttrExpr> refAttr = container.getAttribute("ref");
        if (refAttr == null || refAttr.isAbsent()) {
            container.addAttribute(false, new UiAttrExpr("ref", new StringLiteralExpr("root")));
        }
        metadata.setContainer(container);
        // find and resolve all nodes with ref attributes
        final InterestingNodeResults interestingNodes = new InterestingNodeFinder().findInterestingNodes(container);
        component.setInterestingNodes(interestingNodes);

        for (UiImplementationGenerator impl : impls) {
            impl.spyOnInterestingNodes(component, interestingNodes);
        }

        if (component.hasDataNodes()) {
            generateDataAccessors(component);
        }
        if (component.hasModelNodes()) {
            generateDataAccessors(component);
        }
        if (component.hasCssOrClassname()) {
            generateCssPrimitives(component);
        }
        if (component.hasTemplateReferences()) {
            rewriteTemplateReferences(component);
        }

        return component;
    }

    protected Raw getHints() {
        return null;
    }

    protected ComponentBuffer createSupertype(ComponentBuffer component) {

        // TODO add @Generated tag with all resources we are dependent upon
//        final SourceBuilder<?> root = component.getBinder();
//        onDone.add(()->
//            saveGeneratedComponent(root)
//        );

        return component;
    }

    @Override
    public String overwriteResource(String path, String fileName, String source, Raw hints) {
        return service.saveResource(path, fileName, source, service.filterHints(hints));
    }

    @Override
    public void overwriteSource(String pkgName, String clsName, String source, Raw hints) {
        service.saveSource(pkgName, clsName, source, X_Util.firstNotNull(
            service.filterHints(hints),
            service.filterHints(getHints())
        ));
    }

    protected void saveGeneratedComponent(SourceBuilder<?> binder) {

        final String src = binder.toSource();
        // TODO: add source element types of anything we loaded during compilation
        service.saveSource(binder.getPackage(), binder.getSimpleName(), src, service.filterHints(getHints()));
    }

    protected void rewriteTemplateReferences(ComponentBuffer component) {
        final InterestingNodeResults interestingNodes = component.getInterestingNodes();
        Set<UiContainerExpr> templateParents = interestingNodes.getTemplateNameParents();
        state.componentFactory = containerFilter(templateParents);
        state.featureFactory = (feature, gen) -> {
            if (templateParents.contains(ASTHelper.getContainerParent(feature))) {
                rewriteDataReferences(component, feature, gen, interestingNodes);
                return new UiFeatureGenerator();
            } else {
                return null;
            }
        };
        final ContainerMetadata metadata = component.getRoot();
        UiGeneratorVisitor visitor = createVisitor(metadata, component);
        visitor.visit(metadata.getUi(), this);
        resetFactories();
    }

    protected void rewriteDataReferences(
        ComponentBuffer component,
        UiAttrExpr n,
        UiComponentGenerator gen,
        InterestingNodeResults interestingNodes
    ) {
        final ContainerMetadata me = gen.getMetadata();
        final Expression expr = n.getExpression();
        final Ctx ctx = (Ctx) component.getRoot().getContext();
        Map<Node, Out1<Node>> replacements = new IdentityHashMap<>();
        final ComponentMetadataQuery query = new ComponentMetadataQuery();
        query.setVisitAttributeContainers(false);
        query.setVisitChildContainers(false);
        IntTo<String> refNames = newSet(String.class);
        interestingNodes.getRefNodes().values().forEach(attr->{
            String refName = resolveString(ctx, attr.getExpression());
            refNames.add("$" + refName);
        });
        query.setTemplateNameFilter(refNames::contains);

        expr.accept(
            new ComponentMetadataFinder(),
            query
                .addNameListener((graph, name) -> {
                    String replacement;
                    switch (name.getName()) {
                        case "$root":
                            replacement = "$this";
                            break;
                        default:
                            if (query.isTemplateName(name.getName())) {
                                replacement = query.normalizeTemplateName(name.getName());
                            } else {
                                return;
                            }
                    }
                    String ref = replacement;
                    name.setName(ref);
                    Node parent = name;
                    if (parent.getParentNode() != null) {
                        parent = parent.getParentNode();
                    }
                    if (parent.getParentNode() != null) {
                        parent = parent.getParentNode();
                    }
                    parent.accept(new ModifierVisitorAdapter<Object>() {

                        @Override
                        public Node visit(QualifiedNameExpr n, Object arg) {
                            if (n.getQualifier().getSimpleName().equals("data")) {
                                final NodeTransformer newNode = me.findReplacement(ref, "data");
                                if (newNode != null) {
                                    replacements.put(n, ()->newNode.transformName(n));
                                    return n;
                                }
                            }
                            return super.visit(n, arg);
                        }

                        @Override
                        public Node visit(
                            FieldAccessExpr n, Object arg
                        ) {
                            String var = n.getField();
                            NodeTransformer newNode = me.findReplacement(ref, var);
                            if (newNode != null) {
                                // If this node is the qualifier on a field access,
                                // then we may want to perform additional transformations...
                                if (n.getParentNode() instanceof FieldAccessExpr) {
                                    // A field access may be shorthand notation for a map access...
                                    // The data field was a qualifier of a field access...
                                    FieldAccessExpr parent = (FieldAccessExpr) n.getParentNode();
                                    if (parent.getParentNode() instanceof UnaryExpr) {
                                        // A + - ++ -- ! or ~ expression.  We will replace this with a compute call
                                        // if one is available...
                                        UnaryExpr toReplace = (UnaryExpr) parent.getParentNode();
                                        // ++ and -- must be handled specially, as they perform
                                        // a read and a write
                                        parent.setScope((Expression) newNode.getNode());
                                        replacements.put(toReplace, () -> newNode.transformUnary(n, toReplace));
                                    } else if (parent.getParentNode() instanceof BinaryExpr) {
                                        // A && || = > < >= <= etc binary expression;
                                        // These are safe to replace as simple get operations,
                                        // as they do not perform assignment
                                        BinaryExpr toReplace = (BinaryExpr) parent.getParentNode();
                                        replacements.put(toReplace, () -> newNode.transformBinary(n, toReplace));
                                    } else if (parent.getParentNode() instanceof AssignExpr) {
                                        AssignExpr toReplace = (AssignExpr) parent.getParentNode();
                                        // A plain = assignment will be transformed into a write,
                                        // while all other assignment, += -= etc will need to read and write
                                        final Node result = newNode.transformAssignExpr(toReplace);
                                        replacements.put(
                                            toReplace,
                                            () -> newNode.transformAssignExpr(toReplace)
                                        );
                                    }
                                } else if (n.getParentNode() instanceof ArrayAccessExpr) {
                                    // An array access may be shorthand notation for a list access
                                    ArrayAccessExpr toReplace = (ArrayAccessExpr) n.getParentNode();
                                    final Node result = newNode.transformArrayAccess(toReplace);
                                    replacements.put(toReplace, () -> newNode.transformArrayAccess(toReplace));
                                }
                                return newNode.getNode();
                            }
                            return super.visit(n, arg);
                        }
                    }, null);

                })
        );
        if (!replacements.isEmpty()) {
            ConcreteModifierVisitor.replaceResolved(replacements);
        }
    }

    protected void generateCssPrimitives(ComponentBuffer component) {
        final InterestingNodeResults interestingNodes = component.getInterestingNodes();
        Set<UiContainerExpr> cssParents = interestingNodes.getCssParents();
        state.componentFactory = containerFilter(cssParents);
        state.featureFactory = (feature, gen) -> {
            switch (feature.getNameString().toLowerCase()) {
                case "css":
                case "style":
                case "class":
                    return createCssFeatureGenerator();
            }
            return null;
        };
        final ContainerMetadata metadata = component.getRoot();
        UiGeneratorVisitor visitor = createVisitor(metadata, component);
        visitor.visit(metadata.getUi(), this);
        resetFactories();
    }

    protected abstract UiFeatureGenerator createCssFeatureGenerator();
    protected abstract UiFeatureGenerator createDataFeatureGenerator();
    protected abstract UiFeatureGenerator createModelFeatureGenerator();

    protected void generateDataAccessors(ComponentBuffer component) {
        final InterestingNodeResults interestingNodes = component.getInterestingNodes();
        Set<UiContainerExpr> dataParents = interestingNodes.getDataParents();
        Set<UiContainerExpr> modelParents = interestingNodes.getModelParents();
        final Set<UiContainerExpr> allParents = new LinkedHashSet<>();
        allParents.addAll(dataParents);
        allParents.addAll(modelParents);

        state.componentFactory = containerFilter(allParents);
        state.featureFactory = (feature, gen) -> {
            if (feature.getNameString().equalsIgnoreCase("data")) {
                return createDataFeatureGenerator();
            } else if (feature.getNameString().equalsIgnoreCase("model")) {
                return createModelFeatureGenerator();
            } else if (allParents.contains(ASTHelper.getContainerParent(feature))) {
                // TODO map features which contain nested UiContainerExpr via InterestingNodeFinder
                return new UiFeatureGenerator();
            } else {
                return null;
            }
        };
        final ContainerMetadata metadata = component.getRoot();
        UiGeneratorVisitor visitor = createVisitor(metadata, component);
        visitor.visit(metadata.getUi(), this);
        resetFactories();
    }

    protected ComponentBuffer createImplementation(ComponentBuffer component, MappedIterable<UiImplementationGenerator> impls) {
        // Generate a boilerplate interface that takes generics for all renderable nodes.
        for (UiImplementationGenerator impl : impls) {
            final ContainerMetadata metadata = component.getImplementation(impl.getClass());
            impl.setGenerator(this);
            final GeneratedUiImplementation implResult = impl.generateComponent(
                metadata,
                component,
                impl.getMode(component, metadata)
            );
            onFinish(Integer.MAX_VALUE, ()->{
                final SizedIterable<RequiredChildFactory> debug = implResult.getRequiredChildren();
                debug.size();
            });
        }
        onFinish(Integer.MAX_VALUE-100, ()->
            // This runs fairly late.  TODO: sane location for constants
            component.getGeneratedComponent().resolveGenerics(getGenerator())
        );
        onFinish(Integer.MAX_VALUE, ()->
            // This runs very last, obviously :-)
            component.getGeneratedComponent().saveSource(getGenerator())
        );
        return component;
    }

    protected boolean isUiComponent() {
        return uiComponent;
    }

    protected MappedIterable<UiImplementationGenerator> getImplementations() {
        final ReplayableIterable<UiImplementationGenerator> itr =
            CachingIterator.cachingIterable(ServiceLoader.load(UiImplementationGenerator.class));
        if (itr.isEmpty()) {

            // When no impl type was declared, we should still emit an api and model, if defined.
            // For this to work sanely in java 9, we likely do not want to emit any base types or higher,
            // as you will likely want to generate those together with your final impl,
            // in case your generator decides it wants to add things to the base type.
            // An existing Api type should not be modified when generating an impl type later on;
            // we will want to make an ImmutableUiLayer which enforces this.

            // Once the generator apis themselves are more solid, we can look at
            // ways to ensure both api and base types are always stable / do not change;
            // however, even then, we would want this to be opt-in, so your final set of impls
            // can still insert arbitrary supertypes / shared utilities;
            // remember, you could, theoretically, generate multiple impls for the same runtime environment.

            // As an example, Gwt may want to generate one impl class which uses shadow DOM,
            // and another one which performs slotting + rendering manually.
            // In this case, we would likely want to stuff as much in the base type as possible,
            // with things we likely don't want shared to vert.x, appengine, android, etc.

            // We'll also want to emit partial settings.xapi entries for the component type;
            // final impl-generation will emit the rest of the settings, and we'll need to
            // make the settings reader sanely handle multi-file reads (perhaps someday enforcing
            // single-definition of source files / file hashes or other nice-to-have-but-not-right-nows).

            return SingletonIterator.singleItem(new ApiOnlyUiGenerator().setUiStub(isUiComponent()));
        }
        // Otherwise, lets add a filter on UiGeneratorPlatform annotation, if present...
        StringTo<Integer> priorities = X_Collect.newStringMapInsertionOrdered(Integer.class);
        String[] nullSpace = new String[1];
        String[] ignoredPlatforms = X_Properties.getProperty(UiGeneratorPlatform.SYSTEM_PROP_IGNORE_PLATFORM, "").split(",");
        itr.forAll(impl->{
            final UiGeneratorPlatform type = impl.getClass().getAnnotation(UiGeneratorPlatform.class);
            if (type == null) {
                for (String ignoredPlatform : ignoredPlatforms) {
                    if ("null".equals(ignoredPlatform)) {
                        return;
                    }
                }

                final Integer was = priorities.put("null", 0);
                if (was == null) {
                    nullSpace[0] = impl.getClass().getCanonicalName();
                } else {
                    throw new IllegalStateException("Multiple UiImplementationGenerators without UiGeneratorPlatform found; " +
                        "please add @UiGeneratorPlatform to " + impl.getClass().getCanonicalName() + " and/or " + nullSpace[0]);
                }
            } else {
                for (String ignoredPlatform : ignoredPlatforms) {
                    if (ignoredPlatform.equals(type.value())) {
                        return;
                    }
                }

                final Integer was = priorities.get(type.value());
                if (was == null) {
                    priorities.put(type.value(), type.priority());
                } else {
                    if (was < type.priority()) {
                        priorities.put(type.value(), type.priority());
                    }
                }
            }
        });
        return itr.filter(impl->{
            final UiGeneratorPlatform type = impl.getClass().getAnnotation(UiGeneratorPlatform.class);
            return type == null ||
                (priorities.has(type.value()) && priorities.get(type.value()).equals(type.priority()));
        })
        .spy(impl->impl.setGenerator(this))
        .cached();
    }

    protected ComponentBuffer runBinding(ComponentBuffer component) {
        return component;
    }
    protected ComponentBuffer peekIntegration(ComponentBuffer component) {
        return component;
    }

    protected ComponentBuffer runCustomPhase(String id, ComponentBuffer component) {
        return component;
    }


    protected UiContainerExpr resolveImports(SourceHelper<Raw> filer, IsQualified element, UiContainerExpr container, Raw hints) {
        // we could consider also auto-qualifying all recommended imports... however, this is already complex enough as it is,
        // so we'll leave it to specific code generators to ask the ImportSection to qualify specific ast nodes.
        return (UiContainerExpr) new ModifierVisitorAdapter<Object>(){
            @Override
            public Node visit(
                  UiContainerExpr n, Object arg
            ) {
                if ("import".equals(n.getName())) {
                    final Maybe<UiAttrExpr> file = n.getAttribute("file");
                    if (!file.isPresent()) {
                        throw new IllegalArgumentException("import tags must specify a file feature");
                    }
                    String loc = ASTHelper.extractAttrValue(file.get());
                    final FileObject resource;
                    String src = null;
                    try {
                        if (loc.indexOf('/') == -1) {
                            // This file is relative to our source file
                            String pkgName = element.getPackage();
                            if (pkgName == null) {
                                pkgName = "";
                            } else {
                                pkgName = pkgName.replace('.', '/');
                            }
                            src = filer.readSource(pkgName, loc, filer.filterHints(hints));
                        } else {
                            // Treat the file as absolute classpath uri
                            src = filer.readSource("", loc, filer.filterHints(hints));
                        }
                        final UiContainerExpr newContainer = JavaParser.parseUiContainer(loc, src, level);
                        return newContainer;
                    } catch (ParseException e) {
                        X_Log.error(getClass(), "Error trying to resolve import", n, "with source:\n",
                              X_String.addLineNumbers(src), e);
                    }
                }
                return super.visit(n, arg);
            }
        }.visit(container, null);
    }

    protected FileObject findFile(JavaFileManager filer, String pkgName, String loc) {
        try {
            return filer.getFileForInput(
                  StandardLocation.SOURCE_PATH,
                  pkgName,
                  loc
            );
        } catch (IOException e) {
            throw X_Debug.rethrow(e);
        }
    }

    @Override
    public UiGeneratorVisitor createVisitor(ContainerMetadata metadata, ComponentBuffer buffer) {
        return new UiGeneratorVisitor(scope->{
            scopes.add(scope);
            return ()->{
                UiVisitScope popped = scopes.pop();
                assert popped == scope : "Scope stack inconsistent; " +
                    "expected " + popped + " to be the same reference as " + scope;
            };
        }, metadata, buffer);
    }

    @Override
    public UiGeneratorService getGenerator() {
        return this;
    }

    @Override
    public void finish(MappedIterable<ComponentBuffer> itr, String id) {
        int maxLoop = 50;
        Mutable<Do> startCleanup = new Mutable<>();
        while (maxLoop-->0) {
            startCleanup.in(Do.NOTHING);

            itr.forAll(component->
                startCleanup.process(Do::doAfter, // melt `Do`s together
                tools().startRound(id, component.getGeneratedComponent())));

            while (!onDone.isEmpty()) {
                // Because the underlying collection is an ordered map,
                // we just keep removing the first element, clearing those items.
                // This allows job prioritization without much hassle
                onDone.removeOne(item -> {
                    item.removeAll(Do::done);
                    // This task may have added another job of the same priority,
                    // and the backing map might be configured to return that in
                    // the current iterator, or require us to restart, without
                    // deleting the new item; thus, we only remove a prioritized
                    // multimap entry if the underlying collection is still non-empty
                    // after we called .removeAll.  Comod is dangerous,
                    // but sometimes acceptable, and if you don't want it,
                    // configure your CollectionService(s) to return validating multimaps
                    return item.isEmpty();
                });
            }
            startCleanup.out1().done();
            if (onDone.isEmpty()) {
                return;
            }
        }
        assert maxLoop > 0 : "Ui Generator service " + getClass() + " ran onFinish 50 times.";
    }

    @Override
    public void onFinish(int priority, Do ondone) {
        onDone.get(priority).add(ondone);
    }

    protected void resetFactories() {
        state.metadata = Lazy.deferred1(this::createMetadataRoot);
        state.componentFactory = this::superGetComponentGenerator;
        state.featureFactory = this::superGetFeatureGenerator;
    }

    protected final UiFeatureGenerator superGetFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator) {
        final UiFeatureGenerator generator = super.getFeatureGenerator(container, componentGenerator);
        if (generator == null) {
            if (ignoreMissingFeatures()) {
                warn(UiConstants.location(container, AbstractUiGeneratorService.class)
                    , "Ignoring missing feature ", container);
            } else {
                throw new NullPointerException("Null feature for " + container.getName());
            }
        }
        return generator;
    }

    protected final UiComponentGenerator superGetComponentGenerator(UiContainerExpr container, ContainerMetadata metadata) {
        final UiComponentGenerator generator = super.getComponentGenerator(container, metadata);
        if (generator == null) {
            if (ignoreMissingComponents()) {
                warn(UiConstants.location(container, AbstractUiGeneratorService.class),
                    "Ignoring missing component ", container);
            } else {
                throw new NullPointerException("Null component for " + container.getName());
            }
        }
        return generator;
    }

    @Override
    public UiGeneratorTools tools() {
        return this;
    }

    @Override
    public SizedIterable<GeneratedUiComponent> allComponents() {
        return seen.cached();
    }

    @Override
    public MappedIterable<GeneratedApi> allApis() {
        return apis.cached();
    }

    @Override
    public ComponentBuffer getComponent(ApiGeneratorContext<?> ctx, String name) {
        return allComponents.get(name);
    }

    @Override
    public GeneratedUiDefinition getComponentDefinition(ApiGeneratorContext<?> ctx, String name) {
        return getDefinition((Ctx)ctx, name);
    }

    @Override
    public GeneratedUiDefinition getDefinition(Ctx ctx, String name) {
        if (allComponents.containsKey(name)) {
            return allComponents.get(name).getDefinition();
        }
        // otherwise, we need to lookup the type from saved settings.xapi
        return definitions.io((Ctx)ctx).get(name);
    }

    @Override
    public ReferenceType getDefaultComponentType(
        AbstractUiImplementationGenerator impl, GeneratedUiComponent component
    ) {
        // TODO: create a generator-scoped ClassWorld for type lookups,
        // For now, we are going to use SimpleComponent to erase UiNode from required parameter types
        return null;
    }

    @Override
    public MappedIterable<GeneratedUiComponent> generateComponents(
        SourceHelper<Raw> sources, In1Out1<UiContainerExpr, IsQualified> type, UiContainerExpr ... parsed
    ) {
        try {

            PhaseMap<String> map = PhaseMap.withDefaults(new LinkedHashSet<>());
            final Iterator<PhaseNode<String>> phases = map.forEachNode().iterator();
            final MappedIterable<? extends Out2<
                ComponentBuffer,
                Do>
                > components = ArrayIterable.iterate(parsed)
                .map(ui -> {
                    final IsQualified myType = type.io(ui);
                    ComponentBuffer buffer = initialize(sources, myType, ui);
                    Do onStart = pauseState();
                    return Out2.out2Immutable(buffer, onStart);
                })
                .cached();

            phases.forEachRemaining(phase->
                components.forAll(job->{
                    job.out2().done();
                    final ComponentBuffer buffer = job.out1();
                    runPhase(buffer, phase.getId());
                })
            );
            final MappedIterable<ComponentBuffer> itr = components
                .map(Out2::out1);
            finish(itr, UiPhase.CLEANUP);
        } catch (Throwable t) {
            X_Log.error(AbstractUiGeneratorService.class,
                "Did not generate ui successfully;", t, "for", parsed);
            if (isStrict()) {
                throw t;
            }
        }


        final MappedIterable<GeneratedUiComponent> results = allComponents();
        resetState();
        seen.clear();

        return results;
    }

    @Override
    public MappedIterable<GeneratedApi> generateApis(
        SourceHelper<Raw> sources, In1Out1<UiContainerExpr, IsQualified> typeFactory, UiContainerExpr... parsed
    ) {
        // actually do the necessary things to generate a bunch of api types...
        for (UiContainerExpr expr : parsed) {
            final IsQualified type = typeFactory.io(expr);
            initialize(sources, type, expr);
            GeneratedApi api = new GeneratedApi(this, sources, type, expr);
            apis.add(api);
            for (UiAttrExpr attr : expr.getAttributes()) {
                switch (attr.getNameString()) {
                    case "import":
                    case "imports":
                        api.addImports(attr);
                        break;
                    case "model":
                    case "models":
                        final Ctx ctx = contextFor(type, expr);
                        api.addModels(tools(), ctx, attr);
                        break;
                    case "migration":
                    case "migrations":
                        api.addMigrations(attr);
                        break;
                    default:
                        // log unhandled.
                        X_Log.error(AbstractUiGeneratorService.class,
                            "Unhandled define-api feature ", tools().debugNode(attr));
                }
            }

        }

        final MappedIterable<GeneratedApi> results = allApis();
        return results;
    }

    public Do pauseState() {
        final GeneratorState currentState = state;
        resetState();
        return ()-> state = currentState;
    }

    protected void resetState() {
        state = new GeneratorState();
        resetFactories();
    }

    @Override
    protected void initializeComponent(GeneratedUiComponent result, ContainerMetadata metadata) {
        if (shouldInitialize(result)) {
            for (UiImplementationGenerator impl : impls.out1()) {
                if (impl instanceof UiGeneratorTools) {
                    ((UiGeneratorTools)impl).initializeComponent(result, metadata);
                }
            }
        }
        super.initializeComponent(result, metadata);
    }

    public void setUiComponent(boolean uiComponent) {
        this.uiComponent = uiComponent;
    }

    @Override
    public boolean isStrict() {
        return strict;
    }

    @Override
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    public LogLevel getLevel() {
        return level;
    }

    @Override
    public void setLevel(LogLevel level) {
        this.level = level;
    }
}
