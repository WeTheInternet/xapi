package net.wti.gradle.settings.api;

/**
 * Describes the level of transitivity for a given dependency.
 *
 * This is used to select the correct dependency bucket to
 * add a "properly configured Dependency" (purposely left vague).
 *
 * Read the descriptions of each enum member to see intended purpose.
 */
public enum Transitivity {
    /**
     * Resolved + visible at compiletime, visible at runtime.
     * Compiletime transitive, this dependency is on all downstream classpaths.
     *
     * Counterpart Transitivity: {@link #runtime}
     */
    api,

    /**
     * Resolved + visible at compiletime only.  Never transitive.
     *
     * Counterpart Transitivity: {@link #runtime_only}
     */
    compile_only,

    /**
     * Resolved + visible at compile time, only when no impl is present.
     * Runtime transitive, only when no impl is present.
     *
     * This is a dependency that is meant to be replaced.
     * It may be a functional "plain java" dependency,
     * or just some actual stub classes which an impl will replace.
     *
     * Dependencies in this bucket will only be used
     * when a better platform:module {@link #impl} is not found.
     */
    stub,

    /**
     * Resolved + visible at compile time.
     * Runtime transitive, visible when platforms match.
     *
     * Such dependencies should be interchangeable;
     * i.e. a base module makes a default impl available,
     * and a platform module may choose to replace the impl jar,
     * or simply extend it (i.e. replaces vs. requires).
     *
     * When present, impl dependencies will replace any competing {@link #stub} dependencies.
     *
     * This needs to be codified into {@link net.wti.gradle.internal.require.api.ArchiveRequest).
     *
     */
    impl,

    /**
     * Resolved + visible at compile time only, never at runtime.
     * These dependencies will be compiletime transitive,
     * but only to other modules of the current project.
     *
     * Foreign projects may choose to inherit internal dependencies.
     *
     * This means you can have an api module with internal dependencies
     * that are then shared to all modules in the project, but no further.
     *
     * Think of this like the "project-wide compile_only dependency pool" .
     *
     * Counterpart Transitivity: {@link #execution}
     */
    internal,

    /**
     * Resolved + visible + transitive at runtime.
     *
     * TODO: make runtime transitivity understand platform:module relationships;
     *   ie: put all candidates for a given module into a heap,
     *   and select the best match based on the consumer's platform:module attributes.
     *
     * Counterpart Transitivity: {@link #api}
     */
    runtime,

    /**
     * Resolved + visible at runtime.  Never transitive.
     *
     * Counterpart Transitivity: {@link #compile_only}
     */
    runtime_only,

    /**
     * Resolved + visible at runtime only.
     * Runtime transitive, when platform:module matches,
     * but only to other modules of the current project.
     *
     * Foreign projects may choose to inherit execution dependencies.
     *
     * This means you can have an api module with execution dependencies
     * that are then shared to all modules in the project, but no further.
     *
     * Think of this like the "project-wide runtime_only dependency pool" .
     *
     * Counterpart Transitivity: {@link #internal}
     *
     */
    execution
}
