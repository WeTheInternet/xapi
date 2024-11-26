package xapi.test.components.xapi.test.components.bdd;

import static xapi.fu.Immutable.immutable1;
import static xapi.scope.X_Scope.service;


import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.scope.X_Scope;
import xapi.scope.api.Scope;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.api.component.IsComponentBuilder;

public class BuildToDoAppComponent <El, C extends ToDoAppComponent<El>> implements IsComponentBuilder<ComponentOptions<El, C>> {

  private final ComponentConstructor<El, C>  creator;

  private final In1Out1<El, C>  extractor;

  private final ComponentOptions<El, C> opts;

  public BuildToDoAppComponent (ComponentConstructor<El, C> creator, In1Out1<El, C> extractor) {
    this.creator = creator;
    this.extractor = extractor;
    this.opts = new ComponentOptions<>();
  }

  public ToDoAppComponent<El> build (Out1<Scope> scope) {
    final ToDoAppComponent[] component = {null};
    service().runInScopeNoRelease(scope.out1(), s->{
      component[0] = creator.constructComponent(opts, extractor);
    });
    return component[0];
  }

  public final ToDoAppComponent<El> build () {
    return build(X_Scope::currentScope);
  }

  public final ToDoAppComponent<El> build (Scope scope) {
    return build(immutable1(scope));
  }

  public ComponentOptions<El, C> getOpts () {
    return opts;
  }

}
