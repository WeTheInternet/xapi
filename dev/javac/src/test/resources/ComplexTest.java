import com.google.gwt.core.shared.GWT;

import static com.google.gwt.core.shared.GWT.create;

public class ComplexTest extends Test {
  
  void classLiteral() {
    GWT.create(Test.class);
  }
  
  void finalFieldInitedByClassLiteral() {
    create(FINAL_FIELD_INITED_BY_CLASS_LITERAL);
  }
  
  void nonFinalFieldInitedByClassLiteral() {
    create(NONFINAL_FIELD_INITED_BY_CLASS_LITERAL);
  }
  
  void methodWithLiteralReturn() {
    create(withLiteralReturn());
  }
  
  void methodWithFinalFieldReturn() {
    create(withFinalFieldReturn());
  }

}