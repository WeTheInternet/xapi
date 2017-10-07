package xapi.demo.gwt.client.ui;

import static xapi.fu.Immutable.immutable1;


import xapi.fu.Out1;
import xapi.scope.X_Scope;
import xapi.scope.api.Scope;

public class BuildXapiBoxComponent {

  public XapiBoxComponent build (Out1<Scope> scope) {
    return null;
  }

  public final XapiBoxComponent build () {
    return build(X_Scope::currentScope);
  }

  public final XapiBoxComponent build (Scope scope) {
    return build(immutable1(scope));
  }

}
