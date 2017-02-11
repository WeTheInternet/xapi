package xapi.dev.components;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import elemental.dom.Element;
import elemental.html.StyleElement;
import xapi.components.api.UiConfig;
import xapi.dev.source.ClassBuffer;
import xapi.dev.ui.impl.AbstractUiImplementationGenerator;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.GeneratedUiImplementation;
import xapi.dev.ui.impl.InterestingNodeFinder.InterestingNodeResults;
import xapi.dev.ui.api.UiComponentGenerator.UiGenerateMode;
import xapi.dev.ui.api.UiNamespace;
import xapi.fu.Lazy;
import xapi.fu.MapLike;
import xapi.fu.Maybe;
import xapi.inject.X_Inject;
import xapi.platform.GwtPlatform;
import xapi.ui.api.StyleAssembler;

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
        }
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
        String styleElement = out.addImport(StyleElement.class);
        String clientBundle = out.addImport(ClientBundle.class);
        String cssResource = out.addImport(CssResource.class);
        String assembler = out.addImport(StyleAssembler.class);

        out
            .createMethod("public static void assemble()")
            .addParameter(configType(ns, out), "assembler");


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
