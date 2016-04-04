package xapi.javac.dev.api;

import com.github.javaparser.ast.expr.Expression;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import xapi.source.read.JavaModel.IsType;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
public interface InjectionResolver {

  void doNothing();

  void surroundWith(String prefix, String suffix);

  void replace(Tree source, String newSource);

  void replaceWithMethodCall(Class methodClass, String methodName, String... params);

  void replaceWithFieldReference(Class methodClass, String fieldName);

  void replaceWithExpression(Expression expr);

  String importTypeName(String cls);

  default String importClass(Class cls) {
    return importTypeName(cls.getCanonicalName());
  }

  default String importType(IsType cls) {
    return importTypeName(cls.getQualifiedName());
  }

  boolean isResolved();

  boolean hasChanges();

  void init(JavacService service, CompilationUnitTree cup, SourceTransformationService sourceTransformationService);

  void commit();
}
