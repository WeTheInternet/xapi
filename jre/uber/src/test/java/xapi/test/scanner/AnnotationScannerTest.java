package xapi.test.scanner;


import static xapi.inject.X_Inject.instance;

import org.junit.Test;

import xapi.dev.scanner.ClasspathScanner;
import xapi.platform.JrePlatform;
import xapi.test.AbstractInjectionTest;

@JrePlatform
public class AnnotationScannerTest extends AbstractInjectionTest{

  @SuppressWarnings("unchecked")
  @Test public void testAnnotationScanner() {
    //can we find ourselves?
    // note, we can't do runtime injection,
    // since jre-inject depends on reflect...
    ClasspathScanner scanner =
        instance(ClasspathScanner.class)
        .scanPackage("xapi")
        .scanPackage("java.util")
        .scanAnnotation(JrePlatform.class)
        ;
    scanner.scan(getClass().getClassLoader()).findClassAnnotatedWith(
    		JrePlatform.class);
  }

}
