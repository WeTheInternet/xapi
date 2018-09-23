package xapi.dev.ui.api;

import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.*;
import xapi.dev.ui.api.GeneratedUiLayer.ImplLayer;
import xapi.dev.ui.impl.AbstractUiImplementationGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.assembler.AssembledElement;
import xapi.dev.ui.tags.assembler.AssembledUi;
import xapi.dev.ui.tags.assembler.UiAssembler;
import xapi.dev.ui.tags.factories.GeneratedFactory;
import xapi.dev.ui.tags.factories.LazyInitFactory;
import xapi.dev.ui.tags.members.UserDefinedMethod;
import xapi.fu.*;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.EmptyIterator;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.MappedIterable;
import xapi.source.X_Modifier;
import xapi.util.X_String;

import static com.github.javaparser.ast.expr.StringLiteralExpr.stringLiteral;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/6/17.
 */
public class GeneratedUiComponent {

    private final Lazy<GeneratedUiApi> api;
    private final Lazy<GeneratedUiBase> base;
    private final Lazy<GeneratedUiFactory> factory;
    private final ClassTo<Lazy<GeneratedUiImplementation>> impls;
    private final String packageName;
    private final String className;
    private final StringTo<FieldBuffer> refs;
    private final GeneratedUiGenericInfo genericInfo;
    private final StringTo<GeneratedUiSupertype> superInterfaces;
    private final StringTo<ReferenceType> globalDefs;
    private final StringTo<GeneratedJavaFile> extraLayers;
    private final ReservedUiMethods methods;
    private final ChainBuilder<In1<UiGeneratorService<?>>> beforeSave;
    private final Lazy<UiContainerExpr> ast;
    private String tagName;
    private GeneratedUiSupertype superType;
    private boolean uiComponent;

    public GeneratedUiComponent(String pkg, String cls, Out1<UiContainerExpr> ast) {
        this.ast = Lazy.deferred1(ast);
        api = Lazy.deferred1(this::createApi);
        base = Lazy.deferred1(this::createBase);
        factory = Lazy.deferred1(this::createFactory);
        impls = X_Collect.newClassMap(Lazy.class);
        this.packageName = pkg;
        this.className = cls;
        refs = X_Collect.newStringMapInsertionOrdered(FieldBuffer.class);
        superInterfaces =  X_Collect.newStringMap(GeneratedUiSupertype.class);
        globalDefs = X_Collect.newStringMapInsertionOrdered(ReferenceType.class);
        genericInfo = new GeneratedUiGenericInfo();
        extraLayers = X_Collect.newStringMap(GeneratedUiLayer.class);
        methods = new ReservedUiMethods(this);
        beforeSave = Chain.startChain();
    }

