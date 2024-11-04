package net.wti.gradle.settings.api;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/1/19 @ 2:50 AM.
 */
public enum DependencyKey {
    /**
     * The publishing group / gradle project name
     */
    group,
    /**
     * The "root" name of the archive (no platform or archive type)
     */
    name,
    /**
     * An object to determine the version.  For now, if used, will be a string.
     */
    version,
    /**
     * Only meaningful for external dependencies. Handles both :classifier and :classifier@type (we ignore the @)
     */
    classifier,
    /**
     * The appendix, containing platform/archive type suffix.
     * `main-api-source` or `gwt-spi`;
     */
    appendix,
    /**
     * Set to true/"true" to ensure external dependency metadata is checked / archive re-downloaded.
     * You should explicitly set this to "false" when you know the archive will never change,
     * as we may add a convention to control the default changing attribute.
     */
    changing,
    /**
     * Maps to a list of {@code Map<DependencyKey, ?>}s.
     * If this property is set, then g:n:v would apply for publishing of the artifact.
     *
     * It is implied that `{external: true}` when composite is set.
     * Explicitly setting external to ~"false" with non-null composite is an error state.
     */
    composite,
    /**
     * Set to "project", "internal", "external" or "unknown".
     * Normally stores a {@link DependencyType} value.
     */
    category,
    /**
     * Set to a Map containing new coordinates for this artifact.
     * Not really supported yet, just leaving it here as an idea.
     *
     * When implemented, would only apply when `{external: true}`.
     */
    republish,
    /**
     * A place to stash extra objects;
     * this should point to a map with a less restrictive key type (String).
     */
    extra,
    /**
     * A $platform:$module pair, if no : present, assume implicit main:$module.
     * Normally store a {@link PlatformModule} value.
     */
    platformModule,
    /**
     * Use @transitive or @transitive(true) to ensure transitive (api) dependency,
     * or use @transitive(false) to ensure non-transitive (compileOnly) dependency.
     * Omitting @transitive nets a default scope of implementation.
     */
    transitive,
    /**
     * Use @test to denote that the given dependencies are test dependencies (useful for non-multi-module projects)
     */
    test,
}

