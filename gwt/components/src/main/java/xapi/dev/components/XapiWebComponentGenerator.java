package xapi.dev.components;

import com.github.javaparser.ast.TypeArguments;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import elemental.dom.Element;
import elemental.dom.Node;
import elemental.html.StyleElement;
import xapi.components.api.UiConfig;
import xapi.components.impl.AbstractComponent;
import xapi.components.impl.WebComponentBuilder;
import xapi.components.impl.WebComponentSupport;
import xapi.components.impl.WebComponentVersion;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.api.UiComponentGenerator.UiGenerateMode;
import xapi.dev.ui.impl.AbstractUiImplementationGenerator;
import xapi.dev.ui.impl.InterestingNodeFinder.InterestingNodeResults;
import xapi.fu.In1Out1;
import xapi.fu.Lazy;
import xapi.fu.MapLike;
import xapi.fu.Maybe;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.platform.GwtPlatform;
import xapi.ui.api.StyleAssembler;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.api.component.IsComponent;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public class XapiWebComponentGenerator extends AbstractUiImplementationGenerator <WebComponentContext> {

    private String configType;
    private Lazy<UiNamespace> namespace = Lazy.deferred1(()-> X_Inject.instance(UiNamespaceGwt.class));

    @Override
    protected String getImplPrefix() {
        return "Gwt";
    }

    @Override
    public void initializeComponent(GeneratedUiComponent result) {
        if (result.addImplementationFactory(GwtPlatform.class, GeneratedWebComponent::new)) {
            super.initializeComponent(result);
            // We always want the raw element type in our generics (for now, at least)
            // This triggers a downstream extension of IsComponent<Element, MyType<Element, OtherGenerics>>
            final UiNamespace baseNs = getTools().namespace();
            final GeneratedUiApi api = result.getApi();
            final GeneratedUiBase base = result.getBase();
            final ClassBuffer baseOut = base.getSource().getClassBuffer();

            final String nodeName = base.getNodeType(baseNs);
            final String eleName = base.getElementType(baseNs);
            List<Type> types = new ArrayList<>();

            final ClassOrInterfaceType rawEle = new ClassOrInterfaceType(eleName);
            final ClassOrInterfaceType nodeType = new ClassOrInterfaceType(nodeName);
            final String componentName = baseOut.addImport(IsComponent.class);
            final ClassOrInterfaceType apiSuperType = new ClassOrInterfaceType(componentName);
            final ClassOrInterfaceType apiType = new ClassOrInterfaceType(api.getWrappedName());
            types.add(nodeType);
            types.add(rawEle);
            apiSuperType.setTypeArguments(TypeArguments.withArguments(types));
            apiType.setTypeArguments(TypeArguments.withArguments(types));
            // provide local definitions for api.  TODO: also provide base type now?
            result.addGlobalDefinition(UiNamespace.TYPE_API, new ReferenceType(apiType));

            final TypeParameter nodeParam = new TypeParameter(nodeName);
            String abstractCompPkg = AbstractComponent.class.getPackage().getName();
            String abstractCompName = AbstractComponent.class.getSimpleName();
            final GeneratedUiSupertype superType = new GeneratedUiSupertype(result, abstractCompPkg, abstractCompName,
                new GeneratedTypeParameter(UiNamespace.TYPE_NODE, nodeParam),
                new GeneratedTypeParameter(UiNamespace.TYPE_ELEMENT, new TypeParameter(eleName, nodeType)),
                new GeneratedTypeParameter(UiNamespace.TYPE_API, new TypeParameter(UiNamespace.TYPE_API, apiType))
            );
            result.updateSupertype(superType);

            superType.requireConstructor(new Parameter(UiNamespace.TYPE_ELEMENT, UiNamespace.TYPE_ELEMENT));

            final String o1 = baseOut.addImport(Out1.class);
            ClassOrInterfaceType out1 = new ClassOrInterfaceType(o1);
            out1.setTypeArguments(TypeArguments.withArguments(rawEle));
            superType.requireConstructor(new Parameter(out1, UiNamespace.TYPE_ELEMENT));

            types = new ArrayList<>();
            types.add(nodeType);
            types.add(rawEle);
            types.add(apiType);

            String opts = baseOut.addImport(ComponentOptions.class);
            String ctor = baseOut.addImport(ComponentConstructor.class);

            final ClassOrInterfaceType optsType = new ClassOrInterfaceType(opts);
            final ClassOrInterfaceType ctorType = new ClassOrInterfaceType(ctor);

            optsType.setTypeArguments(TypeArguments.withArguments(types));
            ctorType.setTypeArguments(TypeArguments.withArguments(types));

            superType.requireConstructor(
                new Parameter(optsType, "opts"),
                new Parameter(ctorType, "ctor")
            );

            final String apiNode = api.getNodeType(baseNs);
            final String apiEle = api.getElementType(baseNs);
            final GeneratedTypeParameter apiNodeParam = result.addGeneric(
                UiNamespace.TYPE_NODE,
                new TypeParameter(apiNode)
            );
            final GeneratedTypeParameter apiEleParam = result.addGeneric(
                UiNamespace.TYPE_ELEMENT,
                new TypeParameter(apiEle, nodeType)
            );
            apiEleParam.setExposed(true);
            String isCompPkg = IsComponent.class.getPackage().getName();
            String isCompName = IsComponent.class.getSimpleName();
            api.getSource().addImport(IsComponent.class);
            final GeneratedUiSupertype superInterface = new GeneratedUiSupertype(result, isCompPkg, isCompName,
                new GeneratedTypeParameter(UiNamespace.TYPE_NODE, apiNodeParam.getType()),
                new GeneratedTypeParameter(UiNamespace.TYPE_ELEMENT, apiEleParam.getType())
            );

            result.addSuperInterface(superInterface);

        }
    }

    private Type createType(String s) {
        return new ClassOrInterfaceType(s);
    }

    @Override
    public GeneratedUiImplementation getImpl(GeneratedUiComponent component) {
        for (GeneratedUiImplementation impl : component.getImpls()) {
            if (impl instanceof GeneratedWebComponent) {
                return impl;
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

    protected void fillInImpl(GeneratedWebComponent component, ContainerMetadata metadata, ComponentBuffer buffer) {
        final UiContainerExpr container = metadata.getUi();
        final Maybe<UiAttrExpr> data = container.getAttribute("data");
        final Maybe<UiAttrExpr> model = container.getAttribute("model");
        final Maybe<UiAttrExpr> ui = container.getAttribute("ui");
        final Maybe<UiAttrExpr> css = container.getAttribute("css");
        final Maybe<UiAttrExpr> cssClass = container.getAttribute("class");
        final Maybe<UiAttrExpr> style = container.getAttribute("style");

        final UiNamespace ns = component.reduceNamespace(namespace());

        final ClassBuffer out = component.getSource().getClassBuffer();
        String map = out.addImport(MapLike.class);
        String element = out.addImport(Element.class);
        String node = out.addImport(Node.class);
        String styleElement = out.addImport(StyleElement.class);
        String clientBundle = out.addImport(ClientBundle.class);
        String cssResource = out.addImport(CssResource.class);
        String assembler = out.addImport(StyleAssembler.class);
        String builder = out.addImport(WebComponentBuilder.class);
        String version = out.addImport(WebComponentVersion.class);
        String support = out.addImport(WebComponentSupport.class);
        String clazz = out.addImportStatic(WebComponentBuilder.class, "htmlElementClass");
        String opts = out.addImport(ComponentOptions.class);
        String i1o1 = out.addImport(In1Out1.class);
        String ctor = out.addImport(ComponentConstructor.class);
        final String type = out.getSimpleName();
        final GeneratedUiComponent generated = buffer.getGeneratedComponent();

        final GeneratedUiApi api = component.getOwner().getApi();
        // TODO different supertypes based on needs
        final String apiGenerics = "<" + node + ", " + element + ", " + api.getWrappedName() + "<" + node+", " + element + ">>";

        String ctorName = "NEW_" + generated.getTagName().toUpperCase().replace('-', '_');
        out.createField(ctor + apiGenerics, ctorName)
            .makeStatic().makePrivate();
        out.createField(i1o1+apiGenerics.replaceFirst(node+", ", ""), "getUi").makeStatic().makePrivate();

        out.createConstructor(Modifier.PUBLIC, element+" el")
            .println("super(el);");
        // TODO: call super once we fixup inheritance chain?

        final MethodBuffer mthd = out
            .createMethod("public static void assemble()")
            .addParameter(configType(ns, out), "assembler");

        mthd.println(builder + " component = new " + builder + "(" + clazz+"(), " + version + ".V1);");
        mthd.println();
        mthd.println("component.setClassName(\"" + component.getTypeName() + "\");");

        // TODO generate CSS as needed


        mthd.println(opts + apiGenerics + " opts = new " + opts + "<>();");

        mthd.println("getUi = " + support+".installFactory(component, " + type+"::new, opts);");

        // TODO Add various callbacks

        mthd.println(ctorName + " = " + support+".define(")
            .indentln("\"" + buffer.getTagName() + "\", component);");

        // Consider adding this to the constructor used for this component,
        // then remove the corresponding call in AbstractWtiComponent (private project)
        // JsSupport.setFactory(element, this, ComponentNamespace.JS_KEY);

    }

    protected String configType(UiNamespace ns, ClassBuffer out) {
        String type = getConfigType();
        if (type != null) {
            return type;
        }
        String config = out.addImport(UiConfig.class) + "<"
            + ns.getElementType(out) +", "
            + ns.getStyleElementType(out) +", "
            + ns.getStyleResourceType(out) +", "
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
