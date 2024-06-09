package net.wti.gradle.internal.api

import net.wti.gradle.settings.XapiSchemaParser
import net.wti.gradle.settings.api.SchemaMap
import net.wti.gradle.settings.api.SchemaProperties
import net.wti.gradle.settings.index.SchemaIndex
import net.wti.gradle.settings.index.SchemaIndexerImpl
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
        void setup() {
            indexDir = new File(rootDir, 'build/index')
            if (indexDir.isDirectory()) {
                indexDir.deleteDir()
            }
            println "running in file://$indexDir.parent"
        }

        def "A schema is written for a single module project"() {
            given:

            // setup schema + source files
            addSourceCommon()
            addSourceUtil( false)
            flush()

            // manually trigger an index, so we can debug
            SchemaProperties props = SchemaProperties.getInstance()
            SchemaIndexerImpl indexer = new SchemaIndexerImpl(props)
            XapiSchemaParser parser = XapiSchemaParser.fromView(this)
            final DefaultSchemaMetadata root = parser.getSchema()
            SchemaMap map = SchemaMap.fromView(this, parser, root, props)
            final Out1<SchemaIndex> deferred = indexer.index(this, buildName, map)
            map.setIndexProvider(deferred)
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
//            runSucceed(INFO, "compileJava")
            expect:
//            index.getReader().getEntries(this, "common", gwtPlatform, mainModule).hasExplicitDependencies()
            new File(indexDir, 'path').listFiles()?.length == 7
        }

        def "A dependency created from a schema is automatically added to build scripts"() {
            given:
            pluginList = ['java']
            withProject(':') {}
            doWork()
            pluginList = []
            withProject(':') {
                TestProject proj ->
                    proj.file('schema.xapi') << """
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
                    project: [ "producer", "spi" ]
                } /spi>
            ]
        /consumer>,
        <producer
            modules = [
                <spi requires = {
                    project: [ "jre", "main:api" ]
                } /spi>
            ]
        /producer>,
    ]
/xapi-schema>
"""
            }
            withProject(':consumer') {
                TestProject proj ->
                    proj.file("modules/spi/consumerSpi.gradle") << """
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
            // no code for producer module...
            BuildResult result = runSucceed(":consumer-spi:compileJava")
            expect:
            result.task(':consumer-spi:compileJava').outcome == TaskOutcome.SUCCESS
        }

        @Override
        XapiSchemaIndexerTest selfSpec() {
            return this
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


