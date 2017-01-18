package xapi.test.components.bdd;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import cucumber.api.java.Before;
import cucumber.api.java.en.And;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import org.junit.ComparisonFailure;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.components.api.IsWebComponent;
import xapi.components.api.WebComponentFactory;
import xapi.dev.X_Gwtc;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.gen.FileBasedSourceHelper;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.gwtc.impl.GwtcManifestImpl;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.SourceBuilder.JavaType;
import xapi.dev.ui.ComponentBuffer;
import xapi.dev.ui.GeneratedUiComponent.GeneratedUiApi;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiGeneratorService;
import xapi.dev.ui.UiGeneratorServiceDefault;
import xapi.dev.ui.UiGeneratorVisitor;
import xapi.dev.ui.UiTagGenerator;
import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.fu.Out2;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.GwtManifest.CleanupMode;
import xapi.gwtc.api.GwtcXmlBuilder;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.javac.dev.api.CompilerService;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.source.X_Source;
import xapi.source.read.JavaModel.IsQualified;
import xapi.test.components.client.GeneratedComponentEntryPoint;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;
import xapi.util.X_String;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static xapi.collect.X_Collect.toArray;
import static xapi.fu.X_Fu.notEmpty;
import static xapi.util.X_String.join;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.TreeLogger.Type;

import javax.lang.model.element.Element;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/14/16.
 */
public class GwtcSteps {

  private static final String QUOTED = "\"([^\"]+)\"";

  static {
    X_Log.logLevel(LogLevel.INFO);
    X_Properties.setProperty(X_Namespace.PROPERTY_LOG_LEVEL, "ALL");
    X_Properties.setProperty(X_Namespace.PROPERTY_MULTITHREADED, "10");
  }

  private StringTo<CompiledComponent> compiledComponents;
  private StringTo<ComponentBuffer> generatedComponents;
  private StringTo<String> sources;
  private Lazy<GwtcService> gwtc;
  private Lazy<UiGeneratorService> tools;
  private Lazy<GwtManifest> gwtManifest = Lazy.deferred1(this::createManifest);

  protected GwtManifest createManifest() {
      final GwtcService service = gwtc.out1();
      service.addClasspath(IsWebComponent.class);
      service.addClasspath(GeneratedComponentEntryPoint.class);
      service.createFile("META-INF", "xapi.properties", ()->"");
      final GwtManifest manifest = new GwtcManifestImpl(service.getModuleName());
      initializeManifest(service, manifest);
      return manifest;
  }
  protected <Ctx extends ApiGeneratorContext<Ctx>> UiGeneratorService<Element> createUiGen() {
    return new UiGeneratorServiceDefault<Ctx>() {
      @Override
      protected Iterable<Out2<String, UiComponentGenerator>> getComponentGenerators() {
        return Chain.<Out2<String, UiComponentGenerator>>startChain().addAll(super.getComponentGenerators())
                    .add(Out2.out2Immutable("define-tags", new UiTagGenerator()))
                    .add(Out2.out2Immutable("define-tag", new UiTagGenerator()))
                    .build();
      }
    };
  }

  @Before
  public void before() {
    compiledComponents = X_Collect.newStringMap(CompiledComponent.class);
    generatedComponents = X_Collect.newStringMap(ComponentBuffer.class);
    sources = X_Collect.newStringMap(String.class);
    resetGwtCompiler();
    resetApiGenerator();
  }

  protected void resetApiGenerator() {
    tools = Lazy.deferred1(this::createUiGen);
  }


  protected void resetGwtCompiler() {
    gwtc = Lazy.deferred1(X_Gwtc::getServiceForClass, GeneratedComponentEntryPoint.class);
    gwtManifest = Lazy.deferred1(this::createManifest);
  }

  @Given("^reset compilation:$")
  public void resetCompilation() {
    resetGwtCompiler();
  }
  @Given("^compile the code:$")
  public void compileTheCode(List<String> lines) {
    final Optional<String> first = lines.stream().filter(notEmpty()).findFirst();
    if (!first.isPresent()) {
      throw new AssertionError("No text supplied to compile method.  You sent " + lines);
    }
    CompilerService compiler = X_Inject.singleton(CompilerService.class);

  }

