package net.wti.dsl.parser

import net.wti.lang.parser.JavaParser;
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

    @TempDir(cleanup=TempDir.CleanupMode.ON_SUCCESS)
    File testProjectDir

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
        simpleDsl.parentFile.mkdirs()
        simpleValid.parentFile.mkdirs()
        simpleDsl.text = DslValidatorGeneratorIntegrationTest.getResourceAsStream("/META-INF/xapi/simple-dsl.xapi").text
        simpleValid.text = DslValidatorGeneratorIntegrationTest.getResourceAsStream("/net/wti/dsl/parser/simple-valid.xapi").text

        when: "we generate a validator java file into build/generated/sources/xapiDsl"
        DslValidatorGenerator gen = new DslValidatorGenerator()
        File dslFile = simpleDsl
        File outputDir = genSrcDir
        Path javaFile = gen.generateValidatorFile(dslFile.toPath(), outputDir.toPath())
        final String javaFileName = javaFile.getFileName().toString()
        final URL javaParserLocation = JavaParser.class.protectionDomain.codeSource.location
        File javaParserFile = new File(javaParserLocation.toURI())
        String javaParserPath = javaParserFile.absolutePath.replace("\\", "\\\\") // escape for Gradle string

        final URL logApiLocation = Log.class.protectionDomain.codeSource.location
        File logApiFile = new File(logApiLocation.toURI())
        String logApiPath = logApiFile.absolutePath.replace("\\", "\\\\") // escape for Gradle string

        then: "validator source file was written"
        Files.exists(javaFile) && Files.isRegularFile(javaFile)
        final String testPackage = 'net.wti.test'
        final String testClassName = javaFileName.replace(".java","TestMain")

        when: "we compile and run the runDslValidator task via Gradle TestKit"
        // Minimal build script:
        buildFile.text = """
      plugins {
        id 'java'
      }

      repositories {
        mavenCentral()
      }

      dependencies {
        implementation files("${logApiPath}")
        implementation files("${javaParserPath}")
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

      // Task to run the generated validator on simple-valid.xapi
      tasks.register('runDslValidator', JavaExec) {
        group = 'verification'
        description = 'Run generated DSL validator against simple-valid.xapi'

        // Assume default package (no package) for validator
        mainClass = '${testPackage}.${testClassName}'

        classpath = sourceSets.main.runtimeClasspath
        args = [ "\${projectDir}/src/main/resources/net.wti.dsl.parser/simple-valid.xapi" ]
      }
    """.stripIndent()

        // The generated validator has a public List<String> validate(UiContainerExpr root)
        // For this integration test we'll just add a simple bridge class that:
        //  - parses simple-valid.xapi as UiContainerExpr
        //  - invokes validate()
        //  - prints errors (and exits with non-zero if any)
        final String validatorClassName = javaFileName.replace(".java","")
        final File testPackageDir = new File(srcDir, testPackage.replaceAll("[.]", File.separator))
        testPackageDir.mkdirs()
        final File bridge = new File(testPackageDir, "${testClassName}.java")
        bridge.text = """
      package net.wti.test;

      import net.wti.simple.SimpledslAstValidator;
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
          ${validatorClassName} validator = new ${javaFileName.replace(".java","")}();
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

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("runDslValidator", "--info")
                .build()

        then: "the build succeeds and validation reports OK"
        result.output.contains("Validation OK")
    }
}
