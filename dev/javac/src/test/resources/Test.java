import org.junit.Assert;
import xapi.annotation.api.XApi;
import xapi.inject.X_Inject;

import com.google.gwt.core.shared.GWT;

import javax.inject.Inject;

@XApi
public class Test {

  @Inject
  Test test;

  @Inject
  final Test withInitializer = newTest();

  private static Test newTest() {
    return X_Inject.instance(FINAL_FIELD_INITED_BY_CLASS_LITERAL);
  }

  public Test(){
  }
  public Test(Test param){
    // lets do a bunch of injection in this constructor.
    // It won't cause infinite recursion, as we won't inject this constructor.
    inject: { // Using an inject annotation should cause this null variable to become initialized
      Test test = null;
      assert test != null;
    }
    assert param != null;
    assert this.test != null;
  }

  static final Class<Test> FINAL_FIELD_INITED_BY_CLASS_LITERAL = Test.class;
  static Class<Test> NONFINAL_FIELD_INITED_BY_CLASS_LITERAL = Test.class;
  static Class<Test> withLiteralReturn() { return Test.class; }
  static Class<Test> withFinalFieldReturn() { return FINAL_FIELD_INITED_BY_CLASS_LITERAL; }
  static Class<Test> withNonFinalFieldReturn() { return NONFINAL_FIELD_INITED_BY_CLASS_LITERAL; }

  void classLiteral() {
    test(GWT.create(Test.class));
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

  void test(Test test) {
    Assert.assertEquals(test.getClass(), this.getClass());
  }
}
