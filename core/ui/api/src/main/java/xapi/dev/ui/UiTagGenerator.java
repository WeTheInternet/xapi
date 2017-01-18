package xapi.dev.ui;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.GeneratedUiComponent.GeneratedJavaFile;
import xapi.dev.ui.GeneratedUiComponent.GeneratedUiApi;
import xapi.dev.ui.GeneratedUiComponent.GeneratedUiBase;
import xapi.dev.ui.GeneratedUiComponent.GeneratedUiField;
import xapi.dev.ui.GeneratedUiComponent.GeneratedUiLayer;
import xapi.dev.ui.GeneratedUiComponent.GeneratedUiModel;
import xapi.dev.ui.UiVisitScope.ScopeType;
import xapi.except.NotYetImplemented;
import xapi.fu.Do;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.Out1;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.log.X_Log;
import xapi.util.X_String;
import xapi.util.X_Util;

import static com.github.javaparser.ast.expr.StringLiteralExpr.stringLiteral;
import static com.github.javaparser.ast.expr.TemplateLiteralExpr.templateLiteral;
import static xapi.fu.Out1.out1Deferred;
import static xapi.source.X_Source.javaQuote;

import java.lang.reflect.Modifier;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/24/16.
 */
public class UiTagGenerator extends UiComponentGenerator {

    protected static class UiMethodContext {
        private UiTagGenerator generator;
        private ContainerMetadata container;
        private UiGeneratorTools tools;

        public UiTagGenerator getGenerator() {
            return generator;
        }

        public UiMethodContext setGenerator(UiTagGenerator generator) {
            this.generator = generator;
            return this;
        }

        public ContainerMetadata getContainer() {
            return container;
        }

        public UiMethodContext setContainer(ContainerMetadata container) {
            this.container = container;
            return this;
        }

        public UiGeneratorTools getTools() {
            return tools;
        }

        public UiMethodContext setTools(UiGeneratorTools context) {
            this.tools = context;
            return this;
        }

    }

    protected class UiMethodTransformer extends ModifierVisitorAdapter<UiMethodContext> {

        private final GeneratedJavaFile ui;

        public UiMethodTransformer(GeneratedJavaFile ui) {
            this.ui = ui;
        }

