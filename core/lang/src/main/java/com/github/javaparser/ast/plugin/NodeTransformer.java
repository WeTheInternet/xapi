package com.github.javaparser.ast.plugin;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.expr.UnaryExpr.Operator;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.except.NotImplemented;
import xapi.fu.*;
import xapi.source.X_Source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static xapi.collect.X_Collect.newStringMap;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/20/16.
 */
public class NodeTransformer {

  protected static class NodeGeneratorContext<Ctx extends NodeGeneratorContext<Ctx>> {
    Out1<ClassBuffer> source;

    NodeGeneratorContext(Out1<ClassBuffer> source) {
      this.source = source;
    }

    public String addImport(String importName) {
      return source.out1().addImport(importName);
    }

    public String addImportStatic(String cls, String staticName) {
      return source.out1().addImportStatic(cls, staticName);
    }

    protected final Ctx self() {
      return (Ctx) this;
    }
  }

  protected interface NodeGenerator<Ctx extends NodeGeneratorContext<Ctx>> {
    Node generateNode(Ctx ctx, Node originalNode, Node currentNode);
  }

  private static final String KEY_TRANSFORMER = "__tr@nsf0rmer__"; // purposely misspelled to avoid collisions
  private static final String KEY_SOURCE = "__s0urc3__"; // purposely misspelled to avoid collisions

  public static boolean hasTransformer(Node n) {
    return n.getExtra(KEY_TRANSFORMER) != null;
  }

  public static NodeTransformer getTransformer(Node n) {
    return n.getExtra(KEY_TRANSFORMER);
  }

  public static List<String> findTransformers(Node n) {
    final List<String> list = new ArrayList<>();

    n.accept(new VoidVisitorAdapter<Object>() {
      @Override
      public void visit(MethodCallExpr n, Object arg) {
        final NodeTransformer transformer = getTransformer(n);
        if (transformer != null) {
          final Expression source = n.getExtra(KEY_SOURCE);
          list.add(transformer.generateTransformer(source));
        }
        super.visit(n, arg);
      }
    }, null);

    return list;
  }

  private final Node newNode;
  protected final In2Out1<Node, Expression, Node> createRead;
  protected final In2Out1<Node, Expression, Node> createWrite;
  protected final In1Out1<String, String> addImport;
  protected final In2Out1<Node, Expression, Node> compute;
  protected final ClassBuffer out;

  private final StringTo<String> computeNames;
  private final StringTo<String> getterNames;
  private final StringTo<String> setterNames;
  private final StringTo<String> notifierNames;
  private final StringTo<PrintBuffer> notificationSites;

  public NodeTransformer(
      Node newNode,
      ClassBuffer out,
      In2Out1<Node, Expression, Node> createRead,
      In2Out1<Node, Expression, Node> createWrite,
      In1Out1<String, String> addImport
  ) {
    this(newNode, out, createRead, createWrite, createCompute(newNode, createRead, createWrite, addImport), addImport);
  }

  private static In2Out1<Node, Expression, Node> createCompute(
      Node newNode,
      In2Out1<Node, Expression, Node> createRead,
      In2Out1<Node, Expression, Node> createWrite,
      In1Out1<String, String> addImport
  ) {
    return (n, e) -> {
      String in2out1 = addImport.io(In2Out1.class.getName());
      MethodCallExpr createCompute = new MethodCallExpr(new NameExpr(in2out1), "computeKeyValueTransform");

      List<Expression> args = new ArrayList<>();
      args.add((Expression) n);
      args.add(e);
      MethodCallExpr compute = new MethodCallExpr(createCompute, "io", args);

      //      method.setArgs();
      //      Map<String, String> m = new HashMap<>();
      //      In3Out1<Map<String, String>, String, In1Out1<String, String>, String> f =
      //          (map, k, io) -> {
      //            String old = map.get(k);
      //            String computed = io.io(old);
      //            map.put(k, computed);
      //            return computed;
      //          };
      //
      //      //    In3Out1<Map<String, String>, String, In1Out1<String, String>, String>
      //      //        computer =
      //      In2Out1.computeKeyValueTransform(m, Map::get, Map::put)
      //          .io("s", (k, v)->v == null ? "" : v+1);

      return null;
    };
  }

