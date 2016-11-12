package xapi.server.gen;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.JsonPairExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.ui.ContainerMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.dev.ui.UiGeneratorTools;
import xapi.dev.ui.UiVisitScope;
import xapi.dev.ui.UiVisitScope.ScopeType;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class ClasspathFeatureGenerator extends UiFeatureGenerator {

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service, UiComponentGenerator generator, ContainerMetadata container, UiAttrExpr attr
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
