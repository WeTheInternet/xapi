package xapi.dev.ui.api;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.*;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.assembler.AssembledElement;
import xapi.dev.ui.tags.assembler.AssembledUi;
import xapi.dev.ui.tags.assembler.UiAssembler;
import xapi.dev.ui.tags.factories.GeneratedFactory;
import xapi.dev.ui.tags.factories.LazyInitFactory;
import xapi.fu.*;
import xapi.fu.itr.*;
import xapi.log.X_Log;
import xapi.source.util.X_Modifier;
import xapi.string.X_String;

import static com.github.javaparser.ast.expr.StringLiteralExpr.stringLiteral;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/6/17.
 */
public class GeneratedUiComponent extends GeneratedTypeOwnerBase <GeneratedUiApi, GeneratedUiBase, GeneratedUiImplementation> {

    private final Lazy<GeneratedUiFactory> factory;
    private final StringTo<FieldBuffer> refs;
    private final StringTo<GeneratedUiSupertype> superInterfaces;
    private final ReservedUiMethods methods;
    private final ChainBuilder<In1<UiGeneratorService<?>>> beforeSave;
    private String tagName;
    private GeneratedUiSupertype superType;
    private boolean uiComponent;

    public GeneratedUiComponent(String pkg, String cls, Out1<UiContainerExpr> ast) {
        super(pkg, cls, ast, new GeneratedUiGenericInfo());
        factory = Lazy.deferred1(this::createFactory);
        refs = X_Collect.newStringMapInsertionOrdered(FieldBuffer.class);
        superInterfaces =  X_Collect.newStringMap(GeneratedUiSupertype.class);
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

    protected GeneratedUiFactory createFactory() {
        final GeneratedUiFactory created = new GeneratedUiFactory(this, getApi());
        globalDefs.forBoth(created::addLocalDefinition);
        return created;
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

    public void resolveGenerics(UiGeneratorService<?> gen) {
        UiGeneratorTools<?> tools = gen.tools();
        final UiNamespace ns = tools.namespace();
        // Print API generics.
        final ChainBuilder<GeneratedTypeParameter> publicTypes = Chain.startChain();
        if (superType != null) {
            final GeneratedUiBase baseLayer = getBase();
            computeSuperDefinition(superType, baseLayer);
        }
        if (superInterfaces.isNotEmpty()) {
            final GeneratedUiApi apiLayer = getApi();
            superInterfaces.forEachValue()
                           .forAll(this::computeSuperDefinition, apiLayer);

        }
        if (isApiResolved()) {
            final GeneratedUiApi apiLayer = getApi();
            final SourceBuilder<GeneratedJavaFile> out = apiLayer.getSource();
            MappedIterable<GeneratedTypeParameter> apiGenerics = apiLayer.getTypeParameters();

            if (apiGenerics.isNotEmpty()) {
                for (GeneratedTypeParameter generic : apiGenerics) {
                    // This type has been made public, so we need to require it from subtypes,
                    // and push it into supertypes.
                    if (!apiLayer.hasLocalDefinition(generic.getSystemName())) {
                        final String computedDeclaration = computeDeclaration(generic, apiLayer, SourceLayer.Api, gen, ns, out);
                        apiLayer.getSource().getClassBuffer()
                            .addGenerics(computedDeclaration);
                        publicTypes.add(generic);
                    }
                }
            }
        }
        if (isBaseResolved()) {
            final GeneratedUiBase baseLayer = getBase();
            // our actual class name is WrappedName, and we implement the api type name
            final MappedIterable<GeneratedTypeParameter> baseGenerics = baseLayer.getTypeParameters();
            if (!baseGenerics.isEmpty()) {

                final SourceBuilder<GeneratedJavaFile> out = baseLayer.getSource();
                for (GeneratedTypeParameter generic : baseGenerics) {
                    if (!baseLayer.hasLocalDefinition(generic.getSystemName())) {
                        String baseName = computeDeclaration(generic, baseLayer, SourceLayer.Base, gen, ns, out);
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

    @Override
    public void saveSource(UiGeneratorService<?> gen) {
        beforeSave.removeAll(In1::in,  gen);
        // Write api
        final UiGeneratorTools tools = gen.tools();
        Do commit = Do.NOTHING;
        if (isApiResolved() || isBaseResolved()) {
            // sending null generics because we already handled them
            commit = ()->saveType(getApi(), gen, tools, null);
        }
        MappedIterable<GeneratedTypeParameter> baseGenerics = null;
        if (isBaseResolved()) {
            final GeneratedUiBase baseLayer = getBase();
            final GeneratedUiApi apiLayer = getApi();
            // our actual class name is WrappedName, and we implement the api type name
            final SourceBuilder<GeneratedJavaFile> src = baseLayer.getSource();
            final ClassBuffer out = src.getClassBuffer();
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
                    if (getGenericInfo().hasTypeParameter(SourceLayer.Base, name)) {
                        // Add the generic to our implements clause; we've already declared this type
                        apiName += getGenericInfo().getLayerName(SourceLayer.Base, name);
                    } else {
                        // The api has a type which we lack.  Pass it along?
                        String computed = computeDeclaration(generic, baseLayer, SourceLayer.Base, gen, ns, src);
                        out.addGenerics(computed);
                    }
                }
                if (!first) {
                    apiName += ">";
                }
            }
            if (isApiResolved() && getApi().shouldSaveType()) {
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

        super.saveSource(gen);

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

    @Override
    public String toString() {
        return "GeneratedUiComponent{" +
            "api=" + (isApiResolved() ? getApi() : null) +
            ", base=" + (isBaseResolved() ? getBase() : null) +
            ", impls=" + getImpls() +
            ", packageName='" + getPackageName() + '\'' +
            ", typeName='" + getTypeName() + '\'' +
            ", tagName='" + getTagName() + '\'' +
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
        if (tagName == null) {
            X_Log.warn(GeneratedUiComponent.class, "No tagName set for ", getTypeName());
            return getTypeName();
        }
        return tagName;
    }

    public void setTagName(String tagName) {
        if (this.tagName == null && tagName != null) {
            getApi().getSource().getClassBuffer()
                .createField(String.class, UiNamespace.VAR_TAG_NAME)
                .setInitializer("\"" + tagName + "\"");
        }
        this.tagName = tagName;
    }

    public void addGlobalDefinition(String systemName, ReferenceType referenceType) {
        globalDefs.put(systemName, referenceType);
        // this is kinda shady...  should make globalDefs Allable, w.r.t "when Lazy is resolved"...
        if (isApiResolved()) {
            getApi().addLocalDefinition(systemName, referenceType);
        }
        if (isBaseResolved()) {
            getBase().addLocalDefinition(systemName, referenceType);
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

    public SizedIterable<UserDefinedMethod> findUserMethod(String name) {
        final SizedIterable<UserDefinedMethod> apiMethods;
        if (isApiResolved()) {
            apiMethods = getApi().findMethod(name);
            if (isBaseResolved()) {
                final SizedIterable<UserDefinedMethod> baseMethods = getBase().findMethod(name);
                return apiMethods.plus(baseMethods);
            } else {
                return apiMethods;
            }
        } else if (isBaseResolved()) {
            return getBase().findMethod(name);
        } else {
            return EmptyIterator.none();
        }
    }

    public void setUiComponent(boolean uiComponent) {
        this.uiComponent = uiComponent;
    }

    @Override
    public boolean isUiComponent() {
        return uiComponent;
    }

    @Override
    protected GeneratedUiApi makeApi() {
        return new GeneratedUiApi(this, getPackageName(), getTypeName());
    }

    @Override
    protected GeneratedUiBase makeBase() {
        return new GeneratedUiBase(this, getApi());
    }

}
