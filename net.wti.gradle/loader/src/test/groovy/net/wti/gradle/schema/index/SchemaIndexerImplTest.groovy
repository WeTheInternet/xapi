package net.wti.gradle.schema.index

import net.wti.gradle.test.api.TestProject
import net.wti.loader.plugin.AbstractSchemaTest
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 5:05 a.m..
 */
class SchemaIndexerImplTest extends AbstractSchemaTest<SchemaIndexerImplTest> {

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
        addSourceCommon(false)
        addSourceUtil(false, false)
        runSucceed("compileJava")
        expect:
        new File(indexDir, 'path').listFiles()?.length == 7
    }

    def "A configuration created from a schema can be referenced immediately from build scripts"() {
        given:
        pluginList = ['xapi-parser']
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
    SchemaIndexerImplTest selfSpec() {
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
