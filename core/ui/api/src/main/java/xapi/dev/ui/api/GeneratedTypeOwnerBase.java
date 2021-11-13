package xapi.dev.ui.api;

import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringTo;
import xapi.dev.lang.gen.*;
import xapi.dev.source.CanAddImports;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.impl.ImplementationLayer;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.*;
import xapi.fu.itr.EmptyIterator;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple starting point for shared logic of GeneratedTypeOwner instances.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/2/18 @ 1:55 AM.
 */
public abstract class GeneratedTypeOwnerBase <Api extends GeneratedUiLayer, Base extends GeneratedUiLayer, Impl extends ImplementationLayer> implements
        GeneratedTypeOwner {

    private final Lazy<UiContainerExpr> ast;
    private final GeneratedUiGenericInfo info;
    private final Lazy<Api> api;
    private final Lazy<Base> base;
    private final StringTo<GeneratedJavaFile> extraLayers;
    protected final StringTo<ReferenceType> globalDefs;
    private final ClassTo<Lazy<? extends Impl>> impls;
    private final String pkg;
    private final String typeName;
    private SizedIterable<String> recommendedImports;

    protected GeneratedTypeOwnerBase(
        String pkg,
        String typeName,
        Out1<UiContainerExpr> ast,
        GeneratedUiGenericInfo info
    ) {
        recommendedImports = EmptyIterator.none();
        this.pkg = pkg;
        this.typeName = typeName;
        this.ast = Lazy.deferred1(ast);
        this.info = info;
        api = Lazy.deferred1(this::createApi);
        base = Lazy.deferred1(this::createBase);
        globalDefs = X_Collect.newStringMapInsertionOrdered(ReferenceType.class);
        extraLayers = X_Collect.newStringMap(GeneratedUiLayer.class);
        impls = X_Collect.newClassMap(Lazy.class);
    }

    @Override
    public String getPackageName() {
        return pkg;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    @Override
    public UiContainerExpr getAst() {
        return ast.out1();
    }

    @Override
    public GeneratedUiGenericInfo getGenericInfo() {
        return info;
    }

    protected abstract Api makeApi();
    protected abstract Base makeBase();

    public void saveSource(UiGeneratorService<?> gen) {
        // Save any extra layers... We do this last so other "mainstream" generation can create standalone Extras,
        // knowing they will be saved even if defined in an impl or builder type
        extraLayers.forEachValue()
            .filter(GeneratedJavaFile::shouldSaveType)
            .forAll(layer->gen.overwriteSource(layer.getPackageName(), layer.getWrappedName(), layer.toSource(), null));
    }

    protected Api createApi() {
        final Api created = makeApi();
        globalDefs.forBoth(created::addLocalDefinition);
        return created;
    }

    protected Base createBase() {
        final Base created = makeBase();
        globalDefs.forBoth(created::addLocalDefinition);
        return created;
    }

    public Api getApi() {
        return api.out1();
    }

    public Base getBase() {
        return base.out1();
    }

    protected boolean isApiResolved() {
        return api.isResolved();
    }

    public boolean isBaseResolved() {
        return base.isResolved();
    }

    @Override
    public SizedIterable<String> getRecommendedImports() {
        return recommendedImports;
    }

    public void setRecommendedImports(SizedIterable<String> recommendedImports) {
        this.recommendedImports = recommendedImports;
    }

    public void addRecommendedImports(SizedIterable<String> recommendedImports) {
        if (this.recommendedImports == null) {
            setRecommendedImports(recommendedImports);
        } else {
            this.recommendedImports = this.recommendedImports.plus(recommendedImports);
        }
    }

    public GeneratedJavaFile getOrCreateExtraLayer(String id, String pkg, String cls) {
        return extraLayers.getOrCreate(id, ignored->new GeneratedJavaFile(this, pkg, cls));
    }

    @Override
    public GeneratedJavaFile addExtraLayer(String id, GeneratedJavaFile file) {
        final GeneratedJavaFile was = extraLayers.put(id, file);
        if (was != null && was != file) {
            throw new IllegalStateException("Reused extraLayers key " + id +
                ", was: " + was + "; added:" + file);
        }
        return file;
    }

    public GeneratedJavaFile getOrCreateExtraLayer(String id, String cls, In3Out1<GeneratedTypeOwner, String, String, GeneratedJavaFile> factory) {
        return extraLayers.getOrCreate(id, factory.supply1(this).supply1(getPackageName()).supply(cls).ignoreIn1());
    }

    protected void saveType(GeneratedUiLayer ui, UiGeneratorService<?> gen, UiGeneratorTools<?> tools, MappedIterable<GeneratedTypeParameter> generics) {
        if (ui.shouldSaveType()) {
            ui.prepareToSave(tools);
            final SourceBuilder<GeneratedJavaFile> out = ui.getSource();
            if (generics != null) {
                // If the generics are non-null, then we need to add them to our supertype.
                String rawSuper = out.getClassBuffer().getSuperClass();
                if (rawSuper != null) {
                    if (rawSuper.contains("<")) {
                        rawSuper = rawSuper.substring(0, rawSuper.length()-1);
                    } else {
                        rawSuper += "<";
                    }
                    boolean first = true;
                    for (GeneratedTypeParameter generic : generics) {
                        UiNamespace ns = tools.namespace();
                        if (ui instanceof GeneratedUiImplementation) {
                            ns = ((GeneratedUiImplementation) ui).reduceNamespace(ns);
                        }
                        if (first) {
                            first = false;
                        } else {
                            rawSuper += ", ";
                        }
                        String value = computeDeclaration(generic, ui, ui.getLayer(), gen, ns, out);
                        rawSuper += value;
                    }
                    rawSuper += ">";
                    out.getClassBuffer().setSuperClass(rawSuper.replace("<>", ""));
                }
            }
            if (ui.isModelResolved()) {
                final GeneratedUiModel model = ui.getModel();
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

    public static String computeDeclaration(
        GeneratedTypeParameter generic,
        GeneratedUiLayer layer,
        SourceLayer forLayer,
        UiGeneratorService<?> generator,
        UiNamespace ns,
        CanAddImports out
    ) {
        final List<ClassOrInterfaceType> bounds;
        final TypeParameter type = generic.getType();
        final String systemName = generic.getSystemName();

        if (forLayer == SourceLayer.Impl) {
            bounds = Collections.emptyList();
        } else {
            bounds = new ArrayList<>(type.getTypeBound());
        }
        String name;
        if (forLayer == SourceLayer.Impl) {
            name = ns.loadFromNamespace(systemName, out)
                .ifAbsentSupply(type::getName);
        } else {
            return type.toString();
        }
        if (UiNamespace.TYPE_SELF.equals(name)) {
            // When requesting a self type, we want to add the known bounds of the given layer.
            final MappedIterable<String> names = layer.getGenericNames();
            if (!names.isEmpty()) {
                names
                    .map(ClassOrInterfaceType::new)
                    .forAll(bounds::add);
            }
        }
        if (bounds.isEmpty()) {
            return name;
        }
        final String baseName = ns.loadFromNamespace(systemName, out)
            .ifAbsentReturn(name);
        StringBuilder b = new StringBuilder(baseName);
        String spacer = bounds.size() > 2 ? ",\n" : ", ";
        String toAdd = "<";
        for (ClassOrInterfaceType param : bounds) {
            b.append(toAdd);
            toAdd = spacer;
            // TODO: require UiGeneratorTools so we can normalize any magic strings in our name
            String source = param.toSource();
            if (generator != null) {
                //                generator.tools().resolveString()
            }
            b.append(source);
        }
        if (!"<".equals(toAdd)) {
            b.append(">");
        }

        return b.toString();

    }

    @Override
    public MappedIterable<Impl> getImpls() {
        return impls.forEachValue()
            .map(Lazy::out1);
    }

    public boolean addImplementationFactory(
        ImplementationGenerator<?, Impl> generator,
        Class<?> platform,
        In1Out1<? super GeneratedTypeOwnerBase, Impl> io) {
        if (impls.containsKey(platform)) {
            return false;
        }
        final In1Out1<GeneratedTypeOwnerBase, Impl> spy = In1Out1.weakenOutput(io.spyOut(impl -> {
            impl.setGenerator(generator);
            final In2<String, ReferenceType> addLocal = impl::addLocalDefinition;
            globalDefs.forBoth(addLocal);
        }).strengthenInput());
        final Lazy<? extends Impl> lazy = Lazy.deferSupplier(spy, this);
        final Lazy<? extends Impl> result = impls.put(platform, lazy);
        if (result != null) {
            assert checkSafeOverride(platform, result, lazy);
            return false;
        }
        return true;
    }

    protected boolean checkSafeOverride(
        Class<?> platform, Lazy<? extends Impl> first,
        Lazy<? extends Impl> second
    ) {
        final Impl one = first.out1();
        final Impl two = second.out1();
        assert one == two : "Attempting to override a platform factory; " +
            "[[  " + one + "   ]] != [[   " + two + "   ]] for " + platform.getName();
        return true;
    }



}
