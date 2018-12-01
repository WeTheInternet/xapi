package xapi.gradle.api;

/**
 * A tuple of a {@link ArchiveType}, a project module id and a String platformName
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 11/29/18 @ 3:40 AM.
 */
public interface ArchivePath {
    ArchiveType getType();

    String getModuleId();

    String getPlatformName();
}
