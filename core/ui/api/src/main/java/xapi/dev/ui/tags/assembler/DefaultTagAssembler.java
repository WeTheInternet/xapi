package xapi.dev.ui.tags.assembler;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.lang.gen.GeneratedUiMember;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.UiTagGenerator;
import xapi.dev.ui.tags.UiTagUiGenerator;
import xapi.dev.ui.tags.factories.GeneratedFactory;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.Maybe;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.string.X_String;

import static xapi.dev.ui.api.UiConstants.EXTRA_MODEL_INFO;

/**
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/28/18.
 */
public class DefaultTagAssembler implements TagAssembler {

    private final AssembledUi assembly;

    public DefaultTagAssembler(AssembledUi assembly) {
        this.assembly = assembly;
    }

    public UiAssemblerResult visit(
        UiAssembler assembler,
        GeneratedFactory factory,
        AssembledElement e
    ) {

        // TODO: route a GeneratedUiDefinition into something we can use, like:
//        assembler.getDefinition() // doesn't exist yet
        UiAssemblerResult res = UiAssemblerResult.result(null, factory, e);

        boolean printedVar = checkModelNode(e);

        final UiContainerExpr n = e.getAst();

        try {

            if (printedVar) {
                visitBody(assembler, n, e, res);
            } else {
                if (shouldPrintTagName(n, res)) {
                    res.addDefaultBehavior(()->{
                        PrintBuffer toDom = e.getInitBuffer();
                        toDom.println(factory.getVarName() + ".setTagName(\"" + n.getName() + "\");");
                    });
                }
                visitBody(assembler, n, e, res);
            }

        } finally {
//            if (!e.isParentRoot()) {
//                component.getMethods().elementResolved(namespace)
//                    .beforeResolved().println(
//                    e.getParent().requireRef()+".out1().addChild(" + e.requireRef() + ".out1());"
//                );
//            }
        }



        return res;
    }

    public boolean checkModelNode(AssembledElement e) {
        final UiContainerExpr n = e.getAst();

        final Maybe<Expression> modelNode = n.getAttribute(UiNamespace.ATTR_MODEL)
            .mapNullSafe(UiAttrExpr::getExpression);

        final UiGeneratorTools tools = assembly.getTools();
        final UiTagGenerator generator = assembly.getGenerator();
        final ApiGeneratorContext ctx = assembly.getContext();
        final GeneratedUiComponent component = assembly.getUi();

        final GeneratedUiBase baseClass = component.getBase();
        final UiNamespace namespace = assembly.getNamespace();


        String type = baseClass.getElementBuilderType(namespace);


        boolean printedVar = false;
        if (modelNode.isPresent()) {
            // When a modelNode is present, we should delegate to a generated element's toDom method.
            final Expression modelExpr = modelNode.get();
            Expression nodeToUse = tools.resolveVar(ctx, modelExpr);
            X_Log.info(UiTagUiGenerator.class, "Found model node", tools.debugNode(modelExpr));
            if (nodeToUse != null) {
                if (!nodeToUse.hasExtra(EXTRA_MODEL_INFO)) {
                    nodeToUse = generator.resolveReference(tools, ctx, component, baseClass, e.maybeRequireRefRoot(), e.maybeRequireRef(), nodeToUse, false);
                }
                // TODO: change the mess below into something that understands using a generated component's generated builder.
                if (nodeToUse.hasExtra(EXTRA_MODEL_INFO)) {
                    // yay, some model info for us!
                    ComponentBuffer otherTag = tools.getGenerator().getComponent(ctx, n.getName());
                    // We'll want to defer to a factory method on the other component.
                    if (otherTag == null) {
                        PrintBuffer toDom = e.getInitBuffer();

                        final GeneratedUiDefinition info = tools.getDefinition(ctx, n.getName());
                        if (info == null) {
                            throw new NotConfiguredCorrectly("No definition found for " + n.getName());
                        }
                        toDom.print("return create" + info.getApiName() + "(");

                        // do the grunt work of getTagFactory, but w/out all the mess
                        final MethodBuffer baseMethod = baseClass.getSource().getClassBuffer()
                            .createMethod("protected abstract " + type + " create" + info.getApiName() + "();");

                        GeneratedUiMember member = nodeToUse.getExtra(UiConstants.EXTRA_MODEL_INFO);
                        if (nodeToUse instanceof NameExpr) {
                            assert member != null : "Cannot use a name expression without EXTRA_MODEL_INFO: " + tools.debugNode(nodeToUse);
                            // a local var reference will need to be passed to the method.
                            final String sourceName = ((NameExpr)nodeToUse).getQualifiedName();
                            final Type memberType = member.getMemberType();
                            final String typeName = tools.getComponentType(nodeToUse, memberType);
                            baseMethod.addParameter(typeName, sourceName);
                            toDom.println(sourceName + ");");
                        } else if (nodeToUse instanceof MethodCallExpr){
                            final MethodCallExpr asMethod = (MethodCallExpr) nodeToUse;
                            assert member != null : "Cannot use a method call expression without EXTRA_MODEL_INFO: " + tools.debugNode(nodeToUse);

                            final String sourceName = X_String.debean(asMethod.getName());
                            final Type memberType = member.getMemberType();
                            final String typeName = tools.getComponentType(nodeToUse, memberType);
                            baseMethod.addParameter(typeName, sourceName);

                            final String literal = tools.resolveLiteral(ctx, nodeToUse);
                            toDom.print(literal);
                            toDom.println(");");
                        } else {
                            assert false : "Unhandled node " + nodeToUse.getClass()+" : " + tools.debugNode(nodeToUse);
                        }

                        component.getImpls()
                            .forAll(GeneratedUiImplementation::addChildFactory, info, nodeToUse);

                    } else {
                        if (!nodeToUse.hasExtra(EXTRA_MODEL_INFO)) {
                            nodeToUse = generator.resolveReference(tools, ctx, component, baseClass, e.maybeRequireRefRoot(), e.maybeRequireRef(), nodeToUse, false);
                        }
                        MethodCallExpr factoryMethod = otherTag.getTagFactory(tools, ctx, component, namespace, n, nodeToUse);
                        String initializer = tools.resolveString(ctx, factoryMethod);
                        e.getInitBuffer().println("return " + initializer + ";");
                    }
                    printedVar = true;
                } else {
                    // The model node was not a named key; it might be a json literal...
                    X_Log.info(UiTagUiGenerator.class, "Model node was not a named key", tools.debugNode(modelExpr));
                }
            }
        }
        return printedVar;
    }

