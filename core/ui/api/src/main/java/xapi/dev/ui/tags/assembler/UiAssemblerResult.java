package xapi.dev.ui.tags.assembler;

import xapi.fu.Do;

/**
 * A class to encapsulate state about a given run of a tree of UiAssemblers.
 *
 * Each tag generated should create their own instance of these results,
 * call the various public methods to signal any interesting state detection,
 * AND call .absorb() on the results of all processed children.
 *
 * That is to say, you should not modify the result of a child and then return it.
 * Instead create your own result and absorb the results of your children.
 *
 * This will ensure that any tooling which cares about a given state,
 * like hasStructure or hasLayout will not mistakenly think that
 * your text node inside a scroll panel has structure or layout.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 2/26/18.
 */
public class UiAssemblerResult {

    private boolean structure, layout, model, childModels, logic, style;

    private Do onRelease = Do.NOTHING;
    private boolean released;
    private ElementAssembly assembly;

    public boolean hasStructure() {
        return structure;
    }

    public UiAssemblerResult withStructure() {
        this.structure = true;
        return this;
    }

    public boolean hasLayout() {
        return layout;
    }

    public UiAssemblerResult withLayout() {
        this.layout = true;
        return this;
    }

    public boolean hasChildModels() {
        return childModels;
    }

    public boolean hasModel() {
        return model;
    }

    public UiAssemblerResult withModel() {
        this.model = true;
        return this;
    }

    public boolean hasLogic() {
        return logic;
    }

    public UiAssemblerResult withLogic() {
        this.logic = true;
        return this;
    }

    public boolean hasStyle() {
        return style;
    }

    public UiAssemblerResult withStyle() {
        this.style = true;
        return this;
    }

    public void absorb(UiAssemblerResult child) {
        if (child.structure) {
            this.structure = true;
        }
        // TODO: consider using a whole bunch of child* prefixes for each property;
        // for now, the only place we wouldn't want a parent to assume the same metadata
        // as children is w.r.t. model.
        if (child.model || child.childModels) {
            this.childModels = true;
        }
        if (child.layout) {
            this.layout = true;
        }
        if (child.logic) {
            this.logic = true;
        }
        if (child.style) {
            this.style = true;
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

    public synchronized void release() {
        released = true;
        final Do todo = onRelease;
        onRelease = Do.NOTHING;
        todo.done();
    }

    public void setAssembly(ElementAssembly assembly) {
        this.assembly = assembly;
    }

    public ElementAssembly getAssembly() {
        return assembly;
    }
}
