package xapi.dev.ui;

import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TemplateLiteralExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.expr.UiExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.fu.Do;
import xapi.util.api.Destroyable;

import static xapi.fu.In2.reduceAll;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/29/16.
 */
public class ComponentMetadataFinder extends VoidVisitorAdapter<ComponentMetadataQuery> implements Destroyable {

  private ComponentGraph head, scope;

  @Override
  public void visit(UiContainerExpr n, ComponentMetadataQuery query) {

    // always recurse into the root container
    boolean recurse = head == null;

    if (recurse) {
      head = scope = new ComponentGraph(n);
      super.visit(n, query);
      return;
    }
    // if we aren't the root, check the query to see if we should recurse
    recurse = scope.isAttributeChild() ?
        query.isVisitAttributeContainers() :
        query.isVisitChildContainers() ;

    if (recurse) {
      recurse(n, reduceAll(super::visit, n, query));
    }
  }

  protected void recurse(UiExpr n, Do recurse) {
    final ComponentGraph was = scope;
    scope = was.appendChild(n);
    try {
      recurse.done();
    } finally {
      scope = was;
    }
  }

  @Override
  public void visit(UiAttrExpr n, ComponentMetadataQuery query) {
    recurse(n, reduceAll(super::visit, n, query));
  }

  @Override
  public void visit(NameExpr n, ComponentMetadataQuery query) {
    super.visit(n, query);
    if (query.isTemplateName(n.getName())) {
      query.notifyNameExpr(scope, n);
    }
  }

  @Override
  public void visit(MethodReferenceExpr n, ComponentMetadataQuery query) {
    query.notifyMethodReference(scope, n);
    super.visit(n, query);
  }

  public void visit(StringLiteralExpr n, ComponentMetadataQuery query) {
    super.visit(n, query);
  }

  public void visit(TemplateLiteralExpr n, ComponentMetadataQuery query) {
    super.visit(n, query);
  }

  @Override
  public void destroy() {
    head = scope = null;
  }
}
