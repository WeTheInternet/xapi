package xapi.demo.gwt.client.ui;


import elemental.dom.Element;

import xapi.fu.Out1;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.component.AbstractModelComponent;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ModelComponentOptions;

public abstract class BaseXapiTextComponent <Node, El extends Node, ElBuilder extends NodeBuilder<El>> extends AbstractModelComponent<
    Node,
    El,
    ModelXapiText,
    XapiTextComponent<Node, El>
  > implements XapiTextComponent<Node, El> {

  public ElBuilder text;

  public BaseXapiTextComponent (ModelComponentOptions<Node, El, ModelXapiText, XapiTextComponent<Node, El>> opts, ComponentConstructor<Node, El, XapiTextComponent<Node, El>> ctor) {
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
