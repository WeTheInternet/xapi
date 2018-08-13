package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.source.*;
import xapi.dev.ui.api.GeneratedUiBase;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.tags.UiTagGenerator;
import xapi.dev.ui.tags.factories.GeneratedFactory;
import xapi.dev.ui.tags.factories.LazyInitFactory;
import xapi.fu.Immutable;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.ui.api.component.HasChildren;
import xapi.ui.api.component.HasParent;
import xapi.util.X_String;

/**
 * Represents each assembled element within a component's ui DOM.
 *
 * <pre>
 * For a given component, my-custom-element:
 *
 * <my-custom-element>
 *   <div>
 *       <span>...</span>
 *   </div>
 * </my-custom-element>
 *
 * There are three AssembledElements:
 * a root div,
 * a branch span,
 * and some text, "..."
 *
 * </pre>
 *
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/26/18.
 */
public class AssembledElement implements HasParent<AssembledElement> {

    public static final Immutable<String> GET_ROOT_ELEMENT = Immutable.immutable1("getElement()");
    public static final String BUILDER_VAR = "b";

    private final UiContainerExpr ast;
    private final AssembledElement parent;
    private final AssembledUi source;
    private String ref;
    protected PrintBuffer initBuffer;
    private final Lazy<PrintBuffer> onCreated;

    /**
     * If you want a no-parent version of this constructor, see {@link AssemblyRoot} (or subclass yourself)
     *
     * @param source - The ui assembly which owns this element
     * @param parent - The parent element, never null (see {@link AssemblyRoot#SUPER_ROOT}.
     * @param ast - The {@code <dom />} ast for the current element. never null.
     */
    @SuppressWarnings("unchecked")
    public AssembledElement(AssembledUi source, AssembledElement parent, UiContainerExpr ast) {
        this.source = source;
        this.ast = ast;
        this.parent = parent;
        if (this.parent instanceof HasChildren) {
            ((HasChildren) this.parent).addChildComponent(this);
        }
        onCreated = Lazy.deferred1(()->{
            if (initBuffer == null) {
                // when the initBuffer is created, it will do the attach...
                return new PrintBuffer(2);
            }
            PrintBuffer created = new PrintBuffer(initBuffer.getIndentCount());
            initBuffer.addToEnd(created);
            return created;
        });
    }

    public UiContainerExpr getAst() {
        return ast;
    }

    public AssembledElement getParent() {
        return parent;
    }

    public AssembledUi getSource() {
        return source;
    }

    /**
    * called for you by generator _after_ this instance
    * of an element was created and passed inspection.
    */
    public AssembledElement spyGenerator(UiTagGenerator generator) {

        return this;
    }

    public boolean isRoot() {
        return false;
    }
    public boolean isParentRoot() {
        return parent == null || parent.isRoot();
    }

    public Out1<String> maybeRequireRefRoot() {
        return ()->getRoot().requireRef();
    }

    public Out1<String> maybeRequireRef() {
        return Lazy.deferred1(this::requireRef);
    }

    public String requireRef() {
        if (ref == null) {
            ref = source.newRef(this, true);
        }
        return ref;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * Default behavior for startElement:create a {@code Lazy<ElBuilder> } initialized by instance method
     * (This avoids field-declaration-order issues in compiling java,
     * but does not protect you from actual "concurrent resolution errors".
     *
     * i.e. lazy initialization has to complete for each element before they can / should be able to access each other.
     *
     * In order to code safely, whenever you need to reference any other element,
     * like so:
     *
     * <box ref="parent" model={val: "World"}>
     *     <text ref="child" model={val: "Hi"}>$child.val, $parent.val!</text>
     * </box>
     * // creates html:
     * <box ref="parent" model-id="a123generated">
     *     <text ref="child" model-id="d456" />
     * </box>
     * and:
     * ModelCache cache = X_Model.cache(); // or whatever
     * cache.saveModel(newModel("a123generated", entry("val", "World")))
     * // again for "d456"
     * and:
     * $child.onCreated(el->{
     *    // once we're in here, all elements and models should be created and available
     *    el.innerText = "$child.val, $parent.val!"; // except made of StringBuilder uglies
     *    // hook up bindings to the cached models...
     * });
     *
     * @param assembly
     * @param assembler
     * @param parent
     * @param el
     * @return
     */
    public GeneratedFactory startElement(
        AssembledUi assembly,
        UiAssembler assembler,
        AssembledElement parent,
        UiContainerExpr el
    ) {
        assert initBuffer == null : "Already initialized and calling .startElement again!";
        final GeneratedUiComponent component = assembly.getUi();
        final GeneratedUiBase baseClass = component.getBase();
        String builder = baseClass.getElementBuilderType(assembly.getNamespace());

        GeneratedFactory lazy = new LazyInitFactory(component.getBaseClass(), builder, requireRef(), false);
        final LocalVariable local = lazy.setVar(builder, BUILDER_VAR, false);
        local.setInitializer(assembly.newBuilder());
        initBuffer = lazy.getInitBuffer();
        if (onCreated.isResolved()) {
            // generated added onCreated callback already.  Be nice and attach it.
            final PrintBuffer created = onCreated.out1();
            // fixup indent...
            created.setIndentCount(lazy.getInitBuffer().getIndentCount());
            lazy.getInitBuffer().addToEnd(created);
        }

        return lazy;
    }

    public AssembledElement getRoot() {
        return isRoot() ? this : getParent();
    }

    public PrintBuffer getInitBuffer() {
        return initBuffer;
    }

    public PrintBuffer getOnCreatedBuffer() {
        return onCreated.out1();
    }

    public String prefixName(String prefix) {
        return prefix + X_String.toTitleCase(requireRef());
    }

    public void finishElement(
        AssembledUi assembly,
        UiAssembler assembler, GeneratedFactory factory, UiAssemblerResult result,
        UiContainerExpr el
    ) {

    }

    public String debugNode(Expression attr) {
        return getSource().getTools().debugNode(attr);
    }

    public Expression resolveRef(AssembledElement parent, UiAttrExpr attr) {
        return resolveRef(parent, attr.getExpression(), true);
    }
    public Expression resolveRef(AssembledElement parent, Expression expr, boolean rewriteParent) {
        if (parent == null) {
            parent = this;
        }
        return source.getGenerator().resolveReference(
            source.getTools(),
            source.getContext(),
            source.getUi(),
            source.getUi().getBase(),
            parent.maybeRequireRefRoot(),
            parent.maybeRequireRef(),
            expr instanceof UiAttrExpr ? ((UiAttrExpr) expr).getExpression() : expr,
            rewriteParent
        );
    }

    @SuppressWarnings("unchecked")
    public String serialize(Expression unresolved) {
        final Expression resolved = resolveRef(this, unresolved, false);
        @SuppressWarnings("UnnecessaryLocalVariable") // nice for debugging
        final String serialized = getSource().getTools().resolveString(getSource().getContext(), resolved);
        return serialized;
    }
}
