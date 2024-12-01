import test.Test;
import com.google.gwt.core.shared.GWT;

import static com.google.gwt.core.shared.GWT.create;

public class ComplexTest extends Test {

  public Test classLiteral() {
    return GWT.create(Test.class);
  }

  public Test finalFieldInitedByClassLiteral() {
    return GWT.create(FINAL_FIELD_INITED_BY_CLASS_LITERAL);
  }

  public Test nonFinalFieldInitedByClassLiteral() {
    return GWT.create(NONFINAL_FIELD_INITED_BY_CLASS_LITERAL);
  }

  public Test methodWithLiteralReturn() {
    return GWT.create(withLiteralReturn());
  }

  public Test methodWithFinalFieldReturn() {
    return GWT.create(withFinalFieldReturn());
  }

}
