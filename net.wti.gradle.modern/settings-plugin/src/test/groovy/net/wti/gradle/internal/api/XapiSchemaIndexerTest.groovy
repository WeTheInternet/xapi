package net.wti.gradle.internal.api

import net.wti.gradle.settings.XapiSchemaParser
import net.wti.gradle.settings.api.SchemaMap
import net.wti.gradle.settings.api.SchemaProperties
import net.wti.gradle.settings.index.IndexNodePool
import net.wti.gradle.settings.index.SchemaIndex
import net.wti.gradle.settings.plugin.AbstractSchemaTest
import net.wti.gradle.settings.schema.DefaultSchemaMetadata
import net.wti.gradle.test.api.TestProject
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import xapi.fu.Out1

/**
 * XapiSchemaIndexerTest:
 * <p>
 * <p>
 * Created by James X. Nelson (James@WeTheInter.net) on 08/06/2024 @ 7:26 a.m.
 */
class XapiSchemaIndexerTest extends AbstractSchemaTest<XapiSchemaIndexerTest> {

    File indexDir
    String customSchema

    void setup() {
        customSchema = null
        indexDir = new File(rootDir, 'build/xindex')
        if (indexDir.isDirectory()) {
            indexDir.deleteDir()
        }
        println "running in file://$indexDir.parent"
    }

    def "A schema is written for a single module project"() {
        given:

        // setup schema + source files
        addSourceCommon()
        addSourceUtil(false)
        flush()

        // manually trigger an index, so we can debug
        SchemaProperties props = SchemaProperties.getInstance()
        XapiSchemaParser parser = XapiSchemaParser.fromView(this)
        final DefaultSchemaMetadata root = parser.getSchema("")
        SchemaMap map = SchemaMap.fromView(this, parser, root, props)
        final Out1<SchemaIndex> deferred = map.getIndexProvider()
        def gwtPlatform, mainPlatform, apiModule, mainModule
        map.rootProject.forAllPlatformsAndModules {
            plat, mod ->
                if (plat.name == 'gwt') {
                    gwtPlatform = plat
                } else if (plat.name == 'main') {
                    mainPlatform = plat
                }
                if (mod.name == 'api') {
                    apiModule = mod
                } else if (mod.name == 'main') {
                    mainModule = mod
                }
        }
        map.resolve()
        SchemaIndex index = deferred.out1()
//            runSucceed(INFO, "compileJava")
        expect:
//            index.getReader().getEntries(this, "common", gwtPlatform, mainModule).hasExplicitDependencies()
        new File(indexDir, 'path').listFiles()?.length == 7
    }

    def "A dependency created from a schema is automatically added to build scripts"() {
        given:
        customSchema = """
<xapi-schema
    platforms = [
        main,
        <dev replace = "main" />,
        <prod replace = "main" />,
    ]
    modules = [
        api, spi,
        <main include = [ api, spi ] /main>,
    ]
    projects = [
        <consumer
            platforms = [
                <main
                    modules = [
                        <spi requires = {
                            project: producer1
                        } /spi>
                    ]
                /main>
            ]
        /consumer>,
        <producer1
            platforms = [
                <main
                    modules = [
                        <spi requires = {
                            project: { ":producer2": "main" }
                        } /spi>
                    ]
                /main>
            ]
        /producer1>,
        <producer2
            inherit = false
            platforms = [ main ] // only one platform
        /producer2>,

    ]
/xapi-schema>
"""
        pluginList = ['java']
        withProject(':') {}
        doWork()
        pluginList = []
        withProject(':consumer') {
            TestProject proj ->
                proj.file("src/spi/consumerSpi.gradle") << """
apply from: "\$rootDir/consumer/src/spi/generated-consumerSpi.gradle"
dependencies {
  api project(path: ':producer1-spi')
}
"""
                proj.sourceFile("spi", "com.test", "CompileMe") << """
package com.test;
class CompileMe {
}
"""
        }
        withProject(':producer2') {
            TestProject proj ->
                proj.file("src/gradle/api/buildscript.start") << """
// hi there, hello
"""
        }
        // no code for producer module...
        BuildResult result = runSucceed(":consumer-spi:compileJava", "-i")
        expect:
        result.task(':consumer-spi:compileJava').outcome == TaskOutcome.SUCCESS
    }

