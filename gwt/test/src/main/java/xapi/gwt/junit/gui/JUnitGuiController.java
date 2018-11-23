package xapi.gwt.junit.gui;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.dom.Node;
import xapi.elemental.X_Elemental;
import xapi.fu.Out1;
import xapi.gwt.junit.api.JUnitExecution;
import xapi.gwt.junit.impl.JUnit4Executor;

import javax.inject.Provider;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Created by james on 18/10/15.
 */
public class JUnitGuiController extends JUnit4Executor {

  private Object currentTest; // The current test instance.
  private Out1<Element> stageProvider;
  private final Runnable updater;

  public JUnitGuiController(Runnable updater) {this.updater = updater;}

  public Element getStage() {
    if (execution instanceof JUnitGuiExecution) {
      return ((JUnitGuiExecution)execution).getStage();
    }
    assert stageProvider != null : "Call .onTestState() before calling .getStage() in "+getClass()+" "+this;
    return stageProvider.out1();
  }

  /**
   * Called whenever a test starts.
   *
   * @return false to skip the test.
   */
  protected boolean onTestClassStart(Out1<Element> stageProvider, Object inst){
    this.stageProvider = stageProvider;
    currentTest = inst;
    findAndSetField(JUnitGuiController.class::isAssignableFrom, this, inst, false);
    return true;
  }

  @Override
  protected JUnitExecution newExecution() {
    final JUnitGuiExecution exe = new JUnitGuiExecution(stageProvider, updater);
    return exe;
  }

  public void setFullscreen() {
    setFullscreen(null);
  }

  public void setFullscreen(BooleanSupplier delayFinish) {

    final Element stage = getStage();
    final Element parent = stage.getParentElement();
    final Node after = stage.getNextSibling();

    X_Elemental.addClassName(stage, "fullscreen");
    String oldWidth = stage.getStyle().getWidth();
    String oldHeight = stage.getStyle().getHeight();

    stage.getStyle().setWidth(Browser.getWindow().getInnerWidth()+"px");
    stage.getStyle().setHeight(Browser.getWindow().getInnerHeight()+"px");

    Browser.getDocument().getBody().appendChild(stage);

    execution.onBeforeFinished(delayFinish);

    execution.onFinished((method, error) ->{
          if (after == null) {
            parent.appendChild(stage);
          } else {
            parent.insertBefore(stage, after);
          }
          if (oldWidth == null) {
            stage.getStyle().clearWidth();
          } else {
            stage.getStyle().setWidth(oldWidth);
          }
          if (oldHeight == null) {
            stage.getStyle().clearHeight();
          } else {
            stage.getStyle().setHeight(oldHeight);
          }
          X_Elemental.removeClassName(stage, "fullscreen");
          return null;
    });
  }

  public void onTestClassFinish(Object inst, Map<Method, Throwable> fin) {

  }
}
