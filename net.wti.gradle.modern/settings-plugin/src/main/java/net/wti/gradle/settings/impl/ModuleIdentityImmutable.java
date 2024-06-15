package net.wti.gradle.settings.impl;

import net.wti.gradle.api.BuildCoordinates;
import net.wti.gradle.settings.api.ModuleIdentity;
import net.wti.gradle.settings.api.PlatformModule;
import org.gradle.api.NonNullApi;

import java.util.Objects;

/**
 * ModuleIdentityImmutable:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 14/06/2024 @ 8:19 p.m.
 */
@NonNullApi
public class ModuleIdentityImmutable implements ModuleIdentity {
    private final BuildCoordinates coords;
    private final String path;
    private final PlatformModule platMod;
    private final String serialized;

    public ModuleIdentityImmutable(final BuildCoordinates coords, final String path, final PlatformModule platMod) {
        this.coords = coords;
        this.path = path;
        this.platMod = platMod;
        serialized = ModuleIdentity.toKey(coords, path, platMod);
    }

    @Override
    public PlatformModule getPlatformModule() {
        return platMod;
    }

    @Override
    public BuildCoordinates getBuildCoordinates() {
        return coords;
    }

    @Override
    public String getProjectPath() {
        return path;
    }

    @Override
    public String toString() {
        return serialized;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ModuleIdentityImmutable that = (ModuleIdentityImmutable) o;
        return Objects.equals(serialized, that.serialized);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serialized);
    }
}
