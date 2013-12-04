package xapi.dev.gwtc.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import xapi.annotation.inject.InstanceDefault;
import xapi.dev.X_Gwtc;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.XmlBuffer;
import xapi.file.X_File;
import xapi.gwtc.api.DefaultValue;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.Gwtc.AncestorMode;
import xapi.io.api.SimpleLineReader;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.shell.X_Shell;
import xapi.shell.api.ShellSession;
import xapi.source.X_Source;
import xapi.util.X_Debug;
import xapi.util.X_Properties;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.Compiler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.reflect.client.GwtReflect;

@Gwtc
@InstanceDefault(implFor=GwtcService.class)
public class GwtcServiceImpl implements GwtcService {
  
  private static final String GEN_PREFIX = "__gen";

  Pattern SPECIAL_DIRS = Pattern.compile(
      "("+Pattern.quote(Dependency.DIR_BIN)+")|"+
      "("+Pattern.quote(Dependency.DIR_TEMP)+")|"
  );
  
  private final SourceBuilder<GwtcService> entryPoint;
  private final String genName;
  private final XmlBuffer gwtXml;
  private final Set<Resource> gwtXmlDeps;
  private final Set<Dependency> dependencies;
  private final Set<Resource> htmlHead;
  private final Set<Resource> htmlBody;
  private final Set<Class<?>> finishedClasses;
  private final Set<Package> finishedPackages;
  private final Map<String, String> instanceFields;
  private boolean needsReportError = true;
  private final MethodBuffer out;
  private final File tempDir;
  private String binDir;

  public GwtcServiceImpl() {
    genName = "Gwtc"+hashCode();
    tempDir = X_File.createTempDir(genName);
    String qualifiedName = GEN_PREFIX+"."+genName;
        
    entryPoint = new SourceBuilder<GwtcService>("public class "+ genName).setPackage(GEN_PREFIX);
    out = entryPoint.getClassBuffer()
      .addInterface(EntryPoint.class)
      .createMethod("void onModuleLoad");
    gwtXml = new XmlBuffer("module")
      .setAttribute("rename-to", genName);
    gwtXml
      .makeTag("entry-point")
      .setAttribute("class", qualifiedName);
    gwtXml
      .makeTag("source")
      .setAttribute("path", GEN_PREFIX);
    instanceFields = new HashMap<String, String>();
    gwtXmlDeps = new LinkedHashSet<Resource>();
    htmlBody = new LinkedHashSet<Resource>();
    htmlHead = new LinkedHashSet<Resource>();
    dependencies = new LinkedHashSet<Dependency>();
    finishedClasses = new HashSet<Class<?>>();
    finishedPackages = new HashSet<Package>();
  }
  
  @Override
  public void addAsyncBlock(Class<? extends RunAsyncCallback> asSubclass) {
    
  }

