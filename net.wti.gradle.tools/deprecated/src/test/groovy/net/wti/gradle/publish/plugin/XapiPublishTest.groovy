package net.wti.gradle.publish.plugin

import net.wti.gradle.test.AbstractMultiBuildTest
import net.wti.gradle.test.api.IncludedTestBuild
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 1/14/19 @ 4:51 PM.
 */
class XapiPublishTest extends AbstractMultiBuildTest<XapiPublishTest> {

    final String BUILD_HEADER = """
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
    gwt {
      replace 'main'
      published = true
    }
  }
  archives { 
    main
    api
    maybeCreate('test').require main
    maybeCreate('test').published = true
  }
}
"""

    def setup() {
        this.version = "1.0"
        this.group = "com.consumer"
        withComposite('comp', {
            propertiesFile << """
xapi.home=$topDir
xapiGroupId=com.producer
xapiVersion=1.0
"""
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
            propertiesFile << """
xapi.home=$topDir
xapiVersion=1.1
xapiGroupId=com.consumer
xapiIndexFilter=false
"""
            buildFile << """
$BUILD_HEADER
group = 'com.consumer'
version = '1.1'
xapiRequire {
  external 'com.producer:comp:1.0', 'main'
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

    def "compile dependencies are correct when composite publishing is invoked with client source"() {
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

    def "compile dependencies are correct when non-composite publishing is invoked with client source"() {
        given:
        withConsumerSource()
        BuildResult result
        when: 'Publish the producer project'
        result = runSucceed(LogLevel.INFO, folder('comp'),'xapiPublish')
        then: 'Only the producer project is published'
        result.task(':compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':xapiPublish').outcome == TaskOutcome.SUCCESS
        result.task(':consumer:compileJava') == null

        when: 'Publish the consumer project'
        result = runSucceed('xapiPublish', DISABLE_COMPOSITE)

        then: "The consumer compiles and publishes correctly against the producer's published sources"
        result.task(':comp:compileJava') == null
        result.task(':comp:xapiPublish') == null
        result.task(':consumer:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':consumer:xapiPublish').outcome == TaskOutcome.SUCCESS

        cleanup:
        System.err.flush()
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

    def "published source jars can be consumed by test classpaths"() {
        given:
        IncludedTestBuild comp = getComposite("comp")
        comp.addSource('gwtTest', 'test.gwt', 'TestGwtClass', 'package test.gwt; public class TestGwtClass {}')
        addSource('gwtTest', 'test.gwt', 'TestGwtDepends', 'package test.gwt; public class TestGwtDepends extends TestGwtClass{}')
        buildFile.text += 'xapiRequire.module("gwt", "test").external "com.producer:comp:1.0", "gwt", "test"'

        when:
        def result = runSucceed(LogLevel.INFO, folder('comp'),'xapiPublish')

        then: 'The source and main jars published'
        result.task(':compileGwtTestJava').outcome == TaskOutcome.SUCCESS
        result.task(':xapiPublish').outcome == TaskOutcome.SUCCESS

        when:
        result = runSucceed(LogLevel.INFO, comp.rootDir, 'compileGwtTestJava')

        then:
        result.task(':compileGwtTestJava').outcome == TaskOutcome.UP_TO_DATE
        // TODO: verify that source jars or on test classpath

    }

    def "main and api archives are published with valid poms"() {

    }

    @Override
    XapiPublishTest selfSpec() {
        return this
    }
}
