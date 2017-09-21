package xapi.mvn.api;

import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.util.X_String;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/4/16.
 */
public interface MvnCoords <Self extends MvnCoords<Self>> extends Model {

    String getGroupId();
    String getArtifactId();
    String getVersion();
    String getPackaging();
    String getClassifier();

    Self setGroupId(String groupId);
    Self setArtifactId(String artifactId);
    Self setVersion(String version);
    Self setPackaging(String extension);
    Self setClassifier(String classifier);

    /**
     * Per maven documentation, https://maven.apache.org/pom.html#Maven_Coordinates
     * Valid coordinate string formats are:
     * groupId:artifactId:version
     * groupId:artifactId:packaging:version
     * groupId:artifactId:packaging:classifier:version
     *
     * @param coords
     * @return
     */
    static MvnCoords<?> fromString(String coords) {
        MvnCoords model = X_Model.create(MvnCoords.class);
        String[] bits = coords.split(":");
        switch (bits.length) {
            case 5:
                model.setClassifier(bits[3]);
            case 4:
                model.setPackaging(bits[2]);
            case 3:
                model.setGroupId(bits[0]);
                model.setArtifactId(bits[1]);
                model.setVersion(bits[bits.length-1]);
                break;
            default:
            if (bits.length < 3) {
                throw new IllegalArgumentException("Need at least:three:coordinates; you sent: " + coords);
            }
            throw new IllegalArgumentException("Need:at:most:five:coordinates; you sent: " + coords);
        }
        return model;
    }

    static String toString(MvnCoords<?> c) {
        String start = c.getGroupId() + ":" + c.getArtifactId() + ":";
        final boolean hasType = X_String.isNotEmpty(c.getPackaging());
        if (X_String.isNotEmpty(c.getClassifier())) {
            if (hasType) {
                start += c.getPackaging() + ":";
            } else {
                start += "jar:";
            }
            start += c.getClassifier() + ":";
        } else if (X_String.isNotEmpty(c.getPackaging())) {
            start += c.getPackaging() + ":";
        }
        return start + c.getVersion();
    }
}
