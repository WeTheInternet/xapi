package xapi.dev.lang.gen;

import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.source.read.JavaModel.IsTypeDefinition;
import xapi.string.X_String;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiModel extends GeneratedJavaFile {
    private final StringTo<GeneratedUiMember> fields;
    private String nameOverride;

    public GeneratedUiModel(GeneratedTypeOwner owner, String packageName, String className) {
        this(owner, null, packageName, className);
    }

    public GeneratedUiModel(GeneratedTypeOwner owner, GeneratedJavaFile superType, String packageName, String className) {
        super(owner, superType, packageName, className);
        fields = X_Collect.newStringMap(GeneratedUiMember.class);
        setType(IsTypeDefinition.newInterface(packageName, className));
    }

    public StringTo<GeneratedUiMember> getFields() {
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
        // can't use a class reference here... :-/
        source.getClassBuffer().addInterface("xapi.model.api.Model");
        assert source.toSource().contains("interface");
        getOwner().getRecommendedImports().forEach(source.getImports()::autoImportName);
        return source;
    }

    public String getModelType() {
        String name;
        if (nameOverride == null) {
            name = getTypeName();
        } else {
            name = nameOverride;
        }
        //noinspection UnnecessaryLocalVariable // nice for debugging.
        final String modelType = X_String.firstCharToLowercase(name.replace("Model", ""));
        return modelType;
    }

    @Override
    protected String wrapName(String className) {
        return nameOverride == null ? "Model" + className : nameOverride;
    }

    public GeneratedUiMember addField(ApiGeneratorTools tools, Type type, String fieldName, boolean immutable) {
        final GeneratedUiMember newField = new GeneratedUiMember(type, fieldName);
        fields.put(fieldName, newField);

        final ClassBuffer buf = getSource().getClassBuffer();
        String typeName = tools.lookupType(buf.getImports(), type.toSource());
        if (typeName.contains(".")) {
            // If we turned a simple name into a complex one, be sure to import it
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

    public GeneratedUiMember getField(String name) {
        return fields.get(name);
    }

    public void overrideName(String name) {
        this.nameOverride = name;
    }
}
