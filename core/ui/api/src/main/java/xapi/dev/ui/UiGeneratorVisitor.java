package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiBodyExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.fu.Lazy;
import xapi.fu.Out1;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/4/16.
 */
public class UiGeneratorVisitor extends VoidVisitorAdapter<UiGeneratorService> {

  final Lazy<GeneratedComponentMetadata> root;
  GeneratedComponentMetadata parent;
  private UiComponentGenerator generator;
  private UiFeatureGenerator feature;

  public UiGeneratorVisitor() {
    root = Lazy.ofDeferred(this.createRoot());
  }

  protected Out1<GeneratedComponentMetadata> createRoot() {
    return GeneratedComponentMetadata::new;
  }

  @Override
  public void visit(UiContainerExpr n, UiGeneratorService service) {
    final GeneratedComponentMetadata myParent = getParent();
    final GeneratedComponentMetadata me = parent = myParent.createChild(n, service);
    try {

      final UiComponentGenerator myGenerator = generator = service.getComponentGenerator(n, me);
      if (generator.visitSuper(service, me, n)) {
        super.visit(n, service);
      }
      myGenerator.finishVisit(service, me, n);

    } finally {
      parent = myParent;
    }

  }

  @Override
  public void visit(
      UiAttrExpr n, UiGeneratorService service
  ) {
    final UiComponentGenerator myGenerator = generator;
    final UiFeatureGenerator myFeature = feature = service.getFeatureGenerator(n, generator);

    if (myFeature.shouldVisitorSuper(service, myGenerator, n)) {
      super.visit(n, service);
    }
    myFeature.finishVisit(service, myGenerator, n);

    if (myFeature.hasSideEffects()) {
      myGenerator.getMetadata().recordSideEffects(service, myFeature);
    }

  }

  @Override
  public void visit(
      UiBodyExpr n, UiGeneratorService arg
  ) {
    if (n.isNotEmpty()) {

    }
    super.visit(n, arg);
  }

  private GeneratedComponentMetadata getParent() {
    if (parent == null) {
      parent = root.out1();
    }
    return parent;
  }
}
