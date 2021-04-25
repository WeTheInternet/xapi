package xapi.dev.api;

import xapi.fu.Lazy;
import xapi.source.X_Source;
import xapi.string.X_String;

import static xapi.fu.Lazy.deferAll;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 10/2/18 @ 4:41 AM.
 */
public abstract class GeneratedTypeWithModel extends GeneratedJavaFile {

    protected Lazy<GeneratedUiModel> model = deferAll(
        GeneratedUiModel::new,
        this::getOwner,
        this::getPackageName,
        this::getNameForModel
    );

    public GeneratedTypeWithModel(GeneratedTypeOwner owner, String pkg, String cls) {
        super(owner, pkg, cls);
    }

    public GeneratedTypeWithModel(GeneratedTypeOwner owner, GeneratedJavaFile superType, String pkg, String cls) {
        super(owner, superType, pkg, cls);
    }

    protected String getNameForModel() {
        return getTypeName(); // you may want to suffix your models.
    }

    public boolean hasModel() {
        return model.isResolved();
    }

    public boolean isModelResolved() {
        return model.isResolved();
    }

    public GeneratedUiModel getModel() {
        return model.out1();
    }


    public String getModelName() {
        return model.out1().getWrappedName();
    }
    public String getModelFieldName() {
        return X_String.firstCharToLowercase(getTypeName());
    }

    public String getModelNameQualified() {
        final String modelName = model.out1().getWrappedName();
        if (modelName.indexOf('.') == 0) {
            return X_Source.qualifiedName(getPackageName(), modelName);
        }
        return modelName;
    }


}
