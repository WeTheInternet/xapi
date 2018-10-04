package xapi.dev.ui.api;

import xapi.dev.api.GeneratedJavaFile;
import xapi.dev.api.GeneratedTypeOwner;
import xapi.dev.api.GeneratedTypeParameter;
import xapi.dev.api.SourceLayer;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.SourceBuilder;
import xapi.source.read.JavaModel.IsTypeDefinition;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiApi extends GeneratedUiLayer {

    private boolean shouldSave = false;

    public GeneratedUiApi(GeneratedTypeOwner owner, String packageName, String className) {
        super(packageName, className, SourceLayer.Api, owner);
        suffix = "Component";
        setType(IsTypeDefinition.newInterface(packageName, className));
    }

    @Override
    public boolean isInterface() {
        return true;
    }

    @Override
    protected SourceBuilder<GeneratedJavaFile> createSource() {
        return super.createSource();
    }

    @Override
    public SourceBuilder<GeneratedJavaFile> getSource() {
        shouldSave = true;
        return super.getSource();
    }

    @Override
    public boolean shouldSaveType() {
        return shouldSave;
    }

    @Override
    protected IsTypeDefinition definition() {
        return IsTypeDefinition.newInterface(getPackageName(), getWrappedName());
    }

    @Override
    public GeneratedJavaFile getImplementor() {
        return getOwner().getBase();
    }

    public String getLocalName(
        UiGeneratorService generator,
        SourceLayer layer,
        UiNamespace ns,
        CanAddImports imports
    ) {
        return getWrappedName() +
            getTypeParameters()
                .filter(GeneratedTypeParameter::isExposed)
                .map(p-> GeneratedUiComponent.computeDeclaration(p,this, layer, generator, ns, imports))
                .join("<",",", ">");
    }

    @Override
    public GeneratedUiApi setPrefix(String prefix) {
        super.setPrefix(prefix);
        return this;
    }

    @Override
    public GeneratedUiApi setSuffix(String suffix) {
        super.setSuffix(suffix);
        return this;
    }
}
