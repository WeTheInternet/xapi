package net.wti.gradle.test

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.util.ConfigureUtil
import spock.lang.Specification

/**
 * A groovy trait suitable for use in tests that use multi-project builds.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/17/18 @ 4:54 AM.
 */
trait MultiProjectTestMixin <S extends Specification & MultiProjectTestMixin<S>> extends XapiGradleTestMixin<S> {

    private final LinkedHashMap<Object, Action<? super TestProject>> pendingProjects = [:]
    private final LinkedHashMap<String, TestProject> realizedProjects = [:]

    S withProject(Object name, @DelegatesTo(value = TestProject.class, strategy = Closure.OWNER_FIRST) Closure<?> a) {
        return withProject(name, ConfigureUtil.configureUsing(a))
    }
    S withProject(Object name, Action<? super TestProject> a) {
        insert(pendingProjects, name, a)
        selfSpec()
    }

    TestProject getProject(String named) {
        return realize().get(named)
    }

    @SuppressWarnings("GrUnnecessaryPublicModifier") // it is necessary w/ type parameters
    public <K, V> Action<? super V> insert(Map<K, Action<? super V>> into, K name, Action<? super V> a) {
        into.compute(name, {key, val->
            val ? { p -> val.execute(p); a.execute(p) } as Action<? super V> : a
        })
    }

    @Override
    String getDefaultRootProjectName() {
        return realize().get(':')?.getProjectName() ?: super.getDefaultRootProjectName()
    }

    @Override
    void flush() {
        super.flush()
        realize().each {
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

    @CompileStatic
    private LinkedHashMap<String, TestProject> realize() {
        while (!pendingProjects.isEmpty()) {
            LinkedHashMap<Object, Action<? super TestProject>> copy = new LinkedHashMap<>(pendingProjects)
            pendingProjects.clear()
            copy.each {
                k, v ->
                    String key = coerceString(k)
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
}
