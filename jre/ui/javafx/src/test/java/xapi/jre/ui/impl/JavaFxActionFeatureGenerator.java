package xapi.jre.ui.impl;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.plugin.NodeTransformer;
import com.github.javaparser.ast.visitor.ConcreteModifierVisitor;
import com.github.javaparser.ast.visitor.ModifierVisitorAdapter;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.ui.ComponentMetadataFinder;
import xapi.dev.ui.ComponentMetadataQuery;
import xapi.dev.ui.GeneratedComponentMetadata;
import xapi.dev.ui.UiComponentGenerator;
import xapi.dev.ui.UiFeatureGenerator;
import xapi.dev.ui.UiGeneratorService;
import xapi.fu.Out1;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/19/16.
 */
public class JavaFxActionFeatureGenerator extends UiFeatureGenerator {

  @Override
  public boolean startVisit(
      UiGeneratorService service, UiComponentGenerator generator, GeneratedComponentMetadata me, UiAttrExpr n) {
    final Expression expr = n.getExpression();
    if (expr instanceof LambdaExpr) {

      LambdaExpr body = (LambdaExpr) expr;
      MethodBuffer into = me.getParentMethod();
      Map<Node, Out1<Node>> replacements = new IdentityHashMap<>();
      body.accept(new ComponentMetadataFinder(),
          new ComponentMetadataQuery()
              .addNameListener((graph, name) -> {
                String replacement;
                switch (name.getName()) {
                  case "$root":
                    replacement = me.getRootReference();
                    break;
                  default:
                    if (name.getName().startsWith("$")) {
                      replacement = name.getName().substring(1);
                    } else {
                      replacement = null;
                    }
                }
                String ref = replacement;
                name.setName(ref);
                name.getParentNode().getParentNode().accept(new ModifierVisitorAdapter<Object>(){
                  @Override
                  public Node visit(
                      FieldAccessExpr n, Object arg
                  ) {
                    String var = n.getField();
                    NodeTransformer newNode = me.findReplacement(ref, var);
                    if (newNode != null) {
                      // If this node is the qualifier on a field access,
                      // then we may want to perform additional transformations...
                      if (n.getParentNode() instanceof FieldAccessExpr) {
                        // A field access may be shorthand notation for a map access...
                        // The data field was a qualifier of a field access...
                        FieldAccessExpr parent = (FieldAccessExpr) n.getParentNode();
                        if (parent.getParentNode() instanceof UnaryExpr) {
                          // A + - ++ -- ! or ~ expression.  We will replace this with a compute call
                          // if one is available...
                          UnaryExpr toReplace = (UnaryExpr) parent.getParentNode();
                          // ++ and -- must be handled specially, as they perform
                          // a read and a write
                          replacements.put(toReplace, ()->newNode.transformUnary(toReplace));
                        } else if (parent.getParentNode() instanceof BinaryExpr) {
                          // A && || = > < >= <= etc binary expression;
                          // These are safe to replace as simple get operations,
                          // as they do not perform assignment
                          BinaryExpr toReplace = (BinaryExpr) parent.getParentNode();
                          final Node result = newNode.transformBinary(toReplace);
                          replacements.put(toReplace, ()->newNode.transformBinary(toReplace));
                        } else if (parent.getParentNode() instanceof AssignExpr) {
                          AssignExpr toReplace = (AssignExpr) parent.getParentNode();
                          // A plain = assignment will be transformed into a write,
                          // while all other assignment, += -= etc will need to read and write
                          final Node result = newNode.transformAssignExpr(toReplace);
                          replacements.put(toReplace, ()->newNode.transformAssignExpr(toReplace));
                        }
                      } else if (n.getParentNode() instanceof ArrayAccessExpr) {
                        // An array access may be shorthand notation for a list access
                          ArrayAccessExpr toReplace = (ArrayAccessExpr) n.getParentNode();
                          final Node result = newNode.transformArrayAccess(toReplace);
                          replacements.put(toReplace, ()->newNode.transformArrayAccess(toReplace));
                      }
                      return newNode.getNode();
                    }
                    return super.visit(n, arg);
                  }
                }, null);

              })
      );
      if (!replacements.isEmpty()) {
        ConcreteModifierVisitor.replaceResolved(replacements);
      }
      String target = me.peekPanelName();
      final PrintBuffer buffer = new PrintBuffer();
      into.addToEnd(buffer);
      buffer.println(target + ".setOnAction(")
          .indent()
          .printlns(body.toSource(generator.getTransformer()))
          .outdent()
          .println(");");

    } else {
      throw new IllegalStateException("Illegal argument type " + expr + " found in action");
    }
    return false;
  }
}
