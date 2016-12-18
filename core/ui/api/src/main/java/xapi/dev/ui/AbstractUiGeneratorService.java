package xapi.dev.ui;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.plugin.NodeTransformer;
import com.github.javaparser.ast.visitor.ConcreteModifierVisitor;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.gen.SourceHelper;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.ContainerMetadata.MetadataRoot;
import xapi.dev.ui.InterestingNodeFinder.InterestingNodeResults;
import xapi.fu.Do;
import xapi.fu.In2Out1;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.Out1;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.source.read.JavaModel.IsQualified;
import xapi.ui.api.UiPhase.PhaseBinding;
import xapi.ui.api.UiPhase.PhaseImplementation;
import xapi.ui.api.UiPhase.PhaseIntegration;
import xapi.ui.api.UiPhase.PhasePreprocess;
import xapi.ui.api.UiPhase.PhaseSupertype;
import xapi.util.X_Debug;
import xapi.util.X_Util;

import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Created by james on 6/17/16.
 */
public abstract class AbstractUiGeneratorService <Raw, Ctx extends ApiGeneratorContext<Ctx>> extends UiGeneratorTools <Ctx> implements UiGeneratorService <Raw> {

    protected String phase;
    protected final IntTo<Do> onDone;
    protected Lazy<MetadataRoot> metadata;
    protected SourceHelper<Raw> service;

    protected In2Out1<UiContainerExpr, ContainerMetadata, UiComponentGenerator> componentFactory;
    protected In2Out1<UiAttrExpr, UiComponentGenerator, UiFeatureGenerator> featureFactory;

    public AbstractUiGeneratorService() {
        onDone = X_Collect.newList(Do.class);
        resetFactories();
    }


    @Override
    public ComponentBuffer initialize(
        SourceHelper<Raw> service, IsQualified type, UiContainerExpr container
    ) {
        this.service = service;
        final String pkgName = type.getPackage();
        final String simpleName = X_Source.enclosedNameFlattened(pkgName, type.getQualifiedName());
        final MetadataRoot root = metadata.out1();
        final ContainerMetadata metadata = createMetadata(root, container);
        final ComponentBuffer component = new ComponentBuffer(metadata);
        component.setElement(type);
        metadata.setControllerType(pkgName, simpleName);
        String generatedName = calculateGeneratedName(pkgName, simpleName, container);
        final SourceBuilder<ContainerMetadata> b = new SourceBuilder<>();
        b.setClassDefinition("public class " + generatedName, false);
        b.setPackage(pkgName);
        metadata.setSourceBuilder(b);

        return component;
    }

