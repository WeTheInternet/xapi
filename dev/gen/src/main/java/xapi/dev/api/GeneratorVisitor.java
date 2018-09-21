package xapi.dev.api;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.DumpVisitor;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.impl.SimpleStack;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Do;
import xapi.fu.Filter.Filter1;
import xapi.fu.Maybe;
import xapi.fu.Mutable;
import xapi.fu.Out2;
import xapi.fu.Printable;
import xapi.fu.itr.GrowableIterator;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.source.read.JavaVisitor;
import xapi.util.X_Runtime;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.WeakHashMap;

import static com.github.javaparser.ast.expr.TemplateLiteralExpr.templateLiteral;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/17/16.
 */
public class GeneratorVisitor <Ctx extends ApiGeneratorContext<Ctx>>
    extends VoidVisitorAdapter<Ctx> implements ApiGeneratorTools<Ctx> {

    private static final String DUMP_WRAP_KEY = "wrapVisit";
    private static final Out2<Do,Do> DEFAULT_REMAPPER = Out2.out2Immutable(Do.NOTHING, Do.NOTHING);

    private final Path relativePath;
    private String pkgName;
    boolean globalInput;
    private final WeakHashMap<UiContainerExpr, GrowableIterator<Do>> undos;
    private UiContainerExpr container;

    public GeneratorVisitor(Path relativePath) {
        this.relativePath = relativePath;
        undos = new WeakHashMap<>();
    }

    protected Class<Ctx> contextClass() {
        return Class.class.cast(ApiGeneratorContext.class);
    }

    @Override
    public void visit(UiContainerExpr n, Ctx ctx) {
        final UiContainerExpr was = container;
        container = n;
        try {
            switch (n.getName()) {
                case "generate":
                    generate(n, ctx);
                    return;
                case "generateType":
                    n.accept(generateType(true), ctx);
                    break;
                case "generateClass":
                    n.accept(generateType(false), ctx);
                    break;
                case "loop":
                    final Expression from = n.getAttributeNotNull("from").getExpression();
                    final Expression to = n.getAttributeNotNull("to").getExpression();
                    final Expression var = n.getAttributeNotNull("var").getExpression();
                    String varName = resolveString(ctx, var);
                    for (int i = resolveInt(ctx, from), m = resolveInt(ctx, to); i <= m; i++) {
                        final Do undo = ctx.addToContext(varName, IntegerLiteralExpr.intLiteral(i));
                        super.visit(n, ctx);
                        undo.done();
                    }
                    break;
                case "var":
                    final Maybe<UiAttrExpr> value = n.getAttribute("value");
                    final Maybe<UiAttrExpr> dflt = n.getAttribute("default");
                    String name = resolveString(ctx, n.getAttributeNotNull("name").getExpression());
                    if (value.isPresent() && dflt.isPresent()) {
                        throw new IllegalStateException("A var cannot have both a value and a default attribute");
                    }
                    if (!value.isPresent() && !dflt.isPresent()) {
                        throw new IllegalStateException("A var must have either a value or a default attribute");
                    }

                    Do undo;
                    if (value.isPresent()) {
                        undo = ctx.addToContext(name, value.get().getExpression());
                    } else if (!ctx.hasNode(name)) {
                        undo = ctx.addToContext(name, dflt.get().getExpression());
                    } else {
                        undo = Do.NOTHING;
                    }

                    if (!globalInput) {
                        // global inputs won't be scoped.
                        undosFor(n).concat(undo);
                    }
                default:
                    super.visit(n, ctx);
            }
        } finally {
            final GrowableIterator<Do> undo = undos.remove(n);
            if (undo != null) {
                undo.forEachRemaining(Do::done);
            }
            container = was;
        }
    }

    protected void generate(UiContainerExpr n, Ctx arg) {
        SimpleStack<Do> undos = new SimpleStack<>();
        try {
            for (UiAttrExpr attr : n.getAttributes()) {
                switch (attr.getNameString().toLowerCase()) {
                    case "generateType":
                    case "generateClass":
                    default:
                        try {
                            final Method method = getClass().getMethod(
                                attr.getNameString(),
                                UiAttrExpr.class,
                                contextClass()
                            );
                            method.invoke(this, attr, arg);
                        } catch (NoSuchMethodException e) {
                            // if there is no matching method,
                            // then store this attribute value in the context.
                            final Do undo = arg.addToContext(attr.getNameString(), attr.getExpression());
                            undos.add(undo);
                        } catch (Exception e) {
                            throw rethrow(e);
                        }
                }
            }
        } finally {
            undos.forEach(Do::done);
        }
    }

    @Override
    public void visit(MemberValuePair n, Ctx arg) {
        if ("in".equals(n.getName())) {
            final boolean was = globalInput;
            globalInput = true;
            super.visit(n, arg);
            globalInput = was;
        } else {
            super.visit(n, arg);
        }
    }

    @Override
    public void visit(MethodDeclaration n, Ctx arg) {

        super.visit(n, arg);
    }

    @Override
    public void visit(PackageDeclaration n, Ctx arg) {
        pkgName = n.getPackageName();
        super.visit(n, arg);
    }

    public void loop(UiAttrExpr attr, Ctx arg) {
        JsonContainerExpr body = (JsonContainerExpr) attr.getExpression();
        IntegerLiteralExpr from = ((IntegerLiteralExpr)body.getNode("from"));
        IntegerLiteralExpr to = ((IntegerLiteralExpr)body.getNode("to"));
        String varName = ASTHelper.extractStringValue(body.getNode("var"));
        final Do setStart = arg.addToContext(varName + "Start", from);
        final Do setEnd = arg.addToContext(varName + "End", to);
        final JsonContainerExpr tasks = (JsonContainerExpr) body.getNode("tasks");
        try {
            while (from.intValue() <= to.intValue()) {
                final Do setVar = arg.addToContext(varName, from);
                tasks.getPairs().forEach(entry->{
                    String taskName = ASTHelper.extractStringValue(entry.getKeyExpr());
                    try {
                        final VoidVisitor<Ctx> visitor = (VoidVisitor<Ctx>) getClass().getMethod(taskName).invoke(GeneratorVisitor.this);
                        entry.getValueExpr().accept(visitor, arg);
                    } catch (Exception e) {
                        rethrow(e);
                    }
                });
                from = IntegerLiteralExpr.intLiteral(from.intValue()+1);
                setVar.done();
            }
        } finally {
            setStart.done();
            setEnd.done();
        }
    }

    public VoidVisitor<Ctx> generateType(boolean isInterface) {
        return new VoidVisitorAdapter<Ctx>() {
            public Expression typeParams, extend, implement, name;
            public JsonContainerExpr defaultMethods, staticMethods, methods;

            @Override
            public void visit(UiAttrExpr n, Ctx ctx) {
                switch (n.getNameString()) {
                    case "template":
                        DynamicDeclarationExpr decl = (DynamicDeclarationExpr) n.getExpression();
                        ClassOrInterfaceDeclaration template = (ClassOrInterfaceDeclaration) decl.getBody();
                        // We need to transform this declaration into its final form.
                        final List<ImportDeclaration> imports = new ArrayList<>();
                        final List<AnnotationExpr> annos = new ArrayList<>();
                        n.getAnnotations().forEach(anno->{
                            if ("import".equalsIgnoreCase(anno.getNameString().toLowerCase())) {
                                anno.getMembers().forEach(member->{
                                    IntTo<String> importDecls = resolveToLiterals(ctx, member.getValue());
                                    importDecls.forEachValue(importDecl->{
                                        boolean isStatic = importDecl.contains("static ");
                                        if (isStatic) {
                                            importDecl = importDecl.replace("static ", "").trim();
                                        }
                                        boolean isAsterisk = importDecl.contains(".*");
                                        if (isAsterisk) {
                                            importDecl = importDecl.replace(".*", "");
                                        }
                                        imports.add(new ImportDeclaration(new NameExpr(importDecl),
                                            isStatic, isAsterisk));
                                    });
                                });
                            } else {
                                annos.add(anno);
                            }
                        });
                        template.setAnnotations(annos);
                        final List<TypeDeclaration> asList = new ArrayList<>();
                        asList.add((TypeDeclaration) template.clone());
                        CompilationUnit unit = new CompilationUnit(findPackage(ctx), imports, asList);
                        generateAndSave(ctx, unit);
                        return;
                    case "var":
                        if (!(n.getExpression() instanceof JsonContainerExpr)) {
                            throw new IllegalArgumentException("A var attribute must have a json container child; you sent " + n);
                        }
                        JsonContainerExpr vars = (JsonContainerExpr) n.getExpression();
                        if (vars.isArray()) {
                            throw new IllegalArgumentException("A var attribute cannot be a json array; you sent " + n);
                        }
                        vars.getPairs().forEach(pair->{
                            String key = resolveString(ctx, pair.getKeyExpr());
                            final Do undo = ctx.addToContext(key, pair.getValueExpr());
                            undosFor(container).concat(undo);
                        });
                        return;
                }
                super.visit(n, ctx);
            }

            @Override
            public void visit(JsonContainerExpr n, Ctx ctx) {
                super.visit(n, ctx);
                // We've now visited all the named pairs;
                // lets generate the types we expect!
                if (name == null) {
                    throw new IllegalStateException("generateType task must have a name entry");
                }
                String nameString;
                if (name instanceof TemplateLiteralExpr) {
                    nameString = ((TemplateLiteralExpr)name).getValueWithoutTicks();
                } else if (name.getClass() == StringLiteralExpr.class) {
                    nameString = ((StringLiteralExpr)name).getValue();
                } else {
                    throw new IllegalArgumentException("name entry in generateType must be a string or template literal expression");
                }
                int dollarInd = nameString.indexOf('$');
                if (dollarInd != -1) {
                    nameString = resolveValues(ctx, nameString);
                }
                String pkg;
                if (nameString.indexOf('.') == -1) {
                    // No package?  Lets guess one for you...
                    pkg = ctx.getString("package");
                    pkg = maybeGuessPackage(pkg);
                } else {
                    pkg = X_Source.toPackage(nameString);
                    nameString = X_Source.removePackage(pkg, nameString);
                }
                SourceBuilder<Ctx> builder = ctx.getOrMakeClass(pkg, nameString, isInterface);
                generateType(ctx, builder, typeParams, extend, implement, methods, defaultMethods, staticMethods);
            }

            @Override
            public void visit(JsonPairExpr n, Ctx arg) {
                switch (ASTHelper.extractStringValue(n.getKeyExpr())) {
                    case "typeParams":
                        typeParams = n.getValueExpr();
                        break;
                    case "name":
                        name = n.getValueExpr();
                        break;
                    case "extend":
                    case "extends":
                        extend = n.getValueExpr();
                        break;
                    case "implement":
                    case "implements":
                        implement = n.getValueExpr();
                        break;
                    case "methods":
                        if (n.getValueExpr() instanceof MethodCallExpr) {
                            methods = resolveMethod(arg, (MethodCallExpr) n.getValueExpr());
                        } else {
                            methods = (JsonContainerExpr) n.getValueExpr();
                        }
                        if (!methods.isArray()) {
                            List<JsonPairExpr> pairs = new ArrayList<>();
                            pairs.add(
                                new JsonPairExpr(IntegerLiteralExpr.intLiteral(0), methods)
                            );
                            methods = new JsonContainerExpr(true, pairs);
                        }
                        break;
                    case "defaultMethods":
                        assert isInterface : "Classes should not have default methods! Bad node: " + n.getParentNode();
                        defaultMethods = (JsonContainerExpr) n.getValueExpr();
                        if (!defaultMethods.isArray()) {
                            List<JsonPairExpr> pairs = new ArrayList<>();
                            pairs.add(
                                new JsonPairExpr(IntegerLiteralExpr.intLiteral(0), defaultMethods)
                            );
                            defaultMethods = new JsonContainerExpr(true, pairs);
                        }
                        break;
                    case "staticMethods":
                        staticMethods = (JsonContainerExpr) n.getValueExpr();
                        if (!staticMethods.isArray()) {
                            List<JsonPairExpr> pairs = new ArrayList<>();
                            pairs.add(
                                new JsonPairExpr(IntegerLiteralExpr.intLiteral(0), staticMethods)
                            );
                            staticMethods = new JsonContainerExpr(true, pairs);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unhandled generateType json member " + n);
                }
            }
        };
    }

    protected String resolveValues(Ctx ctx, String nameString) {
        return ctx.resolveValues(nameString, item->resolveString(ctx, (Expression)item));
    }

    protected void generateAndSave(Ctx ctx, CompilationUnit unit) {
        // We have been given a CompilationUnit that contains template values to replace.
        // Lets transform it into a source file, and then save it.
        final TypeDeclaration primary = unit.getPrimaryType();
        String pkgName = resolveString(ctx, templateLiteral(
            unit.getPackage().getPackageName()
        ));
        primary.setPackage(pkgName);
        String typeName = resolveString(ctx, templateLiteral(
            primary.getName()
        ));
        primary.setName(typeName);
        final SourceBuilder<Ctx> builder = ctx.getOrMakeClass(
            pkgName,
            typeName,
            primary.isInterface()
        );

        ModifierVisitorAdapter<Ctx> mods = modifyUnit(ctx, unit);
        unit.accept(mods, ctx);

        DumpVisitor visitor = new DumpVisitor() {
            @Override
            protected Printable createSourcePrinter() {
                return builder.getClassBuffer();
            }

            @Override
            public void visit(MethodDeclaration n, Object arg) {
                Out2<Do, Do> todo = n.getExtra(DUMP_WRAP_KEY);
                if (todo == null) {
                    super.visit(n, arg);
                    return;
                }
                todo.out1().done();
                super.visit(n, arg);
                todo.out2().done();
            }

            @Override
            public void visit(SingleMemberAnnotationExpr n, Object arg) {
                if (isRemoveFromOutput(n)) {
                    return;
                }
                super.visit(n, arg);
            }

            @Override
            public void visit(NormalAnnotationExpr n, Object arg) {
                if (isRemoveFromOutput(n)) {
                    return;
                }
                super.visit(n, arg);
            }

            @Override
            public void visit(MethodCallExpr n, Object arg) {
                if (n.getName().startsWith("$")) {
                    final IntTo<String> lits = resolveToLiterals(ctx, n);
                    // Super-hack... to insert arbitrary code where we don't
                    // know / want to parse the internal structure, we just
                    // replace this code with a single NameExpr that
                    // happens to contain arbitrary code
                    super.visit(new NameExpr(lits.join(", ")), arg);
                } else {
                    super.visit(n, arg);
                }
            }

            @Override
            public void visit(MethodReferenceExpr n, Object arg) {
                if (n.getIdentifier().startsWith("$")) {
                    final IntTo<String> result = resolveToLiterals(ctx, new NameExpr(n.getIdentifier()));
                    MethodReferenceExpr copy = (MethodReferenceExpr) n.clone();
                    copy.setIdentifier(result.join(""));
                    super.visit(copy, arg);
                } else {
                    super.visit(n, arg);
                }
            }

            @Override
            public void visit(ClassOrInterfaceDeclaration n, Object arg) {
                super.visit(n, arg);
            }

            @Override
            protected String resolveName(NameExpr name) {
                return resolveTemplate(ctx, templateLiteral(name.getName()));
            }

            @Override
            protected String resolveType(ClassOrInterfaceType type) {
                return resolveTemplate(ctx, templateLiteral(type.getName()));
            }

            @Override
            protected String resolveTypeParamName(TypeParameter typeParam) {
                if (ctx.hasNode(typeParam.getName())) {
                    final IntTo<String> literals = resolveToLiterals(
                        ctx,
                        (Expression) ctx.getNode(typeParam.getName())
                    );
                    if (literals.size() > 1) {
                        X_Log.trace(GeneratorVisitor.class, "Type parameter as variable returned more than one item...", literals, " from ", typeParam.getName());
                    }
                    return literals.join(", "); // scary...
                }
                return super.resolveTypeParamName(typeParam);
            }
        };
        unit.accept(visitor, null);
        if (X_Runtime.isDebug()) {
            System.out.println(builder.toSource());
        }
    }

    protected ModifierVisitorAdapter<Ctx> modifyUnit(Ctx ctx, CompilationUnit unit) {
        ObjectTo.Many<String, BodyDeclaration> remapping =
            X_Collect.newMultiMap(String.class, BodyDeclaration.class);
        return new ModifierVisitorAdapter<Ctx>() {
            @Override
            public Node visit(MemberValuePair n, Ctx arg) {
                return super.visit(n, arg);
            }

            @Override
            public Node visit(AnnotationDeclaration n, Ctx arg) {
                AnnotationDeclaration result = (AnnotationDeclaration) super.visit(n, arg);
                final List<BodyDeclaration> members = result.getMembers();
                remapMembers(n, members);
                result.setMembers(members);
                return result;
            }

            @Override
            public Node visit(ClassOrInterfaceDeclaration n, Ctx arg) {
                ClassOrInterfaceDeclaration result = (ClassOrInterfaceDeclaration) super.visit(n, arg);
                final List<BodyDeclaration> members = result.getMembers();
                remapMembers(n, members);
                result.setMembers(members);
                return result;
            }

            @Override
            public Node visit(EnumDeclaration n, Ctx arg) {
                EnumDeclaration result = (EnumDeclaration) super.visit(n, arg);
                final List<BodyDeclaration> members = result.getMembers();
                remapMembers(n, members);
                result.setMembers(members);
                return result;
            }

            private void remapMembers(TypeDeclaration n, List<BodyDeclaration> members) {
                final ListIterator<BodyDeclaration> itr = members.listIterator(members.size());

                for (
                    ;itr.hasPrevious(); ) {
                    final BodyDeclaration member = itr.previous();
                    if (member instanceof TypeDeclaration) {
                        // nested types get special treatment.
                    }
                    String declKey = declarationKey(member);
                    if (remapping.containsKey(declKey)) {
                        itr.remove();
                        final Node oldParent = member.getParentNode();
                        member.setParentNode(null);
                        Mutable<Integer> cnt = new Mutable<>(0);
                        remapping.get(declKey)
                            .forEachValue(v->{
                                itr.add(v);
                                v.setParentNode(oldParent);
                                cnt.in(cnt.out1()+1);
                            });
                        while (cnt.out1() > 0) {
                            itr.previous();
                            cnt.in(cnt.out1()-1);
                        }
                    }
                }
                n.setMembers(members);
            }

            private String declarationKey(BodyDeclaration member) {
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration asMethod = (MethodDeclaration) member;
                    return asMethod.getName() + "#" + asMethod.getParameters();
                } else if (member instanceof FieldDeclaration) {
                    FieldDeclaration asField = (FieldDeclaration) member;
                    return asField.getVariables().toString();
                } else if (member instanceof TypeDeclaration) {
                    TypeDeclaration asType = (TypeDeclaration) member;
                    return asType.getQualifiedName();
                } else {
                    throw new IllegalArgumentException("No declaration key for " + debugNode(member));
                }
            }

            @Override
            public Node visit(MethodDeclaration n, Ctx arg) {
                if (!n.getAnnotations().isEmpty()) {
                    IntTo<MethodDeclaration> methods = X_Collect.newList(MethodDeclaration.class);
                    methods.add(n);
                    for (AnnotationExpr anno : n.getAnnotations()) {
                        switch (anno.getNameString().toLowerCase()) {
                            case "unfold":
                                if (isRemoveFromOutput(anno)) {
                                    break;
                                }
                                removeFromOutput(anno);
                                Mutable<Integer> from = new Mutable<>();
                                Mutable<Integer> to = new Mutable<>();
                                Mutable<String> name = new Mutable<>();
                                anno.getMembers().forEach(pair->{
                                    switch (pair.getName()) {
                                        case "from":
                                            from.in(resolveInt(ctx, pair.getValue()));
                                            break;
                                        case "to":
                                            to.in(resolveInt(ctx, pair.getValue()));
                                            break;
                                        case "var":
                                            name.in(resolveString(ctx, pair.getValue()));
                                            break;
                                        default:
                                            throw new IllegalArgumentException("Bad @unfold member name " + pair.getName());
                                    }
                                });
                                assert from.out1() != null : "Missing from= member in @unfold annotation " + anno;
                                assert to.out1() != null : "Missing to= member in @unfold annotation " + anno;
                                assert name.out1() != null : "Missing var= member in @unfold annotation " + anno;
                                // When unfolding, we want to multiplex one method
                                // declaration into multiple declarations.
                                // Note that we might want to unfold multiple times...
                                final IntTo<MethodDeclaration> newMethods = X_Collect.newList(MethodDeclaration.class);
                                methods.forEachValue(decl->{
                                    for (int i = from.out1(), m = to.out1(); i <= m; i++) {
                                        MethodDeclaration newDecl = (MethodDeclaration) decl.clone();
                                        // saves our scope providers, so we can safely release scope between runs
                                        deferVarResolution(ctx, newDecl, name.out1(), IntegerLiteralExpr.intLiteral(i));
                                        newMethods.add(newDecl);
                                    }
                                });
                                methods = newMethods;
                                remapping.put(declarationKey(n), (IntTo<BodyDeclaration>) methods.narrow());
                                methods.forAll(this::visit, ctx);
                                return n;
                            case "var":
                                if (isRemoveFromOutput(anno)) {
                                    break;
                                }
                                removeFromOutput(anno);
                                name = new Mutable<>();
                                Mutable<Expression> value = new Mutable<>();
                                Mutable<Boolean> isDefault = new Mutable<>();

                                Out2<Do, Do> todo = n.getExtra(DUMP_WRAP_KEY);
                                if (todo == null) {
                                    todo = DEFAULT_REMAPPER;
                                }
                                todo.out1().done();

                                anno.getMembers().forEach(pair-> {
                                    switch (pair.getName().toLowerCase()) {
                                        case "name":
                                            final String nameString = resolveString(ctx, pair.getValue());
                                            name.in(nameString);
                                            break;
                                        case "value":
                                            value.in(pair.getValue());
                                            break;
                                        case "default":
                                            if (value.isNull()) {
                                                value.in(pair.getValue());
                                                isDefault.in(true);
                                            } else {
                                                assert false : "You should not define both value= and default= in " + debugNode(anno);
                                            }
                                            break;
                                        default:
                                            throw new IllegalArgumentException("Unsupported var member name " + pair.getName() + " in " + anno);
                                    }
                                });

                                methods.forEachValue(decl->
                                    deferVarResolution(ctx, decl, name.out1(), value.out1(), Boolean.TRUE.equals(isDefault.out1()))
                                );
                                todo.out2().done();
                                break;
                            default:
                                // An annotation we don't treat as special...
                        }
                    }
                }
                return super.visit(n, arg);
            }

        };
    }

    protected void removeFromOutput(Node node) {
        node.addExtra("remove", true);
    }

    protected boolean isRemoveFromOutput(Node node) {
        return Boolean.TRUE.equals(node.getExtra("remove"));
    }

    protected void deferVarResolution(Ctx ctx, Node newDecl, String varName, Expression value) {
        deferVarResolution(ctx, newDecl, varName, value, false);
    }
    protected void deferVarResolution(Ctx ctx, Node newDecl, String varName, Expression value, boolean checkFirst) {
        Mutable<Do> undoer = new Mutable<>();
        final Out2<Do, Do> existing = newDecl.getExtra(DUMP_WRAP_KEY);
        final Out2<Do, Do> myTask = Out2.out2Immutable(
            () -> undoer.in(
                !checkFirst || !ctx.hasNode(varName) ?
                    ctx.addToContext(varName, resolveVar(ctx, value)) :
                    Do.NOTHING
            ),
            () -> undoer.out1().done()
        );
        if (existing == null) {
            newDecl.addExtra(DUMP_WRAP_KEY, myTask);
        } else {
            newDecl.addExtra(DUMP_WRAP_KEY, Out2.out2Immutable(
                existing.out1().doAfter(myTask.out1()),
                existing.out2().doAfter(myTask.out2())
            ));
        }
    }

    protected PackageDeclaration findPackage(Ctx ctx) {
        if (ctx.hasNode("package")) {
            return new PackageDeclaration(new NameExpr(ctx.getString("package")));
        }
        return new PackageDeclaration(new NameExpr(maybeGuessPackage(null)));
    }

    protected String maybeGuessPackage(String pkg) {
        if (pkg == null) {
            if (pkgName != null) {
                return pkgName;
            }
            // Use the source directory of the .xapi file...
            return relativePath.getParent().toString()
                .replace('/', '.')
                .replace('\\', '.');
        }
        return pkg;
    }

    protected GrowableIterator<Do> undosFor(UiContainerExpr container) {
        return undos.computeIfAbsent(container, k->new GrowableIterator<>());
    }

    private void generateType(
        Ctx arg,
        SourceBuilder<Ctx> builder,
        Expression typeParams,
        Expression extend,
        Expression implement,
        JsonContainerExpr methods,
        JsonContainerExpr defaultMethods,
        JsonContainerExpr staticMethods
    ) {
        if (typeParams != null) {
            addTypeParams(arg, builder, typeParams);
        }

        if (extend != null) {
            addExtends(arg, builder, extend);
        }
        if (implement != null) {
            addImplements(arg, builder, implement);
        }

        if (methods != null) {
            // just add the method definitions.
            addMethods(arg, builder, methods, Modifier.PUBLIC | Modifier.ABSTRACT);
        }

        if (defaultMethods != null) {
            assert builder.getClassBuffer().isInterface() : "Classes should not have default methods";
            addMethods(arg, builder, defaultMethods, Modifier.PUBLIC | JavaVisitor.MODIFIER_DEFAULT);
        }

        if (staticMethods != null) {
            addMethods(arg, builder, staticMethods, Modifier.PUBLIC | Modifier.STATIC);
        }

    }

    protected void addMethods(Ctx ctx, SourceBuilder<Ctx> builder, JsonContainerExpr methods, int modifiers) {
        methods.getPairs().forEach(pair->{
            Expression value = pair.getValueExpr();
            if (value instanceof MethodCallExpr) {
                // likely a call to method(type.class, "name", params ... )
                MethodCallExpr methodCall = (MethodCallExpr) value;
                switch (methodCall.getName()) {
                    case "method":
                        value = resolveMethod(ctx, methodCall);
                        break;
                    default:
                        throw new IllegalArgumentException("Unable to create a method definition " +
                            "using source method " + methodCall + " at " + methodCall.getCoordinates());
                }
            }
            if (value instanceof JsonContainerExpr) {
                // Add the method from this json definition
                JsonContainerExpr json = (JsonContainerExpr) value;
                addMethod(ctx, builder, json, modifiers);
            }
        });
    }

    protected void addMethod(Ctx ctx, SourceBuilder<Ctx> builder, JsonContainerExpr json, int modifiers) {
        if (json.hasNode("tasks")) {
            // We are running an internal loop here.
            // lets setup our vars and run the tasks...
            final String from = resolveLiteral(ctx, json.getNode("from"));
            final String to = resolveLiteral(ctx, json.getNode("to"));
            final String var = resolveLiteral(ctx, json.getNode("var"));
            Filter1<Integer> filter;
            if (json.hasNode("filter")) {
                Node filterExpr = json.getNode("filter");
                filter = i-> {
                    final Expression filtered = resolveVar(ctx, (Expression) filterExpr);
                    return ((BooleanLiteralExpr)filtered).getValue();
                };
            } else {
                filter = always->true;
            }
            final JsonContainerExpr tasks = (JsonContainerExpr) json.getNode("tasks");
            for (int i = Integer.parseInt(from), m = Integer.parseInt(to); i <= m; i++) {
                final Do undo = ctx.addToContext(var, IntegerLiteralExpr.intLiteral(i));
                if (filter.filter1(i)) {
                    addMethod(ctx, builder, tasks, modifiers);
                }
                undo.done();
            }
        } else {
            if (json.isArray()) {
                json.getPairs().forEach(pair->{
                    if (!(pair.getValueExpr() instanceof JsonContainerExpr)) {
                        throw new IllegalArgumentException("tasks arrays must contain only json object nodes");
                    }
                    JsonContainerExpr item = (JsonContainerExpr) pair.getValueExpr();
                    addMethod(ctx, builder, item, modifiers);
                });
            } else {
                if (json.hasNode("filter")) {
                    Node filterExpr = json.getNode("filter");
                    final AstFilter<Object> filter = i -> {
                        final Expression filtered = resolveVar(ctx, (Expression) filterExpr);
                        return ((BooleanLiteralExpr) filtered).getValue();
                    };
                    if (!filter.filter1(json)) {
                        return;
                    }
                }
                new ApiMethodGenerator<>(this, builder, json, modifiers)
                    .visit(json, ctx);
            }
        }
    }

    protected void addExtends(Ctx arg, SourceBuilder<Ctx> builder, Expression extend) {
        if (builder.getClassBuffer().isInterface()) {
            addImplements(arg, builder, extend);
        } else {
            if (extend instanceof JsonContainerExpr) {
                JsonContainerExpr json = (JsonContainerExpr) extend;
                assert json.isArray() : "extend members must be arrays, not objects";
                int size = json.size();
                if (size > 1) {
                    throw new IllegalArgumentException("A class cannot extend more than one type; you sent " + extend);
                }
                extend = json.getNode(0);
            }
            if (extend instanceof QualifiedNameExpr) {
                QualifiedNameExpr qualified = (QualifiedNameExpr) extend;
                if (qualified.getName().equals("class")) {
                    extend = qualified.getQualifier();
                }
            }
            if (extend instanceof NameExpr) {
                String name = ASTHelper.extractStringValue(extend);
                if (name.indexOf('.') != -1) {
                    if (name.substring(0, name.lastIndexOf('.')).equals(builder.getPackage())) {
                        name = name.substring(builder.getPackage().length() + 1);
                    } else {
                        name = builder.addImport(name);
                    }
                }
                builder.getClassBuffer().setSuperClass(name);
            } else {
                throw new IllegalArgumentException("Unable to transform node " + extend.getClass() + " : " + extend + " into a type literal");
            }
        }
    }
    protected void addImplements(Ctx arg, SourceBuilder<Ctx> builder, Expression extend) {
        if (extend instanceof JsonContainerExpr) {
            // We have a list type...
            JsonContainerExpr json = (JsonContainerExpr) extend;
            assert json.isArray() : "extend members must be arrays, not objects";
            json.getPairs().forEach(pair->
                addImplements(arg, builder, pair.getValueExpr())
            );
            return;
        } else if (extend instanceof QualifiedNameExpr){
            QualifiedNameExpr qualified = (QualifiedNameExpr) extend;
            if (qualified.getName().equals("class")) {
                extend = qualified.getQualifier();
            }
        }
        if (extend instanceof NameExpr) {
            String name = ASTHelper.extractStringValue(extend);
            if (name.indexOf('.') != -1) {
                if (name.substring(0, name.lastIndexOf('.')).equals(builder.getPackage())) {
                    name = name.substring(builder.getPackage().length() + 1);
                } else {
                    name = builder.addImport(name);
                }
            }
            builder.getClassBuffer().addInterface(name);
        } else {
            throw new IllegalArgumentException("Unable to transform node " + extend.getClass() + " : " + extend + " into a type literal");
        }
    }

    @Override
    public Expression resolveVar(Ctx ctx, Expression valueExpr) {
        Out2<Do, Do> remapping = getRemapper(valueExpr);
        remapping.out1().done();
        try {
            return ApiGeneratorTools.super.resolveVar(ctx, valueExpr);
        } finally {
            remapping.out2().done();
        }
    }

    private Out2<Do,Do> getRemapper(Expression valueExpr) {
        Out2<Do, Do> remapper = valueExpr.getExtra(DUMP_WRAP_KEY);
        return remapper == null ? DEFAULT_REMAPPER : remapper;
    }

    @Override
    public Expression resolveVarReverse(Expression value, Ctx ctx) {
        return ApiGeneratorTools.super.resolveVarReverse(value, ctx);
    }

    public void generateType(UiAttrExpr attr, Ctx arg) {
        JsonContainerExpr body = (JsonContainerExpr) attr.getExpression();
        final Expression typeParams = body.getNode("typeParams");
        final Expression extend = body.getNode("extend");
    }
}
