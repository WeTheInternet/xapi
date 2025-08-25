package net.wti.gradle.atlas.plugin

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

///-----------------------------------------------------------------------------
/// End‑to‑end verification using Gradle TestKit + Spock.
/// A tiny throw‑away project is generated under a temp directory and the full
/// “generateFonts” pipeline is executed.
///-----------------------------------------------------------------------------
class XapiAtlasGradlePluginTest extends Specification {

    /// JUnit/Spock will inject a fresh temp dir
    @TempDir File testProjectDir

    def "plugin can create a simple blue pixel"() {

        given: "a minimal Gradle build that applies the plugin"
        File settings = new File(testProjectDir, 'settings.gradle')
        settings.text = 'rootProject.name="demo"'

        File build = new File(testProjectDir, 'build.gradle')
        build.text = """
            buildscript {
                repositories {
                    gradlePluginPortal()
                }
                dependencies {
                    classpath "com.github.blueboxware.gdx:com.github.blueboxware.gdx.gradle.plugin:1.5"
                }
            }
            plugins {
                id 'base'
                id 'net.wti.gradle.atlas'
            }

            repositories { mavenLocal(); mavenCentral(); gradlePluginPortal() }

            xapiAtlas {
                pixel 'blue', '#0000ffff'
                outputDir = file('src/main/resources/xapi/atlas')
            }
        """.stripIndent()

        when: "running the pipeline"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()   /// pulls in this plugin’s jars from build/classes
                .withArguments('copyXapiAtlas', '--stacktrace', '--console=plain')
                .build()

        println result.output

        then: "the build succeeds and the expected outputs exist"
        result.task(':copyXapiAtlas').outcome == SUCCESS
        new File(testProjectDir,
                'src/main/resources/xapi/atlas/xapi-atlas.atlas').exists()
    }
}
