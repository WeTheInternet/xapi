package xapi.dev.ui.impl;

import xapi.dev.source.ClassBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.api.UiComponentGenerator.UiGenerateMode;
import xapi.platform.Platform;

import java.lang.reflect.Modifier;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/7/17.
 */
public class ApiOnlyGenerator extends AbstractUiImplementationGenerator<ApiOnlyGeneratorContext> {

    private boolean uiStub;

    @Override
    protected void initializeComponent(GeneratedUiComponent result, ContainerMetadata metadata) {
        if (result.addImplementationFactory(this, Platform.class, GeneratedApiOnlyComponent::new)) {
            super.initializeComponent(result, metadata);
        }
    }

    @Override
    protected void standardInitialization(GeneratedUiComponent result, ContainerMetadata metadata) {
        if (isUiStub()) {
            super.standardInitialization(result, metadata);
        }
    }

    @Override
    public GeneratedUiImplementation generateComponent(
        ContainerMetadata metadata, ComponentBuffer buffer, UiGenerateMode mode
    ) {
        final GeneratedUiImplementation impl = super.generateComponent(metadata, buffer, mode);

        final UiNamespace ns = impl.reduceNamespace(namespace());

        final ClassBuffer out = impl.getSource().getClassBuffer();

        if (isUiStub()) {
            out
                .createConstructor(Modifier.PUBLIC, ns.getElementType(out)+" el")
                .println("super(el);");
        }
        return impl;
    }

    @Override
    public GeneratedUiImplementation getImpl(GeneratedUiComponent component) {
        return component.getImpls()
            .filterInstanceOf(GeneratedApiOnlyComponent.class)
            .firstMaybe()
            .getOrThrow(()->new IllegalStateException("Did not have a GeneratedApiOnlyComponent."));
    }

    @Override
    protected String getImplPrefix() {
        return "Stub";
    }

    public boolean isUiStub() {
        return uiStub;
    }

    public ApiOnlyGenerator setUiStub(boolean uiStub) {
        this.uiStub = uiStub;
        return this;
    }
}
