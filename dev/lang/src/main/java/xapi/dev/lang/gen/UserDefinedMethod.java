package xapi.dev.lang.gen;

import net.wti.lang.parser.ast.body.BodyDeclaration;
import net.wti.lang.parser.ast.body.FieldDeclaration;
import net.wti.lang.parser.ast.body.MethodDeclaration;
import net.wti.lang.parser.ast.expr.DynamicDeclarationExpr;
import net.wti.lang.parser.ast.expr.Expression;
import net.wti.lang.parser.ast.type.Type;

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
