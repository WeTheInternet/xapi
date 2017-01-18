package xapi.dev.ui;

import com.github.javaparser.ast.expr.AnnotationExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.fu.*;
import xapi.model.api.Model;
import xapi.source.read.JavaModel.IsTypeDefinition;
import xapi.ui.api.NodeBuilder;
import xapi.util.X_String;

import static xapi.fu.Lazy.deferAll;
import static xapi.fu.Lazy.deferSupplier;

import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/6/17.
 */
public class GeneratedUiComponent {

    public static class GeneratedUiMember implements Serializable {
        private String memberName;
        private final IntTo<AnnotationExpr> annotations;
        private String memberType;

        public GeneratedUiMember(String memberType, String memberName) {
            annotations = X_Collect.newList(AnnotationExpr.class);
            this.memberType = memberType;
            this.memberName = memberName;
        }

        public String getMemberName() {
            return memberName;
        }

        public void setMemberName(String memberName) {
            this.memberName = memberName;
        }

        public String getMemberType() {
            return memberType;
        }

        public void setMemberType(String memberType) {
            this.memberType = memberType;
        }

        public String getCapitalized() {
            return Character.toUpperCase(memberName.charAt(0))
                + (memberName.length() == 1 ? "" : memberName.substring(1));
        }

        public String getterName() {
            return ("boolean".equals(memberType) || "Boolean".equals(memberType) ? "is" : "get")
                + getCapitalized();
        }
    }
    public static class GeneratedUiField extends GeneratedUiMember {

        public GeneratedUiField(String memberType, String memberName) {
            super(memberType, memberName);
        }
    }
    public static class GeneratedUiMethod extends GeneratedUiMember {

        public GeneratedUiMethod(String memberType, String memberName) {
            super(memberType, memberName);
        }
    }

    public static class GeneratedJavaFile {
        private final Lazy<SourceBuilder<GeneratedJavaFile>> source;
        private final Lazy<StringTo<Integer>> fieldNames;
        private final Lazy<StringTo<Integer>> methodNames;
        private final String packageName;
        private final String typeName;
        protected String prefix, suffix;
        private ContainerMetadata metadata;

        private IsTypeDefinition type;

        public boolean shouldSaveType() {
            return true;
        }

        public GeneratedJavaFile(String pkg, String cls) {
            source = Lazy.deferred1(this::createSource);
            suffix = prefix = "";
            this.packageName = pkg;
            this.typeName = cls;
            fieldNames = Lazy.deferred1(()->X_Collect.newStringMap(Integer.class));
            methodNames = Lazy.deferred1(()->X_Collect.newStringMap(Integer.class));
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
    }

    public static class GeneratedUiModel extends GeneratedJavaFile {
        protected final StringTo<GeneratedUiField> fields;

