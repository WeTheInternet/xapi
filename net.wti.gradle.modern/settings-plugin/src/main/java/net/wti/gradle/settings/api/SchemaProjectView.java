package net.wti.gradle.settings.api;

import net.wti.gradle.api.MinimalProjectView;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtensionContainer;

import java.io.File;

/**
 * SchemaProjectView:
 * <p>
 * <p>We need to cheat the project view system for ~virtual schema nodes.
 * <p>The only important value we need to change is the projectDir.
 * <p>Everything else delegates to the real project view
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 04/11/2024 @ 2:49 a.m.
 */
public class SchemaProjectView implements MinimalProjectView {

    private final MinimalProjectView realView;
    private final File projectDir;

    public SchemaProjectView(final MinimalProjectView realView, File projectDir) {
        this.realView = realView;
        this.projectDir = projectDir;
    }

    @Override
    public File getProjectDir() {
        return projectDir;
    }

    @Override
    public Object findProperty(final String key) {
        return realView.findProperty(key);
    }

    @Override
    public MinimalProjectView findView(final String name) {
        return realView.findView(name);
    }

    @Override
    public MinimalProjectView getRootProject() {
        return realView.getRootProject();
    }

    @Override
    public String getBuildName() {
        return realView.getBuildName();
    }

    @Override
    public String getGroup() {
        return realView.getGroup();
    }

    @Override
    public String getVersion() {
        return realView.getVersion();
    }

    @Override
    public Settings getSettings() {
        return realView.getSettings();
    }

    @Override
    public ExtensionContainer getExtensions() {
        return realView.getExtensions();
    }
}
