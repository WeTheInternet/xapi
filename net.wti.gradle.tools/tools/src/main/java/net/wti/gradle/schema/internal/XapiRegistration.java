package net.wti.gradle.schema.internal;

import net.wti.gradle.schema.api.Transitivity;
import net.wti.gradle.system.tools.GradleCoerce;
import org.gradle.api.Named;

import java.util.Objects;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/29/18 @ 10:35 PM.
 */
public class XapiRegistration implements Named {

    public enum RegistrationMode {
        internal, project, external;
        public static RegistrationMode DEFAULT = project;
    }

    private Object project;
    private Object platform;
    private Object archive;
    private Object from;
    private Boolean lenient;
    private boolean resolved;
    private Object transitivity;
    private final RegistrationMode mode;

    public XapiRegistration(Object project, Object platform, Object archive) {
        this(project, platform, archive, null);
    }

    public XapiRegistration(Object project, Object platform, Object archive, Object from) {
        this(project, platform, archive, from, RegistrationMode.DEFAULT);
    }
    public XapiRegistration(Object project, Object platform, Object archive, Object from, RegistrationMode mode) {
        this(project, platform, archive, from, mode, Transitivity.api);
    }
    public XapiRegistration(Object project, Object platform, Object archive, Object from, RegistrationMode mode, Transitivity trans) {
        this.project = project;
        this.platform = platform;
        this.archive = archive;
        this.from = from;
        this.mode = mode;
        this.transitivity = trans;
    }

    public static XapiRegistration from(Object project, Object platform, Object archive) {
        return new XapiRegistration(project, platform, archive);
    }

    public static XapiRegistration from(Object project, Object platform, Object archive, Object into) {
        return new XapiRegistration(project, platform, archive, into);
    }

    public static XapiRegistration from(Object project, Object platform, Object archive, Object into, RegistrationMode mode) {
        return new XapiRegistration(project, platform, archive, into, mode);
    }

    public static XapiRegistration from(Object project, Object platform, Object archive, Object into, RegistrationMode mode, Transitivity trans) {
        return new XapiRegistration(project, platform, archive, into, mode, trans);
    }

    private void maybeResolve() {
        if (!resolved) {
            resolved = true;
            project = resolveProject(project);
            platform = resolvePlatform(platform);
            archive = resolveArchive(archive);
            from = resolveInto(from);
            transitivity = resolveTransitivity(transitivity);
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

    protected Transitivity resolveTransitivity(Object into) {
        return GradleCoerce.unwrapEnum(Transitivity.class, into);
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

    public Object getFrom() {
        maybeResolve();
        return from;
    }

    public Transitivity getTransitivity() {
        maybeResolve();
        return (Transitivity) transitivity;
    }

    public XapiRegistration withTransitive(Transitivity transitivity) {
        this.transitivity = transitivity;
        return this;
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
        return project.equals(that.project)
               && Objects.equals(platform, that.platform)
               && Objects.equals(archive, that.archive)
               && Objects.equals(from, that.from);
    }

    @Override
    public int hashCode() {
        maybeResolve();
        int result = project.hashCode();
        result = 31 * result + (platform != null ? platform.hashCode() : 0);
        result = 31 * result + (archive != null ? archive.hashCode() : 0);
        result = 31 * result + (from != null ? from.hashCode() : 0);
        return result;
    }

    @Override
    public String getName() {
        maybeResolve();
        StringBuilder b = new StringBuilder();
        b
            .append(project)
            .append("/")
            .append(platform)
            .append(":")
            .append(archive);
        return b.toString();
    }

    public RegistrationMode getMode() {
        return mode;
    }

    public Boolean getLenient() {
        return lenient;
    }

    public XapiRegistration setLenient(Boolean lenient) {
        this.lenient = lenient;
        return this;
    }
}
