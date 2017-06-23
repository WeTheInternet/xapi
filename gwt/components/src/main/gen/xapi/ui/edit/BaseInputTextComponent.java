package xapi.ui.edit;


import elemental.dom.Element;

import xapi.fu.Out1;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.component.AbstractModelComponent;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ModelComponentOptions;

public abstract class BaseInputTextComponent <Node, El extends Node, ElBuilder extends NodeBuilder<El>> extends AbstractModelComponent<
    Node,
    El,
    ModelInputText,
    InputTextComponent<Node, El>
  > implements InputTextComponent<Node, El> {

  public ElBuilder root;

  public ElBuilder title;

  public ElBuilder input;

  public BaseInputTextComponent (ModelComponentOptions<Node, El, ModelInputText, InputTextComponent<Node, El>> opts, ComponentConstructor<Node, El, InputTextComponent<Node, El>> ctor) {
    super(opts, ctor);
  }

  public BaseInputTextComponent (Out1<El> El) {
    super(El);
  }

  public BaseInputTextComponent (El El) {
    super(El);
  }

  public ElBuilder toDom () {
    ElBuilder root = newBuilder();
    root.append("<box>");
    if (getModel().getTitle() != null) {
      ElBuilder title = newBuilder();
      title.append("<label>");
      title.append(getModel().getTitle());
      title.append("</label>");
      root.addChild(title);
    }
    input = createInput();
    root.addChild(input);
    root.append("</box>");
    return root;
  }

  public abstract ElBuilder newBuilder () ;

  protected abstract ElBuilder createInput () ;

  public BaseInputTextComponent ui() {
    return this;
  }

}
