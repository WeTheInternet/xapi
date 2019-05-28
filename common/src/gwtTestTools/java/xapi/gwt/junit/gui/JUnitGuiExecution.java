package xapi.gwt.junit.gui;

import elemental.client.Browser;
import elemental.dom.Element;
import xapi.elemental.X_Elemental;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.lazy.ResettableLazy;
import xapi.gwt.junit.api.JUnitExecution;
import xapi.gwt.junit.impl.JUnit4Executor;
import xapi.util.api.ProvidesValue;

import javax.inject.Provider;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * Created by james on 18/10/15.
 */
public class JUnitGuiExecution extends JUnitExecution<JUnitGuiController> {

  private ResettableLazy<Element> stageProvider;
  private Element stageRoot;

  public JUnitGuiExecution(Out1<Element> stageProvider, Runnable update) {
    this.stageProvider = new ResettableLazy<Element>(stageProvider);
    Element[] running = new Element[1];
    onStartMethod(
        m -> {
          final String id = m.getName() + m.getDeclaringClass().hashCode();
          stageRoot = Browser.getDocument().getElementById(id);
          running[0] = X_Elemental.newDiv();
          running[0].setInnerHTML("Running...");
          stageRoot.setInnerHTML("");
          stageRoot.appendChild(running[0]);
          return null;
        }
    );
    onFinished(
        (m, t) -> {
          final String id = m.getName() + m.getDeclaringClass().hashCode();
          final Element stage = Browser.getDocument().getElementById(id);
          if (running[0].getParentElement() == stage) {
            stage.removeChild(running[0]);
            running[0] = null;
          }
          final Element result = X_Elemental.newDiv();

          if (t == JUnit4Executor.SUCCESS) {
            result.setInnerHTML("<div style='color:green'>" + m.getName() + " passes!</div>");
          } else {
            result.setInnerHTML(
                JUnit4Executor.debug("<div style='color:red'>" + m.getName() + " fails!</div>", t)
            );
          }
          stage.appendChild(result);
          if (update != null) {
            update.run();
          }
          return null;
        }
    );
  }

  @Override
  public Iterable<Callable<Boolean>> startMethod(Method method) {
    stageProvider.reset();
    return super.startMethod(method);
  }

  public Element getStage() {
    final Element stage = stageProvider.out1();
    if (stage.getParentElement() == null) {
      stageRoot.appendChild(stage);
    }
    return stage;
  }

}
