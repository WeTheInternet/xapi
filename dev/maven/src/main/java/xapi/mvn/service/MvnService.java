package xapi.mvn.service;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import xapi.fu.Filter.Filter1;
import xapi.log.api.LogLevel;

import java.io.IOException;
import java.util.List;

public interface MvnService {

	String mvnHome();

	String localRepo();

	List<RemoteRepository> remoteRepos();

	Iterable<Model> findPoms(ClassLoader loader);

	ArtifactResult loadArtifact(String groupId, String artifactId,
			String classifier, String extension, String version);

	Model loadPomString(String pomString)
			throws IOException, XmlPullParserException;

	Model loadPomFile(String pomFile)
			throws IOException, XmlPullParserException;

  RepositorySystemSession getRepoSession();

  void setLogLevel(LogLevel logLevel);

  ArtifactResult loadArtifact(String groupId, String artifactId, String version);

  LocalArtifactResult loadLocalArtifact(String groupId, String artifactId, String version);

  LocalArtifactResult loadLocalArtifact(String groupId, String artifactId, String classifier, String extension,
      String version);

    List<String> loadDependencies(Artifact artifact, Filter1<Dependency> filter);
}
