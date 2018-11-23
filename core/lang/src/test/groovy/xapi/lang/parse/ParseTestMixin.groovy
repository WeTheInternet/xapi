package xapi.lang.parse

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.UiContainerExpr

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/20/18 @ 3:59 AM.
 */
trait ParseTestMixin {

    private UiContainerExpr ast

    static <T extends Expression, G extends T> T getAttr(UiContainerExpr ui, String name, Class<G> type) {
        Expression attr = getAttr(ui, name)
        return attr == null ? null : type.cast(attr)
    }
    static Expression getAttr(UiContainerExpr ui, String name) {
        return ui.attrExpr(name).get()
    }

    UiContainerExpr getAst() {
        return this.@ast
    }

    UiContainerExpr parse(String src) {
        return this.@ast = JavaParser.parseUiContainer(src)
    }
}
