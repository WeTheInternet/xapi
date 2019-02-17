package net.wti.gradle.internal.api

import net.wti.gradle.test.AbstractMultiBuildTest
import net.wti.gradle.test.api.IncludedTestBuild
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.TaskOutcome

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/14/19 @ 4:51 PM.
 */
class XapiPublishTest extends AbstractMultiBuildTest<XapiPublishTest> {

    static final String BUILD_HEADER = """
plugins {
  id 'xapi-schema'
  id 'xapi-require'
  id 'maven-publish'
  id 'xapi-publish'
}
// as simple as it gets while still being useful.
xapiSchema {
  platforms {
    main
  }
  archives { 
    main
    api
  }
}
"""

    def setup() {
        withComposite('comp', {
            propertiesFile << """xapi.home=$topDir"""
            withProject(':', {
                buildFile << """
$BUILD_HEADER
group = 'com.producer'
version = '1.0'
"""
                withSource( 'api' ) {
                    'Is.java'('interface Is {}')
                }
                withSource( 'main' ) {
                    'Comp.java'('class Comp implements Is{}')
                }
            }) // end project ':'

        }) // end composite 'comp'
        withProject(':consumer', {
            propertiesFile << """xapi.home=$topDir"""
            buildFile << """
$BUILD_HEADER
group = 'com.consumer'
version = '1.1'
xapiRequire {
  external 'com.producer:comp:1.0'
}

"""
        })
    }

    protected def withConsumerSource() {
        withProject(':consumer') {
            withSource('api') {
                'Is2.java'('interface Is2 extends Is {}')
            }
            withSource('main') {
                'Comp2.java'('class Comp2 extends Comp implements Is2 {}')
            }
        }
    }

    def "composite build assembles and publishes all tasks, even if consumer has no source"() {
        given:
        def result = runSucceed(LogLevel.INFO,'xapiPublish')

        expect:
        result.task(':comp:jar').outcome == TaskOutcome.SUCCESS
        result.task(':comp:apiJar').outcome == TaskOutcome.SUCCESS
    }

    def "composite builds handle api mapping when consumer has source"() {
        given:
        withConsumerSource()
        def result = runSucceed(LogLevel.INFO,'xapiPublish')

        expect:
        result.task(':comp:jar').outcome == TaskOutcome.SUCCESS
        result.task(':comp:apiJar').outcome == TaskOutcome.SUCCESS
        result.task(':consumer:jar').outcome == TaskOutcome.SUCCESS
        result.task(':consumer:apiJar').outcome == TaskOutcome.SUCCESS
    }

    def "compile dependencies are correct when publishing is invoked without client source"() {
        given:
        def result = runSucceed('xapiPublish')

        expect:
        result.task(':comp:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':compileJava') == null // there was no source, so we should not have compiled.
        result.task(':consumer:xapiPublish').outcome == TaskOutcome.SUCCESS
        result.task(':comp:xapiPublish').outcome == TaskOutcome.SUCCESS
    }

    def "compile dependencies are correct when publishing is invoked with client source"() {
        given:
        withConsumerSource()
        def result = runSucceed('xapiPublish')

        expect:
        result.task(':comp:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':consumer:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':consumer:xapiPublish').outcome == TaskOutcome.SUCCESS
        result.task(':comp:xapiPublish').outcome == TaskOutcome.SUCCESS

        cleanup:
        println "Test directory: file://$rootDir"
    }

    def "compile dependencies are correct when publishing is not invoked"() {
        given:
        IncludedTestBuild comp = getComposite("comp")
        def result = runSucceed(LogLevel.INFO, comp.rootDir, 'compileJava')

        expect:
        result.task(':compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':xapiPublish') == null

    }

    def "main and api archives are published with valid poms"() {

    }

    @Override
    XapiPublishTest selfSpec() {
        return this
    }
}
