package xapi.gwt.junit.gui;

import elemental.client.Browser;
import elemental.dom.Element;
import elemental.util.Timer;
import xapi.elemental.X_Elemental;
import xapi.elemental.api.PotentialNode;
import xapi.gwt.junit.impl.JUnit4Executor;
import xapi.util.impl.LazyProvider;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.shared.JsMemberPool;
import com.google.gwt.reflect.shared.ReflectUtil;

import javax.inject.Provider;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by james on 16/10/15.
 */
public abstract class JUnitGui {

  private static final String TEST_RESULTS = "test.result";
  private static final Throwable SUCCESS = new Throwable();
  private final Map<Method, Object> tests = new LinkedHashMap<>();
  private final Map<Class<?>, Method[]> testClasses = new LinkedHashMap<>();
  Map<Class<?>, Map<Method, Throwable>> testResults = new LinkedHashMap<>();

  /**
   * Performs a very primitive test that reflection works.
   * We need it to pull methods off classes and execute them. :-)
   */
  protected void sanityCheck() {
    try {
      String.class.getMethod("equals", Object.class).invoke("!", "!");
    } catch (final Exception e) {
      print(
          "Basic string reflection not working; expect failures...", e
      );
    }
  }

  protected void runAllTests() {
    sanityCheck();
    if (!loadWholeWorld()) {
      loadTests(true);
    } else {
      loadTests(false);
      ConstPool.loadConstPool(
          new Callback<ConstPool, Throwable>() {
            @Override
            public void onSuccess(final ConstPool result) {
              for (final JsMemberPool<?> m : result.getAllReflectionData()) {
                try {
                  final Class<?> c = m.getType();
                  if (!testClasses.containsKey(c)) {
                    addTests(c);
                  }
                } catch (final Throwable e) {
                  print("Error adding tests", e);
                }
              }
              loadTests(true);
            }

            @Override
            public void onFailure(final Throwable caught) {
              print("Error loading ConstPool", caught);
            }
          }
      );
    }
  }

  protected boolean loadWholeWorld() {
    // override this method to skip loading the ConstPool full of reflection data.
    return "wholeWorld".equals(System.getProperty("gwt.test.wholeWorld", "wholeWorld"));
  }

  private void loadTests(final boolean forReal) {
    if (forReal) {
      loadAllTests();
      displayTests();
      runTests();
    }
  }

  /**
   * @return <pre>
   *   new Class[]{
   *     GwtReflect.magicClass(MyTestClass.class),
   *     GwtReflect.magicClass(MyOtherClass.class),
   *   };
   * </pre>
   */
  protected abstract Class[] testClasses();

  protected void loadAllTests() {
    // A hook for subclasses to add arbitrary test cases
    for (Class<?> c : testClasses()) {
      try {
        addTests(c);
      } catch (Throwable e) {
        print("Failure loading test class " + c, e);
      }
    }
  }

  protected void addTests(Class<?> c) throws Throwable {
    final Method[] allTests = findTestMethods(c);
    addTests(c, allTests);
  }

  protected Method[] findTestMethods(Class<?> c) throws Throwable {
    return JUnit4Executor.findTests(c);
  }

  public void addTests(final Class<?> cls, Method[] allTests) throws Throwable {
    if (allTests.length > 0) {
      testClasses.putIfAbsent(cls, allTests);
      // TODO verify that it is correct to only instantiate one instance and share it across methods.
      final Object inst = instantiate(cls);
      for (final Method method : allTests) {
        tests.put(method, inst);
      }
    }
  }

  protected Object instantiate(Class<?> cls) throws Throwable {
    return cls.newInstance();
  }

  protected void displayTests() {
    final Element body = getInsertionPoint();

    for (final Class<?> c : testClasses.keySet()) {
      renderTest(body, c);
    }

  }

  private Element getInsertionPoint() {
    return Browser.getDocument().getBody();
  }

  private void renderTest(Element body, Class<?> c) {
    PotentialNode<Element> out = newClassBlock(c);

    out.getStyle()
        .setDisplay("inline-block")
        .setVerticalAlign("top")
        .setMaxHeight("90%")
        .setOverflowY("auto")
        .setMarginRight("2em")
    ;

    final String id = toId(c);
    buildHeader(out, c, id);

    try {
      final String path = c.getProtectionDomain().getCodeSource().getLocation().getPath();
      out.append("<sup><a href='file://" + path + "'>")
          .append(path)
          .append("</a></s.setInnerHTML(b.toString());up>");
    } catch (final Exception ignored) {}

    for (final Method m : testClasses.get(c)) {
      renderMethod(out, m);
    }

    appendGui(out);
  }

  protected void appendGui(PotentialNode<Element> out) {
    getInsertionPoint().appendChild(out.getElement());
  }

  private void renderMethod(PotentialNode<Element> out, Method m) {
    Class<?> c = m.getDeclaringClass();
    final String methodId = m.getName() + c.hashCode();

    final PotentialNode<Element> block = out.createChild("pre");

    final PotentialNode<Element> link = block
        .createChild("a");
    link
        .setHref("javascript:")
        .append(m.getName());

    block.append("(")
        .append(ReflectUtil.joinClasses(", ", m.getParameterTypes()))
        .append(")");

    block.createChild("div").setId(methodId).append(" ");

    link.onCreated(
        el ->
            el.addEventListener(
                "click",
                e -> runTest(m), false
            )
    );
  }

  protected PotentialNode<Element> newClassBlock(Class<?> c) {
    return new PotentialNode<>("div");
  }

