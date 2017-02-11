package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.TypeExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringTo;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.*;

import static xapi.fu.Lazy.deferSupplier;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/6/17.
 */
public class GeneratedUiComponent {

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

    public GeneratedUiImplementation getBestImpl(GeneratedUiImplementation similarToo) {
        GeneratedUiImplementation best = null;
        int bestScore = Integer.MIN_VALUE;
        for (GeneratedUiImplementation impl : getImpls()) {
            int similarScore = similarToo.isSimilar(impl);
            if (similarScore > bestScore) {
                bestScore = similarScore;
                best = impl;
            }
        }

        if (best == null) {
            throw new IllegalArgumentException("Could not find matching impl for " + similarToo + " from " + getImpls());
        }
        return best;
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
            final ClassBuffer out = baseLayer.getSource().getClassBuffer();
            baseGenerics = baseLayer.getGenericsMap();
            String apiName = baseLayer.getApiName();

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
                out.getClassBuffer().setSuperClass(rawSuper.replace("<>", ""));
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

    public Maybe<GeneratedUiField> getModelField(String name) {
        if (hasPublicModel()) {
            return Maybe.nullable(getPublicModel().getField(name));
        }
        return Maybe.not();
    }

    public void addGeneric(String genericName, TypeExpr type) {
        final GeneratedUiApi a = getApi();
        a.getGenericsMap().put(genericName, (ns, imp)-> {
            return type.getType().toSource();
        });
    }
}
