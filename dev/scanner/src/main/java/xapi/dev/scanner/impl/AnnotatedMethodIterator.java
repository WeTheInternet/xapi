package xapi.dev.scanner.impl;

import java.util.concurrent.ExecutorService;

import xapi.bytecode.ClassFile;
import xapi.bytecode.MethodInfo;
import xapi.bytecode.annotation.Annotation;


/**
 * Finds all classes that have methods with any annotation.
 *
 * If you want more fine-grained search capabilities,
 * override {@link #matches(ClassFile)}.
 *
 * The only annotation this filter ignores is @Override;
 * beware that excessive filtering here may be slower than just collecting all annotated methods,
 * and simply consuming the annotations you are interested in as you find them yourself.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
class AnnotatedMethodIterator extends AnnotatedClassIterator {

  public AnnotatedMethodIterator(ExecutorService executor, ResourceTrie<ByteCodeResource> bytecode) {
    super(executor, bytecode);
  }
  @Override
  public boolean filter1(ClassFile value) {
    for (MethodInfo method : value.getMethods()) {
      if (scanMethod(method)) {
        for (Annotation anno : method.getAnnotations()) {
          if (!anno.getTypeName().equals(Override.class.getName())) {
            return true;
          }
        }
      }
    }
    return false;
  }
  /**
   * Allows subclasses to filter on methods; for example, to choose only public methods,
   * or to implement methodname filtering.
   * <p>
   * Though you may check for annotations here, you might be wise to instead override
   * {@link #matches(ClassFile)}, as that method will perform wasted operations if you check annos here.
   *
   * @param method - The method being filtered to see if it has annotations.
   * @return - true if you want to check that this method has annotations.
   */
  protected boolean scanMethod(MethodInfo method) {
    return true;
  }
}
