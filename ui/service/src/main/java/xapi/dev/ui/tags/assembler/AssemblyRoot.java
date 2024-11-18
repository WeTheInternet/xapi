package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.ui.api.component.HasChildren;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/26/18.
 */
public class AssemblyRoot extends AssembledGraphElement {

    /**
     * You should not reference this field, it's the null sentinel for an AssemblyRoot instance.
     *
     * Instead of checking for null parents, instead use {@link AssemblyRoot#isRoot()} to terminate searches.
     */
    static final AssemblyRoot SUPER_ROOT = new AssemblyRoot(null);
    private static final UiContainerExpr ROOT_NODE = new UiContainerExpr("");

    public AssemblyRoot(AssembledUi assembled) {
        // for SUPER_ROOT's constructor, the first arg will be null
        // (we just kick the can on that null check to beyond usercode reach),
        // so we can all assume null-free getParent() semantics).
        super(assembled, SUPER_ROOT, ROOT_NODE);
    }

    @Override
    public boolean isRoot() {
        return true;
    }

}
