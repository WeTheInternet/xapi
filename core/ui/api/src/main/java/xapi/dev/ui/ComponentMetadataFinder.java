package xapi.dev.ui;

import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/29/16.
 */
public class ComponentMetadataFinder extends VoidVisitorAdapter<Object> {

  private final List<MethodReferenceExpr> methodReferences = new ArrayList<>();

  @Override
  public void visit(MethodReferenceExpr n, Object arg) {
    methodReferences.add(n);
    super.visit(n, arg);
  }

  public List<MethodReferenceExpr> getMethodReferences() {
    return methodReferences;
  }
}
