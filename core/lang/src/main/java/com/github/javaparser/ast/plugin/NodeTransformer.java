package com.github.javaparser.ast.plugin;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import xapi.dev.source.ClassBuffer;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Out1;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/20/16.
 */
public class NodeTransformer {

  protected static class NodeGeneratorContext <Ctx extends NodeGeneratorContext<Ctx>> {
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

  protected interface NodeGenerator <Ctx extends NodeGeneratorContext<Ctx>> {
    Node generateNode(Ctx ctx, Node originalNode, Node currentNode);
  }

  private final Node newNode;
  protected final In2Out1<Node, Expression, Node> createRead;
  protected final In2Out1<Node, Expression, Node> createWrite;
  protected final In1Out1<String, String> addImport;
  protected final In2Out1<Node, Expression, Node> compute;

  public NodeTransformer(Node newNode,
                         In2Out1<Node, Expression, Node> createRead,
                         In2Out1<Node, Expression, Node> createWrite,
                         In1Out1<String, String> addImport
  ) {
    this(newNode, createRead, createWrite, createCompute(newNode, createRead, createWrite, addImport), addImport);
  }

  private static In2Out1<Node,Expression,Node> createCompute(
      Node newNode,
      In2Out1<Node, Expression, Node> createRead,
      In2Out1<Node, Expression, Node> createWrite,
      In1Out1<String, String> addImport
  ) {
    return (n, e) -> {
      String in2out1 = addImport.io(In2Out1.class.getName());
      MethodCallExpr createCompute = new MethodCallExpr(new NameExpr(in2out1), "computeKeyValueTransform");

      List<Expression> args = new ArrayList<>();
      args.add((Expression)n);
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

  public NodeTransformer(Node newNode,
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
  }

  public Node transformUnary(UnaryExpr expr) {
    switch (expr.getOperator()) {
      case posDecrement:
      case posIncrement:

      case preIncrement:
      case preDecrement:
      case inverse:
      case negative:
      case not:
      case positive:
    }
//    return expr;
    return new StringLiteralExpr(expr.toSource());
  }

  public Node transformBinary(BinaryExpr expr) {
    return expr;
  }

  public Node transformAssignExpr(AssignExpr expr) {
    return expr;
  }

  public Node transformArrayAccess(ArrayAccessExpr expr) {
    return expr;
  }

  public Node getNode() {
    return newNode;
  }
}
