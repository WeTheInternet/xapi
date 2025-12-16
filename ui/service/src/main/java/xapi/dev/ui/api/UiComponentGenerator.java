package xapi.dev.ui.api;

import net.wti.lang.parser.ast.expr.UiContainerExpr;
import xapi.dev.lang.GeneratorAwareTransformer;
import net.wti.lang.parser.ast.plugin.Transformer;
import xapi.dev.lang.gen.ApiGeneratorContext;
import xapi.dev.lang.gen.ApiGeneratorTools;
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
    TAG_DEFINITION, UI_BUILDING, MODEL_BUILDING, WEB_APP_BUILDING;
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
    transformer = defaultTransformer();
  }

  protected Transformer defaultTransformer() {
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

  public <Ctx extends ApiGeneratorContext<Ctx>> Transformer getTransformer(ApiGeneratorTools<Ctx> tools, Ctx ctx) {
    if (tools == null || ctx == null) {
      return transformer;
    }
    return new GeneratorAwareTransformer<>(tools, ctx, transformer);
  }
}
