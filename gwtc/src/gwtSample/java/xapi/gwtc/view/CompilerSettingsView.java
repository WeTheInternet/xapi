package xapi.gwtc.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class CompilerSettingsView extends Composite{

  @UiTemplate("CompilerSettingsView.ui.xml")
  interface MyBinder extends UiBinder<HTMLPanel, CompilerSettingsView> {}
  static MyBinder binder = GWT.create(MyBinder.class);
  
  @UiField(provided=true)
  final GwtcResources res;
  @UiField
  ModuleSelectorView modules;
  
  @UiField
  LabelElement portLabel;
  @UiField
  InputElement port;

  public CompilerSettingsView(GwtcResources res) {
    this.res = res;
    
    initWidget(binder.createAndBindUi(this));
  }
  
  @UiFactory ModuleSelectorView createModuleSelector() {
    return new ModuleSelectorView(res);
  }
  
}
