package xapi.dev.lang.gen;

import net.wti.lang.parser.ast.expr.*;

/**
 * A place to put extracted generic methods from UiTagGenerator (for reuse in non-ui related generators).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/2/18 @ 4:15 AM.
 */
public interface ApiResolver {
    default boolean isModelReference(Expression expr) {
        if (expr instanceof NameExpr) {
            return "$model".equals(((NameExpr)expr).getName());
        } else if (expr instanceof MethodCallExpr) {
            return "getModel".equals(((MethodCallExpr)expr).getName());
        } else if (expr instanceof FieldAccessExpr) {
            return "$model".equals(((FieldAccessExpr)expr).getField());
        } else if (expr instanceof TemplateLiteralExpr) {
            return "getModel()".equals(((TemplateLiteralExpr)expr).getValueWithoutTicks());
        } else {
            return false;
        }
    }
}
