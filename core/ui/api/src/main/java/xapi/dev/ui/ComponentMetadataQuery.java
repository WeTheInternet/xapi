package xapi.dev.ui;

import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import xapi.collect.impl.SimpleStack;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.fu.In2Out1;
import xapi.util.api.Destroyable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class ComponentMetadataQuery implements Destroyable {

  private boolean visitChildContainers = false;
  private boolean visitAttributeContainers = true;

  private In1Out1<String, Boolean> templateNameFilter =
      In2Out1.of(String::matches)
             .supply2("^[$]([a-z]\\S*)?"); // strings starting with $

  private final SimpleStack<In2<ComponentGraph, NameExpr>> nameListeners = new SimpleStack<>();
  private final SimpleStack<In2<ComponentGraph, MethodReferenceExpr>> methodReferenceListeners = new SimpleStack<>();


  public boolean isVisitChildContainers() {
    return visitChildContainers;
  }

  public ComponentMetadataQuery setVisitChildContainers(boolean visitChildContainers) {
    this.visitChildContainers = visitChildContainers;
    return this;
  }

  public boolean isVisitAttributeContainers() {
    return visitAttributeContainers;
  }

  public ComponentMetadataQuery setVisitAttributeContainers(boolean visitAttributeContainers) {
    this.visitAttributeContainers = visitAttributeContainers;
    return this;
  }

  public boolean isTemplateName(String name) {
    return templateNameFilter.io(name);
  }

  public ComponentMetadataQuery addNameListener(In2<ComponentGraph, NameExpr> listener) {
    nameListeners.add(listener);
    return this;
  }

  public ComponentMetadataQuery addMethodReferenceListener(In2<ComponentGraph, MethodReferenceExpr> listener) {
    methodReferenceListeners.add(listener);
    return this;
  }

  @Override
  public void destroy() {
    nameListeners.clear();
    methodReferenceListeners.clear();
  }

  public void notifyNameExpr(ComponentGraph scope, NameExpr n) {
    nameListeners.forEach(listener->listener.in(scope, n));
  }

  public void notifyMethodReference(ComponentGraph scope, MethodReferenceExpr n) {
    methodReferenceListeners.forEach(listener->listener.in(scope, n));
  }
}
