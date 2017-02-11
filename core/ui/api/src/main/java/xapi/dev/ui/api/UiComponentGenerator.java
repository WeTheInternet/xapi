package xapi.dev.ui.api;

import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.plugin.Transformer;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.Lazy;
import xapi.fu.Out1;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/3/16.
 */
public class UiComponentGenerator {

  public enum UiGenerateMode {
    TAG_DEFINITION, UI_BUILDING, MODEL_BUILDING;
    public static final UiGenerateMode DEFAULT = UI_BUILDING;
  }

  protected Lazy<DomBuffer> shadowDomBuffer;
  protected Lazy<SourceBuilder<ContainerMetadata>> classBuffer;
  private ContainerMetadata metadata;
  private List<UiFeatureGenerator> featureGenerators;
  private boolean printCommentsAllowed;
  private Transformer transformer;

  public UiComponentGenerator() {
    this(DomBuffer::new);
  }

  public UiComponentGenerator(Out1<DomBuffer> factory) {
    shadowDomBuffer = Lazy.deferred1(factory);
    classBuffer = Lazy.deferBoth(SourceBuilder<ContainerMetadata>::new, this::getMetadata);
    featureGenerators = new ArrayList<>();
    transformer = createTransformer();
  }

  protected Transformer createTransformer() {
    return new Transformer();
  }

  public DomBuffer dom() {
    return shadowDomBuffer.out1();
  }

  public SourceBuilder<ContainerMetadata> java() {
    return classBuffer.out1();
  }

  public ContainerMetadata getMetadata() {
    return metadata;
  }

  public void setMetadata(ContainerMetadata metadata) {
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

  public UiVisitScope startVisit(
      UiGeneratorTools tools,
      ComponentBuffer source,
      ContainerMetadata me,
      UiContainerExpr n,
      UiGenerateMode mode
  ) {
    return UiVisitScope.CONTAINER_VISIT_CHILDREN;
  }

  public void endVisit(UiGeneratorTools tools, ContainerMetadata me, UiContainerExpr n, UiVisitScope scope) {

  }

  public Transformer getTransformer() {
    return transformer;
  }
}
