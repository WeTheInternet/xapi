package xapi.dev.ui.tags.assembler;

import xapi.dev.ui.tags.factories.GeneratedFactory;
import xapi.fu.Do;

/**
 * A class to encapsulate state about a given run of a tree of {@link TagAssembler}s.
 *
 * Each tag generated should create their own instance of these results,
 * call the various public methods to signal any interesting state detection,
 * AND call .absorb() on the results of all processed children.
 *
 * That is to say, you should not modify the result of a child and then return it.
 * Instead create your own result and absorb the results of your children.
 *
 * This will ensure that any tooling which cares about a given state,
 * like hasStructure or hasLayout, will not mistakenly think that
 * your text node has children, or that your empty {@code <if />} tag should be rendered.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 2/26/18.
 */
public class UiAssemblerResult {

    public static final UiAssemblerResult ABORT = new UiAssemblerResult().setAttachToRoot(false).release();

    private boolean
    /**
     * An element has structure if it contains child elements (tags).
     */
    structure,
    /**
     * An element whose children have children.
     * Not very informative unless examining if "parents of leaf nodes" are empty.
     */
    childStructure,
    /**
     * An element has layout if it renders text, images or just takes up space.
     */
    layout,
    /**
     * An element with structure whose children have layout.
     */
    childLayout,
    /**
     * A logical element uses runtime state checks to render 0 or more children.
     *
     * Prime examples are {@code <if /> or <for /> tags}.
     *
     * Whenever a logical element is found, it is assumed to be dynamic,
     * and may serve as a barrier to optimization.  We will make a best-effort
     * to produce a system using compile-time constants that can inline logical elements
     * that would reduce to a single option at all times,
     * to work towards "just spit out html" instead of "wire up do-nothing layers of logic".
     *
     */
    logic,
    /**
     * An element with structure who have children that are logical elements.
     */
    childLogic,
    /**
     * An element with shadow dom maintains separate element graphs for
     * "logical children" (what the outside world sees when querying element),
     * and "physical children" (the native elements stitched into the UI).
     *
     * While this concepts exists natively in html 5,
     * it is a paradigm that can be virtualized on any platform.
     */
    shadow,
    /**
     * An element with structure whose children have shadow DOM
     */
    childShadow,
    /**
     * An element that has a defined model (and thus, model builder)
     */
    model,
    /**
     * An element with structure whose children have model elements.
     */
    childModel,
    /**
     * An element with layout that contains custom style definitions / usage
     */
    style,
    /**
     * An element with structure whose children define styles.
     */
    childStyle;

    private Do onRelease = Do.NOTHING;
    private boolean released;
    private ElementAssembly assembly;
    // set this to false if you don't want to attach a
    private boolean attachToRoot = true;
    private AssembledElement element;
    private GeneratedFactory factory;
    private boolean hidden;

    public boolean hasChildLayout() {
        return childLayout;
    }

    public boolean hasChildLogic() {
        return childLogic;
    }

    public boolean hasChildShadow() {
        return childShadow;
    }

    public boolean hasChildModel() {
        return childModel;
    }

    public boolean hasChildStyle() {
        return childStyle;
    }

    public boolean hasLayout() {
        return layout;
    }

    public boolean hasLogic() {
        return logic;
    }

    public boolean hasModel() {
        return model;
    }

    public boolean hasShadow() {
        return shadow;
    }

    public boolean hasStructure() {
        return structure;
    }


    public boolean hasStyle() {
        return style;
    }

    public UiAssemblerResult withStructure() {
        this.structure = true;
        return this;
    }

    public UiAssemblerResult withLayout() {
        this.layout = true;
        return this;
    }


    public UiAssemblerResult withModel() {
        this.model = true;
        return this;
    }

    public UiAssemblerResult withLogic() {
        this.logic = true;
        return this;
    }

    public UiAssemblerResult withStyle() {
        this.style = true;
        return this;
    }

    /**
     * Absorb the settings of another result.
     * @param other
     */
    public void absorb(UiAssemblerResult other) {
        if (other.released) {
            // no need to absorb released resources.
            // If you use UiAssemblerResult.ABORT,
            // you can return a "do not use this result" released object
            return;
        }
        if (!other.attachToRoot) {
            this.attachToRoot = false;
        }
        if (other.structure) {
            this.structure = true;
        }
        if (other.childStructure) {
            this.childStructure = true;
        }
        if (other.model) {
            this.model = true;
        }
        if (other.childModel) {
            this.childModel = true;
        }
        if (other.layout) {
            this.layout = true;
        }
        if (other.childLayout) {
            this.childLayout = true;
        }
        if (other.logic) {
            this.logic = true;
        }
        if (other.childLogic) {
            this.childLogic = true;
        }
        if (other.style) {
            this.style = true;
        }
        if (other.childStyle) {
            this.childStyle = true;
        }
    }

    public void adopt(UiAssemblerResult child) {
        if (!child.attachToRoot || child.released) {
            return;
        }
        // anything with a child has structure.
        this.structure = true;

        // TODO: consider using a whole bunch of child* prefixes for each property;
        // for now, the only place we wouldn't want a parent to assume the same metadata
        // as children is w.r.t. model.
        if (child.model || child.childModel) {
            this.childModel = true;
        }
        if (child.layout || child.childLayout) {
            this.childLayout = true;
        }
        if (child.logic || child.childLogic) {
            this.childLogic = true;
        }
        if (child.style || child.childStyle) {
            this.childStyle = true;
        }
    }

    public boolean isSuccess() {
        return assembly != null;
    }

    public synchronized void onRelease(Do task) {
        if (released) {
            task.done();
        } else {
            onRelease = onRelease.doAfter(task);
        }
    }

    public synchronized UiAssemblerResult release() {
        released = true;
        final Do todo = onRelease;
        onRelease = Do.NOTHING;
        todo.done();
        return this;
    }

    public UiAssemblerResult setAssembly(ElementAssembly assembly) {
        this.assembly = assembly;
        return this;
    }

    public ElementAssembly getAssembly() {
        return assembly;
    }

    public boolean isAttachToRoot() {
        return attachToRoot;
    }

    public UiAssemblerResult setAttachToRoot(boolean attachToRoot) {
        this.attachToRoot = attachToRoot;
        return this;
    }

    public void setElement(AssembledElement element) {
        this.element = element;
    }

    public AssembledElement getElement() {
        return element;
    }

    public void setFactory(GeneratedFactory factory) {
        this.factory = factory;
    }

    public GeneratedFactory getFactory() {
        return factory;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isHidden() {
        return hidden;
    }
}
