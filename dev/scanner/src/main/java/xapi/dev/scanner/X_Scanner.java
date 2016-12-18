package xapi.dev.scanner;

import xapi.bytecode.ClassFile;
import xapi.dev.X_Dev;
import xapi.dev.resource.impl.StringDataResource;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.inject.X_Inject;
import xapi.util.X_String;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class X_Scanner {

  private X_Scanner() {}

  public static ClasspathResourceMap scanClassloader(ClassLoader cl) {
    return scanAll()
      .scan(cl);
  }

  public static Callable<ClasspathResourceMap> scanClassloaderAsync(ClassLoader cl) {
    return X_Inject.instance(ClasspathScanner.class)
        .matchClassFile(".*")
        .matchResource(".*")
        .matchSourceFile(".*")
        .scanPackage("")
        .scan(cl, Executors.newCachedThreadPool());
  }

  private static ClasspathScanner scanAll() {
    return X_Inject.instance(ClasspathScanner.class)
        .matchClassFile(".*")
        .matchResource(".*")
        .matchSourceFile(".*")
        .scanPackage("");
  }

  public static ClasspathResourceMap scanClassloader(ClassLoader cl,
      boolean scanClasses, boolean scanSources, boolean scanResources, String pkg) {
    ClasspathScanner scanner = X_Inject.instance(ClasspathScanner.class);
    if (scanClasses)
      scanner.matchClassFile(".*");
    if (scanResources)
      scanner.matchResource(".*");
    if (scanSources)
        scanner.matchSourceFile(".*");
    scanner.scanPackage(X_String.notNull(pkg));
    return scanner.scan(cl);
  }

  public static Iterable<StringDataResource> findPoms(ClassLoader cl) {
    return X_Inject.instance(ClasspathScanner.class)
      .matchResource(".*pom.*xml")
      .scanPackage("")
      .scan(cl)
      .findResources("");
  }

  public static Iterable<StringDataResource> findGwtXml(ClassLoader cl) {
    return X_Inject.instance(ClasspathScanner.class)
      .matchResource(".*gwt.*xml")// also match .gwtxml files
      .scanPackage("")
      .scan(cl)
      .findResources("");
  }

  public static Iterable<ClassFile> findEnums(ClassLoader cl) {
    return X_Inject.instance(ClasspathScanner.class)
        .matchClassFile(".*")
        .scanPackage("")
        .scan(cl)
        .findDirectSubclasses(Enum.class);
  }

  @SafeVarargs
  public static Iterable<ClassFile> findMethodsWithAnnotations(ClassLoader cl, Class<? extends Annotation> ... annoClass) {
    return findMethodsWithAnnotations(cl, "", annoClass);
  }

  @SafeVarargs
  public static Iterable<ClassFile> findMethodsWithAnnotations(ClassLoader cl, String packageName, Class<? extends Annotation> ... annoClass) {
    return X_Inject.instance(ClasspathScanner.class)
        .matchClassFile(".*")
        .scanPackage(packageName)
        .scan(cl)
        .findClassWithAnnotatedMethods(annoClass);
  }

  public static ClasspathResourceMap scanFolder(String url) {
    return scanClassloader(new URLClassLoader(new URL[]{X_Dev.toUrl(url)}));
  }

  public static ClasspathResourceMap scanFolder(String url,
    boolean scanClasses, boolean scanSources, boolean scanResources, String pkg) {
    return scanClassloader(
        new URLClassLoader(new URL[]{X_Dev.toUrl(url)})
      , scanClasses, scanSources, scanResources, pkg);
  }

  public static Iterable<ClassFile> findDirectSubclasses(ClassLoader classLoader, Class<?> ... cls) {
    return
        X_Inject.instance(ClasspathScanner.class)
        .matchClassFile(".*")
        .scanPackage("")
        .scan(classLoader)
        .findDirectSubclasses(cls);
  }

  public static Iterable<ClassFile> findImplementationsOf(ClassLoader classLoader, Class<?> ... cls) {
    return
        X_Inject.instance(ClasspathScanner.class)
        .matchClassFile(".*")
        .scanPackage("")
        .scan(classLoader)
        .findImplementationOf(cls);
  }

  public static Iterable<ClassFile> findClassesInPackage(ClassLoader classLoader, String pkgName) {
    return
        X_Inject.instance(ClasspathScanner.class)
        .matchClassFile(".*")
        .scanPackage("")
        .scan(classLoader)
        .findClassesInPackage(pkgName);

  }

}
