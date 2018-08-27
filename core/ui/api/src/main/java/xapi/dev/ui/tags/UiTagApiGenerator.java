package xapi.dev.ui.tags;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.Maybe;
import xapi.util.X_Properties;

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
        final Maybe<String> maybeAttr = isPlatformDisabled(attr.getNameString());
        if (maybeAttr.isAbsent()) {
            return UiVisitScope.FEATURE_NO_CHILDREN;
        }
        String attrName = maybeAttr.get();
        if (attrName.equals("api")) {
            layer = component.getApi();
        } else if (attrName.equals("impl")) {
            layer = component.getBase();
        } else if (attrName.startsWith("impl")) {
            out : {
                for (GeneratedUiImplementation impl : component.getImpls()) {
                    final String prefix = impl.getPrefix().toLowerCase();
                    if (attrName.equals("impl-" + prefix)) {
                        layer = impl;
                        break out;
                    }
                }
                throw new UnsupportedOperationException("Unknown impl type " + attrName
                +"\nin " + tools.debugNode(attr) + "\nof " + tools.debugNode(me.getUi()));
            }
        } else {
            throw new UnsupportedOperationException("Unknown attribute name " + attrName
            +"\nin " + tools.debugNode(attr) + "\nof " + tools.debugNode(me.getUi()));
        }
        final ApiGeneratorContext<?> ctx = me.getContext();
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
        final SourceBuilder<GeneratedJavaFile> src = layer.getSource();
        for (AnnotationExpr expr : attr.findAnnotations("extend", "extends")) {
            for (String member : tools.getStringMembers(ctx, expr)) {
                if (layer.isInterface()) {
                    src.addInterfaces(member);
                } else {
                    // layer is a class...  we can only extend once...
                    final String was = src.getSuperClass();
                    src.setSuperClass(member);
                    if (was != null && !was.equalsIgnoreCase(member)) {
                        throw new IllegalStateException("Cannot change " + was + " to " + member +" (for " + src.getQualifiedName() + ")");
                    }
                }
            }

        }
        for (AnnotationExpr expr : attr.findAnnotations("implement", "implements")) {
            for (String member : tools.getStringMembers(ctx, expr)) {
                src.addInterfaces(member);
            }
        }

        return UiVisitScope.FEATURE_NO_CHILDREN;
    }

    protected Maybe<String> isPlatformDisabled(String implType) {
        final String normalized = normalizeName(implType);
        if (X_Properties.getProperty(UiGeneratorPlatform.SYSTEM_PROP_IGNORE_PLATFORM, "")
            .contains(normalized.replace("impl-", ""))) {
            return Maybe.not();
        }
        return Maybe.immutable(normalized);
    }

    protected String normalizeName(String attr) {
        if (attr.endsWith("-impl")) {
            return "impl-" + attr.replace("-impl", "").toLowerCase();
        }
        return attr.toLowerCase();
    }
}
