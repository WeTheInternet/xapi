package net.wti.gradle.settings.api;

import net.wti.gradle.api.BuildCoordinates;

/**
 * ModuleIdentity:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 14/06/2024 @ 8:17 p.m.
 */
public interface ModuleIdentity {
    static String toKey(BuildCoordinates coords, String path, PlatformModule platMod) {
        return coords.getBuildName() + "~" + path + "~" + platMod.toStringStrict();
    }

    PlatformModule getPlatformModule();
    BuildCoordinates getBuildCoordinates();
    String getProjectPath();
}