    protected boolean shouldPrintTagName(UiContainerExpr n, UiAssemblerResult res) {
        switch (n.getName().toLowerCase()) {
            case "if":
            case "for":
                return false;
        }
        // TODO: something better / configurable.
        return res.getFactory().hasVar();
    }

    private void visitBody(
        UiAssembler assembler,
        UiContainerExpr n,
        AssembledElement e,
        UiAssemblerResult res
    ) {
        if (n.getBody() != null) {
            // instead of actually printing the code,
            // we just set the "default behavior if unhandled"
            // perhaps put an extra on the ast node?  ...for now, we have the UiAssemblerResult in scope.
            res.addDefaultBehavior(()->{
                final PrintBuffer init = e.getInitBuffer();
                ComposableXapiVisitor<Boolean> visitor = ComposableXapiVisitor.whenMissingFail(DefaultTagAssembler.class);
                visitor
                    .withJsonContainerRecurse((json, first)->{
                        assert json.isArray() : "Object type bodies not supported!";
                        // TODO: some extra wiring to handle multiple children?
                    })
                    .withMethodReferenceTerminal((mthd, first)->{
                        if (first) {
                            init.println(AssembledElement.BUILDER_VAR+".append(").indent();
                        }
                        final Expression resolved = e.resolveRef(e, mthd, false);
                        // TODO: smart argument binding...
                        init.printlns(resolved.toSource());
                        if (first) {
                            init.outdent().println(");");
                        }

                    })
                    .withTemplateLiteralTerminal((template, first)->{
                        String lit = template.getValueWithoutTicks();
                        if (first) {
                            init.println(AssembledElement.BUILDER_VAR+".append(").indent();
                        }
                        // If this literal is a method reference or method call, then we should
                        // treat it as a supplier of an appendable value...
                        if (lit.contains("::") || (lit.contains(".") && lit.contains("("))) {
                            try {
                                final Expression parsed = JavaParser.parseExpression(lit);
                                // yay, we were parsed as some valid expression... visit it...
                                parsed.accept(visitor, false);
                                if (first) {
                                    init.outdent().println(");");
                                }
                                return;
                            } catch (ParseException invalid) {
                                // ignored... we will just serialize it into the body
                            }
                        }
                        init.printlns(X_Source.javaQuote(lit));
                        if (first) {
                            init.outdent().println(");");
                        }
                    })
                .withUiContainerTerminal((ui, first) -> {
                    // when we aren't first, we'll need to do some fancy footwork to generate correct code
                    // (most likely, removing the lazy chaining and actually assign local variables for everything)
                    final UiAssemblerResult result = assembler.addChild(assembly, e, ui);
                    res.adopt(result);
                })
                ;
                for (Expression child : n.getBody().getChildren()) {
                    child.accept(visitor, true);
                }
            });
        }
    }
}
