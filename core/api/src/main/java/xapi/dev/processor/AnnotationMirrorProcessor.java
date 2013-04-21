package xapi.dev.processor;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.ReferenceType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

import xapi.util.X_Runtime;
import xapi.util.impl.AbstractPair;

/**
 * This is the annotation processor for our injection library.
 * 
 * It scans for our injection annotations and writes manifests to speed up injection.
 * default implementations go in META-INF/instances or META-INF/singletons.
 * 
 * Platform specific injection types go into META-INF/$type/instances, META-INF/$type/singletons.
 * 
 * It is included in the core api because it is run on the core api;
 * every jar built will include these manifests, whether they are used at runtime or not.
 * 
 * Gwt injection does not look at the manifests, and a bytecode enhancer is 
 * in the works to further process jre environments, by replacing calls to X_Inject
 * with direct access of the injected type.
 * 
 * Final builds (bound to pre-deploy) can also be further enhanced,
 * by replacing all references to an injected interface with it's replacement type
 * (changing all class references, and changing invokeinterface to invokespecial).
 * This may be unnecessary once javac optimize=true after replacing the X_Inject call site.
 * 
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@SupportedAnnotationTypes({"xapi.annotation.reflect.MirroredAnnotation"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class AnnotationMirrorProcessor extends AbstractProcessor{

  protected static class PlatformPair extends AbstractPair<String, Class<?>> {}
  
  
  public AnnotationMirrorProcessor() {}
  
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
    Elements elements = processingEnv.getElementUtils();
    for (TypeElement anno : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(anno)) {
        for (ExecutableElement method : ElementFilter.methodsIn(element.getEnclosedElements())){
          TypeMirror returnType = method.getReturnType();
          log("Checking method "+method+" with return type "+returnType);
          if (returnType instanceof ReferenceType) {
            // We must take special care; class values of annotations cannot be loaded normally.
            if (returnType instanceof ArrayType) {
              
            } else {
              assert returnType instanceof DeclaredType :
                "Unsupported annotation method type: " +
              		returnType+" on " + element;
              DeclaredType declaredType = (DeclaredType) returnType;
              List<? extends TypeMirror> types = declaredType.getTypeArguments();
            }
          } else if (returnType instanceof PrimitiveType) {
            // Plain ol' primitive; we can safely check default value in TypeMirror
            AnnotationValue dflt = method.getDefaultValue();
            if (dflt == null) {
              // A required primitive
            } else {
              // An optional primitive
            }
          }
        }
      }
    }
    if (roundEnv.processingOver())
    try {
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Unable to write injection metadata.");
      return false;
    }
    
    return true;
  }
  
  private void log(String string) {
    if (X_Runtime.isDebug()) {
      System.out.println(string);
    }
  }

  private Integer getPriority(Elements elements, AnnotationMirror mirror,
      ExecutableElement priorityFor) {
    if (priorityFor == null) 
      return null; // *Default annos
    return (Integer)elements.getElementValuesWithDefaults(mirror)
        .get(priorityFor)
        .getValue();
  }

  private AnnotationMirror getMirror(Element element, TypeElement type) {
    for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().toString().equals(type.toString())) {
        return mirror;
      }
    }
    throw new RuntimeException("Element "+element+" did not contain annotation "+type);
  }

  private ExecutableElement extractImplFor(TypeElement anno) {
    for (ExecutableElement e : ElementFilter.methodsIn(anno.getEnclosedElements())) {
      if (e.getSimpleName().toString().equals("implFor")){
        return e;
      }
    }
    throw new RuntimeException("Annotation "+anno+" does not contain an implFor() method.\n" +
    		"Available methods: "+anno.getEnclosedElements());
  }
  private ExecutableElement extractPriorityFor(TypeElement anno) {
    for (ExecutableElement e : ElementFilter.methodsIn(anno.getEnclosedElements())) {
      if (e.getSimpleName().toString().equals("priority")){
        return e;
      }
    }
    return null; // Default annos don't have priority
  }

  protected Iterable<String> getPlatforms(Element element) {
    return Arrays.asList("");
  }

  void dumpType(Element anno) {
    processingEnv.getElementUtils().printElements(new PrintWriter(System.out), anno);
  }

}
