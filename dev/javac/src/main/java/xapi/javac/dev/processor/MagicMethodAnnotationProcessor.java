package xapi.javac.dev.processor;

import com.sun.source.util.Trees;
import xapi.annotation.compile.MagicMethod;
import xapi.annotation.compile.Reference;
import xapi.javac.dev.api.JavacService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/13/16.
 */

@SupportedAnnotationTypes({
    "xapi.annotation.compile.MagicMethod"
})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MagicMethodAnnotationProcessor extends AbstractProcessor {

  private Elements elements;
  private Filer filer;
  private Types types;
  private Messager log;
  private JavacService service;
  private Trees trees;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    if (shouldSkip(processingEnv)) {
      processingEnv.getMessager().printMessage(Kind.NOTE, "Skipping ui annotations because system property xapi.no.javac.plugin was set to true");
      return;
    }
    elements = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    log = processingEnv.getMessager();
    types = processingEnv.getTypeUtils();
    service = JavacService.instanceFor(processingEnv);
    trees = Trees.instance(processingEnv);
  }

  protected boolean shouldSkip(ProcessingEnvironment processingEnv) {
    return "true".equals(System.getProperty("xapi.no.javac.plugin")) || "true".equals(processingEnv.getOptions().get("xapi.no.javac.plugin"));
  }

  @Override
  public boolean process(
      Set<? extends TypeElement> annotations, RoundEnvironment roundEnv
  ) {

    annotations.forEach(annotation->roundEnv.getElementsAnnotatedWith(annotation).forEach(element->{
      MagicMethod magicMethod = element.getAnnotation(MagicMethod.class);

      final Reference generator = magicMethod.generator();
      final TypeElement annoType = elements.getTypeElement(MagicMethod.class.getCanonicalName());
      final TypeElement useName = elements.getTypeElement(Reference.UseTypeName.class.getCanonicalName());
      final List<AnnotationMirror> results = element.getAnnotationMirrors()
          .stream()
          .filter(mirror -> {
            final DeclaredType type = mirror.getAnnotationType();
            if (annoType.equals(type)) {
              final ExecutableElement reference = findMethod(element, "generator");
              final AnnotationMirror value = (AnnotationMirror) mirror.getElementValues().get(reference).getValue();
              final ExecutableElement stringType = findMethod(value.getAnnotationType().asElement(), "typeName");
              final ExecutableElement classType = findMethod(value.getAnnotationType().asElement(), "type");
              final Object classValue = value.getElementValues().get(classType).getValue();
              String stringValue;
              stringValue = String.valueOf(classValue);
              if (useName.equals(classValue)) {
                stringValue = (String) value.getElementValues().get(stringType).getValue();
              } else {
              }
              return true;
            }
            return false;
          })
          .collect(Collectors.toList());

    }));
    if (roundEnv.processingOver()) {
    }
    return true;
  }

  private ExecutableElement findMethod(Element type, String methodName) {
    final Optional<? extends Element> maybe = type.getEnclosedElements()
        .stream()
        .filter(exe -> exe instanceof ExecutableElement)
        .filter(exe -> exe.getSimpleName().contentEquals(methodName))
        .findFirst();
    if (!maybe.isPresent()) {
      throw new UnsupportedOperationException("Cannot find method " + methodName +" in type " + type);
    }
    return (ExecutableElement) maybe.get();
  }

}
