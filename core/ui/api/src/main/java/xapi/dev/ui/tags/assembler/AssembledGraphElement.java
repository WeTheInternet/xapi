package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.fu.itr.SizedIterable;
import xapi.ui.api.component.HasChildren;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/26/18.
 */
public class AssembledGraphElement extends AssembledElement implements HasChildren<AssembledElement> {

    private final IntTo<AssembledElement> children;

    /**
     * If you want a no-parent version of this constructor, see {@link AssemblyRoot} (or subclass yourself)
     *
     * @param source - The ui assembly which owns this element
     * @param parent - The parent element, never null (see {@link AssemblyRoot#SUPER_ROOT}.
     * @param ast    - The {@code <dom />} ast for the current element. never null.
     */
    public AssembledGraphElement(
        AssembledUi source,
        AssembledElement parent,
        UiContainerExpr ast
    ) {
        super(source, parent, ast);
        children = X_Collect.newList(AssembledElement.class);
    }

    @Override
    public SizedIterable<AssembledElement> getChildComponents() {
        return children;
    }

    @Override
    public void addChildComponent(AssembledElement child) {
        assert !children.contains(child) : "Double-adding " + child + " to " + this;
        children.add(child);
    }

    @Override
    public void removeChild(AssembledElement me) {
        children.remove(me);
    }
}