  @Given("^add java component:$")
  public void addJavaComponent(List<String> lines) throws ParseException {
    checkNotEmpty(lines);

    GwtcService service = gwtc.out1();
    final GwtManifest manifest = gwtManifest.out1();

    String code = join("\n", toArray(lines));
    JavaType type = JavaType.UNKNOWN;
    String pkgToUse;
    String clsToUse;
    final CompilationUnit parsed = JavaParser.parse(X_IO.toStreamUtf8(code), "UTF-8");
    type = parsed.getPrimaryType().getJavaType();
    final int ident = System.identityHashCode(manifest);
    final String fileName = "Gen" + ident;
    pkgToUse = parsed.getPackage() == null ? null : parsed.getPackage().getPackageName();
    if (pkgToUse == null) {
      pkgToUse = service.modifyPackage("xapi.test.pkg" + ident);
      parsed.setPackage(new PackageDeclaration(new NameExpr(pkgToUse)));
    }
    final GwtcXmlBuilder builder = manifest.getOrCreateBuilder(pkgToUse, fileName);
    builder.addSource("");
    service.addGwtInherit(builder.getInheritName());

    for (String auto : autoImport()) {
      parsed.getImports().add(new ImportDeclaration(new NameExpr(auto), false, true));
    }
    clsToUse = parsed.getPrimaryType().getName();


    // lets save the code, then generate an inclusion for it.
    // Note, we are only recording the provider for the source;
    // we still have 'til the end of the compile to actually finish generating code.
    service.createFile(pkgToUse.replace('.', '/'), clsToUse + ".java", parsed::toSource);

    final MethodBuffer method = service.addMethodToEntryPoint("public void runMethod" + ident);
    service.getOnModuleLoad()
        .println(method.getName() + "();");

    parsed.getTypes().forEach(t -> {
      if (t instanceof ClassOrInterfaceDeclaration) {
        final ClassOrInterfaceDeclaration cls = (ClassOrInterfaceDeclaration) t;
        In1<ClassOrInterfaceDeclaration> printComponent = coi -> {
          String print = coi.getName();
          String qualifiedName = null;
          boolean hadName = false;
          if (print.indexOf('.') == -1) {
            // check the imports for the correct package
            for (ImportDeclaration importDecl : parsed.getImports()) {
              if (importDecl.getName().getName().endsWith(print)) {
                qualifiedName = importDecl.getName().getName();
                hadName = true;
                break;
              }
            }
            if (!hadName) {
              qualifiedName = X_Source.qualifiedName(parsed.getPackage().getPackageName(), coi.getName());
            }
          } else {
            qualifiedName = coi.getName();
          }
          print = method.addImport(qualifiedName);
          String gwt = method.addImport(GWT.class);
          final String factory = method.addImport(WebComponentFactory.class);
          method.print(factory + "<" + print + "> factory = ");
          method.println(gwt + ".create(" + print + ".class);");
          method.println("factory.newComponent();");

          final CompiledComponent component = new CompiledComponent(cls, parsed, service, manifest);
          String tagName = getTagName(cls);
          compiledComponents.put(qualifiedName, component);
          compiledComponents.put(tagName, component);
        };
        if (cls.isInterface()) {
          for (ClassOrInterfaceType iface : cls.getExtends()) {
            if (iface.getName().endsWith(IsWebComponent.class.getSimpleName())) {
              printComponent.in(cls);
            }
          }
        } else {
          for (ClassOrInterfaceType iface : cls.getImplements()) {
            if (iface.getName().endsWith(IsWebComponent.class.getSimpleName())) {
              printComponent.in(cls);
            }

          }

        }
      } else if (t instanceof AnnotationDeclaration) {
        X_Log.warn(
            getClass(),
            "Saw a generated annotation, but we aren't running javac yet; this annotation won't be used in the Gwt compile"
        );
      } else if (t instanceof EnumDeclaration) {
        X_Log.warn(
            getClass(),
            "Saw a generated enum, but we aren't running javac yet; this enum won't be used in the Gwt compile"
        );
      } else {
        throw new AssertionError("Unsupported type " + t.getClass() + " : " + t);
      }

    });
  }

