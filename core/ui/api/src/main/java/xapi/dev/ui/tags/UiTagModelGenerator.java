package xapi.dev.ui.tags;

import com.github.javaparser.ast.expr.DynamicDeclarationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.type.Type;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.In1Out1;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class UiTagModelGenerator extends UiFeatureGenerator {

    private final String pkg;
    private final String name;
    private final UiTagGenerator owner;

    public UiTagModelGenerator(String pkg, String name, UiTagGenerator owner) {
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
        final GeneratedUiApi api = component.getApi();
        final GeneratedUiModel model = api.getModel();
        final ApiGeneratorContext ctx = me.getContext();

        owner.maybeAddImports(tools, ctx, model, attr);
        final String modelName = api.getModelName();
        final In1Out1<Type, String> apiFactory = Type::toSource;
        final In1Out1<Type, String> baseFactory = Type::toSource;
        api.addExtension("pkgName", api.getModelName(),
            component.getBase().getModelName(), apiFactory, baseFactory);
        final ClassBuffer out = api.getSource().getClassBuffer();
        // TODO: check for custom type hierarchy, and see if somebody defines getModel().
        // right now that is implictly done by extending AbstractModelComponent

       out.createMethod("public String getModelType()")
            .returnValue(api.getModel().getTypeName());

        component.getBase().ensureField(model.getWrappedName(), "model");

        boolean immutable = attr.getAnnotation(anno->anno.getNameString().equalsIgnoreCase("immutable")).isPresent();
        final Expression expr = attr.getExpression();
        if (expr instanceof JsonContainerExpr) {
            JsonContainerExpr json = (JsonContainerExpr) expr;
            final GeneratedUiModel apiModel = api.getModel();
            json.getPairs().forEach(pair->{
                String rawFieldName = tools.resolveString(ctx, pair.getKeyExpr());
                if (rawFieldName.matches("[0-9]+")) {
                    // TODO smarter type->name inference for field names...
                    rawFieldName = "val" + rawFieldName;
                }

                final Expression typeExpr = tools.resolveVar(ctx, pair.getValueExpr());
                if (typeExpr instanceof DynamicDeclarationExpr) {
                    // Must be a default method.
                    DynamicDeclarationExpr method = (DynamicDeclarationExpr) typeExpr;
                    owner.printMember(tools, api.getModel(), me, method);
                } else {
                    Type type = tools.methods().$type(tools, ctx, typeExpr).getType();
                    // TODO smart import lookups...
                    boolean isImmutable = immutable;
                    if (!isImmutable) {
                        isImmutable = pair.getAnnotation(anno -> anno.getNameString().equalsIgnoreCase(
                            "immutable")).isPresent();
                    }
                    apiModel.addField(tools, type, rawFieldName, isImmutable);
                }
            });
        } else {
            throw new IllegalArgumentException("<define-tag model={mustBe: Jso}  />; you sent " + tools.debugNode(attr));
        }

        return UiVisitScope.FEATURE_NO_CHILDREN;
    }
}
