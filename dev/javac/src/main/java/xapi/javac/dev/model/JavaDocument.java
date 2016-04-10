package xapi.javac.dev.model;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.sun.source.tree.CompilationUnitTree;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.Pointer;
import xapi.io.X_IO;
import xapi.javac.dev.api.JavacService;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.util.api.DebugRethrowable;

import static xapi.fu.Lazy.ofDeferredConcrete;
import static xapi.fu.Pointer.pointerToDeferred;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/9/16.
 */
public class JavaDocument implements DebugRethrowable {

  private final CompilationUnitTree compilationUnit;
  private final TypeElement type;
  private final JavacService service;
  private final Lazy<String> originalSource;
  private final Lazy<String> packageName;
  private final Lazy<String> enclosedName;
  private final Lazy<String> fileName;
  private final Pointer<CompilationUnit> currentSource;
  private final Pointer<TypeDeclaration> currentType;
  private final JavaDocument previousVersion;

  public JavaDocument(
      JavacService service,
      CompilationUnitTree compilationUnit,
      TypeElement type
  ) {
    this(null, service, compilationUnit, type);
  }

  public JavaDocument(
      JavaDocument previousVersion,
      JavacService service,
      CompilationUnitTree compilationUnit,
      TypeElement type
  ) {
    this.previousVersion = previousVersion;
    this.service = service;
    this.type = type;
    this.compilationUnit = compilationUnit;
    assert service != null : "null service";
    assert compilationUnit != null : "null compilation unit";
    assert type != null : "null type";
    packageName = Lazy.of(service::getPackageName, compilationUnit);
    enclosedName = Lazy.ofDeferred(()->getTypeName().replace(packageName.out1()+".", ""));
    fileName = Lazy.ofDeferred(()->service.getFileName(compilationUnit));
    originalSource = ofDeferredConcrete(this::toSource, compilationUnit);
    currentSource = pointerToDeferred(
        originalSource.map(X_IO::toStreamUtf8)
            .mapUnsafe(JavaParser::parse)
    );
    currentType = pointerToDeferred(currentSource
      .map(unit->
        unit.getTypes().stream()
            .filter(typeDecl -> {
              String name = typeDecl.getName();
              return name.equals(type.getQualifiedName().toString());
            })
            .findFirst().get()
      ));
  }

  public String toSource(CompilationUnitTree unit) {
    final JavaFileObject source = unit.getSourceFile();
    try {
      return X_IO.toStringUtf8(source.openInputStream());
    } catch (IOException e) {
      throw rethrow(e);
    }
  }

  public boolean hasImport(String importName) {
    return compilationUnit.getImports().stream()
        .anyMatch(importTree->{
          final String name = service.getQualifiedName(compilationUnit, importTree);
          return name.startsWith(importName);
        });
  }

  /**
   * Called whenever a unit which exists is seen again.
   *
   * This will not be relevant until multi-phase compilation is complete.
   */
  public void record(CompilationUnitTree cup, TypeElement typeElement) {

  }

  public String getCompilationUnitName() {
    return service.getQualifiedName(compilationUnit);
  }

  public void finish() {
    X_Log.info(getClass(), "Finished compilation of document ",getTypeName(), this);
  }

  public String getTypeName() {
    return type.getQualifiedName().toString();
  }

  public String getEnclosedName() {
    return enclosedName.out1();
  }

  public JavaFileObject getSourceFile() {
    return compilationUnit.getSourceFile();
  }

  public String getPackageName() {
    return service.getPackageName(compilationUnit);
  }

  public String getFileName() {
    return fileName.out1();
  }

  public String getSource() {
    return originalSource.out1();
  }

  public CompilationUnit getAst() {
    return currentSource.out1();
  }

  public CompilationUnitTree getCompilationUnit() {
    return compilationUnit;
  }

  /**
   * Called when a document that has had source rewritten has been finalized
   * (i.e., it's source was in the working directory, and it finished compilation
   * without any plugins requesting source rewrites, so now it will be placed in the final output directory).
   */
  public void finalize(JavacService service) {
    // TODO add in listeners who want to be notified that this document has completed all source rewriting.

  }

  @Override
  public String toString() {
    X_Log.trace(getClass(),
        "JavaDocument: ", getTypeName(),

        "Source:\n\n", Out1.out1(this::getSource),
        "\n\nCompilation Unit:", Out1.out1(this::getCompilationUnit)
        );
    return "JavaDocument [" + getTypeName() +"]";
  }

  public String getAstName() {
    return X_Source.qualifiedName(getPackageName(), getEnclosedName().split("[.]")[0]);
  }
}
