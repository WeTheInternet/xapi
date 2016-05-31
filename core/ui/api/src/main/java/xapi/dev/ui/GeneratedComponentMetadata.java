package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.SourceTransform;
import xapi.fu.Lazy;
import xapi.ui.service.UiService;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/1/16.
 */
public class GeneratedComponentMetadata {

  protected final Fifo<SourceTransform> modifiers;
  private boolean allowedToFail;
  private boolean sideEffects;
  private UiContainerExpr container;
  private GeneratedComponentMetadata parent;
  private Lazy<ComponentMetadataFinder> metadata;
  private SourceBuilder<?> sourceBuilder;
  private String elementType;
  private String componentType;
  private String controllerType;
  private String type;
  private boolean $thisPrinted;
  private boolean $uiPrinted;

  public GeneratedComponentMetadata() {
    modifiers = newFifo();
    allowedToFail = Boolean.getBoolean("xapi.component.ignore.parse.failure");
  }

  public GeneratedComponentMetadata(UiContainerExpr container) {
    this();
    setContainer(container);
  }
  public void addModifier(SourceTransform transform) {
    modifiers.give(transform);
  }

  protected Fifo<SourceTransform> newFifo() {
    return X_Collect.newFifo();
  }

  public boolean isAllowedToFail() {
    return allowedToFail;
  }

  public void setContainer(UiContainerExpr container) {
    // Check the container for various interesting things, like method references.
    this.container = container;
    this.metadata = Lazy.ofDeferred(()->{
      ComponentMetadataFinder finder = new ComponentMetadataFinder();
      container.accept(finder, null);
      return finder;
    });
  }

  public UiContainerExpr getContainer() {
    return container;
  }

  public GeneratedComponentMetadata createChild(UiContainerExpr n, UiGeneratorService service) {
    final GeneratedComponentMetadata copy = service.createMetadata(n);
    copy.parent = this;
    copy.copyFrom(this);
    return copy;
  }

  protected void copyFrom(GeneratedComponentMetadata metadata) {
    this.allowedToFail = metadata.allowedToFail;
  }

  public GeneratedComponentMetadata getParent() {
    return parent;
  }

  public void recordSideEffects(UiGeneratorService service, UiFeatureGenerator feature) {
    recordSideEffects(service, this, feature);
  }

  public void recordSideEffects(UiGeneratorService service, GeneratedComponentMetadata source, UiFeatureGenerator feature) {
    setSideEffects(true);
    if (parent != null) {
      parent.recordSideEffects(service, source, feature);
    }
  }

  public boolean isSideEffects() {
    return sideEffects;
  }

  public void setSideEffects(boolean sideEffects) {
    this.sideEffects = sideEffects;
  }

  public void applyModifiers(ClassBuffer out, String input) {
    // TODO intelligent handling of multiple modifiers...
    modifiers.out(modifier->out.printlns(modifier.transform(input)));
  }

  public void setSourceBuilder(SourceBuilder<?> sourceBuilder) {
    this.sourceBuilder = sourceBuilder;
  }

  public void setElementType(String elementType) {
    this.elementType = elementType;
  }

  public String getElementType() {
    return elementType;
  }

  public String getElementTypeImported() {
    return sourceBuilder.addImport(elementType);
  }

  public void setComponentType(String componentType) {
    this.componentType = componentType;
  }

  public String getComponentType() {
    return componentType;
  }

  public void setControllerType(String controllerType) {
    this.controllerType = controllerType;
  }

  public String getControllerType() {
    return controllerType;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public String getTypeImported() {
    return sourceBuilder.addImport(type);
  }

  public void ensure$this() {
    ensure$ui();
    if ($thisPrinted) {
      return;
    }
    $thisPrinted = true;
    String imported = getTypeImported();
    addModifier(ele ->
        imported + " $this = (" + imported + ") $ui.getHost(" + ele + ");"
    );
  }

  public void ensure$ui() {
    if ($uiPrinted) {
      return;
    }
    $uiPrinted = true;
    String service = sourceBuilder.addImport(UiService.class);
    addModifier(ele ->
        service + " $ui = " + service + ".getUiService();"
    );
  }
}
