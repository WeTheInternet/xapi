package xapi.javac.dev.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import xapi.javac.dev.model.HasClassLiteralReference;
import xapi.javac.dev.plugin.ClassWorldPlugin;
import xapi.log.X_Log;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;

public class ClassLiteralResolver extends TreeScanner<Void, Void>{

  private final List<HasClassLiteralReference> variables;
  private final List<HasClassLiteralReference> invocations;
  private final List<HasClassLiteralReference> onComplete;
  private final ClassWorldPlugin classWorld;
  private String currentUnit;
  int depth = 0;
  
  public ClassLiteralResolver(List<? extends HasClassLiteralReference> items, ClassWorldPlugin classWorld) {
    this.classWorld = classWorld;
    onComplete = new ArrayList<>();
    variables = new ArrayList<>();
    invocations = new ArrayList<>();
    items
      .forEach(r -> {
        add(r);
      });
  }
  
  private void add(HasClassLiteralReference r) {
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
          X_Log.warn(getClass(), "Unhandled type found while trying to resolve class literal: "+kind);
      }
    }
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, Void p) {
    currentUnit = NameUtil.getName((JCCompilationUnit)node);
    super.visitCompilationUnit(node, p);
    final Set<String> pending = new HashSet<>();
    boolean[] done = new boolean[]{true};
    variables.removeIf(r -> r.isResolved());
    invocations.removeIf(r -> r.isResolved());
    List<HasClassLiteralReference> all = new ArrayList<>();
    all.addAll(variables);
    all.addAll(invocations);
    variables.clear();
    invocations.clear();
    all.forEach(r -> {
      JCIdent ident;
      switch (r.getNodeKind()) {
      case MEMBER_SELECT:
        return;
      case IDENTIFIER:
        ident = (JCIdent) r.getSource();
        break;
      case METHOD_INVOCATION:
        JCMethodInvocation method = (JCMethodInvocation)r.getSource();
        ident = (JCIdent) method.getMethodSelect();
        break;
      default:
        X_Log.warn(getClass(), "Unhandled node type ",r.getNodeKind(),"in",r);
        return;
      }
      add(r);
      String owner = ident.sym.owner.name.toString();
      if (owner.equals(currentUnit)) {
        done[0] = false;
      } else {
        if (pending.add(owner)) {
          classWorld.onCompilationUnitFinished(owner, jcu -> scan(jcu, null));
        }
      }
    });
    if (!done[0]) {
      if (depth ++ > 50) {
        throw new IllegalStateException("Max recursion depth of 50 has been exceeded");
      }
      visitCompilationUnit(node, p);
    }
    return null;
  }
  
  @Override
  public Void visitMethod(MethodTree node, Void p) {
    if (!invocations.isEmpty()) {
      invocations
      .stream()
      .filter(r -> {
        if (r.getNodeKind() != Kind.METHOD_INVOCATION) { return false; }
        JCMethodInvocation method = (JCMethodInvocation)r.getSource();
        JCIdent select = (JCIdent) method.getMethodSelect();
        String owner = select.sym.owner.name.toString();
        return owner.equals(currentUnit) && NameUtil.equals(r.getNodeName(), node.getName());
      }
      )// TODO: also check parameter types
      .forEach(r -> {
        List<? extends StatementTree> body = new ArrayList<>(node.getBody().getStatements());
        body.removeIf(t -> !(t instanceof ReturnTree));
        assert body.size() > 0;
        if (body.size() == 1) {
          // Single return statement, much easier to deal with
          ReturnTree tree = (ReturnTree) body.get(0);
          r.setSource(tree.getExpression());
        } else {
          // Multiple return statements...  things are about to get ugly
          throw new IllegalArgumentException("Method "+node+" is used to supply class literals, "
              + "but this method does not have exactly one return statement: "+body);
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
        .filter(r -> {
          if (r.getNodeKind() != Kind.IDENTIFIER) { return false; }
          JCIdent select = (JCIdent) r.getSource();
          String owner = select.sym.owner.name.toString();
          return owner.equals(currentUnit) && NameUtil.equals(r.getNodeName(),node.getName());
        }
        )
        .forEach(r -> {
          ExpressionTree init = node.getInitializer();
          r.setSource(init);
        });
    }
    return super.visitVariable(node, p);
  }
}
