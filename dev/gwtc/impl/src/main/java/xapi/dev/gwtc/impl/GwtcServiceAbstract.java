package xapi.dev.gwtc.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.Dependency.DependencyType;
import xapi.annotation.compile.DependencyBuilder;
import xapi.annotation.compile.Resource;
import xapi.annotation.compile.ResourceBuilder;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.XmlBuffer;
import xapi.file.X_File;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.util.X_Debug;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.tools.GWTTestSuite;

public abstract class GwtcServiceAbstract implements GwtcService {

  protected class Replacement {
    protected String newValue;
    protected int start, end;

    public Replacement(int start, int end, String path) {
      this.start = start;
      this.end = end;
      newValue = path;
    }
  }

  protected static final String GEN_PREFIX = "__gen";
  protected final GwtcContext context;
  protected final String genName;
  protected final File tempDir;
  protected SourceBuilder<GwtcService> entryPoint;
  protected final Set<String> finished;
  protected GwtcEntryPointBuilder out;
  private boolean needsReportError = true;
  protected String manifestName;

  public GwtcServiceAbstract(ClassLoader resourceLoader) {
    context = new GwtcContext(this, resourceLoader);

    finished = new HashSet<String>();
    genName = context.getGenName();
    tempDir = X_File.createTempDir(genName);
    String qualifiedName = GEN_PREFIX+"."+genName;

    entryPoint = new SourceBuilder<GwtcService>("public class "+ genName).setPackage(GEN_PREFIX);
    out = new GwtcEntryPointBuilder(entryPoint);

    context.setEntryPoint(qualifiedName);
    context.addGwtXmlSource(GEN_PREFIX);

  }


  @Override
  public void addClass(Class<?> clazz) {
    if (!finished.add(clazz.getName())) {
      return;
    }
    addGwtModules(clazz);
    if (EntryPoint.class.isAssignableFrom(clazz)) {
      try {
        addMethod(clazz.getMethod("onModuleLoad"));
      } catch (Exception e) {
        X_Log.error(getClass(), "Could not extract onModuleLoad method from ", clazz, e);
      }
    } else if (GWTTestCase.class.isAssignableFrom(clazz)) {
      addGwtTestCase(clazz.asSubclass(GWTTestCase.class));
    } else if (GWTTestSuite.class.isAssignableFrom(clazz)) {
      addGwtTestSuite(clazz.asSubclass(GWTTestSuite.class));
    } else if (RunAsyncCallback.class.isAssignableFrom(clazz)) {
      addAsyncBlock(clazz.asSubclass(RunAsyncCallback.class));
    } else {
      // Check if this class has methods annotated w/ junit @Test
      for (Method m : clazz.getMethods()) {
        if (m.getAnnotation(Test.class) != null) {
          finished.remove(clazz.getName());
          addJUnitClass(clazz);
          return;
        }
      }
      try {
        addMethod(clazz.getMethod("main", String[].class));
      } catch (Exception ignored){
        X_Log.warn(getClass(), "Class",clazz," was added to Gwtc, "
            + "but that class was not a subclass of EntryPoint, RunAsync,"
            + " GWTTestCase, GWTTestSuite, nor did it have a main method, or"
            + " any JUnit 4 annotated @Test method.");
      }
    }
  }

  @Override
  public void addGwtModules(Class<?> clazz) {
    context.addClass(clazz);
  }

  public String getEntryPoint() {
    return entryPoint.toString();
  }

  public String getGwtXml() {
    return context.getGwtXml().toString();
  }

  @Override
  public String getModuleName() {
    return genName;
  }

  protected void generateWar(String warDir, String moduleName) {
    final XmlBuffer
      buffer = new XmlBuffer("html"),
      head = buffer.makeTag("head"),
      body= buffer.makeTag("body")
    ;
    buffer.printBefore(getHostPageDocType());

    context.generateAll(new File(warDir), moduleName, head, body);

    head.makeTag("script")
    .setAttribute("type", "text/javascript")
    .setAttribute("src", getScriptLocation(moduleName));

    String hostPage = buffer.toString();
    X_File.saveFile(warDir +"/public", "index.html", hostPage);
    info("Generated host page:\n"+hostPage);
    X_Log.info("Generate war into ", warDir);
  }

  protected String getHostPageDocType() {
    return "<!doctype html>\n";
  }

  private String getScriptLocation(String genName) {
    return //genName+"/"+
          genName+".nocache.js";
  }


  protected void doLog(LogLevel level, String msg) {
    X_Log.log(level, msg);
  }

  @Override
  public File getTempDir() {
    return tempDir;
  }

  public void error(String log) {
    doLog(LogLevel.ERROR, log);
  }

  public void warn(String log) {
    doLog(LogLevel.WARN, log);
  }

  public void info(String log) {
    doLog(LogLevel.INFO, log);
  }

  public void trace(String log) {
    doLog(LogLevel.TRACE, log);
  }

  public void debug(String log) {
    doLog(LogLevel.DEBUG, log);
  }


  public void addDependency(Dependency dep) {
    context.addDependency(dep);
  }

