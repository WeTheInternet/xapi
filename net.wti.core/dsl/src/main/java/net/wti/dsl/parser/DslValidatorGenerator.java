package net.wti.dsl.parser;

import net.wti.lang.parser.JavaParser;
import net.wti.lang.parser.ParseException;
import net.wti.lang.parser.ast.expr.*;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Pointer;
import xapi.fu.log.Log;
import xapi.fu.log.Log.LogLevel;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

///
/// DslValidatorGenerator:
///
/// Consumes a {@link DslModel} rooted at <xapi-dsl>
/// and produces a Java class that validates UiContainerExpr trees
/// against the structure described in that DSL.
///
/// The generated class:
///  - Is a simple final class with:
///      - public static final String ROOT_ELEMENT
///      - private static final Map<String, Set<String>> SCHEMA
///      - public List<String> validate(UiContainerExpr root)
///  - Logs using xapi.log.Log.tryLog at TRACE / INFO / WARN / ERROR as appropriate.
///
/// This implementation uses SourceBuilder / ClassBuffer / MethodBuffer
/// so that imports, indentation, and formatting are centrally managed.
///
/// Created by AI Assistant on request.
///
public final class DslValidatorGenerator {

  public static final String DEFAULT_VALIDATOR_NAME = "GeneratedDslAstValidator";

  public DslValidatorGenerator() {
  }

  ///
  /// High-level convenience: given a filesystem path to a .xapi file containing
  /// an <xapi-dsl> root, parse it, infer package + class name, generate a
  /// validator class, and write it to the supplied output directory.
  ///
  /// The final .java location will be:
  ///
  ///   $outputDir / packageName/segments / SimpleClassName.java
  ///
  /// Example:
  ///
  ///   DslValidatorGenerator gen = new DslValidatorGenerator();
  ///   File javaFile = gen.generateValidatorFile(
  ///       new File("src/test/resources/META-INF.xapi/simple-dsl.xapi"),
  ///       new File("build/generated/sources/xapiDsl")
  ///   );
  ///
  /// A Gradle JavaExec task can use this method to generate validators into a
  /// known directory, which tests can then compile and execute (e.g. via Gradle
  /// TestKit in integration tests).
  ///
  /// @param dslFile   The path to the xapi-dsl .xapi file.
  /// @param outputDir The root directory where .java files should be written.
  /// @return The full path to the generated .java file.
  ///
  public Path generateValidatorFile(Path dslFile, Path outputDir) {
    Objects.requireNonNull(dslFile, "dslFile must not be null");
    Objects.requireNonNull(outputDir, "outputDir must not be null");

    Log.tryLog(DslValidatorGenerator.class, this, LogLevel.INFO,
        "Generating validator for DSL file", dslFile, "into", outputDir);

    final UiContainerExpr root;
    try (InputStream in = Files.newInputStream(dslFile)) {
      root = JavaParser.parseXapi(in, StandardCharsets.UTF_8.name());
    } catch (ParseException e) {
      throw new IllegalArgumentException("Invalid xapi-dsl source in " + dslFile, e);
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read DSL file " + dslFile, e);
    }

    DslModel model = new DslModel(root);
    String packageName = model.getPackageName();
    if (packageName == null) {
      packageName = "";
    }

    String className = deriveValidatorClassName(model);
    String source = generateValidator(model, packageName, className);

    Path pkgDir = packageName.isEmpty()
        ? outputDir
        : outputDir.resolve(packageName.replace('.', '/'));
    try {
      Files.createDirectories(pkgDir);
      Path javaFile = pkgDir.resolve(className + ".java");
      Files.write(javaFile, source.getBytes(StandardCharsets.UTF_8));
      Log.tryLog(DslValidatorGenerator.class, this, LogLevel.INFO,
          "Wrote generated validator to", "file://" + javaFile);
      return javaFile;
    } catch (IOException e) {
      throw new UncheckedIOException("Error writing validator for " + dslFile, e);
    }
  }

