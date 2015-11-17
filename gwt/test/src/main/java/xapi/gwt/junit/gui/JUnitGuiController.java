package xapi.gwt.junit.gui;

import elemental.dom.Element;
import xapi.gwt.junit.api.JUnitExecution;
import xapi.gwt.junit.impl.JUnit4Executor;

import javax.inject.Provider;

/**
 * Created by james on 18/10/15.
 */
public class JUnitGuiController extends JUnit4Executor {

  private Object currentTest; // The current test instance.
  private Provider<Element> stageProvider;
  private final Runnable updater;

  public JUnitGuiController(Runnable updater) {this.updater = updater;}

  public Element getStage() {
    assert stageProvider != null : "Call .onTestState() before calling .getStage() in "+getClass()+" "+this;
    return stageProvider.get();
  }

  /**
   * Called whenever a test starts.
   *
   * @return false to skip the test.
   */
  protected boolean onTestStart(Provider<Element> stageProvider, Object inst){
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

}
