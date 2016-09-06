package xapi.mvn.model;

import xapi.model.api.Model;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/4/16.
 */
public interface MvnCoords <Self extends MvnCoords<Self>> extends Model {

    String getGroupId();
    String getArtifactId();
    String getVersion();

    Self setGroupId(String groupId);
    Self setArtifactId(String artifactId);
    Self setVersion(String version);

}