  public NodeTransformer(
      Node newNode,
      ClassBuffer out,
      In2Out1<Node, Expression, Node> createRead,
      In2Out1<Node, Expression, Node> createWrite,
      In2Out1<Node, Expression, Node> compute,
      In1Out1<String, String> addImport
  ) {
    this.newNode = newNode;
    this.createRead = createRead;
    this.createWrite = createWrite;
    this.compute = compute;
    this.addImport = addImport;
    this.out = out;
    computeNames = newStringMap(String.class);
    getterNames = newStringMap(String.class);
    setterNames = newStringMap(String.class);
    notifierNames = newStringMap(String.class);
    notificationSites = newStringMap(MethodBuffer.class);
  }


  public Node transformBinary(Expression source, BinaryExpr expr) {
    if (expr.getLeft() instanceof FieldAccessExpr) {
      FieldAccessExpr field = (FieldAccessExpr) expr.getLeft();
      if (field.getScope() == newNode) {
        String get = generateGetterMethod(field);
        MethodCallExpr invoke = new MethodCallExpr();
        invoke.setName(get);
        expr.setLeft(invoke);
        initExtras(invoke, field);
      }
    }
    if (expr.getRight() instanceof FieldAccessExpr) {
      FieldAccessExpr field = (FieldAccessExpr) expr.getRight();
      if (field.getScope() == newNode) {
        String get = generateGetterMethod(field);
        MethodCallExpr invoke = new MethodCallExpr();
        invoke.setName(get);
        expr.setRight(invoke);
        initExtras(invoke, field);
      }
    }
    return expr;
  }

  public Node transformUnary(Expression source, UnaryExpr expr) {
    final Expression data = expr.getExpr();
    switch (expr.getOperator()) {
      // x++
      case posIncrement:
      // x--
      case posDecrement:
        // For post increment / decrement,
        // we want to store the de/incremented value,
        // but use the current value.
        // So, we will generate setX(getX() + 1),
        // as the setX function returns the previous value
        final String setter = generateSetterMethod(data);
        final String getter = generateGetterMethod(data);
        MethodCallExpr methodCall = new MethodCallExpr();
        methodCall.setName(setter);
        MethodCallExpr get = new MethodCallExpr();
        get.setName(getter);
        final BinaryExpr.Operator operator = expr.getOperator() == Operator.posIncrement ? BinaryExpr.Operator.plus : BinaryExpr.Operator.minus;
        BinaryExpr op = new BinaryExpr(get, new IntegerLiteralExpr("1"), operator);
        methodCall.setArgs(Arrays.asList(op));
        initExtras(methodCall, data);
        return methodCall;
      // ++x
      // --x
      case preIncrement:
      case preDecrement:
        // For pre increment / decrement,
        // we want to de/increment the value,
        // and use the updated value immediately
        // So, we will use computeX_1(i -> i++)
        // TODO: Have a global INCREMENT function we can just reference instead
        String compute = generateComputeMethod(data) + "_1";
        List<Parameter> params = new ArrayList<>();
        Type type = new UnknownType();
        VariableDeclaratorId id = new VariableDeclaratorId("i");
        params.add(new Parameter(type, id));
        UnaryExpr copy = new UnaryExpr(new NameExpr("i"), expr.getOperator());
        Statement body = new ExpressionStmt(copy);
        LambdaExpr lambda = new LambdaExpr(params, body, false);
        methodCall = new MethodCallExpr();
        methodCall.setName(compute);
        methodCall.setArgs(Arrays.asList(lambda));
        initExtras(methodCall, data);
        return methodCall;
      case inverse:
      case negative:
      case not:
      case positive:
        throw new NotImplemented("Need to implement unary operator " + expr.getOperator() + " in " + getClass());
    }
        return expr;
//    return new StringLiteralExpr(expr.toSource());
  }

  // If you change the MethodCallExpr parameter to a weaker type,
  // do be sure to update the visitor used in the findNotifiers method to match!
  protected void initExtras(MethodCallExpr node, Expression data) {
    node.addExtra(KEY_TRANSFORMER, this);
    node.addExtra(KEY_SOURCE, data);
  }

