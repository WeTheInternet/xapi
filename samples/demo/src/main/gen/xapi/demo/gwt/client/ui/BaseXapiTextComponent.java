package xapi.demo.gwt.client.ui;


import elemental.dom.Element;

import xapi.fu.Out1;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.component.AbstractModelComponent;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ModelComponentOptions;

public abstract class BaseXapiTextComponent <El, ElBuilder extends NodeBuilder<El>> extends AbstractModelComponent<
    El,
    ModelXapiText,
    XapiTextComponent<El>
  > implements XapiTextComponent<El> {

  public ElBuilder text;

  public BaseXapiTextComponent (ModelComponentOptions<El, ModelXapiText, XapiTextComponent<El>> opts, ComponentConstructor<El, XapiTextComponent<El>> ctor) {
    super(opts, ctor);
  }

  public BaseXapiTextComponent (Out1<El> El) {
    super(El);
  }

  public BaseXapiTextComponent (El El) {
    super(El);
  }

  public ElBuilder toDom () {
    text = createText();
    return text;
  }

  public abstract ElBuilder newBuilder () ;

  protected abstract ElBuilder createText () ;

}
