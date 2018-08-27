package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.*;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.assembler.AssembledElement;
import xapi.dev.ui.tags.assembler.AssembledUi;
import xapi.fu.*;
import xapi.fu.iterate.CachingIterator;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.fu.iterate.SizedIterable;
import xapi.reflect.X_Reflect;
import xapi.source.X_Source;
import xapi.source.read.JavaModel.IsTypeDefinition;
import xapi.util.X_String;

import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.Map.Entry;

import static xapi.fu.Out2.out2Immutable;

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
                String titled = X_String.toTitleCase(param.out2());
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
        out.patternln("return $1", newBuilder.replace("()","(true)"))
            .indent()
            .patternln(".setTagName(\"$1\")", tagName);
        for (UiAttrExpr attr : n.getAttributes()) {
            resolveNativeAttr(assembly, attr, el, out);
        }
        // TODO: also handle bodies!
        if (n.getBody() != null) {
            for (Expression body : n.getBody().getChildren()) {
                // Bah... we need something smarter here...
                out.println(".append(" + X_Source.javaQuote(body.toSource()) + ")");
            }

        }
        out.outdent().println(";");
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
            (addQuotes ? X_Source.javaQuote(resolved) : resolved) +
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

}
