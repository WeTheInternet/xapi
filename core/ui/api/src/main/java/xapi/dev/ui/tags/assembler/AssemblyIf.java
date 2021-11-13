package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.ComposableXapiVisitor;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.lang.gen.GeneratedUiMember;
import xapi.dev.source.*;
import xapi.dev.ui.api.*;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.dev.ui.tags.factories.GeneratedFactory;
import xapi.dev.ui.tags.factories.LazyInitFactory;
import xapi.dev.lang.gen.UserDefinedMethod;
import xapi.dev.debug.NameGen;
import xapi.except.NotYetImplemented;
import xapi.fu.*;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.MultiIterable;
import xapi.log.X_Log;
import xapi.source.util.X_Modifier;
import xapi.source.X_Source;
import xapi.source.write.Template;
import xapi.ui.api.component.ConditionalComponentMixin;

import java.util.List;

import static xapi.string.X_String.toTitleCase;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/26/18.
 */
public class AssemblyIf extends AssembledElement {

    public enum ConditionType {
        isNull("* == null"), whenNull("* == null"), // semantically equivalent
        isNotNull("* != null"), whenNotNull("* != null"), notNull("* != null"),
        isTrue("*"), isFalse("!(*)"),
        isOne("* == 1"), isZero("* == 0"), isMinusOne("* == -1"),
        isNotOne("* != 1"), isNotZero("* != 0"), isNotMinusOne("* != -1"),
        isEmpty("*.isEmpty()"), isNotEmpty("!*.isEmpty()"),

        ;
        private final Template template;
        ConditionType(String pattern) {
            template = new Template(pattern, "*");
        }

        public String format(String expr) {
            return template.apply(expr);
        }
    }
    public enum ConditionMode {
        And, Or;

        public static ConditionMode parse(AssembledElement el, Expression expr) {
            final AssembledUi ui = el.getSource();
            final String serialized = ui.getTools().resolveString(ui.getContext(), expr);
            switch (serialized.toLowerCase()) {
                case "&":
                case "&&":
                case "\"and\"":
                case "and":
                    return And;
                case "|":
                case "||":
                case "\"or\"":
                case "or":
                    return Or;
            }
            throw new IllegalArgumentException("Not a valid condition mode: " + serialized +" (from " + ui.getTools().debugNode(expr)+ ")");
        }

        public String toSource() {
            switch (this) {
                case And:
                    return " && ";
                case Or:
                    return " || ";
                default:
                    throw new UnsupportedOperationException();
            }
        }
    }
    public static class Conditional {
        private final ConditionMode mode;
        public ConditionType type;
        public ChainBuilder<Iterable<Expression>> expressions = Chain.startChain();

        public Conditional(ConditionType condition, ConditionMode mode, Expression ... ifs) {
            this(condition, mode, ArrayIterable.iterate(ifs));
        }

        public Conditional(
            ConditionType condition,
            ConditionMode mode,
            Iterable<Expression> ifs
        ) {
            this.type = condition;
            this.mode = mode;
            expressions.add(ifs);
        }

        public String print(AssemblyIf el, boolean first) {
            final SizedIterable<Expression> counted = expressions.flatten(In1Out1.identity()).counted();
            int size = counted.size();
            if (size == 0) {
                return "";
            }
            StringBuilder b = new StringBuilder();
            if (size > 1) {
                b.append("(");
            }
            for (Expression expression : counted) {
                final Expression resolved = el.resolveRef(el, expression, false);
                String serialized = el.serialize(resolved);
                if (!first) {
                    b.append(mode.toSource());
                }
                b.append(type.format(serialized));
            }
            if (size > 1) {
                b.append(")");
            }
            return b.toString();
        }

        @Override
        public String toString() {
            return "Conditional{" +
                "mode=" + mode +
                ", type=" + type +
                ", expressions=" + expressions.join(" ; ") +
                '}';
        }
    }

