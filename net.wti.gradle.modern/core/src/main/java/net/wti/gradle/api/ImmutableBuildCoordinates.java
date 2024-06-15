package net.wti.gradle.api;

/**
 * ImmutableProjectCoordinates:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 04/06/2024 @ 12:07 a.m.
 */
public class ImmutableBuildCoordinates implements BuildCoordinates {
    private final String buildName;
    private final String group;
    private final String version;

    public ImmutableBuildCoordinates(final String buildName, final String group, final String version) {
        this.buildName = buildName;
        this.group = group;
        this.version = version;
    }

    @Override
    public String getBuildName() {
        return buildName;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return buildName + '^' + group + '^' + version;
    }
}
