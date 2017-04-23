package xapi.dev.ui.tags;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.except.NotYetImplemented;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.Mutable;
import xapi.fu.X_Fu;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;

import static xapi.dev.ui.api.UiConstants.EXTRA_MODEL_INFO;
import static xapi.fu.Out1.out1Deferred;
import static xapi.source.X_Source.javaQuote;

/**
 * Responsible for handling <define-tag ui= /> feature.
 *
 * This feature contains the glue of elements used to display to the user;
 * for web components, shadow dom will be used if any <select /> elements appear.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 2/11/17.
 */
public class UiTagUiGenerator extends UiFeatureGenerator {

    private class UiContainerExprVoidVisitorAdapter extends VoidVisitorAdapter<UiContainerExpr> {

        private final GeneratedUiComponent component;
        private final UiNamespace namespace;
        private final MethodBuffer toDom;
        private final UiGeneratorTools tools;
        private final ApiGeneratorContext ctx;
        private final GeneratedUiBase baseClass;
        private final ChainBuilder<String> rootRefs;
        private final ClassBuffer out;
        private final ContainerMetadata me;
        public In1<String> refNameSpy;
        public String refFieldName;
        public String rootRef; // The name of the ref in the xapi source
        public String rootRefField; // The name of the ref field in generated java
        Lazy<String> newBuilder;

        public UiContainerExprVoidVisitorAdapter(
            GeneratedUiComponent component,
            MethodBuffer toDom,
            UiGeneratorTools tools,
            ChainBuilder<String> rootRefs,
            ContainerMetadata me
        ) {

            this.component = component;
            this.namespace = tools.namespace();
            this.toDom = toDom;
            this.tools = tools;
            this.ctx = me.getContext();
            this.baseClass = component.getBase();
            this.rootRefs = rootRefs;
            this.out = baseClass.getSource().getClassBuffer();
            this.me = me;
            refNameSpy = In1.ignored();
            newBuilder = Lazy.deferred1(component::getElementBuilderConstructor, namespace);
        }

        @Override
        public void visit(UiContainerExpr n, UiContainerExpr arg) {
            boolean isRoot = arg == null;

            final Maybe<Expression> refNode = n.getAttribute(UiNamespace.ATTR_REF)
                .mapNullSafe(UiAttrExpr::getExpression);
            switch (n.getName().toLowerCase()) {
                case "if":
                    // if tag is special, it will wrap children into if/else block
                    handleIfTag(n, arg);
                    return;
                case "for":
                    // for tag is also special; we will unfold the underlying item type,
                    // generating a for loop in the code, with the as=Name property
                    // available for reference, by name, in shared context.
                    handleForTag(n, arg);
                    return;
                case "native-element":
                    // native elements are special; we are going to ask the current
                    // component to just leave an abstract method for implementors to fill in.
                    handleNativeTag(n, arg, refNode);
                    return;
            }
            String parentRefName = refFieldName;
            // TODO: if the container is a known xapi component,
            // then ask that component how it wants to render this node.
            // For now we just want something that works to iterate on,
            // but a future iteration should include a "ClassWorld" notion,
            // where we have whole-world knowledge available before generating code...
            assert isRoot || !refNode.isPresent() || n.getContainerParentNode() == arg;
            final Do undo = registerNode(n, refNode, isRoot);
            boolean compact = owner.requireCompact(n);

            final Maybe<Expression> modelNode = n.getAttribute(UiNamespace.ATTR_MODEL)
                .mapNullSafe(UiAttrExpr::getExpression);

            String type = baseClass.getElementBuilderType(namespace);
            boolean printedVar = false;
            if (modelNode.isPresent()) {
                // When a modelNode is present, we should delegate to a generated element's toDom method.
                final Expression modelExpr = modelNode.get();
                final Expression nodeToUse = tools.resolveVar(ctx, modelExpr);
                if (nodeToUse != null && nodeToUse.hasExtra(EXTRA_MODEL_INFO)) {
                    // yay, some model info for us!+
                    //                                final GeneratedUiField modelInfo = nodeToUse.getExtra(EXTRA_MODEL_INFO);
                    ComponentBuffer otherTag = tools.getComponentInfo(n.getName());
                    // We'll want to defer to a factory method on the other component.
                    MethodCallExpr factoryMethod = otherTag.getTagFactory(tools, ctx, component, namespace, n);
                    String initializer = tools.resolveString(ctx, factoryMethod);
                    toDom.println(type + " " + refFieldName + " = " + initializer + ";");
                    printedVar = true;
                } else {
                    // The model node was not a named key; it might be a json literal...
                }
            }

            try {

                if (printedVar) {
                    if (n.getBody() != null) {
                        n.getBody().accept(this, n);
                    }
                } else {
                    toDom.println(type + " " + refFieldName + " = " + newBuilder.out1() + ";")
                        .println(refFieldName + ".append(\"<" + n.getName() +
                            (compact ? "" : ">") + "\");");
                    try {
                        // tricky... each ui container sends itself to its children as parent argument;
                        // the visitor will visit all children in n with n as the nearest container parent.
                        super.visit(n, n);
                    } finally {
                        if (compact) {
                            toDom.println(refFieldName + ".append(\" />\");");
                        } else {
                            toDom.println(refFieldName + ".append(\"</" + n.getName() + ">\");");
                        }
                    }
                }

            } finally {
                if (parentRefName != null) {
                    toDom.println(parentRefName+".addChild(" + refFieldName + ");");
                }
                undo.done();
                refFieldName = parentRefName;
            }
        }