    public GeneratedUiImplementation getBestImpl(GeneratedUiImplementation similarToo) {
        GeneratedUiImplementation best = null;
        int bestScore = Integer.MIN_VALUE;
        for (GeneratedUiImplementation impl : getImpls()) {
            int similarScore = similarToo == null ? 1 : similarToo.isSimilar(impl);
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

    public ReservedUiMethods getMethods() {
        return methods;
    }
    public String getElementBuilderConstructor(UiNamespace namespace) {
        return methods.newBuilder(namespace);
    }

    public String getElementInjectorConstructor(UiNamespace namespace) {
        return methods.newInjector(namespace);
    }


    protected GeneratedUiBase createBase() {
        final GeneratedUiBase created = new GeneratedUiBase(this, api.out1());
        globalDefs.forBoth(created::addLocalDefinition);
        return created;
    }

    protected GeneratedUiFactory createFactory() {
        final GeneratedUiFactory created = new GeneratedUiFactory(this, api.out1());
        globalDefs.forBoth(created::addLocalDefinition);
        return created;
    }

    protected GeneratedUiApi createApi() {
        final GeneratedUiApi created = new GeneratedUiApi(this, packageName, className);
        globalDefs.forBoth(created::addLocalDefinition);
        return created;
    }

    public GeneratedUiApi getApi() {
        return api.out1();
    }

    public GeneratedUiBase getBase() {
        return base.out1();
    }
    public ClassBuffer getBaseClass() {
        return getBase().getSource().getClassBuffer();
    }

    public GeneratedUiFactory getFactory() {
        return factory.out1();
    }

    public boolean hasFactory() {
        return isUiComponent();
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

    public GeneratedUiMember getPublicField(String name) {
        name = name.replaceFirst("(is|get|set|has|add|remove|clear)([A-Z][a-zA-Z0-9]*)", "$2");
        name = X_String.firstCharToLowercase(name);
        return getPublicModel().getField(name);
    }

    public GeneratedUiModel getPrivateModel() {
        return getBase().model.out1();
    }

    public MappedIterable<GeneratedUiImplementation> getImpls() {
        return impls.forEachValue()
                    .map(Lazy::out1);
    }

    public UiContainerExpr getAst() {
        return ast.out1();
    }

    public boolean addImplementationFactory(
            AbstractUiImplementationGenerator<?> generator,
            Class<?> platform,
            In1Out1<GeneratedUiComponent, GeneratedUiImplementation> io) {
        if (impls.containsKey(platform)) {
            return false;
        }
        io = io.spyOut(impl-> {
            impl.setGenerator(generator);
            globalDefs.forBoth(impl::addLocalDefinition);
        });
        final Lazy<GeneratedUiImplementation> lazy = Lazy.deferSupplier(io, this);
        final Lazy<GeneratedUiImplementation> result = impls.put(platform, lazy);
        if (result != null) {
            assert checkSafeOverride(platform, result, lazy);
            return false;
        }
        return true;
    }

    public GeneratedUiComponent addSuperInterface(GeneratedUiSupertype superType) {
        superInterfaces.put(superType.getQualifiedName(), superType);
        return this;
    }
    public GeneratedUiComponent updateSupertype(GeneratedUiSupertype superType) {
        return updateSupertype(In1Out1.of(superType));
    }
    public GeneratedUiComponent updateSupertype(In1Out1<GeneratedUiSupertype, GeneratedUiSupertype> factory) {
        superType = factory.io(superType);
        return this;
    }

    protected boolean checkSafeOverride(
        Class<?> platform, Lazy<GeneratedUiImplementation> result,
        Lazy<GeneratedUiImplementation> io
    ) {
        final GeneratedUiImplementation one = result.out1();
        final GeneratedUiImplementation two = io.out1();
        assert one == two : "Attempting to override a platform factory; " +
            "[[  " + one + "   ]] != [[   " + two + "   ]] for " + platform.getName();
        return true;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public void resolveGenerics(UiGeneratorService<?> gen) {
        UiGeneratorTools<?> tools = gen.tools();
        final UiNamespace ns = tools.namespace();
        // Print API generics.
        final ChainBuilder<GeneratedTypeParameter> publicTypes = Chain.startChain();
        if (superType != null) {
            final GeneratedUiBase baseLayer = base.out1();
            computeSuperDefinition(superType, baseLayer);
        }
        if (superInterfaces.isNotEmpty()) {
            final GeneratedUiApi apiLayer = api.out1();
            superInterfaces.forEachValue()
                           .forAll(this::computeSuperDefinition, apiLayer);

        }
        if (api.isResolved()) {
            final GeneratedUiApi apiLayer = api.out1();
            final SourceBuilder<GeneratedJavaFile> out = apiLayer.getSource();
            MappedIterable<GeneratedTypeParameter> apiGenerics = apiLayer.getTypeParameters();

            if (apiGenerics.isNotEmpty()) {
                for (GeneratedTypeParameter generic : apiGenerics) {
                    // This type has been made public, so we need to require it from subtypes,
                    // and push it into supertypes.
                    if (!apiLayer.hasLocalDefinition(generic.getSystemName())) {
                        final String computedDeclaration = generic.computeDeclaration(apiLayer, ImplLayer.Api, gen, ns, out);
                        apiLayer.getSource().getClassBuffer()
                            .addGenerics(computedDeclaration);
                        publicTypes.add(generic);
                    }
                }
            }
        }
        if (base.isResolved()) {
            final GeneratedUiBase baseLayer = base.out1();
            // our actual class name is WrappedName, and we implement the api type name
            final MappedIterable<GeneratedTypeParameter> baseGenerics = baseLayer.getTypeParameters();
            if (!baseGenerics.isEmpty()) {

                final SourceBuilder<GeneratedJavaFile> out = baseLayer.getSource();
                for (GeneratedTypeParameter generic : baseGenerics) {
                    if (!baseLayer.hasLocalDefinition(generic.getSystemName())) {
                        String baseName = generic.computeDeclaration(baseLayer, ImplLayer.Base, gen, ns, out);
                        out.getClassBuffer().addGenerics(baseName);
                    }
                }

            }
        }
    }

    private void computeSuperDefinition(GeneratedUiSupertype superType, GeneratedUiLayer layer) {
        String superName = superType.getName(layer);
        StringBuilder superDef = new StringBuilder(superName);
        int initialLen = superDef.length();
        final SizedIterable<GeneratedTypeParameter> params = superType.getParams();
        final int numParams = params.size();
        params
            .forAll(param-> {
                // update base type with our generic bounds
                String name;
                if (layer.hasLocalDefinition(param.getSystemName())) {
                    name = layer.getLocalDefinition(param.getSystemName()).toSource();
                } else {
                    name = layer.getGenericValue(param);
                }

                if (superDef.length() == initialLen) {
                    superDef.append("<");
                } else {
                    superDef.append(",");
                }

                if (numParams > 2) {
                    superDef.append("\n    ");
                } else if (superDef.length() > initialLen){
                    superDef.append(" ");
                }

                superDef.append(name);
//                layer.addLocalDefinition(param.getSystemName(), param.getReferenceType());
            }
        );
        if (superDef.length() != initialLen) {
            if (numParams > 2) {
                superDef.append("\n  ");
            }
            superDef.append(">");
        }
        if (layer.isInterface()) {
            layer.getSource().addInterfaces(superDef.toString());
            // assert no constructors...
            assert superType.getRequiredConstructors().isEmpty() : "Interface APIs cannot require constructors (found on " + superType + " of " + layer;
        } else {
            layer.getSource().setSuperClass(superDef.toString());
            final SizedIterable<Parameter[]> ctors = superType.getRequiredConstructors();
            for (Parameter[] parameters : ctors) {
                // For each set of constructors, create a new constructor matching super
                final ClassBuffer out = layer.getSource().getClassBuffer();
                final MethodBuffer ctor = out.createConstructor(X_Modifier.PUBLIC);
                if (parameters.length > 0) {
                    // for each parameter, add a parameter, as well as the name in the super call.
                    String next = "";
                    ctor.print("super(");
                    for (Parameter parameter : parameters) {
                        // TODO: resolve variables in AST names
                        final String name = parameter.getId().getName();
                        ctor.addParameter(parameter.getType().toSource(), name);
                        ctor.print(next).print(name);
                        next = ", ";
                    }
                    ctor.println(");");
                }

            }

        }
    }

    public void beforeSave(In1<UiGeneratorService<?>> callback) {
        beforeSave.add(callback);
    }

    public void saveSource(UiGeneratorService<?> gen) {
        beforeSave.removeAll(In1::in,  gen);
        // Write api
        final UiGeneratorTools tools = gen.tools();
        Do commit = Do.NOTHING;
        if (api.isResolved() || base.isResolved()) {
            // sending null generics because we already handled them
            commit = ()->saveType(api.out1(), gen, tools, null);
        }
        MappedIterable<GeneratedTypeParameter> baseGenerics = null;
        if (base.isResolved()) {
            final GeneratedUiBase baseLayer = base.out1();
            final GeneratedUiApi apiLayer = api.out1();
            // our actual class name is WrappedName, and we implement the api type name
            final ClassBuffer out = baseLayer.getSource().getClassBuffer();
            baseGenerics = baseLayer.getTypeParameters();
            String apiName = baseLayer.getApiName();
            final MappedIterable<GeneratedTypeParameter> apiParams = apiLayer.getTypeParameters();
            if (apiParams.isNotEmpty()) {
                boolean first = true;
                // TODO get namespace from somewhere better...
                final UiNamespace ns = gen.tools().namespace();
                for (GeneratedTypeParameter generic : apiParams) {
                    if (baseLayer.hasLocalDefinition(generic.getSystemName())) {
                        continue;
                    }
                    if (first) {
                        first = false;
                        apiName += "<";
                    } else {
                        apiName += ", ";
                    }
                    String name = generic.getSystemName();
                    if (genericInfo.hasTypeParameter(ImplLayer.Base, name)) {
                        // Add the generic to our implements clause; we've already declared this type
                        apiName += genericInfo.getLayerName(ImplLayer.Base, name);
                    } else {
                        // The api has a type which we lack.  Pass it along?
                        String computed = generic.computeDeclaration(baseLayer, ImplLayer.Base, gen, ns, out);
                        out.addGenerics(computed);
                    }
                }
                if (!first) {
                    apiName += ">";
                }
            }
            if (api.isResolved() && api.out1().shouldSaveType()) {
                out.addInterfaces(apiName);
            }
            commit = commit.doAfter(()->saveType(baseLayer, gen, tools, null));
        }

        final GeneratedUiFactory builder = factory.out1();
        // let the impls have a peek at our builder
        getImpls().forAll(GeneratedUiImplementation::finalizeBuilder, builder);

        // Next up... generated builders.
        commit = commit.doAfter(()->saveType(builder, gen, tools, null));


        // write impls
        getImpls()
            .filter(GeneratedJavaFile::shouldSaveType)
            .forAll(this::saveType, gen, tools, baseGenerics);

        // write api, base and builders
        commit.done();

        // Save any extra layers... We do this last so other "mainstream" generation can create standalone Extras,
        // knowing they will be saved even if defined in an impl or builder type
        extraLayers.forEachValue()
                   .filter(GeneratedJavaFile::shouldSaveType)
                   .forAll(layer->gen.overwriteSource(layer.getPackageName(), layer.getWrappedName(), layer.toSource(), null));

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
                        String value = generic.computeDeclaration(ui, ui.getLayer(), gen, ns, out);
                        rawSuper += value;
                    }
                    rawSuper += ">";
                    out.getClassBuffer().setSuperClass(rawSuper.replace("<>", ""));
                }
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

    public Maybe<GeneratedUiMember> getModelField(String name) {
        if (hasPublicModel()) {
            return Maybe.nullable(getPublicModel().getField(name));
        }
        return Maybe.not();
    }

    public GeneratedTypeParameter addGeneric(String genericName, TypeParameter type) {
        final GeneratedTypeParameter param = genericInfo.setLayerName(genericName, ImplLayer.Api, type.getName());
        param.absorb(type);
        return param;
    }

    @Override
    public String toString() {
        return "GeneratedUiComponent{" +
            "api=" + (api.isResolved() ? api.out1() : null) +
            ", base=" + (base.isResolved() ? base.out1() : null) +
            ", impls=" + impls +
            ", packageName='" + packageName + '\'' +
            ", className='" + className + '\'' +
            ", refs=" + refs +
            '}';
    }

    public void requireCoercion(GeneratedUiLayer layer, GeneratedUiMember field) {
        // create a coerce method which handles the type of this field.
        // This method must accept an instance of this field, and return a serialized string
        layer.addCoercion(field);
    }

    public GeneratedFactory createNativeFactory(
        UiAssembler assembler,
        AssembledElement el,
        UiContainerExpr n,
        UiNamespace namespace,
        String refName
    ) {
        // Create an abstract method for the native element we want to create.
        final AssembledUi assembly = assembler.getAssembly();
        final ApiGeneratorContext ctx = assembly.getContext();
        final GeneratedUiComponent component = assembly.getUi();
        Do release = assembly.getGenerator().resolveSpecialNames(ctx, component, component.getBase(), el.maybeRequireRefRoot(), el.maybeRequireRef());

        final String builder = getBase().getElementBuilderType(namespace);
        final ClassBuffer out = getBase().getSource().getClassBuffer();
        final String creator = "create" + X_String.toTitleCase(refName);
        // Now, add a per-implementation override.
        final Type type = new ClassOrInterfaceType(namespace.getElementType(out));
        final GeneratedUiMethod method = new GeneratedUiMethod(type, creator, (imp, ui)->
            imp.getElementBuilderType(namespace)
        );
        final MethodBuffer creatorMethod = out.createMethod("protected abstract " + builder + " " + creator);

        for (GeneratedUiImplementation impl : getImpls()) {
            // The impl must have provided the generic, so we must prefer whatever
            // name was used in the concrete subtype
            final boolean was = ctx.setIgnoreChanges(true);
            Do rescope = assembly.getGenerator().resolveSpecialNames(ctx, component, impl, el.maybeRequireRefRoot(), el.maybeRequireRef());
            final Maybe<UiAttrExpr> attr = n.getAttribute(impl.getAttrKey());
            impl.addNativeMethod(assembler, namespace, method, el, attr
                .mapNullSafe(a->(UiContainerExpr)a.getExpression())
                .ifAbsentReturn(n));
            rescope.done();
            ctx.setIgnoreChanges(was);
        }

        final LazyInitFactory nativeFactory = new LazyInitFactory(creatorMethod, builder, refName, false);
//        nativeFactory.setReturn(creator + "()", false);

        release.done();
        return nativeFactory;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        if (this.tagName == null && tagName != null) {
            api.out1().getSource().getClassBuffer()
                .createField(String.class, UiNamespace.VAR_TAG_NAME)
                .setInitializer("\"" + tagName + "\"");
        }
        this.tagName = tagName;
    }

    public GeneratedUiGenericInfo getGenericInfo() {
        return genericInfo;
    }

    public void addGlobalDefinition(String systemName, ReferenceType referenceType) {
        globalDefs.put(systemName, referenceType);
        if (api.isResolved()) {
            api.out1().addLocalDefinition(systemName, referenceType);
        }
        if (base.isResolved()) {
            base.out1().addLocalDefinition(systemName, referenceType);
        }
        getImpls().forAll(GeneratedUiImplementation::addLocalDefinition, systemName, referenceType);

    }

    public GeneratedUiMethod createFactoryMethod(
        UiGeneratorTools tools,
        ApiGeneratorContext ctx,
        UiNamespace namespace,
        GeneratedUiFactory builder
    ) {


        String myBaseName = getApi().getWrappedName();
        final String name = "create" + myBaseName;
        myBaseName = getApi().getQualifiedName();
        final Type returnType = tools
            .methods()
            .$type(tools, ctx, stringLiteral(myBaseName))
            .getType();

        final In2Out1<GeneratedUiLayer, UiContainerExpr, String> io =
            (layer, ui)-> returnType.toSource();

        final GeneratedUiMethod method = new GeneratedUiMethod(returnType, name, io);

        return method;
    }

    public final GeneratedJavaFile getOrCreateExtraLayer(String id, String cls) {
        return getOrCreateExtraLayer(id, getPackageName(), cls);
    }

    public GeneratedJavaFile getOrCreateExtraLayer(String id, String pkg, String cls) {
        return extraLayers.getOrCreate(id, ignored->new GeneratedJavaFile(this, pkg, cls));
    }

    public GeneratedJavaFile getOrCreateExtraLayer(String id, String cls, In3Out1<GeneratedUiComponent, String, String, GeneratedJavaFile> factory) {
        return extraLayers.getOrCreate(id, factory.supply1(this).supply1(getPackageName()).supply(cls).ignoreIn1());
    }

    public SizedIterable<UserDefinedMethod> findUserMethod(String name) {
        final SizedIterable<UserDefinedMethod> apiMethods;
        if (api.isResolved()) {
            apiMethods = api.out1().findMethod(name);
            if (base.isResolved()) {
                final SizedIterable<UserDefinedMethod> baseMethods = base.out1().findMethod(name);
                return apiMethods.plus(baseMethods);
            } else {
                return apiMethods;
            }
        } else if (base.isResolved()) {
            return base.out1().findMethod(name);
        } else {
            return EmptyIterator.none();
        }
    }

    public void setUiComponent(boolean uiComponent) {
        this.uiComponent = uiComponent;
    }

    public boolean isUiComponent() {
        return uiComponent;
    }
}
