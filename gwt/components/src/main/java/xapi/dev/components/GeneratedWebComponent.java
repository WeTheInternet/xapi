package xapi.dev.components;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.plugin.Transformer;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.assembler.AssembledElement;
import xapi.dev.ui.tags.assembler.AssembledUi;
import xapi.elemental.X_Elemental;
import xapi.except.NotYetImplemented;
import xapi.fu.Immutable;
import xapi.fu.Lazy;
import xapi.fu.itr.MappedIterable;
import xapi.fu.Out1;
import xapi.fu.api.Builderizable;
import xapi.fu.itr.ArrayIterable;
import xapi.inject.X_Inject;
import xapi.source.X_Modifier;
import xapi.source.X_Source;

import java.util.List;

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
        final String lower = name.toLowerCase();
        if (lower.startsWith("on")) {

            final boolean created, attached, detached, attribute;
            switch (lower) {
                // be forgiving to developers, at least until there are better tools to write xapi
                case "oncreated":
                case "oncreate":
                    created = true;
                    attached = detached = attribute = false;
                    break;
                case "onattached":
                case "onattach":
                    attached = true;
                    created = detached = attribute = false;
                    break;
                case "ondetached":
                case "ondetach":
                    detached = true;
                    created = attached = attribute = false;
                    break;
                case "onattribute":
                case "onattributechanged":
                case "onattributechange":
                    attribute = true;
                    created = attached = detached = false;
                    break;
                default:
                    created = attached = detached = attribute = false;
            }
            // for web components, "on*" event handlers are handled "for free".
            // TODO: something generic to serialize lambdas we want to dump into source...
            final String func = tools.resolveString(ctx, condense(created || attached || detached, attr), false, true);

            boolean capture = attr.getAnnotation(a->
                a.getNameString().toLowerCase().equals("capture") &&
                (
                    a.getMembers().isEmpty() ||
                    "true".equals(tools.resolveString(ctx, a.getMembers().first().getValue()) )
                )
            ).isPresent();

            if (attached || detached || attribute) {
                // defer adding this so we can be sure component variable is defined,
                // and that our "userspace" callbacks are serviced after basic wiring is in place.
                // If/when we need more control, we'll need to make a manager class for the generated install method.
                assembly.getUi().beforeSave(service->{

                    // we'll put these guys into the web component definition installation method
                    MethodBuffer install = getOwner().getAst().getExtra(UiConstants.EXTRA_INSTALL_METHOD);
                    assert install != null : "No " + UiConstants.EXTRA_INSTALL_METHOD + " extra found in " + el.debugNode(getOwner().getAst());
                    // add the appropriate callback in the install method

                    install.patternln("component.$1Callback( $2::get$3, e ->",
                        attached ? "attached" : detached ? "detached" : "attributeChanged",
                        getWrappedName(), getOwner().getApi().getWrappedName()
                    ).indent();

                    install.printlns(func);

                    install.outdent().println(");");
                });
            } else {
                // created callbacks and other event-related stuff, we'll use the ElementBuilder's
                // onCreate callback, as that is really the soonest that we can _mutate_ the element.
                // afterCreated() will use RunSoon semantics, and created() is so early we won't be
                // allowed to read or write DOM hierarchy / attributes / etc.
                out.println(".onCreated( e -> {").indent();
                if (created) {
                    // special "magic" property we use to shove code into the init method for the given element.
                    out.printlns(func);
                } else {
                    boolean multiline = func.contains("\n");
                    String fixed = (multiline ? "\n" : " ") + func.trim();
                    out.patternlns("e.addEventListener(\"$1\",$2, $3);", name.substring(2), fixed, capture);
                }
                out.outdent().println("})");
            }

            return;
        }
        switch (lower) {
            case "class":
            case "classname":
                // bind up some css!
                // ...should just call the standard css generator from visitor,
                // and then add an additional "set classname" operation
                throw new NotYetImplemented(name + " not yet supported in "+ el.debug());
            default:
                // just copy the attributes over...
                super.resolveNativeAttr(assembly, attr, el, out);
        }

    }

    private Expression condense(boolean specialCallback, UiAttrExpr attr) {
        final Expression expr = attr.getExpression();
        if (specialCallback && expr instanceof LambdaExpr) {
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
            throw new IllegalArgumentException("oncreated|attached|detached lambdas can have at most one parameter named `e`, you sent " + lambda.toSource());
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

    @Override
    public void addCss(ContainerMetadata container, UiAttrExpr attr) {
        // For now, we'll just dump the css text into a new stylesheet and inject it.
        // we'll worry about shadow dom and the like later (likely revive UiConfig,
        // make the impl accept a platform-specific subtype, and wire it in "officially").
        // For now, we're just going to hack in something that works...

        // TODO: have a base method for getService()
        assemble.patternln("$1().addCss(", assemble.addImportStatic(X_Elemental.class, "getElementalService"));
        // TODO: check attr() for method calls / references, to know if we need to bind dynamic styles...
        String prefix = "  ";
        for (String line : readLines(attr)) {
            boolean putBack = line.endsWith(";");
            assemble.print(prefix).println(X_Source.javaQuote(line + (putBack ? ";" : "")));
            prefix = "+ ";
        }
        assemble.println(", 0);");

    }

    private MappedIterable<String> readLines(UiAttrExpr attr) {
        final Expression expr = attr.getExpression();
        if (expr instanceof CssBlockExpr) {
            // TODO(later): something that emits nicer, structured code, plus respects dynamic values.
            final List<CssContainerExpr> containers = ((CssBlockExpr) expr).getContainers();
            final Transformer transformer = new Transformer();
            transformer.setShouldQuote(false);
            return MappedIterable.mapped(containers)
                .map2(CssContainerExpr::toSource, transformer)
                .flatten(s->ArrayIterable.iterate(s.split("\n")))
                .map(String::trim);
        }
        return ArrayIterable.iterate(attr.getExpression().toSource().split("\n"));
    }
}