        private Do registerNode(UiContainerExpr n, Maybe<Expression> refNode, boolean isRoot) {
            if (isRoot) {
                // we force a ref to any root nodes.
                // note that this can happen multiple times,
                // by parsing something like [ <one />, <two />, ... ]

                setRootRef(refNode);
                final FieldBuffer refField = out.createField(
                    baseClass.getElementBuilderType(namespace),
                    refFieldName
                );
                return component.registerRef(rootRef, refField);
            } else {
                if (refNode.isPresent()) {
                    String ref = tools.resolveString(ctx, refNode.get());
                    refFieldName = baseClass.newFieldName(ref);
                    final FieldBuffer refField = out.createField(
                        baseClass.getElementBuilderType(namespace),
                        refFieldName
                    );
                    component.registerRef(ref, refField);
                } else {
                    refFieldName = me.newVarName("el" + tools.tagToJavaName(n));
                }
                return Do.NOTHING;
            }
        }

        private void setRootRef(Maybe<Expression> refNode) {
            final Expression refExpr = refNode.ifAbsentSupply(
                out1Deferred(
                    TemplateLiteralExpr::templateLiteral,
                    "root"
                )
            );
            rootRef = tools.resolveString(ctx, refExpr);
            rootRefField = refFieldName = baseClass.newFieldName(rootRef);
            refNameSpy.in(rootRefField);
            rootRefs.add(rootRefField);
        }

        private void handleNativeTag(UiContainerExpr n, UiContainerExpr arg, Maybe<Expression> refNode) {
            final boolean isRoot = arg == null;
            final String parentRef = refFieldName;
            final Do undo = registerNode(n, refNode, isRoot);
            try {
                component.createNativeFactory(n, toDom, namespace, refFieldName);
            } finally {
                if (parentRef != null) {
                    toDom.println(parentRef + ".addChild(" + refFieldName + ");");
                }
                undo.done();
                refFieldName = parentRef;
            }

        }

