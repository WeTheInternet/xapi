package net.wti.gradle.settings.plugin;

import net.wti.gradle.settings.api.Transitivity;
import org.gradle.api.GradleException;
import xapi.dev.source.BuildScriptBuffer;

import java.io.File;

/**
 * Generates the Android application layout for one qualified schema module.
 * The owning plugins fragment selects AGP and its version; SDK and application
 * settings belong in an owning body fragment. This renderer adds no dependencies.
 */
final class AndroidApplicationBuildScript {

    private AndroidApplicationBuildScript() {}

    static void validateSourceLayout(final File main, final File test) {
        for (final File root : new File[]{main, test}) {
            for (final String language : new String[]{"groovy", "kotlin"}) {
                if (new File(root, language).isDirectory()) {
                    throw new GradleException("android-application currently supports Java sources only: "
                            + new File(root, language));
                }
            }
        }
    }

    static void render(final BuildScriptBuffer out, final String main, final String test) {
        out.println("if (!plugins.hasPlugin('com.android.application')) {");
        out.indent().println("throw new GradleException('buildKind android-application requires com.android.application in the owning plugins fragment')");
        out.outdent().println("}");
        // AGP resolves build tools (including aapt2) through project repositories.
        // A google() entry in pluginManagement alone does not supply those tools.
        out.println("repositories.google()");
        out.println("repositories.mavenCentral()");
        out.println("android {").indent();
        out.println("sourceSets {").indent();
        out.println("main {").indent();
        out.println("manifest.srcFile(" + path(main, "AndroidManifest.xml") + ")");
        source(out, "java", main, "java");
        source(out, "resources", main, "resources");
        source(out, "res", main, "res");
        source(out, "assets", main, "assets");
        source(out, "jniLibs", main, "jniLibs");
        out.outdent().println("}");
        // Retain XApi's <qualified-key>Test convention for local unit tests.
        out.println("test {").indent();
        source(out, "java", test, "java");
        source(out, "resources", test, "resources");
        out.outdent().println("}");
        out.outdent().println("}");
        out.outdent().println("}");
    }

    private static void source(final BuildScriptBuffer out, final String kind, final String root, final String directory) {
        out.println(kind + ".setSrcDirs([" + path(root, directory) + "])");
    }

    private static String path(final String root, final String child) {
        return "\"" + (root + "/" + child).replace("\\", "/").replace("\"", "\\\"") + "\"";
    }

    static String configurationFor(final Transitivity transitivity) {
        switch (transitivity) {
            case api:
            case impl:
                return "implementation";
            case compile_only:
                return "compileOnly";
            case runtime:
            case runtime_only:
                return "runtimeOnly";
            case annotation_processor:
                return "annotationProcessor";
            case test:
                return "testImplementation";
            default:
                throw new IllegalArgumentException("transitivity " + transitivity + " is not supported for android-application");
        }
    }
}
