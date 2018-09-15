package xapi.dev.ui.tags;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.assembler.*;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.Do;
import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.iterate.ChainBuilder;
import xapi.log.X_Log;
import xapi.util.X_String;

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

    public static class UiTagGeneratorVisitor extends VoidVisitorAdapter<UiContainerExpr> {

        private final GeneratedUiComponent component;
        private final MethodBuffer toDom;
        private final UiGeneratorTools tools;
        private final ApiGeneratorContext ctx;
        private final GeneratedUiBase baseClass;
        private final ChainBuilder<String> rootRefs;
        private final ClassBuffer out;
        private final GeneratedUiLayer layer;
        private final AssembledUi assembly;
        private final UiTagGenerator generator;
        public In1<String> refNameSpy;
        public String refFieldName;
        public String rootRef; // The name of the ref in the xapi source
        public String rootRefField; // The name of the ref field in generated java
        public UiAssembler rootAssembler, parentAssembler;
        private AssembledElement next;

        public UiTagGeneratorVisitor(
            AssembledUi assembly,
            MethodBuffer toDom,
            ChainBuilder<String> rootRefs
        ) {
            this.assembly = assembly;
            this.toDom = toDom;
            this.component = assembly.getUi();
            this.layer = component.getBase();
            this.tools = assembly.getTools();
            this.ctx = assembly.getContext();
            this.baseClass = component.getBase();
            this.rootRefs = rootRefs;
            this.out = baseClass.getSource().getClassBuffer();
            generator = assembly.getGenerator();
            refNameSpy = In1.ignored();
            next = new AssemblyRoot(assembly);
        }

        @Override
        public void visit(UiContainerExpr n, UiContainerExpr arg) {
            boolean isRoot = arg == null;

            next = generator.attachChild(assembly, next, n);

            final UiNamespace namespace = assembly.getNamespace();
            final UiTagGenerator owner = assembly.getGenerator();

            final Maybe<Expression> refNode = n.getAttribute(UiNamespace.ATTR_REF)
                .mapNullSafe(UiAttrExpr::getExpression);

//            DefaultUiAssembler assembler = new DefaultUiAssembler(this, refNode, tools, component, layer, rootRefs);
//            final UiAssemblerResult result = assembler.generateAssembly(
//                UiTagUiGenerator.this,
//                n,
//                parentAssembler
//            );
//            if (result.isSuccess()) {
//                final UiAssembler oldParent = parentAssembler;
//                parentAssembler = assembler;
//                // visit body children and return
//                visitBody(n);
//                parentAssembler = oldParent;
//                return;
//            } // else fallthrough to legacy mess...

            String parentRefName = refFieldName;
            // TODO: if the container is a known xapi component,
            // then ask that component how it wants to render this node.
            // For now we just want something that works to iterate on,
            // but a future iteration should include a "ClassWorld" notion,
            // where we have whole-world knowledge available before generating code...
            assert isRoot || !refNode.isPresent() || n.getContainerParentNode() == arg;
            final Do undo = registerNode(n, refNode, isRoot);
            boolean compact = assembly.requireCompact(n);

            final Maybe<Expression> modelNode = n.getAttribute(UiNamespace.ATTR_MODEL)
                .mapNullSafe(UiAttrExpr::getExpression);

            String type = baseClass.getElementBuilderType(namespace);
            boolean printedVar = false;
            if (modelNode.isPresent()) {
                // When a modelNode is present, we should delegate to a generated element's toDom method.
                final Expression modelExpr = modelNode.get();
                Expression nodeToUse = tools.resolveVar(ctx, modelExpr);
                if (nodeToUse != null) {
                    if (!nodeToUse.hasExtra(EXTRA_MODEL_INFO)) {
                        nodeToUse = owner.resolveReference(tools, ctx, component, baseClass, next.getRoot().maybeRequireRef(), next.maybeRequireRef(), nodeToUse, false);
                    }
                    if (nodeToUse.hasExtra(EXTRA_MODEL_INFO)) {
                        // yay, some model info for us!
                        // final GeneratedUiField modelInfo = nodeToUse.getExtra(EXTRA_MODEL_INFO);
                        ComponentBuffer otherTag = tools.getGenerator().getComponent(ctx, n.getName());
                        // We'll want to defer to a factory method on the other component.
                        if (otherTag == null) {
                            final GeneratedUiDefinition info = tools.getDefinition(ctx, n.getName());
                            if (info == null) {
                                throw new NotConfiguredCorrectly("No definition found for " + n.getName());
                            }
                            toDom.print(type + " " + refFieldName + " = create" + info.getApiName() + "(");

                            // do the grunt work of getTagFactory, but w/out all the mess
                            final MethodBuffer baseMethod = baseClass.getSource().getClassBuffer()
                                .createMethod("protected abstract " + baseClass.getElementBuilderType(namespace) + " create" + info.getApiName() + "();");

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
                                nodeToUse = owner.resolveReference(tools, ctx, component, baseClass, next.getRoot().maybeRequireRef(), next.maybeRequireRef(), nodeToUse, false);
                            }
                            MethodCallExpr factoryMethod = otherTag.getTagFactory(tools, ctx, component, namespace, n, nodeToUse);
                            String initializer = tools.resolveString(ctx, factoryMethod);
                                toDom.println(type + " " + refFieldName + " = " + initializer + ";");
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
                    visitBody(n);
                } else {
                    toDom.println(type + " " + refFieldName + " = " + assembly.newBuilder(namespace) + ";")
                        .println(refFieldName + ".append(\"<" + n.getName() +
                            (compact ? "" : ">") + "\");");
                    try {
                        visitBody(n);
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

        public void visitBody(UiContainerExpr n) {
            if (n.getBody() != null) {
                n.getBody().accept(this, n);
            }
        }

        public Do registerNode(UiContainerExpr n, Maybe<Expression> refNode, boolean isRoot) {
            if (isRoot) {
                // we force a ref to any root nodes.
                // note that this can happen multiple times,
                // by parsing something like [ <one />, <two />, ... ]

                setRootRef(refNode);
                final FieldBuffer refField = out.createField(
                    assembly.newBuilder(),
                    refFieldName
                );
                return component.registerRef(rootRef, refField);
            } else {
                if (refNode.isPresent()) {
                    String ref = tools.resolveString(ctx, refNode.get());
                    refFieldName = baseClass.newFieldName(ref);
                    final FieldBuffer refField = out.createField(
                        assembly.newBuilder(),
                        refFieldName
                    );
                    component.registerRef(ref, refField);
                } else {
                    refFieldName = assembly.newVarName("el" + tools.tagToJavaName(n));
                }
                return Do.NOTHING;
            }
        }

        public String setRootRef(Maybe<Expression> refNode) {
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
            return rootRefField;
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
                            String scopeName = generator.getScopeType(tools, scope);
                            if (scopeName != null && scopeName.startsWith("$")) {
                                // We have a method reference to a bound name ($model, $data, $Api, $Base, $Self)
                                switch (scopeName.toLowerCase().split("<")[0]) {
                                    case "$model":
                                        // model references are excellent; they are defined by their types.
                                        if (!component.hasPublicModel()) {
                                            throw new IllegalStateException("Cannot reference $model on a type without a model={ name: Type.class } feature.");
                                        }
                                        final GeneratedUiModel model = component.getPublicModel();
                                        final GeneratedUiMember field = model.getFields().get(ref.getIdentifier());
                                        if (field != null) {
                                            switch (field.getMemberType().toSource()) {
                                                case "String":
                                                case "java.lang.String":
                                                    // TODO bind the model's string property to the contents of this node
                                                    toDom.println(refFieldName + ".append(getModel().get" + field.getCapitalized()+"());");
                                                    break;
                                                default:
                                                    String elementType = assembly.getTypeElement();
                                                    if (elementType.equals(field.getMemberType().toStringWithoutComments())) {
                                                        // a bit odd to have elements in your model, but we won't stop you...
                                                        toDom.println(refFieldName + ".addChild(getModel().get" + field.getCapitalized()+"());");
                                                    } else {
                                                        toDom.println(refFieldName + ".append(coerce(getModel().get" + field.getCapitalized()+"()));");
                                                        component.requireCoercion(layer, field);
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
                            if (asExpr instanceof NameExpr) {
                                toDom.println(refFieldName + ".append(" + javaQuote(template) + ");");
                            } else {
                                asExpr.accept(this, arg);
                            }
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
                    generator.resolveReference(tools, ctx, component, baseClass, next.getRoot().maybeRequireRef(), next.maybeRequireRef(), param, true);
                }

                Do undo = generator.resolveSpecialNames(ctx, component, baseClass, next.getRoot().maybeRequireRef(), next.maybeRequireRef());
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

    }

    private final String name;
    private final String pkg;
    private final UiTagGenerator owner;
    private Lazy<String> newBuilder;
    private Lazy<String> newInjector;
    private UiNamespace namespace;


    public UiTagUiGenerator(String pkg, String name, UiTagGenerator owner) {
        this.name = name;
        this.pkg = pkg;
        this.owner = owner;
    }

    /**
     * Returns the method name for newBuilder() that should be used.
     *
     * Invoking this method ensures the required method is generated for you.
     */
    public String newBuilder() {
        return newBuilder.out1();
    }
    /**
     * Returns the method name for newInjector() that should be used.
     *
     * Invoking this method ensures the required method is generated for you.
     */
    public String newInjector() {
        return newInjector.out1();
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools tools,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata me,
        UiAttrExpr ui
    ) {
        if (this.namespace == null) {
            this.namespace = tools.namespace();
        }
        AssembledUi assembled = source.getAssembly(tools, namespace, owner);
        final GeneratedUiComponent component = me.getGeneratedComponent();
        this.newBuilder = Lazy.deferred1(component::getElementBuilderConstructor, namespace);
        this.newInjector = Lazy.deferred1(component::getElementInjectorConstructor, namespace);

        // we allow two layers of ui:
        // a logical graph of connected nodes,
        // and a rendered graph of connected nodes.
        // if only one layer is provided, it acts as both layers.

        // ui = <children /> // logical "lightdom"
        // shadow = <children /> // rendered "shadowdom".

        // When both layers are provided,
        // "system objects" will look only in the ui layer for children,
        // and only the renderer will switch to rendering the shadow layer.


        boolean shadowUi = "shadow".equalsIgnoreCase(ui.getNameString()) ||
            ui.getAnnotation(a->a.getNameString().startsWith("shadow")).isPresent();
        boolean hidden = "hidden".equalsIgnoreCase(ui.getNameString()) ||
            ui.getAnnotation(a->a.getNameString().startsWith("hidden")).isPresent();

        final UiAssemblerResult result = assembled.addAssembly(
            ui.getExpression(),
            shadowUi,
            hidden
        );

        return UiVisitScope.FEATURE_NO_CHILDREN;
    }

    public UiNamespace getNamespace() {
        return namespace;
    }
}
