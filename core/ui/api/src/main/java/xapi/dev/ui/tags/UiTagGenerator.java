package xapi.dev.ui.tags;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;
import xapi.collect.X_Collect;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.api.UiVisitScope.ScopeType;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.assembler.*;
import xapi.dev.ui.tags.members.UserDefinedMethod;
import xapi.fu.*;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.lang.api.AstConstants;
import xapi.log.X_Log;
import xapi.source.X_Modifier;
import xapi.util.X_String;
import xapi.util.X_Util;

import java.util.Locale;

import static com.github.javaparser.ast.expr.StringLiteralExpr.stringLiteral;
import static com.github.javaparser.ast.expr.TemplateLiteralExpr.templateLiteral;
import static xapi.collect.X_Collect.MUTABLE_INSERTION_ORDERED;
import static xapi.collect.X_Collect.newMap;
import static xapi.dev.ui.api.UiConstants.EXTRA_MODEL_INFO;
import static xapi.fu.Immutable.immutable1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/24/16.
 */
public class UiTagGenerator extends UiComponentGenerator {

    private final StringTo<In2Out1<AssembledUi, AssembledElement, TagAssembler>> tagFactories;
    private final ChainBuilder<Do> undos;

    public UiTagGenerator() {
        tagFactories = X_Collect.newStringMapInsertionOrdered(In2Out1.class);
        undos = Chain.startChain();
    }

