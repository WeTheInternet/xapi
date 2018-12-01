package xapi.gradle.task

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GFileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/22/18 @ 1:44 AM.
 */
class XapiManifestTest extends Specification implements XapiTestMixin {

    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        buildFile << """
            plugins {
                id 'xapi-base'
            }
        """
    }

    def "Manifest should capture basic gradle standard directories"() {

        when: 'Manifest built with no existing source directories'
        def result = exec('xapiManifest')
        File manifest = builtManifest

        then: 'The manifest built, and is effectively empty'
        result.task(":xapiManifest").outcome == SUCCESS
        manifest.text ==
"""<xapi
module="$root.name"
type="main"
sources=[]

resources=[]

outputs=[]

generated=[]
/>"""

        // this is setup for the next case, but we do it here, in a then: block, for free assertion
        new File(root, 'src/main/resources').mkdirs()
        new File(root, 'src/main/resources/file').createNewFile()
        when: 'Manifest rebuilt when resources are added'
        result = exec('xapiManifest')
        manifest = builtManifest

        then: 'The manifest built, and only has resource input directories'
        result.task(":xapiManifest").outcome == SUCCESS
        manifest.text ==
"""<xapi
module="$root.name"
type="main"
sources=[]

resources=["$root/src/main/resources"]

outputs=[]

generated=[]
/>"""

        when: 'Running processResources adds the output directory'
        result = exec('processResources', '-i')
        manifest = builtManifest
        then: 'The manifest built, and has resource input and output'
        result.task(":xapiManifest").outcome == SUCCESS
        manifest.text ==
"""<xapi
module="$root.name"
type="main"
sources=[]

resources=["$root/src/main/resources"]

outputs=["$root/build/resources/main"]

generated=[]
/>"""
    }

    // TODO: test xapi { sources { api { inherits('xapi-fu') } } }

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
}
