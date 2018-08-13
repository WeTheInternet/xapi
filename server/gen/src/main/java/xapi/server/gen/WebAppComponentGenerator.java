package xapi.server.gen;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.Do;
import xapi.fu.Do.DoUnsafe;
import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.log.X_Log;
import xapi.scope.request.RequestScope;
import xapi.server.api.WebApp;
import xapi.server.api.XapiServer;
import xapi.server.api.XapiServerPlugin;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class WebAppComponentGenerator extends UiComponentGenerator {

    public static class WebAppGeneratorScope extends UiVisitScope {
        private MethodBuffer installMethod;
        private ClassBuffer cb;
        private final ChainBuilder<Do> deferred;
        public WebAppGeneratorScope previousScope;
        private final ChainBuilder<GeneratedRouteInfo> routes;
        private boolean finished;
        public GeneratedUiLayer installLayer;

        public WebAppGeneratorScope() {
            super(ScopeType.CONTAINER);
            deferred = Chain.startChain();
            routes = Chain.startChain();
        }

        public MethodBuffer getInstallMethod() {
            return installMethod;
        }

        public GeneratedUiLayer getInstallLayer() {
            return installLayer;
        }

        public void addRoute(GeneratedRouteInfo result) {
            assert !finished : "You should not be adding routes after the scope has already been finished! (routes are printed last)";
            routes.add(result);
        }

        public boolean needsCallback() {
            for (GeneratedRouteInfo route : routes) {
                if (route.needsCallback()) {
                    return true;
                }
            }
            return false;
        }

        public void finish() {
            deferred.removeAll(Do::done);
            finished = true;
        }
    }

    private final WebAppGenerator generator;

    private WebAppGeneratorScope scope;

    public WebAppComponentGenerator(WebAppGenerator generator) {
        this.generator = generator;
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service, ComponentBuffer source, ContainerMetadata me, UiContainerExpr n,
        UiGenerateMode mode
    ) {
        final WebAppGeneratorScope previousScope = scope;
        scope = new WebAppGeneratorScope();
        scope.previousScope = previousScope;
        final StringTo<UiFeatureGenerator> overrides = scope.getFeatureOverrides();
        overrides.getOrCreate("classpaths", this::overrides);
        overrides.getOrCreate("gwt", this::overrides);
        overrides.getOrCreate("templates", this::overrides);
        overrides.getOrCreate("routes", this::overrides);
        final GeneratedUiComponent component = source.getGeneratedComponent();

        final GeneratedUiBase base = component.getBase();
        final ClassBuffer cb = scope.cb = base.getSource().getClassBuffer();
        final String server = cb.addImport(XapiServer.class);
        final String webApp = cb.addImport(WebApp.class);
        cb.addGenericInterface(XapiServerPlugin.class.getCanonicalName(), "Request");
        String in1 = cb.addImport(In1.class);
        String requestScope = cb.addImport(RequestScope.class);
        scope.installMethod = cb
            .addGenerics("Request extends " + requestScope)
            .createMethod(
                in1 + "<" + server + "<Request>>" +
                " "+
                "installToServer(" + webApp + " app){");
        scope.installLayer = base;
        return scope;
    }

    @Override
    public void endVisit(
        UiGeneratorTools tools, ContainerMetadata me, UiContainerExpr n, UiVisitScope visitScope
    ) {
        super.endVisit(tools, me, n, visitScope);
        WebAppGeneratorScope scope = (WebAppGeneratorScope) visitScope;
        scope.finish();
        // We'll want to defer printing our return statement, as there are likely other components
        // which are waiting on things like classpaths to be fully resolved / generated
        tools.getGenerator().onFinish(WebAppGenerator.PRIORITY_RESOLVE_GWT+1, ()->{
            if (scope.needsCallback()) {
                scope.installMethod.println("return server->{");

                scope.installMethod.indent();
                scope.routes.forAll(info->{
                    for (In1<WebAppGeneratorScope> callback : info.callbackGenerators()) {
                        callback.in(scope);
                    }
                });
                scope.installMethod.outdent();

                // finish again, for good measure (allowing route generators to use onFinish semantics as well).
                scope.finish();

                scope.installMethod.println("};");
            } else {

                final String in1 = scope.cb.addImport(In1.class);
                scope.installMethod.returnValue(in1 + ".ignored()");
            }
        });
        if (this.scope == visitScope) {
            this.scope = ((WebAppGeneratorScope) visitScope).previousScope;
            if (this.scope != null) {
                this.scope.previousScope = null;
            }
        } else {
            X_Log.warn(WebAppComponentGenerator.class, "Mismatched scope visit pattern...");
        }
    }

    private UiFeatureGenerator overrides(String s) {
        switch (s.toLowerCase()) {
            case "classpaths":
                return new ClasspathFeatureGenerator(this);
            case "gwt":
                return new GwtFeatureGenerator(this, scope);
            case "templates":
                return new TemplateFeatureGenerator(this);
            case "routes":
                return new RouteFeatureGenerator(this);
        }
        throw new IllegalArgumentException("Feature " + s + " not (yet) supported " +
            "in web-app elements");
    }

    public MethodBuffer getInstallMethod() {
        return scope.installMethod;
    }

    public WebAppGeneratorScope getScope() {
        return scope;
    }
    public void onFinished(DoUnsafe callback) {
        scope.deferred.add(callback);
    }
    public void onFinished(In1Unsafe<WebAppGeneratorScope> callback) {
        scope.deferred.add(callback.provide(scope));
    }

    public WebAppGenerator getGenerator() {
        return generator;
    }
}