  @Given("^add xapi component named (.+):$")
  public ComponentBuffer addXapiComponent(String name, List<String> lines) throws ParseException {
    checkNotEmpty(lines);

    GwtcService service = gwtc.out1();
    final GwtManifest manifest = gwtManifest.out1();

    if (name == null) {
      name = "Gen"+System.identityHashCode(manifest);
    } else {
      name = X_Source.toCamelCase(name);
    }

    String code = join("\n", toArray(lines));
    String clsToUse;
    final UiContainerExpr parsed = JavaParser.parseXapi(X_IO.toStreamUtf8(code), "UTF-8");
    final String pkgToUse =
        ASTHelper.extractStringValue(
        parsed.getAttribute("package").ifAbsentReturn(UiAttrExpr.of(
            "package",
            "xapi.test.pkg"
        )).getExpression());
    final UiGeneratorService generator = this.tools.out1();
    final IsQualified type = new IsQualified(pkgToUse, name);
    final ComponentBuffer buffer = generator.initialize(new FileBasedSourceHelper(manifest::getGenDir), type, parsed);

    generatedComponents.put(name, buffer);

    final GwtcXmlBuilder builder = manifest.getOrCreateBuilder(pkgToUse, name);
    builder.addSource("");
    service.addGwtInherit(builder.getInheritName());

    final SourceBuilder<?> out = buffer.getBinder();
    autoImport().forEach(out::addImport);

    final ApiGeneratorContext<?> ctx = new ApiGeneratorContext<>();
    buffer.getRoot().setContext(ctx);
    final UiGeneratorVisitor visitor = generator.createVisitor(buffer.getRoot(), buffer);
    visitor.visit(parsed, generator.tools());

    return buffer;
  }

  @Given("^compile the component:$")
  public void compileTheComponent(List<String> lines) throws ParseException {
    boolean isXapi = checkNotEmpty(lines)
      .get().startsWith("<");

    if (isXapi) {
      addXapiComponent("Gen" + System.identityHashCode(lines), lines);
    } else {
      addJavaComponent(lines);
    }

    compileGwt();

  }

  @Given("^compile gwt$")
  public void compileGwt() throws ParseException {

    final GwtcService service = gwtc.out1();
    final GwtManifest manifest = gwtManifest.out1();

    final int result = service.compile(manifest);
    assertEquals("Compile failed w/ code " + result, 0, result);

  }

  private Optional<String> checkNotEmpty(List<String> lines) {
    final Optional<String> first = lines.stream().filter(notEmpty()).findFirst();
    if (!first.isPresent()) {
      throw new AssertionError("No text supplied to compile method.  You sent " + lines);
    }
    return first;
  }

  private ChainBuilder<String> autoImport() {
    return Chain.<String>startChain()
        .add("xapi.components.api")
        .add("xapi.ui.api");
  }

  private void initializeManifest(GwtcService service, GwtManifest manifest) {
    manifest.addSystemProp("gwt.usearchives=false");
    manifest.setDisableUnitCache(true);
    manifest.setLogLevel(Type.TRACE);
    manifest.setWorkDir(service.getTempDir().getAbsolutePath());
    manifest.setGenDir(manifest.getGenDir()); // make the default explicit, so the argument is sent to command line
    manifest.setStrict(true); // break on any error
    manifest.setCleanupMode(CleanupMode.DELETE_ON_SUCCESSFUL_EXIT);
//    manifest.setCleanupMode(CleanupMode.NEVER_DELETE);
    manifest.setUseCurrentJvm(true);
  }

  private String getTagName(ClassOrInterfaceDeclaration cls) {
    for (AnnotationExpr anno : cls.getAnnotations()) {
      if (anno.getName().getSimpleName().equals("WebComponent")) {
        if (anno instanceof NormalAnnotationExpr) {
          for (MemberValuePair pair : ((NormalAnnotationExpr) anno).getPairs()) {
            if (pair.getName().equals("tagName")) {
              String val = ASTHelper.extractAnnoValue(pair);
              if (val != null) {
                return val;
              } else {
                X_Log.error(
                    getClass(),
                    "Type of annotation value not supported: " + pair.getValue().getClass() + " : " + pair.toSource()
                );
                throw new AssertionError("Must use string literals for tagName values of @WebComponent attributes");
              }
            }
          }

        }
      }
    }
    throw new AssertionError("Unable to find the generated tag name from component " + cls.toSource());
  }

