package xapi.server.gen;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.JsonPairExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;
import xapi.dev.ui.api.UiVisitScope.ScopeType;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class ClasspathFeatureGenerator extends UiFeatureGenerator {

    private final WebAppComponentGenerator owner;

    public ClasspathFeatureGenerator(WebAppComponentGenerator owner) {
        this.owner = owner;
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata container,
        UiAttrExpr attr
    ) {
        final UiVisitScope scope = new UiVisitScope(ScopeType.FEATURE);

        Expression expr = attr.getExpression();
        if (!(expr instanceof JsonContainerExpr)) {
            throw new IllegalArgumentException("classpaths feature only accepts json nodes as children; you sent " + attr);
        }

        JsonContainerExpr elements = (JsonContainerExpr) expr;
        if (!elements.isArray()) {
            elements = JsonContainerExpr.jsonArray(elements);
        }
        for (JsonPairExpr classpath : elements.getPairs()) {
            String name = ASTHelper.extractStringValue(classpath.getKeyExpr());
            JsonContainerExpr entries = (JsonContainerExpr) classpath.getValueExpr();

        }

        scope.setVisitChildren(false);
        return scope;
    }
}
