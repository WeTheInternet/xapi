package xapi.gradle.common;

import xapi.gradle.api.ArchivePath;
import xapi.gradle.api.ArchiveType;

import java.util.Objects;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/29/18 @ 3:49 AM.
 */
class ImmutableArchivePath implements ArchivePath {
    private final ArchiveType type;
    private final String moduleId;
    private final String platformName;

    public ImmutableArchivePath(String moduleId, String platformName, ArchiveType type) {
        this.type = type;
        this.moduleId = moduleId;
        this.platformName = platformName;
    }

    @Override
    public ArchiveType getType() {
        return type;
    }

    @Override
    public String getModuleId() {
        return moduleId;
    }

    @Override
    public String getPlatformName() {
        return platformName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final ArchivePath that = (ArchivePath) o;

        if (!type.equals(that.getType()))
            return false;
        if (!moduleId.equals(that.getModuleId()))
            return false;
        return Objects.equals(platformName, that.getPlatformName());
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + moduleId.hashCode();
        result = 31 * result + (platformName != null ? platformName.hashCode() : 0);
        return result;
    }
}
