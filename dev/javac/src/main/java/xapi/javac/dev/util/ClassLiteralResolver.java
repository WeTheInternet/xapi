package xapi.javac.dev.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import xapi.javac.dev.model.HasClassLiteralReference;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;

public class ClassLiteralResolver extends TreeScanner<Void, Void>{

  private final List<HasClassLiteralReference> variables;
  private final List<HasClassLiteralReference> invocations;
  private final Trees trees;
  boolean changed;
  
  public ClassLiteralResolver(Trees trees, List<? extends HasClassLiteralReference> items) {
    this.trees = trees;
    variables = new ArrayList<>();
    invocations = new ArrayList<>();
    items
      .forEach(r -> {
        if (!r.isResolved()) {
          Kind kind = r.getNodeKind();
          switch(kind) {
            case IDENTIFIER:
              variables.add(r);
              break;
            case MEMBER_SELECT:
              break;
            case METHOD_INVOCATION:
              invocations.add(r);
              break;
            default:
              System.out.println("Unhandled type found while trying to resolve class literal: "+kind);
          }
        }
      });
  }
  
  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, Void p) {
    changed = true;
    while (changed) {
      changed = false;
      super.visitCompilationUnit(node, p);
    }
    return p;
  }
  
  @Override
  public Void visitMethod(MethodTree node, Void p) {
    if (!invocations.isEmpty()) {
      invocations
      .stream()
      .filter(r -> r.getNodeName().equals(node.getName()))// TODO: also check parameter types
      .forEach(r -> {
        changed = true;
        List<? extends StatementTree> body = node.getBody().getStatements();
        body.removeIf(t -> !(t instanceof ReturnTree));
        assert body.size() > 0;
        if (body.size() == 1) {
          // Single return statement, much easier to deal with
          ReturnTree tree = (ReturnTree) body.get(0);
          r.setSource(tree.getExpression());
          ClassLiteralResolver resolve = new ClassLiteralResolver(trees, Arrays.asList(r));
          JCMethodDecl d = (JCMethodDecl)node;
          Symbol owner = d.sym.owner;
          resolve.scan(trees.getTree(owner), p);
        } else {
          // Multiple return statements...  things are about to get ugly
          throw new IllegalArgumentException("Method "+node+" is used to supply class literals, "
              + "but this method has more than one return statement: ");
        }
      });
    }
    return super.visitMethod(node, p);
  }
  
  @Override
  public Void visitVariable(VariableTree node, Void p) {
    if (!variables.isEmpty()) {
      variables
        .stream()
        .filter(r -> r.getNodeName().equals(node.getName()))
        .forEach(r -> {
          changed = true;
          ExpressionTree init = node.getInitializer();
          r.setSource(init);
        });
      
    }
    return super.visitVariable(node, p);
  }
}
