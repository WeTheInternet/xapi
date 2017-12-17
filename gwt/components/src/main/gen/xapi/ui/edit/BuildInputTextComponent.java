package xapi.ui.edit;

import static xapi.fu.Immutable.immutable1;


import xapi.fu.Out1;
import xapi.scope.X_Scope;
import xapi.scope.api.Scope;

public class BuildInputTextComponent {

  public InputTextComponent build (Out1<Scope> scope) {
    return null;
  }

  public final InputTextComponent build () {
    return build(X_Scope::currentScope);
  }

  public final InputTextComponent build (Scope scope) {
    return build(immutable1(scope));
  }

}
