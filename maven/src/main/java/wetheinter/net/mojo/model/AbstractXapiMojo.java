package wetheinter.net.mojo.model;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.sonatype.aether.impl.ArtifactResolver;
import org.sonatype.aether.repository.ArtifactRepository;

import com.google.common.base.Preconditions;

public abstract class AbstractXapiMojo extends AbstractMojo{


  /** @component */
  private ArtifactResolver artifactResolver;

  /** @component */
  private ProjectBuilder builder;

  /** @parameter default-value="${localRepository}" */
  private ArtifactRepository localRepository;

  /** @component */
  private MavenProjectHelper projectHelper;

  /**
   * The runtime we are targeting
   *
   * @parameter expression="${xapi.platform}"
   * @required
   * @readonly
   */
  private String platform;

  /**
   * Base file from which all relative uris are resolved
   *
   * @parameter expression="${source.root}" default-value="${project.basedir}/.."
   * @required
   * @readonly
   */
  private File sourceRoot;

  /**
   * The Maven Session Object
   *
   * @parameter expression="${session}"
   * @required
   * @readonly
   */
  private MavenSession session;

  /**
   * A target project to use for dynamically building MavenProjects from other poms.
   * This value can be a simple string as relative from your ${source.root} directory.
   *
   * So long as you stick with the pom.xml naming convention, that is.
   *
   * You may also provide an absolute path name,
   * or even a fully qualified artifact ID to load from local repo.
   *
   * Artifact id must contain at least the following
   * groupId:artifactId:version is the exact format provided.
   *
   * You may optionally include a classifier, as
   * groupId:artifactId:classifier:version
   *
   * @parameter expression="${target.project}" default-value="${project.basedir}/.."
   * @readonly
   */
  private String targetProject;

  public ArtifactResolver getArtifactResolver() {
    return artifactResolver;
  }

  public ProjectBuilder getBuilder() {
    return builder;
  }

  public ArtifactRepository getLocalRepository() {
    return localRepository;
  }

  public MavenProjectHelper getProjectHelper() {
    return projectHelper;
  }

  public String getPlatform() {
    return platform;
  }

  public MavenSession getSession() {
    return session;
  }

  public File getSourceRoot() {
    return sourceRoot;
  }

  public File getTargetPom() {
    Preconditions.checkNotNull(targetProject, "You must supply a ${target.project} configuration property " +
    		"in order to use any service methods which depend upon #getTargetPom().");
    boolean endsWithXml = targetProject.endsWith(".xml");
    String target;
    if (endsWithXml) {
      target = targetProject;
    }else {
      target = targetProject+".pom.xml";
    }
    //first, check for absolute file.
    File targetFile = new File(target);
    if (targetFile.exists())
      return targetFile;

    //okay, no absolute file.  Now check relative to source root.
    targetFile = new File(sourceRoot, target);
    if (targetFile.exists())
      return targetFile;

    //no hits... check for artifact id to load from repo
    //TODO: actually bother with this...

    return targetFile;
  }

}
