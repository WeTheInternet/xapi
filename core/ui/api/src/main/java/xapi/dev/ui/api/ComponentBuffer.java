package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.api.GeneratedUiMember;
import xapi.dev.api.GeneratedUiModel;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.api.GeneratedUiImplementation.RequiredMethodType;
import xapi.dev.ui.impl.InterestingNodeFinder.InterestingNodeResults;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.UiTagGenerator;
import xapi.dev.ui.tags.assembler.AssembledUi;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.Out1;
import xapi.source.util.X_Modifier;
import xapi.source.read.JavaModel.IsQualified;

import java.util.ArrayList;
import java.util.List;

import static xapi.dev.ui.api.GeneratedUiImplementation.RequiredMethodType.*;
import static xapi.fu.Immutable.immutable1;
import static xapi.inject.X_Inject.instance;

/**
 * Created by james on 6/17/16.
 */
public class ComponentBuffer {

    private final Out1<ContainerMetadata> root;

    private final ClassTo<ContainerMetadata> implementations;

    private Out1<DomBuffer> domBuffer = Lazy.deferred1(this::defaultDomBuffer);
    private IsQualified element;
    private InterestingNodeResults interestingNodes;
    private final GeneratedUiComponent component;
    private String tagName;
    private AssembledUi assembled;

    public ComponentBuffer(String pkgName, String simpleName) {
        this(pkgName, simpleName, immutable1(instance(ContainerMetadata.class)), true);
    }

    public ComponentBuffer(String pkgName, String simpleName, ContainerMetadata metadata) {
        this(pkgName, simpleName, immutable1(metadata), true);
    }

    public ComponentBuffer(String pkgName, String simpleName, Out1<ContainerMetadata> metadata) {
        this(pkgName, simpleName, Lazy.deferred1(metadata), false);
    }

    public ComponentBuffer(String pkgName, String simpleName, Out1<ContainerMetadata> root, boolean immediate) {
        this(new GeneratedUiComponent(pkgName, simpleName, root.map(ContainerMetadata::getUi)), root, immediate);
    }

    public ComponentBuffer(GeneratedUiComponent component, ContainerMetadata root) {
        this(component, immutable1(root), true);
    }
    public ComponentBuffer(GeneratedUiComponent component, Out1<ContainerMetadata> root, boolean immediate) {
        this.root = immediate ? immutable1(root.out1()) : Lazy.deferred1(root);
        implementations = X_Collect.newClassMap(ContainerMetadata.class);
        this.component = component;
    }

    protected DomBuffer defaultDomBuffer() {
        return instance(DomBuffer.class);
    }

    public SourceBuilder<?> getBinder() {
        return component.getApi().getSource();
    }

    public ContainerMetadata getRoot() {
        return root.out1();
    }

    public DomBuffer getDom() {
        return domBuffer.out1();
    }

    public IsQualified getElement() {
        return element;
    }

    public void setElement(IsQualified element) {
        this.element = element;
    }

    public void setInterestingNodes(InterestingNodeResults interestingNodes) {
        this.interestingNodes = interestingNodes;
    }

    public InterestingNodeResults getInterestingNodes() {
        return interestingNodes;
    }

    public boolean hasDataNodes() {
        return interestingNodes != null && interestingNodes.hasDataNodes();
    }

    public boolean hasModelNodes() {
        return interestingNodes != null && interestingNodes.hasModelNodes();
    }

    public boolean hasCssNodes() {
        return interestingNodes != null && interestingNodes.hasCssNodes();
    }

    public boolean hasCssOrClassname() {
        return interestingNodes != null && interestingNodes.hasCssOrClassname();
    }

    public boolean hasTemplateReferences() {
        return interestingNodes != null && interestingNodes.hasTemplateReferences();
    }

    public ContainerMetadata getImplementation(Class<? extends UiImplementationGenerator> implType) {
        final ContainerMetadata r = getRoot();
        // TODO use a ClassTo...
        implementations.getOrCompute(implType, t->r.createImplementation(implType));
        return r;
    }

    public GeneratedUiComponent getGeneratedComponent() {
        return component;
    }

