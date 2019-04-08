package xapi.gradle.task

import net.wti.gradle.test.AbstractMultiProjectTest
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GFileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/24/18 @ 5:36 AM.
 */
class XapiSourceSetTest extends AbstractMultiProjectTest<XapiSourceSetTest> {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    File settingsFile
    File propertiesFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        settingsFile = testProjectDir.newFile('settings.gradle')
        propertiesFile = testProjectDir.newFile('gradle.properties')
    }

    def "User can add xapiDev classpath"() {
        given:
        buildFile << """
            plugins {
                id 'java-library'
                id 'xapi'
            }
            group = 'testing'
            version = '1.0'
            dependencies.add 'xapiDev', dependencies.create('javax.validation:validation-api:1.0.0.GA')
            
        """
        // If a directory does not exist, it is not added to the manifest.
        // We may want to revisit those semantics later, but for now, it is ideal to trim as much noise as possible.
        // It is also ideal to be able to detect when _any_ sources are missing, and if so,
        // fallback to "load this from a remote repo".
        // Including sources which we know don't exist makes such a scenario ...very wasteful.
        new File(buildFile.parentFile, 'src/main/java').mkdirs()

        when:
        def result = exec('build', '--stacktrace')
        then:
        result.task(":xapiDevJar").outcome == SUCCESS

        when:
        File manifest = builtManifest
        then:
        manifest.text.contains("src/main/java")
        manifest.text ==
"""<xapi
module="$root.name"
type="main"
sources=["$root.absolutePath/src/main/java"]

resources=[]

outputs=[]

generated=[]

includes={  api: "$root.name/api",
  spi: "$root.name/spi"
}
/>"""
    }

    void touch(String s) {
        File f = new File(testProjectDir.root, s)
        f.parentFile.mkdirs()
        GFileUtils.touch(f)
    }

    File getBuiltManifest() {
        new File(testProjectDir.root, 'build/xapi-paths/META-INF/xapi/paths.xapi')
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

    @Override
    XapiSourceSetTest selfSpec() {
        return this
    }

}
