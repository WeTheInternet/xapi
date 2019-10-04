package net.wti.loader.plugin

import net.wti.gradle.test.AbstractMultiBuildTest
import org.gradle.testkit.runner.BuildResult

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 30/07/19 @ 4:41 AM.
 */
class XapiLoaderPluginTest extends AbstractMultiBuildTest<XapiLoaderPluginTest> {

    @Override
    XapiLoaderPluginTest selfSpec() {
        return this
    }

    def "The loader plugin will create all gradle projects in schema.xapi"() {
        given:
        getProject('p1').buildFile << '''
'''
        getProject('p2').buildFile << '''
xapiRequire.project 'p2'
'''

        when:
        BuildResult res = runSucceed(':p2:xapiReport', ':p2:compileJava', '-Pxapi.debug=true')
        then:
        res.task(':p1:compileGwtJava').outcome == org.gradle.testkit.runner.TaskOutcome.SUCCESS
        res.task(':p1:compileJava').outcome == TaskOutcome.SUCCESS
        res.output.contains "$rootDir/p1/src/main/java"
        res.output.contains "$rootDir/p2/src/main/java"

    }
}
