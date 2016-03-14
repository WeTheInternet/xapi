package xapi.javac.dev.processor;

import xapi.annotation.api.XApi;
import xapi.javac.dev.model.XApiInjectionConfiguration;
import xapi.javac.dev.util.ElementUtil;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Inject;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/13/16.
 */

@SupportedAnnotationTypes({
    "xapi.annotation.api.XApi"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class XapiAnnotationProcessor extends AbstractProcessor {

  private Elements elements;
  private Filer filer;
  private Types types;
  private Messager log;
  private Set<XApiInjectionConfiguration> annotatedTypes = new LinkedHashSet<>();

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    elements = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    types = processingEnv.getTypeUtils();
    log = processingEnv.getMessager();

  }

  @Override
  public boolean process(
      Set<? extends TypeElement> annotations, RoundEnvironment roundEnv
  ) {
    annotations.forEach(this::processAnnotation);
    if (roundEnv.processingOver()) {
      // we are done collecting up XApi annotations; lets generate some code!
      annotatedTypes.forEach(this::generate);
    }
    return true;
  }

  private void processAnnotation(TypeElement element) {
    final Name name = elements.getBinaryName(element);
    final XApi xapi = element.getAnnotation(XApi.class);
    switch (element.getKind()) {
      case PACKAGE:
        // A package-level annotation will be applied, as a default, to all members of that package.
        // Any @XApi annotated types will override this default for this package
        throw new UnsupportedOperationException("XApi annotations on packages not yet supported in generator");
      case ANNOTATION_TYPE:
        // This will cause us to become interested in this new annotation type.
        // To handle this, we will need to add this annotation to a list of types to be scanned;
        // likely by generating an annotation processor and adding an entry to META-INF/services
        throw new UnsupportedOperationException("XApi annotations on annotation types not yet supported in generator");
      case CLASS:
      case INTERFACE:
      case ENUM:
        annotatedTypes.add(new XApiInjectionConfiguration(xapi, name.toString(), element));
        break;
      default:
        throw new UnsupportedOperationException("Unsupported location of XApi annotation ("+element.getKind()+") : " + element);
    }
  }

  private void generate(XApiInjectionConfiguration config) {
    final TypeElement type = config.getElement();

    ElementFilter.fieldsIn(type.getEnclosedElements())
        .stream().filter(ElementUtil.withAnnotation(Inject.class))
        .forEach(field->{
          // A field annotated with @Inject.
          if (field.getModifiers().contains(Modifier.STATIC)) {
          }
        });
  }

}
