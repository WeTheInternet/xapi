package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.ui.api.*;
import xapi.dev.ui.tags.factories.GeneratedFactory;
import xapi.except.NotYetImplemented;
import xapi.fu.Do;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.log.X_Log;
import xapi.source.X_Modifier;
import xapi.source.X_Source;
import xapi.util.X_String;

import static com.github.javaparser.ast.expr.AnnotationExpr.NULLABLE;
import static xapi.dev.ui.api.UiNamespace.*;
import static xapi.source.X_Modifier.ABSTRACT;
import static xapi.source.X_Modifier.PROTECTED;
import static xapi.util.X_String.toTitleCase;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/8/18 @ 2:27 AM.
 */
public abstract class ModelBindingAssembler implements TagAssembler {

    protected Lazy<GeneratedUiDefinition> def = Lazy.deferred1(this::definition);

    protected abstract AssembledUi getAssembly();

    protected abstract GeneratedUiDefinition definition();

    @Override
    public UiAssemblerResult visit(
        UiAssembler assembler, GeneratedFactory factory, AssembledElement e
    ) {
        GeneratedUiDefinition def = this.def.out1();
        def.init(assembler, factory, e);
        final AssembledUi assembly = getAssembly();

        String paramName = X_String.firstCharToLowercase(def.getTypeName());
        String bindMethod = "bind" + def.getTypeName();
        String editTextBuilder = "build" + def.getTypeName();


        final UiAssemblerResult result = new UiAssemblerResult();
        final UiContainerExpr ast = e.getAst();

        final GeneratedUiComponent ui = assembly.getUi();
        final UiNamespace ns = assembly.getNamespace();
        final ClassBuffer cb = ui.getBase().getSource().getClassBuffer();
        final Maybe<UiAttrExpr> model = ast.getAttribute("model");
        final ChainBuilder<ModelBinding> modelFields = Chain.startChain();
        def.getModelFields().forBoth((k,v)->{
            assert model.isAbsent() : "Cannot have both model= and " + k + "= "
                        + "\nin " + e.debugNode(ast)+"";
            // create a ModelBinding
            final Maybe<UiAttrExpr> attr = ast.getAttribute(k);
            final Maybe<UiAttrExpr> getter = ast.getAttribute(v.getterName());
            final Maybe<UiAttrExpr> setter = ast.getAttribute(v.setterName());
            if (attr.isPresent() || getter.isPresent() || setter.isPresent()) {
                ModelBinding binding = new ModelBinding(k, v);
                if (attr.isPresent()) {
                    assert getter.isAbsent() : "Do not bind both " + k + "= and " + v.getterName() +"="
                        + "\nin " + e.debugNode(ast)+"";
                    assert setter.isAbsent() : "Do not bind both " + k + "= and " + v.setterName() +"="
                        + "\nin " + e.debugNode(ast);
                    binding.withGetter(attr.get(), true);
                    binding.withSetter(attr.get(), true);
                } else {
                    if (getter.isPresent()) {
                        binding.withGetter(attr.get(), false);
                    }
                    if (setter.isPresent()) {
                        binding.withSetter(attr.get(), false);
                    }
                }
                modelFields.add(binding);
            }
        });

        final PrintBuffer out = e.getInitBuffer();
        factory.getVar().clear();

        // TODO: refactor this so we don't have to clear the buffer... (this may cause bugs later)
        out.clear();

        final String elType = ui.getBase().getElementType(ns);
        String buildType = def.getBuilderName() +
            "<" +
            elType + ", " +
            def.getApiName() + "<" + elType + ">" +
            ">";

        final PrintBuffer ret = factory.getReturnStmt();
        ret.clear();


        if (modelFields.isEmpty() && model.isAbsent()) {
            // This element is not configured at all; no binding is possible,
            // so just create a blank element and carry on.
            ret.pattern("$1($2, $3())", bindMethod, factory.getFieldName(), editTextBuilder);
        } else {
            // There is some model bindings to consider.
            /*Manual prototype:
    @SuppressWarnings("unchecked")
    final BuildEditTextComponent<El, ?> builder = GwtEditTextComponent.builder();
    builder.withModel(mod->{
      getModel().onChange("title", (was, is) -> {
        mod.setValue((String)is);
      });
      mod.onChange("value", (was, is) -> {
        DomGlobal.console.log("Changed value", was, is);
        getModel().setTitle((String)is);
      });
    });
    return bindTitle(title, builder);
            */
            out
                .println("@SuppressWarnings(\"unchecked\")")
                .patternln("final $1 builder = $2();", buildType, editTextBuilder);

            if (model.isPresent()) {
                // when a model is present, none of the others should be present...
                // set the model value onto the opts.  In the future, we can look at the
                // actual ast presented, and do something smarter regarding model binding.
                out.patternln("builder.getOpts().setModel($1);",
                    serializeWholeModel(assembler, model.get())
                );
            } else {
                out.println("builder.withModel(mod->{")
                    .indent();

                // consider each model field independently
                modelFields.forEach(binding -> {
                    bindModelField(e, binding.getType(), binding.getName(), binding.getSetter(), binding.getGetter());
                });

                out.outdent()
                    .println("});");
            }

            ret.pattern("$1($2, builder)", bindMethod, factory.getFieldName());
        }


        result.setDefaultBehavior(Do.NOTHING);

        // only build the scaffolding for this foreign type once, so if you have multiple instances
        // of the same child type, you do not re-print the protected implementation methods.
        if (ui.getBase().hasMethod(editTextBuilder)) {
            return result;
        }
        ui.getBase().reserveMethodName(editTextBuilder);

        cb.createMethod(PROTECTED | ABSTRACT, buildType, editTextBuilder);

        final String builderType = assembly.getTypeBuilder();
        cb
            .createMethod(PROTECTED | X_Modifier.ABSTRACT, builderType, bindMethod)
            .addParameter(Lazy.class, paramName, builderType)
            .addParameter(buildType, "builder");

        for (GeneratedUiImplementation impl : ui.getImpls()) {
            // the impl type of the class we are adding as a child
            final UiNamespace implNs = impl.reduceNamespace(ns);
            final ClassBuffer implCb = impl.getSource().getClassBuffer();
            final String implType = implCb.addImport(impl.mangleName(def.getPackageName(), def.getApiName()));
            final String implEl = implNs.getElementType(implCb);
            final String implApi =
                implCb.addImport(X_Source.qualifiedName(def.getPackageName(), def.getApiName()))
                    // TODO: not this hideous hack...  type parameters should be represented in the `GeneratedUiDefinition def`
                    + "<" + implEl + ">";
            final String implModel = implCb.addImport(X_Source.qualifiedName(def.getPackageName(), def.getModelName()));
            final String implBuilder = impl.getElementBuilderType(implNs);

            final String implComponentBuilder = implCb.parameterizedType(
                def.getBuilderName(), implEl, implApi);

            // implement the abstract method to create the foreign child's builder.
            implCb
                .createMethod(PROTECTED, implComponentBuilder, editTextBuilder)
                .returnPattern("$1.$2()", implType, METHOD_BUILDER);

            final MethodBuffer implMethod = implCb
                .createMethod(PROTECTED, implBuilder, bindMethod)
                .addParameter(Lazy.class, paramName, implBuilder)
                .addParameter(implComponentBuilder, "builder");

            implMethod.returnPattern("bindModel(builder, $1.$2, $3::$4, $5::new, $5::$6)",
                def.getApiName(), VAR_TAG_NAME, implModel, METHOD_NEW_MODEL, implType, METHOD_AS_BUILDER);

            // Lets also ensure that when the main element is assembled into the document,
            // we include this child type as well... the assemble methods are made idempotent,
            // so it should be okay to be re-entrant here.
            impl.registerCallbackWriter((imp, mth)->
                mth.patternln("$1.assemble(assembler);", implType)
            );
        }
        return result;
    }

