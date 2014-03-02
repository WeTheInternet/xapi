import com.google.gwt.core.shared.GWT;

public class Test {
  static final Class<Test> FINAL_FIELD_INITED_BY_CLASS_LITERAL = Test.class;
  static Class<Test> NONFINAL_FIELD_INITED_BY_CLASS_LITERAL = Test.class;
  static Class<Test> withLiteralReturn() { return Test.class; }
  static Class<Test> withFinalFieldReturn() { return FINAL_FIELD_INITED_BY_CLASS_LITERAL; }
  static Class<Test> withNonFinalFieldReturn() { return NONFINAL_FIELD_INITED_BY_CLASS_LITERAL; }
  
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