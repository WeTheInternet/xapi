package net.wti.gradle.internal.api


import net.wti.gradle.system.plugin.XapiBasePlugin
import net.wti.gradle.test.AbstractMultiBuildTest
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/17/18 @ 4:42 AM.
 */
class XapiSchemaTest extends AbstractMultiBuildTest<XapiSchemaTest> {

    protected String getDefaultPlugin() {
        return "xapi-schema"
    }

    private String setupSimpleGwt(String name="gwtP", String plugin='', String rootProject=':', String version="1.0") {
        boolean isRoot = name == rootProject

        Closure<?> configure = {
            def use = plugin ?: defaultPlugin
            buildFile << """
plugins {
    ${use.startsWith('id') ? use : "id '$use'"}
}
${simpleSchema()}
repositories {
  maven {
    name = 'xapiLocal'
    url = '$xapiRepo'
    metadataSources { gradleMetadata() }
  }
}
version = $version
group = '${rootProject == ':' ? 'testgroup' : rootProject}'
"""
            propertiesFile << "xapi.home=${System.getProperty("xapi.home")}"
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
            null
        }
        if (isRoot && ':' != rootProject) {
            withComposite(rootProject, {
                withProject(':', configure)
                null
            })
        } else {
            withProject(name, configure)
        }
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

    def "Intra-build dependencies correctly inherit transitive platform+archive dependencies"() {
        given:
        setupSimpleGwt('gwt0')
        setupSimpleGwt('gwt1', 'xapi-require')
        getProject('gwt1').buildFile << "xapiRequire.project 'gwt0'"
        setupSimpleGwt('gwt2', 'xapi-require')
        getProject('gwt2').buildFile << """
xapiRequire.project 'gwt1'
import net.wti.gradle.schema.plugin.XapiSchemaPlugin
dependencies {
    attributesSchema.attribute(XapiSchemaPlugin.ATTR_PLATFORM_TYPE)
    attributesSchema.attribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE)
}
"""
//        AttributesSchema attributesSchema; attributesSchema.attribute(XapiSchemaPlugin.ATTR_PLATFORM_TYPE)

        when:
        BuildResult res
        res = runSucceed(':gwt2:xapiReport', // ':gwt2:xapiPublish',
                ':gwt2:compileGwtJava', '-Pxapi.debug=true')
        then:
        res.task(':gwt2:compileGwtJava').outcome == TaskOutcome.SUCCESS
        res.task(':gwt2:compileJava').outcome == TaskOutcome.SUCCESS
        res.task(':gwt1:compileGwtJava').outcome == TaskOutcome.SUCCESS
        res.task(':gwt1:compileJava').outcome == TaskOutcome.SUCCESS
        res.task(':gwt0:compileGwtJava').outcome == TaskOutcome.SUCCESS
        res.task(':gwt0:compileJava').outcome == TaskOutcome.SUCCESS
        res.output.contains "$rootDir/gwt1/build/classes/java/main"
        res.output.contains "$rootDir/gwt0/build/classes/java/main"
        res.output.contains "$rootDir/gwt0/src/main/java"
        res.output.contains "$rootDir/gwt1/src/main/java"
        res.output.contains "$rootDir/gwt2/src/main/java"

    }

    def "Inter-build dependencies correctly inherit their own platform+archive dependencies"() {
        given:
        setupSimpleGwt('gwt1', '', 'gwt1')
        setupSimpleGwt('gwt2', "xapi-require", 'gwt2')
        withComposite 'gwt2', {
            withProject ':', {
                buildFile << '''
xapiRequire.external 'gwt1:gwt1'
'''
                settingsFile << '''
rootProject.name = 'gwt2'
includeBuild '../gwt1'
'''
            }
        }
        withComposite'gwt1', {
            withProject ':', {
                settingsFile << '''
rootProject.name = 'gwt1'
'''
            }
        }

        when:
        BuildResult res = runSucceed(LogLevel.QUIET, folder('gwt2'),':xapiReport', ':compileGwtJava', '-Pxapi.debug=true')
        then:
        res.task(':compileGwtJava').outcome == TaskOutcome.SUCCESS
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

    def "Cross-module imports work sanely"() {
        given:
        String proj = setupSimpleGwt('crossMod', 'xapi-require')
        getProject(proj).buildFile << '''
xapiRequire.gwt.api.internal('main:main')
'''
        // now, src/gwtApi can see src/main
        getProject(proj).withSource('gwtApi') {
            'ApiMain.java'("""
class ApiMain {
  public static void main(String ... a) {
    com.foo.${proj}.Main.main(a);
  }
}
""")
        }

        when:
        // We use -Pxapi.debug=true to get results printed to stdOut.  We could / should also check the report file.
        BuildResult res = runSucceed('compileGwtJava', ":$proj:xapiReport", '-Pxapi.debug=true')
        then:
        res.task(":$proj:compileGwtApiJava").outcome == TaskOutcome.SUCCESS
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS
        res.output.contains "$rootDir/$proj/src/main/java"
    }

    def "Plugin is compatible with java plugin"() {
        given:
        String proj = setupSimpleGwt()

        when : "Set gradle property to tell xapi to apply java plugin"
        String was = getProject(proj).propertiesFile.text
        getProject(proj).propertiesFile << "$XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA=true"
        BuildResult res = runSucceed('compileGwtJava')

        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS

        when: "Explicitly apply java plugin and re-run"
        getProject(proj).buildFile.text = getProject(proj).buildFile.text.replace 'plugins {',
                '''plugins {
id 'java'
'''
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
        getProject(proj).propertiesFile << "$XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA_LIBRARY=true"
        BuildResult res = runSucceed('compileGwtJava')

        then:
        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS

        when: "Explicitly apply java-library plugin and re-run"
        // When applying the plugins directly, java-library must be applied before xapi plugins.
        // This is future-proofing for a time when we "polyfill" missing java plugins, rather than require them.
        // That is, it would be nice to avoid creating "useless" (to us) configurations,
        // yet still be consumable by projects using only java/java-library.  For now, we apply java or java-library directly.
        getProject(proj).buildFile.text = getProject(proj).buildFile.text.replace 'plugins {',
'''plugins {
id 'java-library'
'''
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
        """
xapiSchema {
  platforms {
    main
    gwt {
      replace main
    }
  }
  archives {
    api
    main.require api
  }
}
"""
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
    XapiSchemaTest selfSpec() {
        return this
    }
}
