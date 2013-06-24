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
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.MavenServiceLocator;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.connector.async.AsyncRepositoryConnectorFactory;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.impl.AbstractMultiInitMap;
import xapi.dev.X_Dev;
import xapi.dev.scanner.ClasspathResourceMap;
import xapi.dev.scanner.StringDataResource;
import xapi.inject.impl.SingletonInitializer;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.mvn.service.MvnService;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_String;
import xapi.util.X_Util;

@SingletonDefault(implFor = MvnService.class)
public class MvnServiceDefault implements MvnService {

  public MvnServiceDefault() {
  }

  private static final Pattern POM_PATTERN = Pattern.compile(".*pom.*xml");

  protected final MavenServiceLocator maven = new MavenServiceLocator();

  protected final SingletonInitializer<RepositorySystem> repoSystem = new SingletonInitializer<RepositorySystem>() {
    protected RepositorySystem initialValue() {
      // use netty to stream from maven
      maven.addService(RepositoryConnectorFactory.class,
          AsyncRepositoryConnectorFactory.class);
      // maven.addService(RepositoryConnectorFactory.class,
      // WagonRepositoryConnectorFactory.class);
      return maven.getService(RepositorySystem.class);
    }
  };

  @Override
  public RepositorySystemSession getRepoSession() {
    return session.get();
  }

  protected final SingletonInitializer<RepositorySystemSession> session = new SingletonInitializer<RepositorySystemSession>() {
    protected RepositorySystemSession initialValue() {
      return initLocalRepo();
    }
  };

  private final class ResourceMap extends
      AbstractMultiInitMap<Integer, ClasspathResourceMap, ClassLoader> {
    @SuppressWarnings("unchecked")
    public ResourceMap() {
      super(TO_STRING);
    }

    @Override
    protected ClasspathResourceMap initialize(Integer key, ClassLoader params) {
      return X_Dev.scanClassloader(params);
    }
  }

  @Override
  public ArtifactResult loadArtifact(String groupId, String artifactId,
      String classifier, String extension, String version) {
    Moment before = X_Time.now();

    RepositorySystemSession sess = session.get();
    Artifact artifact = new DefaultArtifact(groupId, artifactId, classifier,
        X_String.isEmpty(extension) ? "jar" : extension, version);
    ArtifactRequest request = new ArtifactRequest(artifact, remoteRepos(), null);
    try {
      return repoSystem.get().resolveArtifact(sess, request);
    } catch (ArtifactResolutionException e1) {
      X_Log.error(e1.getResult().isResolved(), e1.getResult().getExceptions());
      e1.printStackTrace();
      X_Log.error("Could not download " + artifact, e1);
      throw X_Util.rethrow(e1);
    } finally {
      if (X_Log.loggable(LogLevel.DEBUG))
        X_Log.debug("Resolved: " + artifact.toString() + " in "
            + X_Time.difference(before));
    }
  }

  protected RepositorySystemSession initLocalRepo() {
    MavenRepositorySystemSession session = new MavenRepositorySystemSession();
    LocalRepository localRepo = new LocalRepository("target/local-repo");
    localRepo.getBasedir().mkdirs();
    session.setLocalRepositoryManager(repoSystem.get()
        .newLocalRepositoryManager(localRepo));
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
    return Arrays.asList(new RemoteRepository("maven-central", "default",
        "http://repo1.maven.org/maven2/"));
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
