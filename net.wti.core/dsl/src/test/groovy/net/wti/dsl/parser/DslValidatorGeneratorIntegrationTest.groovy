package net.wti.dsl.parser

import net.wti.lang.parser.JavaParser
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir
import xapi.fu.log.Log

import java.nio.file.Files
import java.nio.file.Path

///
/// DslValidatorGeneratorIntegrationTest:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 14/12/2025 @ 03:13
class DslValidatorGeneratorIntegrationTest extends Specification {

    @TempDir(cleanup = TempDir.CleanupMode.ON_SUCCESS)
    File testProjectDir

    ///
    /// Resolve the jar or classes directory that contains the given type
    /// on the current test classpath, with backslashes escaped for Gradle.
    ///
    private static String classpathOf(Class<?> type) {
        final URL location = type.protectionDomain.codeSource.location
        final File file = new File(location.toURI())
        return file.absolutePath.replace("\\", "\\\\")
    }

    ///
    /// Copy a classpath resource into the given target file.
    ///
    private static void copyResource(String resourcePath, File target) {
        target.parentFile.mkdirs()
        target.text = DslValidatorGeneratorIntegrationTest
                .getResourceAsStream(resourcePath)
                .text
    }

    ///
    /// Write the build.gradle file for the temp project.
    ///
    ///  - testPackage / testClassName form the JavaExec mainClass.
    ///  - resourcesDir / instanceFile are used to compute the argument path.
    ///  - dependencyTypes are turned into implementation files("...") entries.
    ///
    private static void writeBuildFile(
            File buildFile,
            String testPackage,
            String testClassName,
            File resourcesDir,
            File instanceFile,
            Class<?>... dependencyTypes
    ) {
        final String deps = dependencyTypes.collect {
            "        implementation files(\"${classpathOf(it)}\")"
        }.join("\n")

        final String relInstancePath = resourcesDir.toPath()
                .relativize(instanceFile.toPath())
                .toString()
                .replace("\\", "/")

        buildFile.text = """
      plugins {
        id 'java'
      }

      repositories {
        mavenCentral()
      }

      dependencies {
${deps}
      }

      // Add the generated validator sources to main source set
      sourceSets {
        main {
          java {
            srcDir file("build/generated/sources/xapiDsl")
          }
          resources {
            srcDirs = [ file("src/main/resources") ]
          }
        }
      }

      // Task to run the generated validator on the given .xapi instance
      tasks.register('runDslValidator', JavaExec) {
        group = 'verification'
        description = 'Run generated DSL validator against an .xapi file'

        mainClass = '${testPackage}.${testClassName}'

        classpath = sourceSets.main.runtimeClasspath
        args = [ "\${projectDir}/src/main/resources/${relInstancePath}" ]
      }
    """.stripIndent()
    }

    ///
    /// Write the Java bridge class which:
    ///  - parses the .xapi file
    ///  - invokes the generated validator
    ///  - logs and exits non‑zero on errors
    ///
    /// The validator lives in a different package than the test main.
    ///
    private static void writeBridge(
            File srcDir,
            String testPackage,
            String testClassName,
            String validatorPackage,
            String validatorClassName
    ) {
        final File testPackageDir = new File(srcDir, testPackage.replaceAll("[.]", File.separator))
        testPackageDir.mkdirs()
        final File bridge = new File(testPackageDir, "${testClassName}.java")

        bridge.text = """
      package ${testPackage};

      import ${validatorPackage}.${validatorClassName};
      import net.wti.lang.parser.ast.expr.UiContainerExpr;
      import net.wti.lang.parser.JavaParser;
      import xapi.fu.log.Log;
      import xapi.fu.log.Log.LogLevel;

      import java.io.FileInputStream;
      import java.io.IOException;
      import java.util.List;

      /// Simple bridge to run the generated validator from JavaExec.
      public class ${testClassName} {

        public static void main(String[] args) throws Exception {
          if (args.length != 1) {
            System.err.println("Usage: ${testClassName} <path-to-xapi>");
            System.exit(1);
          }
          String path = args[0];
          UiContainerExpr root;
          try (FileInputStream in = new FileInputStream(path)) {
            root = JavaParser.parseXapi(in, "UTF-8");
          }
          ${validatorClassName} validator = new ${validatorClassName}();
          List<String> errors = validator.validate(root);
          if (!errors.isEmpty()) {
            Log.tryLog(${testClassName}.class, null, LogLevel.ERROR,
                "Validation errors for ", path, ":", errors);
            System.err.println("Validation failed: " + errors);
            System.exit(2);
          }
          System.out.println("Validation OK for " + path);
        }
      }
    """.stripIndent()
    }

