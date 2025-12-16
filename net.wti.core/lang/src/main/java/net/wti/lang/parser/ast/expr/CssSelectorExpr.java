package net.wti.lang.parser.ast.expr;

import net.wti.lang.parser.ast.visitor.GenericVisitor;
import net.wti.lang.parser.ast.visitor.VoidVisitor;

import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class CssSelectorExpr extends CssExpr {

  private final List<String> parts;
  private boolean frozen;

  public CssSelectorExpr(
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn,
      List<String> parts
  ) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.parts = parts;
  }

  public List<String> getParts() {
    return parts;
  }

  public void setParts(List<String> parts) {
    // Ugh.  This changes the hashcode...
    assert !frozen : "You are mutating a CssSelectorExpr after its hashCode method has been accessed.";
    this.parts.clear();
    this.parts.addAll(parts);
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    return v.visit(this, arg);
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    v.visit(this, arg);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof CssSelectorExpr))
      return false;
    if (!super.equals(o))
      return false;

    final CssSelectorExpr that = (CssSelectorExpr) o;

    return parts.equals(that.parts);

  }

  @Override
  public int hashCode() {
    assert (frozen = true); // do not allow mutations after the hashCode has been accessed.
    int result = super.hashCode();
    result = 31 * result + parts.hashCode();
    return result;
  }

  public String joinParts() {
    StringBuilder b = new StringBuilder();
    String prefix = "";
    for (String part : parts) {
      if (part.startsWith(":") && !part.startsWith("::")) {
        b.append(part);
      } else {
        b.append(prefix).append(part);
      }
      prefix = " ";
    }
    return b.toString();
  }
}
