package net.wti.loader.plugin


import net.wti.gradle.api.MinimalProjectView
import net.wti.gradle.schema.map.SchemaMap
import net.wti.gradle.test.api.IncludedTestBuild
import net.wti.gradle.test.api.TestProject
import net.wti.gradle.test.api.TestProjectDir
import org.gradle.api.Action
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import xapi.util.X_Namespace

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 30/07/19 @ 4:41 AM.
 */
class XapiLoaderPluginTest extends AbstractSchemaTest<XapiLoaderPluginTest> implements MinimalProjectView{

    @Override
    XapiLoaderPluginTest selfSpec() {
        return this
    }

    def "The loader plugin will create all gradle projects in schema xapi file"() {
        given:
        addSourceCommon()
        addSourceUtil(false)

        when:
        BuildResult res = runSucceed(
                ':util:xapiReport', ':util:compileGwtJava')
        String reportText = file('util', 'build', 'xapiReport', 'report').text
        then:
        res.task(':util:compileGwtJava').outcome == TaskOutcome.SUCCESS
        res.task(':common:compileApiJava').outcome == TaskOutcome.SUCCESS
        reportText.contains "$rootDir/common/src/api/java"
        reportText.contains "$rootDir/util/src/gwt/java"

    }

    def "The loader plugin will interact nicely with the xapi require plugin"() {
        given:
        addSourceCommon()
        addSourceUtil(true)

        when:
        BuildResult res = runSucceed(
                ':util:xapiReport', ':util:compileGwtJava')
        String reportText = file('util', 'build', 'xapiReport', 'report').text
        then:
        res.task(':util:compileGwtJava').outcome == TaskOutcome.SUCCESS
        res.task(':common:compileApiJava').outcome == TaskOutcome.SUCCESS
        reportText.contains "$rootDir/common/src/api/java"
        reportText.contains "$rootDir/util/src/gwt/java"
        runSucceed(':util:compileGwtJava', '--rerun-tasks')
    }

    def "Dependencies inherited through modules-without-source code will still function"() {
        given:
        extraProjects = ", \"impl\""
        addSourceCommon()
        addSourceUtil(true)
        String version = VERSION
        // util depends on common.  Let's grab an spi dependency from common, by depending on the spi module of util.
        withProject 'impl', {
            TestProject p ->
                p.buildFile << """
version='$version'
apply plugin: 'xapi-parser'
apply plugin: 'xapi-require'
"""
                p.file('schema.xapi') << """
<xapi-schema
    platforms = [
        <gwt
            requires = {
                project : { "util" : "main:spi" }
            }
        /gwt>
    ]
/xapi-schema>
"""
                p.sourceFile("gwtSpi", "com.impl.gwt", "GwtSpi") << """
package com.impl.gwt;
class GwtSpi implements com.impl.spi.SpiContract {
  public void doStuff(int value) {
  }
}
"""
        }
        withProject 'common', {
            TestProject common ->
                common.sourceFile('spi', 'com.impl.spi', 'SpiContract') << """
package com.impl.spi;
public interface SpiContract {
  void doStuff(int value);
}
"""
        }

        when:
        runSucceed(':impl:tasks', '--all')
        BuildResult res = runSucceed(
                ':impl:xapiReport', ':impl:compileGwtSpiJava')
        String reportText = file('impl', 'build', 'xapiReport', 'report').text
        then:
        res.task(':impl:compileGwtSpiJava').outcome == TaskOutcome.SUCCESS
        res.task(':common:compileSpiJava').outcome == TaskOutcome.SUCCESS
        reportText.contains "$rootDir/common/src/spi/java"
        reportText.contains "$rootDir/impl/src/gwtSpi/java"
        // note: src/api/java is NOT present, since nothing that was compiled depends on it.
        // not sure if we should try to force everything into the light (robust), or just let things be not-created (efficient)
        runSucceed(':impl:compileGwtSpiJava', '--rerun-tasks')
        cleanup:
        extraProjects = ''
    }

    def "A SchemaMap can be generated when only the root schema file exists"() {
        given:
        SchemaMap map = parseSchema()
        expect:
        // one root project, five children
        map.allProjects.size() == 7
        map.getRootProject().children.size() == 6
        map.allProjects.collect { it.name }.toSet().containsAll getClass().getSimpleName(), 'common', 'util', 'gwt', 'jre', 'consumer', 'producer'
    }

    def "A SchemaMap will pick up schema files in any included module"() {
        given:
        generateSubprojects('jre')
        SchemaMap map = parseSchema()
        expect:
        map.allProjects.size() == 9
        map.allProjects.collect { it.name }.toSet().containsAll getClass().getSimpleName(), 'common', 'util', 'gwt', 'jre', 'consumer', 'producer', 'jreMulti', 'jreSingle'
    }

    // TODO: make a multiplatform=true project, and verify that we can hook up dependencies to/through it via schema.xapi
    //  hm... should really be multiproject=true, and multiplatform simply computed from "is there more than one platform"
    def "A multiplatform equal true project will have a gradle project hooked up per realized module"() {
        given:
        addSourceCommon()
        addSourceUtil(false, false)
        BuildResult result = runSucceed(':util:gwt:compileJava')
        expect:
        result.task(':util:gwt:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':common:compileApiJava').outcome == TaskOutcome.SUCCESS
    }

    def "A multiplatform equal true project will receive dependency hookup through modules without sources"() {
        given:
        addSourceCommon()
        addSourceUtil(false, false)
        withProject ':consumer', {
            TestProject p ->
                p.file('schema.xapi') << """<xapi-schema
    multiplatform = true
    requires = { project : "producer" }
/xapi-schema>"""
        }
        withProject ':producer', {
            TestProject p ->
                p.sourceFile('spi', 'com.prod', 'SpiType') << """package com.prod;
interface SpiType { }"""
                p.file('schema.xapi') << """<xapi-schema
    multiplatform = true
/xapi-schema>"""
        }
        withProject ':util', {
            TestProject p ->
                p.addSource("jre", "com.jre", "Cls", """ package com.jre;
import com.prod.SpiType;
class Cls implements SpiType {}""")
                p.file("schema.xapi") << """<xapi-schema
    platforms = <jre
        modules = <spi
            require = {
                project: { "consumer": "spi" }
            }
        /spi>
    /jre>
/xapi-schema>"""
        }
        BuildResult result = runSucceed(LogLevel.INFO,':util:jre:compileJava')
        expect:
        result.task(':util:jre:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':common:api:compileJava').outcome == TaskOutcome.SUCCESS
    }


    def "A SchemaMap will collect all external preloads from child schemas"() {
        given:
        generateSubprojects('gwt', """
        <preload
            name = "gwt"
            url = "${System.getProperty('xapi.mvn.repo', "$testRepo")}"
            version = "$X_Namespace.GWT_VERSION"
            // limits these artifacts to gwt platform, where they will be auto-available as versionless dependencies
            // this inheritance is also given to any platform replacing gwt platform.
            platforms = [ "gwt" ]
            modules = [ main ]
            artifacts = {
                "com.google.gwt" : [
                    "gwt-user",
                    "gwt-dev",
                    "gwt-codeserver",
                ]
            }
        /preload>
""")
        SchemaMap map = parseSchema()
        expect:
        map.allProjects.size() == 9
        map.allPreloads.size() == 2
        map.allPreloads.map({it.name}).contains('gwt')
        map.allPreloads.map({it.name}).contains('util')
    }

    @Override
    String getGroup() {
        return ""
    }

    @Override
    String getVersion() {
        return ""
    }
}