    public MethodCallExpr getTagFactory(
        UiGeneratorTools tools,
        ApiGeneratorContext ctx,
        GeneratedUiComponent other,
        UiNamespace namespace,
        UiContainerExpr ui,
        Expression modelNode
    ) {
        // I hate this method and everything it does.
        // ...but it works, so it stays...  for now.
        GeneratedUiBase otherBase = other.getBase();
        // Our tag factory should be a method generated onto the base class;
        // if the source tag supplied a model or a style, we must pass those references in as parameters.
        final GeneratedUiComponent me = getGeneratedComponent();

        final String elBuilder = otherBase.getElementBuilderType(namespace);
        final GeneratedUiApi myApi = getGeneratedComponent().getApi();
        String methodName = "create" + myApi.getWrappedName();
        boolean firstTime = !otherBase.hasMethod(methodName);
        final MethodBuffer createMethod = otherBase.getOrCreateMethod(X_Modifier.PUBLIC_ABSTRACT, elBuilder, methodName);
        final GeneratedUiFactory externalBuilder = other.getFactory();
        GeneratedUiMethod method = component.createFactoryMethod(tools, ctx, namespace, externalBuilder);
        final String name = method.getMemberName();
        final List<Expression> args = new ArrayList<>();
        MethodCallExpr call = new MethodCallExpr(null, name);

        method.setSource(ui);
        method.setContext(ctx);

        if (modelNode != null) {
            // when we are sent a model node, we are expecting newer semantics;
            // we will leave the old code below for posterity,
            // until such time that it is deemed fit for deletion (likely soon...)
            if (modelNode.hasExtra(UiConstants.EXTRA_MODEL_INFO)) {
                GeneratedUiMember member = modelNode.getExtra(UiConstants.EXTRA_MODEL_INFO);
                assert member != null : "Cannot use a name expression without EXTRA_MODEL_INFO: " + tools.debugNode(modelNode);
                final Type memberType = member.getMemberType();
                final String typeName = tools.getComponentType(modelNode, memberType);
                if (firstTime) {
                    createMethod.addParameter(typeName, member.getMemberName());
                }
                args.add(modelNode);
            }

            other.getImpls()
                .forAll(GeneratedUiImplementation::addChildFactory, getDefinition(), modelNode);

            call.setArgs(args);

            return call;
        }

        final Maybe<UiAttrExpr> model = ui.getAttribute("model");
        final Maybe<UiAttrExpr> style = ui.getAttribute("style");

        RequiredMethodType type = CREATE;
        if (model.isPresent()) {
            type = CREATE_FROM_MODEL;
            final Expression modelExpr = model.get().getExpression();
            final Expression resolved = tools.resolveVar(ctx, modelExpr);
            args.add(resolved);
            method.addParam(me.getPublicModel().getWrappedName(), "model");
            createMethod.addParameter(getGeneratedComponent().getPublicModel().getWrappedName(), "model");
        }
        if (style.isPresent()) {
            type = type == CREATE_FROM_MODEL ? CREATE_FROM_MODEL_AND_STYLE : CREATE_FROM_STYLE;
            args.add( tools.resolveVar(ctx, style.get().getExpression()));
            final String styleType = namespace.getBaseStyleResourceType(CanAddImports.NO_OP);
            method.addParam(styleType, "style");
            createMethod.addParameter(styleType, "style");
        }

        method.setType(type);

        call.setArgs(args);

        other.getImpls().forAll(GeneratedUiImplementation::requireMethod, type, method, call);
        return call;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }

    public GeneratedUiDefinition getDefinition() {
        final GeneratedUiDefinition definition = new GeneratedUiDefinition();
        definition.setTagName(getTagName());
        definition.setTypeName(component.getApi().getTypeName());
        definition.setPackageName(component.getPackageName());
        if (component.getApi().hasModel()) {
            final GeneratedUiModel model = component.getApi().getModel();
            definition.setModelName(model.getWrappedName());
            definition.getModelFields().putEntries(model.getFields());
        }
        if (component.isUiComponent()) {
            definition.setBuilderName(component.getFactory().getWrappedName());
        }
        return definition;
    }

    public ApiGeneratorContext getContext() {
        return getRoot().getContext();
    }

    public AssembledUi getAssembly(UiGeneratorTools tools, UiNamespace namespace, UiTagGenerator generator) {
        if (assembled == null) {
            assembled = generator.newAssembly(this, tools, namespace);
        } else {
            assert assembled.getTools() == tools : "Inconsistent assembly; saw two different sets of tools : " +
                assembled.getTools() + " and " + tools;
            assert assembled.getNamespace() == namespace : "Inconsistent assembly; saw two different namespaces : " +
                assembled.getNamespace() + " and " + namespace;
            assert generator.isCompatible(assembled.getGenerator()) : "Inconsistent assembly; saw incompatible generators; " +
                assembled.getGenerator() + " is rejected by " + generator;
        }
        return assembled;
    }
}
