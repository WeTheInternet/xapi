package xapi.dev.ui.api;

import com.github.javaparser.ast.type.Type;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.SourceBuilder;
import xapi.fu.In1Out1;
import xapi.source.read.JavaModel.IsTypeDefinition;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiApi extends GeneratedUiLayer {

    private boolean shouldSave = false;

    public GeneratedUiApi(GeneratedUiComponent owner, String packageName, String className) {
        super(packageName, className, ImplLayer.Api, owner);
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

    /**
     * Add an arbitrary extension to this Api;
     *
     *
     * @param pkgName
     * @param apiName
     * @param baseName
     * @param apiFactory
     * @param baseFactory
     */
    public void addExtension(
        String pkgName,
        String apiName,
        String baseName,
        In1Out1<Type, String> apiFactory,
        In1Out1<Type, String> baseFactory
    ) {
    }

    public String getConstantName() {
        return getTypeName().replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    @Override
    public GeneratedJavaFile getImplementor() {
        return getOwner().getBase();
    }

    public String getLocalName(
        UiGeneratorService generator,
        ImplLayer layer,
        UiNamespace ns,
        CanAddImports imports
    ) {
        return getWrappedName() +
            getTypeParameters()
                .filter(GeneratedTypeParameter::isExposed)
                .map(p-> p.computeDeclaration(this, layer, generator, ns, imports))
                .join("<",",", ">");
    }
}
