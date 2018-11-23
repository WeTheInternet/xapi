package xapi.gradle.api;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/5/18 @ 12:29 AM.
 */
public interface HasArchiveType {

    ArchiveType getArchiveType();

    default void setArchiveType(ArchiveType type) {
        throw new UnsupportedOperationException(getClass() + " does not implement setArchiveType");
    }

}
