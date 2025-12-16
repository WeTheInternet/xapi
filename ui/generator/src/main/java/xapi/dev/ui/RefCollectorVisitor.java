package xapi.dev.ui;

import net.wti.lang.parser.ASTHelper;
import net.wti.lang.parser.ast.expr.UiAttrExpr;
import net.wti.lang.parser.ast.expr.UiContainerExpr;
import net.wti.lang.parser.ast.visitor.VoidVisitorAdapter;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;

/**
 * Created by james on 6/7/16.
 */
public class RefCollectorVisitor extends VoidVisitorAdapter<Object> {

    public UiContainerExpr current;
    StringTo<UiContainerExpr> refMap = X_Collect.newStringMap(UiContainerExpr.class);

    public static StringTo<UiContainerExpr> collectRefs(UiContainerExpr container) {
        RefCollectorVisitor visitor = new RefCollectorVisitor();
        container.accept(visitor, null);
        return visitor.refMap;
    }

    @Override
    public void visit(UiContainerExpr n, Object arg) {
        final UiContainerExpr was = current;
        current = n;
        super.visit(n, arg);
        current = was;
    }

    @Override
    public void visit(UiAttrExpr n, Object arg) {
        switch (n.getName().getName()) {
            case "ref":
                String ref = ASTHelper.extractAttrValue(n);
                refMap.put(ref, current);
                break;
        }
        super.visit(n, arg);
    }

}
