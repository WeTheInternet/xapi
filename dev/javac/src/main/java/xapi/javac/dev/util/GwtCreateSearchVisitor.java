package xapi.javac.dev.util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.model.GwtCreateInvocationSite;

import java.util.List;

/**
 * Searches for calls to GWT.create within a compilation unit
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class GwtCreateSearchVisitor extends TreePathScanner<List<GwtCreateInvocationSite>, List<GwtCreateInvocationSite>>{

  private final Context context;

  private JavacService service () {
    return JavacService.instanceFor(context);
  }

  public GwtCreateSearchVisitor(Context context) {
    this.context = context;
  }

  @Override
  public List<GwtCreateInvocationSite> visitMethodInvocation(MethodInvocationTree node, List<GwtCreateInvocationSite> list) {
    assert node.getMethodSelect() instanceof JCTree : "All ExpressionTree implements must extend JCTree;"
        + " you sent a "+node.getMethodSelect().getClass()+" : "+node.getMethodSelect();
    super.visitMethodInvocation(node, list);
    JCTree select = (JCTree) node.getMethodSelect();
    if (TreeInfo.name(select).contentEquals("create")) {
      Symbol symbol = TreeInfo.symbol(select);
      if (
          // Breaking the Law of Demeter = :(
          symbol.owner.name.contentEquals("GWT") // Match ANY class named GWT
          // We could use symbol.flatName() to match client+shared GWT,
          // but this lazy namimg allows end users to create their own specialized versions of GWT.create
          ) {
        // We've found a call to GWT.create!
        List<? extends ExpressionTree> args = node.getArguments();
        list.add(0, new GwtCreateInvocationSite(
            service(),
            args.get(0),
            args.subList(1, args.size())
        ));
      }
    }
    return list;
  }
}
