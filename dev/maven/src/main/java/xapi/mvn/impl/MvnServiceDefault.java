package xapi.mvn.impl;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
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
import xapi.inject.impl.SingletonInitializer;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.mvn.service.MvnService;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Debug;
import xapi.util.X_String;
import xapi.util.X_Util;

@SingletonDefault(implFor = MvnService.class)
public class MvnServiceDefault implements MvnService {

  public MvnServiceDefault() {
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

    DefaultArtifact artifact = new DefaultArtifact( groupId,artifactId,classifier, X_String.isEmpty(extension) ? "jar" : extension, version);

    try {
      ArtifactRequest request = new ArtifactRequest(artifact, remoteRepos(), null);
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
    DefaultArtifact artifact = new DefaultArtifact( groupId,artifactId,classifier, X_String.isEmpty(extension) ? "jar" : extension, version);

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
    FileReader reader = null;
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