        @Override
        public Node visit(
            DynamicDeclarationExpr n, UiMethodContext ctx
        ) {
            // We want to transform this method declaration
            // into something safely toString()able.
            final UiGeneratorTools tools = ctx.getTools();
            final ApiGeneratorContext<?> apiCtx = ctx.getContainer().getContext();


//            final Do undos = resolveSpecialNames(apiCtx, ctx.getContainer().getGeneratedComponent(), ui, null);

            if (n.getBody() instanceof MethodDeclaration) {
                MethodDeclaration method = (MethodDeclaration) n.getBody();
                if (ui.isInterface() && method.getBody() != null) {
                    // Make this method default if it has a body
                    method.setDefault(true);
                    method.setModifiers(
                        ( method.getModifiers() & ModifierSet.VISIBILITY_MASK)
                        | ModifierSet.DEFAULT
                    );
                }
            }
            String src = n.toSource(tools.getTransformer(apiCtx));
//            undos.done();
            return templateLiteral(src);
        }
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools tools, ComponentBuffer source, ContainerMetadata me, UiContainerExpr n
    ) {
        if (n.getName().equalsIgnoreCase("define-tags")) {
            // we have a list of tags to consider
            return generateTagList(tools, me, n);
        } else if (n.getName().equalsIgnoreCase("define-tag")) {
            return generateTag(tools, me, n);
        } else {
            throw new IllegalArgumentException("Unhandled component type " + n.getName() + "; " + tools.debugNode(n));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected UiVisitScope generateTagList(UiGeneratorTools tools, ContainerMetadata me, UiContainerExpr n) {
        final Out1<String> rootPkg = Lazy.deferred1(()->tools.getPackage(me.getContext(), n, this::getDefaultPackage));
        final ApiGeneratorContext ctx = me.getContext();
        final Expression tags = tools.resolveVar(ctx, n.getAttributeNotNull("tags").getExpression());
        if (tags instanceof JsonContainerExpr) {
            final JsonContainerExpr json = (JsonContainerExpr) tags;
            if (json.isArray()) {
                // All pairs in an array must be <dom /> based
                final MappedIterable<UiContainerExpr> resolvers = json.getValues()
                    .map(tools.varResolver(ctx));
                resolvers.forEach(ui->{
                        final String tagName = tools.resolveString(me.getContext(),
                            ui.getAttributeNotNull("name")
                              .getExpression()
                        );
                        // look in the current <ui /> for a package
                        String pkg = tools.getPackage(ctx, ui, rootPkg);
                        doTagGeneration(tools, me, ui, pkg, tagName);
                });
            } else {
                // Use the names of the {keys: ofJson}
                json.getPairs().forEach(pair->{
                    final String keyName = tools.resolveString(me.getContext(), pair.getKeyExpr());
                    final Expression value = tools.resolveVar(me.getContext(), pair.getValueExpr());
                    if (value instanceof UiContainerExpr) {
                        String pkg = tools.getPackage(ctx, value, rootPkg);
                        doTagGeneration(tools, me, (UiContainerExpr) value, pkg, keyName);
                    } else {
                        throw new IllegalArgumentException("Invalid json in define-tags; expected dom values:" +
                            " <define-tags tags={name: <define-tag />} />; You sent: " + tools.debugNode(value));
                    }
                });
            }
        } else if (tags instanceof UiContainerExpr){
            // Just a single item in the define-tags list.
            UiContainerExpr ui = (UiContainerExpr) tags;
            final String tagName = tools.resolveString(me.getContext(), ui.getAttributeNotNull("name"));
            // look in the current <ui /> for a package
            String pkg = tools.getPackage(ctx, ui, rootPkg);
            doTagGeneration(tools, me, ui, pkg, tagName);
        } else {
            throw new IllegalArgumentException("define-tags must have a `tags` feature that is either {name: json()}, <dom/>, or [<doms/>]");
        }
        return new UiVisitScope(ScopeType.CONTAINER).setVisitChildren(false);
    }

    protected String getDefaultPackage() {
        return "xapi.ui.generated";
    }

    protected UiVisitScope generateTag(UiGeneratorTools tools, ContainerMetadata me, UiContainerExpr n) {
        String pkg = tools.getPackage(me.getContext(), n, this::getDefaultPackage);
        String name = tools.resolveString(me.getContext(), n.getAttributeNotNull("name").getExpression());
        doTagGeneration(tools, me, n, pkg, name);
        return UiVisitScope.CONTAINER_NO_CHILDREN;
    }

    private void doTagGeneration(UiGeneratorTools tools, ContainerMetadata me, UiContainerExpr n, String pkg, String name) {
        final String tagName = name;
        final String className = toClassName(name);
        final Maybe<UiAttrExpr> apiAttr = n.getAttribute("api");
        final Maybe<UiAttrExpr> implAttr = n.getAttribute("impl");
        final Maybe<UiAttrExpr> data = n.getAttribute("data");
        final Maybe<UiAttrExpr> model = n.getAttribute("model");
        final Maybe<UiAttrExpr> ui = n.getAttribute("ui");
        final Maybe<UiAttrExpr> css = n.getAttribute("css");
        final Maybe<UiAttrExpr> cssClass = n.getAttribute("class");
        final Maybe<UiAttrExpr> style = n.getAttribute("style");
        final MappedIterable<UiAttrExpr> eventHandlers = n.getAttributesMatching(
            attr -> attr.getNameString().startsWith("on")
        );
        final GeneratedUiComponent component = me.getGeneratedComponent();
        final GeneratedUiApi api = component.getApi();
        final GeneratedUiBase base = component.getBase();

        if (data.isPresent()) {
            // The component has some internal data (not shared in model)
            addDataAccessors(tools, me, data.get(), base);
        }
        if (model.isPresent()) {
            // The component has a public data api (get/set Model)
            addDataModel(tools, me, model.get(), api);
        }
        // There is some css to inject with this component
        if (css.isPresent()) {
            addCss(tools, me, api, css.get(), false);
        }
        if (cssClass.isPresent()) {
            addCss(tools, me, api, cssClass.get(), true);
        }
        if (style.isPresent()) {
            addCss(tools, me, api, style.get(), false);
        }
        if (ui.isPresent()) {
            // The component has defined some display ui
            addUi(tools, me, ui.get(), component);
        }

        apiAttr.readIfPresent(attr->addApiMethods(tools, me, attr));
        implAttr.readIfPresent(attr->addImplMethods(tools, me, attr));

        eventHandlers.forEach(onHandler->{
            switch (onHandler.getNameString().toLowerCase()) {
                case "onclick":
                case "ondragstart":
                case "ondragend":
                case "ondrag":
                case "onmouseover":
                case "onmouseout":
                case "onlongpress":
                case "onrightclick":
            }
        });
    }

    protected void addLayerMethods(UiGeneratorTools tools, ContainerMetadata me, GeneratedUiLayer layer, UiAttrExpr attr) {
        final ApiGeneratorContext ctx = me.getContext();
        maybeAddImports(tools, ctx, layer, attr);
        final Expression resolved = tools.resolveVar(ctx, attr.getExpression());
        if (resolved instanceof JsonContainerExpr) {
            JsonContainerExpr asJson = (JsonContainerExpr) resolved;
            asJson.getValues().forAll(expr->{
                if (expr instanceof DynamicDeclarationExpr) {
                    DynamicDeclarationExpr method = (DynamicDeclarationExpr) expr;
                    printMethod(tools, layer, me, method);
                } else {
                    throw new IllegalArgumentException("Unhandled api= feature value " + tools.debugNode(expr) + " from " + tools.debugNode(attr));
                }
            });
        } else if (resolved instanceof DynamicDeclarationExpr) {
            DynamicDeclarationExpr method = (DynamicDeclarationExpr) resolved;
            printMethod(tools, layer, me, method);
        } else {
            throw new IllegalArgumentException("Unhandled api= feature value " + tools.debugNode(resolved) + " from " + tools.debugNode(attr));
        }
    }
    protected void addApiMethods(UiGeneratorTools tools, ContainerMetadata me, UiAttrExpr attr) {
        addLayerMethods(tools, me, me.getGeneratedComponent().getApi(), attr);
    }
    protected void addImplMethods(UiGeneratorTools tools, ContainerMetadata me, UiAttrExpr attr) {
        addLayerMethods(tools, me, me.getGeneratedComponent().getBase(), attr);
    }

    private String toClassName(String name) {
        String[] bits = name.split("-");
        StringBuilder b = new StringBuilder();
        for (String bit : bits) {
            b.append(X_String.toTitleCase(bit));
        }
        return b.toString();
    }

    private void addCss(
        UiGeneratorTools tools,
        ContainerMetadata me,
        GeneratedUiApi api,
        UiAttrExpr uiAttrExpr,
        boolean isClassName
    ) {}

    private void addUi(
        UiGeneratorTools tools,
        ContainerMetadata me,
        UiAttrExpr ui,
        GeneratedUiComponent component
    ) {
        boolean shadowUi = ui.getAnnotation(a->a.getNameString().equalsIgnoreCase("shadowdom")).isPresent();
        if (shadowUi || alwaysUseShadowDom()) {
            // We explicitly want to generate shadow dom
            // (and inject the minimal amount of css needed)
            final Expression uiExpr = ui.getExpression();
            if (uiExpr instanceof UiContainerExpr) {
                UiNamespace namespace = tools.namespace();
                final GeneratedUiBase baseClass = component.getBase();
                final ClassBuffer out = baseClass.getSource().getClassBuffer();

                // Look for template bindings, to figure out if we need to bind to any model fields
                MethodBuffer toDom = toDomMethod(namespace, baseClass, out);

                final ApiGeneratorContext ctx = me.getContext();
                ChainBuilder<String> rootRefs = Chain.startChain();
                // Now, visit any elements, storing variables to any refs.
                uiExpr.accept(new VoidVisitorAdapter<UiContainerExpr>() {

                    public String refFieldName;
                    public String rootRef; // The name of the ref in the xapi source
                    public String rootRefField; // The name of the ref field in generated java
                    Lazy<String> newBuilder = Lazy.deferred1(component::getElementBuilderConstructor, namespace);

                    @Override
                    public void visit(UiContainerExpr n, UiContainerExpr arg) {
                        if ("if".equals(n.getName())) {
                            // if tags are special.  We will use them to wrap the body of this container in a conditional
                            toDom.print("if (");

                            boolean first = true;
                            for (UiAttrExpr attr : n.getAttributes()) {
                                resolveReference(tools, ctx, component, baseClass, rootRefField, attr.getExpression());
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
                            super.visit(n, arg);
                            toDom.outdent();
                            toDom.println("}");
                            return;
                        }
                        String parentRefName = refFieldName;
                        // TODO: if the container is a known xapi component,
                        // then ask that component how it wants to render this node.
                        // For now we just want something that works to iterate on,
                        // but a future iteration should include a "ClassWorld" notion,
                        // where we have whole-world knowledge available before generating code...
                        boolean isRoot = arg == null;
                        final Maybe<Expression> refNode = n.getAttribute(UiNamespace.ATTR_REF)
                                    .mapNullSafe(UiAttrExpr::getExpression);
                        final Do undo;
                        if (isRoot) {
                            // we force a ref to any root nodes.
                            // note that this can happen multiple times,
                            // by parsing something like [ <one />, <two />, ... ]

                            final Expression refExpr = refNode.ifAbsentSupply(
                                out1Deferred(
                                    TemplateLiteralExpr::templateLiteral,
                                    "root"
                                )
                            );
                            rootRef = tools.resolveString(ctx, refExpr);
                            rootRefField = refFieldName = baseClass.newFieldName(rootRef);
                            rootRefs.add(rootRefField);
                            final FieldBuffer refField = out.createField(
                                baseClass.getElementBuilderType(namespace),
                                refFieldName
                            );
                            undo = component.registerRef(rootRef, refField);
                        } else {
                            if (refNode.isPresent()) {
                                assert n.getParentNode() == arg;
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
                            undo = Do.NOTHING;
                        }
                        boolean compact = requireCompact(n);
                        String type = baseClass.getElementBuilderType(namespace);
                        toDom.println(type + " " + refFieldName + " = " + newBuilder.out1() + ";")
                             .println(refFieldName + ".append(\"<" + n.getName() +
                                 (compact ? "" : ">") + "\");");

                        // tricky... each ui container sends itself to the children as argument;
                        try {
                            super.visit(n, n);
                        } finally {
                            if (compact) {
                                toDom.println(refFieldName + ".append(\" />\");");
                            } else {
                                toDom.println(refFieldName + ".append(\"</ " + n.getName() + ">\");");
                            }
                            if (parentRefName != null) {
                                toDom.println(parentRefName+".addChild(" + refFieldName + ");");
                            }
                            undo.done();
                            refFieldName = parentRefName;
                        }
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
                                        String scopeName = getScopeType(tools, scope);
                                        if (scopeName != null && scopeName.startsWith("$")) {
                                            // We have a method reference to a bound name ($model, $data, $Api, $Base, $Self)
                                            switch (scopeName.toLowerCase()) {
                                                case "$model":
                                                    // model references are excellent; they are defined by their types.
                                                    if (!component.hasPublicModel()) {
                                                        throw new IllegalStateException("Cannot reference $model on a type without a model={ name: Type.class } feature.");
                                                    }
                                                    final GeneratedUiModel model = component.getPublicModel();
                                                    final GeneratedUiField field = model.fields.get(ref.getIdentifier());
                                                    if (field != null) {
                                                        switch (field.getMemberType()) {
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
                                resolveReference(tools, ctx, component, baseClass, rootRefField, param);
                            }

                            Do undo = resolveSpecialNames(ctx, component, baseClass, rootRefField);
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

                }, null);

                // k.  All done.  Now to decide what to return.
                if (rootRefs.size() == 1 && !alwaysUseTemplate()) {
                    // With a single root ref and configured to allow non-template,
                    // we will return the root node as-is
                    toDom.returnValue(rootRefs.first());
                } else {
                    // For now, no support for multiple roots;
                    throw new NotYetImplemented("Multiple root refs not yet supported in " + component);
                }
            } else {
                throw new IllegalArgumentException(
                    "<define-tag only supports ui=<dom /> /> nodes;" +
                    "\nYou sent " + uiExpr
                );
            }
        }
    }

    protected void resolveReference(
        UiGeneratorTools tools,
        ApiGeneratorContext ctx,
        GeneratedUiComponent component,
        GeneratedJavaFile target,
        String rootRefField,
        Expression expression
    ) {

        final Do undo = UiTagGenerator.this.resolveSpecialNames(
            ctx,
            component,
            target,
            rootRefField
        );
        try {
            final Expression param = tools.resolveVar(ctx, expression);
            Do modelOrData = () ->
                param.getParentNode().accept(new ModifierVisitorAdapter<Object>() {
                    private QualifiedNameExpr lastQualified;
                    private Node replaceQualified;

                    @Override
                    public Node visit(FieldAccessExpr n, Object arg) {
                        if (n.getScope().toSource().equals("$model")) {
                            final GeneratedUiField field = component.getPublicModel().fields.get(
                                n.getField());
                            if (field == null) {
                                throw new IllegalArgumentException("No model field of name " + n.getField() + " declared");
                            }
                            MethodCallExpr replaceWith = new MethodCallExpr(n.getScope(), field.getterName());
                            return replaceWith;
                        }
                        return super.visit(n, arg);
                    }

                    @Override
                    public Node visit(QualifiedNameExpr n, Object arg) {
                        final QualifiedNameExpr previous = lastQualified;
                        lastQualified = n;
                        try {
                            final Node myReturn = super.visit(n, arg);
                            return X_Util.firstNotNull(replaceQualified, myReturn);
                        } finally {
                            lastQualified = previous;
                            replaceQualified = null;
                        }
                    }

                    @Override
                    public Node visit(NameExpr n, Object arg) {
                        switch (n.getName()) {
                            case "$model":
                                final MethodCallExpr getModel = new MethodCallExpr(null, "getModel");
                                if (lastQualified == null) {
                                    return getModel;
                                } else {
                                    replaceQualified = new MethodCallExpr(getModel, toModelGetter(component, lastQualified.getName()));
                                }
                                break;
                            case "$data":
                                // TODO: data should resolve to individual fields...
                        }
                        return super.visit(n, arg);
                    }
                }, null);
            if (param instanceof MethodCallExpr) {
                String scopeType = getScopeType(tools, ((MethodCallExpr)param).getScope());
                if ("$model".equals(scopeType) || "$data".equals(scopeType)) {
                    modelOrData.done();
                }
            } else if (param instanceof FieldAccessExpr) {
                // A field access on a data or model node is special.
                // We will map it to the correct method for you.
                String scopeType = getScopeType(tools, ((FieldAccessExpr)param).getScope());
                if ("$model".equals(scopeType) || "$data".equals(scopeType)) {
                    modelOrData.done();
                }
            } else if (param instanceof NameExpr) {
                NameExpr rootName = (NameExpr) param;
                while (rootName instanceof QualifiedNameExpr) {
                    rootName = ((QualifiedNameExpr)rootName).getQualifier();
                }
                switch (rootName.getName()) {
                    case "$model":
                    case "$data":
                        modelOrData.done();
                        break;
                    default:
                        // Default case, just leave the name alone
                }
            } else if (param instanceof LiteralExpr) {
                if (param instanceof StringLiteralExpr) {

                } else if (param instanceof TemplateLiteralExpr) {

                } else if (param instanceof UiExpr) {

                }
            } else {
            }
        } finally {
            undo.done();
        }
    }

    protected String toModelGetter(GeneratedUiComponent component, String name) {
        if (component.hasPublicModel()) {
            final GeneratedUiField field = component.getPublicModel().getField(name);
            return field.getterName();
        }
        throw new IllegalStateException("No model field for " + name + " in " + component.getPublicModel());
    }

    protected String getScopeType(UiGeneratorTools tools, Expression scope) {
        if (scope == null) {
            return null;
        }
        if (scope instanceof TypeExpr) {
            return ((TypeExpr)scope).getType().toSource();
        }
        if (scope instanceof NameExpr) {
            return ((NameExpr) scope).getName();
        }
        if (scope instanceof MethodCallExpr) {
            return getScopeType(tools, ((MethodCallExpr)scope).getScope());
        }
        if (scope instanceof FieldAccessExpr) {
            return getScopeType(tools, ((FieldAccessExpr)scope).getScope());
        }
        X_Log.trace(getClass(), "Unable to determine scope of ", Out1.newOut1(()->tools.debugNode(scope)));
        return null;
    }

    protected Do resolveSpecialNames(
        ApiGeneratorContext ctx,
        GeneratedUiComponent component,
        GeneratedJavaFile cls,
        String rootRefField
    ) {
        Do start = ctx.addToContext("$Self", templateLiteral(cls.getWrappedName()));
        start = start.doAfter(ctx.addToContext("$this", templateLiteral(cls.getWrappedName() + ".this")));
        start = start.doAfter(ctx.addToContext("$Api", templateLiteral(
            component.getApi().getTypeName()
        )));
        start = start.doAfter(ctx.addToContext("$Base", templateLiteral(
            component.getBase().getTypeName()
        )));

        if (rootRefField != null) {
            start = start.doAfter(ctx.addToContext("$root", stringLiteral(rootRefField)));
        }
        if (component.hasPublicModel()) {
            start = start.doAfter(ctx.addToContext("$model", templateLiteral("getModel()")));
        }
        if (component.hasPrivateModel()) {
            start = start.doAfter(ctx.addToContext("$data", templateLiteral("getData()")));
        }
        return start;
    }

    protected boolean alwaysUseTemplate() {
        return false;
    }

    protected boolean requireCompact(UiContainerExpr n) {
        return false;
    }

    protected MethodBuffer toDomMethod(
        UiNamespace namespace,
        GeneratedUiLayer ui,
        ClassBuffer output
    ) {
        String builderType = output.addImport(ui.getElementBuilderType(namespace));
        // TODO check if this builderType expects generics...
        final MethodBuffer method = output.createMethod("public " + builderType + " toDom()");
        return method;

    }

    protected boolean alwaysUseShadowDom() {
        return true;
    }

    @SuppressWarnings("unchecked")
    private void addDataModel(
        UiGeneratorTools tools,
        ContainerMetadata me,
        UiAttrExpr attr,
        GeneratedUiApi api
    ) {
        final GeneratedUiModel model = api.getModel();
        final ApiGeneratorContext ctx = me.getContext();
        maybeAddImports(tools, ctx, model, attr);
        api.getSource().getClassBuffer().createMethod(model.getWrappedName()+" getModel()")
                .makeAbstract();
        me.getGeneratedComponent().getBase().getSource().getClassBuffer().createField(model.getWrappedName(), "model")
            .makePrivate()
            .addGetter(Modifier.PUBLIC)
            .addAnnotation(Override.class);

        boolean immutable = attr.getAnnotation(anno->anno.getNameString().equalsIgnoreCase("immutable")).isPresent();
        final Expression expr = attr.getExpression();
        if (expr instanceof JsonContainerExpr) {
            JsonContainerExpr json = (JsonContainerExpr) expr;
            json.getPairs().forEach(pair->{
                final String rawFieldName = tools.resolveString(ctx, pair.getKeyExpr());
                final Expression typeExpr = tools.resolveVar(ctx, pair.getValueExpr());
                if (typeExpr instanceof DynamicDeclarationExpr) {
                    // Must be a default method.
                    DynamicDeclarationExpr method = (DynamicDeclarationExpr) typeExpr;
                    printMethod(tools, api.getModel(), me, method);
                } else {
                    Type type = tools.methods().$type(tools, ctx, typeExpr).getType();
                    // TODO smart import lookups...
                    String typeName = tools.lookupType(type.toSource());
                    boolean isImmutable = immutable;
                    if (!isImmutable) {
                        isImmutable = pair.getAnnotation(anno -> anno.getNameString().equalsIgnoreCase(
                            "immutable")).isPresent();
                    }
                    api.getModel().addField(tools, typeName, rawFieldName, isImmutable);
                }
            });
        } else {
            throw new IllegalArgumentException("<define-tag model={mustBe: Json} />; you sent " + tools.debugNode(attr));
        }
    }

    protected void maybeAddImports(
        UiGeneratorTools tools,
        ApiGeneratorContext ctx,
        GeneratedJavaFile api,
        UiAttrExpr attr
    ) {
        attr.getAnnotation(anno->anno.getNameString().toLowerCase().equals("import"))
            .readIfPresent(anno->anno.getMembers().forEach(pair->{
                final Expression resolvedImport = tools.resolveVar(ctx, pair.getValue());
                String toImport;
                if (resolvedImport instanceof StringLiteralExpr) {
                    toImport = ((StringLiteralExpr)resolvedImport).getValue();
                    api.getSource().addImport(toImport);
                } else if (resolvedImport instanceof TemplateLiteralExpr) {
                    toImport = tools.resolveTemplate(ctx, (TemplateLiteralExpr) resolvedImport);
                    api.getSource().addImport(toImport);
                } else if (resolvedImport instanceof ArrayInitializerExpr) {
                    ArrayInitializerExpr many = (ArrayInitializerExpr) resolvedImport;
                    for (Expression expr : many.getValues()) {
                        toImport = tools.resolveString(ctx, expr);
                        api.getSource().addImport(toImport);
                    }
                } else {
                    throw new IllegalArgumentException("Unhandled @Import value " + tools.debugNode(resolvedImport));
                }
            }));

    }

    protected void printMethod(
        UiGeneratorTools tools,
        GeneratedJavaFile cls,
        ContainerMetadata me,
        DynamicDeclarationExpr method
    ) {
        final ApiGeneratorContext ctx = me.getContext();


        resolveReference(tools, ctx, me.getGeneratedComponent(), cls, null, method);

        final Do undo = resolveSpecialNames(ctx, me.getGeneratedComponent(), cls, null);
        UiMethodTransformer transformer = new UiMethodTransformer(cls);
        final Node result = transformer.visit(method, new UiMethodContext()
            .setContainer(me)
            .setTools(tools)
            .setGenerator(this)
        );

        final String src = tools.resolveLiteral(ctx, (Expression)result);
        cls.getSource().getClassBuffer()
            .println()
            .printlns(src);

        undo.done();
    }

    private void addDataAccessors(
        UiGeneratorTools tools,
        ContainerMetadata me,
        UiAttrExpr uiAttrExpr,
        GeneratedUiBase base
    ) {}

    @Override
    public void endVisit(UiGeneratorTools service, ContainerMetadata me, UiContainerExpr n, UiVisitScope scope) {
        super.endVisit(service, me, n, scope);
    }
}