        private void handleForTag(UiContainerExpr n, UiContainerExpr arg) {
            toDom.print("for (");
            n.getAttribute("allOf")
                .readIfPresent(all->{
                    final Expression startExpr = all.getExpression();
                    owner.resolveReference(tools, ctx, component, baseClass, rootRefField, all.getExpression());
                    final Expression endExpr = all.getExpression();

                    final Maybe<UiAttrExpr> index = n.getAttribute("index");

                    // TODO an alternative to as which exposes the index as well/instead
                    n.getAttribute("as")
                        .readIfPresent(as->{
                            final String asName = tools.resolveString(ctx, as.getExpression());
                            // Ok! We have allOf=someData, likely $model.fieldName,
                            // and an as=name to put into the context.

                            // Lets find out what type our elements are,
                            // open our for loop, register the model information,
                            // and then visit the children of the <for /> tag
                            if (endExpr instanceof ScopedExpression) {
                                // probably a $model/$data reference...
                                Expression allOfRoot = ((ScopedExpression) endExpr)
                                    .getRootScope();
                                if (owner.isModelReference(allOfRoot)) {
                                    GeneratedUiField modelInfo = (GeneratedUiField) endExpr.getExtras().get(
                                        EXTRA_MODEL_INFO);
                                    if (modelInfo == null) {
                                        throw new IllegalArgumentException(
                                            "Complex model expressions not yet supported by <for /> tag."
                                                +"\nYou sent " + tools.debugNode(startExpr) + "; which was converted to " + tools.debugNode(endExpr));
                                    }
                                    String type = toDom.addImport(ASTHelper.extractGeneric(modelInfo.getMemberType()));
                                    String call = tools.resolveString(ctx, endExpr);
                                    if (modelInfo.getMemberType().hasRawType("IntTo")) {
                                        call = call + ".forEach()"; // ew.  But. well, it is what it is.
                                        // TODO: abstract this into something that understands the member type
                                    }
                                    toDom.append(type).append(" ").append(asName)
                                        .append(" : ").append(call).println(") {")
                                        .indent();

                                    // Save our vars to ctx state.
                                    // Since we are running inside a for loop,
                                    // we want the var to point to the named instance we just used.
                                    NameExpr var = new NameExpr(asName);
                                    var.setExtras(endExpr.getExtras());
                                    Do undo = ctx.addToContext(asName, var);
                                    // Now, we can visit children
                                    if (index.isPresent()) {
                                        String indexName = tools.resolveString(ctx, index.get().getExpression());
                                        final UiBodyExpr body = n.getBody();
                                        if (body != null) {
                                            int cnt = 0;
                                            for (Expression child : body.getChildren()) {
                                                final Do indexUndo = ctx.addToContext(
                                                    indexName,
                                                    IntegerLiteralExpr.intLiteral(cnt++)
                                                );
                                                // child.addExtra(EXTRA_GENERATE_MODE, "");
                                                child.accept(this, n);
                                                indexUndo.done();
                                            }

                                        }
                                    } else {
                                        // No index; just visit children.
                                        final UiBodyExpr body = n.getBody();
                                        if (body != null) {
                                            super.visit(body, n);
                                        }
                                    }

                                    undo.done();

                                    toDom.outdent();
                                    toDom.println("}");
                                    return;
                                }
                                throw new IllegalArgumentException("Non-model based for loop expressions not yet supported."
                                    +"\nYou sent " + tools.debugNode(startExpr) + "; which was converted to " + tools.debugNode(endExpr));
                            }
                            throw new IllegalArgumentException("Non-ScopedExpression based for loop expressions not yet supported."
                                +"\nYou sent " + tools.debugNode(startExpr) + "; which was converted to " + tools.debugNode(endExpr));
                        })
                        .readIfAbsent(noAs->{
                            throw new IllegalArgumentException("For now, all <for /> tags *must* have an as=nameToReferenceItems attribute");
                        });
                })
                .readIfAbsentUnsafe(noAllOf->{
                    throw new IllegalArgumentException("For now, all <for /> tags *must* have an allOf= attribute");
                });
        }

