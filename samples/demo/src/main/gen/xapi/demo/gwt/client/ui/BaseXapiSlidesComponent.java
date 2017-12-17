package xapi.demo.gwt.client.ui;


import xapi.fu.Out1;
import xapi.ui.api.NodeBuilder;
import xapi.ui.api.component.AbstractModelComponent;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ModelComponentOptions;

public abstract class BaseXapiSlidesComponent <El, ElBuilder extends NodeBuilder<El>> extends AbstractModelComponent<
    El,
    ModelXapiSlides,
    XapiSlidesComponent<El>
  > implements XapiSlidesComponent<El> {

  public ElBuilder root;

  public BaseXapiSlidesComponent (ModelComponentOptions<El, ModelXapiSlides, XapiSlidesComponent<El>> opts, ComponentConstructor<El, XapiSlidesComponent<El>> ctor) {
    super(opts, ctor);
  }

  public BaseXapiSlidesComponent (Out1<El> El) {
    super(El);
  }

  public BaseXapiSlidesComponent (El El) {
    super(El);
  }

  public ElBuilder toDom () {
    ElBuilder root = newBuilder();
    root.append("<xapi-box>");
    ElBuilder elXapiSlide = createXapiSlideComponent(getModel().getShowing());
    root.addChild(elXapiSlide);
    root.append("</xapi-box>");
    return root;
  }

  public abstract ElBuilder newBuilder () ;

  public abstract ElBuilder createXapiSlideComponent (ModelXapiSlide showing) ;

}
