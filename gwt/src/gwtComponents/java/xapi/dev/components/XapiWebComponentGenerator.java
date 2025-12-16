package xapi.dev.components;

import net.wti.lang.parser.ast.expr.Expression;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.lang.parser.ast.type.ClassOrInterfaceType;
import net.wti.lang.parser.ast.type.Type;
import elemental.dom.Element;
import xapi.components.api.ComponentNamespace;
import xapi.components.api.UiConfig;
import xapi.components.impl.WebComponentBuilder;
import xapi.components.impl.WebComponentSupport;
import xapi.components.impl.WebComponentVersion;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.lang.gen.GeneratedTypeOwner;
import xapi.dev.lang.gen.GeneratedUiMember;
import xapi.dev.lang.gen.SourceLayer;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.api.UiComponentGenerator.UiGenerateMode;
import xapi.dev.ui.impl.AbstractUiImplementationGenerator;
import xapi.dev.ui.impl.InterestingNodeFinder.InterestingNodeResults;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;
import xapi.model.service.ModelCache;
import xapi.platform.GwtPlatform;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.api.component.ModelComponentMixin;
import xapi.ui.api.component.ModelComponentOptions;

import java.lang.reflect.Modifier;

import static xapi.dev.ui.api.UiConstants.EXTRA_INSTALL_METHOD;
import static xapi.dev.ui.api.UiGeneratorPlatform.PLATFORM_WEB_COMPONENT;

@UiGeneratorPlatform(PLATFORM_WEB_COMPONENT)
public class XapiWebComponentGenerator extends AbstractUiImplementationGenerator <WebComponentContext> {

    private static final In1Out1<GeneratedTypeOwner,GeneratedUiImplementation> NEW_COMPONENT = GeneratedWebComponent::new;

    private String configType;
    private Lazy<UiNamespace> namespace = Lazy.deferred1(()->
        X_Inject.instance(UiNamespaceGwt.class)
    );

    @Override
    protected String getImplPrefix() {
        return "Gwt";
    }

    @Override
    public void initializeComponent(GeneratedUiComponent result, ContainerMetadata metadata) {
        if (result.addImplementationFactory(this, GwtPlatform.class, NEW_COMPONENT)) {
            super.initializeComponent(result, metadata);
        }
    }

    private Type createType(String s) {
        return new ClassOrInterfaceType(s);
    }

    @Override
    public GeneratedWebComponent getImpl(GeneratedUiComponent component) {
        for (GeneratedUiImplementation impl : component.getImpls()) {
            if (impl instanceof GeneratedWebComponent) {
                final GeneratedWebComponent winner = (GeneratedWebComponent) impl;
                final UiContainerExpr ast = component.getAst();
                if (!ast.hasExtra(EXTRA_INSTALL_METHOD)) {
                    // this is normally called for us by calling code,
                    // but we need to do this sooner, so we can install this "extra" method.
                    // Might be wise to just make a proper manager class for this method,
                    // and slap it into the GeneratedUiComponent (though, it only really
                    // makes sense for ui components, the class _is_ specifically for UIs
                    // [despite being usable for other purposes that just don't touch ui-only stuff]).
                    winner.setPrefix(getImplPrefix());
                    final ClassBuffer out = winner.getSource().getClassBuffer();
                    final MethodBuffer mthd = out
                        // TODO: rename to install()
                        .createMethod("public static void assemble()");

                    component.getAst().addExtra(EXTRA_INSTALL_METHOD, mthd);
                }

                return winner;
            }
        }
        throw new IllegalStateException("No GeneratedWebComponent impl found");
    }

    @Override
    public GeneratedUiImplementation generateComponent(
        ContainerMetadata metadata, ComponentBuffer buffer,
        UiGenerateMode mode
    ) {
        final GeneratedUiImplementation component = super.generateComponent(metadata, buffer, mode);
        fillInImpl((GeneratedWebComponent) component, metadata, buffer);
        return component;
    }