    private final ChainBuilder<Conditional> ifs;
    private final ChainBuilder<Iterable<Expression>> elses;
    private final ChainBuilder<Iterable<Expression>> whenTrue;
    /**
     * Expressions run whenever the if tag's conditionals are false.
     *
     * If present with elsifs,
     * the expressions in whenFalse are all run as soon as if fails,
     * then, the else/elsif are each responsible for returning a value.
     *
     * TODO: If present with else, we should use a document fragment container,
     * to append whenFalse / else / elsifs all in the order specified in the source.
     * A complex if tag would just lay out a standard if / else if / else branch,
     * and then interleave the whenFalse statements around the else / elsifs.
     *
     */
    private final ChainBuilder<Iterable<Expression>> whenFalse;
    private final ChainBuilder<AssemblyIf> elsifs;
    private ConditionMode mode = ConditionMode.And;
    private boolean simple;
    private boolean complex;
    private LazyInitFactory factory;
    private String selectedField;
    private String redrawName;
    private String attachTo;

    /**
     * If you want a no-parent version of this constructor, see {@link AssemblyRoot} (or subclass yourself)
     *
     * @param source - The ui assembly which owns this element
     * @param parent - The parent element, never null (see {@link AssemblyRoot#SUPER_ROOT}.
     * @param ast    - The {@code <dom />} ast for the current element. never null.
     */
    public AssemblyIf(AssembledUi source, AssembledElement parent, UiContainerExpr ast) {
        super(source, parent, ast);
        ifs = Chain.startChain();
        elses = Chain.startChain();
        elsifs = Chain.startChain();
        whenTrue = Chain.startChain();
        whenFalse = Chain.startChain();
        attachTo = parent.requireRef();
    }

    @Override
    public GeneratedFactory startElement(
        AssembledUi assembly,
        UiAssembler assembler,
        AssembledElement parent,
        UiContainerExpr el
    ) {

        // Create an Out1<ElBuilder> currentSelection; which is kept up-to-date
        // in "as automatic fashion as possible" (model change bindings),
        // along with a "big hammer" to forcibly recheck all conditional layout information.

        // special case to consider: simple if tags with only one result that is either on or off.
        // or, complex if tags all based on compile-time constants that resolve down to nothing...
        // for now, we'll do our best...

        final GeneratedUiComponent component = assembly.getUi();
        final GeneratedUiBase baseClass = component.getBase();
        String builder = baseClass.getElementBuilderType(assembly.getNamespace());
        final ClassBuffer out = assembly.getBaseClass();
        final UiContainerExpr ast = getAst();

        AssembledElement refTarget = this;
        if (!getSource().hasRef(this)) {
            // no ref field... LATER: try to deduce something nice like:
            // isNull=$model::thing -> isThingNull, etc.  ...seems like a waste of effort at this point;
            // if you want a human-friendly name on your if blocks, give them a ref.
            if (!parent.isRoot()) {
                refTarget = parent;
            }
        }
        factory = new LazyInitFactory(out, builder, requireRef(), true);

        // Create a selectedElement field, so we can manage state transitions appropriately.
        selectedField = baseClass.newFieldName(prefixName("selected"));
        out.createField(builder, selectedField, X_Modifier.PRIVATE);

        visitAst(ast);

        if (initBuffer == null) {
            initBuffer = factory.getInitBuffer();
        }
        return factory;
    }

    protected void visitAst(UiContainerExpr ast) {

        for (UiAttrExpr attr : ast.getAttributes()) {
            final Expression expr = resolveRef(this, attr);
//            final Expression expr = resolveRef(refTarget, attr);
            switch (attr.getNameString().toLowerCase()) {
                case "ref":
                    continue;
                case "mode":
                    mode = ConditionMode.parse(this, expr);
                    break;
                case "else":
                    addElse(expr);
                    break;
                case "elif":
                case "elsif":
                case "elseif":
                    elsif(expr);
                    break;
                case "whentrue":
                    whenTrue(expr);
                    break;
                case "whenfalse":
                    whenFalse(expr);
                    break;
                default:
                    try {
                        ConditionType type = ConditionType.valueOf(attr.getNameString());
                        addIf(type, expr);
                    } catch (IllegalArgumentException unsupported) {
                        throw new IllegalArgumentException("Unsupported <if /> attribute " + debugNode(attr));
                    }
            }
        }

        UiBodyExpr myBody = ast.getBody();
        if (myBody != null){
            whenTrue(myBody.getChildren());
        }
    }

