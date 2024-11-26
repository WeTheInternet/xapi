package xapi.mojo.api;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 23/11/15.
 */
public class SourceDependency {

  public SourceDependency() {
  }

  private String groupId;

  private String artifactId;

  private boolean includeTests;

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public boolean isIncludeTests() {
    return includeTests;
  }

  @Override
  public String toString() {
    return "SourceDependency{" +
        "groupId='" + groupId + '\'' +
        ", artifactId='" + artifactId + '\'' +
        ", includeTests=" + includeTests +
        '}';
  }
}
