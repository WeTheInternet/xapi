package xapi.gwtc.view;

import java.util.LinkedHashSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;

public class ModuleSelectorView extends Composite{

  @UiTemplate("ModuleSelectorView.ui.xml")
  interface MyBinder extends UiBinder<HTMLPanel, ModuleSelectorView> {}
  static MyBinder binder = GWT.create(MyBinder.class);
  
  @UiField(provided=true)
  final GwtcResources res;
  
  private final LinkedHashSet<String> modules = new LinkedHashSet<String>();
  
  @UiField
  LabelElement modulesLabel;
  @UiField
  InputElement moduleAdder;
  @UiField
  Image addButton;
  
  @UiField
  DivElement selectedModules;

  @UiField
  HTMLPanel moduleTemplate;
  
  public ModuleSelectorView(GwtcResources res) {
    this.res = res;
    initWidget(binder.createAndBindUi(this));
  }
  
  @UiHandler({"addButton"})
  void onAdd(ClickEvent e) {
    String entered = moduleAdder.getValue();
    if (entered.length() > 0)
      addModule(entered);
    moduleAdder.setValue("");
  }

  public void addModule(final String entered) {
    if (modules.add(entered)) {
      final Element clone = Element.as(moduleTemplate.getElement().cloneNode(true));
      clone.removeClassName(res.css().hidden());
      Element el = clone.getFirstChildElement().getNextSiblingElement();
      el.setInnerText(entered);
      com.google.gwt.user.client.Element image = el.getNextSiblingElement().cast();
      DOM.setEventListener(image, new EventListener() {
        @Override
        public void onBrowserEvent(Event event) {
          modules.remove(entered);
          clone.removeFromParent();
        }
      });
      DOM.sinkEvents(image, Event.ONCLICK | Event.ONKEYDOWN);
      selectedModules.appendChild(clone);
    }
  }
  
  public Iterable<String> getModules() {
    return modules;
  }
  
}
