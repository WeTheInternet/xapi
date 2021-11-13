package xapi.dev.ui.tags;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.DynamicDeclarationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.lang.gen.GeneratedJavaFile;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.source.util.X_Modifier;
import xapi.source.read.SourceUtil;
import xapi.debug.X_Debug;

import java.util.Arrays;
import java.util.List;

import static com.github.javaparser.ast.expr.TemplateLiteralExpr.templateLiteral;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/28/18.
 */
class UiMemberTransformer extends ModifierVisitorAdapter<UiMemberContext> {

    private final GeneratedJavaFile ui;

    public UiMemberTransformer(GeneratedJavaFile ui) {
        this.ui = ui;
    }

    @Override
    public Node visit(
        DynamicDeclarationExpr member, UiMemberContext ctx
    ) {
        // We want to transform this method declaration
        // into something safely toString()able.
        final UiGeneratorTools tools = ctx.getTools();
        final ApiGeneratorContext<?> apiCtx = ctx.getApiContext();

        //            final Do undos = resolveSpecialNames(apiCtx, ctx.getContainer().getGeneratedComponent(), ui, null);

        if (member.getBody() instanceof MethodDeclaration) {
            MethodDeclaration method = (MethodDeclaration) member.getBody();
            if (!method.isStatic() &&
                ui.isInterface() &&
                method.getBody() != null) {
                // Make this method default if it non-static with a body
                method.setDefault(true);
                method.setModifiers(
                    (method.getModifiers() & ModifierSet.VISIBILITY_MASK)
                        | ModifierSet.DEFAULT
                );
            }
        } else if (member.getBody() instanceof FieldDeclaration) {

            final BodyDeclaration decl = member.getBody();
            FieldDeclaration asField = (FieldDeclaration) decl;
            addField(member, asField, ctx);
            return templateLiteral("");
        }
        String src = member.toSource(tools.getTransformer(apiCtx));
        //            undos.done();
        return templateLiteral(src, member);
    }

    private void addField(
        DynamicDeclarationExpr member,
        FieldDeclaration asField,
        UiMemberContext ctx
    ) {
        ChainBuilder<VariableDeclarator> toForward = Chain.startChain();
        final UiGeneratorTools tools = ctx.getTools();

        String typeName = asField.getType().toSource();
        for (VariableDeclarator var : asField.getVariables()) {
            Expression init = var.getInit();
            boolean isConvenienceMethod = init instanceof MethodReferenceExpr &&
                isConvenienceMethod((MethodReferenceExpr) init);

            if (isConvenienceMethod) {
                // when using a method reference, we will bind get/set to whatever is referenced.
                MethodReferenceExpr methodRef = (MethodReferenceExpr) init;

                String scope = tools.resolveString(ctx, methodRef.getScope());
                // Create getter and setter
                String nameToUse = ui.newFieldName(methodRef.getIdentifier());
                String getterName = SourceUtil.toGetterName(typeName, nameToUse);
                String setterName = SourceUtil.toSetterName(nameToUse);
                final ClassBuffer buf = ui.getSource().getClassBuffer();
                boolean addGetter = true, addSetter = true;
                if (nameToUse.startsWith("get")) {
                    // getter only
                    addSetter = false;
                } else if (nameToUse.startsWith("set")) {
                    // setter only
                    addGetter = false;
                }
                String modifierPrefix = ui.isInterface() ? "default " : "public ";
                if (addGetter) {
                    buf.createMethod(modifierPrefix + typeName + " " + getterName + "()")
                        .print("return ")
                        .printlns(scope + "." + getterName + "();");
                }
                if (addSetter) {
                    buf.createMethod(modifierPrefix + "void " + setterName + "()")
                        .addParameter(typeName, nameToUse)
                        .printlns(scope + "." + setterName + "(" + nameToUse + ");");
                }
                // TODO: consider allowing some means to add collection / map helper methods if the field type supports them
                continue; // do not fall through into .ensureField.
            }

            if (ui.isInterface()) {
                if (!X_Modifier.isStatic(asField.getModifiers())) {
                    // instance fields on interfaces must be forwarded to getImplementor() class
                    toForward.add(var);
                    init = null; // we forward the var to implementor type, so to simplify logic here in interface,
                    // we will null out the init variable, and let it be handled normally in the implementor.
                }
            }

            final FieldBuffer field = ui.getOrCreateField(
                asField.getModifiers(),
                typeName,
                var.getId().getName()
            );
            if (init != null) {
                boolean isStatic = X_Modifier.isStatic(asField.getModifiers());
                assert !ui.isInterface() || isStatic :
                    "Interface failed to forward field with initializer and null init variable";

                String initializer = tools.resolveLiteral(ctx, init);
                if (isStatic) {
                    // static fields we'll set in an initializer.
                    // TODO: bother with throws clauses, to know when we need to generate a static intializer block.
                    // for now, anyone who needs to call methods with checked exceptions will need to create their
                    // own helper method to do so, and call that instead.
                    field.setInitializer(initializer);
                } else {
                    // non-static fields we want to initialize in the constructor,
                    // so we can reference things sent to said constructor.
                    ui.getDefaultConstructor() // TODO have .allConstructors(ctor->{}) construct
                        .println(field.getName() + " = " + initializer + ";");
                }
            }

        } // end loop of field vars (FieldType var1, var2, etc;)

        if (!toForward.isEmpty()) {
            // for an interface, we turn a field into a pair of getter / setters (no field!)
            final GeneratedJavaFile implementor = ui.getImplementor();
            if (implementor == null) {
                assert false : "Unsupported interface type " + ui.getClass() + " must override getImplementor() and " +
                    "return a class to install fields into; source dump:\n" + ui.toSource();
                throw X_Debug.recommendAssertions();
            }
            // Whatever instance fields we add to this interface, we also add to the implementor class.
            final List<VariableDeclarator> oldVars = asField.getVariables();
            asField.setVariables(Arrays.asList(toForward.toArray(VariableDeclarator[]::new)));
            // recurse back into our transformer for the base class, because we are forwarding from interface to the class
            final Node result = new UiMemberTransformer(implementor)
                .visit(member, ctx);

            String src = tools.resolveLiteral(ctx, (Expression) result);

            if (!src.trim().isEmpty()) {
                implementor.getSource().getClassBuffer()
                    .println()
                    .printlns(src);
            }

            asField.setVariables(oldVars);
        }
    }

    private boolean isConvenienceMethod(MethodReferenceExpr init) {
        String name = init.getScope().toSource();
        switch (name) {
            case "$model":
            case "$data":
                return true;
        }
        return false;
    }
}
