package xapi.ui.edit;


import elemental.dom.Element;

import xapi.fu.Out1;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.component.AbstractModelComponent;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ModelComponentOptions;

public abstract class BaseInputTextComponent <El, ElBuilder extends NodeBuilder<El>> extends AbstractModelComponent<
    El,
    ModelInputText,
    InputTextComponent<El>
  > implements InputTextComponent<El> {

  public ElBuilder root;

  public ElBuilder title;

  public ElBuilder input;

  public BaseInputTextComponent (ModelComponentOptions<El, ModelInputText, InputTextComponent<El>> opts, ComponentConstructor<El, InputTextComponent<El>> ctor) {
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
