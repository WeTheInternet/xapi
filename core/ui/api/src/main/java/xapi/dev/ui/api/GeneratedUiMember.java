package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.DumpVisitor;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.dev.source.CanAddImports;

import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/10/17.
 */
public class GeneratedUiMember implements Serializable {
    private String memberName;
    private final IntTo<AnnotationExpr> annotations;
    private Type memberType;

    public GeneratedUiMember(Type memberType, String memberName) {
        annotations = X_Collect.newList(AnnotationExpr.class);
        this.memberType = memberType;
        this.memberName = memberName;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public Type getMemberType() {
        return memberType;
    }

    public void setMemberType(Type memberType) {
        this.memberType = memberType;
    }

    public String getCapitalized() {
        return Character.toUpperCase(memberName.charAt(0))
            + (memberName.length() == 1 ? "" : memberName.substring(1));
    }

    public String getterName() {
        return ("boolean".equals(memberType) || "Boolean".equals(memberType) ? "is" : "get")
            + getCapitalized();
    }

    public String importType(CanAddImports out) {
        DumpVisitor v = new DumpVisitor(false) {
            @Override
            protected String resolveType(ClassOrInterfaceType type) {
                String name = type.getName();
                if (name.contains(".")) {
                    return out.addImport(name);
                }
                return name;
            }
        };
        memberType.accept(v, null);
        return v.getSource();
    }
}
