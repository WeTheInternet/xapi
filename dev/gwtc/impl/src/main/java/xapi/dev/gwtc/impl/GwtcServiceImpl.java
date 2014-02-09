package xapi.dev.gwtc.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.ResourceBuilder;
import xapi.annotation.inject.InstanceDefault;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.file.X_File;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.GwtcProperties;
import xapi.io.api.SimpleLineReader;
import xapi.log.X_Log;
import xapi.shell.X_Shell;
import xapi.shell.api.ShellSession;
import xapi.test.junit.JUnit4Runner;
import xapi.test.junit.JUnitUi;
import xapi.util.X_Debug;
import xapi.util.X_Properties;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dev.Compiler;
import com.google.gwt.dev.GwtCompiler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.reflect.client.GwtReflect;

@Gwtc(propertiesLaunch=@GwtcProperties)
@InstanceDefault(implFor=GwtcService.class)
public class GwtcServiceImpl extends GwtcServiceAbstract {
    
  private String binDir;
  
  Pattern SPECIAL_DIRS = Pattern.compile(
      "("+Pattern.quote(Dependency.DIR_BIN)+")|"+
      "("+Pattern.quote(Dependency.DIR_TEMP)+")|"
  );

  private MethodBuffer junitLoader;
  
  public GwtcServiceImpl() {
    this(Thread.currentThread().getContextClassLoader());
  }
  
  public GwtcServiceImpl(ClassLoader resourceLoader) {
    super(resourceLoader);
  }

  @Override
  public void addAsyncBlock(Class<? extends RunAsyncCallback> asSubclass) {
    
  }
  
  @Override
  public void addGwtTestCase(Class<? extends GWTTestCase> subclass) {
    
  }

  @Override
  public void addGwtTestSuite(Class<? extends GWTTestSuite> asSubclass) {
    
  }

  @Override
  public void addMethod(Method method) {
    addMethod(method, false);
  }
  @Override
  public void addMethod(Method method, boolean onNewInstance) {
    if (Modifier.isStatic(method.getModifiers())){ 
      // print a call to a static method
      out.println(out.formatStaticCall(method));
    } else {
      // print a call to an instance method; creating an instance if necessary.
      out.println(out.formatInstanceCall(method, onNewInstance));
    }
  }
  

  @Override
  public void addPackage(Package pkg, boolean recursive) {
    if (!finished.add(pkg.getName())) {
      return;
    }
    Gwtc gwtc = pkg.getAnnotation(Gwtc.class);
    context.addPackages(pkg, this, recursive);
    if (gwtc != null) {
      context.addGwtcPackage(gwtc, pkg, recursive);
    }
  }

  @Override
  public int compile(GwtManifest manifest) {
    String gwtHome = generateCompile(manifest);
    // Logging
    X_Log.info(getClass(), "Starting gwt compile", manifest.getModuleName());
    X_Log.trace(manifest);
    X_Log.trace("Args: java ", manifest.toJvmArgs(),manifest.toProgramArgs());
    X_Log.debug("Requested Classpath\n",manifest.toClasspathFullCompile(gwtHome));
    X_Log.debug("Runtime cp", ((URLClassLoader)getClass().getClassLoader()).getURLs());
    
    ShellSession controller 
      = X_Shell.launchJava(GwtCompiler.class, manifest.toClasspathFullCompile(gwtHome), manifest.toJvmArgArray(), manifest.toProgramArgs().split("[ ]+"));
    controller.stdErr(new SimpleLineReader() {
      @Override
      public void onLine(String errLog) {
        warn("[ERROR] "+errLog);
      }
    });
    controller.stdOut(new SimpleLineReader() {
      @Override
      public void onLine(String logLine) {
        info(logLine);
      }
    });
    int result = controller.block(60, TimeUnit.SECONDS);
    if (result != 0) {
      error("Gwt compile for "+manifest.getModuleName()+" finished w/ non-successful exit code "+
          result);
    }
    X_Log.info("Entry point: "+new File(manifest.getWarDir(), context.getGenName()+".html"));
    return result;
  }

