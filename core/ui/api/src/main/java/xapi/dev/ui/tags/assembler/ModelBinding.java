package xapi.dev.ui.tags.assembler;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.type.Type;
import xapi.dev.api.GeneratedUiMember;

/**
 * Some glue for "things representing a model field with respect to ui assemble".
 *
 * This is used to automate the binding of model fields to element attributes
 * (at first, just within xapi lang component source, but eventually also
 * to html dom attributes as well).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/15/18 @ 1:25 AM.
 */
public class ModelBinding {

    private final String name;
    private final GeneratedUiMember meta;
    private UiAttrExpr setter;
    private UiAttrExpr getter;

    public ModelBinding(String name, GeneratedUiMember meta) {
        this.name = name;
        this.meta = meta;
    }

    public void withGetter(UiAttrExpr expr, boolean readWrite) {
        this.getter = expr;
    }

    public void withSetter(UiAttrExpr expr, boolean readWrite) {
        this.setter = expr;
    }

    public Type getType() {
        return meta.getMemberType();
    }

    public String getName() {
        return name;
    }

    public UiAttrExpr getSetter() {
        return setter;
    }

    public UiAttrExpr getGetter() {
        return getter;
    }
}
