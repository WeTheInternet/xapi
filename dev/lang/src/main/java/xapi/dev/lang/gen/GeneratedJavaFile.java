package xapi.dev.lang.gen;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.fu.X_Fu;
import xapi.fu.itr.EmptyIterator;
import xapi.fu.itr.SizedIterable;
import xapi.source.util.X_Modifier;
import xapi.source.X_Source;
import xapi.source.api.HasSourceBuilder;
import xapi.source.read.JavaModel.IsTypeDefinition;
import xapi.source.read.SourceUtil;

import java.lang.reflect.Modifier;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedJavaFile implements HasSourceBuilder<GeneratedJavaFile> {
    private final Lazy<SourceBuilder<GeneratedJavaFile>> source;
    // lazy pointers to "next valid suffix" for newly requested fields (use getOrCreate methods for a single member per name)
    private final Lazy<StringTo<Integer>> fieldNames;
    private final Lazy<StringTo<Integer>> methodNames;
    // lazy pointers to method and field buffers
    private final Lazy<StringTo<MethodBuffer>> methods;
    private final Lazy<StringTo.Many<UserDefinedMethod>> userMethods;
    private final Lazy<StringTo<FieldBuffer>> fields;
    private final Lazy<StringTo<MethodBuffer>> constructors;

    private final String packageName;
    private final String typeName;

    protected final GeneratedJavaFile superType;

    private final GeneratedTypeOwner owner;
    /**
     * Arbitrary text to append
     */
    protected String suffix;
    /**
     * Arbitrary text to prepend
     */
    protected String prefix;

//    protected final StringTo<In2Out1<UiNamespace, CanAddImports, String>> generics;

    private IsTypeDefinition type;


    public boolean shouldSaveType() {
        return true;
    }

    public GeneratedJavaFile(GeneratedTypeOwner owner, String pkg, String cls) {
        this(owner, null, pkg, cls);
    }

    @SuppressWarnings("unchecked")
    public GeneratedJavaFile(GeneratedTypeOwner owner, GeneratedJavaFile superType, String pkg, String cls) {
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
        constructors = Lazy.deferred1(X_Collect::newStringMap, MethodBuffer.class);
        userMethods = Lazy.deferred1(X_Collect::newStringMultiMap, UserDefinedMethod.class);
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

    public String reserveMethodName(String prefix) {
        if (methodNames.out1().put(prefix, 0) != null) {
            throw new IllegalStateException("Method " + prefix + " already reserved / used!");
        }
        return prefix;
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

    public GeneratedJavaFile setPrefix(String prefix) {
        assert prefix != null;
        this.prefix = prefix;
        return this;
    }

    public String getSuffix() {
        return suffix;
    }

    public GeneratedJavaFile setSuffix(String suffix) {
        assert suffix != null;
        this.suffix = suffix;
        return this;
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

    @Override
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

    public GeneratedTypeOwner getOwner() {
        return owner;
    }

    public boolean hasMethod(String name) {
        return methodNames.isResolved() && methodNames.out1().containsKey(name);
    }
    public MethodBuffer getOrCreateMethod(int modifiers, String returnType, String name) {
        return getOrCreateMethod(modifiers, returnType, name, In1.ignored());
    }
    public MethodBuffer getOrCreateMethod(int modifiers, String returnType, String name, In1<MethodBuffer> initMethod) {
        return methods.out1().getOrCreate(name, n->{
            final String realName = newMethodName(name);
            final MethodBuffer method = getSource().getClassBuffer().createMethod(modifiers, returnType, realName);
            initMethod.in(method);
            return method;
        });
    }

    public boolean hasField(String name) {
        return fields.isResolved() && fields.out1().has(name);
    }
    public FieldBuffer getOrCreateField(int modifiers, String returnType, String name) {
        return getOrCreateField(modifiers, returnType, name, In1.ignored());
    }
    public FieldBuffer getOrCreateField(int modifiers, String returnType, String name, In1<FieldBuffer> initField) {
        return fields.out1().getOrCreate(name, n->{
            final String realName = newFieldName(name);
            final FieldBuffer field = getSource().getClassBuffer().createField(returnType, realName, modifiers);
            initField.in(field);
            return field;
        }).setModifier(modifiers); // always overwrite modifiers; last caller wins.  If you have exotic needs, submit a pull request
    }

    public MethodBuffer getDefaultConstructor() {
        return getDefaultConstructor(-1);
    }
    public MethodBuffer getDefaultConstructor(int modifiers) {
        final MethodBuffer buffer = constructors.out1().getOrCreateFrom("",
            getSource().getClassBuffer()::createConstructor, X_Modifier.PUBLIC
        );
        if (modifiers != -1) {
            // last caller of a non-default (-1) value wins.
            buffer.setModifier(modifiers);
        }
        return buffer;
    }

    /**
     * If you have an interface / implementation pair of classes,
     * like {@link GeneratedUiApi} and {@link GeneratedUiBase},
     * you should override this method to return the type that is expected to implement methods / house instance fields.
     */
    public GeneratedJavaFile getImplementor() {
        return null;
    }

    public void addMethod(UserDefinedMethod userMethod) {
        userMethods.out1().add(userMethod.getDecl().getName(), userMethod);
    }

    /**
     * You probably shouldn't call this directly on a particular generated file.
     *
     * Instead, check with {@link GeneratedUiComponent#findUserMethod(String)},
     * to have all relevant layers of generated code checked for your method.
     */
    public SizedIterable<UserDefinedMethod> findMethod(String named) {
        if (userMethods.isUnresolved()) {
            return EmptyIterator.none();
        }
        return userMethods.out1().forEachValue(named);

    }

    public String getConstantName() {
        return getTypeName().replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

}
