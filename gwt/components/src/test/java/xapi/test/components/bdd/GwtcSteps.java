package xapi.test.components.bdd;

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
import com.github.javaparser.ast.expr.StringLiteralExpr;
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
import xapi.dev.components.WebComponentFactoryGenerator;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder.JavaType;
import xapi.fu.In1;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.GwtManifest.CleanupMode;
import xapi.gwtc.api.GwtcXmlBuilder;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.javac.dev.api.CompilerService;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.source.X_Source;
import xapi.test.components.client.GeneratedComponentEntryPoint;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static xapi.collect.X_Collect.toArray;
import static xapi.fu.X_Fu.notEmpty;
import static xapi.util.X_String.join;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.TreeLogger.Type;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/14/16.
 */
public class GwtcSteps {

  public static class CompiledComponent {

    private final GwtcService gwtc;
    private final CompilationUnit parsed;
    private final ClassOrInterfaceDeclaration cls;
    private final GwtManifest manifest;

    public CompiledComponent(
        ClassOrInterfaceDeclaration cls,
        CompilationUnit parsed,
        GwtcService gwtc,
        GwtManifest manifest
    ) {
      this.cls = cls;
      this.parsed = parsed;
      this.gwtc = gwtc;
      this.manifest = manifest;
    }

    public GwtcService getGwtc() {
      return gwtc;
    }

    public CompilationUnit getParsed() {
      return parsed;
    }

    protected String getWebComponentFactorySimpleName() {
      return WebComponentFactoryGenerator.toFactoryName(cls.getName());
    }

    public File getWebComponentFactoryFile() {
      String simpleName = getWebComponentFactorySimpleName();
      String resourcePath = cls.getPackageAsPath() + simpleName + ".java";
      return new File(getGwtc().inGeneratedDirectory(getManifest(), resourcePath));
    }

    public GwtManifest getManifest() {
      return manifest;
    }
  }
  private static final String QUOTED = "\"([^\"]+)\"";

  static {
    X_Log.logLevel(LogLevel.INFO);
    X_Properties.setProperty(X_Namespace.PROPERTY_LOG_LEVEL, "ALL");
    X_Properties.setProperty(X_Namespace.PROPERTY_MULTITHREADED, "10");
  }

  private StringTo<CompiledComponent> compiledComponents;
  private StringTo<String> sources;

  @Before
  public void before() {
    compiledComponents = X_Collect.newStringMap(CompiledComponent.class);
    sources = X_Collect.newStringMap(String.class);
  }

  @Given("^compile the code:$")
  public void compileTheCode(List<String> lines) {
    final Optional<String> first = lines.stream().filter(notEmpty()).findFirst();
    if (!first.isPresent()) {
      throw new AssertionError("No text supplied to compile method.  You sent " + lines);
    }
    CompilerService compiler = X_Inject.singleton(CompilerService.class);


  }

  @Given("^compile the component:$")
  public void compileTheComponent(List<String> lines) throws ParseException {
    final Optional<String> first = lines.stream().filter(notEmpty()).findFirst();
    if (!first.isPresent()) {
      throw new AssertionError("No text supplied to compile method.  You sent " + lines);
    }

    GwtcService service = X_Gwtc.getServiceFor(GeneratedComponentEntryPoint.class);
    service.addClasspath(GeneratedComponentEntryPoint.class);
    service.addClasspath(IsWebComponent.class);
    service.createFile("META-INF", "xapi.properties", ()->"");
    final GwtManifest manifest = new GwtManifest(service.getModuleName());
    initializeManifest(service, manifest);


    // Now, lets add our component to the Gwt compilation classpath
    String code = join("\n", toArray(lines));
    JavaType type = JavaType.UNKNOWN;
    String pkgToUse;
    String clsToUse;
    try {
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

      parsed.getImports().add(new ImportDeclaration(new NameExpr("xapi.components.api"), false, true));
      parsed.getImports().add(new ImportDeclaration(new NameExpr("xapi.ui.api"), false, true));
      clsToUse = parsed.getPrimaryType().getName();


      // lets save the code, then generate an inclusion for it.
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

    } catch (ParseException e) {
      // The source of the component is just
      final UiContainerExpr uiContainer = JavaParser.parseUiContainer(code);
    }
    if (type != JavaType.UNKNOWN) {
    } else {
      // if it wasn't a valid java file, then it is just a plain UiContainer,
      // which we should componentize (wrap in a valid java class)
    }

    final int result = service.compile(manifest);

    assertEquals("Compile failed w/ code " + result, 0, result);

  }

  private void initializeManifest(GwtcService service, GwtManifest manifest) {
    manifest.addSystemProp("gwt.usearchives=false");
    manifest.setLogLevel(Type.INFO);
    manifest.setWorkDir(service.getTempDir().getAbsolutePath());
    manifest.setGenDir(manifest.getGenDir()); // make the default explicit, so the argument is sent to command line
    manifest.setStrict(true); // break on any error
    manifest.setCleanupMode(CleanupMode.DELETE_ON_SUCCESSFUL_EXIT);
  }

  private String getTagName(ClassOrInterfaceDeclaration cls) {
    for (AnnotationExpr anno : cls.getAnnotations()) {
      if (anno.getName().getSimpleName().equals("WebComponent")) {
        if (anno instanceof NormalAnnotationExpr) {
          for (MemberValuePair pair : ((NormalAnnotationExpr) anno).getPairs()) {
            if (pair.getName().equals("tagName")) {
              if (pair.getValue() instanceof StringLiteralExpr) {
                return ((StringLiteralExpr) pair.getValue()).getValue();
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

  @Then("^confirm source \"([^\"]*)\" matches:$")
  public void confirmSourceMatches(String componentName, List<String> lines) throws Throwable {
    class Matches {
      int expectedLine; String expected;
      int actualLine; String actual;

      public Matches(int expectedLine, String expected) {
        this.expectedLine = expectedLine;
        this.expected = expected;
      }
    }
    List<Matches> matches = new ArrayList<>();

    lines.stream()
        .filter(line->!line.trim().isEmpty())
        .forEach(line ->  {
          for (String subLine : line.split("\n|\\\\n")) {
            if (!subLine.trim().isEmpty()) {
              matches.add(new Matches(matches.size(), subLine));
            }
          }
        });

    final String source = sources.get(componentName);
    assertNotNull(source);

    int lineNum = 0;
    for (String line : source.split("\n|\\\\n")) {
      if (!line.trim().isEmpty()) {
        if (lineNum >= matches.size()) {
          // failure just in line size...
          break;
        }
        final Matches match = matches.get(lineNum);
        match.actualLine = lineNum++;
        match.actual = line;
      }
    }
    final Iterator<Matches> itr = matches.iterator();
    while (itr.hasNext()) {
      Matches match = itr.next();
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
}