        @Override
        public void visit(
            UiBodyExpr n, UiContainerExpr arg
        ) {
            // When a ui body consists solely of text in the form of a java method reference,
            // or just a plain template field, we will want to treat that node specially.
            boolean first = true;
            for (Expression expr : n.getChildren()) {
                expr = tools.resolveVar(ctx, expr);
                if (expr instanceof UiContainerExpr) {
                    // An element child.  visit it.
                    visit((UiContainerExpr)expr, arg);
                    // TODO: something smart with json/css
                } else if (expr instanceof TemplateLiteralExpr) {
                    // Just toString and escape into runtime code
                    String template = tools.resolveString(ctx, expr).trim();
                    if (template.isEmpty()) {
                        continue;
                    }
                    if (template.startsWith("<")) {
                        // The template is an element; lets parse and visit it.
                        try {
                            final UiContainerExpr container = JavaParser.parseUiContainer(template);
                            container.accept(this, arg);
                            continue;
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (template.startsWith("\"")) {
                        toDom.println(refFieldName + ".append(" + javaQuote(template) + ");");
                        continue;
                    }
                    try {
                        Expression asExpr = JavaParser.parseExpression(template);
                        asExpr = tools.resolveVar(ctx, asExpr);

                        if (asExpr instanceof MethodReferenceExpr) {
                            // Method references are fun.
                            // They should return String or NodeBuilder,
                            // or else you will need to implement a `void coerceInto(ElementBuilder b, Object value)` method.
                            MethodReferenceExpr ref = (MethodReferenceExpr) asExpr;
                            String name;
                            final Expression scope = ref.getScope();
                            String scopeName = owner.getScopeType(tools, scope);
                            if (scopeName != null && scopeName.startsWith("$")) {
                                // We have a method reference to a bound name ($model, $data, $Api, $Base, $Self)
                                switch (scopeName.toLowerCase().split("<")[0]) {
                                    case "$model":
                                        // model references are excellent; they are defined by their types.
                                        if (!component.hasPublicModel()) {
                                            throw new IllegalStateException("Cannot reference $model on a type without a model={ name: Type.class } feature.");
                                        }
                                        final GeneratedUiModel model = component.getPublicModel();
                                        final GeneratedUiField field = model.getFields().get(ref.getIdentifier());
                                        if (field != null) {
                                            switch (field.getMemberType().toSource()) {
                                                case "String":
                                                case "java.lang.String":
                                                    // TODO bind the model's string property to the contents of this node
                                                    toDom.println(refFieldName + ".append(getModel().get" + field.getCapitalized()+"());");
                                                    break;
                                                default:
                                                    String elementType = baseClass.getElementType(namespace);
                                                    if (elementType.equals(field.getMemberType())) {
                                                        // a bit odd to have elements in your model, but we won't stop you...
                                                        toDom.println(refFieldName + ".addChild(getModel().get" + field.getCapitalized()+"());");
                                                    } else {
                                                        toDom.println(refFieldName + ".append(coerce(getModel().get" + field.getCapitalized()+"()));");
                                                        component.requireCoercion(field);
                                                    }
                                            }
                                        }
                                        break;
                                    case "$data":
                                    case "$api":
                                    case "$base":
                                    case "$self":
                                    default:
                                        throw new UnsupportedOperationException("No binding available for " + scopeName);
                                }
                            }

                            // Also, the use of method references implies mutability,
                            // so we are also going to want to hookup listeners to data/model references.
                        } else {
                            asExpr.accept(this, arg);
                        }
                    } catch (ParseException e) {
                        // Give up.
                        toDom.printlns(template);
                    }
                } else {

                    // More complex.  We have expression children;
                    // we are going to want to implement some SIMPLE bindings for now,
                    // and leave more complex scenarios after battle testing the simple ones.
                    if (expr instanceof NameExpr || expr instanceof LiteralExpr) {
                        // Names and literals we will just toString into code
                    } else if (expr instanceof MethodReferenceExpr) {
                    }
                }
                first = false;
            }
            // We don't visit super; anything inside the body we care about was handled.
        }

        @Override
        public void visit(
            MethodCallExpr n, UiContainerExpr arg
        ) {
            // This might be a build-time method; lets resolve it!
            final Expression resolved = tools.resolveVar(ctx, n);
            if (resolved instanceof UiContainerExpr || resolved instanceof JsonContainerExpr) {
                resolved.accept(this, arg);
                return;
            }
            boolean isUiRoot = refFieldName == null;
            // A method call / non-element before seeing an expression;
            // this method will be responsible for returning the ui.
            if (resolved instanceof MethodCallExpr) {
                // When there is a method call, we need to force a redraw when inputs are changed.
                // This is going to require careful mapping of all method references / invocations,
                // so we can limit the scope of what is redrawn and when.
                final MethodCallExpr asMethod = (MethodCallExpr) resolved;
                // Check the args for references to things we care about.
                boolean hasUnknownInput = false;
                final Expression[] myArgs = asMethod.getArgs().toArray(new Expression[asMethod.getArgs().size()]);
                for (Expression param : myArgs) {
                    owner.resolveReference(tools, ctx, component, baseClass, rootRefField, param);
                }

                Do undo = owner.resolveSpecialNames(ctx, component, baseClass, rootRefField);
                final String asString = tools.resolveString(ctx, resolved);
                undo.done();
                if (isUiRoot) {
                    toDom.returnValue(asString + (asString.endsWith(";") ? "" : ";"));
                } else {
                    toDom.printlns(refFieldName + ".append(" + asString + ");");
                }
                return;
            } else if (resolved instanceof LiteralExpr) {
                final String asString = tools.resolveString(ctx, resolved);
                if (resolved instanceof StringLiteralExpr) {
                    if (isUiRoot) {
                        toDom.returnValue(javaQuote(asString));
                    } else {
                        toDom.printlns(refFieldName + ".append(" + asString + ");");
                    }
                } else {
                    // TODO: handle way more types than this hack
                    if (isUiRoot) {
                        toDom.printlns(asString);
                    } else {
                        // TODO consider annotations to allow .addChild instead of .append.
                        toDom.printlns(refFieldName + ".append(" + asString + ");");
                    }
                }
            } else
            // TODO bind this method somehow
            //                                if (resolved instanceof MethodReferenceExpr)
            {
                throw new UnsupportedOperationException("Cannot bind " + tools.debugNode(resolved) + " to a ui body");
            }
        }

        @Override
        public void visit(UiAttrExpr n, UiContainerExpr arg) {
            if (n.getNameString().equalsIgnoreCase("children")) {
            }
            super.visit(n, arg);
        }


        private void handleIfTag(UiContainerExpr n, UiContainerExpr arg) {
            boolean isRoot = arg == null;

            // if tags are special.  We will use them to wrap the body of this container in a conditional
            toDom.print("if (");

            boolean first = true;
            for (UiAttrExpr attr : n.getAttributes()) {
                owner.resolveReference(tools, ctx, component, baseClass, rootRefField, attr.getExpression());
                final String serialized = tools.resolveString(ctx, attr.getExpression());
                // TODO handle escaping...
                if (!first) {
                    toDom.print(" &&");
                }
                switch (attr.getName().getName()) {
                    case "notNull":
                        toDom.print(serialized + " != null");
                        break;
                    case "isNull":
                        toDom.print(serialized + " == null");
                        break;
                    case "isTrue":
                        toDom.print(serialized);
                        break;
                    case "isFalse":
                        toDom.print("!" + serialized);
                        break;
                    case "isZero":
                        toDom.print(serialized + " == 0");
                        break;
                    case "isOne":
                        toDom.print(serialized + " == 1");
                        break;
                    case "isMinusOne":
                        toDom.print(serialized + " == -1");
                        break;
                    default:

                }
                first = false;
            }

            toDom.println(") {");
            toDom.indent();
            final UiBodyExpr myBody = n.getBody();
            Mutable<String> childRef = new Mutable<>();
            if (myBody != null) {
                final In1<String> was = refNameSpy;
                refNameSpy = In1.in1(i->{
                    childRef.in(i);
                    refNameSpy = was;
                });
                if (isRoot) {
                    assert myBody.getChildren()
                        .stream().filter(X_Fu::notNull)
                        .filter(e->
                            !(
                                e instanceof TemplateLiteralExpr
                                    && ((TemplateLiteralExpr)e).getValue().trim().isEmpty()
                            )
                        )
                        .count() == 1 : "An <if /> tag as the root of a ui" +
                        " must contain only one element.";
                }
                super.visit(myBody, n);
            }
            if (childRef.out1() != null) {
                toDom.returnValue(childRef.out1());
            }
            toDom.outdent();
            toDom.println("}");
        }

    }

    private final String name;
    private final String pkg;
    private final UiTagGenerator owner;


    public UiTagUiGenerator(String pkg, String name, UiTagGenerator owner) {
        this.name = name;
        this.pkg = pkg;
        this.owner = owner;
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools tools,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata me,
        UiAttrExpr ui
    ) {
        final GeneratedUiComponent component = me.getGeneratedComponent();
        boolean shadowUi = ui.getAnnotation(a->a.getNameString().equalsIgnoreCase("shadowdom")).isPresent();
        if (shadowUi || owner.alwaysUseShadowDom()) {
            // We explicitly want to generate shadow dom
            // (and inject the minimal amount of css needed)
            final Expression uiExpr = ui.getExpression();
            if (uiExpr instanceof UiContainerExpr) {
                // Look for template bindings, to figure out if we need to bind to any model fields
                MethodBuffer toDom = owner.toDomMethod(tools.namespace(), component.getBase());

                ChainBuilder<String> rootRefs = Chain.startChain();
                // Now, visit any elements, storing variables to any refs.
                uiExpr.accept(new UiContainerExprVoidVisitorAdapter(
                    component,
                    toDom,
                    tools,
                    rootRefs,
                    me), null);

                // k.  All done.  Now to decide what to return.
                if (rootRefs.size() == 1) {
                    // With a single root ref and configured to allow non-template,
                    // we will return the root node as-is
                    toDom.returnValue(rootRefs.first());
                } else {
                    if (rootRefs.isEmpty()) {
                        // no root refs; just return an empty builder.
                        toDom.returnValue(
                            tools.getNamespace(component, true)
                                .getElementBuilderConstructor(toDom));
                    } else {
                        // For now, no support for multiple roots;
                        throw new NotYetImplemented("Multiple root refs not yet supported in " + component);
                    }
                }
            } else {
                throw new IllegalArgumentException(
                    "<define-tag only supports ui=<dom /> /> nodes;" +
                        "\nYou sent " + uiExpr
                );
            }
        }

        return UiVisitScope.FEATURE_NO_CHILDREN;
    }

}
