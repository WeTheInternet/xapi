package xapi.javac.dev.model;

import com.sun.source.tree.CompilationUnitTree;

import javax.tools.JavaFileObject;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
public class SourceTransformation {

  public enum SourceTransformType {
    REPLACE, WRAP, REMOVE, REPACKAGE, CHANGE_IMPORT
  }

  private SourceTransformType transformType;

  private SourceRange range;

  private String text;

  private String extraText;

  private String expected;

  private CompilationUnitTree compilationUnit;
  private boolean failedTransform;

  public CompilationUnitTree getCompilationUnit() {
    return compilationUnit;
  }

  public SourceTransformation setCompilationUnit(CompilationUnitTree compilationUnit) {
    this.compilationUnit = compilationUnit;
    return this;
  }

  public String getExpected() {
    return expected;
  }

  public SourceTransformation setExpected(String expected) {
    this.expected = expected;
    return this;
  }

  public String getExtraText() {
    return extraText;
  }

  public SourceTransformation setExtraText(String extraText) {
    this.extraText = extraText;
    return this;
  }

  public SourceRange getRange() {
    return range;
  }

  public SourceTransformation setRange(SourceRange range) {
    this.range = range;
    return this;
  }

  public String getText() {
    return text;
  }

  public SourceTransformation setText(String text) {
    this.text = text;
    return this;
  }

  public SourceTransformType getTransformType() {
    return transformType;
  }

  public SourceTransformation setTransformType(SourceTransformType transformType) {
    this.transformType = transformType;
    return this;
  }

  public void setFailedTransform(String source, String name, JavaFileObject cup) {
    this.failedTransform = true;
  }

  public boolean isFailedTransform() {
    return failedTransform;
  }
}