  protected String generateComputeMethod(Expression expr) {
    String computeName;
    if (expr instanceof FieldAccessExpr) {
      final FieldAccessExpr field = (FieldAccessExpr) expr;
      if (field.getScope() == newNode) {
        computeName = computeNames.get(field.getField());
        if (computeName == null) {
          String keyName = X_Source.toCamelCase(field.getField());
          String datatype = out.addImport(getDataType(field.getField()));
          datatype = X_Source.primitiveToObject(datatype);
          String in1out1 = out.addImport(In1Out1.class);
          boolean isMapType = isMapType(expr);
          computeName = "compute" + keyName;
          String getter = generateGetterMethod(expr);
          String setter = generateSetterMethod(expr);
          out.createMethod("public " + datatype + " " + computeName + "_1")
              .addParameter(in1out1 + "<" + datatype + ", " + datatype + ">", "io")
              .println(datatype + " original = " + getter + "();")
              .println(datatype + " computed = io.io(original);")
              .println(setter + "(computed);")
              .returnValue("computed");
          if (isMapType) {
            String in2out1 = out.addImport(In2Out1.class);
            String keyType = getKeyType(expr);
            String escaped = escapedKey(keyType, field.getField());
            out.createMethod("public " + datatype + " " + computeName + "_2")
                .addParameter(in2out1 + "<" + keyType + ", " + datatype + ", " + datatype + ">", "io")
                .println(datatype + " original = " + getter + "();")
                .println(datatype + " computed = io.io( " + escaped + ", original);")
                .println(setter + "(computed);")
                .returnValue("computed");
          }
          computeNames.put(field.getField(), computeName);
        }
        return computeName;
      }
    }
    throw new NotImplemented("Unable to extract a compute method for expression " + expr);
  }

  protected String generateTransformer(Expression expr) {
    String notifierName;
    if (expr instanceof FieldAccessExpr) {
      final String fieldName = getFieldName(expr);
      notifierName = notifierNames.get(fieldName);
      if (notifierName == null) {
        String datatype = out.addImport(getDataType(fieldName));
        datatype = X_Source.primitiveToObject(datatype);
        generateSetterMethod(expr); // will always need a setter inited to ensure notifier has somewhere to act
        notifierName = "notify" + X_Source.toCamelCase(fieldName);
        notifierNames.put(fieldName, notifierName);

        String lazy = out.addImport(Lazy.class);
        String notifier = out.addImport(Notifier.class);
        out.createField(lazy+"<" + notifier + "<" + datatype + ">>", notifierName)
            .setInitializer(lazy+".deferred1(" + notifier +"::new);");

        // Add a hook in the setter method to trigger notifications.
        final PrintBuffer notificationSite = notificationSites.get(fieldName);
        if (notificationSite.isEmpty()) {
          boolean ifGaurd = isOnlyNotifyChanges(expr);
          if (ifGaurd) {
            String objects = out.addImport(Objects.class);
            notificationSite.println("if (!" + objects +".equals(oldVal, val)) {");
            notificationSite.indent();
          }
          notificationSite.println(notifierName+".out1().notifyListeners(oldVal, val);");
          if (ifGaurd) {
            notificationSite.outdent();
            notificationSite.println("}");
          }
        }
      }
      return notifierName;
    }
    throw new NotImplemented("Unable to generate a transformer for expression " + expr);
  }

  protected boolean isOnlyNotifyChanges(Expression expr) {
    return true;
  }

  protected String generateGetterMethod(Expression expr) {
    String getterName;
    if (expr instanceof FieldAccessExpr) {
      final FieldAccessExpr field = (FieldAccessExpr) expr;
      getterName = getterNames.get(field.getField());
      if (getterName == null) {
        if (field.getScope() == newNode) {
          String keyName = X_Source.toCamelCase(field.getField());
          String datatype = out.addImport(getDataType(field.getField()));
          datatype = X_Source.primitiveToObject(datatype);
          getterName = "get" + keyName;
          final MethodBuffer getter = out.createMethod("public " + datatype + " " + getterName);
          String get = nodeGetMethod(expr);
          String keyType = getKeyType(expr);
          String escapeKey = escapedKey(keyType, field.getField());

          boolean cast = needsCast(expr, field.getField(), datatype);
          getter.returnValue((cast ? "(" + datatype + ")" : "") + nodeSource() + "." + get + "(" + escapeKey + ")");
        } else {
          throw new NotImplemented("Unable to extract a getter method for expression " + expr);
        }
        getterNames.put(field.getField(), getterName);
      }
      return getterName;
    } else if (expr instanceof QualifiedNameExpr) {
      final QualifiedNameExpr name = (QualifiedNameExpr) expr;
      getterName = getterNames.get(name.getSimpleName());
      if (getterName == null) {
        String keyName = X_Source.toCamelCase(name.getSimpleName());
        String datatype = out.addImport(getDataType(name.getSimpleName()));
        datatype = X_Source.primitiveToObject(datatype);
        getterName = "get" + keyName;
        final MethodBuffer getter = out.createMethod("public " + datatype + " " + getterName);
        String get = nodeGetMethod(expr);
        String keyType = getKeyType(expr);
        String escapeKey = escapedKey(keyType, name.getSimpleName());

        boolean cast = needsCast(expr, keyType, datatype);
        getter.returnValue((cast ? "(" + datatype + ")" : "") + nodeSource() + "." + get + "(" + escapeKey + ")");
      }
      getterNames.put(name.getSimpleName(), getterName);
      return getterName;
    }
    throw new NotImplemented("Unable to extract a getter method for expression " + expr);
  }