    ///
    /// Integration-style test using Gradle TestKit:
    ///
    ///  a) Use DslValidatorGenerator to emit a validator .java file into a Gradle project
    ///  b) Compile and run that validator via a custom Gradle task
    ///  c) Have the task validate simple-valid.xapi and fail the build on errors
    ///
    def "generated validator compiles and validates simple-valid.xapi via Gradle"() {
        given: "a temporary Gradle project"
        File buildFile = new File(testProjectDir, "build.gradle")
        File srcDir = new File(testProjectDir, "src/main/java")
        File resourcesDir = new File(testProjectDir, "src/main/resources")
        File genSrcDir = new File(testProjectDir, "build/generated/sources/xapiDsl")

        srcDir.mkdirs()
        resourcesDir.mkdirs()
        genSrcDir.mkdirs()

        // Copy our test DSL + instance into the temp project
        File simpleDsl = new File(resourcesDir, "META-INF/xapi/simple-dsl.xapi")
        File simpleValid = new File(resourcesDir, "net.wti.dsl.parser/simple-valid.xapi")
        copyResource("/META-INF/xapi/simple-dsl.xapi", simpleDsl)
        copyResource("/net/wti/dsl/parser/simple-valid.xapi", simpleValid)

        when: "we generate a validator java file into build/generated/sources/xapiDsl"
        DslValidatorGenerator gen = new DslValidatorGenerator()
        File dslFile = simpleDsl
        File outputDir = genSrcDir
        Path javaFile = gen.generateValidatorFile(dslFile.toPath(), outputDir.toPath())
        final String javaFileName = javaFile.getFileName().toString()

        then: "validator source file was written"
        Files.exists(javaFile) && Files.isRegularFile(javaFile)

        final String testPackage = 'net.wti.test'
        final String testClassName = javaFileName.replace(".java", "TestMain")
        final String validatorClassName = javaFileName.replace(".java", "")
        // TODO: adjust this to the real package your generator uses
        final String validatorPackage = 'net.wti.simple'

        when: "we compile and run the runDslValidator task via Gradle TestKit"
        writeBuildFile(
                buildFile,
                testPackage,
                testClassName,
                resourcesDir,
                simpleValid,
                Log,
                JavaParser
        )
        writeBridge(srcDir, testPackage, testClassName, validatorPackage, validatorClassName)

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("runDslValidator", "--info")
                .build()

        then: "the build succeeds and validation reports OK"
        result.output.contains("Validation OK")
    }

    ///
    /// More complex integration-style test using Gradle TestKit:
    ///
    ///  a) Use DslValidatorGenerator to emit a validator .java file into a Gradle project
    ///  b) Compile and run that validator via a custom Gradle task
    ///  c) Have the task validate test-valid.xapi (from test-dsl.xapi) and fail the build on errors
    ///
    def "generated validator compiles and validates test-dsl.xapi via Gradle"() {
        given: "a temporary Gradle project for the complex DSL"
        File buildFile = new File(testProjectDir, "build.gradle")
        File srcDir = new File(testProjectDir, "src/main/java")
        File resourcesDir = new File(testProjectDir, "src/main/resources")
        File genSrcDir = new File(testProjectDir, "build/generated/sources/xapiDsl")

        srcDir.mkdirs()
        resourcesDir.mkdirs()
        genSrcDir.mkdirs()

        // Copy our complex test DSL + instance into the temp project
        File testDsl = new File(resourcesDir, "META-INF/xapi/test-dsl.xapi")
        File testValid = new File(resourcesDir, "net.wti.dsl.parser/test-valid.xapi")
        copyResource("/META-INF/xapi/test-dsl.xapi", testDsl)
        copyResource("/net/wti/dsl/parser/test-valid.xapi", testValid)

        when: "we generate a validator java file into build/generated/sources/xapiDsl"
        DslValidatorGenerator gen = new DslValidatorGenerator()
        File dslFile = testDsl
        final DslModel dslModel = new DslModel(JavaParser.parseXapi(new FileInputStream(dslFile)))
        File outputDir = genSrcDir
        Path javaFile = gen.generateValidatorFile(dslFile.toPath(), outputDir.toPath())
        final String javaFileName = javaFile.getFileName().toString()

        then: "validator source file was written"
        Files.exists(javaFile) && Files.isRegularFile(javaFile)

        final String testPackage = 'net.wti.test'
        final String testClassName = javaFileName.replace(".java", "TestMain")
        final String validatorClassName = javaFileName.replace(".java", "")
        final String validatorPackage = dslModel.packageName

        when: "we compile and run the runDslValidator task via Gradle TestKit"
        writeBuildFile(
                buildFile,
                testPackage,
                testClassName,
                resourcesDir,
                testValid,
                Log,
                JavaParser
        )
        writeBridge(srcDir, testPackage, testClassName, validatorPackage, validatorClassName)

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("runDslValidator", "--info")
                .build()

        then: "the build succeeds and validation reports OK for the complex DSL"
        result.output.contains("Validation OK")
    }
}