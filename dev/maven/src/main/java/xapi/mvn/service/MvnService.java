package xapi.mvn.service;

import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Model;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResult;

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

}