    @Override
    public void finishElement(
        AssembledUi assembly,
        UiAssembler assembler,
        GeneratedFactory factory,
        UiAssemblerResult result,
        UiContainerExpr el
    ) {

        serialize(factory, assembler, result);

        super.finishElement(assembly, assembler, factory, result, el);

        result.setDefaultBehavior(null);
    }

    private void serialize(GeneratedFactory rootFactory, UiAssembler assembler, UiAssemblerResult result) {
        PrintBuffer toDom = rootFactory.getInitBuffer();

        redrawName = prefixName("redraw");
        writeIfBlock(assembler, toDom);

        final GeneratedFactory wt = serialize(assembler, rootFactory, whenTrue);

        toDom.println("return " + selectedField + " = " + wt.getGetter() +";");
        toDom.outdent().print("}");

        if (elsifs.isEmpty()) {
            // simple path... no nested elseif printing.
            if (elses.isEmpty() && whenFalse.isEmpty()) {
                // really simple... it's just an if and a whenTrue.
                simple = true;
                toDom.println()
                     .println("return " + selectedField + " = null;");
            } else {
                // there are else/whenFalse blocks, and we can safely
                // merge them into a single else block (no elsifs).
                toDom.println(" else {").indent();

                final GeneratedFactory wf = serialize(assembler, rootFactory, MultiIterable.concat(whenFalse, elses));
                toDom.println("return " + selectedField + " = " + wf.getGetter() +";");
                toDom.outdent().println("}");
            }
        } else {
            // elsif + whenFalse == nassssty.
            complex = whenFalse.isNotEmpty();
            if (complex) {
                if (elses.isNotEmpty()) {
                    throw new IllegalStateException("Cannot mix else and whenFalse; pick one or the other:\n\n" + assembler.getAssembly().getTools().debugNode(getAst()));
                }
                // since we promise to run whenFalse on all else blocks,
                // we need to create nested if/else blocks.
                toDom.println(" else {").indent();

                final GeneratedFactory wf = serialize(assembler, rootFactory, whenFalse);
                // not returning anything here...

                // now, serialize our elsifs.  First on does not get an `else` prefix, because we are already in
                // an else block for the whenFalse that we just ran.
                serializeElses(assembler, toDom, rootFactory, false);

                toDom.outdent().print("}");
                if (!hasElses()) {
                    // if there was no explicit else, then let whenFalse have a crack....
                    toDom.println(" else {").indent();
                    toDom.println("return " + selectedField + " = " + wf.getGetter() +";");
                    toDom.outdent().print("}");
                }
                toDom.println();

            } else {
                // not terrible, just elsifs and maybe an else
                serializeElses(assembler, toDom, rootFactory, true);
            }
        }
        final AssembledUi ui = assembler.getAssembly();
        final ClassBuffer baseClass = ui.getBaseClass();
        final GeneratedUiComponent component = ui.getUi();
        final GeneratedUiBase base = component.getBase();
        final String typeEl = ui.getTypeElement();
        final String typeBuilder = ui.getTypeBuilder();
        final String typeInjector = ui.getTypeInjector();
        final String newInjector = ui.newInjector();
        final MethodBuffer redraw = baseClass.createMethod("private void " + redrawName + "()");

        baseClass.addGenericInterface(ConditionalComponentMixin.class, typeEl, typeBuilder);

        redraw
            .addParameter(typeEl, "el")
            // renderConditional(el, selectedRootIf, rootIf, this::findSiblingRootIf);
            .pattern("renderConditional(el, $1, $2, ", selectedField, requireRef());

        // we need to insert a buffer here, so we can wait until the rest of the ui
        // has been visited, and then detect our sibling, so we can do correct insertBefore semantics.
        PrintBuffer insertion = new PrintBuffer();
        insertion.setDefaultSource(()->
            baseClass.addImport(Out1.class) + ".null1()"
        );
        redraw.addToEnd(insertion);

        result.onFinish(()->{
            // when the result is finished, check for siblings!
            // need to go to root if tag, then check parent (json or dom nodes),
            // pick the element after us, find the AssembledElement for our nextSibling,
            // and then use "insertBefore if inserted, else appendChild" semantics.
            Maybe<AssembledElement> sibling = findSibling();
            if (sibling.isPresent() && sibling.get() != this) {
                final AssembledElement after = sibling.get();
                final MethodBuffer findSib = base.getOrCreateMethod(
                    X_Modifier.PROTECTED,
                    typeEl,
                    "findSibling" + toTitleCase(requireRef()),
                    init -> {
                        init.patternln("$1 b = $2.out1();", typeBuilder, after.requireRef());
                        if (after instanceof AssemblyIf) {
                            // our next element can be nullable, so we need to defer to it's findSibling method...
                            init
                                .println("if (b == null) {")
                                .indent()
                                .patternln("return findSibling$1();", toTitleCase(after.requireRef()))
                                .outdent()
                                .println("} else {")
                                .indent()
                                .returnValue("b.getElement()")
                                .outdent()
                                .println("}");
                        } else {
                            // our next element is not nullable, just return it.
                            init.returnValue("b.getElement()");
                        }

                    }
                );
                insertion.clear();
                insertion.pattern("this::$1", findSib.getName());
            }
        });

        redraw.println(");");

        final PrintBuffer beforeResolved = assembler.getElementResolved().beforeResolved();
        if (isParentRoot()) {
            beforeResolved
                .patternln("$1(el);", redraw.getName());
        } else {
            beforeResolved
                .patternln("$1($2.out1().getElement());", redraw.getName(), attachTo);
        }
        // now, also hook up any model listeners necessary to trigger refreshes...

        /*

       getModel().onChange("logoutLink", (was, is)->{
      if ((was == null) != (is == null)) {
        // nullness has changed, attempt redraw
        redrawElIf(el);
      }
    });
    getModel().onChange("loginLink", (was, is)->{
      if ((was == null) != (is == null)) {
        // nullness has changed
      }
    });

        */
    }

