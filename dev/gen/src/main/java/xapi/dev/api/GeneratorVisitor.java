package xapi.dev.api;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.collect.impl.SimpleStack;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Do;
import xapi.source.X_Source;
import xapi.source.read.JavaVisitor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/17/16.
 */
public class GeneratorVisitor <Ctx extends ApiGeneratorContext<Ctx>>
    extends VoidVisitorAdapter<Ctx> implements ApiGeneratorTools<Ctx> {

    private final Path relativePath;

    public GeneratorVisitor(Path relativePath) {
        this.relativePath = relativePath;
    }

    protected Class<Ctx> contextClass() {
        return Class.class.cast(ApiGeneratorContext.class);
    }

    @Override
    public void visit(UiContainerExpr n, Ctx arg) {
        if ("generate".equals(n.getName())) {
            generate(n, arg);
        } else {
            super.visit(n, arg);
        }
    }

    protected void generate(UiContainerExpr n, Ctx arg) {
        SimpleStack<Do> undos = new SimpleStack<>();
        try {
            for (UiAttrExpr attr : n.getAttributes()) {
                switch (attr.getNameString().toLowerCase()) {
                    case "generateInterface":
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

    public VoidVisitor<Ctx> generateInterface() {
        return new VoidVisitorAdapter<Ctx>() {
            public Expression typeParams, extend, name;
            public JsonContainerExpr defaultMethods, staticMethods, methods;

            @Override
            public void visit(JsonContainerExpr n, Ctx arg) {
                super.visit(n, arg);
                // We've now visited all the named pairs;
                // lets generate the types we expect!
                if (name == null) {
                    throw new IllegalStateException("generateInterface task must have a name entry");
                }
                String nameString;
                if (name instanceof TemplateLiteralExpr) {
                    nameString = ((TemplateLiteralExpr)name).getValueWithoutTicks();
                } else if (name.getClass() == StringLiteralExpr.class) {
                    nameString = ((StringLiteralExpr)name).getValue();
                } else {
                    throw new IllegalArgumentException("name entry in generateInterface must be a string or template literal expression");
                }
                int dollarInd = nameString.indexOf('$');
                if (dollarInd != -1) {
                    nameString = arg.resolveValues(nameString);
                }
                String pkg;
                if (nameString.indexOf('.') == -1) {
                    // No package?  Lets guess one for you...
                    pkg = arg.getString("package");
                    if (pkg == null) {
                        // Use the source directory of the .xapi file...
                        pkg = relativePath.getParent().toString()
                            .replace('/', '.')
                            .replace('\\', '.');
                    }
                } else {
                    pkg = X_Source.toPackage(nameString);
                    nameString = X_Source.removePackage(pkg, nameString);
                }
                SourceBuilder<Ctx> builder = arg.newSourceFile(pkg, nameString, true);
                generateInterface(arg, builder, typeParams, extend, methods, defaultMethods, staticMethods);
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
                        extend = n.getValueExpr();
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
                        throw new IllegalArgumentException("Unhandled generateInterface json member " + n);
                }
            }
        };
    }

    private void generateInterface(
        Ctx arg,
        SourceBuilder<Ctx> builder,
        Expression typeParams,
        Expression extend,
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

        if (methods != null) {
            // just add the method definitions.
            addMethods(arg, builder, methods, Modifier.PUBLIC | Modifier.ABSTRACT);
        }

        if (defaultMethods != null) {
            addMethods(arg, builder, defaultMethods, Modifier.PUBLIC | JavaVisitor.MODIFIER_DEFAULT);
        }

        if (staticMethods != null) {
            addMethods(arg, builder, staticMethods, Modifier.PUBLIC | Modifier.STATIC);
        }

        System.out.println(builder);
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
            for (int i = Integer.parseInt(from), m = Integer.parseInt(to); i <= m; i++) {
                final Do undo = ctx.addToContext(var, IntegerLiteralExpr.intLiteral(i));
                addMethod(ctx, builder, (JsonContainerExpr) json.getNode("tasks"), modifiers);
                undo.done();
            }
        } else {
            new ApiMethodGenerator<>(builder, json, modifiers)
                .visit(json, ctx);
        }
    }

    protected void addExtends(Ctx arg, SourceBuilder<Ctx> builder, Expression extend) {
        if (extend instanceof JsonContainerExpr) {
            // We have a list type...
            JsonContainerExpr json = (JsonContainerExpr) extend;
            assert json.isArray() : "extend members must be arrays, not objects";
            json.getPairs().forEach(pair->
                addExtends(arg, builder, pair.getValueExpr())
            );
            return;
        } else if (extend instanceof QualifiedNameExpr){
            QualifiedNameExpr qualified = (QualifiedNameExpr) extend;
            if (qualified.getName().equals("class")) {
                extend = qualified.getQualifier();
            } else {
                extend = qualified;
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

    public void generateInterface(UiAttrExpr attr, Ctx arg) {
        JsonContainerExpr body = (JsonContainerExpr) attr.getExpression();
        final Expression typeParams = body.getNode("typeParams");
        final Expression extend = body.getNode("extend");
    }
}
