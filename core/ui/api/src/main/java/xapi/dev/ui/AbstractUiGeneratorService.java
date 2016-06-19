package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Out2;
import xapi.source.X_Source;

import java.util.Arrays;
import java.util.Objects;

/**
 * Created by james on 6/17/16.
 */
public class AbstractUiGeneratorService implements UiGeneratorService {

  protected final StringTo<UiComponentGenerator> componentGenerators;
  protected final StringTo<UiFeatureGenerator> featureGenerators;
  protected final StringTo<Integer> numGenerated;

  public AbstractUiGeneratorService() {
    componentGenerators = X_Collect.newStringMap(UiComponentGenerator.class);
    featureGenerators = X_Collect.newStringMap(UiFeatureGenerator.class);
    numGenerated = X_Collect.newStringMap(Integer.class);
    componentGenerators.addAll(getComponentGenerators());
    featureGenerators.addAll(getFeatureGenerators());
  }

  protected Iterable<Out2<String, UiComponentGenerator>> getComponentGenerators() {
    return Arrays.asList(
        Out2.out2("app", new UiComponentGenerator()),
        Out2.out2("button", new UiComponentGenerator())
    );
  }

  protected Iterable<Out2<String, UiFeatureGenerator>> getFeatureGenerators() {
    return Arrays.asList(
        Out2.out2("ref", new UiFeatureGenerator()),
        Out2.out2("title", new UiFeatureGenerator()),
        Out2.out2("body", new UiFeatureGenerator()),
        Out2.out2("text", new UiFeatureGenerator()),
        Out2.out2("onClick", new UiFeatureGenerator())
    );
  }

  @Override
  public GeneratedComponentMetadata generateComponent(
      String pkgName, String className, UiContainerExpr expr
  ) {
    final GeneratedComponentMetadata metadata = createMetadata(expr);
    String generatedName = calculateGeneratedName(pkgName, className, expr);
    SourceBuilder b = new SourceBuilder("public class " + generatedName);
    b.setPackage(pkgName);
    metadata.setSourceBuilder(b);
    UiGeneratorVisitor visitor = createVisitor(pkgName, className, expr, metadata);

    visitor.visit(expr, this);
    return metadata;
  }

  protected UiGeneratorVisitor createVisitor(
      String pkgName,
      String className,
      UiContainerExpr expr,
      GeneratedComponentMetadata metadata
  ) {
    return new UiGeneratorVisitor(metadata);
  }

  protected String calculateGeneratedName(String pkgName, String className, UiContainerExpr expr) {
    String fqcn = X_Source.qualifiedName(pkgName, className);
    return className + "__Component_" + numGenerated.compute(fqcn, i->i == null ? 0 : i++);
  }

  @Override
  public UiComponentGenerator getComponentGenerator(UiContainerExpr container, GeneratedComponentMetadata metadta) {
    final UiComponentGenerator generator = componentGenerators.get(container.getName());
    Objects.requireNonNull(generator, "Null component for " + container.getName());
    return generator;
  }

  @Override
  public UiFeatureGenerator getFeatureGenerator(UiAttrExpr container, UiComponentGenerator componentGenerator) {
    final UiFeatureGenerator generator = featureGenerators.get(container.getNameString());
    Objects.requireNonNull(generator, "Null feature for " + container.getNameString());
    return generator;
  }
}
