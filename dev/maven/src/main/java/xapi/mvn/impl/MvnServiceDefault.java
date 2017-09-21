package xapi.mvn.impl;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import xapi.annotation.inject.SingletonDefault;
import xapi.collect.impl.AbstractMultiInitMap;
import xapi.dev.resource.impl.StringDataResource;
import xapi.dev.scanner.X_Scanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.fu.Filter.Filter1;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.fu.Out1;
import xapi.inject.impl.SingletonInitializer;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.mvn.X_Maven;
import xapi.mvn.api.MvnDependency;
import xapi.mvn.service.MvnService;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Debug;
import xapi.util.X_String;
import xapi.util.X_Util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

@SingletonDefault(implFor = MvnService.class)
public class MvnServiceDefault implements MvnService {

  private final MvnCacheImpl cache;

  public MvnServiceDefault() {
    cache = new MvnCacheImpl(this);
  }

  private static final Pattern POM_PATTERN = Pattern.compile(".*pom.*xml");

  protected final DefaultServiceLocator maven = MavenRepositorySystemUtils.newServiceLocator();

  protected final SingletonInitializer<RepositorySystem> repoSystem = new SingletonInitializer<RepositorySystem>() {
    @Override
    protected RepositorySystem initialValue() {
      // use netty to stream from maven

      maven.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
      maven.addService( TransporterFactory.class, FileTransporterFactory.class );
      maven.addService( TransporterFactory.class, HttpTransporterFactory.class );
      return maven.getService(RepositorySystem.class);
    }
  };

  @Override
  public RepositorySystemSession getRepoSession() {
    return session.get();
  }

  protected final SingletonInitializer<RepositorySystemSession> session = new SingletonInitializer<RepositorySystemSession>() {
    @Override
    protected RepositorySystemSession initialValue() {
      return initLocalRepo();
    }
  };

  private LogLevel logLevel = LogLevel.INFO;

  private final class ResourceMap extends
      AbstractMultiInitMap<Integer, ClasspathResourceMap, ClassLoader> {
    @SuppressWarnings("unchecked")
    public ResourceMap() {
      super(TO_STRING);
    }

    @Override
    protected ClasspathResourceMap initialize(Integer key, ClassLoader params) {
      return X_Scanner.scanClassloader(params);
    }
  }

  @Override
  public ArtifactResult loadArtifact(String groupId, String artifactId, String version) {
    return loadArtifact(groupId, artifactId, "", "jar", version);
  }

  @Override
  public ArtifactResult loadArtifact(String groupId, String artifactId,
      String classifier, String extension, String version) {
    Moment before = X_Time.now();
    RepositorySystem repoSystem = this.repoSystem.get();
    RepositorySystemSession session = this.session.get();

    DefaultArtifact artifact = new DefaultArtifact( groupId,artifactId, normalize(classifier), X_String.isEmpty(extension) ? "jar" : extension, version);

    try {
      final LocalArtifactRequest localRequest = new LocalArtifactRequest(artifact, remoteRepos(), null);
      final LocalArtifactResult result = session.getLocalRepositoryManager().find(session, localRequest);
      ArtifactRequest request = new ArtifactRequest(artifact, remoteRepos(), null);
      if (result.isAvailable()) {
        final ArtifactResult artifactResult = new ArtifactResult(request);
        final Artifact withFile = artifact.setFile(result.getFile());
        artifactResult.setArtifact(withFile);
        artifactResult.setRepository(result.getRepository());
        return artifactResult;
      }
      return repoSystem.resolveArtifact(session, request);
    } catch (ArtifactResolutionException e) {
      X_Log.log(getClass(), getLogLevel(), "Resolved? ", e.getResult().isResolved(), e.getResult().getExceptions());
      X_Log.log(getClass(), getLogLevel(), "Could not download " + artifact, e);
      throw X_Debug.rethrow(e);
    } finally {
      if (X_Log.loggable(LogLevel.DEBUG)) {
        X_Log.debug("Resolved: " + artifact.toString() + " in "
            + X_Time.difference(before));
      }
    }
  }

  @Override
  public LocalArtifactResult loadLocalArtifact(String groupId, String artifactId, String version) {
    return loadLocalArtifact(groupId, artifactId, "", "jar", version);
  }

