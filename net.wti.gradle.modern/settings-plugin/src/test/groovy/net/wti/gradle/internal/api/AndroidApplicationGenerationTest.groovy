package net.wti.gradle.internal.api

import net.wti.gradle.settings.api.SchemaModule
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.TempDir
import xapi.fu.java.X_Jdk

import java.util.zip.ZipFile

class AndroidApplicationGenerationTest extends Specification {
    @TempDir File fixture

    private File write(String path, String contents) {
        File target = new File(fixture, path)
        target.parentFile.mkdirs()
        target.text = contents
        target
    }

    private String schema(String attributes, String dependencies = '', String platformAttributes = '') {
        """<xapi-schema name="android-fixture" version="1.0" platforms=[main] modules=[main] projects=[
            <app inherit=false multiplatform=true
                platforms=[main, jre, <android $platformAttributes modules=[
                    <main $attributes $dependencies /main>
                ] /android>]
                modules=[main]
            /app>
        ] /xapi-schema>"""
    }

    private void prepare(String attributes = '', String dependencies = '', String platformAttributes = '') {
        String classpath = new File('build/testRuntime.classpath').text.split(File.pathSeparator)
                .collect { "'${it.replace('\\', '/').replace("'", "\\'")}'" }.join(',\n')
        write('settings.gradle', """
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
buildscript { dependencies { classpath files($classpath) } }
apply plugin: 'xapi-settings'
""")
        write('build.gradle', '''
plugins { id 'base' }
// Marker must precede consumer root beforeProject fallbacks and app plugin application.
gradle.beforeProject { p ->
    if (p.findProperty('xapiBuildKind') != 'android-application') p.plugins.apply('java-library')
}
''')
        write('schema.xapi', schema(attributes, dependencies, platformAttributes))
        write('app/src/main/java/fixture/Shared.java', 'package fixture; public class Shared {}')
        write('app/src/jre/java/fixture/Jre.java', 'package fixture; public class Jre {}')
        write('app/src/android/java/fixture/MainActivity.java', 'package fixture; public class MainActivity {}')
    }

    private def run(boolean succeeds, String... arguments) {
        List<String> args = ['--stacktrace', '-g', new File(System.getProperty('user.home'), '.gradle').path]
        String javaHome = System.getenv('ANDROID_TEST_JAVA_HOME')
        if (javaHome) args.add('-Dorg.gradle.java.home=' + javaHome)
        args.addAll(arguments)
        def runner = GradleRunner.create().withProjectDir(fixture).withArguments(args).withDebug(false)
        succeeds ? runner.build() : runner.buildAndFail()
    }

    private String script(String key) {
        new File(fixture, "app/src/$key/app${key.capitalize()}.gradle").text
    }

    def 'qualified Android declaration preserves JVM output and index semantics'() {
        given:
        prepare()
        run(true, 'help')
        String mainBefore = script('main')
        String jreBefore = script('jre')
        write('schema.xapi', schema('buildKind="android-application"'))

        when:
        def result = run(false, 'help')
        String android = script('android')

        then:
        result.output.contains('requires com.android.application in the owning plugins fragment')
        script('main') == mainBefore
        script('jre') == jreBefore
        android.contains('android {')
        android.contains('repositories.google()')
        android.contains('manifest.srcFile("$rootDir/app/src/android/AndroidManifest.xml")')
        android.contains('jniLibs.setSrcDirs(["$rootDir/app/src/android/jniLibs"])')
        android.contains('java.setSrcDirs(["$rootDir/app/src/androidTest/java"])')
        !android.contains('java-library')
        !android.contains('java.toolchain')
        !android.contains('sourceSets.maybeCreate')
        !android.contains('maven-publish')
        !android.contains('components.named')
        !new File(fixture, 'build/xindex/path/_app/android/publish').exists()
        new File(fixture, 'build/xindex/path/_app/main/publish').text == 'true'
        !new File(fixture, 'app/src/android/build/srcModAndroid').exists()
    }

