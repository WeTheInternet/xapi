package xapi.mojo.gwt;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.resolution.ArtifactResult;
import xapi.dev.gwt.gui.CodeServerGui;
import xapi.inject.impl.SingletonProvider;
import xapi.log.X_Log;
import xapi.mojo.api.AbstractXapiMojo;
import xapi.mojo.api.SourceDependency;
import xapi.mvn.X_Maven;
import xapi.util.X_Debug;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;
import xapi.util.X_String;
import xapi.util.api.Pair;
import xapi.util.api.ReceivesValue;
import xapi.util.impl.PairBuilder;

import com.google.gwt.core.shared.GWT;

import javax.swing.*;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

/**
 * Goal which launches a gui which runs the gwt 2.5 codeserver from maven
 * dependencies
 * @author <a href="mailto:internetparty@wetheinter.net">Ajax</a>
 * @version $Id$
 */
@org.apache.maven.plugins.annotations.Mojo(
    name="codeserver"
    ,requiresDependencyResolution=ResolutionScope.COMPILE_PLUS_RUNTIME
    ,defaultPhase=LifecyclePhase.COMPILE
    ,threadSafe=true
    )
@SuppressWarnings("serial")
public class CodeServerMojo extends AbstractXapiMojo implements ContextEnabled {

  private static final FileFilter gwt_xml_filter = new FileFilter() {
    @Override
    public boolean accept(File pathname) {
      return pathname.isDirectory() || pathname.getName().endsWith(".gwt.xml");
    }
  };

  /**
   * The gwt module to use; if not set, we'll scan the classpath for .gwt.xml
   */
  @Parameter(property="gwt.module", defaultValue="")
  private String module;


  /**
   * The gwt version to use; if not set, we'll scan the classpath for gwt dependencies
   */
  @Parameter(property="gwt.version")
  private String gwtVersion;

  /**
   * The port on which to start opening codeservers.
   */
  @Parameter(property="port",defaultValue="1337")
  private Integer port;

  /**
   * The port on which to listen for a debugger
   */
  @Parameter(property="debug.port",defaultValue="0")
  private Integer debugPort;

  /**
   * The amount of time to wait for the debugger; negative values disable debugger.
   * default = -1
   */
  @Parameter(property="debug.delay", defaultValue="-1")
  private Integer debugDelay;



  /**
   * Whether or not to scan test source paths.
   */
  @Parameter(property="xapi.include.test", defaultValue="false")
  private Boolean includeTestSource;

  @Parameter(property = "gwt.log.level", defaultValue = "INFO")
  private String logLevel;

  public String getLogLevel() {
    return logLevel;
  }

  private Log log;
  @SuppressWarnings("rawtypes")
  private Map pluginContext;

  @Override
  public void setLog(Log log) {
    this.log = log;
  }

