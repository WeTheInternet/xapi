package xapi.dev.ui.tags;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.DynamicDeclarationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class UiTagApiGenerator extends UiFeatureGenerator {

    private final String pkg;
    private final String name;
    private final UiTagGenerator owner;

    public UiTagApiGenerator(String pkg, String name, UiTagGenerator owner) {
        this.pkg = pkg;
        this.name = name;
        this.owner = owner;
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools tools,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata me,
        UiAttrExpr attr
    ) {
        final GeneratedUiComponent component = me.getGeneratedComponent();
        GeneratedUiLayer layer;
        if (attr.getNameString().equals("api")) {
            layer = component.getApi();
        } else if (attr.getNameString().equals("impl")){
            layer = component.getBase();
        } else {
            throw new UnsupportedOperationException("Unknown attribute name " + attr.getNameString()
            +" in " + tools.debugNode(attr) + " of " + tools.debugNode(me.getUi()));
        }
        final ApiGeneratorContext ctx = me.getContext();
        owner.maybeAddImports(tools, ctx, layer, attr);
        final Expression resolved = tools.resolveVar(ctx, attr.getExpression());
        if (resolved instanceof JsonContainerExpr) {
            JsonContainerExpr asJson = (JsonContainerExpr) resolved;
            asJson.getValues().forAll(expr->{
                if (expr instanceof DynamicDeclarationExpr) {
                    DynamicDeclarationExpr member = (DynamicDeclarationExpr) expr;
                    owner.printMember(tools, layer, me, member);
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
                        // TODO add a @Nested annotation to prevent this behavior

                    }
                } else if (!type.isInterface()) {
                    // base is a class, lets use it!
                    // TODO add a @Nested annotation to prevent this behavior
                }
            }
            owner.printMember(tools, layer, me, member);
        } else {
            throw new IllegalArgumentException("Unhandled api= feature value " + tools.debugNode(resolved) + " from " + tools.debugNode(attr));
        }

        return UiVisitScope.FEATURE_NO_CHILDREN;
    }
}
