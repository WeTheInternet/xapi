package wetheinter.net.gen;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.inject.impl.SingletonProvider;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.CompileHelper;
import com.google.gwt.dev.Precompilation;
import com.google.gwt.dev.cfg.ModuleDef;
import com.google.gwt.dev.cfg.ModuleDefLoader;
import com.google.gwt.dev.cfg.ModuleHelper;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.JJSOptions;
import com.google.gwt.dev.jjs.JJSOptionsImpl;
import com.google.gwt.dev.jjs.JsOutputOption;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.log.AbstractTreeLogger;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

/**
 * Goal which creates a GWT type oracle to discover your binding setup. This will check for all
 * {@link SingletonDefault} and {@link SingletonOverride} annotations on your main classes source path
 *
 * @goal gen
 * @execution Default
 * @phase post-compile
 * @requiresDependencyResolution compile
 * @author <a href="mailto:internetparty@wetheinter.net">Ajax</a>
 * @version $Id$
 */
//@Execute(phase=LifecyclePhase.COMPILE,goal="gen")
public class GeneratorMojo
// extends AbstractGwtShellMojo
    extends AbstractMojo {
  /**
   * Location of the file.
   *
   * @parameter expression="${xapi.gen.dir}" default-value="${project.basedir}/gen/main/java/"
   * @required
   */
  private File generateDirectory;

  /**
   * Location of gwt sdk. Needed to use trunk revisions.
   *
   * @parameter expression="${gwt.home}" default-value="/shared/xapi/r10955"
   * @required
   */
  @SuppressWarnings("unused")
  private File gwtSdk;

  /**
   * Location of xapi sdk. Needed to use trunk revisions.
   *
   * @parameter expression="${xapi.sdk}" default-value="/home/x/.m2/repository/wetheinter/net"
   * @required
   */
  @SuppressWarnings("unused")
  private File xapiSdk;


  /**
   * Location of xapi sdk. Needed to use trunk revisions.
   *
   * @parameter expression="${project.sdk}" default-value="${basedir}/src/main/java"
   * @required
   */
  @SuppressWarnings("unused")
private File projectSdk;

  /**
   * Location of xapi sdk. Needed to use trunk revisions.
   *
   * @parameter expression="${modules}" default-value="xapi"
   */
  private List<String> modules;

  /**
   * Location of xapi sdk. Needed to use trunk revisions.
   *
   * @parameter expression="${xapi.version}" default-value="0.1"
   */
  @SuppressWarnings("unused")
  private String xapiVersion;

  /**
   * Location of xapi sdk. Needed to use trunk revisions.
   *
   * @parameter expression="${project.version}" default-value="0.2.1"
   */
  @SuppressWarnings("unused")
  private String projectVersion;
  /**
   * @parameter expression="${project}"
   */
  private MavenProject project;

  private class GeneratorThread extends Thread{
    ModuleDef mod;
    Runnable run0,run1;
    @Override
    public void run() {
      run0.run();
      run1.run();
      run0 = null;
      run1 = null;
    }
    public void initialize(final String modName){
      run0 = new Runnable() {
        @Override
        public void run() {
          try {
            mod =
                ModuleDefLoader.createSyntheticModule(getTreeLogger(), modName, new String[] {
                    "wetheinter.net.XInclude"
                    , "playn.PlayN"
                    }, true);
          } catch (UnableToCompleteException e) {
            getLog().error("Could not synthesize module",e);
            run1 = null;
            return;
          }

          //append the requested gwt modules to generator scope
          for (String moduleName : modules){
            getLog().info("Adding gwt module "+moduleName);
            ModuleHelper.addModuleToCompilation(getTreeLogger(), mod, moduleName);
          }
        }
      };
    }
    public void prepare(final JJSOptions options,final File genDir, final ArrayList<Xpp3Dom> mods) {
      run1 = new Runnable() {
        @Override
        public void run() {
          //Precompile the module
          Precompilation precomp = CompileHelper.precompile(getTreeLogger(), options, mod, genDir);
          if (null==precomp){
            getLog().error("Unable to precompile modules "+modules+".\n\t"+
                ""//TODO: add mojo parameter for log target, then add to this log message
                //which file to check for gwt compile errors
            );
            return;
          }

          //Create our generator context
          StandardGeneratorContext ctx;
          try {
            CompilationState state = ModuleHelper.getCompilationState(getLog(),getTreeLogger(),mod);
            ctx =
                new StandardGeneratorContext(state, mod, genDir, precomp.getGeneratedArtifacts(),
                    false);
          } catch (Exception e1) {
            getLog().error("Failure to initialize generator context",e1);
            return;
          }
          //Run generators
          getLog().info("Generating " +
              Arrays.asList(mod.getEntryPointTypeNames())+" into: " + genDir);
          generateModules(ctx, mods,genDir);
        }
      };
    }
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    // First, grab any xapi.xml module files to parse for interfaces to generate
    ArrayList<Xpp3Dom> mods = getXapiModules();
    getTreeLogger();
    JJSOptions options = getCompilerOptions();
    if (!generateDirectory.exists())
      if (!generateDirectory.mkdirs()){
        getLog().error("Could not create the generator directory: "+generateDirectory);
        return;
      }
    File genDir = generateDirectory.isDirectory() ? generateDirectory : new File("/shared/gen");
    getTreeLogger().setMaxDetail(Type.DEBUG);
    //Create a synthetic gwt module using our maven realm's classpath
    final GeneratorThread thread;
    try {
      thread = synthesizeModule();
    } catch (Exception e1) {
      getLog().error("Unable to synthesize modules for "+modules+"\n\t"+mods,e1);
      return;
    }
    thread.prepare(options,genDir,mods);
    thread.start();
    try {
      thread.join();
    } catch (InterruptedException e) {
      getLog().warn("Interrupted while waiting for generator to finish",e);
    }
  }

  protected void generateModules(StandardGeneratorContext ctx, ArrayList<Xpp3Dom> mods, File genDir) {
    getTreeLogger().setMaxDetail(Type.DEBUG);
    getLog().info("Modules: \n" + mods);
    for (Xpp3Dom module : mods) {
      getLog().debug("Generating xapi module: \n" + module.toUnescapedString());

      boolean
        noGwt = "false".equals(module.getAttribute("use-gwt"))
        ,noJava = "false".equals(module.getAttribute("use-java"))
      ;

      if (noGwt)
        System.setProperty("xapi.gen.gwt", "blacklist");
      else
        System.clearProperty("xapi.gen.gwt");

      if (noJava)
        System.setProperty("xapi.gen.java", "blacklist");
      else
        System.clearProperty("xapi.gen.java");

      for (Xpp3Dom gen : module.getChildren("generate-with")) {
        String generator = gen.getAttribute("class");
        for (Xpp3Dom restriction : gen.getChildren()) {
          if (restriction.getName().equals("when-type-assignable")) {
            String toGen = restriction.getAttribute("class");
            getLog().info(
                "Generating " + toGen + " with "
                    + generator);
            try {
              // First, create the generator.
              Generator o = (Generator) Class.forName(generator).newInstance();
              String cls = restriction.getAttribute("class");
              String returned = o.generate(getTreeLogger(), ctx, cls);
              if (!cls.equals(returned))
                getLog().info("Replacing "+cls+" with "+returned);

              //Now, move our xapi-generated files to source folder?
              String toFile = returned.replaceAll("[.]", "/")+".java";
              File f = new File(genDir,toFile);
              getLog().info("Moving "+f+" into source directory");
              if (f.exists()){
                for (Object src : project.getCompileSourceRoots()){
                  File into = new File(String.valueOf(src));
                  if (into.exists()){
                    File target = new File(into, toFile);
                    getLog().info("Dumping into "+target);
                    Util.copy(getTreeLogger(), f, target);
                  }
                }
              }
            } catch (Exception e) {
              getLog().error("Failure generating " + modules, e);
            }
          }
        }
      }
    }
  }

  /**
   * @throws UnableToCompleteException
   */
  private GeneratorThread synthesizeModule() throws UnableToCompleteException, DependencyResolutionRequiredException {

    String modName = "wetheinter.net.synthetic";

//    ResourceLoader loader = ResourceLoaders.forClassLoader(Thread.currentThread());
    ArrayList<URL> urls = new ArrayList<URL>();
    @SuppressWarnings("rawtypes")
	List l;
    //apply all runtime jars
    l = project.getRuntimeClasspathElements();
    for (Object o : l) {
      try {
        getLog().debug("Adding runtime classpath element: " + String.valueOf(o));
        File asFile = new File(String.valueOf(o));
        if (asFile.isFile()){
          urls.add(asFile.toURI().toURL());
        }
        else if (asFile.isDirectory()){
          //TODO: perhaps search for .gwt.xml files, to offer up to controller ui / suggest in cli help prompts?
          urls.add(asFile.toURI().toURL());
        }else if (!asFile.exists()){
          getLog().warn("Runtime classpath element "+asFile+" does not exist");
        }
      } catch (Exception e) {
        getLog().warn(
            "Error adding runtime classpath element: " + o + " to compilation.", e);
      }
    }

    //apply project classpath last
    l = project.getCompileSourceRoots();
    for (Object o : l) {
      try {
        getLog().info("Adding compile source root: " + String.valueOf(o));
        File asFile = new File(String.valueOf(o));
        if (!asFile.exists())
          asFile.mkdirs();
        if (!asFile.isDirectory()){
          getLog().warn("Compiler source root "+o+" is not a directory");
        }else if (!asFile.canRead()){
          getLog().warn("Compiler source root "+o+" is not readable");
        }else{
          urls.add(asFile.toURI().toURL());
        }
      } catch (Exception e) {
        getLog().warn("Error adding source root " + o + " to compilation.", e);
      }
    }

    GeneratorThread thread = new GeneratorThread();
    thread.setContextClassLoader(new URLClassLoader(urls.toArray(new URL[urls.size()]),Thread.currentThread().getContextClassLoader()));
    thread.initialize(modName);

    return thread;
  }

  private final SingletonProvider<AbstractTreeLogger> log = new SingletonProvider<AbstractTreeLogger>() {
    @Override
    protected AbstractTreeLogger initialValue() {
      try {
        return new PrintWriterTreeLogger(
//            getNoopWriter()getCompilationState
//            getConsoleWriter()
            getDefaultLogFile()
        );
      } catch (Exception e1) {
        e1.printStackTrace();
        return new PrintWriterTreeLogger();
      }
    }
  };

  protected AbstractTreeLogger getTreeLogger() {
    return log.get();
  }

  protected static PrintWriter getNoopWriter() {
    return new PrintWriter(new OutputStream() {
      @Override
      public void write(int b) throws IOException {
      }
    });
  }
  protected static PrintWriter getConsoleWriter() {
    return new PrintWriter(System.out){
      @Override
      public void println(String x) {
        super.println("[INFO] "+x);
      }
    };
  }
  protected static OutputStream getNoopStream() {
    return new OutputStream() {
      @Override
      public void write(int b) throws IOException {
      }
    };
  }

  protected static File getDefaultLogFile() {
    File logFile = new File("/shared/gen/gen.log");
    try {
      if (logFile.isFile())
        logFile.delete();
      logFile.createNewFile();
    } catch (IOException e1) {
      e1.printStackTrace();
    }
    return logFile;
  }

  protected JJSOptions getCompilerOptions() {
    JJSOptions options = new JJSOptionsImpl();
    options.setAggressivelyOptimize(false);
    options.setOptimizePrecompile(false);
    options.setAggressivelyOptimize(false);
    options.setCastCheckingDisabled(false);
    options.setClassMetadataDisabled(false);
    options.setCompilerMetricsEnabled(false);
    options.setOptimizationLevel(0);
    options.setClosureCompilerEnabled(false);
    options.setEnableAssertions(false);
    options.setOptimizePrecompile(false);
    options.setOutput(JsOutputOption.PRETTY);
    options.setRunAsyncEnabled(false);
    options.setSoycEnabled(false);
    options.setSoycExtra(false);
    options.setSoycHtmlDisabled(false);
    options.setStrict(false);
    return options;
  }

  protected ArrayList<Xpp3Dom> getXapiModules() {
    ArrayList<Xpp3Dom> mods = new ArrayList<Xpp3Dom>();
    @SuppressWarnings("rawtypes")
    List list;
    try {
      list = project.getRuntimeClasspathElements();
    } catch (DependencyResolutionRequiredException e) {
      getLog().error("Could not load classpath. Exiting generator", e);
      throw new RuntimeException(e);
    }
    for (Object o : list) {
      File f = new File(String.valueOf(o));
      if (f.isFile()) {
        if (f.getName().endsWith("jar")) {
          JarFile jar = null;
          try{
            jar = new JarFile(f);
            ZipEntry gen = jar.getEntry("xapi.xml");
            if (null != gen) {
              InputStream input = jar.getInputStream(gen);
              Xpp3Dom dom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(input));
              mods.add(dom);
            }
          } catch (Exception e) {
            getLog().warn("Error searching for xapi.xml in file " + f, e);
          }finally {
            if (jar!=null)try {jar.close();}catch (IOException e) {}
          }
        } else if (f.getName().endsWith("xapi.xml")) {
          try {
            InputStream input = new FileInputStream(f);
            Xpp3Dom dom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(input));
            mods.add(dom);
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      } else {
        File moduleFile = new File(f, "xapi.xml");
        getLog().info("Checking for "+moduleFile);

        if (moduleFile.exists()) {
          try {
            InputStream input = new FileInputStream(moduleFile);
            Xpp3Dom dom = Xpp3DomBuilder.build(ReaderFactory.newXmlReader(input));
            mods.add(dom);
          } catch (XmlPullParserException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    return mods;
  }


//  public static void copyFile(File sourceFile, File destFile) throws IOException {
//    if(!destFile.exists()) {
//        destFile.getParentFile().mkdirs();
//        destFile.createNewFile();
//    }
//
//    FileChannel source = null;
//    FileChannel destination = null;
//
//    try {
//        source = new FileInputStream(sourceFile).getChannel();
//        destination = new FileOutputStream(destFile).getChannel();
//        destination.transferFrom(source, 0, source.size());
//    }
//    finally {
//        if(source != null) {
//            source.close();
//        }
//        if(destination != null) {
//            destination.close();
//        }
//    }
//}

}
