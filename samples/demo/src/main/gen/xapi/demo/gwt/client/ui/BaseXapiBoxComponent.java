package xapi.demo.gwt.client.ui;


import xapi.fu.Out1;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.component.AbstractModelComponent;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ModelComponentOptions;

public abstract class BaseXapiBoxComponent <El, ElBuilder extends NodeBuilder<El>> extends AbstractModelComponent<
    El,
    ModelXapiBox,
    XapiBoxComponent<El>
  > implements XapiBoxComponent<El> {

  public BaseXapiBoxComponent (ModelComponentOptions<El, ModelXapiBox, XapiBoxComponent<El>> opts, ComponentConstructor<El, XapiBoxComponent<El>> ctor) {
    super(opts, ctor);
  }

  public BaseXapiBoxComponent (Out1<El> El) {
    super(El);
  }

  public BaseXapiBoxComponent (El El) {
    super(El);
  }

  public ElBuilder toDom () {
    ElBuilder root = newBuilder();
    if (getModel().getTitle() != null) {
      ElBuilder elXapiTitle = newBuilder();
      elXapiTitle.append("<xapi-title>");
      elXapiTitle.append(getModel().getTitle());
      elXapiTitle.append("</xapi-title>");
      root.addChild(elXapiTitle);
    }
    for (ModelXapiText child : getModel().getText().forEach()) {
      ElBuilder elXapiText = createXapiTextComponent(child);
      root.addChild(elXapiText);
    }
    return root;
  }

  public abstract ElBuilder newBuilder () ;

  public abstract ElBuilder createXapiTextComponent (ModelXapiText text) ;

}
