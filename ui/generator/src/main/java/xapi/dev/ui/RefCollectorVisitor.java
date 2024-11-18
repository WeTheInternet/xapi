package xapi.dev.ui;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
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
