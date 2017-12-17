package xapi.ui.layout;

import static xapi.fu.Immutable.immutable1;


import xapi.fu.Out1;
import xapi.scope.X_Scope;
import xapi.scope.api.Scope;

public class BuildBoxComponent {

  public BoxComponent build (Out1<Scope> scope) {
    return null;
  }

  public final BoxComponent build () {
    return build(X_Scope::currentScope);
  }

  public final BoxComponent build (Scope scope) {
    return build(immutable1(scope));
  }

}