    public void addUndo(Do undo) {
        if (undo != null) {
            undos.add(undo);
        }
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools tools, ComponentBuffer source, ContainerMetadata me, UiContainerExpr n,
        UiGenerateMode mode
    ) {
        if (n.getName().equalsIgnoreCase("define-tags")) {
            // we have a list of tags to consider
            return generateTagList(tools, source, me, n, mode);
        } else if (n.getName().equalsIgnoreCase("define-tag")) {
            return generateTag(tools, source, me, n, null, null, mode);
        } else {
            throw new IllegalArgumentException("Unhandled component type " + n.getName() + "; " + tools.debugNode(n));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected UiVisitScope generateTagList(
        UiGeneratorTools tools,
        ComponentBuffer source,
        ContainerMetadata me,
        UiContainerExpr n,
        UiGenerateMode mode
    ) {
        // TODO: pre-visit children so the first child seen can know about the last child seen...
        final Out1<String> rootPkg = Lazy.deferred1(()->tools.getPackage(me.getContext(), n, this::getDefaultPackage));
        final ApiGeneratorContext ctx = me.getContext();
        final Expression tags = tools.resolveVar(ctx, n.getAttributeNotNull("tags").getExpression());
        if (tags instanceof JsonContainerExpr) {
            final JsonContainerExpr json = (JsonContainerExpr) tags;
            ObjectTo<UiContainerExpr, UiVisitScope> map = newMap(UiContainerExpr.class, UiVisitScope.class, MUTABLE_INSERTION_ORDERED);
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
                        final UiVisitScope scope = generateTag(tools, source, me, ui, pkg, tagName, mode);
                        map.put(ui, scope);
                });
            } else {
                // Use the names of the {keys: ofJson}
                json.getPairs().forEach(pair->{
                    final String keyName = tools.resolveString(me.getContext(), pair.getKeyExpr());
                    final Expression value = tools.resolveVar(me.getContext(), pair.getValueExpr());
                    if (value instanceof UiContainerExpr) {
                        String pkg = tools.getPackage(ctx, value, rootPkg);
                        final UiContainerExpr ui = (UiContainerExpr) value;
                        final UiVisitScope scope = generateTag(tools, source, me, ui, pkg, keyName, mode);
                        map.put(ui, scope);
                    } else {
                        throw new IllegalArgumentException("Invalid json in define-tags; expected dom values:" +
                            " <define-tags tags={name: <define-tag />} />; You sent: " + tools.debugNode(value));
                    }
                });
            }
            // Now, return a scope which selects the correct sub-scope for each <define-tag />
            UiVisitScope s = new UiVisitScope(ScopeType.CONTAINER);
            s.getFeatureOverrides().put("imports", new UiFeatureGenerator());
            s.getFeatureOverrides().put("tags", new UiFeatureGenerator());
            s.getComponentOverrides().put("define-tag", new UiComponentGenerator() {
                @Override
                public UiVisitScope startVisit(
                    UiGeneratorTools tools,
                    ComponentBuffer source,
                    ContainerMetadata me,
                    UiContainerExpr n,
                    UiGenerateMode mode
                ) {
                    return map.get(n);
                }
            });
            return s;
        } else if (tags instanceof UiContainerExpr){
            // Just a single item in the define-tags list.
            UiContainerExpr ui = (UiContainerExpr) tags;
            final String tagName = tools.resolveString(me.getContext(), ui.getAttributeNotNull("name"));
            // look in the current <ui /> for a package
            String pkg = tools.getPackage(ctx, ui, rootPkg);
            return generateTag(tools, source, me, ui, pkg, tagName, mode);
        } else {
            throw new IllegalArgumentException("define-tags must have a `tags` feature that is either {name: json()}, <dom/>, or [<doms/>]");
        }
    }

    protected String getDefaultPackage() {
        return "xapi.ui.generated";
    }

    protected UiVisitScope generateTag(
        UiGeneratorTools tools,
        ComponentBuffer source,
        ContainerMetadata me,
        UiContainerExpr n,
        String defaultPackage,
        String tagName,
        UiGenerateMode mode
    ) {
        String pkg = tools.getPackage(me.getContext(), n, ()->{
            if (defaultPackage == null) {
                return getDefaultPackage();
            }
            return defaultPackage;
        });
        String name = tools.resolveString(me.getContext(), n.getAttribute("name")
            .ifAbsentSupplyLazy(UiAttrExpr::of, immutable1("name"), ()->stringLiteral(tagName))
            .getExpression());

        final UiVisitScope scope = new UiVisitScope(ScopeType.CONTAINER);

        final StringTo<UiFeatureGenerator> features = scope.getFeatureOverrides();

        final UiFeatureGenerator apiGen = new UiTagApiGenerator(pkg, name, this),
                                 uiGen = new UiTagUiGenerator(pkg, name, this),
                                 modelGen = new UiTagModelGenerator(pkg, name, this),
                                 renderGen = new UiTagRenderGenerator(pkg, name, this),
                                 genericsGen = new UiTagGenericsGenerator(pkg, name, this),
                                 inputGen = new UiTagInputGenerator(pkg, name, this),
                                 cssGen = new UiTagCssGenerator(pkg, name, this),
                                 callbackGen = new UiTagCallbackGenerator(pkg, name, this)
                                 ;
        // lightdom
        features.put("ui", uiGen);
        // shadowdom
        features.put("shadow", uiGen);
        // offscreen (things you want to plop in when actions happen)
        features.put("hidden", uiGen);

        // We already extract these manually
        features.put("name", UiFeatureGenerator.DO_NOTHING);
        features.put("package", UiFeatureGenerator.DO_NOTHING);
        features.put("tagName", UiFeatureGenerator.DO_NOTHING);

        // TODO: preparse model features?
        features.put("model", modelGen);
        features.put("render", renderGen);

        features.put("api", apiGen);
        features.put("impl", apiGen);

        features.put("onCreated", callbackGen);
        features.put("onAttached", callbackGen);
        features.put("onDetached", callbackGen);
        features.put("onAttributeChanged", callbackGen);

        for (String prefix : ((UiGeneratorTools<?>)tools).getImplPrefixes()) {
            features.put("impl-" + prefix, apiGen);
            features.put(prefix + "-impl", apiGen);
        }

        features.put("generics", genericsGen);
        features.put("input", inputGen);

        features.put("style", cssGen);
        features.put("css", cssGen);
        features.put("class", cssGen);

        return scope;
    }

    private String toClassName(String name) {
        String[] bits = name.split("-");
        StringBuilder b = new StringBuilder();
        for (String bit : bits) {
            b.append(X_String.toTitleCase(bit));
        }
        return b.toString();
    }

    public boolean isModelReference(Expression expr) {
        if (expr instanceof NameExpr) {
            return "$model".equals(((NameExpr)expr).getName());
        } else if (expr instanceof MethodCallExpr) {
            return "getModel".equals(((MethodCallExpr)expr).getName());
        } else if (expr instanceof FieldAccessExpr) {
            return "$model".equals(((FieldAccessExpr)expr).getField());
        } else if (expr instanceof TemplateLiteralExpr) {
            return "getModel()".equals(((TemplateLiteralExpr)expr).getValueWithoutTicks());
        } else {
            return false;
        }
    }

    public Expression resolveReference(
        UiGeneratorTools tools,
        ApiGeneratorContext ctx,
        GeneratedUiComponent component,
        GeneratedJavaFile target,
        Out1<String> rootRefField,
        Out1<String> parentField,
        UiAttrExpr attr
    ) {
        resolveReference(tools, ctx, component, target, rootRefField, parentField, attr.getExpression(), true);
        return attr.getExpression();
    }

    public Expression resolveReference(
        UiGeneratorTools tools,
        ApiGeneratorContext ctx,
        GeneratedUiComponent component,
        GeneratedJavaFile target,
        Out1<String> rootRefField,
        Out1<String> parentField,
        Expression expression,
        boolean rewriteParent
    ) {

        final Do undo = UiTagGenerator.this.resolveSpecialNames(
            ctx,
            component,
            target,
            rootRefField,
            parentField
        );
        final Expression param;
        try {
            param = tools.resolveVar(ctx, expression);
            final Node toMod = rewriteParent ? param.getParentNode() : param;
            Out1<Expression> modelOrData = () ->
                (Expression)toMod.accept(new ModifierVisitorAdapter<Expression>() {
                    private QualifiedNameExpr lastQualified;
                    private Node replaceQualified;

                    @Override
                    public Node visit(FieldAccessExpr n, Expression arg) {
                        if (n.getScope().toSource().equals("$model")) {
                            final GeneratedUiField field = component.getPublicModel().getFields().get(
                                n.getField());
                            if (field == null) {
                                throw new IllegalArgumentException("No model field of name " + n.getField() + " declared");
                            }
                            Expression newScope = tools.resolveVar(ctx, n.getScope());
                            if (newScope == n.getScope()) {
                                newScope = new MethodCallExpr(null, "getModel");
                            }
                            MethodCallExpr replaceWith = new MethodCallExpr(newScope, field.getterName());
                            replaceWith.addExtra(EXTRA_MODEL_INFO, field);
                            return replaceWith;
                        }
                        return super.visit(n, arg);
                    }

                    @Override
                    public Node visit(QualifiedNameExpr n, Expression arg) {
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
                    public Node visit(MethodReferenceExpr n, Expression arg) {
                        switch (n.getScope().toSource()) {
                            case "$model":
                                final GeneratedUiField field = component.getPublicField(n.getIdentifier());
                                if (field == null) {
                                    throw new IllegalArgumentException("No model field of name " + n.getIdentifier() + " declared");
                                }
                                // For now, model references in ui always assumed to be a read operation.
                                // TODO: put in scope barriers so we only make this assumption in ui tags, but not in
                                // structures which will have other interpretations.
                                Expression newScope = tools.resolveVar(ctx, n.getScope());
                                if (newScope == n.getScope()) {
                                    newScope = new MethodCallExpr(null, "getModel");
                                }
                                MethodCallExpr replaceWith = new MethodCallExpr(newScope, field.getterName());
                                replaceWith.addExtra(EXTRA_MODEL_INFO, field);
                                return replaceWith;
                            case "$this":
                                return AstConstants.THIS_REF;
                        }
                        return super.visit(n, arg);
                    }

                    @Override
                    public Node visit(NameExpr n, Expression arg) {
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
                String scopeType = getScopeType(tools, ((MethodCallExpr) param).getScope());
                if ("$model".equals(scopeType) || "$data".equals(scopeType)) {
                    return modelOrData.out1();
                }
            } else if (param instanceof MethodReferenceExpr) {
                String scopeType = getScopeType(tools, ((MethodReferenceExpr)param).getScope());
                if ("$model".equals(scopeType) || "$data".equals(scopeType)) {
                    return modelOrData.out1();
                }
            } else if (param instanceof FieldAccessExpr) {
                // A field access on a data or model node is special.
                // We will map it to the correct method for you.
                String scopeType = getScopeType(tools, ((FieldAccessExpr)param).getScope());
                if ("$model".equals(scopeType) || "$data".equals(scopeType)) {
                    return modelOrData.out1();
                }
            } else if (param instanceof NameExpr) {
                NameExpr rootName = (NameExpr) param;
                while (rootName instanceof QualifiedNameExpr) {
                    rootName = ((QualifiedNameExpr)rootName).getQualifier();
                }
                switch (rootName.getName()) {
                    case "$model":
                    case "$data":
                       return modelOrData.out1();
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
            return param;
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
        if (scope instanceof TemplateLiteralExpr) {
            return ((TemplateLiteralExpr) scope).getValueWithoutTicks();
        }
        if (scope instanceof MethodCallExpr) {
            return getScopeType(tools, ((MethodCallExpr)scope).getScope());
        }
        if (scope instanceof FieldAccessExpr) {
            return getScopeType(tools, ((FieldAccessExpr)scope).getScope());
        }
        X_Log.trace(UiTagGenerator.class, "Unable to determine scope of ", Out1.newOut1(()->tools.debugNode(scope)));
        return null;
    }

    public Do resolveSpecialNames(
        ApiGeneratorContext ctx,
        GeneratedUiComponent component,
        GeneratedJavaFile cls,
        Out1<String> rootRefField,
        Out1<String> parentField
    ) {
        // TODO: create and use a LiteralCache so equivalent templates pass object identity semantics
        Do start = ctx.addToContext("$Self", templateLiteral(cls.getWrappedName()));
        start = start.doAfter(ctx.addToContext("$this", templateLiteral(cls.getWrappedName() + ".this")));
        start = start.doAfter(ctx.addToContext("$Api", templateLiteral(
            component.getApi().getWrappedName()
        )));
        start = start.doAfter(ctx.addToContext("$Base", templateLiteral(
            component.getBase().getWrappedName()
        )));

        if (rootRefField != null) {
            start = start.doAfter(ctx.addToContext("$root", stringLiteral(rootRefField)));
        }
        if (parentField != null) {
            start = start.doAfter(ctx.addToContext("$parent", stringLiteral(parentField)));
        }
        if (component.hasPublicModel()) {
            start = start.doAfter(ctx.addToContext("$model", templateLiteral("getModel()")));
        }
        if (component.hasPrivateModel()) {
            start = start.doAfter(ctx.addToContext("$data", templateLiteral("getData()")));
        }
        return start;
    }

    public boolean requireCompact(UiContainerExpr n) {

        return false;
    }

    public MethodBuffer toDomMethod(
        UiNamespace namespace,
        GeneratedUiLayer ui
    ) {
        ClassBuffer output = ui.getSource().getClassBuffer();
        String builderType = output.addImport(ui.getElementBuilderType(namespace));
        // TODO check if this builderType expects type parameters...
        final MethodBuffer method = ui.getOrCreateMethod(X_Modifier.PUBLIC,  builderType, "toDom");
        return method;

    }

    protected void maybeAddImports(
        UiGeneratorTools tools,
        ApiGeneratorContext ctx,
        GeneratedJavaFile api,
        UiAttrExpr attr
    ) {
        // This is a really handy method... we should move it somewhere upstream for others to enjoy.
        // like putting it in GeneratedJavaFile, or UiComponentGenerator (or both)
        attr.getAnnotationsMatching(anno->X_String.containsAny(anno.getNameString().toLowerCase(), "import", "importstatic", "staticimport"))
            .forAll(anno->anno.getMembers().forEach(pair->{
                boolean isStatic = anno.getNameString().toLowerCase().contains("static");
                In1<String> addImport = isStatic ? api.getSource()::addImportStatic : api.getSource()::addImport;
                final Expression resolvedImport = tools.resolveVar(ctx, pair.getValue());
                String toImport;
                if (resolvedImport instanceof StringLiteralExpr) {
                    toImport = ((StringLiteralExpr)resolvedImport).getValue();
                    addImport.in(toImport);
                } else if (resolvedImport instanceof TemplateLiteralExpr) {
                    toImport = tools.resolveTemplate(ctx, (TemplateLiteralExpr) resolvedImport);
                    addImport.in(toImport);
                } else if (resolvedImport instanceof ArrayInitializerExpr) {
                    ArrayInitializerExpr many = (ArrayInitializerExpr) resolvedImport;
                    for (Expression expr : many.getValues()) {
                        toImport = tools.resolveString(ctx, expr);
                        addImport.in(toImport);
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

        resolveReference(tools, ctx, me.getGeneratedComponent(), cls, null, null, member, true);

        // whenever we resolve special names in an ast block's context, we always rollback anything we added.
        // this prevents variables from leaking out of their intended scope, at the expense of ugly try/finally undo.done()s
        final Do undo = resolveSpecialNames(ctx, me.getGeneratedComponent(), cls, null, null);
        try {

            UiMemberTransformer transformer = new UiMemberTransformer(cls, me);
            final Expression result = (Expression) transformer.visit(member, new UiMemberContext(ctx)
                .setContainer(me)
                .setTools(tools)
                .setGenerator(this)
            );

            final String src = tools.resolveLiteral(ctx, result);
            if (!src.trim().isEmpty()) {
                // allow an escape hatch for the transformer to add (and index!) stuff added to the class buffer
                cls.getSource().getClassBuffer()
                    .println()
                    .printlns(src);
            }

            recordMember(cls, member, result, src);

        } finally {
            undo.done();
        }
    }

    private void recordMember(
        GeneratedJavaFile cls,
        DynamicDeclarationExpr member,
        Expression result,
        String src
    ) {
        cls.addMethod(new UserDefinedMethod(member, result, src));
    }

    @Override
    public void endVisit(
        UiGeneratorTools tools, ContainerMetadata me, UiContainerExpr n, UiVisitScope scope
    ) {
        Do[] jobs;

        int loopBuster = 50;
        while ( (jobs = undos.toArray(Do[]::new)).length > 0 && loopBuster --> 0) {
            undos.clear();
            for (Do job : jobs) {
                job.done();
            }

        }
        assert loopBuster > 0 : "Hit loop buster in endVisit of " + this + "; check that your " +
            "task which calls .onComponentComplete is not adding itself back on every loop.";
        super.endVisit(tools, me, n, scope);
    }

    public void onComponentComplete(Do task) {
        undos.add(task);
    }

    public AssembledUi newAssembly(ComponentBuffer componentBuffer, UiGeneratorTools tools, UiNamespace namespace) {
        return new AssembledUi(componentBuffer, tools, namespace, this);
    }

    public boolean isCompatible(UiTagGenerator generator) {
        return generator == this;
    }

    public final AssembledElement attachChild(
        AssembledUi assembly,
        AssembledElement root,
        UiContainerExpr n
    ) {
        final AssembledElement e;
        switch (n.getName().toLowerCase(Locale.US)) {
            case "if":
                e = attachConditional(assembly, root, n);
                break;
            case "for":
                e = attachLoop(assembly, root, n);
                break;
            case "native":
            case "native-element":
                e = attachNativeElement(assembly, root, n);
                break;
            default:
                e = attachDefault(assembly, root, n);
        }
        return maybeSwap(assembly, root, e);
    }

    protected AssembledElement attachDefault(
        AssembledUi assembly,
        AssembledElement root,
        UiContainerExpr n
    ) {
        return new AssembledElement(assembly, root, n);
    }

    protected AssembledElement attachNativeElement(
        AssembledUi assembly,
        AssembledElement root,
        UiContainerExpr n
    ) {
        return new AssemblyNative(assembly, root, n);
    }

    protected AssembledElement attachLoop(
        AssembledUi assembly,
        AssembledElement root,
        UiContainerExpr n
    ) {
        return new AssemblyFor(assembly, root, n);
    }

    protected AssembledElement attachConditional(
        AssembledUi assembly,
        AssembledElement root,
        UiContainerExpr n
    ) {
        return new AssemblyIf(assembly, root, n);
    }

    protected AssembledElement maybeSwap(
        AssembledUi assembly,
        AssembledElement root,
        AssembledElement e
    ) {
        return e.spyGenerator(this);
    }

    public UiAssembler startAssembly(
        AssembledUi assembled,
        Expression uiExpr,
        boolean shadowUi,
        boolean hidden
    ) {
        final AssemblyRoot root = shadowUi ? assembled.requireLayoutRoot() : assembled.requireLogicalRoot();
        return new DefaultUiAssembler(assembled, root, hidden);
    }

    public TagAssembler getDefaultAssembler(
        UiAssembler assembler,
        AssembledUi assembly,
        AssembledElement e
    ) {
        final UiContainerExpr ast = e.getAst();
        String name = ast.getName();
        return tagFactories.getMaybe(name)
            .mapIfPresent(In2Out1::io, assembly, e)
            .ifAbsentSupply(DefaultTagAssembler::new, assembly);
    }

    public void addTagFactory(String name, In2Out1<AssembledUi, AssembledElement, TagAssembler> assemblerFactory) {
        tagFactories.put(name, assemblerFactory);
    }

}