  @Override
  public LocalArtifactResult loadLocalArtifact(String groupId, String artifactId,
      String classifier, String extension, String version) {
    Moment before = X_Time.now();
    RepositorySystemSession session = this.session.get();
    DefaultArtifact artifact = new DefaultArtifact(
        groupId,
        artifactId,
        normalize(classifier),
        X_String.isEmpty(extension) ? "jar" : extension,
        version
    );

    try {
      LocalArtifactRequest request = new LocalArtifactRequest(artifact, null, null);
      return session.getLocalRepositoryManager().find(session, request);
    } finally {
      if (X_Log.loggable(LogLevel.DEBUG)) {
        X_Log.debug("Resolved: " + artifact.toString() + " in "
            + X_Time.difference(before));
      }
    }
  }

  @Override
  public String normalize(String classifier) {
    if (classifier == null) {
      return "";
    }

    if ("${os.detected.classifier}".equals(classifier)) {
      String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
      if (os.contains("win")) {
        return "windows-x86_64";
      } else if (os.contains("mac")) {
        return "osx-x86_64";
      } else {
        return "linux-x86_64";
      }
    }
    return classifier;
  }

  protected LogLevel getLogLevel() {
    return logLevel;
  }

  @Override
  public void setLogLevel(LogLevel logLevel) {
    this.logLevel = logLevel;
  }

  protected RepositorySystemSession initLocalRepo() {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
    String userHome = System.getProperty("user.home");
    String path = "target/local-repo";
    if (userHome != null) {
      File maybeUse = new File(userHome, ".m2/repository");
      if (maybeUse.exists()) {
        path = maybeUse.getAbsolutePath();
      }
    }
    LocalRepository localRepo = new LocalRepository(path);
    localRepo.getBasedir().mkdirs();
    session.setLocalRepositoryManager(repoSystem.get()
        .newLocalRepositoryManager(session, localRepo));
    return session;
  }

  @Override
  public Model loadPomFile(String pomLocation) throws IOException,
      XmlPullParserException {
    File pomfile = new File(pomLocation);
    FileReader reader;
    MavenXpp3Reader mavenreader = new MavenXpp3Reader();
    reader = new FileReader(pomfile);
    Model model = mavenreader.read(reader);
    model.setPomFile(pomfile);
    return model;
  }

  @Override
  public Model loadPomString(String pomString) throws XmlPullParserException {
    try {
      return new MavenXpp3Reader().read(new StringReader(pomString));
    } catch (IOException ignored) {
      throw X_Util.rethrow(ignored);
    }
  }

  @Override
  public List<String> loadDependencies(Artifact artifact, Filter1<Dependency> filter) {
    Map<String, String> dependencies = Collections.synchronizedMap(new LinkedHashMap<>());

    loadInto(dependencies, artifact, filter);

    return new ArrayList<>(dependencies.values());
  }

  @Override
  public Out1<MappedIterable<String>> downloadDependencies(MvnDependency dep) {

    final Lazy<List<String>> artifact = Lazy.deferred1(()->{
      final ArtifactResult result = X_Maven.loadArtifact(
          dep.getGroupId(),
          dep.getArtifactId(),
          dep.getClassifier(),
          dep.getPackaging(),
          dep.getVersion()
      );
      return loadDependencies(result.getArtifact(), check->
          !"test".equals(check.getScope()) && !"system".equals(check.getScope())
              && !check.isOptional()
      );
    });
    // Start download of artifact info immediately, but do not block
    runLater(artifact.ignoreOut1().toRunnable());
    // Return a string output that will block on the lazy initializer
    return artifact.map(MappedIterable::mapped);
  }

  protected void runLater(Runnable runnable) {
    X_Time.runLater(runnable);
  }

