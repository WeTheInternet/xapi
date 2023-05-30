package net.wti.gradle.test.api

import net.wti.gradle.system.tools.GradleCoerce
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.util.ConfigureUtil

import static groovy.lang.Closure.OWNER_FIRST
/**
 * A test-level abstraction for "a directory with settings.gradle and
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 1/12/19 @ 10:49 PM.
 */
trait TestBuild implements Named, Flushable, HasBuildFiles {

    private String name

    private final LinkedHashMap<Object, Action<? super TestProject>> pendingProjects = [:]
    private final LinkedHashMap<String, TestProject> realizedProjects = [:]

    void withProject(Object name, @DelegatesTo(value = TestProject.class, strategy = OWNER_FIRST) Closure<?> a) {
        withProject(name, ConfigureUtil.configureUsing(a))
    }

    void withProject(Object name, Action<? super TestProject> a) {
        insert(pendingProjects, name, a)
    }

    void initSettings(File settings) {
        settings.text += """
// from ${getClass().simpleName} ${name ? "($name)" : ''}
if (System.getProperty('${HasBuildFiles.SKIP_METADATA_SYS_PROP}') != 'true') {
    //enableFeaturePreview('GRADLE_METADATA')
}
"""
    }

    @Override
    String getName() {
        return name ?: ':'
    }

    void setName(Object name) {
        this.name = GradleCoerce.unwrapString(name)
    }

    Boolean hasWorkRemaining() {
        return !pendingProjects.isEmpty()
    }

    @Override
    void doWork() {
        realizeProjects()
    }

    LinkedHashMap<String, TestProject> realizeProjects() {
        while (!pendingProjects.isEmpty()) {
            LinkedHashMap<Object, Action<? super TestProject>> copy = new LinkedHashMap<>(pendingProjects)
            pendingProjects.clear()
            copy.each {
                k, v ->
                    String key = GradleCoerce.unwrapString(k)
                    TestProject proj
                    File dir
                    if (key == ':') {

                        dir = rootDir
                    } else {
                        key = key.startsWith(':') ? key.substring(1) : key
                        dir = folder(key.split(':'))
                    }
                    proj = realizedProjects.computeIfAbsent(key, {
                        TestProject p = new TestProject(key, dir)
                        if (p.path != ':') {
                            settingsFile << "include('$p.path')\n"
                        }
                        return p
                    })
                    v.execute(proj)
            }
        }

        return realizedProjects
    }

    @Override
    void flush() {
        super.flush()
        if (!buildFile.text) {
            buildFile.text = "plugins { id 'base' }"
        }
    }
}
