package xapi.dev.processor;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementVisitor;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.AbstractElementVisitor6;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.util.api.Pair;
import xapi.util.impl.AbstractPair;
import static xapi.util.impl.PairBuilder.of;

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
@SupportedAnnotationTypes({"xapi.annotation.inject.*"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class InjectionAnnotationProcessor extends AbstractProcessor{

  protected static class PlatformPair extends AbstractPair<String, Class<?>> {}
  
  protected static class ManifestWriter {
    void writeSingleton(Class<?> iface, String platform, Integer priority, Element element) {
      
    }
    void writeInstance(Class<?> iface, String platform, Integer priority, Element element) {
      
    }
    void commit(Filer filer) {
      
    }
  }
  
  
  public InjectionAnnotationProcessor() {}
  
  protected Filer filer;
  protected ManifestWriter writer;
  
  
  
  @Override
  public final synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    filer = processingEnv.getFiler();
    writer = initWriter(processingEnv);
  }
  
  protected ManifestWriter initWriter(ProcessingEnvironment processingEnv) {
    return new ManifestWriter();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {
    // TODO use Pair<String, Class<?>> as key, and put overrides into scoped folders in META-INF
    // Also, we should be looking up the current runtime platform and adjusting /META-INF for /assets
    HashMap<Class<?>, Pair<Integer, Element>> singletons = new HashMap<Class<?>, Pair<Integer, Element>>();
    HashMap<Class<?>, Pair<Integer, Element>> instances = new HashMap<Class<?>, Pair<Integer, Element>>();
    for (TypeElement anno : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(anno)) {
        //Disabled until TypeMirror issue is resolved
//
//        if (anno.getQualifiedName().contentEquals(SingletonDefault.class.getCanonicalName())) {
//          SingletonDefault def = element.getAnnotation(SingletonDefault.class);
//          if (!singletons.containsKey(def.implFor()))
//            singletons.put(def.implFor(), of((Integer)null, element));
//        } else if (anno.getQualifiedName().contentEquals(SingletonOverride.class.getCanonicalName())) {
//          SingletonOverride def = element.getAnnotation(SingletonOverride.class);
//          Pair<Integer, Element> existing = singletons.get(def.implFor());
//          if (
//              existing == null || 
//              existing.get0() == null ||
//              existing.get0() < def.priority()
//          )
//            singletons.put(def.implFor(), of(def.priority(), element));
//        } else if (anno.getQualifiedName().contentEquals(InstanceDefault.class.getCanonicalName())) {
//          InstanceDefault def = element.getAnnotation(InstanceDefault.class);
//          if (!instances.containsKey(def.implFor()))
//            instances.put(def.implFor(), of((Integer)null, element));
//        } else if (anno.getQualifiedName().contentEquals(InstanceOverride.class.getCanonicalName())) {
//          InstanceOverride def = element.getAnnotation(InstanceOverride.class);
//          Pair<Integer, Element> existing = instances.get(def.implFor());
//          if (
//              existing == null || 
//              existing.get0() == null ||
//              existing.get0() < def.priority()
//              )
//            instances.put(def.implFor(), of(def.priority(), element));
//        } 
      }
    }
    for (Class<?> iface : singletons.keySet()) {
      Pair<Integer, Element> impl = singletons.get(iface);
      writer.writeSingleton(iface, "", impl.get0(), impl.get1());
    }
    for (Class<?> iface : instances.keySet()) {
      Pair<Integer, Element> impl = instances.get(iface);
      writer.writeInstance(iface, "", impl.get0(), impl.get1());
    }
    writer.commit(filer);
    
    return true;
  }
  
  protected Iterable<String> getPlatforms(Element element) {
    return Arrays.asList("");
  }

  void dumpType(Element anno) {
    processingEnv.getElementUtils().printElements(new PrintWriter(System.out), anno);
  }

}
