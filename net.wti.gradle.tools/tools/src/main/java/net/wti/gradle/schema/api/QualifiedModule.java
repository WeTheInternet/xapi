package net.wti.gradle.schema.api;

import net.wti.gradle.require.api.PlatformModule;
import org.gradle.api.NonNullApi;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 1:44 a.m..
 */
@NonNullApi
public class QualifiedModule extends PlatformModule {

    public static final String UNKNOWN_VALUE = "UNKNOWN";

    private final String build;
    private final String project;

    public QualifiedModule(String build, String project, String platform, String module) {
        super(platform, module);
        this.build = build;
        this.project = project;
    }

    public static String mangleProjectPath(String path) {
        if (!path.startsWith(":")) {
            path = ":" + path;
        }
        path = path.replace(':', '_');
        return path;
    }

    public String getBuild() {
        return build;
    }

    public String getProject() {
        return project;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof QualifiedModule))
            return false;
        if (!super.equals(o))
            return false;

        final QualifiedModule that = (QualifiedModule) o;

        if (!getBuild().equals(that.getBuild()))
            return false;
        return getProject().equals(that.getProject());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getBuild().hashCode();
        result = 31 * result + getProject().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return build + ':' + project + ':' + getPlatform() + ':' + getModule();
    }
}
