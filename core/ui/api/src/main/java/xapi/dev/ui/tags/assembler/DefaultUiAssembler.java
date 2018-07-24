package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.ui.api.GeneratedUiBase;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.GeneratedUiLayer;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.UiTagUiGenerator;
import xapi.dev.ui.tags.UiTagUiGenerator.UiTagGeneratorVisitor;
import xapi.fu.Do;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.iterate.ChainBuilder;
import xapi.source.X_Modifier;
import xapi.util.X_String;

import java.util.Locale;

/**
 * A default ui assembler which simply inspects the tag definition,
 * generates the correct
 *
 * Created by James X. Nelson (james @wetheinter.net) on 2/26/18.
 */
public class DefaultUiAssembler implements UiAssembler {

    private final UiGeneratorTools tools;
    private final GeneratedUiComponent component;
    private final GeneratedUiLayer layer;
    private final ChainBuilder<String> rootRefs;
    private final UiTagGeneratorVisitor owner;
    private final Maybe<Expression> refNode;
    private final GeneratedUiBase baseClass;
    private final ClassBuffer out;

    public DefaultUiAssembler(
        UiTagGeneratorVisitor owner,
        Maybe<Expression> refNode,
        UiGeneratorTools tools,
        GeneratedUiComponent component,
        GeneratedUiLayer layer,
        ChainBuilder<String> rootRefs
    ) {
        this.owner = owner;
        this.refNode = refNode;
        this.tools = tools;
        this.component = component;
        this.layer = layer;
        this.rootRefs = rootRefs;
        baseClass = component.getBase();
        out = baseClass.getSource().getClassBuffer();
    }

    @Override
    public UiAssemblerResult generateAssembly(
        UiTagUiGenerator owner, UiContainerExpr n, UiAssembler parent
    ) {
        boolean isRoot = parent == null;

        // (almost) Everything currently in UiTagUiGenerator should go in here,
        // and then we will start pulling pieces out into more maintainable chunks as we go

        if (isRoot) {
            // Whenever we are root, we must record a root fieldname to use,
            // and generate that fieldname if no explicit ref is defined.
            return processAsRoot(owner, n);
        } else {
            return processAsChild(owner, n, parent);
        }
//
//        String parentRefName = refFieldName;
//        final Do undo = registerNode(n, refNode, isRoot);
//        boolean compact = owner.requireCompact(n);
//
//        final Maybe<Expression> modelNode = n.getAttribute(UiNamespace.ATTR_MODEL)
//            .mapNullSafe(UiAttrExpr::getExpression);
//
//        String type = baseClass.getElementBuilderType(namespace);
//        boolean printedVar = false;
//        if (modelNode.isPresent()) {
//            // When a modelNode is present, we should delegate to a generated element's toDom method.
//            final Expression modelExpr = modelNode.get();
//            Expression nodeToUse = tools.resolveVar(ctx, modelExpr);
//            if (nodeToUse != null) {
//                if (!nodeToUse.hasExtra(EXTRA_MODEL_INFO)) {
//                    nodeToUse = owner.resolveReference(tools, ctx, component, baseClass, rootRefField, nodeToUse, false);
//                }
//                if (nodeToUse.hasExtra(EXTRA_MODEL_INFO)) {
//                    // yay, some model info for us!
//                    // final GeneratedUiField modelInfo = nodeToUse.getExtra(EXTRA_MODEL_INFO);
//                    ComponentBuffer otherTag = tools.getGenerator().getComponent(ctx, n.getName());
//                    // We'll want to defer to a factory method on the other component.
//                    if (otherTag == null) {
//                        final GeneratedUiDefinition info = tools.getDefinition(ctx, n.getName());
//                        if (info == null) {
//                            throw new NotConfiguredCorrectly("No definition found for " + n.getName());
//                        }
//                        toDom.print(type + " " + refFieldName + " = create" + info.getApiName() + "(");
//
//                        // do the grunt work of getTagFactory, but w/out all the mess
//                        final MethodBuffer baseMethod = baseClass.getSource().getClassBuffer()
//                            .createMethod("protected abstract " + baseClass.getElementBuilderType(namespace) + " create" + info.getApiName() + "();");
//
//                        GeneratedUiMember member = nodeToUse.getExtra(EXTRA_MODEL_INFO);
//                        if (nodeToUse instanceof NameExpr) {
//                            assert member != null : "Cannot use a name expression without EXTRA_MODEL_INFO: " + tools.debugNode(nodeToUse);
//                            // a local var reference will need to be passed to the method.
//                            final String sourceName = ((NameExpr)nodeToUse).getQualifiedName();
//                            final Type memberType = member.getMemberType();
//                            final String typeName = tools.getComponentType(nodeToUse, memberType);
//                            baseMethod.addParameter(typeName, sourceName);
//                            toDom.println(sourceName + ");");
//                        } else if (nodeToUse instanceof MethodCallExpr){
//                            final MethodCallExpr asMethod = (MethodCallExpr) nodeToUse;
//                            assert member != null : "Cannot use a method call expression without EXTRA_MODEL_INFO: " + tools.debugNode(nodeToUse);
//
//                            final String sourceName = X_String.debean(asMethod.getName());
//                            final Type memberType = member.getMemberType();
//                            final String typeName = tools.getComponentType(nodeToUse, memberType);
//                            baseMethod.addParameter(typeName, sourceName);
//
//                            final String literal = tools.resolveLiteral(ctx, nodeToUse);
//                            toDom.print(literal);
//                            toDom.println(");");
//                        } else {
//                            assert false : "Unhandled node " + nodeToUse.getClass()+" : " + tools.debugNode(nodeToUse);
//                        }
//
//                        component.getImpls()
//                            .forAll(GeneratedUiImplementation::addChildFactory, info, nodeToUse);
//
//                    } else {
//                        if (!nodeToUse.hasExtra(EXTRA_MODEL_INFO)) {
//                            nodeToUse = owner.resolveReference(tools, ctx, component, baseClass, rootRefField, nodeToUse, false);
//                        }
//                        MethodCallExpr factoryMethod = otherTag.getTagFactory(tools, ctx, component, namespace, n, nodeToUse, toDom);
//                        String initializer = tools.resolveString(ctx, factoryMethod);
//                        toDom.println(type + " " + refFieldName + " = " + initializer + ";");
//                    }
//                    printedVar = true;
//                } else {
//                    // The model node was not a named key; it might be a json literal...
//                    X_Log.info(DefaultUiAssembler.class, "");
//                }
//            }
//        }
//
//        try {
//
//            if (printedVar) {
//                if (n.getBody() != null) {
//                    n.getBody().accept(this, n);
//                }
//            } else {
//                toDom.println(type + " " + refFieldName + " = " + newBuilder.out1() + ";")
//                    .println(refFieldName + ".append(\"<" + n.getName() +
//                        (compact ? "" : ">") + "\");");
//                try {
//                    // tricky... each ui container sends itself to its children as parent argument;
//                    // the visitor will visit all children in n with n as the nearest container parent.
//                    super.visit(n, n);
//                } finally {
//                    if (compact) {
//                        toDom.println(refFieldName + ".append(\" />\");");
//                    } else {
//                        toDom.println(refFieldName + ".append(\"</" + n.getName() + ">\");");
//                    }
//                }
//            }
//
//        } finally {
//            if (parentRefName != null) {
//                toDom.println(parentRefName+".addChild(" + refFieldName + ");");
//            }
//            undo.done();
//            refFieldName = parentRefName;
//        }

    }

