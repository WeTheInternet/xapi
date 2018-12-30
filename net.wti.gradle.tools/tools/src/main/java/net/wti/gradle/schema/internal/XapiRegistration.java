package net.wti.gradle.schema.internal;

import net.wti.gradle.system.tools.GradleCoerce;

import java.util.Objects;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/29/18 @ 10:35 PM.
 */
public class XapiRegistration {

    private Object project;
    private Object platform;
    private Object archive;
    private boolean resolved;

    public XapiRegistration(Object project, Object platform, Object archive) {
        this.project = project;
        this.platform = platform;
        this.archive = archive;
    }

    public static XapiRegistration from(Object project, Object platform, Object archive) {
        return new XapiRegistration(project, platform, archive);
    }

    private void maybeResolve() {
        if (!resolved) {
            resolved = true;
            project = resolveProject(project);
            platform = resolvePlatform(platform);
            archive = resolveArchive(archive);
        }
    }

    protected String resolveProject(Object project) {
        return GradleCoerce.unwrapString(project);
    }

    protected String resolvePlatform(Object project) {
        return GradleCoerce.unwrapString(project);
    }

    protected String resolveArchive(Object project) {
        return GradleCoerce.unwrapString(project);
    }

    public String getProject() {
        maybeResolve();
        return (String) project;
    }

    public String getArchive() {
        maybeResolve();
        return (String) archive;
    }

    public String getPlatform() {
        maybeResolve();
        return (String) platform;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final XapiRegistration that = (XapiRegistration) o;

        maybeResolve();
        that.maybeResolve();

        if (!project.equals(that.project))
            return false;
        if (!Objects.equals(platform, that.platform))
            return false;
        return Objects.equals(archive, that.archive);
    }

    @Override
    public int hashCode() {
        maybeResolve();
        int result = project.hashCode();
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        result = 31 * result + (archive != null ? archive.hashCode() : 0);
        return result;
    }
}