    private Maybe<AssembledElement> findSibling() {
        AssemblyIf ai = this;
        while (getParent() instanceof AssemblyIf) {
            ai = (AssemblyIf) getParent();
        }
        final UiContainerExpr root = ai.getAst();
        Node parent = root.getParentNode();
        final Node sib;
        if (parent instanceof UiBodyExpr) {
            final List<Expression> children = ((UiBodyExpr) parent).getChildren();
            final int ind = children.indexOf(root);
            assert ind != -1 : "Parent does not contain child; parent: " + parent + "\nchild:" + root;
            if (children.size() > ind + 1) {
                sib = children.get(ind + 1);
            } else {
                // this if is the last node.  No sibling here.
                // We may want to consider further recursing into parent nodes,
                // but for now, it will be assumed that there's no reason to peek any further
                return Maybe.not();
            }
        } else if (parent instanceof JsonPairExpr) {
            JsonContainerExpr json = (JsonContainerExpr) parent.getParentNode();
            int key = Integer.parseInt(((JsonPairExpr) parent).getKeyString());
            if (json.size() > key) {
                sib = json.getNode(key + 1);
            } else {
                // last element in array.
                return Maybe.not();
            }
        } else {
            return Maybe.not();
        }
        AssembledElement sibling = sib.getExtra(UiConstants.EXTRA_ASSEMBLED_ELEMENT);
        return Maybe.nullable(sibling);
    }

