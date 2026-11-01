package net.wti.gradle.settings.api;

import org.gradle.api.Named;
import xapi.fu.Debuggable;
import xapi.fu.data.SetLike;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

///
/// Abstraction layer over a module descriptor, like `<mod-name require=["a", b] published=true />`
///
/// Created by James X. Nelson (James@WeTheInter.net) on 2020-02-06 @ 4:39 a.m.
///
public class SchemaModule implements Named {
    /** Selects generated build behavior; application artifacts are not Java publications. */
    public enum BuildKind {
        JAVA_LIBRARY("java-library"), ANDROID_APPLICATION("android-application");

        private final String schemaValue;

        BuildKind(final String schemaValue) {
            this.schemaValue = schemaValue;
        }

        public String getSchemaValue() {
            return schemaValue;
        }

        public static BuildKind fromSchema(final String value) {
            for (final BuildKind kind : values()) {
                if (kind.schemaValue.equals(value)) {
                    return kind;
                }
            }
            throw new IllegalArgumentException("Unknown buildKind '" + value
                    + "'; expected java-library or android-application");
        }
    }

    private final String name;
    private final SetLike<String> include;
    private String publishPattern;
    private boolean published;
    // Computed by SchemaMap after declarations are merged. Keys are logical platform
    // names, not generated config names; inference must not change sibling platforms.
    private final Set<String> inferredPublishedPlatforms = new HashSet<>();
    private boolean test;
    private boolean force;
    // Module descriptors are shared across platforms. Keep an explicit default
    // separate from qualified overrides so an Android main does not change JVM main.
    private static final class BuildKindDeclaration {
        private final BuildKind kind;
        private final int projectDepth;

        private BuildKindDeclaration(final BuildKind kind, final int projectDepth) {
            this.kind = kind;
            this.projectDepth = projectDepth;
        }
    }

    private BuildKindDeclaration defaultBuildKind;
    private final Map<String, BuildKindDeclaration> platformBuildKinds = new LinkedHashMap<>();

    public SchemaModule(String name, SetLike<String> include, boolean published, boolean test, final boolean force) {
        this(name, "main".equals(name) ? "$name-$platform" : "$module-$name-$platform", include, published, test, force);
    }

    public SchemaModule(String name, String publishPattern, SetLike<String> include, boolean published, boolean test, final boolean force) {
        this.name = name;
        this.publishPattern = publishPattern;
        this.include = include;
        this.published = published;
        this.test = test;
        this.force = force;
    }

    @Override
    public String getName() {
        return name;
    }

    public SetLike<String> getInclude() {
        return include;
    }

    /** Declared publication only; platform-aware consumers must use isPublished(platform). */
    public boolean isPublished() {
        return published;
    }

    public boolean isPublished(final String platform) {
        return (published || inferredPublishedPlatforms.contains(platform))
                && getBuildKind(platform) == BuildKind.JAVA_LIBRARY;
    }

    void inferPublished(final String platform) {
        inferredPublishedPlatforms.add(platform);
    }

    /** Qualified choices outrank defaults; a child default does not erase inherited overrides. */
    public BuildKind getBuildKind(final String platform) {
        final BuildKindDeclaration override = platformBuildKinds.get(platform);
        return override != null ? override.kind : defaultBuildKind == null ? BuildKind.JAVA_LIBRARY : defaultBuildKind.kind;
    }

    /** A null platform declares a default; a named platform affects only that pair. */
    public SchemaModule updateBuildKind(final String platform, final BuildKind kind) {
        return updateBuildKind(platform, kind, 0);
    }

    /**
     * Retains declaration specificity when parent modules are inserted after a
     * child's schema has loaded. Equal-depth declarations retain later-wins order.
     */
    public SchemaModule updateBuildKind(final String platform, final BuildKind kind, final int projectDepth) {
        final BuildKindDeclaration declaration = new BuildKindDeclaration(kind, projectDepth);
        if (platform == null) {
            defaultBuildKind = selectDeclaration(defaultBuildKind, declaration);
        } else {
            platformBuildKinds.put(platform, selectDeclaration(platformBuildKinds.get(platform), declaration));
        }
        return this;
    }

    private static BuildKindDeclaration selectDeclaration(final BuildKindDeclaration existing, final BuildKindDeclaration incoming) {
        return incoming == null || existing != null && existing.projectDepth > incoming.projectDepth ? existing : incoming;
    }

    public boolean isTest() {
        return test;
    }

    public boolean isForce() {
        return force;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SchemaModule))
            return false;

        final SchemaModule that = (SchemaModule) o;

        return getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    public String getPublishPattern() {
        return publishPattern;
    }

    public SchemaModule updatePublished(boolean published) {
        this.published = published;
        return this;
    }

    public SchemaModule updatePublishPattern(String publishPattern) {
        this.publishPattern = publishPattern;
        return this;
    }

    public SchemaModule updateTest(boolean test) {
        this.test = test;
        return this;
    }

    public SchemaModule updateForce(boolean force) {
        this.force = force;
        return this;
    }

    public SchemaModule update(SchemaModule module) {
        final SchemaModule merged = new SchemaModule(name, module.publishPattern, include.addNow(module.include), published || module.published, test || module.test, force || module.force);
        // Preserve inferred state if a resolved descriptor is subsequently merged,
        // without sharing mutable sets with either input.
        merged.inferredPublishedPlatforms.addAll(inferredPublishedPlatforms);
        merged.inferredPublishedPlatforms.addAll(module.inferredPublishedPlatforms);
        merged.defaultBuildKind = selectDeclaration(defaultBuildKind, module.defaultBuildKind);
        merged.platformBuildKinds.putAll(platformBuildKinds);
        module.platformBuildKinds.forEach((platform, declaration) ->
                merged.platformBuildKinds.put(platform, selectDeclaration(merged.platformBuildKinds.get(platform), declaration)));
        return merged;
    }

    @Override
    public String toString() {
        if (Debuggable.debugEnabled()) {
            return "SchemaModule{" +
                    "name='" + name + '\'' +
                    ", publishedPattern='" + publishPattern + '\'' +
                    ", require=" + include +
                    ", published=" + published +
                    ", test=" + test +
                    '}';
        }
        return "@" + name;
    }

}

