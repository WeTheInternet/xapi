package xapi.dev.model;

import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

import xapi.log.X_Log;

@SupportedAnnotationTypes({"xapi.annotation.model.*"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ModelAnnotationProcessor extends AbstractProcessor{

  public ModelAnnotationProcessor() {}
  
  protected Filer filer;
  
  @Override
  public final synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    filer = processingEnv.getFiler();
  }
  
  @Override
  public boolean process(
      Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv
  ) {
    HashSet<String> modelTypes = new HashSet<String>();
    HashSet<String> modelPackages = new HashSet<String>();
    for (TypeElement anno : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(anno)) {
        Element e = element.getEnclosingElement();
        if (e instanceof TypeElement) {
          modelTypes.add(((TypeElement)e).getQualifiedName().toString());
        } else if (e instanceof PackageElement){
          modelPackages.add(((PackageElement)e).getQualifiedName().toString());
        } else {
          X_Log.warn("Ignored an enclosing element that was not a TypeElement or package element"
              ,e,e.getClass());
        }
      }
    }
    ClassLoader parentCl = ModelAnnotationProcessor.class.getClassLoader();
    URL[] urls = ((URLClassLoader)parentCl).getURLs();
    URLClassLoader cl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
//    X_Log.info(Arrays.asList(urls));
    for (String s : modelPackages) {
      X_Log.info("Model Package",s);
    }
    for (String modelCls : modelTypes) {
      Class<?> cls = loadClass(modelCls, cl);
      X_Log.info("Loaded model: ", cls);
    }
    
    if (roundEnv.processingOver())
    try {
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Unable to generate model classes.");
      return false;
    }
    
    return true;
  }
  
  private Class<?> loadClass(String s, ClassLoader cl) {

    try{
      return cl.loadClass(s);
    } catch (Throwable e) {
      // An inner class can do this.  Just search backwards...
      int ind = s.lastIndexOf('.');
      String tryLoad = s;
      ArrayDeque<String> stack = new ArrayDeque<String>();
      while (ind != -1) {
        stack.add(tryLoad.substring(ind+1));
        tryLoad = tryLoad.substring(0, ind);
        try {
          Class<?> cls = cl.loadClass(tryLoad);
          innerClassLoop:
          while (stack.size() > 0) {
            String next = stack.pop();
            for (Class<?> inner : cls.getClasses()) {
              if (inner.getSimpleName().equals(next)) {
                cls = inner;
                continue innerClassLoop;
              }
            }
            throw new RuntimeException(new ClassNotFoundException(
                "Could not load class: "+s+"; ensure that it is public " +
                		"and available on the classpath."
                ));
          }
          return cls;
        } catch (ClassNotFoundException ex) {}
        ind = tryLoad.lastIndexOf('.');
      }
    }
    throw new RuntimeException(new ClassNotFoundException(s));
  }

  protected Iterable<String> getPlatforms(Element element) {
    return Arrays.asList("");
  }

  void dumpType(Element anno) {
    processingEnv.getElementUtils().printElements(new PrintWriter(System.out), anno);
  }

}