        public GeneratedUiModel(String packageName, String className) {
            super(packageName, className);
            fields = X_Collect.newStringMap(GeneratedUiField.class);
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

        public GeneratedUiField addField(UiGeneratorTools tools, String typeName, String fieldName, boolean immutable) {
            final GeneratedUiField newField = new GeneratedUiField(typeName, fieldName);
            fields.put(fieldName, newField);

            final ClassBuffer buf = getSource().getClassBuffer();
            if (typeName.contains(".")) {
                typeName = buf.addImport(typeName);
            }
            String capitalized = X_String.toTitleCase(fieldName);
            buf.createMethod(typeName + " get"+capitalized)
                .makeAbstract();
            if (!immutable) {
                    buf.createMethod("public " + typeName + " set"+capitalized)
                        .addParameter(typeName, fieldName)
                        .makeAbstract();
                    if (typeName.contains("[]") ||
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

    public static abstract class GeneratedUiLayer extends GeneratedJavaFile {
        protected Lazy<GeneratedUiModel> model = deferAll(
            GeneratedUiModel::new,
            this::getPackageName,
            this::getWrappedName
        );
        private String nameElement, nameElementBuilder, nameStyleService, nameStyleElement;
        private final StringTo<In2Out1<UiNamespace, CanAddImports, String>> generics;
        private final StringTo<In1<GeneratedUiImplementation>> abstractMethods;

        @SuppressWarnings("unchecked")
        public GeneratedUiLayer(String pkg, String cls) {
            super(pkg, cls);
            generics = X_Collect.newStringMapInsertionOrdered(In2Out1.class);
            abstractMethods = X_Collect.newStringMap(In1.class);
        }

        public String getModelName() {
            return model.out1().getWrappedName();
        }

        public GeneratedUiModel getModel() {
            return model.out1();
        }

        protected abstract IsTypeDefinition definition();

        @Override
        protected SourceBuilder<GeneratedJavaFile> createSource() {
            final SourceBuilder<GeneratedJavaFile> source = super.createSource();
            final IsTypeDefinition definition = definition();
            if (definition != null) {
                source.setClassDefinition(definition.toDefinition(), false);
                source.setPackage(definition.getPackage());
            }
            return source;
        }

        public boolean hasModel() {
            return model.isResolved();
        }

        public String getElementType(UiNamespace namespace) {
            if (nameElement == null) {
                if (this instanceof GeneratedUiApi || this instanceof GeneratedUiBase) {
                    // When in api/base layer, we need to use generics instead of concrete types.
                    nameElement = getSource().getImports().reserveSimpleName("Element", "E", "El", "Ele");
                    generics.put(nameElement, UiNamespace::getElementType);
                } else {
                    nameElement = namespace.getElementType(getSource());
                }
            }
            // Use the concrete type
            return nameElement;
        }

        public String getElementBuilderType(UiNamespace namespace) {
            if (nameElementBuilder == null) {
                if (this instanceof GeneratedUiApi || this instanceof GeneratedUiBase) {
                    // When in api/base layer, we need to use generics instead of concrete types.
                    nameElementBuilder = getSource().getImports().reserveSimpleName("ElementBuilder", "Builder", "EB", "ElBuilder");
                    String nodeBuilder = getSource().addImport(NodeBuilder.class);
                    final String elementType = getElementType(namespace);
                    generics.put(elementType, UiNamespace::getElementType);
                    generics.put(nameElementBuilder + " extends " + nodeBuilder + "<" + elementType + ">", UiNamespace::getElementBuilderType);
                } else {
                    nameElementBuilder = namespace.getElementBuilderType(getSource());
                }
            }
            // Use the concrete type
            return nameElementBuilder;
        }

        public boolean hasGenerics() {
            return !generics.isEmpty();
        }

        public MappedIterable<String> getGenerics() {
            return generics.mappedKeys();
        }

        public StringTo<In2Out1<UiNamespace, CanAddImports, String>> getGenericsMap() {
            return generics;
        }
    }

    public static class GeneratedUiApi extends GeneratedUiLayer {

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

    public static class GeneratedUiBase extends GeneratedUiLayer {

        private final String apiName;

        public GeneratedUiBase(GeneratedUiApi api) {
            super(api.getPackageName(), api.getTypeName());
            this.apiName = api.getWrappedName();
        }

        @Override
        protected SourceBuilder<GeneratedJavaFile> createSource() {
            final SourceBuilder<GeneratedJavaFile> builder = super.createSource();
            return builder;
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
    }

    public static class GeneratedUiImplementation extends GeneratedUiLayer {

        protected final String apiName;
        protected final String implName;

        public GeneratedUiImplementation(String pkg, GeneratedUiApi api, GeneratedUiBase base) {
            super(pkg, api.getTypeName());
            apiName = api.getWrappedName();
            implName = base.getWrappedName();
            setSuffix("Component");
        }

        @Override
        protected SourceBuilder<GeneratedJavaFile> createSource() {
            final SourceBuilder<GeneratedJavaFile> source = super.createSource();
            source.setSuperClass(implName);
            return source;
        }

        public void commitOutput(UiGeneratorService<?> gen) {
            if (shouldSaveType()) {
                gen.overwriteSource(getPackageName(), getWrappedName(), getSource().toSource(), null);
            }
        }

        protected IsTypeDefinition definition() {
            return IsTypeDefinition.newClass(getPackageName(), getWrappedName());
        }

        public UiNamespace reduceNamespace(UiNamespace from) {
            return from;
        }
    }

    private final Lazy<GeneratedUiApi> api;
    private final Lazy<GeneratedUiBase> base;
    private final ClassTo<Lazy<GeneratedUiImplementation>> impls;
    private final String packageName;
    private final String className;
    private final StringTo<FieldBuffer> refs;
    private String nameElementBuilderFactory;

    public GeneratedUiComponent(String pkg, String cls) {
        api = Lazy.deferred1(this::createApi);
        base = Lazy.deferred1(this::createBase);
        impls = X_Collect.newClassMap(Lazy.class);
        this.packageName = pkg;
        this.className = cls;
        refs = X_Collect.newStringMap(FieldBuffer.class);
    }

    public String getElementBuilderConstructor(UiNamespace namespace) {
        if (nameElementBuilderFactory == null) {
            final GeneratedUiBase baseClass = base.out1();
            final String builderName = baseClass.newMethodName("newBuilder");
            baseClass.getSource().getClassBuffer().makeAbstract();
            String builderType = baseClass.getElementBuilderType(namespace);
            // When in api/base layer, we will create an abstract method that impls must fill;
            baseClass.getSource().getClassBuffer().createMethod("public abstract " +builderType +" " + builderName + "()");
            getImpls().forAll(impl -> {
                final UiNamespace ns = impl.reduceNamespace(namespace);
                String type = impl.getElementBuilderType(ns);
                impl.getSource()
                    .getClassBuffer().createMethod("public " + type +" " + builderName + "()")
                    .returnValue("new " + type + "()");
            });
            nameElementBuilderFactory = builderName + "()";
        }
        // Use the concrete type
        return nameElementBuilderFactory;
    }


    protected GeneratedUiBase createBase() {
        return new GeneratedUiBase(api.out1());
    }

    protected GeneratedUiApi createApi() {
        return new GeneratedUiApi(packageName, className);
    }

    public GeneratedUiApi getApi() {
        return api.out1();
    }

    public GeneratedUiBase getBase() {
        return base.out1();
    }

    public boolean hasPublicModel() {
        return getApi().hasModel();
    }

    public boolean hasPrivateModel() {
        return getBase().model.isResolved();
    }

    public GeneratedUiModel getPublicModel() {
        return getApi().model.out1();
    }

    public GeneratedUiModel getPrivateModel() {
        return getBase().model.out1();
    }

    public MappedIterable<GeneratedUiImplementation> getImpls() {
        return impls.forEachValue()
                    .map(Lazy::out1);
    }

    public boolean addImplementationFactory(Class<?> platform,
                                            In1Out1<GeneratedUiComponent, GeneratedUiImplementation> io) {
        final Lazy<GeneratedUiImplementation> result = impls.put(platform, deferSupplier(io, this));
        return result == null;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public void saveSource(UiGeneratorTools<?> tools, UiGeneratorService<?> gen) {
        // Write api
        StringTo<In2Out1<UiNamespace, CanAddImports, String>> apiGenerics = null;
        if (api.isResolved() || base.isResolved()) {
            final GeneratedUiApi apiLayer = api.out1();
            apiGenerics = apiLayer.getGenericsMap();
            if (apiLayer.hasGenerics()) {
                final ClassBuffer out = apiLayer.getSource().getClassBuffer();
                for (String generic : apiGenerics.keys()) {
                    out.addGenerics(generic);
                }
            }
            saveType(apiLayer, gen, tools, null);
        }
        StringTo<In2Out1<UiNamespace, CanAddImports, String>> baseGenerics = null;
        if (base.isResolved()) {
            // our actual class name is WrappedName, and we implement the api type name
            final GeneratedUiBase baseLayer = base.out1();
            // Only implement the api if we generated methods into it...
            final ClassBuffer out = baseLayer.getSource().getClassBuffer();
            baseGenerics = baseLayer.getGenericsMap();
            String apiName = baseLayer.apiName;
            if (apiGenerics != null && apiGenerics.isNotEmpty()) {
                apiName += "<";
                for (Out2<String, In2Out1<UiNamespace, CanAddImports, String>> generic : apiGenerics.forEachItem()) {
                    String name = generic.out1();
                    if (baseGenerics.containsKey(name)) {
                        // Add the generic to our implements clause
                        if (!apiName.endsWith("<")) {
                            apiName += ", ";
                        }
                        apiName += name;
                    } else {
                        // Add the generic to our generics clause
                        out.addGenerics(name);
                        baseLayer.getGenericsMap().put(name, generic.out2());
                    }
                }
                apiName += ">";
            }
            if (baseGenerics != null) {

                for (Out2<String, In2Out1<UiNamespace, CanAddImports, String>> generic : baseGenerics.forEachItem()) {
                    String name = generic.out1();
                    if (apiGenerics == null || !apiGenerics.containsKey(name)) {
                        out.addGenerics(name);
                        baseLayer.getGenericsMap().put(name, generic.out2());
                    }
                }
            }
            if (api.isResolved() && api.out1().shouldSaveType()) {
                out.addInterfaces(apiName);
            }


            saveType(baseLayer, gen, tools,null);
        }
        getImpls()
            .filter(GeneratedJavaFile::shouldSaveType)
            .forAll(this::saveType, gen, tools, baseGenerics);

    }

    protected void saveType(GeneratedUiLayer ui, UiGeneratorService<?> gen, UiGeneratorTools<?> tools, StringTo<In2Out1<UiNamespace, CanAddImports, String>> generics) {
        if (ui.shouldSaveType()) {
            final SourceBuilder<GeneratedJavaFile> out = ui.getSource();
            if (generics != null) {
                // If the generics are non-null, then we need to add them to our supertype.
                String rawSuper = out.getClassBuffer().getSuperClass();
                if (rawSuper.contains("<")) {
                    rawSuper = rawSuper.substring(0, rawSuper.length()-1);
                } else {
                    rawSuper += "<";
                }
                boolean first = true;
                for (Out2<String, In2Out1<UiNamespace, CanAddImports, String>> generic : generics.forEachItem()) {
                    String name = generic.out1();
                    UiNamespace ns = tools.namespace();
                    if (ui instanceof GeneratedUiImplementation) {
                        ns = ((GeneratedUiImplementation) ui).reduceNamespace(ns);
                    }
                    if (first) {
                        first = false;
                    } else {
                        rawSuper += ", ";
                    }
                    String value = generic.out2().io(ns, out);
                    rawSuper += value;
                }
                rawSuper += ">";
                out.getClassBuffer().setSuperClass(rawSuper);
            }
            if (ui.model.isResolved()) {
                final GeneratedUiModel model = ui.model.out1();
                if (model.shouldSaveType()) {
                    gen.overwriteSource(model.getPackageName(), model.getWrappedName(), model.getSource().toSource(), null);
                }

            }
            if (ui instanceof GeneratedUiImplementation) {
                // Generated uis must implement all unimplemented abstract methods.
                // let platform specific implementations get a chance to save files.
                ((GeneratedUiImplementation)ui).commitOutput(gen);
            } else {
                gen.overwriteSource(ui.getPackageName(), ui.getWrappedName(), out.toSource(), null);
            }
        }
    }

    public Do registerRef(String ref, FieldBuffer refField) {
        final FieldBuffer was = this.refs.put(ref, refField);
        return ()-> {
            final FieldBuffer is = refs.get(ref);
            if (is == refField) {
                if (was == null) {
                    refs.remove(ref);
                } else {
                    refs.put(ref, was);
                }
            }
        };
    }

}