    def "A project dependency in a non-standard platform does not use consumer platform"() {
        given:
        customSchema = """
<xapi-schema
    platforms = [
        main,
        <dev replace = "main" />,
        <prod replace = "main" />,
    ]
    modules = [
        api, spi,
        <main include = [ api, spi ] /main>,
    ]

    projects = [
        <consumer
            modules = [
                <spi requires = {
                    project: { ":producer" : "spi" }
                } /spi>
            ]
        /consumer>,
        <producer
            modules = [
                <spi requires = {
                    project: { "producer2" : "main:api" }
                } /spi>
            ]
        /producer>,
        producer2,
    ]
/xapi-schema>
"""
        pluginList = ['java']
        withProject(':') {}
        doWork()
        pluginList = []
        withProject(':consumer') {
            TestProject proj ->
                proj.file("src/spi/consumerSpi.gradle") << """
apply from: "\$rootDir/consumer/src/spi/generated-consumerSpi.gradle"
dependencies {
  api project(path: ':producer-spi')
}
"""
                proj.sourceFile("spi", "com.test", "CompileMe") << """
package com.test;
class CompileMe {
}
"""
        }
        withProject(':producer2') {
            TestProject proj ->
                proj.file("src/gradle/api/buildscript.start") << """
// hi there, hello
"""
        }
        // no code for producer module...
        BuildResult result = runSucceed(":consumer-spi:compileJava", "-i")
        expect:
        result.task(':consumer-spi:compileJava').outcome == TaskOutcome.SUCCESS
    }

