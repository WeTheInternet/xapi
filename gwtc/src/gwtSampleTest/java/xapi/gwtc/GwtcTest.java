package xapi.gwtc;

import static xapi.gwtc.view.GwtcResources.DEFAULT;
import xapi.gwtc.view.GwtCompilerView;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;

public class GwtcTest implements EntryPoint {

  @Override
  public void onModuleLoad() {
    DEFAULT.css().ensureInjected();
    
    GwtCompilerView view = new GwtCompilerView(DEFAULT);
    view.setWidth("350px");
    view.getElement().getStyle().setProperty("margin", "auto");
    HorizontalPanel wrapper = new HorizontalPanel();
    wrapper.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
    wrapper.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
    wrapper.add(view);
    wrapper.setSize("100%", "100%");
    RootPanel.get().add(wrapper);
    
    ResizeHandler handler = new ResizeHandler() {
      
      @Override
      public void onResize(ResizeEvent event) {
        RootPanel.get().setSize(
            Window.getClientWidth()+"px", 
            Window.getClientHeight()+"px");
      }
    };
    handler.onResize(null);
    Window.addResizeHandler(handler);
    
  }

}
