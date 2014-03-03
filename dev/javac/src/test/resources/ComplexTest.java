import com.google.gwt.core.shared.GWT;

import static com.google.gwt.core.shared.GWT.create;

public class ComplexTest extends Test {
  
  void classLiteral() {
    GWT.create(Test.class);
  }
  
  void finalFieldInitedByClassLiteral() {
    GWT.create(FINAL_FIELD_INITED_BY_CLASS_LITERAL);
  }
  
  void nonFinalFieldInitedByClassLiteral() {
    GWT.create(NONFINAL_FIELD_INITED_BY_CLASS_LITERAL);
  }
  
  void methodWithLiteralReturn() {
    GWT.create(withLiteralReturn());
  }
  
  void methodWithFinalFieldReturn() {
    GWT.create(withFinalFieldReturn());
  }

}