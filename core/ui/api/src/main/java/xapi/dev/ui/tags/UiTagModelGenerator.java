package xapi.dev.ui.tags;

import com.github.javaparser.ast.expr.DynamicDeclarationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.type.Type;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.GeneratedUiApi;
import xapi.dev.ui.api.GeneratedUiModel;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;

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
        final GeneratedUiApi api = me.getGeneratedComponent().getApi();

        //        UiGeneratorTools tools,
        //        ContainerMetadata me,
        //        UiAttrExpr attr,
        //        GeneratedUiApi api
        final GeneratedUiModel model = api.getModel();
        final ApiGeneratorContext ctx = me.getContext();
        owner.maybeAddImports(tools, ctx, model, attr);
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
                    owner.printMember(tools, api.getModel(), me, method);
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

        return UiVisitScope.FEATURE_NO_CHILDREN;
    }
}
