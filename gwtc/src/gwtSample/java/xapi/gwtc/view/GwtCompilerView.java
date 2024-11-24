package xapi.gwtc.view;

//import xapi.gui.gwt.widget.GwtListView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class GwtCompilerView extends Composite{

  @UiTemplate("GwtCompilerView.ui.xml")
  interface MyBinder extends UiBinder<HTMLPanel, GwtCompilerView> {}
  static MyBinder binder = GWT.create(MyBinder.class);
  
  @UiField(provided=true)
  final GwtcResources res;
  @UiField
  CompilerSettingsView settings;
  @UiField
  LogLevelView logLevel;
//  @UiField
//  GwtListView<String> classpath;
  
  public GwtCompilerView(GwtcResources res) {
    this.res = res;
    initWidget(binder.createAndBindUi(this));
//    classpath.addItem("one");
//    classpath.addItem("two");
//    classpath.addItem("three");
//    classpath.addItem("four");
  }
  
  @UiFactory CompilerSettingsView createCompilerSettings() {
    return new CompilerSettingsView(res);
  }
  
  @UiFactory LogLevelView createLogLevel() {
    return new LogLevelView(res);
  }
  
}
