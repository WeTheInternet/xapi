package xapi.mojo.api;

import com.google.common.base.Preconditions;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.WorkspaceReader;
import xapi.dev.X_Dev;
import xapi.fu.Lazy;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.mojo.gwt.MavenServiceMojo;
import xapi.mvn.X_Maven;
import xapi.time.X_Time;
import xapi.util.X_Debug;
import xapi.util.X_GC;
import xapi.util.X_Namespace;
import xapi.util.X_String;
import xapi.util.X_Util;

import javax.annotation.Generated;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @requiresProject true
 */
public abstract class AbstractXapiMojo extends AbstractMojo {

  @Component
  private ProjectBuilder builder;

  @Component
  private MavenProjectHelper projectHelper;

  /**
   * The project the mojo is being executed upon
   */
  @Component
  private MavenProject project;

  @Component
  private WorkspaceReader workspace;

  @Component()
  private ProjectDependenciesResolver resolver;

  /**
   * The directory in which to place generated resources
   */
  @Parameter(property = "xapi.gen.dir", defaultValue = "target/generated-sources/xapi")
  private String generateDirectory;

  /**
   * The version of xapi to use when programatically adding xapi artifacts
   */
  @Parameter(property = "xapi.version", defaultValue = X_Namespace.XAPI_VERSION)
  private String xapiVersion;

  /**
   * The loglevel to use by default
   */
  @Parameter(property = "xapi.log.level", defaultValue = "WARN")
  private String xapiLogLevel;

  /**
   * The runtime platform this mojo execution is targeting.
   */
  @Parameter(property = "xapi.platform", defaultValue = "jre")
  private String platform;

  /**
   * Base file from which all relative uris are resolved
   */
  @Parameter(property = "source.root", defaultValue = "${project.basedir}")
  private File sourceRoot;


  @Parameter(property = "xapi.source.artifacts")
  private SourceDependency[] sourceDependencies;

  @Parameter(property = "assertions")
  private Boolean assertions;

  @Parameter(property = "injection")
  private Map<String, String> injection;

  /**
   * The Maven Session Object, injected by plexus
   *
   */
  @Component
  private MavenSession session;

  @Component
  private PluginDescriptor plugin;

  /**
   * A target project to use for dynamically building MavenProjects from other
   * poms. This value can be a simple string as relative from your
   * ${source.root} directory.
   *
   * So long as you stick with the pom.xml naming convention, that is.
   *
   * You may also provide an absolute path name, or even a fully qualified
   * artifact ID to load from local repo.
   *
   * Artifact id must contain at least the following groupId:artifactId:version
   * is the exact format provided.
   *
   * You may optionally include a classifier, as
   * groupId:artifactId:classifier:version
   *
   */
  @Parameter(property = "target.project", defaultValue = "${project.basedir}")
  private String targetProject;

  @Parameter(property = "output.directory", defaultValue = "${project.build.directory}/generated-sources/xapi")
  private String outputDirectory;

  /**
   * Additional source roots to use when looking up source artifacts.
   *
   * These files should point to the parent pom from which you want to start searching.
   */
  @Parameter(property = "source.project.roots")
  private List<File> sourceProjectRoots;

  public ProjectBuilder getBuilder() {
    return builder;
  }

  public ProjectBuildingRequest getBuildingRequest() {
    return session.getProjectBuildingRequest();
  }

  public MavenProjectHelper getProjectHelper() {
    return projectHelper;
  }

  public PluginDescriptor getPluginDescriptor() {
    return plugin;
  }

  public String getPlatform() {
    return platform;
  }

  public MavenProject getProject() {
    X_Log.trace(getClass(), "project", project,"dependencies",project.getDependencies());
    return project;
  }

  public File getGenerateDirectory() {
    return generateDirectoryProvider.out1();
  }

  public MavenSession getSession() {
    return session;
  }

  public File getSourceRoot() {
    return sourceRoot;
  }

