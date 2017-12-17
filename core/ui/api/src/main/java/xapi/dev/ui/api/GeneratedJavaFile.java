package xapi.dev.ui.api;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Lazy;
import xapi.fu.X_Fu;
import xapi.source.X_Source;
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
    private final Lazy<StringTo<MethodBuffer>> methods;
    private final Lazy<StringTo<FieldBuffer>> fields;
    private final String packageName;
    private final String typeName;
    protected final GeneratedJavaFile superType;
    private final GeneratedUiComponent owner;
    protected String prefix, suffix;
//    protected final StringTo<In2Out1<UiNamespace, CanAddImports, String>> generics;

    private IsTypeDefinition type;


    public boolean shouldSaveType() {
        return true;
    }

    public GeneratedJavaFile(GeneratedUiComponent owner, String pkg, String cls) {
        this(owner, null, pkg, cls);
    }

    @SuppressWarnings("unchecked")
    public GeneratedJavaFile(GeneratedUiComponent owner, GeneratedJavaFile superType, String pkg, String cls) {
        this.owner = owner;
        source = Lazy.deferred1(this::createSource);
        suffix = prefix = "";
        this.packageName = pkg;
        this.superType = superType;
        this.typeName = cls;
        fieldNames = Lazy.deferred1(X_Collect::newStringMap, Integer.class);
        methodNames = Lazy.deferred1(X_Collect::newStringMap, Integer.class);
        methods = Lazy.deferred1(X_Collect::newStringMap, MethodBuffer.class);
        fields = Lazy.deferred1(X_Collect::newStringMap, FieldBuffer.class);
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

    public String getQualifiedName() {
        return X_Source.qualifiedName(getPackageName(), getWrappedName());
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
        if (packageName != null) {
            builder.setPackage(packageName);
        }
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

    public GeneratedJavaFile setType(IsTypeDefinition type) {
        this.type = type;
        return this;
    }

    public boolean isInterface() {
        return false;
    }

    public void ensureField(String type, String name) {
        ensureFieldDefined(type, name, true);
    }
    public void ensureFieldDefined(String type, String name, boolean define) {
        GeneratedJavaFile check = this;
        synchronized (fieldNames) {
            while (check != null) {
                if (check.fieldNames.out1().containsKey(name)) {
                    return;
                }
                check = check.superType;
            }
            fieldNames.out1().put(name, 0);

            if (define) {
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

    public GeneratedUiComponent getOwner() {
        return owner;
    }

    public boolean hasMethod(String name) {
        return methods.isResolved() && methods.out1().has(name);
    }
    public MethodBuffer getOrCreateMethod(int modifiers, String returnType, String name) {
        return methods.out1().getOrCreate(name, n->{
            final String realName = newMethodName(name);
            final MethodBuffer method = getSource().getClassBuffer().createMethod(modifiers, returnType, realName);
            return method;
        });
    }
}