    def "Nested multi-module projects correctly create non-main modules"() {
        given:
        customSchema = """
<xapi-schema
    platforms = [
        main, gwt
    ]
    modules = [
        main,
        <sample include = [ main ] /sample>,
    ]
    projects = {
        multiplatform : [
            <consumer
                modules = [
                    <spi requires = {
                        platform : {
                            main : {
                                project: { ":ui:producer" : "sample" }
                            }
                        }
                    } /spi>
                ]
            /consumer>
        ],
        virtual : [
            <ui
                multiplatform = true
                inherit = false
                projects = [
                    <producer
                        modules = [
                            main,
                            <sample
                                includes = main
                                published = true
                                requires = { external : "junit:junit:4.13" }
                            /sample>,
                        ]
                    /producer>
                ]
            /ui>,
        ]
    }
/xapi-schema>
"""
        pluginList = ['java']
        withProject(':') {}
        doWork()
        pluginList = []
        withProject(':consumer') {
            TestProject proj ->
                proj.sourceFile("spi", "com.test", "CompileMe") << """
package com.test;
import com.test.producer.Producer;
class CompileMe extends Producer { }
"""
        }
        withProject(':ui:producer') {
            TestProject proj ->
                proj.sourceFile("com.test.producer", "Producer") << """
package com.test.producer;
public class Producer {}
"""
        }
        // no code for producer module...
        BuildResult result = runSucceed(":consumer-spi:compileJava", "-i")
        expect:
        result.task(':consumer-spi:compileJava').outcome == TaskOutcome.SUCCESS
    }
    def "A single module dependency is targeted correctly from a main module"() {
        given:
        setupConsumerWithTest('main', 'main')
        // no code for producer module...
        BuildResult result = runSucceed(":consumer-main:compileJava", ":producer2:compileTestJava", "-i")
        expect:
        result.task(':consumer-main:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':producer2:compileTestJava').outcome == TaskOutcome.SUCCESS
    }
    def "A single module dependency is targeted correctly from a main module through a non-main module"() {
        given:
        setupConsumerWithTest('main', 'spi')
        // no code for producer module...
        BuildResult result = runSucceed(":consumer-main:compileJava", ":producer2:compileTestJava", "-i")
        expect:
        result.task(':consumer-main:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':producer2:compileTestJava').outcome == TaskOutcome.SUCCESS
    }

    def "A single module dependency is targeted correctly from a non-main module through a main module"() {
        given:
        setupConsumerWithTest('spi', 'main')
        // no code for producer module...
        BuildResult result = runSucceed(":consumer-spi:compileJava", ":producer2:compileTestJava", "-i")
        expect:
        result.task(':consumer-spi:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':producer2:compileTestJava').outcome == TaskOutcome.SUCCESS
    }

    def "A single module dependency is targeted correctly from a non-main module through a non-main module"() {
        given:
        setupConsumerWithTest('spi', 'api')
        // no code for producer module...
        BuildResult result = runSucceed(":consumer-spi:compileJava", ":producer2:compileTestJava", "-i")
        expect:
        result.task(':consumer-spi:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':producer2:compileTestJava').outcome == TaskOutcome.SUCCESS
    }

    def "A non-multiplatform module with main and test source acts like a normal gradle module"() {
        given:
        setupConsumerWithTest('spi')
        // no code for producer module...
        BuildResult result = runSucceed(":consumer-spi:compileJava", ":producer2:compileTestJava", "-i")
        expect:
        result.task(':consumer-spi:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':producer2:compileTestJava').outcome == TaskOutcome.SUCCESS
    }

    def "An external dependency does not add any internal dependencies"() {
        given:
        customSchema = """
<xapi-schema
    platforms = [
        main,
        <dev replace = "main" />,
        <prod replace = "main" />,
    ]
    modules = [
        api, spi,
        <main include = [ api, spi ] /main>,
    ]
    projects = [
        <consumer
            modules = [
                <main
                    requires = {
                        external : "javax.annotation:javax.annotation-api:1.2"
                    }
                /main>,
                <api
                    requires = {
                        @transitive
                        project : { ":producer2" : "main" }
                    }
                /api>,
            ]
        /consumer>,
        <producer2
            inherit=false 
        /producer2>
    ]
/xapi-schema>
"""
        pluginList = ['java']
        withProject(':') {}
        doWork()
        pluginList = []

        addTestSource("main")

        // no code for producer module...
        BuildResult result = runSucceed(":consumer-main:compileJava", ":producer2:compileTestJava", "-i")
        expect:
        result.task(':consumer-main:compileJava').outcome == TaskOutcome.SUCCESS
        result.task(':producer2:compileTestJava').outcome == TaskOutcome.SUCCESS
    }

    @Override
    XapiSchemaIndexerTest selfSpec() {
        return this
    }

    @Override
    String getSchemaText() {
        return customSchema ?: super.getSchemaText()
    }

    @Override
    String getGroup() {
        return ""
    }

    @Override
    String getVersion() {
        return ""
    }

    void setupConsumerWithTest(String consumerModule, String producerModule = 'spi') {

        customSchema = """
<xapi-schema
    platforms = [
        main,
        <dev replace = "main" />,
        <prod replace = "main" />,
    ]
    modules = [
        api, spi,
        <main include = [ api, spi ] /main>,
    ]
    projects = [
        <consumer
            modules = [
                <$consumerModule requires = {
                    project: { ":producer" : "$producerModule" }
                } /$consumerModule>
            ]
        /consumer>,
        <producer
            modules = [
                <$producerModule requires = {
                    @transitive
                    project: { ":producer2" : main }
                } /$producerModule>
            ]
        /producer>,
        <producer2
            inherit = false
            multiplatform = false
        /producer2>,
    ]
/xapi-schema>
"""
        pluginList = ['java']
        withProject(':') {}
        doWork()
        pluginList = []

        addTestSource(consumerModule)
    }

    void addTestSource(String consumerModule) {

        withProject(':consumer') {
            TestProject proj ->
                proj.sourceFile(consumerModule, "com.test", "CompileMe") << """
package com.test;
import com.test2.MainCode;

class CompileMe {
{ System.out.println(MainCode.class); }
}
"""
        }
        withProject(':producer2') {
            TestProject proj ->
                proj.sourceFile("main", "com.test2", "MainCode") << """
package com.test2;
public class MainCode { }
"""
                proj.sourceFile("test", "com.test2", "MainCodeTest") << """
package com.test2;
class MainCodeTest {
{ System.out.println(MainCode.class); }
}
"""
        }
    }
}


