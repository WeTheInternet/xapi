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

  final Lazy<ContainerMetadata> root;
  ContainerMetadata parent;
  private UiComponentGenerator generator;
  private UiFeatureGenerator feature;

  public UiGeneratorVisitor() {
    root = Lazy.deferred1(this.createRoot());
  }

  public UiGeneratorVisitor(Out1<ContainerMetadata> source) {
    root = Lazy.deferred1(source);
  }

  public UiGeneratorVisitor(ContainerMetadata source) {
    root = Lazy.immutable1(source);
  }

  protected Out1<ContainerMetadata> createRoot() {
    return ContainerMetadata::new;
  }

  @Override
  public void visit(UiContainerExpr n, UiGeneratorService service) {
    boolean isRoot = parent == null;
    final ContainerMetadata myParent = getParent();

    final ContainerMetadata me;
    if (isRoot) {
      me = parent = myParent;
    } else {
      me = parent = myParent.createChild(n, service);
    }
    try {

      final UiComponentGenerator myGenerator = generator = service.getComponentGenerator(n, me);
      if (generator.startVisit(service, me, n)) {
        super.visit(n, service);
      }
      myGenerator.endVisit(service, me, n);

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

    if (myFeature.startVisit(service, myGenerator, parent, n)) {
      super.visit(n, service);
    }
    myFeature.finishVisit(service, myGenerator, parent, n);

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

  private ContainerMetadata getParent() {
    if (parent == null) {
      parent = root.out1();
    }
    return parent;
  }
}
