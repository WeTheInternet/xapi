package net.wti.gradle.internal.api

import net.wti.gradle.settings.plugin.AbstractSchemaTest
import net.wti.gradle.test.api.TestProject
import org.gradle.api.logging.LogLevel
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

import static org.gradle.api.logging.LogLevel.INFO

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
            addSourceCommon()
            addSourceUtil( false)
            runSucceed(INFO, "compileJava")
            expect:
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
                    proj.buildFile << """
dependencies {
  spiTransitive project(path: ':producer', configuration: 'spiOut')
}
"""
                    proj.sourceFile("spi", "com.test", "CompileMe") << """
package com.test;
class CompileMe {
}
"""
            }
            // no code for producer module...
            BuildResult result = runSucceed(":consumer:compileSpiJava")
            expect:
            result.task(':consumer:compileSpiJava').outcome == TaskOutcome.SUCCESS
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


