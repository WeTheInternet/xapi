package xapi.dev.components;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.plugin.Transformer;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.api.GeneratedTypeOwner;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.assembler.AssembledElement;
import xapi.dev.ui.tags.assembler.AssembledUi;
import xapi.dev.ui.tags.assembler.UiAssembler;
import xapi.elemental.X_Elemental;
import xapi.fu.*;
import xapi.fu.api.Builderizable;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SingletonIterator;
import xapi.inject.X_Inject;
import xapi.source.util.X_Modifier;
import xapi.source.X_Source;

import java.util.List;

import static com.github.javaparser.ast.visitor.ComposableXapiVisitor.whenMissingFail;
import static xapi.fu.X_Fu.weakener;
import static xapi.source.util.X_Modifier.PUBLIC;
import static xapi.source.util.X_Modifier.STATIC;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/8/17.
 */
public class GeneratedWebComponent extends GeneratedUiImplementation {

    private final Lazy<UiNamespaceGwt> namespace;
    private FieldBuffer extractor;
    private MethodBuffer assemble;
    private FieldBuffer creator;

    public GeneratedWebComponent(GeneratedTypeOwner ui) {
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
        UiAssembler assembler, UiAttrExpr attr, AssembledElement el, MethodBuffer out
    ) {
        final AssembledUi assembly = assembler.getAssembly();
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
            case "css":
            case "class":
            case "classname":
                // bind up some css!
                // ...should just call the standard css generator from visitor,
                // and then add an additional "set classname" operation

                // hm.... this _should_ be pretty universal though,
                // so lets consider moving this out (a la UiFeatureGenerator...),
                // instead of squirreled away here, on native-element. (though, maybe this _is_ better,
                // if shadow dom is used for native rendering)
                final Expression resolved = el.resolveRef(el.getParent(), attr);
                Mutable<String> clsToUse = new Mutable<>();
                SysExpr dynamicCls = new SysExpr(clsToUse
                    .mapIf(In1Out1.checkIsNotNull(), TemplateLiteralExpr::templateLiteral, nul->TemplateLiteralExpr.templateLiteral("cls"))
                );
                final Do undo = ctx.addToContext("class", dynamicCls).once();
                try {
                    final ComposableXapiVisitor<Object> visitor = whenMissingFail(GeneratedWebComponent.class);
                    final In2<Expression, Object> stringishNode = (Expression node, Object arg) -> {
                        assert clsToUse.out1() == null;
                        String cls = el.resolveSource(el.getParent(), node);
                        clsToUse.in(X_Source.javaQuote(cls));
                    };
                    visitor
                        .withJsonContainerTerminal((json, arg)->{
                            // for json container, if it's an array, we blindly visit elements.
                            // if it's a map, we use the keys for classnames, with values being css
                            if (json.isArray()) {
                                // just visit each element...
                                json.getPairs().forEach(pair->pair.getValueExpr().accept(visitor, arg));
                            } else {
                                for (JsonPairExpr pair : json.getPairs()) {
                                    String cls = assembly.getTools().resolveString(ctx, pair.getKeyExpr());
                                    assert !cls.contains(" ") : name + " = { " + cls + " : ... } is not valid (keys cannot contain spaces)";
                                    clsToUse.set(cls);
                                    pair.getValueExpr().accept(visitor, arg);
                                }
                                clsToUse.in(null);
                            }
                        })
                        .withCssBlockExpr((block, arg)->{
                            // inside the .{ } block expression, we'll bind up $class
                            // which, if used, will trigger generating and assigning a classname.
                            // For now, we'll just cheat a touch, and print the whole thing right now.
                            assembly.getUi().beforeSave(serv->{
                                addCss(block);
                            });
                            return false;
                        })
                        .withCssContainerExpr((container, arg)->{
                            // an inline container; if it defines a single class selector, we will add that class to this element.
                            if (container.getSelectors().size() == 1) {
                                final List<String> parts = container.getSelectors().get(0).getParts();
                                if (parts.size() == 1) {
                                    final String part = parts.get(0);
                                    if (part.startsWith(".")) {
                                        clsToUse.in(X_Source.javaQuote(part.substring(1)));
                                    }
                                }
                            }
                            assembly.getUi().beforeSave(serv->{
                                addCss(container);
                            });
                            return false;
                        })
                        .withStringLiteralTerminal(stringishNode.map1(weakener()))
                        .withTemplateLiteralTerminal(stringishNode.map1(weakener()))
                        .withNameTerminal(stringishNode.map1(weakener()))
                        .withQualifiedNameTerminal(stringishNode.map1(weakener()))
                    ;
                    resolved.accept(visitor, null);
                    attr.setExpression(dynamicCls);
                    final String src = el.resolveSource(el.getParent(), dynamicCls);
                    out.patternln(".setClass($1)", src);
                    break;
                }finally {
                    // release scope variables; don't want other code addidentally seeing our values.
                    undo.done();
                }
            default:
                // just copy the attributes over...
                super.resolveNativeAttr(assembler, attr, el, out);
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
    public void addCss(Expression cssExpr) {
        if (cssExpr instanceof UiAttrExpr) {
            cssExpr = ((UiAttrExpr) cssExpr).getExpression();
        }
        // For now, we'll just dump the css text into a new stylesheet and inject it.
        // we'll worry about shadow dom and the like later (likely revive UiConfig,
        // make the impl accept a platform-specific subtype, and wire it in "officially").
        // For now, we're just going to hack in something that works...

        // TODO: have a base method for getService()
        assemble.patternln("$1().addCss(", assemble.addImportStatic(X_Elemental.class, "getElementalService"));
        // TODO: check attr() for method calls / references, to know if we need to bind dynamic styles...
        String prefix = "  ";
        for (String line : readCssLines(cssExpr)) {
            boolean putBack = line.endsWith(";");
            assemble.print(prefix).println(X_Source.javaQuote(line + (putBack ? ";" : "")));
            prefix = "+ ";
        }
        assemble.println(", 0);");

    }

    private MappedIterable<String> readCssLines(Expression expr) {
        if (expr instanceof CssBlockExpr) {
            // TODO(later): something that emits nicer, structured code, plus respects dynamic values.
            final List<CssContainerExpr> containers = ((CssBlockExpr) expr).getContainers();
            final Transformer transformer = new Transformer();
            transformer.setShouldQuote(false);
            return MappedIterable.mapped(containers)
                .map2(CssContainerExpr::toSource, transformer)
                .flatten(s->ArrayIterable.iterate(s.split("\n")))
                .map(String::trim);
        } else if (expr instanceof CssContainerExpr) {
            final Transformer transformer = new Transformer();
            transformer.setShouldQuote(false);
            return SingletonIterator.singleItem(expr.toSource(transformer).trim());
        }
        return ArrayIterable.iterate(expr.toSource().split("\n"));
    }
}