  @Override
  public boolean addJUnitClass(Class<?> clazz) {
    inheritGwtXml(clazz, ResourceBuilder.buildResource("org.junit.JUnit4").build());
    X_Log.info(getClass(), "Adding class", clazz," to junit 4 module");
    addDependency(
        DependencyBuilder.buildDependency("gwt-reflect")
        .setGroupId("net.wetheinter")
        .setVersion("2.7.0")
        .setClassifier("tests")
        .setDependencyType(DependencyType.MAVEN)
        .build()
        );
    List<Method>
      beforeClass = new ArrayList<>(),
      before = new ArrayList<>(),
      test = new ArrayList<>(),
      after = new ArrayList<>(),
      afterClass = new ArrayList<>()
    ;
    List<Class<?>> hierarchy = new LinkedList<>();
    {
      Class<?> c = clazz;
      while (c != Object.class) {
        hierarchy.add(0, c);
        c = c.getSuperclass();
      }
    }
    for (Class<?> c : hierarchy) {
      for (Method method : c.getMethods()) {
        if (method.getAnnotation(Test.class) != null) {
          test.add(method);
        } else if (method.getAnnotation(Before.class) != null) {
          before.add(method);
        } else if (method.getAnnotation(BeforeClass.class) != null) {
          beforeClass.add(method);
        } else if (method.getAnnotation(After.class) != null) {
          after.add(method);
        } else if (method.getAnnotation(AfterClass.class) != null) {
          afterClass.add(method);
        }
      }
    }
    out.println(generateTestRunner(clazz, beforeClass, before, test, after, afterClass));
    return true;
  }

  protected void inheritGwtXml(Class<?> clazz, Resource build) {
    context.addGwtXmlInherit(build.value());
  }

  protected ClassBuffer classBuffer() {
    return entryPoint.getClassBuffer();
  }


  protected String extractGwtVersion(String gwtHome) {
    int lastInd = gwtHome.lastIndexOf("gwt-dev");
    gwtHome = gwtHome.substring(lastInd+7).replace(".jar", "");
    return gwtHome.startsWith("-") ? gwtHome.substring(1) : gwtHome;
  }

  protected void generateReportError(ClassBuffer classBuffer) {
    classBuffer.createMethod("private static void reportError"
        + "(Class<?> clazz, String method, Throwable e)")
        .println("String error = method+\" failed test\";")
        .println("System.err.println(error);")
        .println("e.printStackTrace();");
  }

  protected String generateSetupMethod(String field, String type, Class<?> clazz, List<Method> befores) {
    if (befores.isEmpty()) {
      return "";
    }
    String methodName = type+"TestRun_"+field;
    String simpleName = entryPoint.getImports().addImport(clazz);
    MethodBuffer runner = entryPoint.getClassBuffer().createMethod(
        "private final void "+methodName+"("+simpleName+" field)");

    for (Method before : befores) {
      runner.println("field."+before.getName()+"();");
    }

    return methodName;
  }

  protected String generateTestRunner(Class<?> clazz, List<Method> beforeClasses,
      List<Method> befores, List<Method> tests, List<Method> afters,
      List<Method> afterClasses) {
    String field = out.formatInstanceProvider(clazz);
    String methodName = "testRun_"+field;

    MethodBuffer runner = classBuffer().createMethod("private final void "+methodName);
    // Setup
    for (Method beforeClass : beforeClasses) {
      String shortName = runner.addImport(beforeClass.getDeclaringClass());
      runner.println(shortName+"."+beforeClass.getName()+"();");
    }
    String shortName = runner.addImport(clazz);
    String beforeMethod = generateSetupMethod(field, "before", clazz, befores);
    String afterMethod = generateSetupMethod(field, "after", clazz, afters);
    for (Method test : tests) {
      String testName = methodName+"_"+test.getName();
      MethodBuffer testRunner = entryPoint.getClassBuffer().createMethod
          ("private final void "+testName);
      if (!beforeMethod.isEmpty()) {
        testRunner.println(beforeMethod+"("+field+");");
      }
      Test testAnno = test.getAnnotation(Test.class);
      testRunner.startTry();
      testRunner.println(field+"."+test.getName()+"();");
      if (testAnno.expected() != Test.None.class) {
        String assertClass = testRunner.addImport(Assert.class);
        String exceptionType = testRunner.addImport(testAnno.expected());
        testRunner.println(assertClass+".fail(\""
            + "expected exception "+exceptionType+ "\");");
        testRunner.startCatch(exceptionType);
      }
      String errorName = testRunner.startCatch("Throwable");
      ensureReportError();
      testRunner.println("reportError("+shortName+".class,"
          + "\""+test.toGenericString()+"\", "+errorName+ ");");
      if (!afterMethod.isEmpty()) {
        testRunner.startFinally();
        testRunner.println(afterMethod+"("+field+");");
      }
      testRunner.endTry();
      runner.println(testName+"();");
    }
    for (Method afterClass : afterClasses) {
      shortName = runner.addImport(afterClass.getDeclaringClass());
      runner.println(shortName+"."+afterClass.getName()+"();");
    }

    return methodName+"();";
  }

  protected void ensureReportError() {
    if (needsReportError) {
      needsReportError = false;
      generateReportError(entryPoint.getClassBuffer());
    }
  }


  public void copyModuleTo(String module) {
    String from = manifestName == null ? genName : manifestName;
    File f = new File(tempDir, from+".gwt.xml");
    if (f.exists()) {
      int ind = module.lastIndexOf('.');
      String path;
      if (ind == -1) {
        path = "";
      } else {
        path = module.substring(0, ind).replace('.', '/');
      }
      f = new File(tempDir, path);
      f.mkdirs();
      saveGwtXmlFile(context.getGwtXml(), module.substring(ind+1), f);
    }
  }

  protected void saveGwtXmlFile(XmlBuffer xml, String moduleName, File dest) {
    saveTempFile(GwtcXmlBuilder.HEADER+xml, new File(dest,moduleName+".gwt.xml"));
  }

  protected void saveTempFile(String value, File dest) {
    X_Log.trace(getClass(), "saving generated file to",dest);
    dest.getParentFile().mkdirs();
    try (FileWriter out = new FileWriter(dest);) {
      out.append(value);
      out.close();
    } catch (IOException e) {
      X_Log.warn(getClass(), "Error saving generated file ",dest,"\n"+value);
      throw X_Debug.rethrow(e);
    }
  }

}
