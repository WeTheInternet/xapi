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
trait TestBuild implements Named, HasWork, HasBuildFiles {

    private String name

    private final LinkedHashMap<Object, Action<? super TestProject>> pendingProjects = [:]
    private final LinkedHashMap<String, TestProject> realizedProjects = [:]

    void withProject(Object name, @DelegatesTo(value = TestProject.class, strategy = OWNER_FIRST) Closure<?> a) {
        withProject(name, ConfigureUtil.configureUsing(a))
    }

    void withProject(Object name, Action<? super TestProject> a) {
        insert(pendingProjects, name, a)
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

                    File dir = key == ':' ? rootDir : newFolder(key.split(':'))
                    proj = realizedProjects.computeIfAbsent(key, {
                        new TestProject(key, dir)
                    })
                    v.execute(proj)
            }
        }

        return realizedProjects
    }

    @Override
    void flush() {
        doFlush()
        realizeProjects().each {
            name, proj ->
                String path = proj.path
                if (path != ':') {
                    settingsFile << "include('$path')\n"
                }
        }
        if (!buildFile.text) {
            buildFile.text = "plugins { id 'base' }"
        }
    }
}