    def 'build kind merging preserves absent and qualified declarations without sharing maps'() {
        given:
        def original = new SchemaModule('main', X_Jdk.setLinked(), true, false, false)
                .updateBuildKind(null, SchemaModule.BuildKind.JAVA_LIBRARY)
                .updateBuildKind('android', SchemaModule.BuildKind.ANDROID_APPLICATION)
        def merged = original.update(new SchemaModule('main', X_Jdk.setLinked(), false, false, false))
        def override = new SchemaModule('main', X_Jdk.setLinked(), false, false, false)
                .updateBuildKind('android', SchemaModule.BuildKind.JAVA_LIBRARY)

        expect:
        merged.getBuildKind('main') == SchemaModule.BuildKind.JAVA_LIBRARY
        merged.getBuildKind('android') == SchemaModule.BuildKind.ANDROID_APPLICATION
        !merged.isPublished('android')
        merged.isPublished('main')
        merged.update(override).getBuildKind('android') == SchemaModule.BuildKind.JAVA_LIBRARY
        original.getBuildKind('android') == SchemaModule.BuildKind.ANDROID_APPLICATION

        when:
        merged.updateBuildKind('android', SchemaModule.BuildKind.JAVA_LIBRARY)

        then:
        original.getBuildKind('android') == SchemaModule.BuildKind.ANDROID_APPLICATION
    }

    def 'nearer default changes fallback platforms without erasing inherited qualified choices'() {
        given:
        def parent = new SchemaModule('main', X_Jdk.setLinked(), true, false, false)
                .updateBuildKind(null, SchemaModule.BuildKind.ANDROID_APPLICATION, 0)
                .updateBuildKind('android', SchemaModule.BuildKind.ANDROID_APPLICATION, 0)
        def childDefault = new SchemaModule('main', X_Jdk.setLinked(), false, false, false)
                .updateBuildKind(null, SchemaModule.BuildKind.JAVA_LIBRARY, 1)
        def child = parent.update(childDefault).update(parent)

        expect:
        child.getBuildKind('main') == SchemaModule.BuildKind.JAVA_LIBRARY
        child.getBuildKind('android') == SchemaModule.BuildKind.ANDROID_APPLICATION
        parent.getBuildKind('main') == SchemaModule.BuildKind.ANDROID_APPLICATION

        when:
        def childOverride = new SchemaModule('main', X_Jdk.setLinked(), false, false, false)
                .updateBuildKind('android', SchemaModule.BuildKind.JAVA_LIBRARY, 1)

        then:
        child.update(childOverride).update(parent).getBuildKind('android') == SchemaModule.BuildKind.JAVA_LIBRARY
        child.getBuildKind('android') == SchemaModule.BuildKind.ANDROID_APPLICATION
    }

    def 'child qualified override preserves the inherited kind of its sibling'() {
        given:
        prepare()
        write('schema.xapi', """<xapi-schema name="android-fixture" version="1.0"
            platforms=[main, jre, <android modules=[<main buildKind="android-application" />] />]
            modules=[main]
            projects=[
                <app inherit=true multiplatform=true platforms=[
                    <android modules=[<main buildKind="java-library" />] />
                ] />,
                <sibling inherit=true multiplatform=true />
            ]
        />""")
        write('sibling/src/android/java/fixture/Sibling.java', 'package fixture; public class Sibling {}')

        when:
        def result = run(false, 'help')

        then:
        result.output.contains('requires com.android.application in the owning plugins fragment')
        script('android').contains('java-library')
        script('main').contains('java-library')
        new File(fixture, 'sibling/src/android/siblingAndroid.gradle').text.contains('android {')
        !new File(fixture, 'build/xindex/path/_sibling/android/publish').exists()
        new File(fixture, 'build/xindex/path/_app/android/publish').text == 'true'
    }

    def 'unknown build kind fails at the schema boundary'() {
        given:
        prepare('buildKind="android-magic"')
        expect:
        run(false, 'help').output.contains("Unknown buildKind 'android-magic'")
    }

    def 'Android application rejects synthetic Java source publication'() {
        given:
        prepare('buildKind="android-application"', '', 'publishSource=true')
        expect:
        run(false, 'help').output.contains('cannot publish or consume synthetic Java source modules')
    }

