package wetheinter.net.dev;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import wetheinter.net.dev.gui.CodeServerGui;

/**
 * Goal which launches a gui which runs the gwt 2.5 codeserver from maven dependencies
 *
 * @goal codeserver
 * @execution Default
 * @phase compile
 * @requiresDependencyResolution compile
 * @author <a href="mailto:internetparty@wetheinter.net">Ajax</a>
 * @version $Id$
 */
// @Execute(phase=LifecyclePhase.TEST,goal="xapi")
@SuppressWarnings("serial")
public class CodeServerMojo extends CodeServerGui implements Mojo, ContextEnabled {

  private static final FileFilter gwt_xml_filter = new FileFilter() {
    @Override
    public boolean accept(File pathname) {
      return pathname.isDirectory() || pathname.getName().endsWith(".gwt.xml");
    }
  };

  /**
   * Location to export draft compiles.
   *
   * @parameter expression="${xapi.gen.dir}"
   *            default-value="${project.build.directory}/generated-sources/codeserver/"
   * @required
   */
  @SuppressWarnings("unused")
private File workDirectory;

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
   * @parameter expression="${xapi.version}" default-value="0.1"
   */
  @SuppressWarnings("unused")
  private String xapiVersion;

  /**
   *
   * @parameter expression="${project.version}" default-value="0.2.1"
   */
  @SuppressWarnings("unused")
  private String projectVersion;
  /**
   *
   * @parameter expression="${xapi.include.test}" default-value="false"
   */
  private Boolean includeTestSource;

  /**
   * @parameter expression="${project}"
   */
  private MavenProject project;

  /**
   * TODO: instead of parsing the objects directly from the maven project, assemble a command line
   * argument to pass to {@link Runtime#exec(String)}, so we can fork a single codeserver gui, save
   * pid w/ {@link System#setProperty(String, String)}, and send new executions by writing to the
   * processes {@link Process#getOutputStream()}.
   *
   * This will also allow us to launch the gui as a maven goal, or as a java executable; both of
   * which can be created as an IDE launch config.
   *
   */

  public void execute() throws MojoExecutionException, MojoFailureException {
    if (null != project) {
      addSource(project.getBasedir());
      LinkedList<String> modules = new LinkedList<String>();
      if (isUseTestSources()) {
        for (Object o : project.getTestCompileSourceRoots()) {
          File f = new File(String.valueOf(o));
          if (f.exists()) {
            //Add the source location
            addSource(f);
            // also scan for .gwt.xml modules
            try {
              modules.addAll(findModules(f));
            } catch (Exception e) {
              getLog().warn("An error was encountered while searching for .gwt.xml modules", e);
            }
          }
        }
        for (Resource o : project.getTestResources()) {
          File f = new File(o.getDirectory());
          if (f.exists()) {
            addSource(f);
            //scan for .gwt.xml modules; the resources will be on classpath already
            try {
              modules.addAll(findModules(f));
            } catch (Exception e) {
              getLog().warn("An error was encountered while searching for .gwt.xml modules", e);
            }
          }
        }
      }
      for (Object o : project.getCompileSourceRoots()) {
        File f = new File(String.valueOf(o));
        if (f.exists()) {
          //Add the source location
          addSource(f);
          // also scan for .gwt.xml modules
          try {
            modules.addAll(findModules(f));
          } catch (Exception e) {
            getLog().warn("An error was encountered while searching for .gwt.xml modules in "+f, e);
          }
        }
      }
      for (Resource o : project.getResources()) {
        File f = new File(o.getDirectory());
        if (f.exists()) {
          addSource(f);
          // only scan for .gwt.xml modules; the resources will be on classpath already
          try {
            modules.addAll(findModules(f));
          } catch (Exception e) {
            getLog().warn("An error was encountered while searching for .gwt.xml modules in "+f, e);
          }
        }
      }

      if (modules.size() > 0) {
        setModule(modules.get(0));
      }

      try {
        if (isUseTestSources()) {
          for (Object o : project.getTestClasspathElements()) {
            getLog().info(o.toString());
            File f = new File(String.valueOf(o));
            if (f.exists())
              if (f.isDirectory()) {
                // directories are to be handled differently
                addToClasspath(f);
              } else {
                addSource(f);
              }
          }
        }
        for (Object o : project.getCompileClasspathElements()) {
          File f = new File(String.valueOf(o));
          if (f.exists())
            if (f.isDirectory()) {
              // directories are to be handled differently
              addToClasspath(f);
            } else {
              addSource(f);
            }
        }
      } catch (DependencyResolutionRequiredException e1) {
        getLog()
            .error(
                "Unable to load compile-scoped classpath elements."
                    + "\nIf you are extending this plugin, "
                    + "you may need to include &lt;requiresDependencyResolution>compile&lt;/requiresDependencyResolution> "
                    + "in your plugin.xml file", e1);
      }
    }
    setVisible(true);

    try {
      while (isVisible()) {
        Thread.sleep(1000);
      }
    } catch (Exception e) {
      throw new MojoExecutionException("Error while waiting on codeserver", e);
    }
  }

  private Collection<String> findModules(File f) throws FileNotFoundException,
      XmlPullParserException, IOException {
    Collection<String> list = new ArrayList<String>();

    if (f.isDirectory()) {
      for (File child : f.listFiles(gwt_xml_filter)) {
        list.addAll(findModules(child));
      }
    } else if (f.getName().endsWith(".gwt.xml")) {
      getLog().debug("Checking for entry points in " + f);
      // try to get entry points
      Xpp3Dom dom = Xpp3DomBuilder.build(new FileReader(f));
      getLog().debug(dom.toString());
      for (Xpp3Dom entry : dom.getChildren("entry-point")) {
        String attr = entry.getAttribute("class");
        if (null != attr && attr.length() > 0)
          list.add(attr.substring(0, attr.lastIndexOf('.', attr.lastIndexOf('.') - 1)) + "."
              + f.getName().replace(".gwt.xml", ""));
      }
    }

    return list;
  }

  private Log log;
  @SuppressWarnings("rawtypes")
private Map pluginContext;

  public void setLog(Log log) {
    this.log = log;
  }

  public Log getLog() {
    if (log == null) {
      log = new SystemStreamLog();
    }

    return log;
  }

  @SuppressWarnings("rawtypes")
public Map getPluginContext() {
    return pluginContext;
  }

  @SuppressWarnings("rawtypes")
  public void setPluginContext(Map pluginContext) {
    this.pluginContext = pluginContext;
  }
  @Override
  protected boolean isUseTestSources() {
    return Boolean.TRUE.equals(includeTestSource);
  }
}
