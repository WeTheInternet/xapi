package net.wti.gradle.test

import net.wti.gradle.api.MinimalProjectView
import org.gradle.api.Describable
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.reflect.ObjectInstantiationException
import org.gradle.internal.extensibility.DefaultConvention
import org.gradle.internal.instantiation.InstanceGenerator
import org.gradle.internal.reflect.DirectInstantiator;

/**
 * TestProjectView:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 09/06/2024 @ 12:37 a.m.
 */
class TestProjectView implements MinimalProjectView {

    private final MinimalProjectView sourceView
    final File projectDir
    ExtensionContainer convention

    TestProjectView(MinimalProjectView source, String path) {
        this.sourceView = source
        assert ! (source instanceof TestProjectView) : "TestProjectView needs a real implementor"
        if (path.startsWith(":")) {
            path = path.substring(1)
        }
        this.projectDir = new File(source.getProjectDir(), path.replaceAll(":", File.separator))
        this.convention = new DefaultConvention(new InstanceGenerator() {
            @Override
            <T> T newInstanceWithDisplayName(final Class<? extends T> type, final Describable displayName, final Object... parameters) throws ObjectInstantiationException {
                return DirectInstantiator.INSTANCE.newInstance(type, parameters)
            }

            @Override
            <T> T newInstance(final Class<? extends T> type, final Object... parameters) throws ObjectInstantiationException {
                return DirectInstantiator.INSTANCE.newInstance(type, parameters)
            }
        })
    }


    @Override
    File getProjectDir() {
        return projectDir
    }

    @Override
    Object findProperty(final String key) {
        return System.getProperty(key, null)
    }

    @Override
    MinimalProjectView findView(final String name) {
        return sourceView.findView(name)
    }

    @Override
    String getBuildName() {
        return sourceView.getBuildName()
    }

    @Override
    String getGroup() {
        return sourceView.getGroup()
    }

    @Override
    String getVersion() {
        return sourceView.getVersion()
    }

    @Override
    ExtensionContainer getExtensions() {
        return extensions
    }

    @Override
    Settings getSettings() {
        return sourceView.getSettings()
    }
}
