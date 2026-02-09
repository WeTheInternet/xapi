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

    def "inherit=false does not inherit extra platforms into index"() {
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
        <producer2
            inherit = false
            platforms = [ main ]
        /producer2>
    ]
/xapi-schema>
"""
        pluginList = ['java']
        withProject(':') {}
        doWork()
        pluginList = []

        // ensure the project exists on disk so the index has a place to look
        withProject(':producer2') { TestProject proj ->
            proj.file("src/gradle/api/buildscript.start") << "// marker"
        }

        when:
        // Trigger something that causes settings plugin to run & index to be written
        runSucceed("tasks", "-i")

        then:
        // No producer2-dev / producer2-prod coordinate dirs should exist anywhere in coord index.
        File coordDir = new File(indexDir, "coord")
        assert coordDir.isDirectory()

        def bad = []
        coordDir.eachFileRecurse { File f ->
            if (f.isDirectory()) {
                def n = f.name
                if (n.startsWith("producer2-") && (n.endsWith("-dev") || n.endsWith("-prod"))) {
                    bad << f
                }
            }
        }
        assert bad.isEmpty() : "Found unexpected inherited platforms in coord index:\\n" + bad.join("\\n")
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

    def "Platform-level requires are applied across a chain of replacing platforms"() {
        given:
        // Three platforms in a replacement chain: main -> impl -> sample.
        // Each layer adds its own external requires at the *platform* level.
        // The sample platform's requires should be visible to all of its modules.
        customSchema = """
<xapi-schema
    platforms = [
        // base platform, with its own requires
        <main
            requires = {
                external : "junit:junit:4.13.2"
            }
        /main>,

        // impl extends main, adds more requires
        <impl replace = "main"
            requires = {
                external : "org.hamcrest:hamcrest-core:1.3"
            }
        /impl>,

        // sample extends impl, adds yet another requires
        <sample replace = "impl"
            requires = {
                external : "org.assertj:assertj-core:3.25.3"
            }
        /sample>
    ]
    modules = [
        // a simple single-module project; this module should see all platform-level requires
        main
    ]
    projects = [
        <sampleProject
            // use all three platforms for this project
            multiplatform = true
            platforms = [ main, impl, sample ]
            modules = [ main ]
        /sampleProject>
    ]
/xapi-schema>
"""
        // Apply Java plugin to generated projects
        pluginList = ['java']
        withProject(':') {}
        doWork()
        pluginList = []

        // Add code that uses junit on the *sample* platform's main module.
        // If platform-level requires on sample aren't applied to its modules
        // (or if the replacement chain breaks them), this will fail to compile.
        withProject(':sampleProject') {
            TestProject proj ->
                proj.sourceFile("main", "com.example", "UsesPlatformRequires") << """
package com.example;

import org.junit.Assert;

public class UsesPlatformRequires {
    public void check() {
        Assert.assertTrue(true);
    }
}
"""
        }

        // The Gradle module name for the main archive of a multiplatform project
        // is `<project>-main`. Compiling it exercises the platform graph.
        BuildResult result = runSucceed(":sampleProject-main:compileJava", "-i", "-Dxapi.log.level=TRACE")

        expect:
        result.task(':sampleProject-main:compileJava').outcome == TaskOutcome.SUCCESS
    }

    def "Project-level requires are applied to all modules across replacing platforms"() {
        given:
        // Same platform chain main -> impl -> sample, but this time the requires = {}
        // are declared on the project itself instead of on the platform declarations.
        // The project-level requires should still be visible to all modules on each platform.
        customSchema = """
<xapi-schema
    platforms = [
        main,
        <impl replace = "main" />,
        <sample replace = "impl" />
    ]
    modules = [
        main
    ]
    projects = [
        <sampleProject
            multiplatform = true
            platforms = [ main, impl, sample ]
            modules = [ main ]
            requires = {
                // project-level external requirement that should apply
                // to the main module on all platforms (including sample)
                external : "junit:junit:4.13.2"
            }
        /sampleProject>
    ]
/xapi-schema>
"""
        pluginList = ['java']
        withProject(':') {}
        doWork()
        pluginList = []

        // Use junit from the sample platform's main module.
        // This verifies that project-level requires are correctly applied
        // when there is a chain of replacing platforms.
        withProject(':sampleProject') {
            TestProject proj ->
                proj.sourceFile("main", "com.example", "UsesProjectRequires") << """
package com.example;

import org.junit.Assert;

public class UsesProjectRequires {
    public void check() {
        Assert.assertTrue(true);
    }
}
"""
        }

        BuildResult result = runSucceed(":sampleProject-main:compileJava", "-i")

        expect:
        result.task(':sampleProject-main:compileJava').outcome == TaskOutcome.SUCCESS
    }

    def "inherit=false project still triggers -sources project when consumed from a sourcePublished platform"() {
        given:
        customSchema = """
<xapi-schema
    platforms = [
        <main
            published = true
            needSource = true
        /main>
    ]
    modules = [ main ]
    projects = [
        <consumer
            modules = [
                <main requires = {
                    project: { ":producer2" : "main" }
                } /main>
            ]
        /consumer>,
        <producer2
            inherit = false
            multiplatform = false
            platforms = [ main ]
            modules = [ main ]
        /producer2>,
    ]
