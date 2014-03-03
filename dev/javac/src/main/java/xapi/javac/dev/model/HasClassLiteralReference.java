package xapi.javac.dev.model;

import javax.lang.model.element.Name;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree.Kind;

public interface HasClassLiteralReference {

  boolean isResolved();
  
  Kind getNodeKind();

  Name getNodeName();
  
  ExpressionTree getSource();

  void setSource(ExpressionTree init);

}
