package xapi.server.gen;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.GeneratedUiBase;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.api.UiVisitScope;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.iterate.SingletonIterator;
import xapi.util.X_String;

import static xapi.source.X_Source.javaSafeName;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class RouteFeatureGenerator extends UiFeatureGenerator {

    private final WebAppComponentGenerator owner;

    public RouteFeatureGenerator(WebAppComponentGenerator owner) {
        this.owner = owner;
    }

    protected boolean looksLikeUrl(String scriptText) {
        if (scriptText.startsWith("http://") || scriptText.startsWith("https://")) {
            return true;
        }
        if (!scriptText.contains(" ") && !scriptText.contains("\n") && !scriptText.contains("{")) {
            return true;
        }
        return false;
    }

    protected String toPageFactoryName(String path, String method) {
        return X_String.toTitleCase(method.toLowerCase()) + "Page" + javaSafeName(path);
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata container,
        UiAttrExpr attr
    ) {
        final GeneratedUiBase layer = source.getGeneratedComponent().getBase();

        Iterable<Expression> routes;
        if (attr.getExpression() instanceof JsonContainerExpr) {
            JsonContainerExpr json = (JsonContainerExpr) attr.getExpression();
            routes = json.getValues();
        } else if (attr.getExpression() instanceof UiContainerExpr) {
            routes = SingletonIterator.singleItem(attr.getExpression());
        } else {
            throw new IllegalArgumentException("route features only handle <route /> expression values; you sent " + attr);
        }

        final RouteMethodFactory factory = container.getOrCreateFactory(
            RouteMethodFactory.class,
            c -> new RouteMethodFactory(this, service, layer, source, container)
        );
        routes.forEach(route->{
            if (!(route instanceof UiContainerExpr)) {
                throw new IllegalArgumentException("route features only handle <route /> expression values; you sent " + route);
            }
            owner.onFinished(s->{
                final GeneratedRouteInfo result = factory.ensureRouteExists((UiContainerExpr) route);
                owner.getScope().addRoute(result);
            });
        });

        return UiVisitScope.FEATURE_NO_CHILDREN; // handled already
    }

    public WebAppComponentGenerator getOwner() {
        return owner;
    }
}