    protected void writeIfBlock(UiAssembler assembler, PrintBuffer toDom) {
        toDom.print("if (");
        boolean first = true, nls = ifs.size() > 2; // print newlines if more than two conditions
        for (Conditional cond : ifs) {
            String bits = cond.print(this, first);
            if (nls && !first) {
                toDom.println();
            }
            first = false;
            toDom.print(bits);
            for (Expression expr : cond.expressions.flatten(In1Out1.identity())) {
                expr.accept(ComposableXapiVisitor.whenMissingIgnore(AssemblyIf.class)
                    .withUiAttrExpr((attr, ctx)->true)
                    .withNameExpr((name, ctx)-> {
                        // TODO: check for $model or $data?  They should already have been resolved though...
                        return true;
                    })
                    .withTemplateLiteralExpr((name, ctx)-> {
                        // TODO: check for $model or $data?  They should already have been resolved though...
                        return true;
                    })
                    .withMethodCallExpr((call, ctx) -> {
                        final GeneratedUiMember modelInfo = call.getExtra(UiConstants.EXTRA_MODEL_INFO);
                        if (modelInfo != null) {
                            // this if block is based on a model expression.
                            // lets go ahead and setup a listener to refresh the conditional whenever
                            // the given model field changes (bonus points for work avoidance).
                            bindToModel(assembler, modelInfo, call.getName());
                        }
                        return true;
                    })

//                    .withUiContainerExpr((node, ctx)->)
                    , null);
            }

        }
        toDom.println(") {").indent();
    }

    private void bindToModel(
        UiAssembler assembler,
        GeneratedUiMember modelInfo,
        String name
    ) {
        // TODO: just cache up that we want to add bindings to this model field,
        // and also include more information from the conditional so we can do work avoidance
        // (like, isNull=$model::blah would only rebuild if the nullness of the value changes)
        // the api is currently very forgiving, with lists of expressions accepted, so we might
        // need to build out these optimizations slowly / as needed (high effort, medium value).

        // TODO: get ahold of the lazy factory for the given element, and add these handlers there
        // We eagerly call the redraw when first created, so we can avoid superfluous listeners
        // and rebuilds for elsif cases that haven't been created yet, by moving where these
        // handlers are registered to be behind a Lazy.out1() call.
        final PrintBuffer toDom = assembler.getElementResolved().afterResolved();
        toDom
            .patternln("getModel().onChange(\"$1\", (was, is)-> {", modelInfo.getMemberName())
            .indent();
        // bonus points for work avoidance here...
        if (isParentRoot()) {
            toDom.patternln("$1(el);", redrawName);
        } else {
            toDom.patternln("$1($2.out1().getElement());", redrawName, attachTo);
        }

        toDom
            .outdent()
            .println("});");
    }

    private void serializeElses(
        UiAssembler assembler,
        PrintBuffer toDom,
        GeneratedFactory rootFactory,
        boolean needsLeadingElse
    ) {
        final boolean hasElses = hasElses();
        for (AssemblyIf elsif : elsifs) {
            if (needsLeadingElse) {
                toDom.print(" else ");
            }
            needsLeadingElse = true;

            // need to give each elsif a visit.
            final UiContainerExpr ast = elsif.getAst();

            if (elsif.isUseLazyFactory()) {

                final GeneratedFactory lazy = elsif.startElement(getSource(), assembler, this, ast);
                // purposely not calling finishElement...
                // we don't want a top-level if, we want a nested `else if`, backed by lazy factory only if necessary;
                // i.e. if this is a leaf conditional else if (does not contain conditionals),
                // then inline the content.  For now, we'll just inline by default,
                // and do it right when we want / need arrays of elements (which will require the lazy factory)

                elsif.writeIfBlock(assembler, toDom);
                final GeneratedFactory ei = serialize(assembler, rootFactory, elsif.whenTrue);
                ei.getInitBuffer()
                    .println("return " + ei.getGetter() +";");


                toDom.println("return " + selectedField + " = " + lazy.getGetter() +";");
            } else {
                elsif.borrowState(this);
                elsif.visitAst(ast);
                // purposely not calling finishElement...
                // we don't want a top-level if, we want a nested `else if`, backed by lazy factory only if necessary;
                // i.e. if this is a leaf conditional else if (does not contain conditionals),
                // then inline the content.  For now, we'll just inline by default,
                // and do it right when we want / need arrays of elements (which will require the lazy factory)

                elsif.writeIfBlock(assembler, toDom);
                final GeneratedFactory ei = serialize(assembler, rootFactory, elsif.whenTrue);
                toDom.println("return " + selectedField + " = " + ei.getGetter() +";");
            }

            toDom.outdent().print("}");
            if (!hasElses) {
                toDom.println();
            }
        }
        if (hasElses) {
            toDom.println(" else {").indent();
            final GeneratedFactory els = serialize(assembler, rootFactory, elses);
            toDom.println("return " + selectedField + " = " + els.getGetter() +";");
            toDom.outdent().println("}");
        }
    }

