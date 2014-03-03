package xapi.javac.dev.model;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Name;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import xapi.javac.dev.util.SearchFor;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;

public class GwtCreateInvocationSite implements HasClassLiteralReference {

  private Type type;
  private ExpressionTree source;
  private final List<? extends ExpressionTree> args;
  private final ExpressionTree invoke;

  public GwtCreateInvocationSite(ExpressionTree arg, List<? extends ExpressionTree> args) {
    this.args = args;
    this.invoke = arg;
    setSource(arg);
  }

  public Type getType() {
    return type;
  }
  
  @Override
  public Kind getNodeKind() {
    return source == null
        ? Kind.MEMBER_REFERENCE // class literal
        : source.getKind(); // anything else
  }
  
  @Override
  public Name getNodeName() {
    return type != null
        ? type.tsym.flatName()
        : nameOf(source);
  }
  
  @Override
  public ExpressionTree getSource() {
    return source;
  }
  
  @Override
  public void setSource(ExpressionTree init) {
    source = init;
    type = SearchFor.classLiteral(init);
  }

  private Name nameOf(ExpressionTree source) {
    if (source instanceof IdentifierTree) {
      return ((IdentifierTree)source).getName();
    } else if (source instanceof MethodInvocationTree){
      ExpressionTree select = ((MethodInvocationTree)source).getMethodSelect();
      return ((IdentifierTree)select).getName();
    } else {
      System.out.println("Unhandled type sent to nameOf(): "+source.getClass());
    }
    return null;
  }

  public List<? extends ExpressionTree> getArgs() {
    return new ArrayList<>(args);// Maintain immutability
  }

  @Override
  public String toString() {
    return "GWT.create("+(type == null ? source : type).toString()+".class)";
  }

  public boolean isResolved() {
    return type != null;
  }

  public ExpressionTree getInvocation() {
    return invoke;
  }
  
}
