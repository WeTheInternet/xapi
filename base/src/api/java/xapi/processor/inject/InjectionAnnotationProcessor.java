package xapi.processor.inject;

import xapi.constants.X_Namespace;
import xapi.fu.Out2;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

///
/// This is the annotation processor for our injection library.
///
/// It scans for our injection annotations and writes manifests to speed up injection.
/// default implementations go in META-INF/instances or META-INF/singletons.
///
/// Platform specific injection types go into META-INF/$type/instances, META-INF/$type/singletons.
///
/// It is included in the core api because it is run on the core api;
/// every jar built will include these manifests, whether they are used at runtime or not.
///
/// Gwt injection does not look at the manifests, and a bytecode enhancer is
/// in the works to further process jre environments, by replacing calls to X_Inject
/// with direct access of the injected type.
///
/// Final builds (bound to pre-deploy) can also be further enhanced,
/// by replacing all references to an injected interface with it's replacement type
/// (changing all class references, and changing invokeinterface to invokespecial).
/// This may be unnecessary once javac optimize=true after replacing the X_Inject call site.
///
///
/// @author "James X. Nelson (james@wetheinter.net)"
///
///
@SupportedAnnotationTypes({"xapi.annotation.inject.*"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class InjectionAnnotationProcessor extends AbstractProcessor{

  protected static class ManifestWriter {
    HashMap<String, Out2<Integer, String>> singletons = new HashMap<>();
    HashMap<String, Out2<Integer, String>> instances = new HashMap<>();
    private final Messager messager;

      protected ManifestWriter(final Messager messager) {
          this.messager = messager;
      }

      void writeSingleton(final String iface, final String platform, final Integer priority, final String element) {
      if (priority == null) {
        if (!singletons.containsKey(iface)) {
          singletons.put(iface, Out2.out2Immutable(priority, element));
        }
      } else {
        final Out2<Integer, String> existing = singletons.get(iface);
        if (
            existing == null ||
            existing.out1() == null ||
            existing.out1() < priority
        ) {
          singletons.put(iface, Out2.out2Immutable(priority, element));
        }
      }
    }
    void writeInstance(final String iface, final String platform, final Integer priority, final String element) {
      if (priority == null) {
        if (!instances.containsKey(iface)) {
          instances.put(iface, Out2.out2Immutable(priority, element));
        }
      } else {
        final Out2<Integer, String> existing = instances.get(iface);
        if (
            existing == null ||
            existing.out1() == null ||
            existing.out1() < priority
            ) {
          instances.put(iface, Out2.out2Immutable(priority, element));
        }
      }
    }
    void commit(final Filer filer) throws IOException {
      for (final String iface : singletons.keySet()) {
        final String impl = singletons.get(iface).out2();
        writeTo("singletons",iface, impl, filer);
      }
      for (final String iface : instances.keySet()) {
        final String impl = instances.get(iface).out2();
        writeTo("instances",iface, impl, filer);
      }
    }
    protected void writeTo(final String location, final String iface, final String impl, final Filer filer) throws IOException {
      CharSequence existing;
      final String manifest = "META-INF/" + location+ "/" + iface;
      try {
        existing = filer.getResource(StandardLocation.CLASS_OUTPUT, "", manifest).getCharContent(true);
        if (!impl.contentEquals(existing)) {
          messager.printMessage(Diagnostic.Kind.ERROR, "Cannot overwrite existing " +location+" injection target.\n" +
              "Tried: "+iface+" -> "+impl+"\n" +
              "but existing manifest has: "+existing);
        }
      } catch (final FilerException ignored) {
        // Re-opening the file for reading or after writing is ignorable
      } catch (final IOException e) {
        // File does not exist, just create one.
        final FileObject res = filer.createResource(StandardLocation.CLASS_OUTPUT, "",manifest);
        if ("true".equals(System.getProperty(X_Namespace.PROPERTY_DEBUG, System.getenv("XAPI_DEBUG")))) {
          messager.printMessage(Diagnostic.Kind.NOTE, "Creating new injection file: " + manifest);
        }
        final OutputStream out = res.openOutputStream();
        out.write(impl.getBytes());
        out.close();
      }
    }
  }

  private boolean skip;

  public InjectionAnnotationProcessor() {}

  protected Filer filer;
  protected ManifestWriter writer;
  protected Messager messager;

  @Override
  public final synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    messager = processingEnv.getMessager();
    filer = processingEnv.getFiler();
    writer = initWriter(processingEnv);
    skip = shouldSkip(processingEnv);
  }

  protected ManifestWriter initWriter(final ProcessingEnvironment processingEnv) {
    return new ManifestWriter(messager);
  }


  protected boolean shouldSkip(ProcessingEnvironment processingEnv) {
    return "true".equals(System.getProperty("xapi.no.javac.plugin")) || "true".equals(processingEnv.getOptions().get("xapi.no.javac.plugin"));
  }

  @Override
  public boolean process(
      final Set<? extends TypeElement> annotations,
      final RoundEnvironment roundEnv
  ) {
    if (skip) {
      return true;
    }
    final Elements elements = processingEnv.getElementUtils();
    for (final TypeElement anno : annotations) {
      final ExecutableElement implFor = extractImplFor(anno);
      final ExecutableElement priorityFor = extractPriorityFor(anno);
      for (final Element element : roundEnv.getElementsAnnotatedWith(anno)) {
        final AnnotationMirror mirror = getMirror(element, anno);
        final AnnotationValue t =
            elements.getElementValuesWithDefaults(mirror)
            .get(implFor);
        if (!(t.getValue() instanceof DeclaredType)) {
          // TODO investigate why the value is sometimes a string and react accordingly
          continue;
        }
        final DeclaredType cls = (DeclaredType) t.getValue();
        final Integer priority = getPriority(elements, mirror, priorityFor);
        if (anno.getSimpleName().contentEquals("SingletonDefault")) {
          for (final String platform : getPlatforms(element)) {
            writer.writeSingleton(cls.toString(), platform, null, element.toString());
          }
        } else if (anno.getSimpleName().contentEquals("SingletonOverride")) {
          for (final String platform : getPlatforms(element)) {
            writer.writeSingleton(cls.toString(), platform, priority, element.toString());
          }
        } else if (anno.getSimpleName().contentEquals("InstanceDefault")) {
          for (final String platform : getPlatforms(element)) {
            writer.writeInstance(cls.toString(), platform, null, element.toString());
          }
        } else if (anno.getSimpleName().contentEquals("InstanceOverride")) {
          for (final String platform : getPlatforms(element)) {
            writer.writeInstance(cls.toString(), platform, priority, element.toString());
          }
        }
      }
    }
    if (roundEnv.processingOver()) {
      try {
        writer.commit(filer);
      } catch (final Exception e) {
        e.printStackTrace();
        messager.printMessage(Diagnostic.Kind.ERROR, "Unable to write injection metadata.");
        return false;
      }
    }

    return true;
  }

  private Integer getPriority(final Elements elements, final AnnotationMirror mirror,
      final ExecutableElement priorityFor) {
    if (priorityFor == null)
     {
      return null; // *Default annos
    }
    return (Integer)elements.getElementValuesWithDefaults(mirror)
        .get(priorityFor)
        .getValue();
  }

  private AnnotationMirror getMirror(final Element element, final TypeElement type) {
    for (final AnnotationMirror mirror : element.getAnnotationMirrors()) {
      if (mirror.getAnnotationType().toString().equals(type.toString())) {
        return mirror;
      }
    }
    throw new RuntimeException("Element "+element+" did not contain annotation "+type);
  }

  private ExecutableElement extractImplFor(final TypeElement anno) {
    for (final ExecutableElement e : ElementFilter.methodsIn(anno.getEnclosedElements())) {
      if (e.getSimpleName().toString().equals("implFor")){
        return e;
      }
    }
    throw new RuntimeException("Annotation "+anno+" does not contain an implFor() method.\n" +
    		"Available methods: "+anno.getEnclosedElements());
  }
  private ExecutableElement extractPriorityFor(final TypeElement anno) {
    for (final ExecutableElement e : ElementFilter.methodsIn(anno.getEnclosedElements())) {
      if (e.getSimpleName().toString().equals("priority")){
        return e;
      }
    }
    return null; // Default annos don't have priority
  }

  protected Iterable<String> getPlatforms(final Element element) {
    return Arrays.asList("");
  }

  void dumpType(final Element anno) {
    processingEnv.getElementUtils().printElements(new PrintWriter(System.out), anno);
  }

}