  protected void buildHeader(PotentialNode<Element> b, Class<?> c, String id) {
    b.createChild("h3")
        .createChild("a")
        .setAttribute("id", id)
        .setAttribute("href", "#run:" + id)
        .onCreated(
            a -> a.addEventListener(
                "click", e -> {
                  runTests(c);
                }, false
            )
        )
        .append(c.getName())
    ;
    b.createChild("div")
        .setClass("results")
        .setAttribute("id", TEST_RESULTS + id)
    ;

  }

  private native void log(Object o)
  /*-{
      $wnd.console && $wnd.console.log(o);
  }-*/;

  private String toId(final Class<?> c) {
    return c.getName().replace('.', '_');
  }

  public void runTests() {
    int delay = 1;
    testResults.clear();
    for (final Method method : tests.keySet()) {
      Map<Method, Throwable> results = testResults.get(method.getDeclaringClass());
      if (results == null) {
        results = new LinkedHashMap<>();
        testResults.put(method.getDeclaringClass(), results);
      }
      results.put(method, null);
      scheduleRun(method, delay += 5);
    }
    testResults.keySet().forEach(this::updateTestClass);
  }

  public void runTests(Class<?> c) {
    final Map<Method, Throwable> res = testResults.get(c);

    for (final Method m : res.keySet().toArray(new Method[res.size()])) {
      res.put(m, null);
    }
    updateTestClass(c);

    for (final Method m : testClasses.get(c)) {
      Scheduler.get().scheduleDeferred(() -> runTest(m));
    }
  }

  private void scheduleRun(final Method method, int delay) {
    new Timer() {

      @Override
      public void run() {
        runTest(method);
      }
    }.schedule(delay);
  }

  protected Element elementForClass(Class<?> cls) {
    final String id = toId(cls);
    return Browser.getDocument().getElementById(TEST_RESULTS + id);
  }

  private void updateTestClass(final Class<?> cls) {
    final Element el = elementForClass(cls);
    final Map<Method, Throwable> results = testResults.get(cls);
    int success = 0, fail = 0;
    final int total = results.size();
    for (final Map.Entry<Method, Throwable> e : results.entrySet()) {
      if (e.getValue() != null) {
        if (e.getValue() == SUCCESS) {
          success++;
        } else {
          fail++;
        }
      }
    }
    final StringBuilder b = new StringBuilder("<span class='success'>Passed: ")
        .append(success).append("/").append(total).append("</span>; ")
        .append("<span");
    if (fail > 0) {
      b.append(" class='fail'");
    }
    b.append(">Failed: ").append(fail).append("/").append(total);
    el.setInnerHTML(b.toString());
  }

  protected void runTest(final Method m) {
    final String id = m.getName() + m.getDeclaringClass().hashCode();
    final Element stage = Browser.getDocument().getElementById(id);
    stage.setInnerHTML("Running...");
    final Map<Method, Throwable> results = testResults.get(m.getDeclaringClass());
    try {
      final Object inst = tests.get(m);
      JUnitGuiController controller;
      if (inst instanceof JUnitGuiController) {
        controller = (JUnitGuiController) inst;
      } else {
        controller = JUnitGuiController.DEFAULT_CONTROLLER;
      }
      Element[] view = new Element[0];
      final Provider<Element> stageProvider = () -> {
        view[0] = X_Elemental.newDiv();
        final Element result = initialize(view[0], controller, inst, m, id);
        if (result.getParentElement() == null) {
          stage.appendChild(result);
        }
        return result;
      };
      if (controller.onTestStart(LazyProvider.of(stageProvider), inst, m)) {
        //        final JUnitGuiExecution exe = newExecution(inst, m);
        //        controller.setExecution(exe);
        controller.runTest(
            inst, m, e -> {
              results.put(m, e == null ? SUCCESS : e);
              stage.setInnerHTML(debug("<div style='color:green'>" + m.getName() + " passes!</div>", null));

              updateTestClass(m.getDeclaringClass());
            }
        );
      }
    } catch (Throwable e) {
      results.put(m, e);
    } finally {
    }
  }

  /**
   * This method is here to let subclasses pick what element, if any,
   * that we want to render for our execution.
   */
  protected Element initialize(
      Element element,
      JUnitGuiController controller,
      Object inst,
      Method method,
      String id
  ) {
    return element == null ? X_Elemental.newDiv() : element;
  }

  protected boolean shouldUnwrap(Throwable e) {
    return e.getClass() == RuntimeException.class;
  }

  protected void print(final Object string, final Throwable e) {
    final Element el = Browser.getDocument().createDivElement();
    el.setInnerHTML(debug(string, e));
    insertionPoint().appendChild(el);
  }

  protected Element insertionPoint() {
    return Browser.getDocument().getBody();
  }

  protected String debug(final Object message, Throwable e) {
    final StringBuilder b = new StringBuilder();
    b.append(message);
    b.append('\n');
    b.append("<pre style='color:red;'>");
    while (e != null) {
      b.append(e);
      b.append('\n');
      for (final StackTraceElement trace : e.getStackTrace()) {
        b.append('\t')
            .append(trace.getClassName())
            .append('.')
            .append(trace.getMethodName())
            .append(' ')
            .append(trace.getFileName())
            .append(':')
            .append(trace.getLineNumber())
            .append('\n');
      }
      e = e.getCause();
    }
    b.append("</pre>");

    log(message);
    log(e);

    return b.toString();
  }

}