  private void loadInto(Map<String, String> dependencies, Artifact artifact, Filter1<Dependency> filter) {
    String artifactString = toArtifactString(artifact);
    if (!dependencies.containsKey(artifactString)) {
      String fileLoc = artifact.getFile().getAbsolutePath();
      dependencies.put(artifactString, fileLoc);
      try (
          JarFile jar = new JarFile(artifact.getFile())
       ) {
        final ZipEntry pomEntry = jar.getEntry("META-INF/maven/" + artifact.getGroupId() + "/" + artifact.getArtifactId() + "/pom.xml");
        if (pomEntry != null) {
          // some jars, like javax.inject, do not package a pom inside the jar :-/
          String pomString = X_IO.toStringUtf8(jar.getInputStream(pomEntry));
          final Model pom = loadPomString(pomString);
          loadDependencies(dependencies, jar, pom, filter);
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      } catch (XmlPullParserException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private String toArtifactString(Artifact artifact) {
    if (artifact.getExtension() == null) {
      if (artifact.getClassifier() == null) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
      } else {
        // classifier is the fifth coordinate type, which implicitly uses jar for extension / packaging type
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":jar:" + artifact.getClassifier() + ":" + artifact.getVersion();
      }
    } else {
      if (artifact.getClassifier() == null) {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getExtension() + ":" + artifact.getVersion();
      } else {
        return artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getExtension() + ":" + artifact.getClassifier() + ":" + artifact.getVersion();
      }
    }
  }

  private void loadDependencies(Map<String, String> dependencies, JarFile jar, Model pom, Filter1<Dependency> filter) {
    pom.getDependencies().parallelStream().forEach(dependency -> {
      if (filter.filter1(dependency)) {
        final ZipEntry pomEntry = jar.getEntry("META-INF/maven/" + cache.resolveProperty(pom, "${pom.groupId}") + "/" + dependency.getArtifactId() + "/pom.xml");
        if (pomEntry != null) {
          // If the pom of the dependency is in the jar, it is very likely a shaded jar that already contains the
          // rest of the contents of the dependency, so we should skip it.
          return;
        }
        if (dependency.getVersion() == null) {
          // dependency management was used somewhere along the way :-/
          if (pom.getDependencyManagement() != null) {
            for (Dependency dep : pom.getDependencyManagement().getDependencies()) {
              if (dep.getGroupId().equals(dependency.getGroupId()) && dep.getArtifactId().equals(dependency.getArtifactId())) {
                loadDependency(dependencies, pom, dep, filter);
                return;
              }
            }
          }
          // ugh.  We have to look up parent dependency chains.
          Parent parent = pom.getParent();
          while (parent != null) {
            final ArtifactResult parentArtifact = loadArtifact(
                parent.getGroupId(),
                parent.getArtifactId(),
                "",
                "pom",
                parent.getVersion()
            );
            try {
              final Model parentPom = loadPomFile(parentArtifact.getArtifact().getFile().getAbsolutePath());
              if (parentPom.getDependencyManagement() != null) {
                for (Dependency dep : parentPom.getDependencyManagement().getDependencies()) {
                  if (
                      dep.getGroupId().equals(dependency.getGroupId()) &&
                          dep.getArtifactId().equals(dependency.getArtifactId()) &&
                          Objects.equals(dep.getClassifier(), dependency.getClassifier())) {
                    loadDependency(dependencies, pom, dep, filter);
                    return;
                  }
                }
              }
              parent = parentPom.getParent();
            } catch (IOException | XmlPullParserException e) {
              throw new RuntimeException(e);
            }
          }
        } else {
          loadDependency(dependencies, pom, dependency, filter);
        }
      }
    });

  }

  private void loadDependency(Map<String, String> dependencies, Model pom, Dependency dependency, Filter1<Dependency> filter) {
    String artifactString = cache.toArtifactString(pom, dependency);
    if (!dependencies.containsKey(artifactString)) {
      final Artifact artifact = cache.loadArtifact(pom, dependency);
      loadInto(dependencies, artifact, filter);
    }
  }

  @Override
  public String mvnHome() {
    return System.getenv("M2_HOME");
  }

  @Override
  public String localRepo() {
    return session.get().getLocalRepository().getBasedir().getAbsolutePath();
  }

  @Override
  public List<RemoteRepository> remoteRepos() {
    return Arrays.asList(new RemoteRepository
      .Builder("maven-central", "default", "http://repo1.maven.org/maven2/")
      .build()
    );
  }

  private final ResourceMap loaded = new ResourceMap();

  @Override
  public Iterable<Model> findPoms(final ClassLoader loader) {
    final Iterable<StringDataResource> poms = loaded.get(loader.hashCode(),
        loader).findResources("", POM_PATTERN);
    class Itr implements Iterator<Model> {
      Iterator<StringDataResource> iterator = poms.iterator();

      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Model next() {
        StringDataResource next = iterator.next();
        try {
          return loadPomString(next.readAll());
        } catch (Exception e) {
          X_Log.error("Unable to load resouce " + next.getResourceName(), e);
          throw X_Util.rethrow(e);
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }

    }
    return new Iterable<Model>() {
      @Override
      public Iterator<Model> iterator() {
        return new Itr();
      }
    };
  }

}
