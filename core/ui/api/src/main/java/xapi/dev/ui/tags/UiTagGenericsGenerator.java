package xapi.dev.ui.tags;

import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.api.UiVisitScope;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.Do;
import xapi.fu.Mutable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class UiTagGenericsGenerator extends UiFeatureGenerator {

    private final String pkg;
    private final String name;
    private final UiTagGenerator owner;

    public UiTagGenericsGenerator(String pkg, String name, UiTagGenerator owner) {
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
            final TypeParameter type = tools.methods().$typeParam(tools, ctx, resolved);

            if (genericName.contains("$")) {
                final Do was = undos.out1();
                final Do use = was.doAfter(ctx.addToContext(genericName, type));
                undos.in(use);
            }
            component.addGeneric(genericName, type);
        });
        owner.onComponentComplete(undos.out1());
        return UiVisitScope.FEATURE_NO_CHILDREN;
    }
}
