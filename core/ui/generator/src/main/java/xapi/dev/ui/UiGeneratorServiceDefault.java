package xapi.dev.ui;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.plugin.NodeTransformer;
import com.github.javaparser.ast.visitor.ConcreteModifierVisitor;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;
import xapi.annotation.inject.InstanceDefault;
import xapi.dev.processor.AnnotationTools;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.ContainerMetadata.MetadataRoot;
import xapi.dev.ui.InterestingNodeFinder.InterestingNodeResults;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.ui.api.Ui;
import xapi.ui.api.UiPhase.PhaseBinding;
import xapi.ui.api.UiPhase.PhaseImplementation;
import xapi.ui.api.UiPhase.PhaseIntegration;
import xapi.ui.api.UiPhase.PhasePreprocess;
import xapi.ui.api.UiPhase.PhaseSupertype;
import xapi.util.X_Debug;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/26/16.
 */
@InstanceDefault(implFor = UiGeneratorService.class)
public class UiGeneratorServiceDefault extends AbstractUiGeneratorService {

    private AnnotationTools service;
    private In2Out1<UiContainerExpr, ContainerMetadata, UiComponentGenerator> componentFactory;
    private In2Out1<UiAttrExpr, UiComponentGenerator, UiFeatureGenerator> featureFactory;

    public UiGeneratorServiceDefault() {
        resetFactories();
    }

    private void resetFactories() {
        componentFactory = super::getComponentGenerator;
        featureFactory = super::getFeatureGenerator;
    }

    @Override
    public ComponentBuffer initialize(
          AnnotationTools service, TypeElement type, Ui ui, UiContainerExpr container
    ) {

        this.service = service;
        final String pkgName = service.getPackageName(type);
        final String simpleName = X_Source.enclosedNameFlattened(pkgName, type.getQualifiedName().toString());
        final MetadataRoot root = new MetadataRoot();
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
    public ComponentBuffer runPhase(String id, ComponentBuffer component) {
        component = super.runPhase(id, component);
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
        UiContainerExpr container = metadata.getContainer();
        // replace all <import /> tags with imported resources
        container = resolveImports(service.getElements(), service.getFileManager(), component.getElement(), container);
        if (container.getAttribute("ref") == null) {
            container.addAttribute(false, new UiAttrExpr("ref", new StringLiteralExpr("root")));
        }
        metadata.setContainer(container);
        // find and resolve all nodes with ref attributes
        final InterestingNodeResults interestingNodes = new InterestingNodeFinder().findInterestingNodes(container);
        component.setInterestingNodes(interestingNodes);
        return component;
    }

    @Override
    public UiComponentGenerator getComponentGenerator(UiContainerExpr container, ContainerMetadata metadata) {
        return componentFactory.io(container, metadata);
    }

    @Override
    public UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator) {
        return featureFactory.io(container, componentGenerator);
    }

    protected ComponentBuffer createSupertype(ComponentBuffer component) {

        if (component.hasDataNodes()) {
            generateDataAccessors(component);
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
    public String calculateGeneratedName(
          String pkgName, String className, UiContainerExpr expr
    ) {
        return "Super" + super.calculateGeneratedName(pkgName, className, expr);
    }

    private void saveGeneratedComponent(SourceBuilder<?> binder) {

        final String src = binder.toSource();
        try {
            // TODO: add source element types of anything we loaded during compilation
            final JavaFileObject out = service.outputJava(binder.getQualifiedName());
            try (OutputStream o = out.openOutputStream()) {
                X_IO.drain(o, X_IO.toStreamUtf8(src));
            }
            X_Log.info(getClass(), "Generating ui into ", out.toUri(), "\n", src);
        } catch (IOException e) {
            throw X_Debug.rethrow(e);
        }
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
        visitor.visit(metadata.getContainer(), this);
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

    protected void generateDataAccessors(ComponentBuffer component) {
        final InterestingNodeResults interestingNodes = component.getInterestingNodes();
        Set<UiContainerExpr> dataParents = interestingNodes.getDataParents();
        componentFactory = containerFilter(dataParents);
        featureFactory = (feature, gen) -> {
            if (feature.getNameString().equalsIgnoreCase("data")) {
                return new DataFeatureGenerator();
            } else if (dataParents.contains(ASTHelper.getContainerParent(feature))) {
                // TODO map features which contain nested UiContainExpr via InterestingNodeFinder
                return new UiFeatureGenerator();
            } else {
                return null;
            }
        };
        final ContainerMetadata metadata = component.getRoot();
        UiGeneratorVisitor visitor = createVisitor(component.getRoot());
        visitor.visit(metadata.getContainer(), this);
        resetFactories();
    }

    protected ComponentBuffer createImplementation(ComponentBuffer component) {
        // Generate a boilerplate interface that takes generics for all renderable nodes.

        for (UiImplementationGenerator service : getImplementations()) {
            final ContainerMetadata metadata = component.getImplementation(service.getClass());
            service.setGenerator(this);
            final ContainerMetadata result = service.generateComponent(metadata, component);
            saveGeneratedComponent(result.getSourceBuilder());
        }
        return component;
    }

    protected Iterable<UiImplementationGenerator> getImplementations() {
        return ServiceLoader.load(UiImplementationGenerator.class);
    }

    protected ComponentBuffer peekIntegration(ComponentBuffer component) {
        return component;
    }

    protected ComponentBuffer runBinding(ComponentBuffer component) {
        return component;
    }

    protected ComponentBuffer runCustomPhase(String id, ComponentBuffer component) {
        return component;
    }

}
