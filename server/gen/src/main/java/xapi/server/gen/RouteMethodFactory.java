package xapi.server.gen;

import net.wti.lang.parser.ast.visitor.VoidVisitorAdapter;
import net.wti.lang.parser.ast.expr.*;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.HtmlBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.except.NotYetImplemented;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.server.api.Route;
import xapi.server.api.Route.RouteType;
import xapi.server.api.WebApp;
import xapi.source.X_Source;
import xapi.source.read.JavaModel.IsQualified;

import java.util.List;

import static xapi.source.X_Source.javaSafeName;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/3/17.
 */
class RouteMethodFactory {

    private RouteFeatureGenerator generator;
    private final MethodBuffer mb;
    private final UiGeneratorTools tools;
    private final ApiGeneratorContext<?> ctx;
    private final GeneratedUiLayer layer;
    private final ClassBuffer cb;
    private final ComponentBuffer source;
    private final GeneratedUiComponent component;
    private final String ROUTE;
    private final Lazy<String> TYPE_DIRECTORY;
    private final Lazy<String> TYPE_FILE;
    private final Lazy<String> MODEL_CREATE;
    private final Lazy<String> TYPE_TEMPLATE;
    private final Lazy<String> TYPE_CALLBACK;
    private final Lazy<String> TYPE_TEXT;
    private final Lazy<String> TYPE_REROUTE;

    public RouteMethodFactory(
        RouteFeatureGenerator generator,
        UiGeneratorTools service,
        GeneratedUiBase out,
        ComponentBuffer source,
        ContainerMetadata container
    ) {
        this.generator = generator;
        this.tools = service;
        this.layer = out;
        this.cb = out.getSource().getClassBuffer();
        this.source = source;
        this.component = source.getGeneratedComponent();
        this.ctx = container.getContext();

        String installRouteName = routeName(container);
        this.mb = cb.createMethod("public void " + installRouteName)
            .addParameter(WebApp.class, "app");

        final MethodBuffer install = generator.getOwner().getInstallMethod();
        install.println(installRouteName + "(app);");

        this.ROUTE = mb.addImport(Route.class);
        this.MODEL_CREATE = Lazy.deferSupplier(mb::addImportStatic, X_Model.class, "create");
        this.TYPE_FILE = Lazy.deferSupplier(mb::addImportStatic, RouteType.class, RouteType.File.name());
        this.TYPE_DIRECTORY = Lazy.deferSupplier(mb::addImportStatic, RouteType.class, RouteType.Directory.name());
        this.TYPE_TEXT = Lazy.deferSupplier(mb::addImportStatic, RouteType.class, RouteType.Text.name());
        this.TYPE_TEMPLATE = Lazy.deferSupplier(mb::addImportStatic, RouteType.class, RouteType.Template.name());
        this.TYPE_CALLBACK = Lazy.deferSupplier(mb::addImportStatic, RouteType.class, RouteType.Callback.name());
        this.TYPE_REROUTE = Lazy.deferSupplier(mb::addImportStatic, RouteType.class, RouteType.Reroute.name());
    }

