package xapi.server.gen;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.ComponentBuffer;
import xapi.dev.ui.ContainerMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.dev.ui.UiGeneratorTools;
import xapi.dev.ui.UiVisitScope;
import xapi.fu.Do;
import xapi.fu.iterate.SingletonIterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class RouteFeatureGenerator extends UiFeatureGenerator {

    private class RouteMethodFactory {

        private final MethodBuffer mb;
        private final UiGeneratorTools tools;
        private final ApiGeneratorContext<?> ctx;
        private final ClassBuffer cb;

        public RouteMethodFactory(UiGeneratorTools service, ClassBuffer out, ContainerMetadata container) {
            this.tools = service;
            this.cb = out;
            this.mb = out.createMethod("public boolean handleRequest()")
                .addParameter("Request", "request")
                .addParameter("In2<Request, Response>", "callback");
            this.ctx = container.getContext();
            service.getGenerator().onFinish(Do.of(()->
                mb.returnValue("false")
            ));
        }

        public void ensureRouteExists(UiContainerExpr route) {
            final Expression pathExpr = route.getAttributeNotNull("path").getExpression();
            final Expression methodExpr = route.getAttribute("method")
                .ifAbsentSupply(()-> new UiAttrExpr("", StringLiteralExpr.stringLiteral("GET"))).getExpression();
            final Expression responseExpr = route.getAttributeNotNull("response").getExpression();

            String path = tools.resolveString(ctx, pathExpr);
            boolean hasWildcards = path.indexOf('*') != -1;
            String method = tools.resolveString(ctx, methodExpr);
            if (responseExpr instanceof UiContainerExpr) {
                UiContainerExpr container = (UiContainerExpr) responseExpr;
                if (container.getName().equals("html")) {
                    // An html container... Check if its static or not.
                    String rawSource = tools.resolveString(ctx, container);
                    final Expression resolved = tools.resolveVar(ctx, container);
                    String resolvedSource = tools.resolveString(ctx, resolved);
                    boolean isStatic = rawSource.equals(resolvedSource);
                    tools.getGenerator().overwriteResource(path, method, resolvedSource, null);
                    if (!isStatic) {
                        // Add code to handle this dynamically.

                    }
                } else {
                    String response = tools.resolveString(ctx, responseExpr);
                    // Serve this text to the given path.
                    tools.getGenerator().overwriteResource(path, method, response, null);
                }
            } else {
                String response = tools.resolveString(ctx, responseExpr);
                // Serve this text to the given path.
                tools.getGenerator().overwriteResource(path, method, response, null);
            }
        }
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata container,
        UiAttrExpr attr
    ) {
        final ClassBuffer out = container.getSourceBuilder().getClassBuffer();

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
            c -> new RouteMethodFactory(service, out, container)
        );
        routes.forEach(route->{
            if (!(route instanceof UiContainerExpr)) {
                throw new IllegalArgumentException("route features only handle <route /> expression values; you sent " + route);
            }
            factory.ensureRouteExists((UiContainerExpr) route);
        });

        return super.startVisit(service, generator, source, container, attr);
    }
}
