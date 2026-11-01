package net.wti.gradle.settings.api

import net.wti.gradle.settings.XapiSchemaParser
import net.wti.gradle.settings.plugin.AbstractSchemaTest
import spock.lang.Unroll
import xapi.fu.java.X_Jdk

class SchemaPublicationClosureTest extends AbstractSchemaTest<SchemaPublicationClosureTest> {
    String customSchema

    @Override
    String getSchemaText() { customSchema }

    @Override
    SchemaPublicationClosureTest selfSpec() { this }

    private SchemaProject resolvedProject() {
        doWork()
        def parser = XapiSchemaParser.fromView(this)
        def map = SchemaMap.fromView(this, parser, parser.getSchema(''), SchemaProperties.getInstance())
        map.resolve()
        map.rootProject
    }

    @Unroll
    def 'publication closure is platform scoped with #platforms and Android app at #applicationModule'() {
        given:
        customSchema = """<xapi-schema name="closure" multiplatform=true
            platforms=[$platforms]
            modules=[
                <main published=true include=[bridge] />,
                <bridge published=false include=[leaf] />,
                <leaf published=false include=[bridge] />
            ]
        />"""

        when:
        def project = resolvedProject()

        then:
        !project.getModule('bridge').isPublished()
        !project.getModule('leaf').isPublished()
        project.getModule('main').isPublished('jre')
        project.getModule('bridge').isPublished('jre')
        project.getModule('leaf').isPublished('jre')
        !project.getModule('bridge').isPublished('android')
        !project.getModule('leaf').isPublished('android')
        project.getModule('main').isPublished('android') == (applicationModule != 'main')

        where:
        applicationModule | platforms
        'main'   | '<android modules=[<main buildKind="android-application" />] />, jre'
        'main'   | 'jre, <android modules=[<main buildKind="android-application" />] />'
        'bridge' | '<android modules=[<bridge buildKind="android-application" />] />, jre'
        'bridge' | 'jre, <android modules=[<bridge buildKind="android-application" />] />'
    }

    def 'Android-only application does not promote its library includes'() {
        given:
        customSchema = '''<xapi-schema name="closure" multiplatform=true
            platforms=[<android modules=[<main buildKind="android-application" />] />]
            modules=[<main published=true include=[bridge] />,
                <bridge published=false include=[leaf] />, <leaf published=false />]
        />'''

        when:
        def project = resolvedProject()

        then:
        !project.getModule('main').isPublished('android')
        !project.getModule('bridge').isPublished('android')
        !project.getModule('leaf').isPublished('android')
        !project.getModule('bridge').isPublished()
        !project.getModule('leaf').isPublished()
    }

    def 'resolved inference survives merges without sharing inferred state or overriding build kinds'() {
        given:
        def original = new SchemaModule('leaf', X_Jdk.setLinked(), false, false, false)
        original.inferPublished('jre')
        def other = new SchemaModule('leaf', X_Jdk.setLinked(), false, false, false)
        other.inferPublished('android')
        other.updateBuildKind('android', SchemaModule.BuildKind.ANDROID_APPLICATION)
        def merged = original.update(other)

        expect:
        !merged.isPublished()
        merged.isPublished('jre')
        !merged.isPublished('android')
        !merged.isPublished('main')

        when:
        merged.inferPublished('main')
        merged.updateBuildKind('android', SchemaModule.BuildKind.JAVA_LIBRARY)

        then:
        merged.isPublished('android')
        merged.isPublished('main')
        !original.isPublished('main')
        !original.isPublished('android')
        !other.isPublished('jre')
        !other.isPublished('main')
        other.getBuildKind('android') == SchemaModule.BuildKind.ANDROID_APPLICATION
    }
}