    public GeneratedRouteInfo ensureRouteExists(UiContainerExpr route) {
        final Expression pathExpr = route.getAttributeNotNull("path").getExpression();
        final Expression methodExpr = route.getAttribute("method")
            .ifAbsentSupply(() -> new UiAttrExpr("", StringLiteralExpr.stringLiteral("GET"))).getExpression();
        final Expression responseExpr = route.getAttributeNotNull("response").getExpression();

        String path = tools.resolveString(ctx, pathExpr);
        boolean hasWildcards = path.indexOf('*') != -1;
        String method = tools.resolveString(ctx, methodExpr);
        GeneratedRouteInfo info = new GeneratedRouteInfo(path, method);
        info.setSource(route);
        if (responseExpr instanceof UiContainerExpr) {
            UiContainerExpr container = (UiContainerExpr) responseExpr;
            switch (container.getName()) {
                case "html":
                    // An html container... Check if its static or not.
                    String rawSource = tools.resolveString(ctx, container);
                    final Expression resolved = tools.resolveVar(ctx, container);
                    String resolvedSource = tools.resolveString(ctx, resolved);
                    boolean isStatic = rawSource.equals(resolvedSource);
                    String fileLoc = tools.getGenerator().overwriteResource(path, method, resolvedSource, null);
                    if (!isStatic) {
                        // Add code to handle dynamism in resolved output
                        // Do this by creating a Callback type route, and add a callback to the GeneratedRouteInfo,
                        // so we ensure that we also add an install callback to a server, to service requests to the route
                    }

                    printRoute(path, fileLoc.replace(ctx.getOutputDirectory(), "" ), RouteType.File);

                    break;
                case "page":
                    handlePage(info, container);
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
        return info;
    }

    private String tFile() {
        return TYPE_FILE.out1();
    }

    private String tDirectory() {
        return TYPE_DIRECTORY.out1();
    }

    private String tText() {
        return TYPE_TEXT.out1();
    }

    private String tTemplate() {
        return TYPE_TEMPLATE.out1();
    }

    private String tCallback() {
        return TYPE_CALLBACK.out1();
    }

    private String tReroute() {
        return TYPE_REROUTE.out1();
    }

    private String mModelCreate() {
        return MODEL_CREATE.out1();
    }

    private String tRoute() {
        return ROUTE;
    }

    /**
     * Generate a page handler which can pull in templates, define scripts, styles and dom.
     * <p>
     * Example page:
     * <pre>
     * <page
     * template = "session"
     * script = $gwt("Collide")
     * script = {
     * collide : {
     * name : $user.name,
     * }
     * }
     * style = .{
     * #gwt_root {
     * position: absolute;
     * }
     * }
     * dom = <div id="gwt_root" />
     * /page>
     * </pre>
     */
    protected void handlePage(
        GeneratedRouteInfo info,
        UiContainerExpr page
    ) {
        // A generated page handler will need to know about all defined templates,
        // so we should be doing a pre-process phase where we load up all templates before
        // even looking at routes.
        HtmlBuffer output = info.getHtmlBuffer();
        // Lets generate a class which generates this page at the appropriate path.

        String path = info.getPath();
        String javaName = generator.toPageFactoryName(path, info.getMethod());
        final IsQualified name = source.getElement();

        for (UiAttrExpr attr : page.getAttributes()) {
            handlePagePart(info, attr);
        }

        // Now that we've generated our dom templates, lets add a route (bonus points for identifying static responses)
        // An html container... Check if its static or not.
        String rawSource = output.toString();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        String fileLoc = tools.getGenerator().overwriteResource(path, javaName, rawSource, null);
        final String routeName = printRoute(path, fileLoc.replace(ctx.getOutputDirectory(), ""), RouteType.File);

    }

    private String printRoute(String path, String payload, RouteType type) {
        final String routeName = layer.newFieldName("route" + javaSafeName(path));

        mb.println(tRoute() + " " + routeName + " = " + mModelCreate() + "(" + tRoute() + ".class);");
        mb.println(routeName + ".setPayload(" + "\"" + X_Source.escape(payload) + "\");");
        mb.println(routeName + ".setRouteType(" + type(type) + ");");
        mb.println(routeName + ".setPath(\"" + (path.startsWith("/") ? path : "/" + path) + "\");");
        mb.println("app.getRoute().add(" + routeName + ");");

        return routeName;
    }

    private String type(RouteType type) {
        switch (type) {
            case Callback:
                return tCallback();
            case Template:
                return tTemplate();
            case Text:
                return tText();
            case File:
                return tFile();
            case Directory:
                return tDirectory();
            case Reroute:
                return tReroute();
            case Gwt:
            case Service:
            default:
                throw new NotYetImplemented("Type " + type + " must be implemented in " + getClass());
        }
    }

    private void handlePagePart(GeneratedRouteInfo info, UiAttrExpr attr) {
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
                        if (name.isPresentAndMatches(n -> templateName.equals(tools.resolveString(
                            ctx,
                            n.getExpression()
                        )))) {
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
                    throw new IllegalArgumentException("Unable to load template " + templateName + " from " + tools.debugNode(
                        attr.getParentNode()));
                }
                // Now, visit this template.
                final Expression resolved = tools.resolveVar(ctx, template);
                if (!(resolved instanceof UiContainerExpr)) {
                    throw new IllegalArgumentException(
                        "templates in a <page /> must point to a <page /> element; you sent " + tools.debugNode(
                            resolved));
                }
                UiContainerExpr t = (UiContainerExpr) resolved;
                if (!t.getName().equals("page")) {
                    if (t.getName().equals("template")) {
                        for (Expression child : t.getBody().getChildren()) {
                            if (TemplateLiteralExpr.isWhitespaceLiteral(child)) {
                                continue;
                            }
                            final Expression resolvedChild = tools.resolveVar(ctx, child);
                            if (!(resolvedChild instanceof UiContainerExpr)) {
                                throw new IllegalArgumentException(
                                    "templates in a <page /> must point to a UiContainerExpr; you sent " + tools.debugNode(
                                        resolved));
                            }
                            UiContainerExpr childUi = (UiContainerExpr) resolvedChild;
                            if (!childUi.getName().endsWith("page")) {
                                throw new IllegalArgumentException(
                                    "templates in a <page /> must point to a <page /> element; you sent " + tools.debugNode(
                                        resolved));
                            }
                            for (UiAttrExpr importMe : childUi.getAttributes()) {
                                handlePagePart(info, importMe);
                            }

                        }
                        break;

                    }
                    throw new IllegalArgumentException(
                        "templates in a <page /> must point to a <page /> element; you sent " + tools.debugNode(
                            resolved));
                }
                for (UiAttrExpr importMe : t.getAttributes()) {
                    handlePagePart(info, importMe);
                }
                break;
            case "script":
                // Either a url, a meta-function (like $gwt()), or a json literal...
                // Once the body is resolved, scripts go in the body
                final DomBuffer target = info.getScriptTarget();
                final DomBuffer script = target.makeScript();

                if (attr.getExpression() instanceof MethodCallExpr) {
                    // a meta-template... right now, only $gwt is supported
                    MethodCallExpr method = (MethodCallExpr) attr.getExpression();
                    switch (method.getName()) {
                        case "$gwt":
                            // hokay!  we're going to need to ensure the given gwt module is added to the application,
                            // plus add a source tag pointing to said module.
                            final List<Expression> args = method.getArgs();
                            if (args.size() != 1) {
                                final String msg = "$gwt() accepts only one argument; either a module name, or a json {} compile descriptor";
                                X_Log.error(RouteMethodFactory.class, msg, "you sent", args);
                                throw new IllegalArgumentException(msg);
                            }
                            final Expression arg = args.get(0);
                            if (arg instanceof JsonContainerExpr) {
                                // When it's a json container, we should expect a bunch of Gwt compile parameters
                                // TODO defer to one of the many instances of gwt params that we maintain...
                                // for now...
                                throw new NotYetImplemented("$gwt({params...}) is not (yet) implemented");
                            } else if (arg instanceof NameExpr || arg instanceof StringLiteralExpr || arg instanceof TemplateLiteralExpr) {
                                String module = tools.resolveString(ctx, arg);
                                // The given module might be an entry point, or it might be a named gwt module.

                                if (module.indexOf('.') != -1) {
                                    // Attempt to load entry point class, and slurp up @Gwtc annotations
                                }
                                String gwtXml = module.replace('.', '/') + ".gwt.xml";
                                script.setSrc(module + "/" + module + ".nocache.js");
                            }
//                            info.addGwtModule();
                            break;
                        default:
                            throw new IllegalArgumentException("Route methods other than $gwt are not yet supported; you sent " + method.getName());
                    }
                } else if (attr.getExpression() instanceof JsonContainerExpr) {
                    // a json container.  We want to generate a generator that emits <script>name = { some: "thing" };</script>
                    // This is a likely candidate to contain resources to be injected into the page
                } else {
                    // assume text, create code to resolve template values, if present, at runtime.
                    final Expression scriptBody = tools.resolveVar(ctx, attr.getExpression());
                    String scriptText = tools.resolveString(ctx, scriptBody);
                    if (scriptText.indexOf('$') != -1) {
                        // When it looks like there are unresolved templates, we'll want to emit
                        // a Route that has an embedded Template to resolve w/ runtime values

                    }
                    if (generator.looksLikeUrl(scriptText)) {
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
                // See about hooking up CssMapProxy...
                break;
            case "dom":
                // Add some html to the page body.
                // TODO: actually look at what's going on, and decide if we should generate static source or a runtime handler
                final RouteDomGeneratingVisitor visitor = createDomVisitor();
                attr.getExpression().accept(visitor, info);
                break;
            default:
                assert false : "Unhandled page part: " + tools.debugNode(attr);
        }
    }

    protected RouteDomGeneratingVisitor createDomVisitor() {
        return new RouteDomGeneratingVisitor();
    }

    protected String routeName(ContainerMetadata ui) {
        final Maybe<UiAttrExpr> name = ui.getUi().getAttribute("name");
        String desired = name.mapNullSafe(UiAttrExpr::getExpression).mapNullSafe(
            expr->tools.resolveString(ctx, expr)
        ).ifAbsentReturn("installRoute");
        return layer.newMethodName(desired);
    }

    protected class RouteDomGeneratingVisitor extends VoidVisitorAdapter<GeneratedRouteInfo> {

        private UiContainerExpr parentContainer;
        private UiAttrExpr parentAttr;

        @Override
        public void visit(UiAttrExpr n, GeneratedRouteInfo arg) {
            // TODO handle some special names, like id or ref (putting them into info map...)
            final UiAttrExpr was = parentAttr;
            parentAttr = n;
            try {
                String val = tools.resolveString(ctx, n.getExpression());
                final DomBuffer target = arg.getDomTarget();
                switch (n.getNameString().toLowerCase()) {
                    case "id":
                        arg.recordId(val, parentContainer, target);
                        break;
                    case "ref":
                        arg.recordRef(val, parentContainer, target);
                        break;
                }
                target.setAttribute(n.getNameString(), val);
            } finally {
                parentAttr = was;
            }
        }

        @Override
        public void visit(UiContainerExpr n, GeneratedRouteInfo arg) {
            // When generating routes, we'll want to take note of objects with dynamism in them;
            // for example, tags like <if/> or <for/>, or values containing $templateNames
            final UiContainerExpr wasContainer = parentContainer;
            parentContainer = n;
            try {

                switch (n.getName().toLowerCase()) {
                    case "sub-route":
                    case "subroute":
                        // A <subroute /> element is code which wishes to defer to another route for its content.
                        // This forces the response to be of type callback, so that we can defer to another source.
                        handleSubRoute(n, arg);
                        break;
                    case "if":
                        handleIf(n, arg);
                        break;
                    case "for":
                        handleFor(n, arg);
                        break;
                    default:
                        final DomBuffer wasBuffer = arg.getDomTarget();
                        try {
                            // Add code to print this route's element
                            final DomBuffer buf = arg.getDomTarget().makeTagNoIndent(n.getName());
                            arg.setDomTarget(buf);
                            for (UiAttrExpr attr : n.getAttributes()) {
                                attr.accept(this, arg);
                            }
                            if (n.getBody() != null) {
                                n.getBody().accept(this, arg);
                            }
                        } finally {
                            arg.setDomTarget(wasBuffer);
                        }
                }
            } finally {
                parentContainer = wasContainer;
            }
        }

        @Override
        public void visit(TemplateLiteralExpr n, GeneratedRouteInfo arg) {
            String val = tools.resolveString(ctx, n);
            if (val.trim().isEmpty()) {
                System.out.println();
            }
            arg.getDomTarget().print(val);
        }

        @Override
        public void visit(NameExpr n, GeneratedRouteInfo arg) {
            String val = tools.resolveString(ctx, n);
            arg.getDomTarget().print(val);
        }

        @Override
        public void visit(StringLiteralExpr n, GeneratedRouteInfo arg) {
            String val = tools.resolveString(ctx, n);
            arg.getDomTarget().print("\"" + X_Source.escape(val) + "\"");
        }

        @Override
        public void visit(IntegerLiteralExpr n, GeneratedRouteInfo arg) {
            arg.getDomTarget().print(n.toSource());
        }

        @Override
        public void visit(LongLiteralExpr n, GeneratedRouteInfo arg) {
            arg.getDomTarget().print(n.toSource());
        }

        @Override
        public void visit(DoubleLiteralExpr n, GeneratedRouteInfo arg) {
            arg.getDomTarget().print(n.toSource());
        }

        @Override
        public void visit(CharLiteralExpr n, GeneratedRouteInfo arg) {
            arg.getDomTarget().print(n.toSource());
        }

        @Override
        public void visit(BooleanLiteralExpr n, GeneratedRouteInfo arg) {
            arg.getDomTarget().print(n.toSource());
        }

        @Override
        public void visit(MethodCallExpr n, GeneratedRouteInfo arg) {
            if (n.getName().startsWith("$")) {
                // magic methods...  we'll want to defer the handling of these...
            }
            super.visit(n, arg);
        }
    }

    private void handleFor(UiContainerExpr n, GeneratedRouteInfo arg) {
        // TODO extract the for-loop logic in UiTagUiGenerator into something generic enough to reuse
    }

    private void handleIf(UiContainerExpr n, GeneratedRouteInfo arg) {
    }

    private void handleSubRoute(UiContainerExpr n, GeneratedRouteInfo arg) {

    }
}
