package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import xapi.dev.api.ApiGeneratorContext;
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
import xapi.util.X_String;

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

        UiAssemblerResult res = new UiAssemblerResult();
        res.setFactory(factory);

        final GeneratedUiComponent component = assembly.getUi();
        final GeneratedUiBase baseClass = component.getBase();
        final UiNamespace namespace = assembly.getNamespace();
        final UiGeneratorTools tools = assembly.getTools();
        final UiTagGenerator generator = assembly.getGenerator();
        final ApiGeneratorContext ctx = assembly.getContext();
        final UiContainerExpr n = e.getAst();
        final Maybe<Expression> modelNode = n.getAttribute(UiNamespace.ATTR_MODEL)
            .mapNullSafe(UiAttrExpr::getExpression);

        String type = baseClass.getElementBuilderType(namespace);
        boolean printedVar = false;
        PrintBuffer toDom = e.getInitBuffer();
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
                        toDom.println("return " + initializer + ";");
                    }
                    printedVar = true;
                } else {
                    // The model node was not a named key; it might be a json literal...
                    X_Log.info(UiTagUiGenerator.class, "Model node was not a named key", tools.debugNode(modelExpr));
                }
            }
        }

        try {

            if (printedVar) {
                visitBody(assembler, n, e, res);
            } else {
                if (shouldPrintTagName(n, res)) {
                    toDom.println(factory.getVarName() + ".setTagName(\"" + n.getName() + "\");");
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
            for (Expression child : n.getBody().getChildren()) {
                if (child instanceof JsonContainerExpr) {
                    final JsonContainerExpr json = (JsonContainerExpr) child;
                    assert json.isArray() : "Object type bodies not supported!";
                    json.getValues().forAll(expr -> {
                        if (!(expr instanceof UiContainerExpr)) {
                            throw new IllegalArgumentException("Cannot handle " + assembly.getTools().debugNode(child));
                        }
                        final UiAssemblerResult result = assembler.addChild(assembly, e, (UiContainerExpr) expr);
                        res.adopt(result);
                    });
                } else if (child instanceof TemplateLiteralExpr) {
                    String lit = ((TemplateLiteralExpr)child).getValueWithoutTicks();
                    e.getInitBuffer()
                        .printlns(AssembledElement.BUILDER_VAR+".append(" + X_Source.javaQuote(lit) + ");");
                } else if (child instanceof UiContainerExpr) {
                    final UiAssemblerResult result = assembler.addChild(assembly, e, (UiContainerExpr) child);
                    res.adopt(result);
                } else {
                    throw new IllegalArgumentException("Cannot handle " + assembly.getTools().debugNode(child));
                }
            }

        }
    }
}
