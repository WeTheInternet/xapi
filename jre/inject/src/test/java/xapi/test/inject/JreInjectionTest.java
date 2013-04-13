package xapi.test.inject;

import org.junit.Test;

import xapi.test.AbstractInjectionTest;
import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.SingletonDefault;
import xapi.inject.X_Inject;
import static org.junit.Assert.assertNotNull;

public class JreInjectionTest extends AbstractInjectionTest{


  private interface ImportTestSingleton {
    void test();
  }
  private interface ImportTestInstance {
    void test();
  }

  @SingletonDefault(implFor=ImportSingleton.class)
  public static class ImportSingleton implements ImportTestSingleton {
    @Override
    public void test() {

    }
  }
  @InstanceDefault(implFor=ImportTestInstance.class)
  public static class ImportInstance implements ImportTestInstance {
    @Override
    public void test() {

    }
  }

	@Test
	public void testSingletonInjection(){
		ImportSingleton service = X_Inject.singleton(ImportSingleton.class);
		assertNotNull("Injector did not provide a singleton service", service);
		service.test();
	}
	@Test
	public void testInstanceInjection(){
		ImportInstance instance = X_Inject.instance(ImportInstance.class);
		assertNotNull("Injector did not provide an instance object", instance);
		instance.test();
	}

}