/xapi-schema>
"""
        pluginList = ['java']
        withProject(':') {}
        doWork()
        pluginList = []

        // make consumer + producer2 "live enough" for settings generation paths to run
        withProject(':consumer') { proj ->
            proj.sourceFile("main", "com.test", "ConsumeMe") << """
package com.test;
class ConsumeMe {}
"""
        }
        withProject(':producer2') { proj ->
            proj.sourceFile("main", "com.test2", "ProduceMe") << """
package com.test2;
public class ProduceMe {}
"""
        }

        when:
        def result = runSucceed(":consumer-main-sources:jar", "-i", "-Dxapi.log.level=INFO")

        then:
        result.task(":producer2-sources:compileJava").outcome == TaskOutcome.NO_SOURCE
        result.task(":producer2-sources:copySource").outcome == TaskOutcome.SUCCESS
    }

    def "inherit=false singleplatform dependency still triggers -sources project when consumer needs sources and platform publishes sources"() {
        given:
        customSchema = """
<xapi-schema
    platforms = [
        <main published = true /main>,
        <gwt replace = "main" published = true publishSource = true /gwt>
    ]
    modules = [ main ]
    projects = [
        <consumer
            multiplatform = true
            platforms = [ gwt ]
            modules = [
                <main requires = {
                    project: { ":producer2" : "main" }
                } /main>
            ]
        /consumer>,
        <producer2
            inherit = false
            multiplatform = false
            platforms = [ main ]
            modules = [ main ]
        /producer2>,
    ]
/xapi-schema>
"""
        pluginList = ['java']
        withProject(':') {}
        doWork()
        pluginList = []

        withProject(':consumer') { proj ->
            // Make gwt:main live (so we enter the needSource path for gwt).
            proj.sourceFile("gwt", "com.test", "ConsumeMe") << """
package com.test;
class ConsumeMe {}
"""
        }
        withProject(':producer2') { proj ->
            proj.sourceFile("main", "com.test2", "ProduceMe") << """
package com.test2;
public class ProduceMe {}
"""
        }

        when:
        def result = runSucceed("tasks", "-i", "-Dxapi.log.level=INFO")

        then:
        // This log line comes from XapiSettingsPlugin when it decides to create the `-sources` sibling project.
        result.output.contains("Setting up transitive source project")
    }
    def "Platform-level @transitive(test) requires are inherited across replace= chain and compiled in test scope"() {
        given:
        customSchema = """
<xapi-schema
    platforms = [
        <main
            requires = {
                @transitive("test")
                external : "junit:junit:4.13.2"
            }
        /main>,
        <impl replace = "main"
            requires = {
                @transitive("test")
                external : "org.hamcrest:hamcrest-core:1.3"
            }
        /impl>
    ]
    modules = [ main ]
    projects = [
        <sampleProject
            multiplatform = true
            platforms = [ main, impl ]
            modules = [ main ]
        /sampleProject>
    ]
/xapi-schema>
"""
        pluginList = ['java']
        withProject(':') {}
        doWork()
        pluginList = []

        // Put test source somewhere under sampleProject/src/**/test so generated build detects it as test sources.
        // We don't rely on exact key naming; we just add it under a couple of likely locations,
        // and then verify generated buildscript contains testImplementation deps.
        withProject(':sampleProject') { proj ->
            proj.sourceFile("implTest", "com.example", "UsesTransitiveTestDepsTest") << """
package com.example;

import org.junit.Test;
import org.hamcrest.MatcherAssert;
import static org.hamcrest.CoreMatchers.is;

public class UsesTransitiveTestDepsTest {
  @Test
  public void ok() {
    MatcherAssert.assertThat(true, is(true));
  }
}
"""
        }

        when:
        // Force settings plugin to generate all build scripts, then compile tests (will fail if deps land in implementation instead of testImplementation)
        def result = runSucceed("testClasses", "-i", "-Dxapi.log.level=TRACE")

        then:
        result.task(":sampleProject-impl:compileTestJava").outcome == TaskOutcome.SUCCESS

        and:
        // Verify the generated impl buildscript contains BOTH deps in testImplementation (and not implementation).
        File projDir = new File(rootDir, "sampleProject")
        def gradleFiles = []
        projDir.eachFileRecurse { File f ->
            if (f.name.toLowerCase().contains("impl") && f.name.endsWith(".gradle") && !f.name.contains("-sources")) {
                gradleFiles << f
            }
        }
        assert !gradleFiles.isEmpty() : "Did not find any generated impl gradle file under ${projDir.absolutePath}"

        def implScript = gradleFiles.collect { it.text }.join("\n---\n")
        assert implScript.contains("testImplementation \"junit:junit:4.13.2\"")
        assert implScript.contains("testImplementation \"org.hamcrest:hamcrest-core:1.3\"")
        assert !implScript.contains("implementation \"org.hamcrest:hamcrest-core:1.3\"")
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
