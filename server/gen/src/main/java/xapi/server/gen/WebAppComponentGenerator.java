package xapi.server.gen;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope.ScopeType;
import xapi.fu.In2;
import xapi.server.api.WebApp;
import xapi.server.api.XapiServer;
import xapi.server.api.XapiServerPlugin;
import xapi.util.api.RequestLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class WebAppComponentGenerator extends UiComponentGenerator {

    private MethodBuffer installMethod;
    private ClassBuffer cb;

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service, ComponentBuffer source, ContainerMetadata me, UiContainerExpr n,
        UiGenerateMode mode
    ) {
        final UiVisitScope scope = new UiVisitScope(ScopeType.CONTAINER);
        final StringTo<UiFeatureGenerator> overrides = scope.getFeatureOverrides();
        overrides.getOrCreate("classpaths", this::overrides);
        overrides.getOrCreate("gwt", this::overrides);
        overrides.getOrCreate("templates", this::overrides);
        overrides.getOrCreate("routes", this::overrides);
        final GeneratedUiComponent component = source.getGeneratedComponent();

        final GeneratedUiBase base = component.getBase();
        cb = base.getSource().getClassBuffer();
        final String server = cb.addImport(XapiServer.class);
        final String webApp = cb.addImport(WebApp.class);
        cb.addGenericInterface(XapiServerPlugin.class.getCanonicalName(), "Request", "Response");
        installMethod = cb
            .addGenerics("Request extends " + cb.addImport(RequestLike.class), "Response")
            .createMethod("void installToServer(" + server + "<Request, Response> server){");
        installMethod.println(webApp + " app = server.getWebApp();");
        return scope;
    }

    private UiFeatureGenerator overrides(String s) {
        switch (s.toLowerCase()) {
            case "classpaths":
                return new ClasspathFeatureGenerator(this);
            case "gwt":
                return new GwtFeatureGenerator(this);
            case "templates":
                return new TemplateFeatureGenerator(this);
            case "routes":
                return new RouteFeatureGenerator(this);
        }
        throw new IllegalArgumentException("Feature " + s + " not (yet) supported " +
            "in web-app elements");
    }

    public MethodBuffer getInstallMethod() {
        return installMethod;
    }
}