  @Override
  public void addClass(Class<?> clazz) {
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
      try {
        addMethod(clazz.getMethod("main", String[].class));
      } catch (Exception ignored){
        // Check if this class has methods annotated w/ junit @Test
        for (Method m : clazz.getMethods()) {
          if (m.getAnnotation(Test.class) != null) {
            addJUnitClass(clazz);
            return;
          }
        }
        X_Log.warn(getClass(), "Class",clazz,"was added to Gwtc, "
            + "but that class was not a subclass of EntryPoint, RunAsync,"
            + " GWTTestCase, GWTTestSuite, nor did it have a main method, or"
            + " any JUnit 4 annotated @Test method.");
      }
    }
  }
  
  @Override
  public void addGwtModules(Class<?> clazz) {
    Gwtc gwtc = clazz.getAnnotation(Gwtc.class);
    Class<?> c = clazz;
    if (gwtc == null) {
      while (c != Object.class) {
        gwtc = c.getAnnotation(Gwtc.class);
        if (gwtc != null) {
          maybeAddAncestors(gwtc, c);
          break;
        }
        c = c.getSuperclass();
      }
    } else {
      maybeAddAncestors(gwtc, c);
    }
    Package pkg = clazz.getPackage();
    if (gwtc == null) {
      gwtc = pkg.getAnnotation(Gwtc.class);
      String parentName = pkg.getName();
      search:
      while (gwtc == null) {
        int ind = parentName.lastIndexOf('.');
        if (ind == -1) {
          break;
        }
        parentName = parentName.substring(0, ind);
        pkg = Package.getPackage(parentName);
        while (pkg == null) {
          ind = parentName.lastIndexOf('.');
          if (ind == -1) {
            X_Log.warn("No package found for ",clazz.getPackage(),"; aborting @Gwtc search");
            break search;
          }
          parentName = parentName.substring(0, ind);
          pkg = Package.getPackage(parentName);
        }
        gwtc = pkg.getAnnotation(Gwtc.class);
      }
      if (gwtc != null) {
        maybeAddAncestors(gwtc, pkg);
      }
    } else {
      maybeAddAncestors(gwtc, pkg);
    }
  }

  @Override
  public void addGwtTestCase(Class<? extends GWTTestCase> subclass) {
    
  }
  
  @Override
  public void addGwtTestSuite(Class<? extends GWTTestSuite> asSubclass) {
    
  }

  @Override
  public void addJUnitClass(Class<?> clazz) {
    addGwtXml(clazz, ResourceBuilder.buildResource("org.junit.JUnit4").build());
    X_Log.info(getClass(), "adding JUnit 4 class", clazz);
    dependencies.add(
        DependencyBuilder.buildDependency("gwt-reflect")
        .setGroupId("net.wetheinter")
        .setVersion("2.5.1-rc1")
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
  }

  @Override
  public void addMethod(Method method) {
    if (Modifier.isStatic(method.getModifiers())){ 
      // print a call to a static method
      out.println(formatStaticCall(method));
    } else {
      // print a call to an instance method; creating an instance if necessary.
      out.println(formatInstanceCall(method));
    }
  }

  @Override
  public void addPackage(Package pkg) {
    Gwtc gwtc = pkg.getAnnotation(Gwtc.class);
    if (gwtc != null) {
      addGwtcPackage(gwtc, pkg);
    }
  }

  @Override
  public int compile(GwtManifest manifest) {
    assert tempDir.exists() : "No usable directory "+tempDir.getAbsolutePath();
    X_Log.trace(getClass(), "Generated entry point", "\n"+getEntryPoint());
    X_Log.info(getClass(), "Generated module", "\n"+getGwtXml());
    saveGwtXmlFile(gwtXml, tempDir);
    saveTempFile(entryPoint.toString(), new File(tempDir, GEN_PREFIX+File.separator+ genName+".java"));
    if (manifest.getWarDir() == null) {
      File f = tempDir;
      try {
        manifest.setWarDir(f.getCanonicalPath());
      } catch (IOException e) {
        X_Log.warn("Unable to create temporary war directory for GWT compile",
            "You will likely get an unwanted war folder in the directory you executed this program");
        X_Debug.maybeRethrow(e);
      }
    }
    if (manifest.getUnitCacheDir() == null) {
      try {
        File f = X_File.createTempDir("gwtc-"+manifest.getModuleName()+"UnitCache");
        if (f != null) {
          manifest.setUnitCacheDir(f.getCanonicalPath());
        }
      } catch (IOException e) {
        X_Log.warn("Unable to create unit cache work directory for GWT compile",
            "You will likely get unwanted gwtUnitcache folders in the directory you executed this program");
      }
    }
    for (Dependency dependency : dependencies) {
      manifest.addDependency(resolveDependency(dependency));
    }
    String gwtHome = X_Properties.getProperty("gwt.home");
    if (gwtHome == null) {
      URL gwtHomeLocation = Compiler.class.getClassLoader().getResource(Compiler.class.getName().replace('.', '/')+".class");
      if (gwtHomeLocation == null) {
        X_Log.warn("Unable to find gwt home from System property gwt.home, "
            , "nor from looking up the gwt compiler class from classloader");
      } else {
        gwtHome = gwtHomeLocation.toExternalForm();
        if (gwtHome.contains("jar!")) {
          gwtHome = gwtHome.split("jar!")[0]+"jar";
        }
        gwtHome = gwtHome.replace("file:", "").replace("jar:", "");
        if (manifest.getGwtVersion().length() == 0) {
          if (gwtHome.contains("gwt-dev.jar")) {
            manifest.setGwtVersion("");
          } else {
            manifest.setGwtVersion(extractGwtVersion(gwtHome));
          }
        }
        int ind = gwtHome.lastIndexOf("gwt-dev");
        gwtHome = gwtHome.substring(0, ind-1);
      }
    }
    X_Log.info(getClass(), "Starting gwt compile", manifest.getModuleName());
    X_Log.trace(manifest);
    X_Log.trace("Args: java ", manifest.toJvmArgs(),manifest.toProgramArgs());
    X_Log.debug("Requested Classpath\n",manifest.toClasspathFullCompile(gwtHome));
    X_Log.debug("Runtime cp", ((URLClassLoader)getClass().getClassLoader()).getURLs());
    ShellSession controller 
      = X_Shell.launchJava(Compiler.class, manifest.toClasspathFullCompile(gwtHome), manifest.toJvmArgArray(), manifest.toProgramArgs().split("[ ]+"));
    controller.stdErr(new SimpleLineReader() {
      @Override
      public void onLine(String errLog) {
        doLog(errLog, LogLevel.ERROR);
      }
    });
    controller.stdOut(new SimpleLineReader() {
      @Override
      public void onLine(String logLine) {
        doLog(logLine, LogLevel.INFO);
      }
    });
    int result = controller.block(60, TimeUnit.SECONDS);
    if (result != 0) {
      doLog("Gwt compile for "+manifest.getModuleName()+" finished w/ non-successful exit code "+
          result, LogLevel.ERROR);
    }
    return result;
  }

  private void saveGwtXmlFile(XmlBuffer xml, File dest) {
    saveTempFile(GwtXmlBuilder.HEADER+xml, new File(dest,genName+".gwt.xml"));
  }
  private void saveTempFile(String value, File dest) {
    X_Log.trace(getClass(), "saving generated file to",dest);
    if (!dest.exists()) {
      dest.getParentFile().mkdirs();
    }
    try (FileWriter out = new FileWriter(dest);) {
      out.append(value);
      out.close();
    } catch (IOException e) {
      X_Log.warn(getClass(), "Error saving generated file ",dest,"\n"+value);
      throw X_Debug.rethrow(e);
    }
  }

  protected String extractGwtVersion(String gwtHome) {
    int lastInd = gwtHome.lastIndexOf("gwt-dev");
    gwtHome = gwtHome.substring(lastInd+7).replace(".jar", "");
    return gwtHome.startsWith("-") ? gwtHome.substring(1) : gwtHome;
  }

  private String resolveDependency(Dependency dependency) {
    switch (dependency.dependencyType()) {
      case ABSOLUTE:
        return replaceLocationVars(dependency.value());
      case RELATIVE:
        if (dependency.groupId().isEmpty()) {
          return replaceLocationVars(dependency.value());
        } else {
          return replaceLocationVars(dependency.groupId())+
              File.separator+
              replaceLocationVars(dependency.version());
        }
      case MAVEN:
        String m2Home = X_Properties.getProperty("maven.home");
        if (m2Home == null) {
          m2Home = X_Properties.getProperty("user.home");
          if (m2Home != null) {
            File f = new File(m2Home,".m2/repository");
            if (f.exists()) {
              m2Home = f.getAbsolutePath();
            }
          }
          if (m2Home == null) {
            X_Log.warn(getClass(), "Cannot resolve maven dependency",dependency
                ,"as M2_HOME environment variable is not set");
          }
        }
        if (m2Home != null) {
          File artifact = new File(m2Home, dependency.groupId().replace('.', File.separatorChar));
          artifact = new File(artifact, dependency.value());
          artifact = new File(artifact, dependency.version());
          if (dependency.classifier().length() > 0) {
            artifact = new File(artifact, dependency.value()+"-"+dependency.version()+"-"+dependency.classifier()+".jar");
          } else {
            artifact = new File(artifact, dependency.value()+"-"+dependency.version()+".jar");
          }
          if (artifact.exists()) {
            return artifact.getAbsolutePath();
          } else {
            X_Log.warn(getClass(),"could not find maven dependency",dependency,"in",artifact);
          }
        }
    }
    return null;
  }

  private class Replacement {
    int start, end;
    String newValue;
    public Replacement(int start, int end, String path) {
      this.start = start;
      this.end = end;
      newValue = path;
    }
  }
  
  private String replaceLocationVars(String value) {
    Matcher matcher = SPECIAL_DIRS.matcher(value);
    List<Replacement> replacements = new ArrayList<Replacement>();
    if (matcher.matches()) {
      String type = matcher.group();
      switch (type) {
        case Dependency.DIR_BIN:
          if (binDir == null) {
            binDir = X_Properties.getProperty("java.class.path", "bin");
          }
          replacements.add(new Replacement(matcher.start(), matcher.end(), binDir));
          break;
        case Dependency.DIR_TEMP:
          replacements.add(new Replacement(matcher.start(), matcher.end(), tempDir.getAbsolutePath()));
          break;
      }
    }
    for (int i = replacements.size(); i-->0;) {
      Replacement replacement = replacements.get(i);
      value = value.substring(0, replacement.start)
          + replacement.newValue+value.substring(replacement.end);
    }
    return value;
  }

  protected void doLog(String msg, LogLevel level) {
    X_Log.log(level, msg);
  }
  
  @Override
  public String getModuleName() {
    return genName;
  }

  public String getEntryPoint() {
    return entryPoint.toString();
  }

  public String getGwtXml() {
    return gwtXml.toString();
  }

  private void maybeAddAncestors(Gwtc gwtc, Package pkg) {
    for (AncestorMode mode : gwtc.inheritanceMode()) {
      switch (mode) {
        case INHERIT_OWN_PACKAGE:
          if (finishedPackages.add(pkg)) {
            addGwtcPackage(gwtc, pkg);
          }
          break;
        case INHERIT_ALL_PACKAGES:
          addAllPackages(pkg);
          break;
        default:
          X_Log.trace("Unsupported ancestor mode",mode,"for package",pkg,"from", "\n"+gwtc);
      }
    }
  }

  protected void addGwtcClass(Gwtc gwtc, Class<?> clazz) {
    // Generate a new gwt.xml file and inherit it.
    GwtXmlBuilder builder = new GwtXmlBuilder(gwtc, clazz.getPackage().getName(), clazz.getSimpleName(), tempDir);
    gwtXml.makeTagAtBeginning("inherits")
    .setAttribute("name", builder.getInheritName());
    addGwtcSettings(gwtc);
  }

  protected void addGwtcSettings(Gwtc gwtc) {
    for (Dependency dep : gwtc.dependencies()) {
      dependencies.add(dep);
    }
    for (Resource html : gwtc.includeHostHtml()) {
      ("head".equals(html.qualifier())
          ? htmlHead : htmlBody).add(html);
    }
  }

  protected void addGwtcPackage(Gwtc gwtc, Package pkg) {
    String name = pkg.getName();
    int i = name.lastIndexOf('.');
    name = name.substring(i+1)+"Generated";
    GwtXmlBuilder builder = new GwtXmlBuilder(gwtc, pkg.getName(), name, tempDir);
    gwtXml.makeTag("inherits")
    .setAttribute("name", builder.getInheritName());
    addGwtcSettings(gwtc);
  }

  protected void maybeAddAncestors(Gwtc gwtc, Class<?> c) {
    if (finishedClasses.add(c)) {
      addGwtcClass(gwtc, c);
    }
    for (AncestorMode mode : gwtc.inheritanceMode()) {
      switch (mode) {
        case INHERIT_OWN_PACKAGE:
          Package pkg = c.getPackage(); 
          gwtc = pkg.getAnnotation(Gwtc.class);
          if (gwtc != null && finishedPackages.add(pkg)) {
            addGwtcPackage(gwtc, pkg);
          }
          break;
        case INHERIT_ALL_PACKAGES:
          addAllPackages(c.getPackage());
          break;
        case INHERIT_ENCOLSING_CLASSES:
          addEnclosingClasses(c);
          break;
        case INHERIT_SUPER_CLASSES:
          addSuperclasses(c);
          break;
        default:
          X_Log.warn("Unsupported mode type",mode,"for class",c);
      }
    }
  }

  private void addAllPackages(Package pkg) {
    Gwtc gwtc = pkg.getAnnotation(Gwtc.class);
    if (gwtc != null && finishedPackages.add(pkg)) {
      addGwtcPackage(gwtc, pkg);
    }
    String parentName = pkg.getName();
    int ind = parentName.lastIndexOf('.');
    while (ind > -1) {
      parentName = parentName.substring(0, ind);
      ind = parentName.lastIndexOf('.');
      pkg = Package.getPackage(parentName);
      if (pkg != null) {
        gwtc = pkg.getAnnotation(Gwtc.class);
        if (gwtc != null && finishedPackages.add(pkg)) {
          addGwtcPackage(gwtc, pkg);
        }
      }
    }
    pkg = Package.getPackage("");
    if (pkg != null) {
      gwtc = pkg.getAnnotation(Gwtc.class);
      if (gwtc != null && finishedPackages.add(pkg)) {
        addGwtcPackage(gwtc, pkg);
      }
    }
  }

  private void addEnclosingClasses(Class<?> c) {
    c = c.getDeclaringClass();
    while (c != null) {
      Gwtc gwtc = c.getAnnotation(Gwtc.class);
      if (gwtc != null && finishedClasses.add(c)) {
        addGwtcClass(gwtc, c);
      }
      c = c.getDeclaringClass();
    }
  }

  private void addSuperclasses(Class<?> c) {
    c = c.getSuperclass();
    while (c != null) {
      Gwtc gwtc = c.getAnnotation(Gwtc.class);
      if (gwtc != null && finishedClasses.add(c)) {
        addGwtcClass(gwtc, c);
      }
      c = c.getSuperclass();
    }
  }

  private void ensureReportError() {
    if (needsReportError) {
      needsReportError = false;
      generateReportError(entryPoint.getClassBuffer());
    }
  }

  private String formatInstanceCall(Method method) {
    String cls = formatInstanceField(method.getDeclaringClass());
    StringBuilder b = new StringBuilder();
    b.append(cls).append(".");
    b.append(formatMethodCall(method));
    b.append(";");
    return b.toString();
  }

  private String formatInstanceField(Class<?> declaringClass) {
    String field = instanceFields.get(declaringClass.getCanonicalName());
    if (field == null) {
      X_Log.trace(getClass(), "Generating instance field for ",declaringClass);
      field = X_Source.toStringEnclosed(declaringClass).replace('.', '_');
      field = Character.toLowerCase(field.charAt(0)) + field.substring(1);
      instanceFields.put(declaringClass.getCanonicalName(), field);
      FieldBuffer buffer = entryPoint.getClassBuffer()
          .createField(declaringClass, field)
          .setModifier(Modifier.PRIVATE | Modifier.FINAL);
      StringBuilder b = new StringBuilder()
         .append("new ")
         .append(entryPoint.getImports().addImport(declaringClass))
         .append("(");
      // Find the best constructor
      Constructor<?> winner = null;
      search:
      for (Constructor<?> ctor : declaringClass.getConstructors()) {
        for (Annotation[] annos : ctor.getParameterAnnotations()) {
          for (Annotation anno : annos) {
            if (anno instanceof DefaultValue) {
              winner = ctor;
              break search;
            }
          }
        }
      }
      if (winner == null) {
        winner = GwtReflect.getPublicConstructor(declaringClass);
      }
      if (winner == null) {
        String error =
          "Cannot instantiate instance of "+declaringClass.getCanonicalName()+"; "
              + "as it does not have an any public constructors annotated with "
              + "@DefaultValue, or a zero-arg public constructor.";
        IllegalArgumentException exception = new IllegalArgumentException(error);
        X_Log.error(getClass(), error, exception);
        throw exception;
      }
      b.append(formatParameters(winner.getParameterTypes(), winner.getParameterAnnotations()));
      buffer.setInitializer(b+");");
    }
    return field;
  }

  private String formatMethodCall(Method method) {
    StringBuilder b = new StringBuilder();
    b.append(method.getName()).append("(");
    b.append(formatParameters(method.getParameterTypes(), method.getParameterAnnotations()));
    return b.append(")").toString();
  }

  private String formatParameters(Class<?>[] params, Annotation[][] annos) {
    StringBuilder b = new StringBuilder();
    for (int i = 0, m = params.length; i < m; i++){
      Class<?> param = params[i];
      DefaultValue value = X_Gwtc.getDefaultValue(param, annos[i]);
      b.append(value.value());
      if (i > 0) {
        b.append(", ");
      }
    }
    return b.toString();
  }

  private String formatStaticCall(Method method) {
    String instance = formatInstanceField(method.getDeclaringClass());
    StringBuilder b = new StringBuilder();
    b.append(instance).append(".");
    b.append(formatMethodCall(method));
    b.append(";");
    return b.toString();
  }

  protected void addGwtXml(Class<?> clazz, Resource build) {
    if (gwtXmlDeps.add(build)) {
      gwtXml
        .makeTagAtBeginning("inherits")
        .setAttribute("name", build.value());
    }
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
    String field = formatInstanceField(clazz);
    String methodName = "testRun_"+field;
    
    MethodBuffer runner = entryPoint.getClassBuffer().createMethod(
        "private final void "+methodName);
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
  
}