    private void borrowState(AssemblyIf assemblyIf) {
        factory = assemblyIf.factory;
        selectedField = assemblyIf.selectedField;
        redrawName = assemblyIf.redrawName;
        attachTo = assemblyIf.attachTo;
    }

    private boolean hasElses() {
        // TODO memoize
        return elses.flatten(In1Out1.identity()).isNotEmpty();
    }

    protected boolean isUseLazyFactory() {
        // we need to use a lazy factory when the conditional has more than 2 resulting statements.
        return whenTrue.flatten(In1Out1.identity()).hasAtLeast(2);
    }

    private GeneratedFactory serialize(
        UiAssembler assembler,
        GeneratedFactory rootFactory,
        MappedIterable<Iterable<Expression>> items
    ) {
        final SizedIterable<Expression> flattened = items.flatten(In1Out1.identity()).counted();
        final AssembledUi assembly = getSource();
        final UiGeneratorTools tools = assembly.getTools();
        final GeneratedUiComponent component = assembly.getUi();
        final GeneratedUiBase baseClass = assembly.getUi().getBase();
        String builder = baseClass.getElementBuilderType(assembly.getNamespace());

        GeneratedFactory factory;

        if (flattened.size() == 1 && flattened.first() instanceof UiContainerExpr) {
            // when serializing a single item that is a <dom /> element,
            // we can skip any (ugly) extra wrapping by just creating the child directly and using its factory
            final UiContainerExpr body = (UiContainerExpr) flattened.first();
            final UiAssemblerResult child = assembler.addChild(getSource(), AssemblyIf.this, body);
            factory = child.getFactory();
        } else {
            String field = baseClass.newFieldName(requireRef());
            factory = new LazyInitFactory(getSource().getBaseClass(), builder, field,true);
            final LocalVariable var = factory.setVar(builder, BUILDER_VAR, true)
                .setInitializer(assembly.newBuilder());

            if (rootFactory.isResettable()) {
                assert factory.isResettable() : "We expected a resettable factory; got " + factory;
                final PrintBuffer beforeResolved = assembler.getElementResolved().beforeResolved();
                PrintBuffer added = new PrintBuffer(beforeResolved.getIndentCount());
                beforeResolved.addToBeginning(added);
                added.patternln("$1.bind($2);", rootFactory.getFieldName(), factory.getFieldName());
            }

            final PrintBuffer init = factory.getInitBuffer();
            final ApiGeneratorContext ctx = assembly.getContext();
            String scopeKey = NameGen.getGlobal().newId();
            final ComposableXapiVisitor<AssemblyIf> visitor = ComposableXapiVisitor.onMissingFail(AssemblyIf.class);
            visitor
                .withUiContainerExpr((dom, ifTag) -> {
                    final UiAssemblerResult child = assembler.addChild(getSource(), AssemblyIf.this, dom);
                    init.println(factory.getVarName() +".addChild(" + child.getFactory().getGetter()+");");
                    return false;
                })
                .withStringLiteralTerminal((str, ifTag) -> {
                    init.patternln("$1.append($2)", factory.getVarName(), X_Source.javaQuote(str.getValue()));
                })
                .withTemplateLiteralTerminal((n, ifTag) -> {
                    init.patternln("$1.append($2)", factory.getVarName(), X_Source.javaQuote(n.getValueWithoutTicks()));
                })
                .withNameTerminal((n, ifTag) -> {
                    // TODO: figure out if this is a name of something w/ a type that we should handle differently
                    final Expression expr = assembly.getGenerator().resolveReference(
                        tools,
                        ctx,
                        component,
                        baseClass,
                        null,
                        null,
                        n, false
                    );
                    String resolved = tools.resolveString(ctx, expr, true);
                    if (!resolved.trim().isEmpty()) {
                        final AssemblyIf parent = n.removeExtra(scopeKey);
                        if (parent == null) {
                            // no parent scope...
                            init.patternln("$1.append($2)", factory.getVarName(),  resolved);
                        } else {
                            // parent scope... just print the resolved value alone.
                            init.print(resolved);
                        }
                    }
                })
                .withJsonContainerRecurse((n, ifTag)->{
                    if (!n.isArray()) {
                        throw new IllegalArgumentException("<if /> cannot serialize json object " + getSource().getTools().debugNode(n));
                    }
                })
                .withMethodCallExpr((n, call)->{
                    // TODO: handle properly by abstracting method reference handler below into a reusable method.
                    return false;
                })
                .withMethodReferenceExpr((methodRef, ifTag) -> {
                    String methodName = methodRef.getIdentifier();
                    // find the target for this method, so we can figure out type information.
                    final SizedIterable<UserDefinedMethod> results = component.findUserMethod(methodName);
                    final UserDefinedMethod winner;
                    if (results.size() == 1)  {
                        // yay!  we're done...
                        winner = results.first();
                    } else if (results.isEmpty()) {
                        throw new UnsupportedOperationException(debugNode(methodRef) + " not supported");
                    } else {
                        // more than one result for the method name...
                        throw new UnsupportedOperationException(
                            "Overloading not (yet) supported " + results.join("("," + ", ")")
                              + " found for " + debugNode(methodRef)
                        );
                    }
                    Type type = winner.getType();
                    methodRef.getScope().addExtra(scopeKey, AssemblyIf.this);
                    if (type == null) {
                        X_Log.warn(AssemblyIf.class, "No type information found for ", methodRef, " from ", winner);

                        init.pattern("$1.append(", factory.getVarName()).indent();
                        methodRef.getScope().accept(visitor, ifTag);
                        init.outdent().patternln(".$1());", methodRef.getIdentifier());


//                        init.pattern("$1.append(", factory.getVarName()).indent();
//                        methodRef.getScope().addExtra(scopeKey, true); // TODO: an object of some kind to accurately encapsulate ParentScope
//                        methodRef.getScope().accept(visitor, ifTag);
//                        init.outdent().patternln(".$1());", methodRef.getIdentifier());
                        // TODO: a printbuffer command that will shrink any empty multiline(
                        // ) parens (deferred until toSource(), of course)
                        // See xapi.dev.source.statements.StatementBuffer
                    } else if (type instanceof VoidType) {
                        // a method w/out return type.  just call that method.
                        methodRef.getScope().accept(visitor, ifTag);
                        init.patternln(".$1()", methodRef.getIdentifier());
                    } else if (type instanceof PrimitiveType || type instanceof ReferenceType) {
                        X_Log.error(AssemblyIf.class, "Not handled", type.getClass(), type, "from", this);
                    } else {
                        X_Log.error(AssemblyIf.class, "Not handled", type.getClass(), type);
                        // TODO: actually implement something smart regarding supported types / actions.
                        // String / <dom/> -> append to buffer
                        // Out1 / Provider -> call factory and append result to buffer.
                        throw new NotYetImplemented("TODO: actually bother with this");
                    }
                    // we already resolved scope / etc.
                    return false;
                })
            ;

            for (Expression wt : flattened) {
                wt.accept(visitor, this);
            }

        }
        return factory;
    }

