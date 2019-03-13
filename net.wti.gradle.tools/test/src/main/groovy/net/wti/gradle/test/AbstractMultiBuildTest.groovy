package net.wti.gradle.test

import net.wti.gradle.system.tools.GradleCoerce
import net.wti.gradle.test.api.IncludedTestBuild
import net.wti.gradle.test.api.TestBuild
import net.wti.gradle.test.api.TestBuildDir
import net.wti.gradle.test.api.TestProject
import org.gradle.api.Action
import org.gradle.util.ConfigureUtil

import static groovy.lang.Closure.OWNER_FIRST
/**
 * A groovy trait suitable for use in tests that use multi-project builds.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 12/17/18 @ 4:54 AM.
 */
abstract class AbstractMultiBuildTest<S extends AbstractMultiBuildTest<S>> extends AbstractMultiProjectTest<S> implements TestBuild, TestBuildDir {


    private final LinkedHashMap<Object, Action<? super IncludedTestBuild>> pendingBuilds = [:]
    private final LinkedHashMap<String, IncludedTestBuild> realizedBuilds = [:]


    S withComposite(Object name, @DelegatesTo(value = IncludedTestBuild.class, strategy = OWNER_FIRST) Closure<?> a) {
        return withComposite(name, ConfigureUtil.configureUsing(a))
    }

    S withComposite(Object name, Action<? super IncludedTestBuild> a) {
        insert(pendingBuilds, name, a)
        selfSpec()
    }

    IncludedTestBuild getComposite(String name) {
        realizeBuilds()
        return realizedBuilds.computeIfAbsent(name, {
            throw new IllegalArgumentException("No such build $name")
        })
    }

    TestProject getProject(String named) {
        return realizeProjects().get(named)
    }

    @Override
    String getDefaultRootProjectName() {
        return realizeProjects().get(':')?.getProjectName() ?: super.getDefaultRootProjectName()
    }

    @Override
    Boolean hasWorkRemaining() {
        return !pendingBuilds.isEmpty() || super.hasWorkRemaining()
    }

    @Override
    void doWork() {
        realizeBuilds()
        super.doWork()
        realizeBuilds().each {
            name, build ->
                String path = build.name
                settingsFile << """
if (System.getProperty('${SKIP_COMPOSITE_SYS_PROP}') != 'true' && System.getProperty('${SKIP_COMPOSITE_SYS_PROP}.recurse') != 'true') {
  // prevent included builds from trying to do any inclusions themselves.
  System.setProperty('${SKIP_COMPOSITE_SYS_PROP}.recurse', 'true')
  
  includeBuild('$path')

  gradle.buildFinished {
    System.clearProperty('$SKIP_COMPOSITE_SYS_PROP')
    System.clearProperty('${SKIP_COMPOSITE_SYS_PROP}.recurse')
  }
}
"""
        }
    }

    LinkedHashMap<String, IncludedTestBuild> realizeBuilds() {
        while (!pendingBuilds.isEmpty()) {
            LinkedHashMap<Object, Action<? super IncludedTestBuild>> copy = new LinkedHashMap<>(pendingBuilds)
            this.@pendingBuilds.clear()
            LinkedHashMap<String, IncludedTestBuild> realized = realizedBuilds
            copy.each {
                k, v ->
                    String key = GradleCoerce.unwrapString(k)
                    TestBuild build = realized.computeIfAbsent(key, {new IncludedTestBuild(this, key)})
                    v.execute(build)
                    build.flush()
            }
        }

        return realizedBuilds
    }

    File getTopDir() {
        rootDir
    }

    File getXapiRepo() {
        new File(topDir, 'repo')
    }
}
