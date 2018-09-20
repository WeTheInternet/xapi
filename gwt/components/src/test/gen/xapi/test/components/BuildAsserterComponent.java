package xapi.test.components;

import static xapi.fu.Immutable.immutable1;
import static xapi.scope.X_Scope.service;


import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.Out1;
import xapi.scope.X_Scope;
import xapi.scope.api.Scope;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.IsComponentBuilder;
import xapi.ui.api.component.ModelComponentOptions;

public class BuildAsserterComponent <El, C extends AsserterComponent<El>> implements IsComponentBuilder<ModelComponentOptions<El, ModelAsserter, C>> {

  private final ComponentConstructor<El, C>  creator;

  private final In1Out1<El, C>  extractor;

  private final ModelComponentOptions<El, ModelAsserter, C> opts;

  public BuildAsserterComponent (ComponentConstructor<El, C> creator, In1Out1<El, C> extractor) {
    this.creator = creator;
    this.extractor = extractor;
    this.opts = new ModelComponentOptions<>();
  }

  public AsserterComponent<El> build (Out1<Scope> scope) {
    final AsserterComponent[] component = {null};
    service().runInScopeNoRelease(scope.out1(), s->{
      component[0] = creator.constructComponent(opts, extractor);
    });
    return component[0];
  }

  public final AsserterComponent<El> build () {
    return build(X_Scope::currentScope);
  }

  public final AsserterComponent<El> build (Scope scope) {
    return build(immutable1(scope));
  }

  public ModelComponentOptions<El, ModelAsserter, C> getOpts () {
    return opts;
  }

  public BuildAsserterComponent withModel (In1<ModelAsserter> callback) {
    opts.addModelListener(callback);
    return this;
  }

}
