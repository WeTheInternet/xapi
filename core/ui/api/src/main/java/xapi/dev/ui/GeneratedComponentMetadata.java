package xapi.dev.ui;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.plugin.NodeTransformer;
import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.collect.api.StringTo;
import xapi.collect.impl.SimpleLinkedList;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.SourceTransform;
import xapi.fu.In1Out1;
import xapi.ui.service.UiService;

import java.util.Optional;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/1/16.
 */
public class GeneratedComponentMetadata {

  public static class MetadataRoot {

    private String rootReference;
    private final StringTo<Integer> nameCounts;
    private final StringTo<StringTo<NodeTransformer>> fieldRenames;

    public MetadataRoot() {
      nameCounts = X_Collect.newStringMap(Integer.class);
      fieldRenames = X_Collect.newStringDeepMap(NodeTransformer.class);
    }

    public String getRootReference() {
      return rootReference;
    }

    public void setRootReference(String rootReference) {
      this.rootReference = rootReference;
    }

    public String newVarName(String prefix) {
      final Integer cnt;
      synchronized (nameCounts) {
        cnt = nameCounts.compute(prefix, (k, was) -> was == null ? 0 : was + 1);
      }
      if (cnt == 0) {
        return prefix;
      }
      return prefix + "_" + cnt;
    }

    public void reserveName(String refName) {
      Integer was = nameCounts.put(refName, 0);
      assert was == null : "Tried to reserve a name, `" + refName + "` more than once\n" +
          "Existing items: " + nameCounts;
    }

    public void registerFieldMapping(String ref, String fieldName, NodeTransformer accessor) {
      fieldRenames.get(ref).put(fieldName, accessor);
    }

    public NodeTransformer findReplacement(String ref, String var) {
      if (fieldRenames.containsKey(ref)) {
        return fieldRenames.get(ref).get(var);
      }
      return null;
    }
  }

  private MetadataRoot root;
  protected final Fifo<SourceTransform> modifiers;
  private StringTo<MethodBuffer> methods;
  private SimpleLinkedList<String> panelNames;
  private boolean allowedToFail;
  private boolean sideEffects;
  private UiContainerExpr container;
  private GeneratedComponentMetadata parent;
  private SourceBuilder<?> sourceBuilder;
  private String elementType;
  private String componentType;
  private String controllerType;
  private String type;
  private boolean $thisPrinted;
  private boolean $uiPrinted;
  private boolean searchTypes;

  public GeneratedComponentMetadata() {
    modifiers = newFifo();
    searchTypes = true;
    methods = X_Collect.newStringMap(MethodBuffer.class);
    panelNames = new SimpleLinkedList<>();
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
  }

  public UiContainerExpr getContainer() {
    return container;
  }

  public GeneratedComponentMetadata createChild(UiContainerExpr n, UiGeneratorService service) {
    final GeneratedComponentMetadata copy = service.createMetadata(root, n);
    copy.parent = this;
    copy.copyFrom(this);
    return copy;
  }

  protected void copyFrom(GeneratedComponentMetadata metadata) {
    this.allowedToFail = metadata.allowedToFail;
    this.methods = metadata.methods;
    this.panelNames = metadata.panelNames;
    this.root = metadata.root;
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

  public void saveMethod(String key, MethodBuffer method) {
    final MethodBuffer was = methods.put(key, method);
    assert was == null || was == method : "Attempting to reassign a method that already exists to key " + key +
        ", previous: " + was + "\n new: " + method;
  }

  public MethodBuffer getMethod(String key) {
    return methods.get(key);
  }

  public MethodBuffer getMethod(String key, In1Out1<String, MethodBuffer> create) {
    return methods.getOrCreate(key, create);
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
    assert type.toString().endsWith(sourceBuilder.addImport(type))
        : "Bad type import: " + type;
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

  public SourceBuilder<?> getSourceBuilder() {
    return sourceBuilder == null ? getParent() == null ? null : getParent().getSourceBuilder() : sourceBuilder;
  }

  public void pushPanelName(String root) {
    panelNames.add(root);
  }

  public String peekPanelName() {
    return panelNames.tail();
  }

  public String popPanelName() {
    return panelNames.pop();
  }

  public MethodBuffer removeMethod(String key) {
    return methods.remove(key);
  }

  public MethodBuffer getParentMethod() {
    String parent = peekPanelName();
    final MethodBuffer method = getMethod(parent);
    assert method != null : "No method named " + parent + " found in " + this;
    return method;
  }

  public String getRefName() {
    return getRefName("ref");
  }

  public String getRefName(String backup) {

    final Optional<UiAttrExpr> refAttr = container.getAttribute("ref");
    if (refAttr.isPresent()) {
      String refName = ASTHelper.extractAttrValue(refAttr.get());
      root.reserveName(refName);
      return refName;
    } else {
      String genRef = newVarName(backup);
      container.addAttribute(true, new UiAttrExpr("ref", new StringLiteralExpr(genRef)));
      return genRef;
    }
  }

  public void setRoot(MetadataRoot root) {
    this.root = root;
  }

  public void queryContainer(ComponentMetadataQuery query) {
    container.accept(new ComponentMetadataFinder(), query);
  }

  public String getRootReference() {
    if (root.rootReference == null) {
      GeneratedComponentMetadata seed = this;
      while (seed.getParent() != null) {
        seed = seed.getParent();
      }
      root.rootReference = seed.getRefName();
    }
    return root.rootReference;
  }

  public String newVarName(String prefix) {
    // TODO: look up at parents for scoping...
    return root.newVarName(prefix);
  }

  public boolean isSearchTypes() {
    return searchTypes;
  }

  public void setSearchTypes(boolean searchTypes) {
    this.searchTypes = searchTypes;
  }

  public void registerFieldProvider(String ref, String fieldName, NodeTransformer accessor) {
    root.registerFieldMapping(ref, fieldName, accessor);
  }

  public NodeTransformer findReplacement(String ref, String var) {
    return root.findReplacement(ref, var);
  }
}
