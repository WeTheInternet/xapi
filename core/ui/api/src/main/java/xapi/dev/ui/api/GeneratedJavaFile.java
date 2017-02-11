package xapi.dev.ui.api;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Lazy;
import xapi.fu.X_Fu;
import xapi.source.read.JavaModel.IsTypeDefinition;
import xapi.source.read.SourceUtil;

import java.lang.reflect.Modifier;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedJavaFile {
    private final Lazy<SourceBuilder<GeneratedJavaFile>> source;
    private final Lazy<StringTo<Integer>> fieldNames;
    private final Lazy<StringTo<Integer>> methodNames;
    private final String packageName;
    private final String typeName;
    protected final GeneratedJavaFile superType;
    protected String prefix, suffix;
    private ContainerMetadata metadata;

    private IsTypeDefinition type;

    public boolean shouldSaveType() {
        return true;
    }

    public GeneratedJavaFile(String pkg, String cls) {
        this(null, pkg, cls);
    }

    public GeneratedJavaFile(GeneratedJavaFile superType, String pkg, String cls) {
        source = Lazy.deferred1(this::createSource);
        suffix = prefix = "";
        this.packageName = pkg;
        this.superType = superType;
        this.typeName = cls;
        fieldNames = Lazy.deferred1(() -> X_Collect.newStringMap(Integer.class));
        methodNames = Lazy.deferred1(() -> X_Collect.newStringMap(Integer.class));
    }

    public String newFieldName(String prefix) {
        final Integer cnt;
        synchronized (fieldNames) {
            cnt = fieldNames.out1().compute(prefix, (k, was) -> was == null ? 0 : was + 1);
        }
        if (cnt == 0) {
            return prefix;
        }
        return prefix + "_" + cnt;
    }

    public String newMethodName(String prefix) {
        final Integer cnt;
        synchronized (methodNames) {
            cnt = methodNames
                .out1()
                .computeValue(prefix, X_Fu::nullSafeIncrement);
        }
        if (cnt == 0) {
            return prefix;
        }
        return prefix + "_" + cnt;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getWrappedName() {
        return wrapName(typeName);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        assert prefix != null;
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        assert suffix != null;
        this.suffix = suffix;
    }

    protected String wrapName(String className) {
        return prefix + className + suffix;
    }

    protected SourceBuilder<GeneratedJavaFile> createSource() {
        final SourceBuilder<GeneratedJavaFile> builder = new SourceBuilder<GeneratedJavaFile>()
            .setPayload(this);
        if (type != null) {
            builder.setClassDefinition(type.toDefinition(), false);
        }
        return builder;
    }

    public SourceBuilder<GeneratedJavaFile> getSource() {
        return source.out1();
    }

    public String toSource() {
        return getSource().toSource();
    }

    public void setType(IsTypeDefinition type) {
        this.type = type;
    }

    public boolean isInterface() {
        return false;
    }

    public void ensureField(String type, String name) {
        GeneratedJavaFile check = this;
        synchronized (fieldNames) {
            while (check != null) {
                if (check.fieldNames.out1().containsKey(name)) {
                    return;
                }
                check = check.superType;
            }
            fieldNames.out1().put(name, 0);

            final ClassBuffer buf = getSource().getClassBuffer();
            if (buf.isInterface()) {
                buf.createMethod("public " + type + " " + SourceUtil.toGetterName(type, name) + "()");
                buf.createMethod("public void " + SourceUtil.toSetterName(name) + "()")
                    .addParameter(type, name);
            } else {
                buf.createField(type, name, Modifier.PROTECTED)
                    .createGetter(Modifier.PUBLIC)
                    .createSetter(Modifier.PUBLIC);
            }
        }
    }
}
