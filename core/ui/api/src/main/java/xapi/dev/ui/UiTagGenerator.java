package xapi.dev.ui;

import com.github.javaparser.ast.expr.DynamicDeclarationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.Type;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.UiVisitScope.ScopeType;
import xapi.except.NotYetImplemented;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.Out1;
import xapi.util.X_String;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/24/16.
 */
public class UiTagGenerator extends UiComponentGenerator {

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools tools, ContainerMetadata me, UiContainerExpr n
    ) {
        if (n.getName().equalsIgnoreCase("define-tags")) {
            // we have a list of tags to consider
            return generateTagList(tools, me, n);
        } else if (n.getName().equalsIgnoreCase("define-tag")) {
            return generateTag(tools, me, n);
        } else {
            throw new IllegalArgumentException("Unhandled component type " + n.getName() + "; " + tools.debugNode(n));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected UiVisitScope generateTagList(UiGeneratorTools tools, ContainerMetadata me, UiContainerExpr n) {
        final Out1<String> rootPkg = Lazy.deferred1(()->tools.getPackage(me.getContext(), n, this::getDefaultPackage));
        final ApiGeneratorContext ctx = me.getContext();
        final Expression tags = tools.resolveVar(ctx, n.getAttributeNotNull("tags").getExpression());
        if (tags instanceof JsonContainerExpr) {
            final JsonContainerExpr json = (JsonContainerExpr) tags;
            if (json.isArray()) {
                // All pairs in an array must be <dom /> based
                final MappedIterable<UiContainerExpr> resolvers = json.getValues()
                    .map(tools.varResolver(ctx));
                resolvers.forEach(ui->{
                        final String tagName = tools.resolveString(me.getContext(), ui.getAttributeNotNull("name"));
                        // look in the current <ui /> for a package
                        String pkg = tools.getPackage(ctx, ui, rootPkg);
                        doTagGeneration(tools, me, ui, pkg, tagName);
                });
            } else {
                // Use the names of the {keys: ofJson}
                json.getPairs().forEach(pair->{
                    final String keyName = tools.resolveString(me.getContext(), pair.getKeyExpr());
                    final Expression value = tools.resolveVar(me.getContext(), pair.getValueExpr());
                    if (value instanceof UiContainerExpr) {
                        String pkg = tools.getPackage(ctx, value, rootPkg);
                        doTagGeneration(tools, me, (UiContainerExpr) value, pkg, keyName);
                    } else {
                        throw new IllegalArgumentException("Invalid json in define-tags; expected dom values:" +
                            " <define-tags tags={name: <define-tag />} />; You sent: " + tools.debugNode(value));
                    }
                });
            }
        } else if (tags instanceof UiContainerExpr){
            // Just a single item in the define-tags list.
            UiContainerExpr ui = (UiContainerExpr) tags;
            final String tagName = tools.resolveString(me.getContext(), ui.getAttributeNotNull("name"));
            // look in the current <ui /> for a package
            String pkg = tools.getPackage(ctx, ui, rootPkg);
            doTagGeneration(tools, me, ui, pkg, tagName);
        } else {
            throw new IllegalArgumentException("define-tags must have a `tags` feature that is either {name: json()}, <dom/>, or [<doms/>]");
        }
        return new UiVisitScope(ScopeType.CONTAINER).setVisitChildren(false);
    }

    protected String getDefaultPackage() {
        return "xapi.ui.generated";
    }

    protected UiVisitScope generateTag(UiGeneratorTools tools, ContainerMetadata me, UiContainerExpr n) {
        String pkg = tools.getPackage(me.getContext(), n, this::getDefaultPackage);
        String name = tools.resolveString(me.getContext(), n.getAttributeNotNull("name"));
        doTagGeneration(tools, me, n, pkg, name);
        return UiVisitScope.DEFAULT_CONTAINER;
    }

    private void doTagGeneration(UiGeneratorTools tools, ContainerMetadata me, UiContainerExpr n, String pkg, String name) {
        final String tagName = name;
        final String className = toClassName(name);
        final Maybe<UiAttrExpr> data = n.getAttribute("data");
        final Maybe<UiAttrExpr> model = n.getAttribute("model");
        final Maybe<UiAttrExpr> ui = n.getAttribute("ui");
        final Maybe<UiAttrExpr> css = n.getAttribute("css");
        final Maybe<UiAttrExpr> cssClass = n.getAttribute("class");
        final Maybe<UiAttrExpr> style = n.getAttribute("style");
        final MappedIterable<UiAttrExpr> eventHandlers = n.getAttributesMatching(
            attr -> attr.getNameString().startsWith("on")
        );
        SourceBuilder<?> api = new SourceBuilder<>("public class " + className);
        SourceBuilder<?> base = new SourceBuilder<>("public class " + baseClassName(className));
        api.setPackage(pkg);
        base.setPackage(pkg);
        base.addInterfaces(className);

        if (data.isPresent()) {
            // The component has some internal data (not shared in model)
            addDataAccessors(tools, me, data.get(), base);
        }
        if (model.isPresent()) {
            // The component has a public data api (get/set Model)
            addDataModel(tools, me, model.get(), api);
        }
        // There is some css to inject with this component
        if (css.isPresent()) {
            addCss(tools, me, api, css.get(), false);
        }
        if (cssClass.isPresent()) {
            addCss(tools, me, api, cssClass.get(), true);
        }
        if (style.isPresent()) {
            addCss(tools, me, api, cssClass.get(), false);
        }
        if (ui.isPresent()) {
            // The component has defined some display ui
            addUi(tools, me, ui.get(), api);
        }

        eventHandlers.forEach(onHandler->{
            switch (onHandler.getNameString().toLowerCase()) {
                case "onclick":
                case "ondragstart":
                case "ondragend":
                case "ondrag":
                case "onmouseover":
                case "onmouseout":
                case "onlongpress":
                case "onrightclick":
            }
        });

        tools.getGenerator().overwriteSource(pkg, className+".java", api.toSource(), null);
        tools.getGenerator().overwriteSource(pkg, baseClassName(className) + ".java", base.toSource(), null);
    }

    protected String baseClassName(String className) {
        return "Base" + className;
    }

    private String toClassName(String name) {
        String[] bits = name.split("-");
        StringBuilder b = new StringBuilder();
        for (String bit : bits) {
            b.append(X_String.toTitleCase(bit));
        }
        return b.toString();
    }

    private void addCss(
        UiGeneratorTools tools,
        ContainerMetadata me,
        SourceBuilder<?> api,
        UiAttrExpr uiAttrExpr,
        boolean isClassName
    ) {}

    private void addUi(UiGeneratorTools tools, ContainerMetadata me, UiAttrExpr uiAttrExpr, SourceBuilder<?> api) {

    }

    private void addDataModel(
        UiGeneratorTools tools,
        ContainerMetadata me,
        UiAttrExpr attr,
        SourceBuilder<?> api
    ) {
        String name = "Model" + api.getSimpleName();
        SourceBuilder<?> model = new SourceBuilder<>("public interface " + name);
        // TODO consider some annotations to control this behavior
        boolean immutable = attr.getAnnotation(anno->anno.getNameString().equalsIgnoreCase("immutable")).isPresent();
        final Expression expr = attr.getExpression();
        final ApiGeneratorContext ctx = me.getContext();
        if (expr instanceof JsonContainerExpr) {
            JsonContainerExpr json = (JsonContainerExpr) expr;
            json.getPairs().forEach(pair->{
                String fieldName = tools.resolveString(ctx, pair.getKeyExpr());
                fieldName = Character.toUpperCase(fieldName.charAt(0)) +
                    (fieldName.length() == 1 ? "" : fieldName.substring(1));
                final Expression typeExpr = tools.resolveVar(ctx, pair.getValueExpr());
                if (typeExpr instanceof DynamicDeclarationExpr) {
                    // Must be a default method.
                    throw new NotYetImplemented("Adding default methods to model types not yet implemented");
                } else {
                    Type type = tools.methods().$type(tools, ctx, typeExpr).getType();
                    // TODO smart import lookups...
                    String typeName = model.addImport(type.toSource());
                    model.getClassBuffer().createMethod("public " + typeName + " get"+fieldName)
                        .makeAbstract();
                    if (!immutable) {
                        final boolean localImmutable = pair.getAnnotation(anno -> anno.getNameString().equalsIgnoreCase(
                            "immutable")).isPresent();
                        if (!localImmutable) {
                            model.getClassBuffer().createMethod("public " + name + " set"+fieldName)
                                .addParameter(typeName, fieldName)
                                .makeAbstract();
                            if (typeName.contains("[]") || typeName.matches("Collection|List|Set|Map|IntTo|StringTo|ObjectTo|ClassTo")) {
                                // TODO We'll need adders, removers and clear

                            }
                        }
                    }
                }
            });
            tools.getGenerator().overwriteSource(api.getPackage(), name+".java", model.toSource(), null);
        } else {
            throw new IllegalArgumentException("<define-tag model={mustBe: Json} />; you sent " + tools.debugNode(attr));
        }
    }

    private void addDataAccessors(
        UiGeneratorTools tools,
        ContainerMetadata me,
        UiAttrExpr uiAttrExpr,
        SourceBuilder<?> base
    ) {}

    @Override
    public void endVisit(UiGeneratorTools service, ContainerMetadata me, UiContainerExpr n, UiVisitScope scope) {
        super.endVisit(service, me, n, scope);
    }
}