  @And("^save generated source of component " + QUOTED + " as " + QUOTED + "$")
  public void saveGeneratedSourceOfComponentAs(String componentName, String targetName) throws Throwable {
    final CompiledComponent component = compiledComponents.get(componentName);
    if (component == null) {
      throw new NullPointerException();
    }

    File location = component.getWebComponentFactoryFile();
    String fileContents;
    try (FileInputStream in = new FileInputStream(location)) {
      fileContents = X_IO.toStringUtf8(in);
    }
    sources.put(targetName, fileContents);
  }

  @And("^save generated gwt source file " + QUOTED + " as " + QUOTED + "$")
  public void saveGeneratedGwtSourceFileAs(String fileName, String targetName) throws Throwable {
    final CompiledDirectory compiled = gwtManifest.out1().getCompileDirectory();
    if (compiled == null) {
      throw new AssertionError("Gwt compile failed or not yet run.");
    }

    final Path genDir = Paths.get(compiled.getGenDir());
    final String cleaned = fileName.contains(".") ? fileName.contains("/") || fileName.contains(".java") ?
        fileName : fileName.replace('.', '/') + ".java" : fileName + ".java";
    final Path file = genDir.resolve(cleaned);
    if (!Files.exists(file)) {
      throw new AssertionError("No file exists for " + file +" " +
          "-- requested: " + fileName);
    }
    final String fileContents = X_IO.toStringUtf8(Files.newInputStream(file));
    sources.put(targetName, fileContents);
  }

  @Then("^confirm source \"([^\"]*)\" matches:$")
  public void confirmSourceMatches(String componentName, List<String> lines) throws Throwable {
    final String source = sources.get(componentName);
    assertNotNull(source);
    fuzzyEquals(source, lines);
  }

  public void fuzzyEquals(String source, List<String> expected) throws Throwable {
    // cucumber gives us lists, so lists it is!
    List<SourceMatches> matches = new ArrayList<>();

    expected.stream()
        .filter(line->!line.trim().isEmpty())
        .forEach(line ->  {
          for (String subLine : line.split("\n|\\\\n")) {
            if (!subLine.trim().isEmpty()) {
              matches.add(new SourceMatches(matches.size(), subLine));
            }
          }
        });


    int lineNum = 0;
    for (String line : source.split("\n|\\\\n")) {
      if (!line.trim().isEmpty()) {
        if (lineNum >= matches.size()) {
          throw new ComparisonFailure("Different size of lines; failing early; "
              + "Expected: " + matches.size() + "; got " + expected.size() + "."
              , "\n" + X_String.join("\n", matches), "\n" +source);
        }
        final SourceMatches match = matches.get(lineNum);
        match.actualLine = lineNum++;
        match.actual = line;
      }
    }
    final Iterator<SourceMatches> itr = matches.iterator();
    while (itr.hasNext()) {
      SourceMatches match = itr.next();
      if (match.actual == null ||
          !match.expected.replaceAll("\\s+", "")
          .equals(
            match.actual.replaceAll("\\s+", "")
          )
          ) {
        StringBuilder restExpected = new StringBuilder();
        StringBuilder restActual = new StringBuilder();

        while (true) {
          restExpected.append(match.expectedLine).append('\t').append(match.expected).append("\n");
          restActual.append(match.actualLine).append('\t').append(match.actual).append("\n");
          if (!itr.hasNext()) {
            break;
          }
          match = itr.next();
        }
        throw new ComparisonFailure(
            "Expected source failed on line " + match.expectedLine + ": \n" +
            match.expected + "\n" +
            "Action source match @ line " + match.actualLine +  "\n" +
            match.actual + "\n" +
            "Rest of expected source:\n" +
                restExpected + "\n" +
            "Rest of actual source:\n" +
                restActual + "\n"
            +" Full actual source: \n" + source
            , match.expected, match.actual);
      }
    }
  }

  @Then("^confirm api source for \"([^\"]*)\" matches:$")
  public void confirmApiSourceForMatches(String source, List<String> expected) throws Throwable {
    final ComponentBuffer component = generatedComponents.getOrReturn(source, () -> {
      throw new IllegalStateException("No component for " + source + " among: " + generatedComponents.keys());
    });
    final GeneratedUiApi api = component.getGeneratedComponent().getApi();
    String actual = api.getSource().toSource();
    fuzzyEquals(actual, expected);
  }
}
