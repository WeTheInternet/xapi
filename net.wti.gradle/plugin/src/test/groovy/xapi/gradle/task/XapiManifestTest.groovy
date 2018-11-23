package xapi.gradle.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GFileUtils
import org.gradle.util.GUtil
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/22/18 @ 1:44 AM.
 */
class XapiManifestTest extends Specification {

    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
    }

    def "Manifest should capture basic gradle standard directories"() {
        given:
        buildFile << """
            plugins {
                id 'xapi-base'
            }
        """

        when:
        def result = exec('xapiManifest')
        then:
        result.task(":xapiManifest").outcome == SUCCESS

        when:
        File manifest = builtManifest
        then:
        manifest.text ==
"""<xapi sources = [
  "$root/src/main/java"
]
resources = [
  "$root/src/main/resources"
]
outputs = [
  "$root/build/classes/java/main"
  ,  "$root/build/resources/main"
]
/>"""
    }

    void touch(String s) {
        File f = new File(testProjectDir.root, s)
        f.parentFile.mkdirs()
        GFileUtils.touch(f)
    }

    File getBuiltManifest() {
        new File(testProjectDir.root, 'build/xapi-paths/META-INF/xapi/settings.xapi')
    }

    BuildResult exec(String ... s) {
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .withDebug(true)
                .withArguments(s)
                .build()
    }

    private File getRoot() {
        return testProjectDir.root
    }
}