  @Override
  public Log getLog() {
    if (log == null) {
      log = new SystemStreamLog();
    }

    return log;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public Map getPluginContext() {
    return pluginContext;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void setPluginContext(Map pluginContext) {
    this.pluginContext = pluginContext;
  }


  private final SingletonProvider<String> _gwtVersion = new SingletonProvider<String>() {
    @Override
    protected String initialValue() {
      if (X_String.isNotEmpty(gwtVersion)) {
        return gwtVersion;
      }
      String version = X_Properties.getProperty("gwt.version");
      if (version != null) {
        return version;
      }
//      version = GWT.getVersion();
//      if (version != null)
//        return version;
      return superGuess("com.google.gwt", X_Namespace.GWT_VERSION);
    };
  };

  /**
   * Whether or not to compile immediately.
   */
  @Parameter(property="xapi.auto.launch", defaultValue="true")
  private Boolean autoCompile;

  private class CodeServerView extends CodeServerGui {

    @Override
    protected int getPort() {
      return port == null ? super.getPort() : port;
    }

    @Override
    protected int debugTimeout() {
      return debugDelay == null ? -1 : debugDelay;
    }

    @Override
    protected int debugPort() {
      int delay = debugDelay;
      if (delay < 1) {
        return 0;
      }
      return debugPort == 0 ? super.debugPort() : debugPort;
    }

    @Override
    protected LinkedList<String> getSourcePaths(boolean includeTestSources) {
      final LinkedList<String> sources = super.getSourcePaths(includeTestSources);
      ReceivesValue<String> addSource = new ReceivesValue<String>() {
        @Override
        public void set(String location) {
            if (new File(location).exists()) {
              sources.add(new File(location).getAbsolutePath());
            } else {
              X_Log.trace(getClass(), "Skipping non-existent file @ ", location);
            }
        }
      };
      if (hasSourceDependencies()) {
        for (SourceDependency sourceDependency : getSourceDependencies()) {
          try {
            final MavenProject resultProject = findInWorkspace(sourceDependency.getGroupId(),
                sourceDependency.getArtifactId());
            if (resultProject != null) {
              for (String location : resultProject.getCompileSourceRoots()) {
                addSource.set(location);
              }

              addSource.set(resultProject.getBuild().getOutputDirectory());
              for (Resource resource : resultProject.getResources()) {
                addSource.set(resource.getDirectory()); // We are ignoring excludes.  If someone needs them, patches are welcome.
              }

              if (sourceDependency.isIncludeTests()) {
                for (String location : resultProject.getTestCompileSourceRoots()) {
                  addSource.set(location);
                }
                addSource.set(resultProject.getBuild().getTestOutputDirectory());
                for (Resource resource : resultProject.getTestResources()) {
                  addSource.set(resource.getDirectory());
                }
              }

            } else {
              X_Log.warn(getClass(), "No artifact found in workspace for ", sourceDependency);
            }
          } catch (Exception e) {
            X_Log.warn(getClass(), "Error resolving source paths for ", sourceDependency, e);
          }
        }
      }
      return sources;
    }

    public void keepAlive() throws MojoExecutionException {
      final MavenProject project = getProject();
      X_Log.info(getClass(),"Preparing gwt recompiler for "+project,"Include test sources? "+isUseTestSources());
      if (null != project) {
        addSource(project.getBasedir());
        LinkedList<String> modules = new LinkedList<String>();
        if (isUseTestSources()) {
          for (Object o : project.getTestCompileSourceRoots()) {
            File f = new File(String.valueOf(o));
            if (f.exists()) {
              // Add the source location
              addTestSource(f);
              // also scan for .gwt.xml modules
              try {
                modules.addAll(findModules(f));
              } catch (Exception e) {
                getLog()
                    .warn(
                        "An error was encountered while searching for .gwt.xml modules",
                        e);
              }
            } else {
              X_Log.warn(getClass(), "Test source does not exist",f);
            }
          }
          for (Resource o : project.getTestResources()) {
            File f = new File(o.getDirectory());
            if (f.exists()) {
              addTestSource(f);
              // scan for .gwt.xml modules; the resources will be on classpath
              // already
              try {
                modules.addAll(findModules(f));
              } catch (Exception e) {
                getLog()
                    .warn(
                        "An error was encountered while searching for .gwt.xml modules",
                        e);
              }
            } else {
              X_Log.warn(getClass(), "Test resource does not exist",f);
            }
          }
        }
        for (Object o : project.getCompileSourceRoots()) {
          File f = new File(String.valueOf(o));
          if (f.exists()) {
            // Add the source location
            addSource(f);
            // also scan for .gwt.xml modules
            try {
              modules.addAll(findModules(f));
            } catch (Exception e) {
              getLog().warn(
                  "An error was encountered while searching for .gwt.xml modules in "
                      + f, e);
            }
          }
        }
        for (Resource o : project.getResources()) {
          File f = new File(o.getDirectory());
          if (f.exists()) {
            addSource(f);
            // only scan for .gwt.xml modules; the resources will be on classpath
            // already
            try {
              modules.addAll(findModules(f));
            } catch (Exception e) {
              getLog().warn(
                  "An error was encountered while searching for .gwt.xml modules in "
                      + f, e);
            }
          }
        }

        if (modules.size() > 0) {
          setModule(modules.get(0));
        }

        try {
          if (isUseTestSources()) {
            for (Object o : project.getTestClasspathElements()) {
              File f = new File(String.valueOf(o));
              if (f.exists()) {
                if (f.isDirectory()) {
                  // directories are to be handled differently
                  addToTestClasspath(f);
                } else {
                  addTestSource(f);
                }
              } else {
                X_Log.warn(getClass(), "Test classpath element does not exist",f,"from "+o);
              }
            }
          } else {
            for (Object o : project.getCompileClasspathElements()) {
              File f = new File(String.valueOf(o));
              if (f.exists()) {
                if (f.isDirectory()) {
                  // directories are to be handled differently
                  addToClasspath(f);
                } else {
                  addSource(f);
                }
              }
            }
          }
        } catch (DependencyResolutionRequiredException e1) {
          getLog()
              .error(
                  "Unable to load compile-scoped classpath elements."
                      + "\nIf you are extending this plugin, "
                      + "you may need to include &lt;requiresDependencyResolution>compile&lt;/requiresDependencyResolution> "
                      + "in your @Mojo annotation / plugin.xml file", e1);
        }
      }
      setVisible(true);
      SwingUtilities.invokeLater(new Runnable() {

        @Override
        public void run() {
          String cp = getClasspath(includeTestSource, ":");
          gwtLocations.findArtifact(cp, "gwt-dev", ":");
          gwtLocations.findArtifact(cp, "gwt-codeserver", ":");
          int before = cp.length();
          gwtLocations.findArtifact(cp, "gwt-user", ":");
          if (before != cp.length()) {
            // Missing gwt-user means we're probably missing org.json and validation apis...
          }
          if (autoCompile) {
            launchServer(includeTestSource, logLevel);
          }
        }
        });

      try {
        while (isVisible()) {
          Thread.sleep(1000);
        }
      } catch (Exception e) {
        throw new MojoExecutionException("Error while waiting on codeserver", e);
      }

    }

    @Override
    protected boolean isUseTestSources() {
      return Boolean.TRUE.equals(includeTestSource);
    }

    @Override
    protected Pair<String, Boolean> findGwt(String cp, String cpSep) {
      VersionRange versions;
      try {
        versions = VersionRange.createFromVersionSpec("[2.5.0,)");
      } catch (InvalidVersionSpecificationException e) {
        throw X_Debug.rethrow(e);
      }

      ArtifactHandler artifactHandler = new DefaultArtifactHandler("default");
      Artifact gwtUser = new DefaultArtifact("com.google.gwt", "gwt-user", versions, "compile", "default", "jar",
          artifactHandler);
      // Check maven first
      Artifact local = getSession().getLocalRepository().find(gwtUser);
      if (local != null) {
        return PairBuilder.pairOf(local.getFile().getParentFile().getParent(), true);
      }
      return super.findGwt(cp, cpSep);
    }


    @Override
    protected String getModuleDefault() {
      if (!"".equals(module)) {
        return module;
      }
      for (String source : getProject().getCompileSourceRoots()) {
        File f = new File(source);
        if (f.exists()) {
          try {
            String module = findModule(f);
            if (module != null) {
              return module;
            }
          }catch(Throwable e) {
            X_Log.error("Failed lookup of gwt module for ", f, e);
          }
        }
      }
      return super.getModuleDefault();
    }

    protected Collection<String> findModules(File f) throws FileNotFoundException,
    XmlPullParserException, IOException {
      ArrayList<String> list = new ArrayList<String>();
      findModules(f, f, list);
      return list;
    }

    private void findModules(File rootFile, File f, Collection<String> into) throws FileNotFoundException,
      XmlPullParserException, IOException {
      if (f.isDirectory()) {
        for (File child : f.listFiles(gwt_xml_filter)) {
          findModules(rootFile, child, into);
        }
      } else if (f.getName().endsWith(".gwt.xml")) {
        getLog().debug("Checking for entry points in " + f);
        // try to get entry points
        Xpp3Dom dom = Xpp3DomBuilder.build(new FileReader(f));
        getLog().debug(dom.toString());
        for (Xpp3Dom entry : dom.getChildren("entry-point")) {
          String attr = entry.getAttribute("class");
          if (null != attr && attr.length() > 0) {
//            into.add(attr.substring(0,
//                attr.lastIndexOf('.', attr.lastIndexOf('.') - 1))
//                + "." + f.getName().replace(".gwt.xml", ""));
            String mod = f.getAbsolutePath().substring(rootFile.getAbsolutePath().length()+1);
            into.add(mod.replace('/', '.').replace(".gwt.xml", ""));
          }
        }
      }
    }
    private String findModule(File f) throws FileNotFoundException,
        XmlPullParserException, IOException {
      if (f.isDirectory()) {
        String module;
        for (File child : f.listFiles(gwt_xml_filter)) {
          module = findModule(child);
          if (module != null) {
            return module;
          }
        }
      } else if (f.getName().endsWith(".gwt.xml")) {
        getLog().debug("Checking for entry points in " + f);
        // try to get entry points
        Xpp3Dom dom = Xpp3DomBuilder.build(new FileReader(f));
        getLog().debug(dom.toString());
        for (Xpp3Dom entry : dom.getChildren("entry-point")) {
          String attr = entry.getAttribute("class");
          if (null != attr && attr.length() > 0) {
            return attr.substring(0,
                attr.lastIndexOf('.', attr.lastIndexOf('.') - 1))
                + "." + f.getName().replace(".gwt.xml", "");
          }
        }
      }
      return null;
    }

    @Override
    protected GwtFinder initFinder() {
      return new GwtFinder() {
        @Override
        public String findArtifact(String cp, String artifact, String cpSep) {
          if (cp.contains(artifact)) {
            return cp;
          }
          String location = locateArtifact(cp, artifact, cpSep);
          File f = new File(location);
          if (f.exists()){
            addToClasspath(f);
            return f.getAbsolutePath() + cpSep + cp;
          }
          else {
            X_Log.warn("Could not find artifact",artifact,"looked in",f);
            return super.findArtifact(cp, artifact, cpSep);
          }
        }
      };
    }

    protected String locateArtifact(String cp, String artifact, String cpSep) {
      ArtifactResult location = X_Maven.loadArtifact("com.google.gwt", artifact, guessVersion("com.google.gwt", "2.6.1"));
      if (location.isResolved()) {
        return location.getArtifact().getFile().getAbsolutePath();
      }
      return null;
    }

  }

  /**
   * TODO: instead of parsing the objects directly from the maven project,
   * assemble a command line argument to pass to {@link Runtime#exec(String)},
   * so we can fork a single codeserver gui, save pid w/
   * {@link System#setProperty(String, String)}, and send new executions by
   * writing to the processes {@link Process#getOutputStream()}.
   *
   * This will also allow us to launch the gui as a maven goal, or as a java
   * executable; both of which can be created as an IDE launch config.
   *
   */

  @Override
  public void doExecute() throws MojoExecutionException, MojoFailureException {
    new CodeServerView().keepAlive();
  }

  protected String superGuess(String groupId, String backup) {
    return super.guessVersion(groupId, backup);
  }

  @Override
  public String guessVersion(String groupId, String backup) {

    String version = X_Properties.getProperty("gwt.version");
    if (version != null) {
      return version;
    }
    version = GWT.getVersion();
    if (version != null) {
      return version;
    }
    return superGuess(groupId, backup);
  }

}