  public String generateCompile(GwtManifest manifest) {
    assert tempDir.exists() : "No usable directory "+tempDir.getAbsolutePath();
    X_Log.info(getClass(), "Generated entry point", "\n"+getEntryPoint());
    X_Log.info(getClass(), "Generated module", "\n"+getGwtXml());
    if (manifest.getModuleName() == null) {
      manifest.setModuleName(genName);
      manifestName = genName;
    } else {
      manifestName = manifest.getModuleName();
      context.setRenameTo(manifest.getModuleName());
    }
    saveGwtXmlFile(context.getGwtXml(), manifest.getModuleName(), tempDir);
    saveTempFile(entryPoint.toString(), new File(tempDir, GEN_PREFIX+File.separator+ genName+".java"));
    return prepareCompile(manifest);
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
            X_Log.trace(getClass(), "Using maven artifact ",artifact.getAbsolutePath());
            return artifact.getAbsolutePath();
          } else {
            X_Log.warn(getClass(),"could not find maven dependency",dependency,"in",artifact);
          }
        }
    }
    return null;
  }

  protected GwtcProperties getDefaultLaunchProperties() {
    return getClass().getAnnotation(Gwtc.class).propertiesLaunch()[0];
  }

  protected String prepareCompile(GwtManifest manifest) {
    
    GwtcProperties defaultProp = getDefaultLaunchProperties();
    for (GwtcProperties prop : context.getLaunchProperties()) {
      if (prop.obfuscationLevel() != defaultProp.obfuscationLevel()) {
        manifest.setObfuscationLevel(prop.obfuscationLevel());
      }
      if (prop.logLevel() != defaultProp.logLevel()) {
        manifest.setLogLevel(prop.logLevel());
        manifest.addSystemProp("xapi.log.level="+prop.logLevel().name());
      }
    }
    
    if (manifest.getWarDir() == null) {
      File f = tempDir;
      try {
        manifest.setWarDir(f.getCanonicalPath());
        X_Log.info(getClass(), "Manifest WAR: ",manifest.getWarDir());
      } catch (IOException e) {
        X_Log.warn("Unable to create temporary war directory for GWT compile",
            "You will likely get an unwanted war folder in the directory you executed this program");
        X_Debug.maybeRethrow(e);
      }
    }
    if (manifest.getUnitCacheDir() == null) {
      try {
        File f = X_File.createTempDir("gwtc-"+manifest.getModuleName()+"UnitCache", manifest.isDisableUnitCache());
        if (f != null) {
          manifest.setUnitCacheDir(f.getCanonicalPath());
        }
      } catch (IOException e) {
        X_Log.warn("Unable to create unit cache work directory for GWT compile",
            "You will likely get unwanted gwtUnitcache folders in the directory you executed this program");
      }
    }
    for (Dependency dependency : context.getDependencies()) {
      manifest.addDependency(resolveDependency(dependency));
    }
    String gwtHome = X_Properties.getProperty("gwt.home");
    if (gwtHome == null) {
      URL gwtHomeLocation = Compiler.class.getClassLoader().getResource(Compiler.class.getName().replace('.', '/')+".class");
      if (gwtHomeLocation == null) {
        X_Log.warn("Unable to find gwt home from System property gwt.home, "
            , "nor from looking up the gwt compiler class from classloader.  Defaulting to ./lib");
        gwtHome = X_File.getPath(".");
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
    generateWar(manifest.getWarDir(), manifest.getModuleName());
    return gwtHome;
  }
  
  @Override
  public boolean addJUnitClass(Class<?> clazz) {
    if (!finished.add(clazz.getName())) {
      X_Log.info(getClass(), "Skipped JUnit 4 class",clazz);
      return false;
    }
    search: {
      for (Method m : clazz.getMethods()) {
        if (m.isAnnotationPresent(Test.class)) {
          break search;
        }
      }
      return false;
    }
    Gwtc gwtc = clazz.getAnnotation(Gwtc.class);
    X_Log.info(getClass(), "generating JUnit class", clazz, "?"+(gwtc != null));
    if (gwtc != null) {
      context.addGwtcClass(gwtc, clazz);
    }
    addGwtModules(clazz);
    X_Log.info(getClass(), "added test class for JUnit 4",clazz);
    ensureReportError();
    inheritGwtXml(clazz, ResourceBuilder.buildResource("org.junit.JUnit4").build());
    inheritGwtXml(clazz, ResourceBuilder.buildResource("com.google.gwt.core.Core").build());
    ClassBuffer cb = classBuffer();
    String simple = cb.addImport(clazz);
    String methodName = "add"+simple+"Tests";
    String gwt = cb.addImport(GWT.class);
    String callback = cb.addImport(RunAsyncCallback.class);
    String magic = cb.addImportStatic(GwtReflect.class, "magicClass");
    cb.createMethod("void "+methodName)
      .println(gwt+".runAsync("+simple+".class,")
      .indent()
      .println("new "+callback+ "() {")
      .indent()
      .println("public void onSuccess() {")
      .indent()
      .println(magic+"("+simple+".class);")
      .startTry()
      .println("junit.addTests("+simple+".class);")
      .startCatch("Throwable", "e")
      .println("junit.print(\"Error adding "+simple+" to unit test\", e);")
      .endTry()
      .outdent()
      .println("}")
      .println()
      .println("public void onFailure(Throwable reason) {")
      .indent()
      .println("junit.print(\"Error loading "+simple+"\", reason);")
      .outdent()
      .println("}")
      .outdent()
      .println("}")
      .outdent()
      .println(");")
      ;
    
    junitLoader.println(methodName+"();");
    return true;
  }
  
  @Override
  protected void generateReportError(ClassBuffer classBuffer) {
    super.generateReportError(classBuffer);
    addClass(JUnit4Runner.class);
    junitLoader = classBuffer.createInnerClass("private final class JUnit extends JUnitUi")
      .createMethod("public void loadAllTests()");
    
    classBuffer.createField(JUnitUi.class, "junit")
      .setModifier(Modifier.FINAL | Modifier.PRIVATE)
      .setInitializer("new JUnit()");
    
    out.println("junit.onModuleLoad();");
  }

}
