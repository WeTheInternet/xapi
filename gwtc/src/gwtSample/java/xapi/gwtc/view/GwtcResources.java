package xapi.gwtc.view;

//import xapi.gui.gwt.resources.BasicResources;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ClientBundle.Source;
public interface GwtcResources extends ClientBundle {

  GwtcResources DEFAULT = GWT.create(GwtcResources.class);

  interface Css extends CssResource {
    String hidden();
  }

  @Source({ "Gwtc.css" })
  Css css();

  ImageResource gwtLogo();
  
  @ImageOptions(height=50,width=50)
  @Source("gwtLogo.png")
  ImageResource gwtLogoSmall();

  @ImageOptions(height=20,width=20)
  @Source("plus.png")
  ImageResource plus();
  
  @ImageOptions(height=20,width=20)
  @Source("minus.png")
  ImageResource minus();

}
