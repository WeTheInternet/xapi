package xapi.dev.ui;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.plugin.NodeTransformer;
import com.github.javaparser.ast.plugin.Transformer;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.api.UiVisitScope;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Lazy;
import xapi.fu.Maybe;
import xapi.source.api.IsType;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class DataFeatureGenerator extends UiFeatureGenerator {

  @Override
  public UiVisitScope startVisit(
      UiGeneratorTools service,
      UiComponentGenerator generator,
      ComponentBuffer source,
      ContainerMetadata container,
      UiAttrExpr attr
  ) {
    final Expression value = attr.getExpression();
    if (value instanceof JsonContainerExpr) {
      // Check the annotation for a type to use,
      // either a bean with all the keys that match the supplied data,
      // or a list / map / array / container type to use.
      JsonContainerExpr json = (JsonContainerExpr) value;
      Maybe<AnnotationExpr> anno = attr.getAnnotation(
          a -> a.getName().getName().equalsIgnoreCase("type"));
      DataTypeOptions opts;
      final ClassBuffer cb = container.getSourceBuilder().getClassBuffer();
      if (anno.isPresent()) {
        opts = getDatatypeFrom(cb, container, json, anno.get());
      } else {
        opts = getDatatypeFrom(cb, container, json);
      }
      final Transformer transformer = generator.getTransformer(service, source.getRoot().getContext());
      String var = printFactory(json, container, anno, opts, cb, transformer);
      // register `varName.out1()` as a replacement for all accessors of this particular data
      MethodCallExpr expr = new MethodCallExpr(new NameExpr(var), "out1");
      container.registerFieldProvider(container.getRefName(), "data", newTransformer(service, generator, container, json, opts, expr));
      return UiVisitScope.FEATURE_VISIT_CHILDREN;
    } else {
      throw new IllegalArgumentException("Cannot assign a node of type " + value.getClass() + " to a data feature; bad data: " + value);
    }
  }

  protected static class DataTypeTransformer extends NodeTransformer {

    private final DataTypeOptions opts;
    private final JsonContainerExpr json;


    public DataTypeTransformer(
        DataTypeOptions opts,
        JsonContainerExpr json,
        Node newNode,
        ClassBuffer out,
        In2Out1<Node, Expression, Node> createRead,
        In2Out1<Node, Expression, Node> createWrite,
        In1Out1<String, String> addImport
    ) {
      super(newNode, out, createRead, createWrite, addImport);
      this.opts = opts;
      this.json = json;
    }

    public DataTypeTransformer(
        DataTypeOptions opts,
        JsonContainerExpr json,
        Node newNode,
        ClassBuffer out,
        In2Out1<Node, Expression, Node> createRead,
        In2Out1<Node, Expression, Node> createWrite,
        In2Out1<Node, Expression, Node> compute,
        In1Out1<String, String> addImport
    ) {
      super(newNode, out, createRead, createWrite, compute, addImport);
      this.opts = opts;
      this.json = json;
    }

    @Override
    protected String getDataType(String keyName) {
      final IsType type = opts.getFieldTypes().get(keyName);
      if (type == null) {
        if (opts.getType() != null) {
          return opts.getType();
        }
      }
      return type == null ? super.getDataType(keyName) : type.getQualifiedName();
    }

    @Override
    public Node transformUnary(Expression source, UnaryExpr expr) {
      final Node node = getNode();
      final Expression scope = expr.getExpr();
      if (scope instanceof FieldAccessExpr) {
        FieldAccessExpr field = (FieldAccessExpr) scope;
        if (field.getScope() == node) {
          // an expression like $root.data.key ++
          // was transformed into rootData.out1().key ++
          // and the scope is rootData.out1()
          // so now we can look at "key" to see it's datatype,
          // and figure out how to compute the given operation.
          String key = field.getField();
          final IsType type = opts.getFieldTypes().get(key);
          switch (expr.getOperator()) {
            case posDecrement: // key--
            case posIncrement: // key++
              // we want to return the current value,
              // but store a mutated value
              // For StringTo types we can use computeReturnPrevious
//              createRead.io()

            case preDecrement: // --key
            case preIncrement: // ++key
              // For StringTo types we can use compute

            case inverse: // !key
            case negative: // -key
            case not: // !key
            case positive: // +key
          }
        }
      }
      return super.transformUnary(source, expr);
    }
  }

  protected NodeTransformer newTransformer(
      UiGeneratorTools service,
      UiComponentGenerator generator,
      ContainerMetadata container,
      JsonContainerExpr value,
      DataTypeOptions opts,
      Node expr
  ) {

    final In2Out1<Node, Expression, Node> read = (i, k)->i;
    final In2Out1<Node, Expression, Node> write = (i, k)->i;

    switch (opts.getType()) {
      case "xapi.collect.api.StringTo":
        break;
      case "xapi.collect.api.IntTo":
        break;
      default:
        if (value.isArray()) {
          // modifiers will be .get() and .set()
        } else { // map
          // modifiers will be .get() and .put()
        }
    }
    return new DataTypeTransformer(opts, value, expr, container.getSourceBuilder().getClassBuffer(), read, write, container.getSourceBuilder()::addImport);
  }

  protected String printFactory(
      JsonContainerExpr json,
      ContainerMetadata container,
      Maybe<AnnotationExpr> anno,
      DataTypeOptions opts,
      ClassBuffer cb,
      Transformer transformer
  ) {
    String collection = cb.addImport(opts.getCollection());

    String type = opts.getType();
    boolean hasTwoGenerics = type.indexOf(',') != -1;
    if (hasTwoGenerics) {
      type = type.replaceFirst("String\\s*,\\s*", "");
    }
    type = type.indexOf('.') == -1 ? opts.getType() : cb.addImport(opts.getType());

    String lazy = cb.addImport(Lazy.class);
    String varName = container.newVarName(container.getRefName()+"Data");
    String fieldType = collection + "<" + (hasTwoGenerics ? "String, " : "") + type + ">";
    final PrintBuffer initializer = cb.createField(
        lazy + "<" + fieldType + ">",
        varName
    ).getInitializer()
        .print(lazy).print(".deferred1(()->")
    ;
    String inst = opts.getFactory().replaceAll("[$]type", type);
    if (json.isEmpty()) {
      initializer.print(inst);
    } else {
      initializer.println("{");
      initializer.indent();
      initializer.println(fieldType + " data = " + inst + ";");
      json.getPairs().forEach(pair->{
        initializer.print("data." + opts.getAdder() + "(");
        if (json.isArray()) {
          initializer.println(pair.getValueExpr().toSource(transformer) + ");");
        } else {
          initializer
              .print(pair.getKeyQuoted())
              .print(", ")
              .print(pair.getValueExpr().toSource(transformer))
              .println(");");

        }
      });
      initializer.println("return data;")
                 .outdent()
                 .print("}");
    }

    initializer.println(");");
    return varName;
  }

  protected DataTypeOptions dataTypeOptions() {
    return new DataTypeOptions();
  }

  protected DataTypeOptions getDatatypeFrom(ClassBuffer cb, ContainerMetadata metadata, JsonContainerExpr json) {
    return dataTypeOptions().fromAnnotation(cb, json, null, metadata.isSearchTypes());
  }

  protected DataTypeOptions getDatatypeFrom(ClassBuffer cb, ContainerMetadata metadata, JsonContainerExpr json, AnnotationExpr anno) {
    return dataTypeOptions().fromAnnotation(cb, json, anno, metadata.isSearchTypes());
  }

}
