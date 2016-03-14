package xapi.javac.dev.model;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Type;
import xapi.javac.dev.api.JavacService;

import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

public class GwtCreateInvocationSite implements HasClassLiteralReference {

  private TypeMirror type;
  private ExpressionTree source;
  private final List<? extends ExpressionTree> args;
  private final ExpressionTree invoke;
  private final JavacService service;

  public GwtCreateInvocationSite(JavacService service, ExpressionTree arg, List<? extends ExpressionTree> args) {
    this.args = args;
    this.invoke = arg;
    this.service = service;
    setSource(arg);
  }

  public TypeMirror getType() {
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

    return type instanceof Type
        ? ((Type)type).tsym.flatName()
        : nameOf(source);
  }

  @Override
  public ExpressionTree getSource() {
    return source;
  }

  @Override
  public void setSource(ExpressionTree init) {
    source = init;
    type = service.findType(init);
    assert type != null : "Null type found for " + init.getClass()+" of " + init;
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