  ///
  /// Generate validator source for a given xapi-dsl model.
  ///
  /// @param model       A parsed DslModel, rooted at <xapi-dsl>
  /// @param packageName Java package for the generated class
  /// @param className   Simple class name to generate
  /// @return Java source code as a String
  ///
  public String generateValidator(DslModel model, String packageName, String className) {
    Objects.requireNonNull(model, "model must not be null");
    Objects.requireNonNull(packageName, "packageName must not be null");
    Objects.requireNonNull(className, "className must not be null");

    UiContainerExpr dslRoot = model.getRoot();
    Log.tryLog(DslValidatorGenerator.class, this, LogLevel.INFO,
        "Generating DSL validator", className, "for xapi-dsl:", dslRoot.getName());

    SchemaShape shape = extractShape(dslRoot);

    SourceBuilder<Void> out = new SourceBuilder<>();
    out.setPackage(packageName);
    out.setClassDefinition("public final class " + className + " {", false);

    // imports
    out.addImports(
            UiAttrExpr.class,
            UiContainerExpr.class,
            Log.class,
            LogLevel.class,
            ArrayList.class,
            Collections.class,
            LinkedHashMap.class,
            LinkedHashSet.class,
            List.class,
            Map.class,
            Set.class
    );

    ClassBuffer cls = out.getClassBuffer();

    // class doc
    cls .getJavadoc()
        .println("///")
        .println("/// " + className + ":")
        .println("///")
        .println("/// Generated AST validator for xapi-dsl '" + model.getName() + "'.")
        .println("///")
        .println("/// This class was produced by " + DslValidatorGenerator.class.getName() + ".")
        .println("/// It validates a UiContainerExpr tree according to:")
        .println("///   - root element name: <" + shape.rootElement + ">")
        .println("///   - known elements: " + shape.elements.keySet())
        .println("///   - allowed attributes per element.")
        .println("///");

    // static ROOT_ELEMENT
    cls.createField(String.class, "ROOT_ELEMENT", Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)
        .setInitializer("\"" + shape.rootElement + "\"");

    // static SCHEMA
    cls.createField("Map<String, Set<String>>", "SCHEMA",
            Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);

    // static block to build SCHEMA
    final PrintBuffer header = cls.createStaticBlock();
    header
        .println("Map<String, Set<String>> m = new LinkedHashMap<>();")
        .indent();
    for (Map.Entry<String, Set<String>> e : shape.elements.entrySet()) {
      String elemName = e.getKey();
      Set<String> attrs = e.getValue();
      header
          .println("{")
          .indent()
              .println("Set<String> attrs = new LinkedHashSet<>();")
              .indent();
      for (String attr : attrs) {
        header.println("attrs.add(\"" + attr + "\");");
      }
      header
              .outdent()
              .println("m.put(\"" + elemName + "\", Collections.unmodifiableSet(attrs));")
          .outdent()
          .println("}");
    }
    header
        .println("SCHEMA = Collections.unmodifiableMap(m);")
        .println("Log.tryLog(" + className + ".class, null, LogLevel.INFO,")
        .indent()
            .println("\"Initialized schema for\", ROOT_ELEMENT, \"with elements\", SCHEMA.keySet());")
        .outdent()
        .outdent()
        .println("}")
        .outdent();

    // constructor
    MethodBuffer ctor = cls.createConstructor(Modifier.PUBLIC);
    ctor.getJavadoc()
        .println("///")
        .println("/// Stateless constructor; all schema data lives in SCHEMA.")
        .println("///");
    ctor.println("// nothing to do");

    // validate method
    MethodBuffer validate = cls.createMethod(
        Modifier.PUBLIC,
        "java.util.List<String>",
        "validate",
        "UiContainerExpr root"
    );
    validate.getJavadoc()
        .println("///")
        .println("/// Validate a UiContainerExpr tree as an instance of this DSL.")
        .println("///")
        .println("/// @return A list of human-readable error strings. Empty list means \"valid\".")
        .println("///");
    validate
        .println("List<String> errors = new ArrayList<>();")
        .println("if (root == null) {")
        .indent()
            .println("errors.add(\"Root element is null\");")
            .println("return errors;")
        .outdent()
        .println("}")
        .println("Log.tryLog(" + className + ".class, this, LogLevel.TRACE,")
        .indent()
            .println("\"Validating root element\", root.getName());")
        .outdent()
        .println("validateRoot(root, errors);")
        .println("if (!errors.isEmpty()) {")
        .indent()
            .println("Log.tryLog(" + className + ".class, this, LogLevel.WARN,")
            .indent()
                .println("\"Validation found\", errors.size(), \"error(s):\", errors);")
            .outdent()
        .outdent()
        .println("} else {")
        .indent()
            .println("Log.tryLog(" + className + ".class, this, LogLevel.INFO,")
            .indent()
                .println("\"Validation succeeded for document rooted at <\" + root.getName() + \">\");")
            .outdent()
        .outdent()
        .println("}")
        .println("return errors;");

    // validateRoot method
    MethodBuffer validateRoot = cls.createMethod(
        Modifier.PRIVATE,
        void.class,
        "validateRoot",
        "UiContainerExpr root",
        "List<String> errors"
    );
    validateRoot
        .println("if (!ROOT_ELEMENT.equals(root.getName())) {")
        .indent()
            .println("String msg = \"Root element should be <\" + ROOT_ELEMENT + \"> but was <\" + root.getName() + \">\";")
            .println("errors.add(msg);")
            .println("Log.tryLog(" + className + ".class, this, LogLevel.ERROR, msg);")
        .outdent()
        .println("}")
        .println("validateElementRecursive(root, errors);");

    final String _UiBodyExpr = cls.addImport(UiBodyExpr.class);

    // validateElementRecursive method
    MethodBuffer validateElementRecursive = cls.createMethod(
        Modifier.PRIVATE,
        void.class,
        "validateElementRecursive",
        "UiContainerExpr element",
        "List<String> errors"
    );
    validateElementRecursive
        .println("String name = element.getName();")
        .println("Set<String> allowedAttrs = SCHEMA.get(name);")
        .println("if (allowedAttrs == null) {")
        .indent()
            .println("String msg = \"Unknown element <\" + name + \">\";")
            .println("errors.add(msg);")
            .println("Log.tryLog(" + className + ".class, this, LogLevel.ERROR, msg);")
        .outdent()
        .println("}")
        .println("for (UiAttrExpr attr : element.getAttributes()) {")
        .indent()
            .println("String attrName = attr.getNameString();")
            .println("if (allowedAttrs != null && !allowedAttrs.contains(attrName)) {")
            .indent()
                .println("String msg = \"Element <\" + name + \"> has unknown attribute '\" + attrName + \"'\";")
                .println("errors.add(msg);")
                .println("Log.tryLog(" + className + ".class, this, LogLevel.ERROR, msg);")
            .outdent()
            .println("}")
        .outdent()
        .println("}")
        .append(_UiBodyExpr).println(" body = element.getBody();")
        .println("if (body != null) {")
        .indent()
            .println("body.getChildren().stream()")
            .indent()
                .println(".filter(e -> e instanceof UiContainerExpr)")
                .println(".forEach(child -> validateElementRecursive((UiContainerExpr)child, errors));")
            .outdent()
            .println("}")
        .outdent();
    return out.toSource();
  }


