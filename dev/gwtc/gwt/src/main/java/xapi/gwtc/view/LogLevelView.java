package xapi.gwtc.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class LogLevelView extends Composite{

  @UiTemplate("LogLevelView.ui.xml")
  interface MyBinder extends UiBinder<HTMLPanel, LogLevelView> {}
  static MyBinder binder = GWT.create(MyBinder.class);

  @UiField(provided=true) final GwtcResources res;
  
  @UiField InputElement radioLogAll;
  @UiField LabelElement labelLogAll;
  @UiField InputElement radioLogSpam;
  @UiField LabelElement labelLogSpam;
  @UiField InputElement radioLogDebug;
  @UiField LabelElement labelLogDebug;
  @UiField InputElement radioLogTrace;
  @UiField LabelElement labelLogTrace;
  @UiField InputElement radioLogInfo;
  @UiField LabelElement labelLogInfo;
  @UiField InputElement radioLogWarn;
  @UiField LabelElement labelLogWarn;
  @UiField InputElement radioLogError;
  @UiField LabelElement labelLogError;
  
  public LogLevelView(GwtcResources res) {
    this.res = res == null ? GwtcResources.DEFAULT : res;
    initWidget(binder.createAndBindUi(this));
    
    setLabelFor(radioLogAll, labelLogAll);
    setLabelFor(radioLogSpam, labelLogSpam);
    setLabelFor(radioLogDebug, labelLogDebug);
    setLabelFor(radioLogTrace, labelLogTrace);
    setLabelFor(radioLogWarn, labelLogWarn);
    setLabelFor(radioLogInfo, labelLogInfo);
    setLabelFor(radioLogError, labelLogError);
    
    
  }

  private void setLabelFor(InputElement radio, LabelElement label) {
    if (radio.getId().length()==0)
      radio.setId(DOM.createUniqueId());
    label.setHtmlFor(radio.getId());
  }
  
}
