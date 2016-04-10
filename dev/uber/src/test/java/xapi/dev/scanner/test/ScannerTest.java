package xapi.dev.scanner.test;

import org.junit.Test;
import xapi.bytecode.ClassFile;
import xapi.dev.scanner.X_Scanner;
import xapi.log.X_Log;
import xapi.time.X_Time;
import xapi.time.api.Moment;

import static org.junit.Assert.fail;

public class ScannerTest {

  private static class PrivateSubclass extends ScannerTest{}
  private static class SecondSubclass extends PrivateSubclass{}

  @Test(timeout = 15_000)
  public void testFindTestClasses() {
    Moment start = X_Time.now();
    for (ClassFile cls : X_Scanner.findMethodsWithAnnotations(getClass().getClassLoader(), Test.class)) {
      if (cls.getName().equals(ScannerTest.class.getName())) {
        X_Log.trace("Found self class in "+X_Time.difference(start));
        return;
      }
    };
    fail("Could not find "+ScannerTest.class.getName()+" using X_Scanner.findMethodWithAnnotations()");
  }

  @Test(timeout = 15_000)
  public void testFindDirectSubclasses() {
    Moment start = X_Time.now();
    for (ClassFile cls : X_Scanner.findDirectSubclasses(getClass().getClassLoader(), ScannerTest.class)) {
      if (cls.hasSuperClass(getClass().getName())) {
        X_Log.info("Found private subclass in "+X_Time.difference(start), cls);
        return;
      }
    };
    fail("Could not find "+PrivateSubclass.class.getName()+" using X_Scanner.findDirectSubclasses()");
  }

  @Test(timeout = 15_000)
  public void testFindImplementationsOf() {
    Moment start = X_Time.now();
    for (ClassFile cls : X_Scanner.findImplementationsOf(getClass().getClassLoader(), ScannerTest.class)) {
      if (cls.hasSuperClass(PrivateSubclass.class.getName())) {
        X_Log.info("Found class ancestor in "+X_Time.difference(start), cls);
        return;
      }
    };
    fail("Could not find "+SecondSubclass.class.getName()+" using X_Scanner.findImplementationsOf()");
  }

  @Test(timeout = 15_000)
  public void testFindInPackage() {
    Moment start = X_Time.now();
    for (ClassFile cls : X_Scanner.findClassesInPackage(getClass().getClassLoader(),
        ScannerTest.class.getPackage().getName())) {
      X_Log.info(getClass(), cls);
      if (cls.getName().equals(ScannerTest.class.getName())) {
        X_Log.trace("Found self class in "+X_Time.difference(start));
        return;
      }
    };
    fail("Could not find "+ScannerTest.class+" using X_Scanner.findClassesInPackages()");
  }



}
