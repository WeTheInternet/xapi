package xapi.test.gwt.reflect;

import static xapi.log.X_Log.error;
import static xapi.log.X_Log.trace;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import xapi.annotation.reflect.KeepClass;
import xapi.annotation.reflect.KeepConstructor;
import xapi.annotation.reflect.KeepField;
import xapi.annotation.reflect.KeepMethod;
import xapi.inject.X_Inject;
import xapi.reflect.X_Reflect;
import xapi.test.Assert;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;


@KeepConstructor
@KeepMethod
@KeepField
@KeepClass(
  keepPackage = true,
  keepCodeSource = true
  )
public class MagicClassEntryPoint implements EntryPoint{

  //this boolean will only be set if everything else succeeds.
  private boolean passed = false;

  boolean ctor0 = false;
  public MagicClassEntryPoint() {
    ctor0 = true;
    trace("Empty ctor");
  }

  private static boolean ctor1 = false;
  public MagicClassEntryPoint(boolean b, byte B, short s, char c,
    int i, long l, float f, double d) {
    ctor1 = true;
    trace("Primitive ctor",b,B,c,s,i,l,f,d);
  }

  protected static boolean ctor2 = false;
  public MagicClassEntryPoint(Boolean b, Byte B, Short s, Character c,
    Integer i, Long l, Float f, Double d) {
    ctor2 = true;
    trace("Object ctor",b,B,c,s,i,l,f,d);
  }

  protected static boolean ctor3 = false;
  public MagicClassEntryPoint(String str, int i, Integer I, Object o
    , JavaScriptObject jso, float f, Float F, short s, Short S) {
    ctor3 = true;
    trace("ctor w/ params",str,i,I,o);
  }

  public void onModuleLoad() {
    Class<MagicClassEntryPoint> cls = X_Reflect.magicClass(MagicClassEntryPoint.class);
    //test the big ugly ctor
    try {
    Constructor<MagicClassEntryPoint> ctor = cls.getConstructor(String.class, int.class, Integer.class,
      Object.class, JavaScriptObject.class, float.class, Float.class,
      short.class, Short.class);
    ctor.newInstance("", 1,2,new Object(),
      JavaScriptObject.createObject(),0.5,2.5,6,7);
    }catch(Exception e) {
      error("ERROR",e);
    }

    //test the primitive ctor
    try {
      Constructor<MagicClassEntryPoint> ctor = cls.getConstructor(boolean.class,
        byte.class, short.class, char.class, int.class,
        long.class,float.class,double.class);
      // primitives
      ctor.newInstance(true, 0, 2,
          'a',3, 4L,
          5.5f, 6.6);
      // non-primitives
      ctor.newInstance(Boolean.TRUE, new Byte("1"), new Short("2"),
       new Character('a'),new Integer(3), new Long(4),
       new Float(5.5), new Double(6.6));
    }catch(Exception e) {
      error("ERROR",e);
    }

    //test the object ctor
    try {
      Constructor<MagicClassEntryPoint> ctor = cls.getConstructor(Boolean.class,
        Byte.class, Short.class, Character.class,
        Integer.class,Long.class,Float.class,Double.class);
      ctor.newInstance(false, (byte)1, (short)2, 'a',
        3, 4L, 5.5f, 6.6);
    }catch(Exception e) {
      error("ERROR",e);
    }

    for (Constructor<?> c : cls.getConstructors()) {
      try {
        trace((MagicClassEntryPoint)c.newInstance());
      } catch (Exception e) {
        error(e);
      }
    }

    //constructors passed.
    //now lets grab the methods and use them...
    //...to perform asserts on our fields!!

    Method[] methods = cls.getMethods();
    Method testAsserts = null;
    //call all of them EXCEPT onModuleLoad, as recursion sickness is no fun!
    for (Method m : methods) {
      if (!"onModuleLoad".equals(m.getName())) {
        if ("testAsserts".equals(m.getName())) {
          testAsserts = m;
        }else
        try {
          m.invoke(this, 2, 2, 3, "four");
        } catch (Exception e) {
          error(e);
        }
      }
    }

    //make sure methods retain type data.
    Class<?>[] params = testAsserts.getParameterTypes();
    assertEqual(params[0], X_Inject.class);
    assertEqual(params[1], Long.class);
    assertEqual(params[2], long.class);
    assertEqual(params[3], Void.class);


    try {
      //first, let's grab our Boolean field and change it from null to true
      Field field = cls.getField("field0");
      if (field.get(this) != null) {
        error("Boolean field did not return null");
      }
      field.set(this, true);
      assertEqual(field.getType(), Boolean.class);

      //Now, let's check all of our fields, except for the passed variable;
      //that variable is only set by the testAsserts() method,
      //which we call by reflection at the very end
      Field[] fields = cls.getFields();
      for (Field f : fields) {
        if ("passed".equals(f.getName()))
          continue;
        if (!f.getBoolean(this)) {
          error("Field "+f.getName()+" was not set to true!!");
        }
      }
    //now, let's grab all our fields, and assert they are true!
    }catch(Exception e) {
      error("Error testing fields", e);
    }


    //finished our functional tests,
    //now invoke our asserts method
    try {
      testAsserts.invoke(this);
    }catch(Exception e) {
      error("Failed assertions!", e);
      throw new RuntimeException(e);
    }
//    String codeSource = MagicClassEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm();
//    Assert.assertTrue("Metadata missing", codeSource.contains(MagicClassEntryPoint.class.getSimpleName()));
    
    //finally, since we can't trust our reflection tests,
    //let's manually check.
    if (!passed) {//can't be set unless testAsserts() was called and passed.
      error("One or more tests have failed.");
      throw new RuntimeException("Test Failed");
    }

  }

  private void assertEqual(Object one, Object two) {
    if (one == two)
      return;
    if (one.equals(two))
      return;
    error("Objects not equal!",one,two);
    throw new AssertionError("Expected "+one+"; received "+two);
  }

  private boolean method0 = false;

  public void method0(int i, short s, Long l, String val) {
    method0 = true;
    trace("method0", i, s, l, val);
  }

  private static boolean method1 = false;

  public static void method1(int i, short s, Long l, String val) {
    method1 = true;
    trace("method1", i, s, l, val);
  }

  private Boolean field0;

  @SuppressWarnings("unused")
  private void testAsserts(X_Inject inject, Long L, long l, Void Void) {
    passed = true;
    ArrayList<String> errors = new ArrayList<String>();
    if (!ctor0) {
      passed = false;
      String err = "Constructor 0 was not called!";
      errors.add(err);
      error(err);
    }
    if (!ctor1) {
      passed = false;
      String err = "Constructor 1 was not called!";
      errors.add(err);
      error(err);
    }
    if (!ctor2) {
      passed = false;
      String err = "Constructor 2 was not called!";
      errors.add(err);
      error(err);
    }
    if (!ctor3) {
      passed = false;
      String err = "Constructor 3 was not called!";
      errors.add(err);
      error(err);
    }
    if (!method0) {
      passed = false;
      String err = "Method 0 was not called!";
      errors.add(err);
      error(err);
    }
    if (!method1) {
      passed = false;
      String err = "Method 1 was not called!";
      errors.add(err);
      error(err);
    }
    if (!field0) {
      passed = false;
      String err = "Field 0 was not updated!";
      errors.add(err);
      error(err);
    }
    Document.get().getBody().setInnerHTML("Tests Have "
      +(passed?"Passed":"Failed; check console for details."));
    if (!passed)
      throw new RuntimeException("Failed test");
    trace("Success == ",passed);
  }

}
