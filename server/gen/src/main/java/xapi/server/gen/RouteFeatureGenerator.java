package xapi.server.gen;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.api.UiVisitScope;
import xapi.fu.Do;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.iterate.SingletonIterator;
import xapi.server.api.WebApp;
import xapi.source.read.JavaModel.IsQualified;
import xapi.util.X_String;

import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class RouteFeatureGenerator extends UiFeatureGenerator {

    private class RouteMethodFactory {

        private final MethodBuffer mb;
        private final UiGeneratorTools tools;
        private final ApiGeneratorContext<?> ctx;
        private final ClassBuffer cb;
        private final ComponentBuffer source;
        private final GeneratedUiComponent component;

        public RouteMethodFactory(
            UiGeneratorTools service,
            ClassBuffer out,
            ComponentBuffer source,
            ContainerMetadata container
        ) {
            this.tools = service;
            this.cb = out;
            this.source = source;
            this.component = source.getGeneratedComponent();
            this.ctx = container.getContext();

            String installRouteName = routeName(container);
            this.mb = out.createMethod("public void " + installRouteName)
                .addParameter(WebApp.class, "app");

            final MethodBuffer install = owner.getInstallMethod();
            install.println(installRouteName + "(app);");
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
                switch (container.getName()) {
                    case "html":
                        // An html container... Check if its static or not.
                        String rawSource = tools.resolveString(ctx, container);
                        final Expression resolved = tools.resolveVar(ctx, container);
                        String resolvedSource = tools.resolveString(ctx, resolved);
                        boolean isStatic = rawSource.equals(resolvedSource);
                        tools.getGenerator().overwriteResource(path, method, resolvedSource, null);
                        if (!isStatic) {
                            // Add code to handle dynamism in resolved output
                        }
                        break;
                    case "page":
                        handlePage(path, method, container, responseExpr);
                        break;
                    default:
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

        /**

        Generate a page handler which can pull in templates, define scripts, styles and dom.

        Example page:
            <page
              template = "session"
              script = $gwt("Collide")
              script = {
                collide : {
                  name : $user.name,
                }
              }
              style = .{
                #gwt_root {
                  position: absolute;
                }
              }
              dom = <div id="gwt_root" />
            /page>
        */
        protected void handlePage(String path, String method, UiContainerExpr page, Expression responseExpr) {
            // A generated page handler will need to know about all defined templates,
            // so we should be doing a pre-process phase where we load up all templates before
            // even looking at routes.
            DomBuffer output = new DomBuffer("");
            output.println("<!DOCTYPE html>");
            final DomBuffer doc = output.makeTag("html");
            final DomBuffer head = doc.makeHead();
            Lazy<DomBuffer> body = Lazy.deferred1(doc::makeBody);
            // Lets generate a class which generates this page at the appropriate path.

            String javaName = toPageFactoryName(path, method);
            final IsQualified name = source.getElement();

            for (UiAttrExpr attr : page.getAttributes()) {
                handlePagePart(head, body, attr);
            }

        }

        private void handlePagePart(DomBuffer head, Lazy<DomBuffer> body, UiAttrExpr attr) {
            switch (attr.getNameString()) {
                case "template":
                    // load up a template, and "visit" its contents now.
                    final Expression templateExpr = tools.resolveVar(ctx, attr.getExpression());
                    String templateName = tools.resolveString(ctx, templateExpr);
                    UiContainerExpr template = null;
                    for (UiContainerExpr candidate : source.getInterestingNodes().getTemplateElements().values()) {
                        if (candidate.getName().equals(templateName)) {
                            // found our template...
                            template = candidate;
                            break;
                        } else {
                            final Maybe<UiAttrExpr> name = candidate.getAttribute("name");
                            if (name.isPresentAndMatches(n->templateName.equals(tools.resolveString(ctx, n.getExpression())))) {
                                template = candidate;
                                break;
                            }
                        }
                    }
                    if (template == null) {
                        // TODO: consider imports of templates from other files...
                        // need to have imported those into our interesting nodes...
                        // also consider reusing templates?  probably not, since we'll
                        // want to be able to resolve compile-time vars based on usage location
                        // (so we would want raw source, not half-resolved values that we may have updated earlier)
                        throw new IllegalArgumentException("Unable to load template " + templateName +" from " + tools.debugNode(attr.getParentNode()));
                    }
                    // Now, visit this template.
                    final Expression resolved = tools.resolveVar(ctx, template);
                    if (!(resolved instanceof UiContainerExpr)) {
                        throw new IllegalArgumentException("templates in a <page /> must point to a <page /> element; you sent " + tools.debugNode(resolved));
                    }
                    UiContainerExpr t = (UiContainerExpr) resolved;
                    if (!t.getName().equals("page")) {
                        if (t.getName().equals("template")) {
                            for (Expression child : t.getBody().getChildren()) {
                                final Expression resolvedChild = tools.resolveVar(ctx, child);
                                if (!(resolvedChild instanceof UiContainerExpr)) {
                                    throw new IllegalArgumentException("templates in a <page /> must point to a <page /> element; you sent " + tools.debugNode(resolved));
                                }
                                UiContainerExpr childUi = (UiContainerExpr) resolvedChild;
                                if (!childUi.getName().endsWith("page")) {
                                    throw new IllegalArgumentException("templates in a <page /> must point to a <page /> element; you sent " + tools.debugNode(resolved));
                                }
                                for (UiAttrExpr importMe : childUi.getAttributes()) {
                                    handlePagePart(head, body, importMe);
                                }

                            }
                            break;

                        }
                        throw new IllegalArgumentException("templates in a <page /> must point to a <page /> element; you sent " + tools.debugNode(resolved));
                    }
                    for (UiAttrExpr importMe : t.getAttributes()) {
                        handlePagePart(head, body, importMe);
                    }
                    break;
                case "script":
                    // Either a url, a meta-function (like $gwt()), or a json literal...
                    // Once the body is resolved, scripts go in the body
                    final DomBuffer target = body.isResolved() ? body.out1() : head;
                    final DomBuffer script = target.makeScript();
                    final GeneratedUiComponent plugin = source.getGeneratedComponent();

                    if (attr.getExpression() instanceof MethodCallExpr) {
                        // a meta-template... right now, only $gwt is supported
                    } else if (attr.getExpression() instanceof JsonContainerExpr) {
                        // a json container.  We want to generate a generator that emits <script>name = { some: "thing" };</script>
                    } else {
                        // assume text, create code to resolve template values, if present, at runtime.
                        final Expression scriptBody = tools.resolveVar(ctx, attr.getExpression());
                        String scriptText = tools.resolveString(ctx, scriptBody);
                        if (scriptText.indexOf('$') != -1) {
                            // When it looks like there are unresolved templates, we'll want to emit
                            // a Route that has an embedded Template to resolve w/ runtime values

                        }
                        if (looksLikeUrl(scriptText)) {
                            // If it's a url, we want to emit <script src="value" />
                            script.setSrc(scriptText);
                        } else {
                            // If it's not a url, we want to emit a <script> with.codeInside() </script>
                            script.append(scriptText);
                        }
                    }
                    break;
                case "style":
                    // Emit some css.  TODO: interface w/ gss compiler?
                    // ...probably not, as we want to make a simple runtime generator which can emit a <style /> tag
                    break;
                case "dom":
                    // Add some html to the page body.
                    break;
                default:
                    assert false : "Unhandled page part: " + tools.debugNode(attr);
            }
        }
        protected String routeName(ContainerMetadata ui) {
            // TODO: use context hints for a better name;
            return component.getBase().newMethodName("installRoute");
        }
    }


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
        return X_String.toTitleCase(method.toLowerCase()) + "Page" + path.replace('/', '_');
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata container,
        UiAttrExpr attr
    ) {
        final ClassBuffer out = source.getGeneratedComponent().getBase().getSource().getClassBuffer();

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
            c -> new RouteMethodFactory(service, out, source, container)
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