    protected String serializeWholeModel(UiAssembler assembler, UiAttrExpr model) {
        final ComposableXapiVisitor<Object> visitor = ComposableXapiVisitor.whenMissingFail(ModelBindingAssembler.class);
        model.getExpression().accept(visitor, null);
        String lazy = getAssembly().getTools().resolveString(getAssembly().getContext(), model.getExpression());
        return lazy;
    }

    protected void bindModelField(
        AssembledElement e,
        String type,
        String targetName,
        UiAttrExpr setter,
        UiAttrExpr getter
    ) {
        assert getter == setter : "Disparate getter and setter for " + targetName + " not yet supported;" +
            " you sent " + e.debug();
        final PrintBuffer out = e.getInitBuffer();
        final ComposableXapiVisitor<Object> visitor = ComposableXapiVisitor.whenMissingFail(ModelBindingAssembler.class);
        visitor
            .withStringLiteralTerminal((str, i)->{
                // any literal value should be copied over to the model directly.
                out.patternln("mod.set$1($2);", toTitleCase(targetName), serialized(e, str, type));
            })
            .withMethodReferenceTerminal((ref, i)->{
                // for now, this is all we support because this is all we need atm.
                switch (ref.getScope().toSource()) {
                    case "$this":
                    case "this":
                        // ok... we want to model field to a method on the component.
                        // We'll do our best to coerce arguments sanely...
                        if (type.contains("xapi.fu")) {
                            // The model field itself is a functional expression; force user to get bindings right for now.
                            // in the future, we should lookup the reference, and do coercion...
                            out.patternln("mod.set$1(this::$2);",
                                toTitleCase(targetName), ref.getIdentifier()
                            );
                        } else {
                            // TODO: look for getters/setters that we can map to...
                            throw new NotYetImplemented("Only xapi.fu functional expressions support $this:: method references at this time");
                        }
                        break;
                    case "$model":
                        // alright, bind model fields together.
                        out
                            .patternln("getModel().onChange(\"$1\", (was, is) ->", ref.getIdentifier())
                            .indent()
                                .patternln("mod.set$1(($2)is)", toTitleCase(targetName), type)
                            .outdent()
                            .println(");")

                            .patternln("mod.onChange(\"$1\", (was, is) ->", targetName)
                            .indent()
                                .patternln("getModel().set$1(($2)is)", toTitleCase(ref.getIdentifier()), type)
                            .outdent()
                            .println(");")
                        ;
                        break;
                    default:
                        throw new NotYetImplemented("Only $model::refs are supported at this time; you sent " + getAssembly().getTools().debugNode(ref));
                }
            })
        ;
        setter.getExpression().accept(visitor, null);

    }

