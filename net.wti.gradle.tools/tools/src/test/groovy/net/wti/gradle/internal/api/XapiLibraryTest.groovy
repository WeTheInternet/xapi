package net.wti.gradle.internal.api

import net.wti.gradle.test.MultiProjectTestMixin
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/17/18 @ 4:42 AM.
 */
class XapiLibraryTest extends Specification implements MultiProjectTestMixin<XapiLibraryTest> {

    private String setupSimpleGwt(String name="gwtP") {
        if (buildFile.length() == 0) {
            buildFile << simpleSchema()
        }

        withProject(name, {
            buildFile << """
plugins { 
    id 'java'
    id 'xapi-schema'
}
"""
            addSource("com.foo.$name", 'Main', """
package com.foo.$name;

public class Main {
  public static void main(String ... a) {
    System.out.println("Hi!");
  }
}
""")
            addSource('gwt', "com.gwt.$name", 'GwtMain', """
package com.gwt.$name;

class GwtMain {
  public static void main(String ... a) {
    com.foo.${name}.Main.main(a);
  }
}
""")
        })
        return name
    }

    def "Intra-build dependencies correctly inherit their own platform+archive dependencies"() {
        given:
        setupSimpleGwt('gwt1')
        setupSimpleGwt('gwt2')
        getProject('gwt2').buildFile << '''
xapiRequire.project 'gwt1'
'''

        when:
        BuildResult res = runSucceed(LogLevel.INFO, ':gwt2:compileGwtJava', 'xapiReport', '-Pxapi.debug=true')
        then:
        res.task(':gwt1:compileGwtJava').outcome == TaskOutcome.SUCCESS
        res.task(':gwt1:compileJava').outcome == TaskOutcome.SUCCESS
        res.output.contains "$rootDir/gwt1/src/main/java"
        res.output.contains "$rootDir/gwt2/src/main/java"

    }

    def "Gwt platform automatically inherits own project sources"() {
        given:
        String proj = setupSimpleGwt()

        when:
        // We use -Pxapi.debug=true to get results printed to stdOut.  We could / should also check the report file.
        BuildResult res = runSucceed(LogLevel.INFO, 'compileGwtJava', 'xapiReport', '-Pxapi.debug=true')
        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS
        res.output.contains "$rootDir/$proj/src/main/java"
    }

    def "Plugin is compatible with 'java' plugin"() {
        given:
        String proj = setupSimpleGwt()
        getProject(proj).buildFile << """
apply plugin: 'java'
"""
        when:
        BuildResult res = runSucceed(LogLevel.INFO, 'compileGwtJava')
        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS
    }

    def "Plugin is compatible with 'java-library' plugin"() {
        given:
        String proj = setupSimpleGwt()
        getProject(proj).buildFile << """
apply plugin: 'java-library'
"""
        when:
        BuildResult res = runSucceed(LogLevel.INFO, 'compileGwtJava')
        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS
    }

    String simpleSchema() {
        '''
plugins {
  id 'xapi-schema'
}
xapiSchema {
  platforms {
    main
    gwt {
      replace main
    }
  }
  archives {
    main
    api
  }
}
'''
    }

    String schemaWithArchives() {
        '''
plugins {
  id 'xapi-schema'
}
xapiSchema {
  platforms {
    main
    gwt {
      replace main
    }
  }
  archives {
    main
  }
}
'''
    }

    def declareAttributes() {
        """
            def usage = Attribute.of('usage', String)
            def artifactType = Attribute.of('artifactType', String)
            def platformType = Attribute.of('platformType', String)
                
            allprojects {
                dependencies {
                    attributesSchema {
                        attribute(usage)
                    }
                }
                configurations {
                    compile {
                        attributes.attribute usage, 'api'
                    }
                }
            }
        """
    }

    @Override
    XapiLibraryTest selfSpec() {
        return this
    }
}
