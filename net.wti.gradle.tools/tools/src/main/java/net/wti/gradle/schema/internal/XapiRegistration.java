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
    private Object into;
    private boolean resolved;

    public XapiRegistration(Object project, Object platform, Object archive) {
        this(project, platform, archive, null);
    }

    public XapiRegistration(Object project, Object platform, Object archive, Object into) {
        this.project = project;
        this.platform = platform;
        this.archive = archive;
        this.into = into;
    }

    public static XapiRegistration from(Object project, Object platform, Object archive) {
        return new XapiRegistration(project, platform, archive);
    }

    public static XapiRegistration from(Object project, Object platform, Object archive, Object into) {
        return new XapiRegistration(project, platform, archive, into);
    }

    private void maybeResolve() {
        if (!resolved) {
            resolved = true;
            project = resolveProject(project);
            platform = resolvePlatform(platform);
            archive = resolveArchive(archive);
            into = resolveInto(into);
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

    protected String resolveInto(Object into) {
        return GradleCoerce.unwrapString(into);
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

    public Object getInto() {
        maybeResolve();
        return into;
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
        if (!Objects.equals(archive, that.archive))
            return false;
        return Objects.equals(into, that.into);
    }

    @Override
    public int hashCode() {
        maybeResolve();
        int result = project.hashCode();
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        result = 31 * result + (archive != null ? archive.hashCode() : 0);
        result = 31 * result + (into != null ? into.hashCode() : 0);
        return result;
    }
}