    protected String serialized(AssembledElement e, Expression str, String type) {
        final Expression resolved = e.resolveRef(null, str, false);
        final AssembledUi ui = getAssembly();
        final String asString = ui.getTools().resolveString(ui.getContext(), resolved);
        if (str instanceof MethodCallExpr || str instanceof LambdaExpr) {
            // anything that looked  like a method call or lambda expression gets serialized directly...
            return asString;
        }
        switch (type) {
            case "java.lang.String":
            case "String":

                return X_Source.javaQuote(asString);

            case "java.lang.Boolean":
            case "java.lang.Byte":
            case "java.lang.Short":
            case "java.lang.Character":
            case "java.lang.Integer":
                // Long and Float get special treatment
            case "java.lang.Double":
                if (asString.contains("new ")) {
                    return asString;
                }
            case "boolean":
            case "byte":
            case "short":
            case "char":
            case "int":
                // long and float get special treatment
            case "double":
                return maybeCast(type, asString);
            case "java.lang.Long":
                if (asString.contains("new ")) {
                    return asString;
                }
            case "long":
                if (asString.chars().allMatch(Character::isDigit)) {
                    return asString + "L";
                }
                if (asString.trim().toUpperCase().endsWith("L")) {
                    return asString;
                }
                return maybeCast(type, asString);

            case "java.lang.Float":
                if (asString.contains("new ")) {
                    return asString;
                }
            case "float":
                if (asString.chars().allMatch(c->Character.isDigit(c) || c == '.')) {
                    return asString + "f";
                }
                if (asString.trim().toUpperCase().endsWith("F")) {
                    return asString;
                }
                return maybeCast(type, asString);

        }
        // give up...
        X_Log.warn(ModelBindingAssembler.class, "Unhandled type", type, "from",
            getAssembly().getTools().debugNode(str), "resolved to",
            getAssembly().getTools().debugNode(resolved));
        return asString;
    }

    private String maybeCast(String type, String asString) {
        String endName = X_String.lastChunk(type, '.');
        if (asString.contains("(")) { // anything with parens we will assume correctly provided type info
            // (if you put garbage code in, we're not going to spam casts that might confuse your error message).
            return asString;
        }
        return "(" + endName + ")" + asString;
    }

    public GeneratedUiMember addField(String type, String name) {
        return def.out1().addField(type, name);
    }

    public GeneratedUiMember addField(Type type, String name) {
        return def.out1().addField(type, name);
    }

    public GeneratedUiMember addFieldNullable(String type, String name) {
        final GeneratedUiMember field = def.out1().addField(type, name);
        field.addAnnotation(NULLABLE);
        return field;
    }

    public GeneratedUiMember addFieldNullable(Type type, String name) {
        final GeneratedUiMember field = def.out1().addField(type, name);
        field.addAnnotation(NULLABLE);
        return field;
    }

}