    protected UiAssemblerResult processAsChild(UiTagUiGenerator owner, UiContainerExpr n, UiAssembler parent) {

        switch (n.getName().toLowerCase(Locale.US)) {
            case "if":
                // a nested conditional.  fun!  ...bake logic into parent container.
                break;
            case "for":
                // a nested loop... bake logic into parent container.
                break;
        }
        final UiAssemblerResult result = new UiAssemblerResult();

        processNode(parent, result, owner, n);
        return result;
    }

    protected UiAssemblerResult processAsRoot(UiTagUiGenerator owner, UiContainerExpr n) {
        final UiAssemblerResult result = new UiAssemblerResult();
        switch (n.getName().toLowerCase(Locale.US)) {
            case "if":
                // a root conditional.  fun!
                break;
            case "for":
                // a root loop... implicitly document fragment -like behavior
                break;
        }
        this.owner.setRootRef(refNode);

        processNode(null, result, owner, n);

        return result;
    }

    private void processNode(
        UiAssembler parent,
        UiAssemblerResult result,
        UiTagUiGenerator owner,
        UiContainerExpr n
    ) {

        String lazy = out.addImport(Lazy.class);
        String builder = baseClass.getElementBuilderType(owner.getNamespace());

        final FieldBuffer refField = out.createField(
            out.parameterizedType(lazy, builder),
            this.owner.refFieldName
        ).setModifier(X_Modifier.PRIVATE);

        final MethodBuffer initEl = out.createMethod(
            X_Modifier.PROTECTED,
            out.parameterizedType(lazy, builder),
            "init" + X_String.toTitleCase(this.owner.refFieldName)
        );

        refField.setInitializer(lazy + ".deferred1(this::" + initEl.getName() + ");");

        initEl.println(builder + " b = " + this.owner.getOwner().newBuilder() + ";");
        PrintBuffer initBuffer = new PrintBuffer();
        initEl.addToEnd(initBuffer);
        initEl.returnValue("b");

        ElementAssembly assembly = new ElementAssembly(refField.getName(), initBuffer);
        result.setAssembly(assembly);

        Do undo  = component.registerRef(this.owner.rootRef, refField);
        result.onRelease(undo);

    }
}
