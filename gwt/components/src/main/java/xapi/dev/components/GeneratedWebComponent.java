package xapi.dev.components;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.SysExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.GeneratedUiFactory;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.dev.ui.api.UiNamespace;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.assembler.AssembledElement;
import xapi.dev.ui.tags.assembler.AssembledUi;
import xapi.fu.Immutable;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.api.Builderizable;
import xapi.inject.X_Inject;
import xapi.source.X_Modifier;

import static xapi.source.X_Modifier.PUBLIC;
import static xapi.source.X_Modifier.STATIC;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/8/17.
 */
public class GeneratedWebComponent extends GeneratedUiImplementation {

    private final Lazy<UiNamespaceGwt> namespace;
    private FieldBuffer extractor;
    private MethodBuffer assemble;
    private FieldBuffer creator;

    public GeneratedWebComponent(GeneratedUiComponent ui) {
        super(ui, ui.getPackageName());
        namespace = Lazy.deferred1(()-> X_Inject.instance(UiNamespaceGwt.class));
    }

    @Override
    public String getAttrKey() {
        return "gwt";
    }

    @Override
    public UiNamespace reduceNamespace(UiNamespace from) {
        return namespace.out1();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void resolveNativeAttr(
        AssembledUi assembly, UiAttrExpr attr, AssembledElement el, MethodBuffer out
    ) {
        final UiGeneratorTools tools = assembly.getTools();
        final ApiGeneratorContext ctx = assembly.getContext();
        final String name = tools.resolveString(ctx, attr.getName());
        if (name.startsWith("on")) {

            boolean createdCallback = "oncreated".equals(name.toLowerCase());
            // for web components, "on*" event handlers are handled "for free".
            // TODO: something generic to serialize lambdas we want to dump into source...
            final String func = tools.resolveString(ctx, condense(createdCallback, attr), false, true);


            boolean capture = attr.getAnnotation(a->
                a.getNameString().toLowerCase().equals("capture") &&
                (
                    a.getMembers().isEmpty() ||
                    "true".equals(tools.resolveString(ctx, a.getMembers().first().getValue()) )
                )
            ).isPresent();


            out.println(".onCreated( e -> {").indent();
            if (createdCallback) {
                // special "magic" property we use to shove code into the init method for the given element.
                out.printlns(func);
            } else {
                out.patternlns("e.addEventListener(\"$1\", $2, $3);", name.substring(2), func, capture);
            }
            out.outdent().println("})");
            return;
        }
        super.resolveNativeAttr(assembly, attr, el, out);
    }

    private Expression condense(boolean createdCallback, UiAttrExpr attr) {
        final Expression expr = attr.getExpression();
        if (createdCallback && expr instanceof LambdaExpr) {
            final LambdaExpr lambda = (LambdaExpr) expr;
            switch (lambda.getParameters().size()) {
                case 1:
                    if ("e".equals(lambda.getParameters().get(0).getId().getName())) {
                        return new SysExpr(Immutable.immutable1(lambda.getBody()));
                    }
                    break;
                case 0:
                    return new SysExpr(Immutable.immutable1(lambda.getBody()));
            }
            throw new IllegalArgumentException("oncreated lambdas can have at most one parameter named `e`, you sent " + lambda.toSource());
        }
        return expr;
    }

    @Override
    public void finalizeBuilder(GeneratedUiFactory builder) {
        final ClassBuffer cb = getSource().getClassBuffer();

        // We need to strengthen the return type of some inherited methods...
        final UiNamespaceGwt ns = namespace.out1();
        final String builderType = ns.getElementBuilderType(cb);

        if (builder.shouldSaveType()) {
            final MethodBuffer builderMethod = cb.createMethod(PUBLIC | STATIC,
                builder.getQualifiedName(), UiNamespace.METHOD_BUILDER);
            builderMethod.patternln("return new $1<>($2, $3);"
                    // $1
                    , builderMethod.getReturnType()
                    // $2
                    , creator.getName()
                    // $3
                    , extractor.getName()
                );
            cb.addInterface(
                cb.parameterizedType(Builderizable.class, builderType)
            );
        }


        cb
            .createMethod(X_Modifier.PUBLIC, builderType, UiNamespace.METHOD_AS_BUILDER)
            .addAnnotation(Override.class) // fail to compile if this is not a valid override
            .patternln("return ($1) super.$2();", builderType, UiNamespace.METHOD_AS_BUILDER);
        /*
@Override
public PotentialNode<Element> asBuilder() {
  return (PotentialNode<Element>) super.asBuilder();
}
        */

        cb
            .createMethod(X_Modifier.PROTECTED, builderType, UiNamespace.METHOD_NEW_BUILDER)
            .addAnnotation(Override.class)
            .addParameter(cb.parameterizedType(Out1.class, ns.getElementType(cb)), "e")
            .patternln("return new $1<>(e);", builderType.split("<")[0]);
        /*
@Override
protected PotentialNode<Element> newBuilder(Out1<Element> element) {
  PotentialNode<Element> e = new PotentialNode<>(element);
  return e;
}
        */

    }

    public void setMetadata(MethodBuffer assemble, FieldBuffer creator, FieldBuffer extractor) {
        setAssemble(assemble);
        setCreator(creator);
        setExtractor(extractor);
    }

    public void setExtractor(FieldBuffer extractor) {
        this.extractor = extractor;
    }

    public FieldBuffer getExtractor() {
        return extractor;
    }

    public void setAssemble(MethodBuffer assemble) {
        this.assemble = assemble;
    }

    public MethodBuffer getAssemble() {
        return assemble;
    }

    public void setCreator(FieldBuffer creator) {
        this.creator = creator;
    }

    public FieldBuffer getCreator() {
        return creator;
    }
}