    private void elsif(Expression expr) {
        if (expr instanceof JsonContainerExpr) {
            ((JsonContainerExpr) expr).getValues().forAll(this::elsif);
        } else if (expr instanceof UiContainerExpr) {
            addElseIf((UiContainerExpr) expr);
        } else {
            throw new IllegalArgumentException("elsif may only have <if /> or [<if/>, ...] children. You sent " + getSource().getTools().debugNode(expr));
        }
    }

    /**
     * Adds expressions to print when all of the {@code <if />} conditions are true.
     * @param whenTrue
     */
    public final void whenTrue(Expression ... whenTrue) {
        whenTrue(ArrayIterable.iterate(whenTrue));
    }

    /**
     * Adds expressions to print when all of the {@code <if />} conditions are true.
     * @param whenTrue
     */
    public void whenTrue(Iterable<Expression> whenTrue) {
        this.whenTrue.add(whenTrue);
    }

    /**
     * Adds expressions to print when any of the {@code <if />} conditions are false.
     *
     * Examples:
     * <pre>
     *   <if isNull="var" whenTrue=... whenFalse=::: />
     *   if (var == null) { ... }
     *   else { ::: }
     * </pre>
     *
     * Note that whenFalse is always run when the if fails, so if you mix in elsif/else...
     *
     *
     * <pre>
     *
     *   <if isNull="var" whenTrue=wt() whenFalse=wf()
     *     elsif = <if isNotNull="bar">bar()</if>
     *     elsif = <if isNotNull="baz">baz()</if>
     *     else=els()
     *   /if>
     *
     *   ->
     *
     *   if (var == null) { wt(); }
     *   else {
     *     // whenFalse always runs
     *     wf();
     *     // elsif / else continue here
     *     if (bar != null) { bar(); }
     *     else if (baz != null) { baz(); }
     *     else { els(); }
     *
     *   }
     *
     *   If whenFalse was not used:
     *
     *   if (var == null) { wt(); }
     *   else if (bar != null) { bar(); }
     *   else if (baz != null) { baz(); }
     *   else { els(); }
     *
     * </pre>
     *
     *
     * @param whenFalse Expressions to print whenever the if is false
     *                  (this code is run in all else/elsif cases).
     */
    public final void whenFalse(Expression ... whenFalse) {
        whenFalse(ArrayIterable.iterate(whenFalse));
    }