    ///
    /// Compute a simple validator class name from the DslModel.
    ///
    /// Rule of thumb:
    ///  - take model.getName()
    ///  - strip non-identifier characters
    ///  - capitalize the first letter
    ///  - append "AstValidator"
    ///  - fallback to DEFAULT_VALIDATOR_NAME when necessary.
    ///
    private String deriveValidatorClassName(DslModel model) {
        String base = model.getName();
        if (base == null || base.trim().isEmpty()) {
            return DEFAULT_VALIDATOR_NAME;
        }
        base = base.trim();

        StringBuilder simple = new StringBuilder();
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (i == 0) {
                if (Character.isJavaIdentifierStart(c)) {
                    simple.append(c);
                } else if (Character.isJavaIdentifierPart(c)) {
                    simple.append(c);
                }
            } else if (Character.isJavaIdentifierPart(c)) {
                simple.append(c);
            }
        }
        if (simple.length() == 0) {
            return DEFAULT_VALIDATOR_NAME;
        }
        char first = simple.charAt(0);
        if (Character.isLowerCase(first)) {
            simple.setCharAt(0, Character.toUpperCase(first));
        }
        simple.append("AstValidator");
        return simple.toString();
    }

  ///
  /// Internal, minimal shape extracted from xapi-dsl:
  ///  - root element name
  ///  - element name -> set of attribute names
  ///
  private SchemaShape extractShape(UiContainerExpr dslRoot) {
    UiAttrExpr elementsAttr = dslRoot.getAttribute("elements")
        .ifAbsentThrow(() ->
            new IllegalArgumentException("xapi-dsl root is missing elements= attribute"))
        .get();

    // Optional top-level configuration of the root element name.
    // If present, we require there to be an element-def with a matching name.
    String explicitRootTag = dslRoot.getAttribute("rootTag")
        .mapNullSafe(attr -> attr.getString(false, true))
        .getOrNull();

    Expression expr = elementsAttr.getExpression();
    if (!(expr instanceof JsonContainerExpr)) {
      throw new IllegalArgumentException(
          "xapi-dsl elements= must be a json array of <element-def>; got: " + expr.getClass().getSimpleName());
    }

    JsonContainerExpr json = (JsonContainerExpr) expr;
    if (!json.isArray()) {
      throw new IllegalArgumentException("xapi-dsl elements= must be a json array");
    }

    Map<String, Set<String>> elements = new LinkedHashMap<>();
    final Pointer<String> rootName = Pointer.pointerTo(null);

    json.getValues()
        .filterInstanceOf(UiContainerExpr.class)
        .forAll(def -> {
          if (!"element-def".equals(def.getName())) {
            Log.tryLog(DslValidatorGenerator.class, this, LogLevel.INFO,
                "Ignoring non element-def entry in elements= array:", def.toSource());
            return;
          }

          UiAttrExpr nameAttr = def.getAttribute("name")
              .ifAbsentThrow(() -> new IllegalArgumentException(
                  "element-def missing name= attribute: " + def.toSource()))
              .get();
          String elementName = nameAttr.getString(false, true);

          UiAttrExpr attrsAttr = def.getAttribute("attributes")
              .ifAbsentThrow(() -> new IllegalArgumentException(
                  "element-def " + elementName + " missing attributes= map"))
              .get();

          Set<String> allowedAttrs = new LinkedHashSet<>();
          Expression attrsExpr = attrsAttr.getExpression();
          if (attrsExpr instanceof JsonContainerExpr) {
            JsonContainerExpr attrsJson = (JsonContainerExpr) attrsExpr;
            if (!attrsJson.isArray()) {
              for (JsonPairExpr pair : attrsJson.getPairs()) {
                allowedAttrs.add(pair.getKeyString());
              }
            } else {
              Log.tryLog(DslValidatorGenerator.class, this, LogLevel.WARN,
                  "Attributes for element-def", elementName,
                  "are an array, expected map; treating as empty");
            }
          } else {
            Log.tryLog(DslValidatorGenerator.class, this, LogLevel.WARN,
                "Attributes for element-def", elementName,
                "are not json; got:", attrsExpr.getClass().getSimpleName());
          }

          elements.put(elementName, allowedAttrs);
          Log.tryLog(DslValidatorGenerator.class, this, LogLevel.TRACE,
              "Discovered element-def", elementName, "attrs:", allowedAttrs);

          // Determine root element:
          //  - If rootTag is set, we pick the element-def whose name matches rootTag.
          //  - Else, we fall back to legacy behavior: element-def named "root".
          if (rootName.out1() == null) {
            if (explicitRootTag != null && explicitRootTag.equals(elementName)) {
              rootName.in(elementName);
            } else if (explicitRootTag == null && "root".equals(elementName)) {
              rootName.in(elementName);
            }
          }
        });

    if (rootName.out1() == null) {
      if (explicitRootTag != null) {
        throw new IllegalArgumentException(
            "xapi-dsl rootTag='" + explicitRootTag + "' does not match any element-def; have: " + elements.keySet());
      }
      throw new IllegalArgumentException(
          "xapi-dsl did not define a rootTag attribute or an element-def named 'root'; have: " + elements.keySet());
    }

    return new SchemaShape(rootName.out1(), elements);
  }

  private static final class SchemaShape {
    final String rootElement;
    final Map<String, Set<String>> elements;

    SchemaShape(String rootElement, Map<String, Set<String>> elements) {
      this.rootElement = rootElement;
      this.elements = elements;
    }
  }
}