  protected String generateSetterMethod(Expression expr) {
    String setterName;
    if (expr instanceof FieldAccessExpr) {
      final FieldAccessExpr field = (FieldAccessExpr) expr;
      setterName = setterNames.get(field.getField());
      if (setterName == null) {
        if (field.getScope() == newNode) {
          String keyName = X_Source.toCamelCase(field.getField());
          String datatype = out.addImport(getDataType(field.getField()));
          datatype = X_Source.primitiveToObject(datatype);
          setterName = "set" + keyName;
          final MethodBuffer setter = out.createMethod("public " + datatype + " " + setterName)
              .addParameter(datatype, "val");
          String set = nodeSetMethod(expr);
          String keyType = getKeyType(expr);
          String escapeKey = escapedKey(keyType, field.getField());
          setter.print(datatype + " oldVal = ");
          if (needsCast(expr, field.getField(), datatype)) {
            setter.print("(" + datatype + ")");
          }
          setter.println(nodeSource() + "." + set + "(" + escapeKey + ", val);");
          // Add a printbuffer that we can optionally add a notifier for
          final PrintBuffer buffer = new PrintBuffer(setter.getIndentCount());
          setter.addToEnd(buffer);
          notificationSites.put(field.getField(), buffer);
          setter.returnValue("oldVal");
        } else {
          throw new NotImplemented("Unable to extract a setter method for expression " + expr);
        }
        setterNames.put(field.getField(), setterName);
      }
      return setterName;
    }
    throw new NotImplemented("Unable to extract a setter method for expression " + expr);
  }

  protected String getFieldName(Expression expr) {

    if (expr instanceof FieldAccessExpr) {
      final FieldAccessExpr field = (FieldAccessExpr) expr;
      if (field.getScope() == newNode) {
        return field.getField();
      }
    }
    throw new NotImplemented("Unable to extract a field name for expression " + expr);
  }

  protected boolean needsCast(Expression expr, String field, String datatype) {
    return !"Object".equals(datatype);
  }

  protected String escapedKey(String keyType, String field) {
    switch (keyType) {
      case "String":
      case "java.lang.String":
        return "\"" + X_Source.escape(field) + "\"";
      case "int":
      case "Integer":
      case "java.lang.Integer":
        return field;
      default:
        throw new IllegalArgumentException("Unknown keyType " + keyType);
    }
  }

  protected String nodeGetMethod(Expression expr) {
    return "get";
  }

  protected String nodeSetMethod(Expression expr) {
    return isMapType(expr) ? "put" : "set";
  }

  protected String nodeSource() {
    return newNode.toSource();
  }

  protected String getKeyType(Expression expr) {
    return isMapType(expr) ? "String" : "int";
  }

  protected boolean isMapType(Expression expr) {
    return expr instanceof FieldAccessExpr || expr instanceof QualifiedNameExpr;
  }

  protected String getDataType(String keyName) {
    return "Object";
  }

  public Node transformAssignExpr(AssignExpr expr) {
    return expr;
  }

  public Node transformName(NameExpr expr) {
    String get = generateGetterMethod(expr);
    MethodCallExpr invoke = new MethodCallExpr();
    invoke.setName(get);
    initExtras(invoke, expr);
    return invoke;
  }

  public Node transformArrayAccess(ArrayAccessExpr expr) {
    return expr;
  }

  public Node getNode() {
    return newNode;
  }
}
