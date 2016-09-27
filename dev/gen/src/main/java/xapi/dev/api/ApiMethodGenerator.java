package xapi.dev.api;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.collect.api.IntTo;
import xapi.collect.impl.SimpleStack;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Do;
import xapi.source.read.JavaVisitor;

import java.lang.reflect.Modifier;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/21/16.
 */
public class ApiMethodGenerator <Ctx extends ApiGeneratorContext<Ctx>> extends VoidVisitorAdapter<Ctx> implements ApiGeneratorTools<Ctx> {

    private final SourceBuilder<Ctx> builder;
    private final JsonContainerExpr json;
    private final SimpleStack<Do> undos;
    private final int modifiers;
    private IntTo<String> typeParams;
    private String returnType;
    private String name;
    private IntTo<String> params;
    private String body;

    public ApiMethodGenerator(
        SourceBuilder<Ctx> builder,
        JsonContainerExpr json,
        int modifiers
    ) {
        this.builder = builder;
        this.json = json;
        this.modifiers = modifiers;
        undos = new SimpleStack<>();
    }

    @Override
    public void visit(JsonContainerExpr n, Ctx arg) {
        super.visit(n, arg);
        // Once we've visited all members, we will construct the actual method
        boolean isInterface = builder.getClassBuffer().isInterface();
        StringBuilder methodDef = new StringBuilder("public ");
        if (body == null) {
            // we are going to generate an abstract method
            assert Modifier.isAbstract(modifiers);
        } else {
            // we are going to generate a non-abstract method
            assert !Modifier.isAbstract(modifiers);
            if (Modifier.isStatic(modifiers)) {
                assert (modifiers & JavaVisitor.MODIFIER_DEFAULT) == JavaVisitor.MODIFIER_DEFAULT;
            } else if (isInterface) {
                assert !Modifier.isStatic(modifiers);
            }
        }
        methodDef
            .append(returnType)
            .append(" ")
            .append(name)
            .append("(");
        String prefix = "";
        if (params != null) {
            for (String param : params.forEach()) {
                methodDef.append(prefix).append(param);
                prefix = ", ";
            }
        }
        methodDef.append(")");
        final MethodBuffer buffer = builder.getClassBuffer().createMethod(methodDef.toString());
        if (body != null) {
            int trim = Integer.MAX_VALUE;
            for (String line : body.split("\\n|(?:\\r|\\n)")) {
                int whitespace = 0;
                while (line.length() > 0 && Character.isWhitespace(line.charAt(0))) {
                    whitespace++;
                    line = line.substring(1);
                }
                if (whitespace > 0) {
                    trim = Math.min(trim, whitespace);
                }
            };
            buffer.printlns(body.replaceAll("\n\\s{"+ trim + "}", "\n"));
        }
        buffer.setModifier(modifiers);
        undos.forEach(Do::done);
    }

    @Override
    public void visit(JsonPairExpr n, Ctx ctx) {
        String key = n.getKeyString();
        switch (key) {
            case "var":
                final Expression value = n.getValueExpr();
                if (value instanceof JsonContainerExpr) {
                    ((JsonContainerExpr)value).getPairs()
                        .forEach(pair->{
                            String varName = pair.getKeyString();
                            final Expression resolved = resolveVar(ctx, pair.getValueExpr());
                            final Do undo = ctx.addToContext(varName, resolved);
                            undos.add(undo);
                        });
                } else {
                    throw new IllegalArgumentException("Cannot understand a var field in a method " +
                        "definition which does not have a json object as its value.  You sent " + value + " at " + value.getCoordinates());
                }
                break;
            case "typeParams":
                typeParams = resolveToLiterals(ctx, n.getValueExpr());
                break;
            case "returns":
                final IntTo<String> returnTypes = resolveToLiterals(ctx, n.getValueExpr());
                if (returnTypes.size() != 1) {
                    throw new IllegalArgumentException("Bad returns member for method declaration; " +
                        n.getCoordinates() + " must return exactly one type, but instead returned " + returnTypes);
                }
                returnType = returnTypes.at(0);
                break;
            case "name":
                name = resolveString(ctx, n.getValueExpr());
                break;
            case "params":
                final Expression resolved = resolveVar(ctx, n.getValueExpr());
                params = resolveToLiterals(ctx, resolved);
                break;
            case "body":
                body = resolveBody(ctx, n.getValueExpr());
                break;
            case "filter":
                break;
            default:
                throw new IllegalArgumentException("Method generator cannot understand json nodes with " +
                    "key named " + key + " from " + n + " at " + n.getCoordinates());
        }
    }

}
