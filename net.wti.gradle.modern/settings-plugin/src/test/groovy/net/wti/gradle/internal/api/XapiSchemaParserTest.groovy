package net.wti.gradle.internal.api

import net.wti.gradle.api.MinimalProjectView
import net.wti.gradle.settings.XapiSchemaParser
import net.wti.gradle.settings.schema.DefaultSchemaMetadata
import net.wti.gradle.test.AbstractMultiBuildTest

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/17/18 @ 4:42 AM.
 */
class XapiSchemaParserTest extends AbstractMultiBuildTest<XapiSchemaParserTest> {


    protected String getDefaultPlugin() {
        return "xapi-schema"
    }

    void setup() {
        setGroup("test")
    }

    private void setupSimpleProject() {

        settingsFile << """
plugins {
  id 'xapi-settings'
}
"""

        schemaFile << """
<xapi-schema
    name = "xapi-test"
    group = "net.wti"
    version = "0.5.1"

    defaultRepoUrl = mavenCentral()

    // schemaLocation = "schema/schema.gradle"

    platforms = [
        <main />,
        // purposely using two different forms of references to main,
        // so that we can detect and fix any weirdness as we go:
        <jre replace = "main" published = true/>,
        <gwt replace = main published = true/>,
    ]

    modules = [
        <api />,
        <spi />,
        <main include = [ api, spi ] />,
        <sample include = "main" published = true />,
        <testTools include = "main" published = true />,
        <test include = ["sample", testTools ] />, // purposely mixing strings and name references, to ensure visitor is robust
    ]

    projects = {
        // the projects below all have gwt, jre and other platforms
        multiplatform: [
            "inject",
            "common",
        ],

        // the projects below are single-module projects.
        standalone: [
            "simple",
            "complex"
        ],
    }

/xapi-schema>
"""

    }

