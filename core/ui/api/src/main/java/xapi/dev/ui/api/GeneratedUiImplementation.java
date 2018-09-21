package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.plugin.Transformer;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.LocalVariable;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.impl.AbstractUiImplementationGenerator;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.assembler.AssembledElement;
import xapi.dev.ui.tags.assembler.AssembledUi;
import xapi.except.NotYetImplemented;
import xapi.fu.*;
import xapi.fu.itr.CachingIterator;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.MappedIterable;
import xapi.reflect.X_Reflect;
import xapi.source.read.JavaModel.IsTypeDefinition;

import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.List;
import java.util.Map.Entry;

import static xapi.fu.Out2.out2Immutable;
import static xapi.source.X_Source.javaQuote;
import static xapi.util.X_String.toTitleCase;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiImplementation extends GeneratedUiLayer {

    public enum RequiredMethodType {
        NEW_BUILDER, CREATE_FROM_MODEL, CREATE_FROM_STYLE, CREATE, CREATE_NATIVE, CREATE_FROM_MODEL_AND_STYLE
    }

    protected final String apiName;
    protected final String implName;
    private final EnumMap<RequiredMethodType, Out2<GeneratedUiMethod, MethodCallExpr>> requiredMethods;
    private final StringTo<RequiredChildFactory> childFactories;
    private final ChainBuilder<In2<GeneratedUiImplementation, MethodBuffer>> callbackWriters;
    private AbstractUiImplementationGenerator<?> generator;

    public GeneratedUiImplementation(GeneratedUiComponent owner, String pkg) {
        super(owner.getApi(), pkg, owner.getApi().getTypeName(), ImplLayer.Impl, owner);
        apiName = owner.getApi().getWrappedName();
        implName = owner.getBase().getWrappedName();
        setSuffix("Component");
        requiredMethods = new EnumMap<>(RequiredMethodType.class);
        childFactories = X_Collect.newStringMap(RequiredChildFactory.class);
        callbackWriters = Chain.startChain();
    }

    public String getAttrKey() {
        return "fallback";
    }

    @Override
    protected SourceBuilder<GeneratedJavaFile> createSource() {
        final SourceBuilder<GeneratedJavaFile> source = super.createSource();
        source.setSuperClass(implName);
        return source;
    }

    public void commitOutput(UiGeneratorService<?> gen) {
        if (shouldSaveType()) {
            while (!requiredMethods.isEmpty()) {
                Iterable<Entry<RequiredMethodType, Out2<GeneratedUiMethod, MethodCallExpr>>> itr = requiredMethods.entrySet();
                final MappedIterable<Out3<RequiredMethodType, GeneratedUiMethod, MethodCallExpr>> todo = CachingIterator.cachingIterable(
                    itr.iterator())
                    .map(e -> Out3.out3(e.getKey(), e.getValue().out1(), e.getValue().out2()))
                    .cached();
                requiredMethods.clear();
                todo.forEach(e ->
                    addRequiredMethod(gen, e.out1(), e.out2(), e.out3())
                );
            }
            gen.overwriteSource(getPackageName(), getWrappedName(), getSource().toSource(), null);
        }
    }

    @SuppressWarnings("unchecked")
    protected void addRequiredMethod(
        UiGeneratorService<?> gen,
        RequiredMethodType key,
        GeneratedUiMethod method,
        MethodCallExpr call
    ) {
        final ClassBuffer buf = getSource().getClassBuffer();
        StringBuilder methodDef = new StringBuilder("public ");
        final ApiGeneratorContext<?> ctx = method.getContext();
        final String typeName;
        final Expression memberType = new TypeExpr(method.getMemberType());
        switch (key) {
            case CREATE:
            case CREATE_FROM_MODEL:
            case CREATE_FROM_MODEL_AND_STYLE:
            case CREATE_FROM_STYLE:
                typeName = gen.tools().resolveString(ctx, memberType);
                break;
            case CREATE_NATIVE:

            case NEW_BUILDER:
                // TODO remove manual newBuilder wiring and let us handle that here.
                return;
            default:
                assert false : "Unhandled required method " + key + " from " + call;
                return;
        }
        methodDef.append(typeName).append(" ");
        methodDef.append(call.getName());
        final MethodBuffer out = buf.createMethod(methodDef + "()");

        out.print("return new " + getWrappedName() + "(");

        Mutable<String> prefix = new Mutable<>("");
        method.getParams().forAll(i -> {
            out.addParameter(i.out1(), i.out2());
            out.print(prefix.replace(", ")).print(i.out2());
        });
        out.println(");");

        // now, we also need to make the constructor we just referenced.
        // if all calls to constructors are done via required CREATE_* methods,
        // then we can safely create the constructor now, and not make two.
        // If we want to create constructors elsewhere, we'll need to abstract
        // out the code below into a required method that is a constructor.
        final MappedIterable<String> paramsItr = method.getParams()
            .map2(Out2::join, " ");

        final String[] params = paramsItr.toArray(String[]::new);

        final MethodBuffer ctor = getSource().getClassBuffer()
            .createConstructor(Modifier.PUBLIC, params);

        // For all params, call a setter.
        // That is, ensure there is a field w/ getter and setter.
        method.getParams()
            .spy(param -> {
                // call setters for each field.
                String titled = toTitleCase(param.out2());
                ctor.println("set" + titled + "(" + param.out2() + ");");
            })
            .forAllMapped(
                this::ensureField, Out2::out1, Out2::out2
            );

    }

    protected IsTypeDefinition definition() {
        return IsTypeDefinition.newClass(getPackageName(), getWrappedName());
    }

    public UiNamespace reduceNamespace(UiNamespace from) {
        return from;
    }

    @Deprecated
    public boolean requireMethod(RequiredMethodType type, GeneratedUiMethod method, MethodCallExpr call) {
        final Out2<GeneratedUiMethod, MethodCallExpr> was = requiredMethods.put(type, out2Immutable(method, call));
        return was == null;
    }

    public int isSimilar(GeneratedUiImplementation impl) {
        if (impl.getClass() == getClass()) {
            return Integer.MAX_VALUE;
        }
        if (X_Reflect.mostDerived(this, impl) == impl) {
            // the other is a subclass of us...
            return 100000;
        }
        if (X_Reflect.mostDerived(impl, this) == this) {
            // we are a subclass of the other...
            return 1000;
        }
        if (X_Fu.equal(getSuffix(), impl.getSuffix())) {
            if (X_Fu.equal(getPrefix(), impl.getPrefix())) {
                return 1000;
            }
            return 100;
        } else if (X_Fu.equal(getPrefix(), impl.getPrefix())) {
            return 100;
        }
        return 0;
    }

    public void addNativeMethod(
        AssembledUi assembly,
        UiNamespace namespace,
        GeneratedUiMethod method,
        AssembledElement el,
        UiContainerExpr n
    ) {

        final MethodBuffer out = getSource().getClassBuffer()
            .createMethod(method.toSignature(this, n))
            .addAnnotation(Override.class);
        //        requireMethod(RequiredMethodType.CREATE, method);

        final String newBuilder = getOwner().getElementBuilderConstructor(namespace);
        String tagName = n.getName();
        final LocalVariable builder = out.newVariable(out.getReturnType().getQualifiedName(), "b");
        builder.setInitializer(newBuilder.replace("()","(true)"));
        out
            .println(builder.getName()).indent()
            .patternln(".setTagName(\"$1\")", tagName)
            .indent();
        for (UiAttrExpr attr : n.getAttributes()) {
            resolveNativeAttr(assembly, attr, el, out);
        }
        out.outdent().println(";");
        // TODO: also handle bodies!
        if (n.getBody() != null) {
            for (Expression body : n.getBody().getChildren()) {
                if (body instanceof MethodReferenceExpr) {
                    final MethodReferenceExpr ref = (MethodReferenceExpr) body;
                    if ("$model".equals(ref.getScope().toSource())) {
                        // Hokayyy!  We have a model field for a body...
                        // for now, we'll do only minimal introspection
                        addModelItem(assembly, out, builder, ref.getIdentifier());
                    } else {
                        throw new NotYetImplemented("Only $model:: fields are supported inside native tag bodies");
                    }
                } else {
                    // Bah... we need something smarter here...
                    final String src = body.toSource(new Transformer().setShouldQuote(false));
                    if (src.startsWith("$model::")) {
                        // supernasty... we should really be sending the template literal body through the parser again...
                        final String identifier = src.substring(8);
                        assert identifier.chars().allMatch(Character::isJavaIdentifierPart) : "Invalid $model:: reference " + src;
                        addModelItem(assembly, out, builder, identifier);
                    } else {
                        out.patternln("$1.append($2)", builder.getName(), javaQuote(src));
                    }
                }
            }

        }
        out.returnValue(builder.getName());
    }

    private void addModelItem(
        AssembledUi assembly,
        MethodBuffer out,
        LocalVariable builder,
        String identifier
    ) {
        final GeneratedUiMember modField = assembly.getUi().getPublicModel().getField(identifier);
        final String titleName = toTitleCase(identifier);
        final Type type = modField.getMemberType();
        final UiNamespace ns = reduceNamespace(assembly.getNamespace());
        boolean componentList = type.hasRawType("ComponentList");
        boolean listWrap = type.hasRawType("IntTo");
        if (componentList) {
            // setup all() listeners
            String injector = assembly.newInjector();
            out
                .patternln("$1().all(added -> ", identifier)
                .indent()
                    .patternln("$1.append(added.asBuilder())", builder.getName())
                .outdent()
                .println(", removed ->")
                .indent()
                    // going to need to use injector to handle removes...
                    .patternln("$1($2.getElement()).removeChild(removed.getElement())",
                        injector, builder.getName()
                    )
                .outdent()
                .println(");")
            ;
        } else if (listWrap) {
            // We need to iterate items.
            out.addImport(type.toSource());

            out.patternln("getModel().get$1().forAll(c -> ", titleName)
                .indent();
            if (type instanceof ClassOrInterfaceType) {
                final List<Type> typeArgs = ((ClassOrInterfaceType) type).getTypeArgs();
                if (typeArgs.size() == 1) {
                    if (typeArgs.get(0).hasRawType("IsComponent")) {
                        // we have component children!
                        out.patternln("(($1)$2).append(c.asBuilder())",
                            ns.getElementBuilderType(out).split("<")[0], builder.getName());
                    } // else if (isModelOfAComponentType(typeArgs[0]) { ... hook up builder here ... }
                    else {
                        out.patternln("$1.append(c);", builder.getName());
                    }
                    out.outdent().println(");");
                } else {
                    throw new IllegalArgumentException("IntTo $model::references must have type parameters, like model={key: IntTo.class.$generic(String.class) } " +
                        "\nYou sent " + type);
                }
            } else {
                throw new IllegalArgumentException("IntTo $model::references must have type parameters, like model={key: IntTo.class.$generic(String.class) } " +
                    "\nYou sent " + type);
            }
        } else {
            // It's a single item.
            if (type.hasRawType("IsComponent")) {
                out.patternln("(($1)$2).append(getModel().get$3().asBuilder())",
                    ns.getElementBuilderType(out).split("<")[0], builder.getName(), titleName);
            } else {
                out.patternln("$1.append(getModel().get$2());", builder.getName(), titleName);
            }
        }

    }

    protected void resolveNativeAttr(
        AssembledUi assembly,
        UiAttrExpr attr,
        AssembledElement el,
        MethodBuffer out
    ) {
        UiGeneratorTools tools = assembly.getTools();
        ApiGeneratorContext ctx = assembly.getContext();
        final Expression val = attr.getExpression();
        final Expression modelized = assembly.getGenerator().resolveReference(
            tools,
            ctx,
            assembly.getUi(),
            assembly.getUi().getBase(),
            el.maybeRequireRefRoot(),
            el.maybeRequireRef(),
            val,
            false
        );
        final String resolved = tools.resolveString(ctx, modelized);
        final String name = tools.resolveString(ctx, attr.getName());
        boolean addQuotes = modelized instanceof StringLiteralExpr || modelized instanceof TemplateLiteralExpr;
        out.println(".setAttribute(\"" + name + "\", " +
            (addQuotes ? javaQuote(resolved) : resolved) +
        ")");
    }

    public void addChildFactory(GeneratedUiDefinition definition, Expression sourceNode) {
        childFactories.put(definition.getTypeName(), new RequiredChildFactory(definition, sourceNode));
    }

    public SizedIterable<RequiredChildFactory> getRequiredChildren() {
        return childFactories.forEachValue();
    }

    public void finalizeBuilder(GeneratedUiFactory builder) {

    }

    public void writeCallbacks(MethodBuffer buffer) {
        callbackWriters.forAll(In2::in, this, buffer);
    }

    public void registerCallbackWriter(In2<GeneratedUiImplementation, MethodBuffer> callback) {
        assert callbackWriters.noneMatch(callback::equals);
        callbackWriters.add(callback);
    }

    public String mangleName(GeneratedUiDefinition def) {
        return generator.getImplName(def.getPackageName(), def.getApiName());
    }

    public String mangleName(String pkgName, String typeName) {
        return generator.getImplName(pkgName, typeName);
    }

    public void setGenerator(AbstractUiImplementationGenerator<?> generator) {
        this.generator = generator;
    }

    public AbstractUiImplementationGenerator<?> getGenerator() {
        return generator;
    }

    public void addCss(ContainerMetadata container, UiAttrExpr attr) {
        throw new NotYetImplemented(getClass() + " must implement addCss()");
    }
}