    protected void fillInImpl(
        GeneratedWebComponent component,
        ContainerMetadata metadata,
        ComponentBuffer buffer
    ) {
        final UiContainerExpr container = metadata.getUi();
        final Maybe<UiAttrExpr> data = container.getAttribute("data");
        final Maybe<UiAttrExpr> model = container.getAttribute("model");
        final Maybe<UiAttrExpr> ui = container.getAttribute("ui");
        final Maybe<UiAttrExpr> css = container.getAttribute("css");
        final Maybe<UiAttrExpr> cssClass = container.getAttribute("class");
        final Maybe<UiAttrExpr> style = container.getAttribute("style");

        final UiNamespace ns = component.reduceNamespace(namespace());

        final ClassBuffer out = component.getSource().getClassBuffer();
        String element = out.addImport(Element.class);
//        String node = out.addImport(Node.class);
//        String map = out.addImport(MapLike.class);
//        String styleElement = out.addImport(StyleElement.class);
//        String clientBundle = out.addImport(ClientBundle.class);
//        String cssResource = out.addImport(CssResource.class);
//        String assembler = out.addImport(StyleAssembler.class);
        String builder = out.addImport(WebComponentBuilder.class);
        String version = out.addImport(WebComponentVersion.class);
        String support = out.addImport(WebComponentSupport.class);
        String clazz = out.addImportStatic(WebComponentBuilder.class, "htmlElementClass");
        String opts = model.isPresent() ? out.addImport(ModelComponentOptions.class) : out.addImport(ComponentOptions.class);
        String i1o1 = out.addImport(In1Out1.class);
        String o1 = out.addImport(Out1.class);
        String ctor = out.addImport(ComponentConstructor.class);
        String compNs = out.addImport(ComponentNamespace.class);
        final String type = out.getSimpleName();
        final GeneratedUiComponent generated = buffer.getGeneratedComponent();

        final GeneratedUiApi api = component.getOwner().getApi();
        // TODO different supertypes based on needs
        final String genericName = type; // api.getTypeName() + "<" + node+", " + element + ">;
        final String apiGenerics = "<" + element + ", " + genericName +">";

        String ctorName = "NEW_" + generated.getTagName().toUpperCase().replace('-', '_');
        final FieldBuffer ctorField = out.createField(ctor + apiGenerics, ctorName)
            .makeStatic().makePrivate();
        final FieldBuffer getUi = out.createField(i1o1 + apiGenerics, "getUi").makeStatic().makePrivate();

        final String genericApi = api.getLocalName(generator, SourceLayer.Impl, ns, out);

        final String apiOptType = opts + "<" + element + ", " + (
            model.isPresent() ?
                buffer.getGeneratedComponent().getPublicModel().getWrappedName() + ", " : "")
            + genericApi + ">";

        final String optType = opts + "<" + element + ", " + (
            model.isPresent() ?
                buffer.getGeneratedComponent().getPublicModel().getWrappedName() + ", " : "")
            + component.getWrappedName() + ">";

        out.createConstructor(Modifier.PUBLIC, element+" el")
            .println("super(el);");

        out.createConstructor(Modifier.PUBLIC, apiOptType + " opts")
            .addAnnotation(out.addImport(SuppressWarnings.class) + "(\"unchecked\")")
            .patternln("super(opts, (ComponentConstructor)$1);", ctorName);

        MethodBuffer mthd = component.getOwner().getAst().getExtra(EXTRA_INSTALL_METHOD);

        mthd.addParameter(configType(ns, out), "assembler");
        component.setMetadata(mthd, ctorField, getUi);

        mthd.patternln("if ($1 != null) { return; }", ctorName);

        mthd.println(builder + " component = new " + builder + "(" + clazz+"(), " + version + ".V1);");
        mthd.println();
        mthd.println("component.setClassName(\"" + component.getTypeName() + "\");");

        // TODO generate CSS as needed

        mthd.println(optType + " opts = new " + opts + "<>();");

        mthd.println("getUi = " + support+".installFactory(component, " + type+"::new, opts);");

        // Now, add a "constructor" callback.
        mthd.println("component.afterCreatedCallback(e->{")
            .indent()
            .print("final " + type + " c = ")
            .println("get" + api.getWrappedName() + "(e);")
            // Now, lets grab the inner dom and jam it into our element.
            .println("c.getElement(); // ensure ui is initialized")
            // TODO: add pre-override checks, in case an element wants to
            // check the element body for source code before generating interface
            .outdent()
            .println("});");

        // Let the component print various callbacks
        component.writeCallbacks(mthd);

        mthd.println(ctorName + " = " + support+".define(")
            .indentln("\"" + buffer.getTagName() + "\", component);");

        // Consider adding this to the constructor used for this component,
        // then remove the corresponding call in AbstractWtiComponent (private project)
        // JsSupport.setFactory(element, this, ComponentNamespace.JS_KEY);

        // Create a global "get component from raw element" method
        out.createMethod(type + " get" + api.getWrappedName() )
            .makePublic().makeStatic()
            .addParameter(element, "e")
            .indent()
            .println("assert e != null;")
            .println("assert e.getTagName().toLowerCase().equals(\"" + generated.getTagName() +"\");")
            .print("final " + type + " component = ")
            .println(compNs + ".getComponent(e, getUi);")
            .returnValue("component")
            .outdent()

            ;

        out.createMethod(type + " create()" )
            .makePublic().makeStatic()
            .addParameter(optType, "opts")
            .println("if (opts == null) {")
            .indentln("opts = new " + opts + "<>();")
            .println("}")
            .returnValue(ctorName + ".constructComponent(opts, getUi)");

        if (model.isPresent()) {
            final CanAddImports imp = component.getSource();
            ns.getBaseStyleResourceType(imp);

            component.getSource().getClassBuffer()
                .addGenericInterface(ns.getModelComponentMixin(imp), element, api.getModelName());
        }

        component.getRequiredChildren().forAll(child->{
            final GeneratedUiDefinition def = child.getDefinition();
            final Expression source = child.getSourceNode();
            final String eleBuilder = component.getElementBuilderType(ns);
            String name = "create" + def.getApiName();
            final MethodBuffer createChild = out.createMethod("public " + eleBuilder + " " + name)
                                                .addAnnotation(Override.class)
                                                .println("final " + eleBuilder + " builder = newBuilder()")
                                                .indentln(".setTagName(\"" + def.getTagName() + "\");");
            final String keyName;
            boolean printClosingBrace = false;
            if (def.getModelName() == null) {
                keyName = null;
            } else {
                final ApiGeneratorContext ctx = buffer.getRoot().getContext();
                final Expression resolved = getTools().resolveVar(ctx, source);
                final String modName = out.addImport(def.getQualifiedModelName());
                final String xmodel = out.addImport(X_Model.class);
                final String modelCache = out.addImport(ModelCache.class);
                final String modelKey = out.addImport(ModelKey.class);
                GeneratedUiMember modelInfo = resolved.getExtra(UiConstants.EXTRA_MODEL_INFO);
                final String modKeyConstant = def.getModelKeyConstant();
                if (modelInfo != null) {
                    final String typeToUse = getTools().getComponentType(resolved, modelInfo.getMemberType());
                    String paramType = out.addImport(typeToUse);
                    String varName = modelInfo.getMemberName();
                    createChild.addParameter(paramType, varName);
                    createChild.println("if (" + varName + " != null) {")
                               .indent();
                    printClosingBrace = true;
                    if ("ModelKey".equals(paramType)) {
                        // We are being sent a key.
                        keyName = varName;
                    } else {
                        // assume it is a model
                        createChild
                                    // TODO: route cache and key as local var names
                                   .println(modelCache + " cache = " + "cache();")
                                   .println(modelKey + " key = cache.ensureKey(" + modName + "." + modKeyConstant+", " + varName + ");")
                                   .println("cache.cacheModel(" + varName + ", ignore->{});");
                        keyName = "key";
                    }
                } else {
                    // This mess leftover from initial prototype...

                    String printModel = getTools().resolveString(ctx, resolved)
                        // filthy hack... good enough for now, but we need
                        // to bring sanity to our ApiGeneratorContext, and how it is distributed
                        .replace("$model", "getModel()");
                    int lastGet = printModel.lastIndexOf("get");
                    final String setterName = printModel.substring(0, lastGet) + "s" + printModel.substring(lastGet + 1);
                    createChild .println(modName + " mod = " + printModel + ";")
                                .println("if (mod == null) {")
                                .indentln("mod = " + xmodel + ".create(" + modName+".class);")
                                .indentln(setterName.substring(0, setterName.length()-1) + "mod);")
                                .println("}")
                                .println(modelCache + " cache = " + xmodel + ".cache();")
                                .println(modelKey + " key = cache.ensureKey(" + modName + "." + modKeyConstant + ", mod);")
                                .println("cache.cacheModel(mod, ignore->{});");
                    keyName = "key";
                }
            }

            if (keyName != null) {
                String mixin = createChild.addImportStatic(ModelComponentMixin.class, ModelComponentMixin.METHOD_SHORTEN);
                String attrName = createChild.addImportStatic(ModelComponentMixin.class, ModelComponentMixin.FIELD_MODEL_ATTR_NAME);
                createChild
                    .patternln("if ($1 != null) {", keyName).indent()
                    .patternln("builder.setAttribute(\"$1\", $2($3));", attrName, mixin, keyName)
                    .outdent().println("}");
            }
            if (printClosingBrace) {
                createChild.outdent().println("}");
            }

            createChild.println("return builder;");

        });

    }

    protected String configType(UiNamespace ns, ClassBuffer out) {
        String type = getConfigType();
        if (type != null) {
            return type;
        }
        String config = out.addImport(UiConfig.class) + "<"
            + ns.getElementType(out) +", "
            + ns.getStyleElementType(out) +", "
            + "? extends " + ns.getStyleResourceType(out) +", "
            + ns.getServiceType(out)
        + ">";
        return config;
    }

    @Override
    public void spyOnNewComponent(ComponentBuffer component) {
        super.spyOnNewComponent(component);
    }

    @Override
    public void spyOnInterestingNodes(
        ComponentBuffer component, InterestingNodeResults interestingNodes
    ) {
        super.spyOnInterestingNodes(component, interestingNodes);
    }

    @Override
    public UiNamespace namespace() {
        return namespace.out1();
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

}
