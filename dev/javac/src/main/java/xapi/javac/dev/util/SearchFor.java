package xapi.javac.dev.util;

import javax.lang.model.element.Modifier;
import javax.lang.model.util.Elements;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.List;

public class SearchFor {

  public static Type classLiteral(ExpressionTree arg) {
    if (arg.getKind() == Kind.MEMBER_SELECT) { // Field access
        MemberSelectTree member = (MemberSelectTree) arg;
        assert member instanceof JCFieldAccess : "Member "+member+" must be a JCFieldAccess";
        JCFieldAccess asField = (JCFieldAccess)member;
        if (member.getIdentifier().contentEquals("class")) {
          // must be a field access
          assert asField.type.asElement().flatName().contentEquals("java.lang.Class");
          // Grab the type parameter off the class literal
          List<Type> params = asField.type.allparams();
          return asField.type.allparams().last();
        }
    }
    return null;
  }

}