    /**
     * See {@link #whenFalse(Expression...)} for examples.
     *
     * Adds expressions to print when any of the {@code <if />} conditions are false.
     * @param whenFalse Expressions to print whenever the if is false
     *                  (this code is run in all else/elsif cases).
     */
    public void whenFalse(Iterable<Expression> whenFalse) {
        this.whenFalse.add(whenFalse);
    }

    public final void addIf(ConditionType condition, Expression ... ifs) {
        addIf(condition, ArrayIterable.iterate(ifs));
    }

    public void addIf(ConditionType condition, Iterable<Expression> ifs) {
        this.ifs.add(new Conditional(condition, mode, ifs));
    }

    public final void addElse(Expression ... elses) {
        addElse(ArrayIterable.iterate(elses));
    }

    public void addElse(Iterable<Expression> elses) {
        this.elses.add(elses);
    }

    public final void addElseIf(UiContainerExpr elsifTag) {
        // validate...
        for (UiAttrExpr attr : elsifTag.getAttributes()) {
            switch (attr.getNameString().toLowerCase()) {
                case "elIf":
                case "elsIf":
                case "elseIf":
                    throw new IllegalArgumentException("An elsif may not have any elsifs; (make this node a sibling of its parent elsif): " + debugNode(elsifTag));
                case "else":
                    throw new IllegalArgumentException("An elsif may not have an else block; " + debugNode(elsifTag));
                case "whenFalse":
                    throw new IllegalArgumentException("An elsif may not have a whenFalse block; " + debugNode(elsifTag));
            }
        }
        AssemblyIf elsif = new AssemblyIf(getSource(), this, elsifTag);

        elsifs.add(elsif);
    }

    @Override
    public PrintBuffer getInitBuffer() {
        return factory.getInitBuffer();
    }

    public boolean canBeEmpty() {
        return elses.isEmpty();
    }

    public String getRedrawMethod() {
        return redrawName;
    }

    @Override
    protected void updateResult(
        UiAssemblerResult result
    ) {
        result.withLogic();
        super.updateResult(result);
    }
}