    @Override
    public UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata) {
        return componentFactory.io(container, metadata);
    }

    protected boolean ignoreMissingFeatures() {
        return true;
    }

    protected boolean ignoreMissingComponents() {
        return true;
    }

    protected void warn(Object ... logMe) {
        X_Log.warn(logMe);
    }

    @Override
    public UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator) {
        return featureFactory.io(container, componentGenerator);
    }

    @Override
    public ContainerMetadata createMetadata(MetadataRoot root, UiContainerExpr n) {
        final ContainerMetadata component = new ContainerMetadata(n);
        component.setRoot(root == null ? createMetadataRoot() : root);
        return component;
    }

    protected MetadataRoot createMetadataRoot() {
        return new MetadataRoot();
    }

    @Override
    public ComponentBuffer runPhase(String id, ComponentBuffer component) {
        this.phase = id;
        switch (id) {
            case PhasePreprocess.PHASE_PREPROCESS:
                return preprocessComponent(component);
            case PhaseSupertype.PHASE_SUPERTYPE:
                return createSupertype(component);
            case PhaseImplementation.PHASE_IMPLEMENTATION:
                return createImplementation(component);
            case PhaseIntegration.PHASE_INTEGRATION:
                return peekIntegration(component);
            case PhaseBinding.PHASE_BINDING:
                return runBinding(component);
            default:
                return runCustomPhase(id, component);
        }
    }


    protected ComponentBuffer preprocessComponent(ComponentBuffer component) {
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
        return component;
    }

    protected Raw getHints() {
        return null;
    }

    protected ComponentBuffer createSupertype(ComponentBuffer component) {

        if (component.hasDataNodes()) {
            generateDataAccessors(component);
        }
        if (component.hasCssOrClassname()) {
            generateCssPrimitives(component);
        }
        if (component.hasTemplateReferences()) {
            rewriteTemplateReferences(component);
        }

        SourceBuilder<ContainerMetadata> binder = component.getBinder();
        // TODO add @Generated tag with all resources we are dependent upon

        final SourceBuilder<ContainerMetadata> root = component.getBinder();
        onDone.add(()->
            saveGeneratedComponent(root)
        );

        return component;
    }

    @Override
    public void overwriteResource(String path, String fileName, String source, Raw hints) {
        service.saveResource(path, fileName, source, hints);
    }

    @Override
    public void overwriteSource(String pkgName, String clsName, String source, Raw hints) {
        service.saveSource(pkgName, clsName, source, X_Util.firstNotNull(hints, getHints()));
    }

    protected void saveGeneratedComponent(SourceBuilder<?> binder) {

        final String src = binder.toSource();
        // TODO: add source element types of anything we loaded during compilation
        service.saveSource(binder.getPackage(), binder.getSimpleName(), src, getHints());
    }

    protected void rewriteTemplateReferences(ComponentBuffer component) {
        final InterestingNodeResults interestingNodes = component.getInterestingNodes();
        Set<UiContainerExpr> templateParents = interestingNodes.getTemplateNameParents();
        componentFactory = containerFilter(templateParents);
        featureFactory = (feature, gen) -> {
            if (templateParents.contains(ASTHelper.getContainerParent(feature))) {
                rewriteDataReferences(component, feature, gen);
                return new UiFeatureGenerator();
            } else {
                return null;
            }
        };
        final ContainerMetadata metadata = component.getRoot();
        UiGeneratorVisitor visitor = createVisitor(component.getRoot());
        visitor.visit(metadata.getUi(), this);
        resetFactories();
    }

    protected void rewriteDataReferences(ComponentBuffer component, UiAttrExpr n, UiComponentGenerator gen) {
        final ContainerMetadata me = gen.getMetadata();
        final Expression expr = n.getExpression();
        Map<Node, Out1<Node>> replacements = new IdentityHashMap<>();
        final ComponentMetadataQuery query = new ComponentMetadataQuery();
        query.setVisitAttributeContainers(false);
        query.setVisitChildContainers(false);
        expr.accept(
            new ComponentMetadataFinder(),
            query
                .addNameListener((graph, name) -> {
                    String replacement;
                    switch (name.getName()) {
                        case "$root":
                            replacement = me.getRootReference();
                            break;
                        default:
                            if (query.isTemplateName(name.getName())) {
                                replacement = query.normalizeTemplateName(name.getName());
                            } else {
                                replacement = null;
                            }
                    }
                    String ref = replacement;
                    name.setName(ref);
                    name.getParentNode().getParentNode().accept(new ModifierVisitorAdapter<Object>() {
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
        componentFactory = containerFilter(cssParents);
        featureFactory = (feature, gen) -> {
            switch (feature.getNameString().toLowerCase()) {
                case "css":
                case "style":
                case "class":
                    return createCssFeatureGenerator();
            }
            return null;
        };
        final ContainerMetadata metadata = component.getRoot();
        UiGeneratorVisitor visitor = createVisitor(component.getRoot());
        visitor.visit(metadata.getUi(), this);
        resetFactories();
    }

    protected abstract UiFeatureGenerator createCssFeatureGenerator();
    protected abstract UiFeatureGenerator createDataFeatureGenerator();

    protected void generateDataAccessors(ComponentBuffer component) {
        final InterestingNodeResults interestingNodes = component.getInterestingNodes();
        Set<UiContainerExpr> dataParents = interestingNodes.getDataParents();
        componentFactory = containerFilter(dataParents);
        featureFactory = (feature, gen) -> {
            if (feature.getNameString().equalsIgnoreCase("data")) {
                return createDataFeatureGenerator();
            } else if (dataParents.contains(ASTHelper.getContainerParent(feature))) {
                // TODO map features which contain nested UiContainExpr via InterestingNodeFinder
                return new UiFeatureGenerator();
            } else {
                return null;
            }
        };
        final ContainerMetadata metadata = component.getRoot();
        UiGeneratorVisitor visitor = createVisitor(component.getRoot());
        visitor.visit(metadata.getUi(), this);
        resetFactories();
    }

    protected ComponentBuffer createImplementation(ComponentBuffer component) {
        // Generate a boilerplate interface that takes generics for all renderable nodes.

        for (UiImplementationGenerator service : getImplementations()) {
            final ContainerMetadata metadata = component.getImplementation(service.getClass());
            service.setGenerator(this);
            final ContainerMetadata result = service.generateComponent(metadata, component);
            final SourceBuilder<ContainerMetadata> src = result.getSourceBuilder();
            onFinish(()->{
                saveGeneratedComponent(src);
            });
        }
        return component;
    }

    protected Iterable<UiImplementationGenerator> getImplementations() {
        return ServiceLoader.load(UiImplementationGenerator.class);
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
                            }
                            src = filer.readSource(pkgName, loc, hints);
                        } else {
                            // Treat the file as absolute classpath uri
                            src = filer.readSource("", loc, hints);
                        }
                        final UiContainerExpr newContainer = JavaParser.parseUiContainer(src);
                        return newContainer;
                    } catch (ParseException e) {
                        X_Log.error(getClass(), "Error trying to resolve import", n, "with source:\n",
                              X_Source.addLineNumbers(src), e);
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
    public UiGeneratorVisitor createVisitor(ContainerMetadata metadata) {
        return new UiGeneratorVisitor(scope->{
           scopes.add(scope);
            return ()->{
                UiVisitScope popped = scopes.pop();
                assert popped == scope : "Scope stack inconsistent; " +
                      "expected " + popped + " to be the same reference as " + scope;
            };
        }, metadata);
    }

    @Override
    public UiGeneratorService getGenerator() {
        return this;
    }

    @Override
    public void finish() {
        onDone.removeAll(Do::done);
    }

    @Override
    public void onFinish(Do ondone) {
        onDone.add(ondone);
    }

    protected void resetFactories() {
        metadata = Lazy.deferred1(this::createMetadataRoot);
        componentFactory = this::superGetComponentGenerator;
        featureFactory = this::superGetFeatureGenerator;
    }

    protected final UiFeatureGenerator superGetFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator) {
        final UiFeatureGenerator generator = super.getFeatureGenerator(container, componentGenerator);
        if (generator == null) {
            if (ignoreMissingFeatures()) {
                warn(getClass(), "Ignoring missing feature ", container);
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
                warn(getClass(), "Ignoring missing component ", container);
            } else {
                throw new NullPointerException("Null component for " + container.getName());
            }
        }
        return generator;
    }

    @Override
    public String calculateGeneratedName(
        String pkgName, String className, UiContainerExpr expr
    ) {
        return "Super" + super.calculateGeneratedName(pkgName, className, expr);
    }

    @Override
    public UiGeneratorTools tools() {
        return this;
    }
}
