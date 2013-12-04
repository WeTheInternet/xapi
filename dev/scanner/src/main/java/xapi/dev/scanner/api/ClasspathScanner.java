package xapi.dev.scanner.api;

import java.lang.annotation.Annotation;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import xapi.dev.scanner.impl.ClasspathResourceMap;

/**
 * Most runtime annotation scanner libraries are too heavyweight;
 * they try to do too much.  Though great tools, we want flyweight and fast.
 *
 * This service is based on org.reflections:reflections,
 * except we are doing much less,
 * and using an api that will enable multi-threaded scanning,
 * using a work-stealing algorithm to fill in a PackageTrie.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface ClasspathScanner {

  ClasspathScanner scanPackage(String pkg);
  ClasspathScanner scanPackages(String ... pkgs);
  ClasspathScanner scanAnnotation(Class<? extends Annotation> annotations);
  ClasspathScanner scanAnnotations(@SuppressWarnings("unchecked") Class<? extends Annotation> ... annotations);
  ClasspathScanner matchClassFile(String regex);
  ClasspathScanner matchClassFiles(String ... regex);
  ClasspathScanner matchSourceFile(String regex);
  ClasspathScanner matchSourceFiles(String ... regex);
  ClasspathScanner matchResource(String regex);
  ClasspathScanner matchResources(String ... regex);

  ClasspathResourceMap scan(ClassLoader loader);
  Callable<ClasspathResourceMap> scan(ClassLoader loader, ExecutorService executor);
  ExecutorService newExecutor();

}
