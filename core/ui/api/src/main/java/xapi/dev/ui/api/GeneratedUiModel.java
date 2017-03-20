package xapi.dev.ui.api;

import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.model.api.Model;
import xapi.source.read.JavaModel.IsTypeDefinition;
import xapi.util.X_String;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiModel extends GeneratedJavaFile {
    private final StringTo<GeneratedUiField> fields;

    public GeneratedUiModel(String packageName, String className) {
        this(null, packageName, className);
    }

    public GeneratedUiModel(GeneratedJavaFile superType, String packageName, String className) {
        super(superType, packageName, className);
        fields = X_Collect.newStringMap(GeneratedUiField.class);
    }

    public StringTo<GeneratedUiField> getFields() {
        return fields;
    }

    @Override
    public boolean isInterface() {
        return true;
    }

    @Override
    protected SourceBuilder<GeneratedJavaFile> createSource() {
        final SourceBuilder<GeneratedJavaFile> source = super.createSource();
        final IsTypeDefinition type = IsTypeDefinition.newInterface(getPackageName(), getWrappedName());
        source.setClassDefinition(type.toDefinition(), false);
        source.setPackage(type.getPackage());
        source.getClassBuffer().addInterface(Model.class);
        return source;
    }

    @Override
    protected String wrapName(String className) {
        return "Model" + className;
    }

    public GeneratedUiField addField(UiGeneratorTools tools, Type type, String fieldName, boolean immutable) {
        final GeneratedUiField newField = new GeneratedUiField(type, fieldName);
        fields.put(fieldName, newField);

        final ClassBuffer buf = getSource().getClassBuffer();
        String typeName = tools.lookupType(type.toSource());
        if (typeName.contains(".")) {
            typeName = buf.addImport(typeName);
        }
        String capitalized = X_String.toTitleCase(fieldName);
        buf.createMethod(typeName + " get" + capitalized)
            .makeAbstract();
        if (!immutable) {
            buf.createMethod("public " + typeName + " set" + capitalized)
                .addParameter(typeName, fieldName)
                .makeAbstract();
            if (typeName.contains("[]")) {
                // TODO We'll need adders, removers and clear

            } else if (
                tools.allListTypes().anyMatch(typeName::equals)
                ) {
                // TODO We'll need adders, removers and clear


            }
        }

        return newField;
    }

    @Override
    public boolean shouldSaveType() {
        return !fields.isEmpty();
    }

    public GeneratedUiField getField(String name) {
        return fields.get(name);
    }
}
