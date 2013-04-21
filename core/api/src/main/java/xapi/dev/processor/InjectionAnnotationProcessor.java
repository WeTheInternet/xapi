package xapi.dev.processor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.FilerException;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import xapi.util.X_Util;
import xapi.util.api.Pair;
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
@SupportedAnnotationTypes({"xapi.annotation.inject.*"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class InjectionAnnotationProcessor extends AbstractProcessor{

  protected static class PlatformPair extends AbstractPair<String, Class<?>> {}
  
  protected static class ManifestWriter {
    HashMap<String, Pair<Integer, String>> singletons = new HashMap<String, Pair<Integer, String>>();
    HashMap<String, Pair<Integer, String>> instances = new HashMap<String, Pair<Integer, String>>();
    void writeSingleton(String iface, String platform, Integer priority, String element) {
      if (priority == null) {
        if (!singletons.containsKey(iface))
          singletons.put(iface, X_Util.pairOf(priority, element));
      } else {
        Pair<Integer, String> existing = singletons.get(iface);
        if (
            existing == null || 
            existing.get0() == null ||
            existing.get0() < priority
        )
          singletons.put(iface, X_Util.pairOf(priority, element));
      }
    }
    void writeInstance(String iface, String platform, Integer priority, String element) {
      if (priority == null) {
        if (!instances.containsKey(iface))
          instances.put(iface, X_Util.pairOf(priority, element));
      } else {
        Pair<Integer, String> existing = instances.get(iface);
        if (
            existing == null || 
            existing.get0() == null ||
            existing.get0() < priority
            )
          instances.put(iface, X_Util.pairOf(priority, element));
      }
    }
    void commit(Filer filer) throws IOException {
      for (String iface : singletons.keySet()) {
        String impl = singletons.get(iface).get1();
        writeTo("singletons",iface, impl, filer);
      }
      for (String iface : instances.keySet()) {
        String impl = instances.get(iface).get1();
        writeTo("instances",iface, impl, filer);
      }
    }
    protected void writeTo(String location, String iface, String impl, Filer filer) throws IOException {
      CharSequence existing;
      String manifest = "META-INF/" + location+ "/" + iface;
      try {
        existing = filer.getResource(StandardLocation.CLASS_OUTPUT, "", manifest).getCharContent(true);
        if (!impl.contentEquals(existing))
          System.out.println("Cannot overwrite existing " +location+" injection target.\n" +
              "Tried: "+iface+" -> "+impl+"\n" +
              "but existing manifest has: "+existing);
      } catch (FilerException ignored) {
        // Re-opening the file for reading or after writing is ignorable
      } catch (IOException e) {
        // File does not exist, just create one.
        FileObject res = filer.createResource(StandardLocation.CLASS_OUTPUT, "",manifest);
        OutputStream out = res.openOutputStream();
        out.write(impl.getBytes());
        out.close();
      }
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
  public boolean process(
      Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv
  ) {
    Elements elements = processingEnv.getElementUtils();
    for (TypeElement anno : annotations) {
      ExecutableElement implFor = extractImplFor(anno);
      ExecutableElement priorityFor = extractPriorityFor(anno);
      for (Element element : roundEnv.getElementsAnnotatedWith(anno)) {
        AnnotationMirror mirror = getMirror(element, anno);
        AnnotationValue t = 
            elements.getElementValuesWithDefaults(mirror)
            .get(implFor);
        DeclaredType cls = (DeclaredType) t.getValue();
        Integer priority = getPriority(elements, mirror, priorityFor);
        if (anno.getSimpleName().contentEquals("SingletonDefault")) {
          for (String platform : getPlatforms(element))
            writer.writeSingleton(cls.toString(), platform, null, element.toString());
        } else if (anno.getSimpleName().contentEquals("SingletonOverride")) {
          for (String platform : getPlatforms(element))
            writer.writeSingleton(cls.toString(), platform, priority, element.toString());
        } else if (anno.getSimpleName().contentEquals("InstanceDefault")) {
          for (String platform : getPlatforms(element))
            writer.writeInstance(cls.toString(), platform, null, element.toString());
        } else if (anno.getSimpleName().contentEquals("InstanceOverride")) {
          for (String platform : getPlatforms(element))
            writer.writeInstance(cls.toString(), platform, priority, element.toString());
        } 
      }
    }
    if (roundEnv.processingOver())
    try {
      writer.commit(filer);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Unable to write injection metadata.");
      return false;
    }
    
    return true;
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
