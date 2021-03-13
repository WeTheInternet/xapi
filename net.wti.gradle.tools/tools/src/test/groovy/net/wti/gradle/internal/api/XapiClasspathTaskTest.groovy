package net.wti.gradle.internal.api

import net.wti.gradle.classpath.tasks.XapiClasspathFileTask
import net.wti.gradle.classpath.tasks.XapiClasspathTask
import net.wti.gradle.test.AbstractMultiBuildTest
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.TaskOutcome

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 3/8/21 @ 4:51 PM.
 */
class XapiClasspathTaskTest extends AbstractMultiBuildTest<XapiClasspathTaskTest> {

    private static final String CLASSPATH_PRODUCER_GWT_API = "testProducerGwtApiClasspath"

    final String BUILD_HEADER = """
plugins {
  id 'xapi-schema'
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

        withProject(':', {
            buildFile << """
allprojects {
    repositories {
      maven {
        url 'file:///opt/xapi/repo'
      }
    }
}
"""
        })
        withProject(':producer', {
            buildFile << """
$BUILD_HEADER
group = 'com.consumer'
version = '1.0'
apply plugin: 'xapi-require'
xapiRequire.module('gwt', 'api').configure {
  external 'net.wti.gradle.tools:xapi-gradle-tools:0.5.1'
}
"""
            withSource( 'gwtApi' ) {
                'Is.java'('interface Is {}')
            }
            withSource( 'gwt' ) {
                'Comp.java'('class Comp implements Is{}')
            }
        }) // end project ':producer'

        withProject(':consumer', {
            propertiesFile << """xapi.home=$topDir"""
            buildFile << """
$BUILD_HEADER
group = 'com.consumer'
version = '1.1'
//xapiRequire.module('main').configure {
//  project ':producer', 'gwt:main'
//}
import ${XapiClasspathFileTask.getCanonicalName()}

"""
        }) // end project ':consumer'
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

    def "consumer module can create a classpath for producer project gwt api module"() {
        given:
        withProject(':consumer', {
            buildFile << """
// register some useful tasks for us to ensure basic classpath file creation is fully functional
tasks.register('$CLASSPATH_PRODUCER_GWT_API', ${XapiClasspathFileTask.name}) {
  cp ->
    cp.projectPath.set ':producer'
    cp.platform.set 'gwt'
    cp.module.set 'api'
}
"""
        })
        def result = runSucceed(LogLevel.INFO, CLASSPATH_PRODUCER_GWT_API)

        expect:
        result.task(":consumer:$CLASSPATH_PRODUCER_GWT_API").outcome == TaskOutcome.SUCCESS
        // now, verify output file contents!
    }

    def "an exposed XapiClasspathTask in one project is resolvable from another"() {
        given:
        withProject(':consumer', {
            buildFile << """
//configurations { exposedPath }
// create a useful tasks for us to ensure basic classpath file creation is fully functional
// note that we're not registering, b/c below we test a non-asynchronous reference to the created configuration
tasks.create('$CLASSPATH_PRODUCER_GWT_API', ${XapiClasspathTask.name}) {
  cp ->
    cp.projectPath.set ':producer'
    cp.platform.set 'gwt'
    cp.module.set 'api'
    cp.exposeConfiguration.set 'exposedPath'
    // our requested dependency will be glued into a configuration named "exposedPath"
    cp.expose()
}
"""
        })
        withProject(':other-consumer', {
            buildFile << """
plugins {
  id 'java'
}
// we need this ugly evaluationDependsOn, b/c our :consumer project is exposing archives created by :producer,
// and we are testing raw, synchronous access to the generated configurations...
// once we get xapi-classpath plugin smart enough to pre-create things, we can likely remove this need
evaluationDependsOn(':producer')
version = '1.2'
dependencies {
    compile project(path: ':consumer', configuration: 'exposedPath')
}
tasks.compileJava.doFirst {
  println ""
  println "CLASSPATH:"
  println tasks.compileJava.classpath.files
}
"""
            withSource("main") {
                'SomeClass.java'("""class SomeClass implements Is {} // need a class for javac to resolve api configuration""")
            }
        })
        def result = runSucceed(LogLevel.INFO, ":other-consumer:compileJava")

        expect:
        result.task(":other-consumer:compileJava").outcome == TaskOutcome.SUCCESS
        // now, verify output file contents!

        // rerun, this time

    }

    @Override
    XapiClasspathTaskTest selfSpec() {
        return this
    }

    void cleanup() {
        println "View test resources: file://$buildFile.parent"
    }
}
