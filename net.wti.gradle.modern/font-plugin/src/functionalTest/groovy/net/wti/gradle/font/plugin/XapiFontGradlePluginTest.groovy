package net.wti.gradle.font.plugin

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

///-----------------------------------------------------------------------------
/// End‑to‑end verification using Gradle TestKit + Spock.
/// A tiny throw‑away project is generated under a temp directory and the full
/// “generateFonts” pipeline is executed.
///-----------------------------------------------------------------------------
class XapiFontGradlePluginTest extends Specification {

    /// JUnit/Spock will inject a fresh temp dir
    @TempDir File testProjectDir

    def "plugin downloads fonts, generates bitmap fonts and packs textures"() {

        given: "a minimal Gradle build that applies the plugin"
        File settings = new File(testProjectDir, 'settings.gradle')
        settings.text = 'rootProject.name="demo"'

        File build = new File(testProjectDir, 'build.gradle')
        build.text = """
            import net.wti.gradle.font.ext.FontConfig
            buildscript {
                repositories {
                    gradlePluginPortal()
                }
                dependencies {
                    classpath "com.github.blueboxware.gdx:com.github.blueboxware.gdx.gradle.plugin:1.5"
                }
            }
            plugins {
                id 'net.wti.gradle.font'
            }

            repositories { mavenLocal(); mavenCentral(); gradlePluginPortal() }

            xapiFonts {
                googleFont 'Questrial', { FontConfig conf ->
                    conf.addWeight 'Regular', 8, 12
                }
            }
        """.stripIndent()

        /// Dummy characters file so the task has input
        File chars = new File(testProjectDir, 'build/all-i18n.txt')
        chars.parentFile.mkdirs()
        chars.text = 'ABC✓→✕'

        when: "running the pipeline"
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()   /// pulls in this plugin’s jars from build/classes
                .withArguments('packFonts', '--stacktrace')
                .build()

        println result.output

        then: "the build succeeds and the expected outputs exist"
        result.task(':packFonts').outcome == SUCCESS
        new File(testProjectDir,
                'build/generated-fonts/Questrial-Regular12px.fnt').exists()
        new File(testProjectDir,
                'build/generated-assets/font.atlas').exists()
    }
}
