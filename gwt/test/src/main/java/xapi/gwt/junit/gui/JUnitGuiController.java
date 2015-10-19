package xapi.gwt.junit.gui;

import elemental.dom.Element;
import xapi.gwt.junit.impl.JUnit4Executor;
import xapi.log.X_Log;
import xapi.util.api.ProvidesValue;

import com.google.gwt.reflect.shared.GwtReflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Predicate;

/**
 * Created by james on 18/10/15.
 */
public class JUnitGuiController extends JUnit4Executor {

  public static final JUnitGuiController DEFAULT_CONTROLLER = new JUnitGuiController();

  private Object currentTest; // The current test instance.
  private ProvidesValue<Element> stageProvider;

  public Element getStage() {
    assert stageProvider != null : "Call .onTestState() before calling .getStage() in "+getClass()+" "+this;
    return stageProvider.get();
  }

  /**
   * Called whenever a test starts.
   *
   * @return false to skip the test.
   */
  protected boolean onTestStart(ProvidesValue<Element> stageProvider, Object inst, Method method){
    this.stageProvider = stageProvider;
    currentTest = inst;
    findAndSetField(JUnitGuiController.class::isAssignableFrom, this, inst, false);
    return true;
  }

  private void findAndSetField(Predicate<Class> matcher, Object value, Object inst, boolean forceSet) {
    Class<?> declaringClass = inst.getClass();
    Field[] fields = GwtReflect.getPublicFields(declaringClass);
    for (Field field : fields) {
      if (matcher.test(field.getType())) {
        try {
          if (forceSet || field.get(inst) == null) {
            field.set(inst, value);
          }
        } catch (Throwable e) {
          X_Log.warn(getClass(), "findAndSetField for "+matcher+" on "+declaringClass+" ("+inst+") encountered an error", e);
        }
      }
    }
    while (declaringClass != null && declaringClass != Object.class) {
      fields = GwtReflect.getDeclaredFields(declaringClass);
      for (Field field : fields) {
        if (matcher.test(field.getType())) {
          try {
            field.setAccessible(true);
            if (forceSet || field.get(inst) == null) {
              field.set(inst, value);
            }
          } catch (Throwable e) {
            X_Log.warn(getClass(), "findAndSetField for "+matcher+" on "+declaringClass+" ("+inst+") encountered an error", e);
          }
        }
      }
      declaringClass = declaringClass.getSuperclass();
    }
  }

  protected void setExecution(JUnitGuiExecution execution) {
    execution.autoClean();
    findAndSetField(JUnitGuiExecution.class::isAssignableFrom, execution, currentTest, true);
  }

}
