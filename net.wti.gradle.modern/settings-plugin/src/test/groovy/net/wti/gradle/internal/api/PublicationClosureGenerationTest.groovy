package net.wti.gradle.internal.api

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

class PublicationClosureGenerationTest extends Specification {
    @TempDir File fixture

    private File write(String path, String contents) {
        File target = new File(fixture, path)
        target.parentFile.mkdirs()
        target.text = contents
        target
    }

    private def run() {
        GradleRunner.create().withProjectDir(fixture).withArguments(
                '--offline', '--stacktrace', '-g', new File(System.getProperty('user.home'), '.gradle').path,
                'verifyPublicationClosure').withDebug(false).build()
    }

    @Unroll
    def 'generated publication closure: #label'() {
        given:
        String classpath = new File('build/testRuntime.classpath').text.split(File.pathSeparator)
                .collect { "'${it.replace('\\', '/').replace("'", "\\'")}'" }.join(',\n')
        write('settings.gradle', """
buildscript { dependencies { classpath files('stub-plugins', $classpath) } }
apply plugin: 'xapi-settings'
""")
        write('stub-plugins/META-INF/gradle-plugins/com.android.application.properties',
                'implementation-class=' + PublicationProbeAndroidPlugin.name + '\n')
        String applicationConfig = applicationModule == 'main' ? 'android' : 'androidBridge'
        write('build.gradle', """
plugins { id 'base' }
gradle.beforeProject { p ->
    if (p.findProperty('xapiBuildKind') != 'android-application') p.plugins.apply('java-library')
}
tasks.register('verifyPublicationClosure') {
    doLast {
        def app = project(':app-$applicationConfig')
        assert !app.plugins.hasPlugin('java')
        assert !app.plugins.hasPlugin('java-library')
        assert !app.plugins.hasPlugin('maven-publish')
        assert app.tasks.findByName('xapiPublish') == null
        assert app.tasks.matching { it.name.startsWith('publish') }.empty
    }
}
""")
        String android = "<android modules=[<$applicationModule buildKind=\"android-application\" />] />"
        String platforms = androidOnly ? android : androidFirst ? "$android, jre" : "jre, $android"
        String moduleGraph = '''modules=[<main published=true include=[bridge] />,
            <bridge published=false include=[leaf] />, <leaf published=false />]'''
        if (inherited) {
            String inheritedPlatforms = androidFirst ? 'android, jre' : 'jre, android'
            write('schema.xapi', """<xapi-schema name="publication-fixture" version="1.0"
                multiplatform=true platforms=[$inheritedPlatforms] $moduleGraph projects=[
                    <app inherit=true multiplatform=true platforms=[$android] />,
                    <sibling inherit=true multiplatform=true />
                ]
            />""")
        } else {
            write('schema.xapi', """<xapi-schema name="publication-fixture" version="1.0"
            platforms=[main] modules=[main] projects=[
                <app inherit=false multiplatform=true platforms=[$platforms]
                    $moduleGraph
                />
            ]
        />""")
        }
        def configs = ['android', 'androidBridge', 'androidLeaf']
        if (!androidOnly) configs.addAll(['jre', 'jreBridge', 'jreLeaf'])
        configs.each { key ->
            write("app/src/$key/java/fixture/Probe.java", 'package fixture; public class Probe {}')
            if (inherited) {
                write("sibling/src/$key/java/fixture/Probe.java", 'package fixture; public class Probe {}')
            }
        }
        write("app/src/$applicationConfig/gradle/plugins", "id 'com.android.application'\n")

        when:
        def firstResult = run()
        def scripts = configs.collectEntries { key ->
            [(key): new File(fixture, "app/src/$key/app${key.capitalize()}.gradle").text]
        }
        def secondResult = run()

        then:
        configs.every { key ->
            new File(fixture, "app/src/$key/app${key.capitalize()}.gradle").text == scripts[key]
        }
        ['androidBridge', 'androidLeaf'].every { key ->
            !new File(fixture, "build/xindex/path/_app/$key/publish").exists()
        }
        new File(fixture, 'build/xindex/path/_app/android/publish').exists() == (applicationModule != 'main')
        !scripts[applicationConfig].contains('maven-publish')
        !scripts[applicationConfig].contains('xapiPublish')
        !scripts.androidLeaf.contains('maven-publish')
        androidOnly || ['jre', 'jreBridge', 'jreLeaf'].every { key ->
            new File(fixture, "build/xindex/path/_app/$key/publish").text == 'true' &&
                    scripts[key].contains('maven-publish') && scripts[key].contains('xapiPublish')
        }
        !inherited || configs.every { key ->
            new File(fixture, "build/xindex/path/_sibling/$key/publish").text == 'true' &&
                    new File(fixture, "sibling/src/$key/sibling${key.capitalize()}.gradle").text.contains('maven-publish')
        }

        cleanup:
        if (firstResult != null) {
            File evidence = new File('build/test-evidence/publication-closure', label)
            evidence.mkdirs()
            new File(evidence, 'first.log').text = firstResult.output
            if (secondResult != null) new File(evidence, 'second.log').text = secondResult.output
            scripts?.each { key, contents -> new File(evidence, "${key}.gradle").text = contents }
            new File(evidence, 'schema.xapi').text = new File(fixture, 'schema.xapi').text
            new File(evidence, 'publish-flags.txt').text = configs.collect { key ->
                "$key=" + new File(fixture, "build/xindex/path/_app/$key/publish").exists()
            }.join('\n') + '\n'
        }

        where:
        label                     | androidOnly | androidFirst | applicationModule | inherited
        'android-only'            | true        | true         | 'main'            | false
        'android-first-source'    | false       | true         | 'main'            | false
        'jre-first-source'        | false       | false        | 'main'            | false
        'android-first-barrier'   | false       | true         | 'bridge'          | false
        'jre-first-barrier'       | false       | false        | 'bridge'          | false
        'android-first-inherited' | false       | true         | 'bridge'          | true
        'jre-first-inherited'     | false       | false        | 'bridge'          | true
    }

    static class PublicationProbeAndroidPlugin implements Plugin<Project> {
        void apply(Project project) {
            assert project.findProperty('xapiBuildKind') == 'android-application'
            assert !project.plugins.hasPlugin('java')
            assert !project.plugins.hasPlugin('java-library')
            // Supply only the application dependency bucket used by the generated
            // includes. Publication behavior needs neither AGP nor an Android SDK.
            project.configurations.create('implementation')
            project.extensions.add('android', new PublicationProbeAndroidExtension())
        }
    }

    static class PublicationProbeAndroidExtension {
        void sourceSets(Closure ignored) {}
    }
}