    def 'Android dependencies use application configurations and retain classifier closure'() {
        given:
        prepare('buildKind="android-application"', '''requires={
            @transitive external: "fixture:api:1",
            @transitive("impl") external: "fixture:impl:1",
            @transitive("compile_only") external: "fixture:compile:1",
            @transitive("runtime") external: "fixture:runtime:1",
            @transitive("runtime_only") @closure("artifact { classifier = 'natives-arm64-v8a' }") external: "fixture:native:1",
            @transitive("annotation_processor") external: "fixture:processor:1",
            @transitive("test") external: "fixture:test:1"
        }''')
        when:
        run(false, 'help')
        String android = script('android')
        then:
        android =~ /implementation\s+["']fixture:api:1["']/
        android =~ /implementation\s+["']fixture:impl:1["']/
        android =~ /compileOnly\s+["']fixture:compile:1["']/
        android =~ /runtimeOnly\s+["']fixture:runtime:1["']/
        android.contains("classifier = 'natives-arm64-v8a'")
        android =~ /annotationProcessor\s+["']fixture:processor:1["']/
        android =~ /testImplementation\s+["']fixture:test:1["']/
    }

    @Requires({ System.getenv('ANDROID_HOME') && System.getenv('ANDROID_TEST_JAVA_HOME') })
    def 'real AGP app assembles with mapped assets and unit tests and stable regeneration'() {
        given:
        prepare('buildKind="android-application"', 'requires={ @transitive("test") external: "junit:junit:4.13.2" }')
        write('app/src/android/gradle/plugins', "id 'com.android.application' version '8.5.2'\n")
        write('app/src/android/gradle/body.end', '''
android {
    namespace = 'fixture'
    compileSdk = 34
    defaultConfig {
        applicationId = 'fixture.generated.app'
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = '1.0'
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
tasks.register('assertAndroidBackend') {
    doLast {
        assert !plugins.hasPlugin('java')
        assert !plugins.hasPlugin('java-library')
        assert !plugins.hasPlugin('maven-publish')
        assert project.xapiBuildKind == 'android-application'
        assert tasks.findByName('xapiPublish') == null
        assert project.findProject(':app-android-sources') == null
    }
}
''')
        write('app/src/android/AndroidManifest.xml', '''<manifest xmlns:android="http://schemas.android.com/apk/res/android">
<application android:label="@string/app_name"><activity android:name="fixture.MainActivity" android:exported="false" /></application>
</manifest>''')
        write('app/src/android/java/fixture/MainActivity.java', 'package fixture; public class MainActivity extends android.app.Activity {}')
        write('app/src/android/res/values/strings.xml', '<resources><string name="app_name">Generated fixture</string></resources>')
        write('app/src/android/assets/probe.txt', 'asset from qualified module')
        write('app/src/androidTest/resources/unit-probe.txt', 'test resource from qualified module')
        write('app/src/androidTest/java/fixture/LayoutTest.java', '''package fixture;
import org.junit.Test;
import static org.junit.Assert.*;
public class LayoutTest {
    @Test public void generatedTestLayoutIncludesResources() throws Exception {
        assertNotNull(getClass().getResourceAsStream("/unit-probe.txt"));
        assertEquals("fixture.MainActivity", MainActivity.class.getName());
    }
}''')

        when:
        def result = run(true, ':app-android:assembleDebug', ':app-android:testDebugUnitTest', ':app-android:assertAndroidBackend')
        String first = script('android')
        run(true, 'help')
        File apk = new File(fixture, 'app/src/android/build/outputs/apk/debug/app-android-debug.apk')

        then:
        result.task(':app-android:assembleDebug').outcome == TaskOutcome.SUCCESS
        result.task(':app-android:testDebugUnitTest').outcome == TaskOutcome.SUCCESS
        script('android') == first
        apk.isFile()
        new File(fixture, 'app/src/android/build/test-results/testDebugUnitTest/TEST-fixture.LayoutTest.xml').text.contains('failures="0"')
        new ZipFile(apk).withCloseable { zip ->
            assert zip.getEntry('classes.dex') != null
            assert zip.getEntry('AndroidManifest.xml') != null
            assert zip.getEntry('resources.arsc') != null
            assert zip.getInputStream(zip.getEntry('assets/probe.txt')).getText('UTF-8') == 'asset from qualified module'
            true
        }

        cleanup:
        if (apk?.isFile()) {
            File evidence = new File('build/test-evidence/android-application')
            evidence.mkdirs()
            new File(evidence, apk.name).bytes = apk.bytes
            new File(evidence, 'generated.gradle').text = first
            new File(evidence, 'assemble.log').text = result.output
            new File(evidence, 'unit-test-results.xml').bytes = new File(fixture,
                    'app/src/android/build/test-results/testDebugUnitTest/TEST-fixture.LayoutTest.xml').bytes
        }
    }
}
