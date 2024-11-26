package xapi.ui.layout;


import elemental.dom.Element;

import xapi.fu.Out1;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.component.AbstractModelComponent;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ModelComponentOptions;

public abstract class BaseBoxComponent <El, ElBuilder extends NodeBuilder<El>> extends AbstractModelComponent<
    El,
    ModelBox,
    BoxComponent<El>
  > implements BoxComponent<El> {

  public ElBuilder root;

  public BaseBoxComponent (ModelComponentOptions<El, ModelBox, BoxComponent<El>> opts, ComponentConstructor<El, BoxComponent<El>> ctor) {
    super(opts, ctor);
  }

  public BaseBoxComponent (Out1<El> El) {
    super(El);
  }

  public BaseBoxComponent (El El) {
    super(El);
  }

  public ElBuilder toDom () {
    root = createRoot();
    return root;
  }

  public abstract ElBuilder newBuilder () ;

  protected abstract ElBuilder createRoot () ;

}
