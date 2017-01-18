package xapi.server.gen;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.ComponentBuffer;
import xapi.dev.ui.ContainerMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.dev.ui.UiGeneratorTools;
import xapi.dev.ui.UiVisitScope;
import xapi.dev.ui.UiVisitScope.ScopeType;
import xapi.fu.In2;
import xapi.server.api.XapiServer;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class WebAppComponentGenerator extends UiComponentGenerator {

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service, ComponentBuffer source, ContainerMetadata me, UiContainerExpr n
    ) {
        final UiVisitScope scope = new UiVisitScope(ScopeType.CONTAINER);
        final StringTo<UiFeatureGenerator> overrides = scope.getFeatureOverrides();
        overrides.getOrCreate("classpaths", this::overrides);
        overrides.getOrCreate("gwt", this::overrides);
        overrides.getOrCreate("templates", this::overrides);
        overrides.getOrCreate("routes", this::overrides);
        final ClassBuffer cb = me.getSourceBuilder().getClassBuffer();
        final MethodBuffer main = cb
            .addInterface(XapiServer.class.getCanonicalName() + "<Request, Response>")
            .addGenerics("Request", "Response")
            .makeAbstract()
            .addImports(In2.class)
            .createMethod("void serviceRequest(Request request, Response response, In2<Request, Response> callback){");
        main.println("String path = getPath(request);");

        return scope;
    }

    private UiFeatureGenerator overrides(String s) {
        switch (s.toLowerCase()) {
            case "classpaths":
                return new ClasspathFeatureGenerator();
            case "gwt":
                return new GwtFeatureGenerator();
            case "templates":
                return new TemplateFeatureGenerator();
            case "routes":
                return new RouteFeatureGenerator();
        }
        throw new IllegalArgumentException("Feature " + s + " not (yet) supported " +
            "in web-app elements");
    }
}
