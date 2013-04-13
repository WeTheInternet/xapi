package xapi.mvn.model;

import xapi.model.api.Model;

public interface MvnArtifact extends Model {


  String groupId();
  String artifactId();
  String version();

  MvnArtifact groupId(String groupId);
  MvnArtifact artifactId(String artifactId);
  MvnArtifact version(String version);




}
