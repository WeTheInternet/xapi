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
            propertiesFile << """xapi.home=${System.getProperty("xapi.home")}"""
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
            })
        })
        withProject(':consumer', {
            propertiesFile << """xapi.home=${System.getProperty("xapi.home")}"""
            buildFile << """
$BUILD_HEADER
group = 'com.consumer'
version = '1.1'
xapiRequire {
  external 'com.producer:comp:1.0'
}

PublishingExtension publishing = extensions.getByType(PublishingExtension.class);
publishing.repositories( {repos -> 
        repos.maven( { maven ->
                maven.setName("xapiLocal");
                maven.url = '$xapiRepo'
                maven.metadataSources( { gradleMetadata() });
            }
        );
    }
);

"""
        })
    }

    protected def withConsumerSource() {
        withProject('consumer') {
            withSource('api') {
                'interface Is2 extends Is {}'
            }
            withSource('main') {
                'interface Comp2 extends Comp implements Is2 {}'
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

    def "compile dependencies are correct when publishing is invoked"() {
        given:
        IncludedTestBuild comp = getComposite("comp")
        def result = runSucceed(LogLevel.INFO, comp.rootDir, 'xapiPublish')

        expect:
        result.task(':compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':xapiPublish').outcome == TaskOutcome.SUCCESS
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
