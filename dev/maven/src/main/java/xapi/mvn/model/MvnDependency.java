package xapi.mvn.model;

import xapi.model.api.Model;

public interface MvnDependency extends MvnArtifact{

  String groupId();
  String artifactId();
  String version();
  String extension();
  String classifier();

  MvnDependency groupId(String groupId);
  MvnDependency artifactId(String artifactId);
  MvnDependency version(String version);
  MvnDependency extension(String extension);
  MvnDependency classifier(String classifier);

}
