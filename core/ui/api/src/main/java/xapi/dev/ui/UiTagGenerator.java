package xapi.dev.ui;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ModifierSet;
import com.github.javaparser.ast.body.VariableDeclarator;
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
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.log.X_Log;
import xapi.source.read.SourceUtil;
import xapi.util.X_String;
import xapi.util.X_Util;

import java.util.Arrays;
import java.util.List;

import static com.github.javaparser.ast.expr.StringLiteralExpr.stringLiteral;
import static com.github.javaparser.ast.expr.TemplateLiteralExpr.templateLiteral;
import static xapi.dev.ui.UiConstants.EXTRA_MODEL_INFO;
import static xapi.fu.Out1.out1Deferred;
import static xapi.source.X_Source.javaQuote;

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
        UiGeneratorTools tools, ComponentBuffer source, ContainerMetadata me, UiContainerExpr n,
        UiGenerateMode mode
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
        final Maybe<UiAttrExpr> generics = n.getAttribute("generics");
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

        if (generics.isPresent()) {
            addGenerics(tools, component, me, generics.get());
        }
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

        apiAttr.readIfPresent(attr-> addApiMembers(tools, me, attr));
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

    protected Do addGenerics(
        UiGeneratorTools tools,
        GeneratedUiComponent component,
        ContainerMetadata me,
        UiAttrExpr attr
    ) {
        final Expression generics = attr.getExpression();
        if (!(generics instanceof JsonContainerExpr)) {
            throw new IllegalArgumentException("A generic={} feature must be a json value; you sent " + tools.debugNode(attr));
        }
        JsonContainerExpr container = (JsonContainerExpr) generics;
        if (container.isArray()) {
            throw new IllegalArgumentException("A generic={} feature must be a json map; you sent array " + tools.debugNode(attr));
        }
        Mutable<Do> undos = new Mutable<>(Do.NOTHING);
        final ApiGeneratorContext ctx = me.getContext();
        container.getPairs().forEach(pair->{
            String genericName = tools.resolveString(ctx, pair.getKeyExpr());
            final Expression resolved = tools.resolveVar(ctx, pair.getValueExpr());
            final TypeExpr type = tools.methods().$type(tools, ctx, resolved);
            if (genericName.startsWith("$")) {
                final Do was = undos.out1();
                final Do use = was.doAfter(ctx.addToContext(genericName, type));
                undos.in(use);
            }
            component.addGeneric(genericName, type);
        });

        return undos.out1();
    }

    protected void addLayerMembers(UiGeneratorTools tools, ContainerMetadata me, GeneratedUiLayer layer, UiAttrExpr attr) {
        final ApiGeneratorContext ctx = me.getContext();
        maybeAddImports(tools, ctx, layer, attr);
        final Expression resolved = tools.resolveVar(ctx, attr.getExpression());
        if (resolved instanceof JsonContainerExpr) {
            JsonContainerExpr asJson = (JsonContainerExpr) resolved;
            asJson.getValues().forAll(expr->{
                if (expr instanceof DynamicDeclarationExpr) {
                    DynamicDeclarationExpr member = (DynamicDeclarationExpr) expr;
                    printMember(tools, layer, me, member);
                } else {
                    throw new IllegalArgumentException("Unhandled api= feature value " + tools.debugNode(expr) + " from " + tools.debugNode(attr));
                }
            });
        } else if (resolved instanceof DynamicDeclarationExpr) {
            // A single dynamic declaration is special; if it is a class or an interface
            // and the layer matches that type, we will use that type directly.
            DynamicDeclarationExpr member = (DynamicDeclarationExpr) resolved;
            if (member.getBody() instanceof ClassOrInterfaceDeclaration) {
                ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) member.getBody();
                if (layer.isInterface()) {
                    if (type.isInterface()) {
                       // api has an interface, lets use it!

                    }
                } else if (!type.isInterface()) {
                       // base is a class, lets use it!
                }
            }
            printMember(tools, layer, me, member);
        } else {
            throw new IllegalArgumentException("Unhandled api= feature value " + tools.debugNode(resolved) + " from " + tools.debugNode(attr));
        }
    }
    protected void addApiMembers(UiGeneratorTools tools, ContainerMetadata me, UiAttrExpr attr) {
        addLayerMembers(tools, me, me.getGeneratedComponent().getApi(), attr);
    }
    protected void addImplMethods(UiGeneratorTools tools, ContainerMetadata me, UiAttrExpr attr) {
        addLayerMembers(tools, me, me.getGeneratedComponent().getBase(), attr);
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
                        switch (n.getName().toLowerCase()) {
                            case "if":
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
                                final UiBodyExpr myBody = n.getBody();
                                if (myBody != null) {
                                    super.visit(myBody, n);
                                }
                                toDom.outdent();
                                toDom.println("}");
                                return;
                            // for tag is also special; we will unfold the underlying item type
                            case "for":
                                toDom.print("for (");
                                n.getAttribute("allOf")
                                    .readIfPresent(all->{
                                        final Expression startExpr = all.getExpression();
                                        resolveReference(tools, ctx, component, baseClass, rootRefField, all.getExpression());
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
                                                    if (isModelReference(allOfRoot)) {
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
                                                            call = call + ".forEach()"; // ew.  But. well, it is what it is.  TODO: abstract this away
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
//                                                                    child.addExtra(EXTRA_GENERATE_MODE, "");
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
                                assert n.getContainerParentNode() == arg;
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
                                    // tricky... each ui container sends itself to the children as argument;
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
                                            switch (scopeName.toLowerCase().split("<")[0]) {
                                                case "$model":
                                                    // model references are excellent; they are defined by their types.
                                                    if (!component.hasPublicModel()) {
                                                        throw new IllegalStateException("Cannot reference $model on a type without a model={ name: Type.class } feature.");
                                                    }
                                                    final GeneratedUiModel model = component.getPublicModel();
                                                    final GeneratedUiField field = model.fields.get(ref.getIdentifier());
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

    protected boolean isModelReference(Expression expr) {
        if (expr instanceof NameExpr) {
            return "$model".equals(((NameExpr)expr).getName());
        } else if (expr instanceof MethodCallExpr) {
            return "getModel".equals(((MethodCallExpr)expr).getName());
        } else if (expr instanceof FieldAccessExpr) {
            return "$model".equals(((FieldAccessExpr)expr).getField());
        } else {
            return false;
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
                            replaceWith.addExtra(EXTRA_MODEL_INFO, field);
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
                                    final Maybe<GeneratedUiField> modelField = component.getModelField(lastQualified.getName());
                                    replaceQualified = new MethodCallExpr(getModel, modelField.getOrThrow().getterName());
                                    replaceQualified.addExtra(EXTRA_MODEL_INFO, modelField.get());
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

        me.getGeneratedComponent().getBase().ensureField(model.getWrappedName(), "model");

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
                    printMember(tools, api.getModel(), me, method);
                } else {
                    Type type = tools.methods().$type(tools, ctx, typeExpr).getType();
                    // TODO smart import lookups...
                    boolean isImmutable = immutable;
                    if (!isImmutable) {
                        isImmutable = pair.getAnnotation(anno -> anno.getNameString().equalsIgnoreCase(
                            "immutable")).isPresent();
                    }
                    api.getModel().addField(tools, type, rawFieldName, isImmutable);
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

    protected void printMember(
        UiGeneratorTools tools,
        GeneratedJavaFile cls,
        ContainerMetadata me,
        DynamicDeclarationExpr member
    ) {
        final ApiGeneratorContext ctx = me.getContext();

        resolveReference(tools, ctx, me.getGeneratedComponent(), cls, null, member);

        final Do undo = resolveSpecialNames(ctx, me.getGeneratedComponent(), cls, null);
        try {

            final BodyDeclaration decl = member.getBody();
            ChainBuilder<VariableDeclarator> toForward = Chain.startChain();
            if (decl instanceof FieldDeclaration) {
                FieldDeclaration asField = (FieldDeclaration) decl;
                String typeName = asField.getType().toSource();
                for (VariableDeclarator var : asField.getVariables()) {
                    if (var.getInit() == null) {
                        if (cls.isInterface()) {
                            toForward.add(var);
                        }
                    } else {
                        // A field with an initializer.
                        final Expression init = var.getInit();
                        // If this is an interface, we may be able to use only default methods
                        if (cls.isInterface()) {

                            if (init instanceof MethodReferenceExpr) {
                                // when using a method reference, we will bind get/set to whatever is referneced.
                                MethodReferenceExpr methodRef = (MethodReferenceExpr) init;
                                if ("$model".equals(methodRef.getScope().toSource())) {
                                    String scope = tools.resolveString(ctx, methodRef.getScope());
                                    // Create getter and setter
                                    String nameToUse = cls.newFieldName(methodRef.getIdentifier());
                                    String getterName = SourceUtil.toGetterName(typeName, nameToUse);
                                    String setterName = SourceUtil.toSetterName(nameToUse);
                                    final ClassBuffer buf = cls.getSource().getClassBuffer();
                                    boolean addGetter = true, addSetter = true;
                                    if (nameToUse.startsWith("get")) {
                                        // getter only
                                        addSetter = false;
                                    } else if (nameToUse.startsWith("set")) {
                                        // setter only
                                        addGetter = false;
                                    }
                                    if (addGetter) {
                                        buf.createMethod("default " + typeName + " " + getterName+ "()")
                                            .print("return ")
                                            .printlns(scope + "." + getterName + "();");
                                    }
                                    if (addSetter) {
                                        buf.createMethod("default void " + setterName+ "()")
                                            .addParameter(typeName, nameToUse)
                                            .printlns(scope + "." + setterName + "(" + nameToUse + ");");
                                    }
                                    continue; // do not fall through into .ensureField.
                                } else {
                                    throw new NotYetImplemented("Can only support $model::fieldReferences; you sent " + tools.debugNode(init));
                                }


                            } else if (var.getInit() instanceof MethodCallExpr) {
                                // no magic for these guys yet;
                            } else {
                                // assume this is a constant.  forward to base type to fill out.
                                toForward.add(var);
                                // falls through to .ensureField.
                            }
                        }
                    }
                    cls.ensureField(typeName, var.getId().getName());
                }

                if (toForward.isEmpty()) {
                    return;
                } else {
                    // for an interface, we turn a field into a pair of getter / setters (no field!)
                    if (cls instanceof GeneratedUiApi) {
                        // Whenever we add to the interface, we also add to the base class.
                        final List<VariableDeclarator> oldVars = asField.getVariables();
                        asField.setVariables(Arrays.asList(toForward.toArray(VariableDeclarator[]::new)));
                        printMember(tools, me.getGeneratedComponent().getBase(), me, member);
                        asField.setVariables(oldVars);
                        return;
                    }
                }
            }

            UiMethodTransformer transformer = new UiMethodTransformer(cls);
            final Node result = transformer.visit(member, new UiMethodContext()
                .setContainer(me)
                .setTools(tools)
                .setGenerator(this)
            );

            final String src = tools.resolveLiteral(ctx, (Expression) result);
            cls.getSource().getClassBuffer()
                .println()
                .printlns(src);

        } finally {
            undo.done();
        }
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
