package xapi.javac.dev.util;

import java.util.List;

import javax.lang.model.util.Elements;

import xapi.javac.dev.model.GwtCreateInvocationSite;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;

/**
 * Searches for calls to GWT.create within a compilation unit
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class GwtCreateSearchVisitor extends TreePathScanner<List<GwtCreateInvocationSite>, List<GwtCreateInvocationSite>>{

  public GwtCreateSearchVisitor() {
  }
  
  @Override
  public List<GwtCreateInvocationSite> visitMethodInvocation(MethodInvocationTree node, List<GwtCreateInvocationSite> list) {
    assert node.getMethodSelect() instanceof JCTree : "All ExpressionTree implements must extend JCTree;"
        + " you sent a "+node.getMethodSelect().getClass()+" : "+node.getMethodSelect();
    super.visitMethodInvocation(node, list);
    JCTree select = (JCTree) node.getMethodSelect();
    JCTree asTree = (JCTree) select;
    if (TreeInfo.name(asTree).contentEquals("create")) {
      Symbol symbol = TreeInfo.symbol(asTree);
      if (
          // Breaking the Law of Demeter = :(
          symbol.owner.name.contentEquals("GWT") // Match ANY class named GWT
          // We could use symbol.flatName() to match client+shared GWT,
          // but this lazy namimg allows end users to create their own specialized versions of GWT.create
          ) {
        // We've found a call to GWT.create!
        List<? extends ExpressionTree> args = node.getArguments();
        list.add(0, new GwtCreateInvocationSite(
            args.get(0), 
            args.subList(1, args.size())
        ));
      }
    }
    return list;
  }
}
