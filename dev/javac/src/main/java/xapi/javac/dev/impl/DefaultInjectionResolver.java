package xapi.javac.dev.impl;

import com.github.javaparser.ast.expr.Expression;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeInfo;
import xapi.annotation.inject.InstanceDefault;
import xapi.javac.dev.api.InjectionResolver;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.api.SourceTransformationService;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
@InstanceDefault(implFor = InjectionResolver.class)
public class DefaultInjectionResolver implements InjectionResolver {

  boolean changes;
  boolean finished;
  private JavacService service;
  private CompilationUnitTree cup;
  private SourceTransformationService sources;

  @Override
  public void surroundWith(String prefix, String suffix) {
    changed();

  }

  private void changed() {
    checkNotFinished();
    changes = true;
  }

  @Override
  public void doNothing() {
    finish();
  }

  private void finish() {
    checkNotFinished();
    finished = true;
  }

  protected void checkNotFinished() {
    if (finished) {
      throw new IllegalStateException("Injection already finished");
    }
  }

  @Override
  public void replace(Tree source, String newSource) {
    changed();
    finish();
    final EndPosTable endPosTable = ((JCCompilationUnit) cup).endPositions;
    final int endPos = TreeInfo.getEndPos((JCTree) source, endPosTable);
    final int startPos = TreeInfo.getStartPos((JCTree) source);
    sources.requestOverwrite(cup, startPos, endPos, newSource);
  }

  @Override
  public void replaceWithMethodCall(Class methodClass, String methodName, String... params) {
    changed();
    finish();
  }

  @Override
  public void replaceWithFieldReference(Class methodClass, String fieldName) {
    importClass(methodClass);
    changed();
    finish();

  }

  @Override
  public void replaceWithExpression(Expression expr) {
    changed();
    finish();

  }

  @Override
  public String importTypeName(String cls) {
    changed();
    return cls;
  }

  @Override
  public boolean isResolved() {
    return finished;
  }

  @Override
  public boolean hasChanges() {
    return changes;
  }

  @Override
  public void init(
      JavacService service,
      CompilationUnitTree cup,
      SourceTransformationService sourceTransformationService
  ) {
    this.service = service;
    this.cup = cup;
    this.sources = sourceTransformationService;
  }

  @Override
  public void commit() {

  }
}
