package xapi.dev.ui.api;

import xapi.dev.source.SourceBuilder;
import xapi.source.read.JavaModel.IsTypeDefinition;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiBase extends GeneratedUiLayer {

    private final String apiName;

    public GeneratedUiBase(GeneratedUiComponent owner, GeneratedUiApi api) {
        super(api, api.getPackageName(), api.getTypeName(), ImplLayer.Base, owner);
        this.apiName = api.getWrappedName();
    }

    @Override
    protected SourceBuilder<GeneratedJavaFile> createSource() {
        final SourceBuilder<GeneratedJavaFile> builder = super.createSource();
        return builder;
    }

    public String getApiName() {
        return apiName;
    }

    @Override
    protected String wrapName(String className) {
        return "Base" + apiName;
    }

    @Override
    public SourceBuilder<GeneratedJavaFile> getSource() {
        return super.getSource();
    }

    @Override
    protected IsTypeDefinition definition() {
        return IsTypeDefinition.newClass(getPackageName(), getWrappedName());
    }

    @Override
    public String getElementType(UiNamespace namespace) {
        // We want to force the base node type to come before the element type
        getNodeType(namespace);
        return super.getElementType(namespace);
    }

}
