import test.Test;
import com.google.gwt.core.shared.GWT;

import static com.google.gwt.core.shared.GWT.create;

public class ComplexTest extends Test {

  public void classLiteral() {
    GWT.create(Test.class);
  }

  public void finalFieldInitedByClassLiteral() {
    GWT.create(FINAL_FIELD_INITED_BY_CLASS_LITERAL);
  }

  public void nonFinalFieldInitedByClassLiteral() {
    GWT.create(NONFINAL_FIELD_INITED_BY_CLASS_LITERAL);
  }

  public void methodWithLiteralReturn() {
    GWT.create(withLiteralReturn());
  }

  public void methodWithFinalFieldReturn() {
    GWT.create(withFinalFieldReturn());
  }

}
