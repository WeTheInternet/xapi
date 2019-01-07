package net.wti.gradle.internal.api

import net.wti.gradle.schema.plugin.XapiSchemaPlugin
import net.wti.gradle.test.MultiProjectTestMixin
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Specification
/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/17/18 @ 4:42 AM.
 */
class XapiLibraryTest extends Specification implements MultiProjectTestMixin<XapiLibraryTest> {

    protected String getDefaultPlugin() {
        return "xapi-schema"
    }

    private String setupSimpleGwt(String name="gwtP", String plugin='') {
        if (buildFile.length() == 0) {
            buildFile << simpleSchema()
        }

        withProject(name, {
            def use = plugin ?: defaultPlugin
            buildFile << """
plugins {
    id 'java'
    ${use.startsWith('id') ? use : "id '$use'"}
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
        setupSimpleGwt('gwt2', "xapi-require")
        getProject('gwt2').buildFile << '''
xapiRequire.project 'gwt1'
'''

        when:
        BuildResult res = runSucceed(':gwt2:xapiReport', ':gwt2:compileGwtJava', '-Pxapi.debug=true')
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
        BuildResult res = runSucceed('compileGwtJava', ":$proj:xapiReport", '-Pxapi.debug=true')
        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS
        res.output.contains "$rootDir/$proj/src/main/java"
    }

    def "Plugin is compatible with java plugin"() {
        given:
        String proj = setupSimpleGwt()

        when : "Set gradle property to tell xapi to apply java plugin"
        String was = getProject(proj).propertiesFile.text
        getProject(proj).propertiesFile << "$XapiSchemaPlugin.PROP_SCHEMA_APPLIES_JAVA=true"
        BuildResult res = runSucceed('compileGwtJava')

        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS

        when: "Explicitly apply java plugin and re-run"
        getProject(proj).buildFile << """
apply plugin: 'java'"""
        res = runSucceed('compileGwtJava')
        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.UP_TO_DATE
        res.task(":$proj:compileJava").outcome == TaskOutcome.UP_TO_DATE

        when: "Remove the gradle.properties changes and run again"
        getProject(proj).propertiesFile.text = was
        res = runSucceed('compileGwtJava')
        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.UP_TO_DATE
        res.task(":$proj:compileJava").outcome == TaskOutcome.UP_TO_DATE
    }

    def "Plugin is compatible with java-library plugin"() {
        given:
        String proj = setupSimpleGwt()

        when : "Set gradle property to tell xapi to apply java library plugin"
        String was = getProject(proj).propertiesFile.text
        getProject(proj).propertiesFile << "$XapiSchemaPlugin.PROP_SCHEMA_APPLIES_JAVA_LIBRARY=true"
        BuildResult res = runSucceed('compileGwtJava')

        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS

        when: "Explicitly apply java-library plugin and re-run"
        getProject(proj).buildFile << """
apply plugin: 'java-library'"""
        res = runSucceed('compileGwtJava')
        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.UP_TO_DATE
        res.task(":$proj:compileJava").outcome == TaskOutcome.UP_TO_DATE

        when: "Remove the gradle.properties changes and run again"
        getProject(proj).propertiesFile.text = was
        res = runSucceed('compileGwtJava')
        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.UP_TO_DATE
        res.task(":$proj:compileJava").outcome == TaskOutcome.UP_TO_DATE
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
