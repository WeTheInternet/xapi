package xapi.test.gwt.reflect;

import static com.google.gwt.reflect.client.GwtReflect.magicClass;
import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.reflect.KeepClass;
import xapi.annotation.reflect.KeepField;
import xapi.annotation.reflect.KeepMethod;
import xapi.log.X_Log;

@KeepClass(debugData="")
@KeepField
@InstanceDefault(implFor=MagicClassTest.class)
public class MagicClassTest {

  String field;
  
  public MagicClassTest() {
    
  }
  @KeepMethod
  public void packageProtected(){}

  // This method is actually elided from the generator
  public void testCompleteEmulation() {
    Class<MagicClassTest> magicClass = magicClass(MagicClassTest.class);

//    assert X_Reflect.getPackage(MagicClassTest.class).getName()
//      .equals(MagicClassTest.class.getName());

    X_Log.info("Emulated package: ",magicClass.getPackage());
//    X_Log.info("Injected instance : ",X_Inject.instance(X_Reflect.magicClass(MagicClassTest.class)));

  }

}
