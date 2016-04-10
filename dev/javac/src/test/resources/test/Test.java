package test;

import org.junit.Assert;
import xapi.annotation.api.XApi;
import xapi.annotation.inject.InstanceDefault;
import xapi.inject.X_Inject;

import com.google.gwt.core.shared.GWT;

import javax.inject.Inject;

@XApi(
    templates = "./Generated"
)
@InstanceDefault(implFor = Test.class)
public class Test {

  public static final Class<Test> FINAL_FIELD_INITED_BY_CLASS_LITERAL = Test.class;
  @Inject
  Test test;

  public String world;

  @Inject
  final static Test withInitializer = newTest();

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

  public static Class<Test> NONFINAL_FIELD_INITED_BY_CLASS_LITERAL = Test.class;
  public static Class<Test> withLiteralReturn() { return Test.class; }
  public static Class<Test> withFinalFieldReturn() { return FINAL_FIELD_INITED_BY_CLASS_LITERAL; }
  public static Class<Test> withNonFinalFieldReturn() { return NONFINAL_FIELD_INITED_BY_CLASS_LITERAL; }

  public Test classLiteral() {
    return test(GWT.create(Test.class));
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

  public Test test(Test test) {
    Assert.assertEquals(test.getClass(), this.getClass());
    return test;
  }

  public String hello(String world) {
    this.world = world;
    return "hello: " + world;
  }
}