    private String setupSimpleGwt(String name="gwtP", String plugin='', String rootProject=':', String version="1.0") {
        boolean isRoot = name == rootProject
        String localGroup = rootProject == ':' ? 'testgroup' : rootProject
        Closure<?> configure = {
            def use = plugin ?: defaultPlugin
            buildFile << """
allprojects {
  repositories {
    maven {
      name = 'xapiLocal'
      url = '$xapiRepo'
      metadataSources { gradleMetadata() }
    }
  }
  version = $version
  group = '${localGroup}'
}
"""
            propertiesFile << """
xapi.home=${topDir}
xapiGroupId=$localGroup
xapiVersion=$version
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

    def "Schema parser can read our test schema correctly"() {
        given:
        setupSimpleProject()
        XapiSchemaParser parser = new XapiSchemaParser() {
            @Override
            MinimalProjectView getView() {
                return XapiSchemaParserTest.this
            }
        }
        DefaultSchemaMetadata metadata = parser.parseSchema(this, null)
        expect:
        metadata.getModules().size() == 6
        metadata.getPlatforms().size() == 3 // main, jre, gwt
    }

//    def "Intra-build dependencies correctly inherit their own platform+archive dependencies"() {
//        given:
//        setupSimpleGwt('gwt1')
//        setupSimpleGwt('gwt2', "xapi-require")
//        getProject('gwt2').buildFile << '''
//xapiRequire.project 'gwt1'
//'''
//
//        when:
//        BuildResult res = runSucceed(':gwt2:xapiReport', ':gwt2:compileGwtJava', '-Pxapi.debug=true')
//        then:
//        res.task(':gwt1:compileGwtJava').outcome == TaskOutcome.SUCCESS
//        res.task(':gwt1:compileJava').outcome == TaskOutcome.SUCCESS
//        res.output.contains "$rootDir/gwt1/src/main/java"
//        res.output.contains "$rootDir/gwt2/src/main/java"
//
//    }
//
//    def "Intra-build dependencies correctly inherit transitive platform+archive dependencies"() {
//        given:
//        setupSimpleGwt('gwt0')
//        setupSimpleGwt('gwt1', 'xapi-require')
//        getProject('gwt1').buildFile << "xapiRequire.project 'gwt0'"
//        setupSimpleGwt('gwt2', 'xapi-require')
//        getProject('gwt2').buildFile << """
//xapiRequire.project 'gwt1'
//import net.wti.gradle.schema.plugin.XapiSchemaPlugin
//dependencies {
//    attributesSchema.attribute(XapiSchemaPlugin.ATTR_PLATFORM_TYPE)
//    attributesSchema.attribute(XapiSchemaPlugin.ATTR_ARTIFACT_TYPE)
//}
//"""
////        AttributesSchema attributesSchema; attributesSchema.attribute(XapiSchemaPlugin.ATTR_PLATFORM_TYPE)
//
//        when:
//        BuildResult res
//        res = runSucceed(':gwt2:xapiReport', // ':gwt2:xapiPublish',
//                ':gwt2:compileGwtJava', '-Pxapi.debug=true')
//        then:
//        res.task(':gwt2:compileGwtJava').outcome == TaskOutcome.SUCCESS
//        res.task(':gwt2:compileJava').outcome == TaskOutcome.SUCCESS
//        res.task(':gwt1:compileGwtJava').outcome == TaskOutcome.SUCCESS
//        res.task(':gwt1:compileJava').outcome == TaskOutcome.SUCCESS
//        res.task(':gwt0:compileGwtJava').outcome == TaskOutcome.SUCCESS
//        res.task(':gwt0:compileJava').outcome == TaskOutcome.SUCCESS
//        res.output.contains "$rootDir/gwt1/build/classes/java/main"
//        res.output.contains "$rootDir/gwt0/build/classes/java/main"
//        res.output.contains "$rootDir/gwt0/src/main/java"
//        res.output.contains "$rootDir/gwt1/src/main/java"
//        res.output.contains "$rootDir/gwt2/src/main/java"
//
//    }
//
//    def "Inter-build dependencies correctly inherit their own platform+archive dependencies"() {
//        given:
//        setupSimpleGwt('gwt1', '', 'gwt1')
//        setupSimpleGwt('gwt2', "xapi-require", 'gwt2')
//        withComposite 'gwt2', {
//            withProject ':', {
//                buildFile << '''
//xapiRequire.external 'gwt1:gwt1', 'gwt:main'
//'''
//                settingsFile << '''
//rootProject.name = 'gwt2'
//includeBuild '../gwt1'
//'''
//            }
//        }
//        withComposite'gwt1', {
//            withProject ':', {
//                settingsFile << '''
//rootProject.name = 'gwt1'
//'''
//            }
//        }
//
//        when:
//        // seems to be failing to properly aggregate the gwt platform; check the BuildGraph for bugs!
//        BuildResult res = runSucceed(INFO, folder('gwt2'),
//                ':xapiReport', ':compileGwtJava', '-Pxapi.debug=true')
//        then:
//        res.task(':compileGwtJava').outcome == TaskOutcome.SUCCESS
//        res.task(':gwt1:compileGwtJava').outcome == TaskOutcome.SUCCESS
//        res.task(':gwt1:compileJava').outcome == TaskOutcome.SUCCESS
//        res.output.contains "$rootDir/gwt1/src/main/java"
//        res.output.contains "$rootDir/gwt2/src/main/java"
//        // enforce finding class folders rather than jars.  We want to keep everything directory-based as long as possible.
//        res.output.contains "$rootDir/gwt1/build/classes/java/main"
//        res.output.contains "$rootDir/gwt1/build/classes/java/gwt"
//        res.output.contains "$rootDir/gwt2/build/classes/java/main"
//        res.output.contains "$rootDir/gwt2/build/classes/java/gwt"
//
//    }
//
//    def "Gwt platform automatically inherits own project sources"() {
//        given:
//        String proj = setupSimpleGwt()
//
//        when:
//        // We use -Pxapi.debug=true to get results printed to stdOut.  We could / should also check the report file.
//        BuildResult res = runSucceed('compileGwtJava', ":$proj:xapiReport", '-Pxapi.debug=true')
//        then:
//        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
//        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS
//        res.output.contains "$rootDir/$proj/src/main/java"
//    }
//
//    def "Cross-module imports work sanely"() {
//        given:
//        String proj = setupSimpleGwt('crossMod', 'xapi-require')
//        getProject(proj).buildFile << '''
//xapiRequire.gwt.api.internal('main:main')
//'''
//        // now, src/gwtApi can see src/main
//        getProject(proj).withSource('gwtApi') {
//            'ApiMain.java'("""
//class ApiMain {
//  public static void main(String ... a) {
//    com.foo.${proj}.Main.main(a);
//  }
//}
//""")
//        }
//
//        when:
//        // We use -Pxapi.debug=true to get results printed to stdOut.  We could / should also check the report file.
//        BuildResult res = runSucceed('compileGwtJava', ":$proj:xapiReport", '-Pxapi.debug=true')
//        then:
//        res.task(":$proj:compileGwtApiJava").outcome == TaskOutcome.SUCCESS
//        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
//        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS
//        res.output.contains "$rootDir/$proj/src/main/java"
//    }
//
//    def "Plugin is compatible with java plugin"() {
//        given:
//        String proj = setupSimpleGwt()
//
//        when : "Set gradle property to tell xapi to apply java plugin"
//        String was = getProject(proj).propertiesFile.text
//        getProject(proj).propertiesFile << "$XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA=true"
//        BuildResult res = runSucceed('compileGwtJava')
//
//        then:
//        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
//        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS
//
//        when: "Explicitly apply java plugin and re-run"
//        getProject(proj).buildFile.text = getProject(proj).buildFile.text.replace 'plugins {',
//                '''plugins {
//id 'java'
//'''
//        res = runSucceed('compileGwtJava')
//        then:
//        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.UP_TO_DATE
//        res.task(":$proj:compileJava").outcome == TaskOutcome.UP_TO_DATE
//
//        when: "Remove the gradle.properties changes and run again"
//        getProject(proj).propertiesFile.text = was
//        res = runSucceed('compileGwtJava')
//        then:
//        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.UP_TO_DATE
//        res.task(":$proj:compileJava").outcome == TaskOutcome.UP_TO_DATE
//    }
//
//    def "Plugin is compatible with java-library plugin"() {
//        given:
//        String proj = setupSimpleGwt()
//
//        when : "Set gradle property to tell xapi to apply java library plugin"
//        String was = getProject(proj).propertiesFile.text
//        getProject(proj).propertiesFile << "$XapiBasePlugin.PROP_SCHEMA_APPLIES_JAVA_LIBRARY=true"
//        BuildResult res = runSucceed('compileGwtJava')
//
//        then:
//        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.SUCCESS
//        res.task(":$proj:compileJava").outcome == TaskOutcome.SUCCESS
//
//        when: "Explicitly apply java-library plugin and re-run"
//        // When applying the plugins directly, java-library must be applied before xapi plugins.
//        // This is future-proofing for a time when we "polyfill" missing java plugins, rather than require them.
//        // That is, it would be nice to avoid creating "useless" (to us) configurations,
//        // yet still be consumable by projects using only java/java-library.  For now, we apply java or java-library directly.
//        getProject(proj).buildFile.text = getProject(proj).buildFile.text.replace 'plugins {',
//'''plugins {
//id 'java-library'
//'''
//        res = runSucceed('compileGwtJava')
//        then:
//        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.UP_TO_DATE
//        res.task(":$proj:compileJava").outcome == TaskOutcome.UP_TO_DATE
//
//        when: "Remove the gradle.properties changes and run again"
//        getProject(proj).propertiesFile.text = was
//        res = runSucceed('compileGwtJava')
//        then:
//        res.task(":$proj:compileGwtJava").outcome == TaskOutcome.UP_TO_DATE
//        res.task(":$proj:compileJava").outcome == TaskOutcome.UP_TO_DATE
//    }

    @Override
    XapiSchemaParserTest selfSpec() {
        return this
    }
}
