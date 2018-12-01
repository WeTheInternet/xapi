package xapi.lang.api;

import com.github.javaparser.ast.expr.*;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/9/18.
 */
public interface AstConstants {

    NameExpr THIS_REF = new NameExpr("this");
    NameExpr SUPER_REF = new NameExpr("super");

    BooleanLiteralExpr TRUE = new BooleanLiteralExpr(true);
    BooleanLiteralExpr FALSE = new BooleanLiteralExpr(false);

    StringLiteralExpr EMPTY = new StringLiteralExpr("");

    IntegerLiteralExpr INT_0 = new IntegerLiteralExpr("0");
    IntegerLiteralExpr INT_1 = new IntegerLiteralExpr("1");
    IntegerLiteralExpr INT_NEG_1 = new IntegerLiteralExpr("-1");
    IntegerLiteralExpr INT_MIN = new IntegerLiteralExpr(Integer.toString(Integer.MIN_VALUE));
    IntegerLiteralExpr INT_MAX = new IntegerLiteralExpr(Integer.toString(Integer.MAX_VALUE));

    DoubleLiteralExpr DOUBLE_0 = new DoubleLiteralExpr("0");
    DoubleLiteralExpr DOUBLE_1 = new DoubleLiteralExpr("1");
    DoubleLiteralExpr DOUBLE_NEG_1 = new DoubleLiteralExpr("-1");
    DoubleLiteralExpr DOUBLE_MIN = new DoubleLiteralExpr(Double.toString(Double.MIN_VALUE));
    DoubleLiteralExpr DOUBLE_MAX = new DoubleLiteralExpr(Double.toString(Double.MAX_VALUE));

    LongLiteralExpr LONG_0 = new LongLiteralExpr("0");
    LongLiteralExpr LONG_1 = new LongLiteralExpr("1");
    LongLiteralExpr LONG_NEG_1 = new LongLiteralExpr("-1");
    LongLiteralExpr LONG_MIN = new LongLiteralExpr(Long.toString(Long.MIN_VALUE));
    LongLiteralExpr LONG_MAX = new LongLiteralExpr(Long.toString(Long.MAX_VALUE));
}
