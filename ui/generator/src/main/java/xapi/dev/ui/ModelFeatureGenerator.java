package xapi.dev.ui;

import com.github.javaparser.ast.expr.UiAttrExpr;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.GeneratedUiComponent;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiVisitScope;
import xapi.dev.ui.impl.UiGeneratorTools;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class ModelFeatureGenerator extends DataFeatureGenerator {

  @Override
  public UiVisitScope startVisit(
      UiGeneratorTools service,
      UiComponentGenerator generator,
      ComponentBuffer source,
      ContainerMetadata container,
      UiAttrExpr attr
  ) {
    final GeneratedUiComponent component = source.getGeneratedComponent();
    return UiVisitScope.FEATURE_VISIT_CHILDREN;
//    if (component.hasPublicModel()) {
//
//    }
//    final Expression value = attr.getExpression();
//    if (value instanceof JsonContainerExpr) {
//      // Check the annotation for a type to use,
//      // either a bean with all the keys that match the supplied data,
//      // or a list / map / array / container type to use.
//      JsonContainerExpr json = (JsonContainerExpr) value;
//      Optional<AnnotationExpr> anno = attr.getAnnotation(
//          a -> a.getName().getName().equalsIgnoreCase("type"));
//      DataTypeOptions opts;
//      final ClassBuffer cb = container.getSourceBuilder().getClassBuffer();
//      if (anno.isPresent()) {
//        opts = getDatatypeFrom(cb, container, json, anno.get());
//      } else {
//        opts = getDatatypeFrom(cb, container, json);
//      }
//      String var = printFactory(json, container, anno, opts, cb, generator.getTransformer());
//      // register `varName.out1()` as a replacement for all accessors of this particular data
//      MethodCallExpr expr = new MethodCallExpr(new NameExpr(var), "out1");
//      container.registerFieldProvider(container.getRefName(), "model", newTransformer(service, generator, container, json, opts, expr));
//      return UiVisitScope.FEATURE_VISIT_CHILDREN;
//    } else {
//      throw new IllegalArgumentException("Cannot assign a node of type " + value.getClass() + " to a data feature; bad data: " + value);
//    }
  }

}
