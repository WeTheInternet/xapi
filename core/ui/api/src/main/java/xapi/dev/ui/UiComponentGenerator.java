package xapi.dev.ui;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TemplateLiteralExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Lazy;
import xapi.fu.Out1;

import static xapi.reflect.X_Reflect.className;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/3/16.
 */
public class UiComponentGenerator {

  protected Lazy<DomBuffer> shadowDomBuffer;
  protected Lazy<SourceBuilder<GeneratedComponentMetadata>> classBuffer;
  private GeneratedComponentMetadata metadata;
  private List<UiFeatureGenerator> featureGenerators;
  private boolean printCommentsAllowed;

  public UiComponentGenerator() {
    this(DomBuffer::new);
  }

  public UiComponentGenerator(Out1<DomBuffer> factory) {
    shadowDomBuffer = Lazy.ofDeferred(factory);
    classBuffer = Lazy.ofDeferred(SourceBuilder<GeneratedComponentMetadata>::new, this::getMetadata);
    featureGenerators = new ArrayList<>();
  }

  public String convertToSource(Expression value) {
    if (value instanceof StringLiteralExpr) {
      return ((StringLiteralExpr)value).getValue();
    } else if (value instanceof TemplateLiteralExpr) {
      return ((TemplateLiteralExpr)value).getValueWithoutTicks();
    } else {
      throw new IllegalArgumentException("Cannot convert " + className(value) +" into source: " + value);
    }
  }

  public DomBuffer dom() {
    return shadowDomBuffer.out1();
  }

  public SourceBuilder<GeneratedComponentMetadata> java() {
    return classBuffer.out1();
  }

  public GeneratedComponentMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(GeneratedComponentMetadata metadata) {
    this.metadata = metadata;
    if (classBuffer.isImmutable()) {
      classBuffer.out1().setPayload(metadata);
    }
  }

  public boolean isPrintCommentsAllowed() {
    return printCommentsAllowed;
  }

  public void setPrintCommentsAllowed(boolean printCommentsAllowed) {
    this.printCommentsAllowed = printCommentsAllowed;
  }

  public boolean visitSuper(UiGeneratorService service, GeneratedComponentMetadata me, UiContainerExpr n) {
    return true;
  }

  public void finishVisit(UiGeneratorService service, GeneratedComponentMetadata me, UiContainerExpr n) {

  }
}
