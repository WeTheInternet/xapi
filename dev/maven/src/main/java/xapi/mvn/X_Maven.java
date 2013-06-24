package xapi.mvn;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResult;

import xapi.bytecode.impl.BytecodeAdapterService;
import xapi.dev.X_Dev;
import xapi.dev.scanner.ClasspathResourceMap;
import xapi.inject.X_Inject;
import xapi.inject.impl.SingletonProvider;
import xapi.log.X_Log;
import xapi.mvn.service.MvnService;
import xapi.util.X_Debug;
import xapi.util.X_Runtime;
import xapi.util.X_String;

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

  public static ArtifactResult loadArtifact(String groupId, String artifactId,
      String extension, String version) {
    return loadArtifact(groupId, artifactId, "", extension, version);
  }

  public static ArtifactResult loadArtifact(String groupId, String artifactId,
      String classifier, String extension, String version) {
    return service.loadArtifact(groupId, artifactId, classifier, extension,
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
    return X_Dev.scanClassloader(URLClassLoader.newInstance(urls));
  }

  public static URL[] compileScopeUrls(MavenProject project,
      MavenSession session) {
    try {
      List<String> compile = project.getCompileClasspathElements();
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

}
