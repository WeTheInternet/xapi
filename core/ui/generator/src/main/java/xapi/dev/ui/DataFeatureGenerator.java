package xapi.dev.ui;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.plugin.NodeTransformer;
import com.github.javaparser.ast.plugin.Transformer;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Lazy;
import xapi.source.api.IsType;

import java.util.Optional;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class DataFeatureGenerator extends UiFeatureGenerator {

  @Override
  public boolean startVisit(
      UiGeneratorService service, UiComponentGenerator generator, GeneratedComponentMetadata container, UiAttrExpr attr
  ) {
    final Expression value = attr.getExpression();
    if (value instanceof JsonContainerExpr) {
      // Check the annotation for a type to use,
      // either a bean with all the keys that match the supplied data,
      // or a list / map / array / container type to use.
      JsonContainerExpr json = (JsonContainerExpr) value;
      Optional<AnnotationExpr> anno = attr.getAnnotation(
          a -> a.getName().getName().equalsIgnoreCase("type"));
      DataTypeOptions opts;
      final ClassBuffer cb = container.getSourceBuilder().getClassBuffer();
      if (anno.isPresent()) {
        opts = getDatatypeFrom(cb, container, json, anno.get());
      } else {
        opts = getDatatypeFrom(cb, container, json);
      }
      String var = printFactory(json, container, anno, opts, cb, generator.getTransformer());
      // register `varName.out1()` as a replacement for all accessors of this particular data
      MethodCallExpr expr = new MethodCallExpr(new NameExpr(var), "out1");
      container.registerFieldProvider(container.getRefName(), "data", newTransformer(service, generator, container, json, opts, expr));
      return false;
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
        In2Out1<Node, Expression, Node> createRead,
        In2Out1<Node, Expression, Node> createWrite,
        In1Out1<String, String> addImport
    ) {
      super(newNode, createRead, createWrite, addImport);
      this.opts = opts;
      this.json = json;
    }

    public DataTypeTransformer(
        DataTypeOptions opts,
        JsonContainerExpr json,
        Node newNode,
        In2Out1<Node, Expression, Node> createRead,
        In2Out1<Node, Expression, Node> createWrite,
        In2Out1<Node, Expression, Node> compute,
        In1Out1<String, String> addImport
    ) {
      super(newNode, createRead, createWrite, compute, addImport);
      this.opts = opts;
      this.json = json;
    }

    @Override
    public Node transformUnary(UnaryExpr expr) {
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
      return super.transformUnary(expr);
    }
  }

  protected NodeTransformer newTransformer(
      UiGeneratorService service,
      UiComponentGenerator generator,
      GeneratedComponentMetadata container,
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
    return new DataTypeTransformer(opts, value, expr, read, write, container.getSourceBuilder()::addImport);
  }

  protected String printFactory(
      JsonContainerExpr json,
      GeneratedComponentMetadata container,
      Optional<AnnotationExpr> anno,
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

  protected DataTypeOptions getDatatypeFrom(ClassBuffer cb, GeneratedComponentMetadata metadata, JsonContainerExpr json) {
    return dataTypeOptions().fromAnnotation(cb, json, null, metadata.isSearchTypes());
  }

  protected DataTypeOptions getDatatypeFrom(ClassBuffer cb, GeneratedComponentMetadata metadata, JsonContainerExpr json, AnnotationExpr anno) {
    return dataTypeOptions().fromAnnotation(cb, json, anno, metadata.isSearchTypes());
  }

}
