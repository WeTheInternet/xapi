package xapi.dev.ui.api;

import xapi.dev.source.SourceBuilder;
import xapi.source.read.JavaModel.IsTypeDefinition;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiApi extends GeneratedUiLayer {

    boolean shouldSave = false;

    public GeneratedUiApi(String packageName, String className) {
        super(packageName, className);
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
}
