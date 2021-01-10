package net.wti.gradle.schema.index


import net.wti.loader.plugin.AbstractSchemaTest

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 2020-07-20 @ 5:05 a.m..
 */
class SchemaIndexerImplTest extends AbstractSchemaTest<SchemaIndexerImplTest> {

    File indexDir
    void setup() {
        indexDir = new File(rootDir, 'index')
        if (indexDir.isDirectory()) {
            indexDir.deleteDir()
        }
        println "running in file://$indexDir.path"
    }

    def "A schema is written for a single module project"() {
        given:
        addSourceUtil(false)
        runSucceed("compileJava")
        expect:
        new File(indexDir, 'path').listFiles()?.length == 6
    }

    @Override
    SchemaIndexerImplTest selfSpec() {
        return this
    }

}