  public String getXapiVersion() {
    return xapiVersion;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void execute() throws MojoExecutionException, MojoFailureException {
    injection.forEach((k, v)->{
      String key = "xinject." + k;
      if (System.getProperty(key) == null) {
        System.setProperty(key, v);
      }
    });
    try {
      X_Log.logLevel(LogLevel.valueOf(xapiLogLevel));
    } catch (Throwable e){getLog().warn("Could not set xapi.log.level to "+xapiLogLevel, e);}

    MavenServiceMojo.init(this);
    doExecute();
    X_GC.deepDestroy(Class.class.cast(getClass()), this);
  }

  protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

  private Lazy<Map<String, MavenProject>> localWorkspace = Lazy.deferred1(() -> {
      final MavenProject root = X_Maven.getRootArtifact(getProject());
      final Iterable<MavenProject> children = X_Maven.getAllChildren(
          root,
          getBuilder(),
          getBuildingRequest()
      );

      final Map<String, MavenProject> map = new LinkedHashMap<String, MavenProject>();
      for (MavenProject child : children) {
        map.put(child.getGroupId()+":"+child.getArtifactId(), child);
      }

      if (sourceProjectRoots != null) {
        boolean warnOnce = true;
        for (File sourceProjectRoot : sourceProjectRoots) {
          if (sourceProjectRoot.isDirectory()) {
            sourceProjectRoot = new File(sourceProjectRoot, "pom.xml");
          }
          if (sourceProjectRoot.exists()) {
            try {
              final ProjectBuildingResult sourceProject = builder.build(sourceProjectRoot, getBuildingRequest());
              for (MavenProject child : X_Maven.getAllChildren(
                  sourceProject.getProject(),
                  getBuilder(),
                  getBuildingRequest()
              )) {
                String key = child.getGroupId()+":"+child.getArtifactId();
                if (map.containsKey(key)) {
                  final File existing = map.get(key).getFile();
                  if (existing.equals(child.getFile())) {
                    if (warnOnce) {
                      warnOnce = false;
                      X_Log.warn(getClass(), "Sourcepath ", existing, "is already added.");
                      X_Log.warn(getClass(), "You do not have to add source paths that are in the same maven project as the plugin is executed");
                    }
                  } else {
                    X_Log.warn(getClass(), "Duplicate artifact id found: ", key,
                        "Choosing", existing, "over", child.getFile());
                  }
                } else {
                  map.put(key, child);
                }
              }

            } catch (ProjectBuildingException e) {
              X_Log.warn(getClass(), "Unable to build source project root ", sourceProjectRoot, e);
            }
          } else {
            X_Log.warn(getClass(), "Unable to find source project root ", sourceProjectRoot);
          }
        }

      }

      return map;
    }
  );


  private final Lazy<File> targetProjectDirectory = Lazy.deferred1(() -> {
      Preconditions
          .checkNotNull(
              targetProject,
              "You must supply a ${target.project} configuration property "
                  + "in order to use any service methods which depend upon #getTargetPom().");
      boolean endsWithXml = targetProject.endsWith(".xml");
      // first, check for absolute file.
      File targetFile = new File(targetProject);
      try {
        targetFile = targetFile.getCanonicalFile();
      } catch (IOException ignored) {
      }
      if (endsWithXml && targetFile.exists()) {
        targetFile = targetFile.getParentFile();
      }
      if (targetFile.isDirectory()) {
        return targetFile;
      }
      try {
        // okay, no absolute file. Now check relative to source root.
        targetFile = new File(sourceRoot.getCanonicalFile(), targetProject);
        if (targetFile.exists()) {
          return targetFile.getParentFile();
        }
      } catch (IOException ignored) {
      }

        // Assume maven artifact
        String[] bits = targetProject.split(":", -1);
        if (bits.length < 2) {
          throw new AssertionError(
              "The target.project you supplied, "
                  + targetProject
                  + ", is neither the "
                  + "location of a pom file (*.xml)," +
                  " nor a maven artifact (groupId:artifactId:extension:version)");
        }
        DefaultArtifact artifact;
        if (bits.length == 2) {
          Preconditions
              .checkArgument(
                  bits[0].equals(X_Namespace.XAPI_GROUP_ID),
                  "Unless your target artifact, "
                      + targetProject
                      + " begins with group Id, "
                      + X_Namespace.XAPI_GROUP_ID
                      + ", you must supply, at the very least, groupId:artifactId:version");
          artifact = new DefaultArtifact(targetProject + ":"
              + X_Namespace.XAPI_VERSION);
        } else {
          artifact = new DefaultArtifact(targetProject);
        }
        // Check workspace first, since that is the most useful place for use to
        // resolve artifacts
        if (workspace != null) {
          File result = workspace.findArtifact(artifact);
          X_Log.info(getClass(), "Searching for target project directory from",result
              ,"derived from artifact",artifact);
          if (result != null) {
            return result.getParentFile().getParentFile();
          }
          // If we couldn't find the artifact directly from the workspace, we need to
          // look up the pom tree to the root, build all modules, and find the correct artifact.
          MavenProject root = getSession().getCurrentProject();
          while (root.getParent() != null) {
            root = root.getParent();
          }
          try {
            DefaultProjectBuildingRequest req = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
            req.setProject(root);
            req.setResolveDependencies(true);
            List<File> poms = new ArrayList<File>();
            poms.add(new File(root.getBasedir(), "pom.xml"));
            List<ProjectBuildingResult> res = builder.build(poms,true,req);
            for (ProjectBuildingResult proj : res) {
              String ident = proj.getProject().getArtifact().getId();
              if (ident.startsWith(targetProject)) {
                if (ident.equals(targetProject)||ident.startsWith(targetProject+":")) {
                  return proj.getProject().getBasedir();
                }
              }
            }
          } catch (ProjectBuildingException e) {
            e.printStackTrace();
          }
        }
        throw new RuntimeException(
            "Could not find pom file for "
                + targetProject + "; if you wish to target a project outside your "
                + "maven workspace, you must specify the full location of the project pom" +
                " (using groupId:artifactId only works if the given project "
                + "is accessible to WorkspaceReader)");
      }
  );

  private final Lazy<File> generateDirectoryProvider = Lazy.deferred1(() -> {
      if (outputDirectory != null) {
        File f = new File(outputDirectory);
        if (f.exists()) {
          return f;
        }
      }
      if (targetProject == null) {
        return new File(getSourceRoot(), generateDirectory);
      }
      File f = new File(generateDirectory);
      if (f.isAbsolute()) {
        X_Log.info(AbstractXapiMojo.class, "Using generated directory:", f, " (ignoring ", targetProjectDirectory.out1(), ")");
        return f;
      }
      X_Log.error(AbstractXapiMojo.class, "Using generated directory:", targetProjectDirectory.out1(), "+", generateDirectory);
      return new File(targetProjectDirectory.out1(), generateDirectory);
    }
  );

  public File getTargetProjectDirectory() {
    return targetProjectDirectory.out1();
  }

  public final Lazy<JavaCompiler> compiler = Lazy.deferred1(this::initCompiler);

  public final Lazy<String[]> compileClasspath = Lazy.deferred1(() -> {
      URL[] cp = X_Maven.compileScopeUrls(getProject(), getSession());
      return X_Dev.toStrings(cp);
    }
  );

  public MavenProject findInWorkspace(String groupId, String artifactId) {

    Map<String, MavenProject> projects = localWorkspace.out1();
    return projects.get(groupId+":"+artifactId);
  }

  public void compile(final String javaName, final String source,
      boolean overwrite, String... additionalClasspath) {
    File file = saveModel(javaName, source, overwrite);
    prepareCompile(file, javaName, source, overwrite, additionalClasspath).run();
  }

  public File saveModel(final String javaName, final String source,
      boolean overwrite) {
    File genDir = getGenerateDirectory();
    X_Log.trace(getClass(),"Preparing compile", genDir);
    genDir.mkdirs();
    String sourceName;
    if (javaName.endsWith(".java")) {
      sourceName = javaName.substring(0, source.length() - 5);
    } else {
      sourceName = javaName;
    }
    File f = new File(genDir, sourceName.replace('.',
        File.separatorChar) + ".java");
    try {
      f.getParentFile().mkdirs();
      if (!f.createNewFile()) {
        if (overwrite) {
          f.delete();
          f.createNewFile();
        } else {
          throw new RuntimeException("Source file " + f
              + " exists, but overwrite was false.");
        }

      }
      FileWriter writer = new FileWriter(f);
      try {
        writer.write(source);
      } finally {
        writer.close();
      }
    } catch (Exception e) {
      X_Log.error("Unable to save generated file", javaName, "to", f, e);
      throw X_Debug.rethrow(e);
    }
    return f;
  }
  public Runnable prepareCompile(File srcFile,final String javaName, final String source,
      boolean overwrite, String... additionalClasspath) {
    File genDir = getGenerateDirectory();
    String[] cp = compileClasspath.out1();
    if (additionalClasspath.length > 0) {
      String[] clone = Arrays.copyOf(additionalClasspath,
          additionalClasspath.length + cp.length);
      System.arraycopy(cp, 0, clone, additionalClasspath.length, cp.length);
      cp = clone;
    }
    String[] args = new String[] {
        "-sourcepath", genDir.getAbsolutePath() + File.separator,
        "-classpath", X_String.join(File.pathSeparator, cp),
        "-d", getProject().getBuild().getDirectory() + File.separator + "classes",
        "-proc:none",
        srcFile.getAbsolutePath() };
    final String[] finalArgs = Boolean.TRUE.equals(assertions)
     ? X_Util.pushOnto(args, "-ea") : args;

    X_Log.log(getClass(), logLevel(), "Compile arguments", finalArgs);

    return () -> {
      int result;
      try {
        result = compiler.out1().run(null, null, null, finalArgs);
      } catch (Exception e) {
        X_Log.error("Cannot compile", javaName, ":\n", source, e);
        throw X_Debug.rethrow(e);
      }
      if (result != 0) {
        throw new RuntimeException("Unable to compile generated source("
            + result + ")" + "\nClass: " + javaName + "\nSource: " + source);
      }
    };

  }

  protected LogLevel logLevel() {
    if (xapiLogLevel == null) {
      return LogLevel.INFO;
    }
    return LogLevel.valueOf(xapiLogLevel);
  }

  protected JavaCompiler initCompiler() {
    return ToolProvider.getSystemJavaCompiler();
  }

  public String findArtifact(String groupId, String artifactId,
      String extension, String version) {
    if (extension == null) {
      extension = "jar";
    }
//    WorkspaceReader workspace = getSession().getRepositorySession()
//        .getWorkspaceReader();
    File artifact = workspace.findArtifact(new DefaultArtifact(groupId,
        artifactId, extension, version));
    if (artifact != null) {
      return artifact.getAbsolutePath();
    }
    return null;
  }

  public String generatedAnnotation() {
    return "@"+Generated.class.getName()+"(" +
        "\n  value=\"" + getClass().getName()+ "\", " +
        "\n  comments=\"Generated by xapi:annogen\"," +
        "\n  date=\"" +X_Time.timestamp()+"\"" +
        ")";
  }

  public String guessVersion(String groupId, String backup) {
      if (getPluginDescriptor() != null) {
        for (ComponentDependency dep : getPluginDescriptor().getDependencies()) {
          if (dep.getGroupId().equals(groupId)) {
            return dep.getVersion();
          }
        }
      }
      if (getProject() != null) {
        for (Dependency dep : getProject().getDependencies()) {
          if (dep.getGroupId().equals(groupId)) {
            return dep.getVersion();
          }
        }
      }
      return backup;
  }

  public SourceDependency[] getSourceDependencies() {
    return sourceDependencies;
  }

  public void setSourceDependencies(SourceDependency[] sourceDependencies) {
    this.sourceDependencies = sourceDependencies;
  }

  public boolean hasSourceDependencies() {
    return sourceDependencies != null && sourceDependencies.length > 0;
  }
}
