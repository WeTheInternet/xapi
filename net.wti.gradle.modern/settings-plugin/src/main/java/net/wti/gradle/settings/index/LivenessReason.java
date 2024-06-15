package net.wti.gradle.settings.index;

/**
 * LivenessReason:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 10/06/2024 @ 1:46 a.m.
 */
enum LivenessReason {
    /**
     * If a module's only reason to be live is that it has includes, then it is elligible for compression.
     */
    has_includes,
    /**
     * A module with source has some files in src/platMod/*
     */
    has_source,
    /**
     * A module with buildscript changes has a non-default modules/platMod/platMod.gradle file
     */
    has_buildscript,
    /**
     * A module with explicit external / required dependencies will be forced to exist
     */
    has_dependencies,
    /**
     * A module that is an explicit / required dependency of another module will be forced to exist.
     */
    is_dependency,
    /**
     * A module that was forced to exist via force=true in schema.xapi
     */
    forced
}
