package xapi.mvn;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import xapi.bytecode.impl.BytecodeAdapterService;
import xapi.dev.X_Dev;
import xapi.dev.scanner.X_Scanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.fu.Filter.Filter1;
import xapi.inject.X_Inject;
import xapi.inject.impl.SingletonProvider;
import xapi.log.X_Log;
import xapi.mvn.impl.ProjectIterable;
import xapi.mvn.service.MvnService;
import xapi.util.X_Debug;
import xapi.util.X_Runtime;
import xapi.util.X_String;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class X_Maven {

  private static final MvnService service = X_Inject
      .singleton(MvnService.class);

  public static Model loadPom(String fileLocation) {
    try {
      return service.loadPomFile(fileLocation);
    } catch (Exception e) {
      throw X_Debug.rethrow(e);
    }
  }

  public static Model loadPomString(String pomString) {
    try {
      return service.loadPomString(pomString);
    } catch (Exception e) {
      throw X_Debug.rethrow(e);
    }
  }

  public static List<RemoteRepository> remoteRepos() {
    return service.remoteRepos();
  }

  public static MvnService getMavenService() {
    return service;
  }

  public static ArtifactResult loadArtifact(String groupId, String artifactId,
      String version) {
    return loadArtifact(groupId, artifactId, "jar", version);
  }

  public static List<String> loadCompileDependencies(Artifact artifact) {
    return loadDependencies(artifact, d->
        d.getScope() == null ? "tests".equals(d.getType()) : "compile".equals(d.getScope()));
  }

  public static List<String> loadDependencies(Artifact artifact, Filter1<Dependency> filter) {
    return service.loadDependencies(artifact, filter);
  }

  public static ArtifactResult loadArtifact(String groupId, String artifactId,
      String extension, String version) {
    return loadArtifact(groupId, artifactId, "", extension, version);
  }

  public static ArtifactResult loadArtifact(String groupId, String artifactId,
      String classifier, String extension, String version) {
    return service.loadArtifact(groupId, artifactId, classifier, extension,
        version);
  }

  public static LocalArtifactResult loadLocalArtifact(String groupId, String artifactId,
      String version) {
    return loadLocalArtifact(groupId, artifactId, "jar", version);
  }

  public static LocalArtifactResult loadLocalArtifact(String groupId, String artifactId,
      String extension, String version) {
    return loadLocalArtifact(groupId, artifactId, "", extension, version);
  }

  public static LocalArtifactResult loadLocalArtifact(String groupId, String artifactId,
      String classifier, String extension, String version) {
    return service.loadLocalArtifact(groupId, artifactId, classifier, extension,
      version);
  }

  public static String toDescriptor(Model model, boolean verbose) {
    String artifactId = model.getArtifactId();
    assert artifactId != null : "Null artifactId for " + model;
    String version = model.getVersion();
    if (version == null) {// allowed null, since parent doesn't allow null
      version = model.getParent().getVersion();
    }
    assert version != null : "No version for " + model;
    if (verbose) {
      String groupId = model.getGroupId();
      if (groupId == null) {// only null if it matches the parent
        // <parent> element can't have null groupId.
        groupId = model.getParent().getGroupId();
      }
      assert groupId != null : "Null groupId for " + model.getArtifactId();
      return groupId + ":" + artifactId + ":" + version;
    }
    return artifactId + ":" + version;
  }

  public static ClasspathResourceMap compileScopeScanner(MavenProject project,
      MavenSession session) {
    URL[] urls = compileScopeUrls(project, session);
    X_Log.debug(X_Maven.class,"Compile scope URLS",urls);
    return X_Scanner.scanClassloader(URLClassLoader.newInstance(urls));
  }

  public static URL[] compileScopeUrls(MavenProject project,
      MavenSession session) {
    try {
      List<String> compile = project.getCompileClasspathElements();
      X_Log.debug(X_Maven.class,"Compile classpath",compile);
      X_Log.debug(X_Maven.class,"Runtime classpath",project.getRuntimeClasspathElements());
      if (project.hasLifecyclePhase("test-classes")) {
        List<String> testElements = project.getTestClasspathElements();
        testElements.addAll(compile);
        compile = testElements;
      }
      URL[] urls = new URL[compile.size()];
      for (int i = compile.size(); i-- > 0;) {
        urls[i] = X_Dev.toUrl(compile.get(i));
      }
      return urls;
    } catch (DependencyResolutionRequiredException e) {
      throw X_Debug.rethrow(e);
    }
  }

  public static BytecodeAdapterService compileScopeAdapter(
      MavenProject project, MavenSession session) {
    return new CompileScopeBytecodeAdapter(project, session);
  }

  private static class CompileScopeBytecodeAdapter extends BytecodeAdapterService {

    private final URL[] urls;
    private final SingletonProvider<ClassLoader> cl = new SingletonProvider<ClassLoader>() {
      @Override
      protected ClassLoader initialValue() {
        if (X_Runtime.isDebug()) {
          X_Log.info("Maven compile scope: "+X_String.joinObjects(urls));
        }
        return new URLClassLoader(urls, X_Maven.class.getClassLoader());
      };
    };
    public CompileScopeBytecodeAdapter(MavenProject project, MavenSession session) {
      this.urls = compileScopeUrls(project, session);
    }
    @Override
    protected URL[] getScanUrls() {
      return urls;
    }

    @Override
    protected ClassLoader getClassLoader() {
      return cl.get();
    }

  }

  public static MavenProject getRootArtifact(MavenProject project) {
    final MavenProject parent = project.getParent();
    if (parent == null) {
      return project;
    }
    final String thisDirectory = project.getFile().getParent();
    final String parentDirectory = parent.getFile().getParent();
    if (thisDirectory.startsWith(parentDirectory)) {
      return getRootArtifact(project.getParent());
    }
    return project;
  }

  public static Iterable<MavenProject> getAllChildren(MavenProject project, ProjectBuilder builder, ProjectBuildingRequest request) {
    return getAllChildren(project, builder, request, true);
  }

  public static Iterable<MavenProject> getAllChildren(MavenProject project, ProjectBuilder builder, ProjectBuildingRequest request, boolean includeSelf) {
    return new ProjectIterable(project, builder, request, includeSelf);
  }

}
