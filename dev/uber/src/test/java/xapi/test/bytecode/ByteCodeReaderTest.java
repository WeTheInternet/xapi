package xapi.test.bytecode;

import java.io.DataInputStream;
import java.io.InputStream;

import org.junit.Assert;
import org.junit.Test;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.bytecode.ClassFile;
import xapi.bytecode.ClassPool;
import xapi.bytecode.annotation.Annotation;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.dev.scanner.impl.ClasspathScannerDefault;
import xapi.platform.JrePlatform;
import xapi.platform.Platform;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.string.X_String;

class OuterClass {
  class InnerOuter{}
}

@JrePlatform
@SingletonDefault(implFor=ByteCodeReaderTest.class)
@SingletonOverride(implFor=ByteCodeReaderTest.class)
public class ByteCodeReaderTest  {

  private interface InnerInterface {}
  @InstanceDefault(implFor=InnerInterface.class)
  static class InnerClass implements InnerInterface {}

  @Test
  public void testReadClass() throws Exception {
    InputStream in = getClass().getResourceAsStream(
      "/"+getClass().getName().replace('.', '/')+".class");
    ClassFile file = new ClassFile(new DataInputStream(in));
    Annotation singleton = file.getRuntimeAnnotation(SingletonOverride.class.getName());
    Assert.assertNotNull("Did not load SingletonOverride", singleton);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testClasspathScanner() throws Exception{
    System.gc();
    long mem = Runtime.getRuntime().freeMemory();
    X_Time.tick();
    Moment start = X_Time.now();
	ClasspathResourceMap resources = new ClasspathScannerDefault()
      .scanAnnotations(
        Platform.class,
        SingletonDefault.class, SingletonOverride.class,
        InstanceDefault.class, InstanceOverride.class
      )
      .matchResource(".*")
      .matchClassFile(".*")
      .scan(Thread.currentThread().getContextClassLoader())
      ;
    ClassPool cp = new ClassPool();
    for (ClassFile cls : resources.findClassAnnotatedWith(
      SingletonDefault.class, SingletonOverride.class,
      InstanceDefault.class, InstanceOverride.class
      )) {
      Annotation anno = cls.getAnnotation(SingletonDefault.class.getName());
      if (anno != null) {
        SingletonDefault a = (SingletonDefault)anno.toAnnotationType(Thread.currentThread().getContextClassLoader(), cp);
        System.out.println(a);
      }
    }
    X_Time.tick();
    System.gc();
    long memDone = Runtime.getRuntime().freeMemory();
    System.out.println(
      "Scanned annotations in "+X_Time.difference(start)
    );
    System.out.println(
      "Used memory: "+X_String.toBinarySuffix(mem-memDone)
      );
  }

}
