package xapi.demo.gwt.client.ui;


import xapi.fu.Out1;
import xapi.model.api.ModelKey;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.component.AbstractModelComponent;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ModelComponentOptions;

public abstract class BaseXapiSlideComponent <El, ElBuilder extends NodeBuilder<El>> extends AbstractModelComponent<
    El,
    ModelXapiSlide,
    XapiSlideComponent<El>
  > implements XapiSlideComponent<El> {

  public ElBuilder root;

  public BaseXapiSlideComponent (ModelComponentOptions<El, ModelXapiSlide, XapiSlideComponent<El>> opts, ComponentConstructor<El, XapiSlideComponent<El>> ctor) {
    super(opts, ctor);
  }

  public BaseXapiSlideComponent (Out1<El> El) {
    super(El);
  }

  public BaseXapiSlideComponent (El El) {
    super(El);
  }

  public ElBuilder toDom () {
    ElBuilder root = newBuilder();
    root.append("<box>");
    if (getModel().getTitle() != null) {
      ElBuilder elH2 = newBuilder();
      elH2.append("<h2>");
      elH2.append(getModel().getTitle());
      elH2.append("</h2>");
      root.addChild(elH2);
    }
    for (ModelKey child : getModel().getItems().forEach()) {
      ElBuilder elXapiBox = createXapiBoxComponent(child);
      root.addChild(elXapiBox);
    }
    root.append("</box>");
    return root;
  }

  public abstract ElBuilder newBuilder () ;

  public abstract ElBuilder createXapiBoxComponent (ModelKey items) ;

}
