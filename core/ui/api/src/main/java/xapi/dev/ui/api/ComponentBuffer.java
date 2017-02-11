package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.GeneratedUiImplementation.RequiredMethodType;
import xapi.dev.ui.impl.InterestingNodeFinder.InterestingNodeResults;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.Out1;
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
        this.root = immediate ? immutable1(root.out1()) : Lazy.deferred1(root);
        implementations = X_Collect.newClassMap(ContainerMetadata.class);
        component = new GeneratedUiComponent(pkgName, simpleName);
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

    private Out1<DomBuffer> dom = Lazy.deferred1(DomBuffer::new);

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
        UiContainerExpr ui
    ) {
        GeneratedUiBase otherBase = other.getBase();
        // Our tag factory should be a method generated onto the base class;
        // if the source tag supplied a model or a style, we must pass those references in as parameters.
        final GeneratedUiComponent me = getGeneratedComponent();
        final GeneratedUiBase myBase = me.getBase();
        String myBaseName = myBase.getWrappedName();
        myBaseName = otherBase.getSource().addImport(myBaseName);
        final Maybe<UiAttrExpr> model = ui.getAttribute("model");
        final Maybe<UiAttrExpr> style = ui.getAttribute("style");
        final String name = "create" + myBaseName;
        MethodCallExpr call = new MethodCallExpr(null, name);
        final Type returnType = tools.methods().$type(tools, ctx, StringLiteralExpr.stringLiteral(myBaseName)).getType();
        GeneratedUiMethod method = new GeneratedUiMethod(returnType, name);
        method.setSource(ui);
        method.setContext(ctx);
        final MethodBuffer creator = other.getBase().getSource().getClassBuffer()
            .createMethod("public abstract " + myBaseName + " " + name);
        final List<Expression> args = new ArrayList<>();
        RequiredMethodType type = CREATE;
        if (model.isPresent()) {
            type = CREATE_FROM_MODEL;
            args.add(tools.resolveVar(ctx, model.get().getExpression()));
            method.addParam(me.getPublicModel().getWrappedName(), "model");
            creator.addParameter(getGeneratedComponent().getPublicModel().getWrappedName(), "model");
        }
        if (style.isPresent()) {
            type = type == CREATE_FROM_MODEL ? CREATE_FROM_MODEL_AND_STYLE : CREATE_FROM_STYLE;
            args.add( tools.resolveVar(ctx, style.get().getExpression()));
            final String styleType = namespace.getBaseStyleResourceType(CanAddImports.NO_OP);
            method.addParam(styleType, "style");
            creator.addParameter(styleType, "style");
        }
        method.setType(type);

        call.setArgs(args);

        call.addExtra(UiConstants.EXTRA_SOURCE_COMPONENT, getGeneratedComponent());

        other.getImpls().forAll(GeneratedUiImplementation::requireMethod, type, method, call);
        return call;
    }
}
