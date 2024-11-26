package xapi.dev.lang.gen;

import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.DynamicDeclarationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.type.Type;

/**
 * A method created by a user via api= or impl= attributes.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/5/18.
 */
public class UserDefinedMethod {

    private final DynamicDeclarationExpr decl;
    private final Expression result;
    private final String serialized;

    public UserDefinedMethod(DynamicDeclarationExpr decl, Expression result, String serialized) {
        this.decl = decl;
        this.result = result;
        this.serialized = serialized;
    }

    public DynamicDeclarationExpr getDecl() {
        return decl;
    }

    public Expression getResult() {
        return result;
    }

    public String getSerialized() {
        return serialized;
    }

    public MethodDeclaration asMethod() {
        return (MethodDeclaration) decl.getBody();
    }

    public Type getType() {
        final BodyDeclaration body = decl.getBody();
        if (body instanceof MethodDeclaration) {
            return ((MethodDeclaration) body).getType();
        }
        if (body instanceof FieldDeclaration) {
            return ((FieldDeclaration) body).getType();
        }
//        // TODO: nothing else supported / throw?
        return null;
    }
}
