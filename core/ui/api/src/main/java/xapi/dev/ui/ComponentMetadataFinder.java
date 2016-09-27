package xapi.dev.ui;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.fu.Do;
import xapi.fu.In2;
import xapi.util.api.Destroyable;

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
      recurse(n, In2.reduceAll(super::visit, n, query));
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
    recurse(n, In2.reduceAll(super::visit, n, query)
                .doBefore(()->
                    query.notifyUiAttrExpr(scope, n)
                ));
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
  public void visit(CssBlockExpr n, ComponentMetadataQuery query) {
    query.notifyCssExpr(scope, n);
    super.visit(n, query);
  }

  @Override
  public void visit(CssContainerExpr n, ComponentMetadataQuery query) {
    query.notifyCssExpr(scope, n);
    super.visit(n, query);
  }

  @Override
  public void visit(CssSelectorExpr n, ComponentMetadataQuery query) {
    query.notifyCssExpr(scope, n);
    super.visit(n, query);
  }

  @Override
  public void visit(CssRuleExpr n, ComponentMetadataQuery query) {
    query.notifyCssExpr(scope, n);
    super.visit(n, query);
  }

  @Override
  public void visit(CssValueExpr n, ComponentMetadataQuery query) {
    query.notifyCssExpr(scope, n);
    super.visit(n, query);
  }

  @Override
  public void destroy() {
    head = scope = null;
  }
}
