package xapi.javac.dev.processor;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.List;
import xapi.annotation.api.XApi;
import xapi.fu.Pointer;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.model.InjectionBinding;
import xapi.javac.dev.model.XApiInjectionConfiguration;
import xapi.javac.dev.search.InjectionTargetSearchVisitor;
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
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementKindVisitor8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;
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
  private JavacService service;
  private Trees trees;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    elements = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    types = processingEnv.getTypeUtils();
    log = processingEnv.getMessager();

    service = JavacService.instanceFor(processingEnv);
    trees = Trees.instance(processingEnv);
  }

  @Override
  public boolean process(
      Set<? extends TypeElement> annotations, RoundEnvironment roundEnv
  ) {

    if (Math.random() > -1) {
      return true;
    }
    annotations.forEach(annotation->roundEnv.getElementsAnnotatedWith(annotation).forEach(this::processAnnotation));
    if (roundEnv.processingOver()) {
      // we are done collecting up XApi annotations; lets generate some code!
      annotatedTypes.forEach(this::generate);
    }
    return true;
  }

  private void processAnnotation(Element element) {
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
        log.printMessage(Kind.WARNING, "Unhandled XApi annotations on annotation types not yet supported in generator.  Element:"+element);
        break;
      case CLASS:
      case INTERFACE:
      case ENUM:
        final Name name = elements.getBinaryName((TypeElement) element);
        log.printMessage(Kind.NOTE, "Adding XApi annotated element "+element);
        annotatedTypes.add(new XApiInjectionConfiguration(xapi, name.toString(), element));
        break;
      default:
        throw new UnsupportedOperationException("Unsupported location of XApi annotation ("+element.getKind()+") : " + element);
    }
  }

  private void generate(XApiInjectionConfiguration config) {
    final Element type = config.getElement();

    final ClassTree tr = trees.getTree((TypeElement) type);
    final java.util.List<InjectionBinding> results = new InjectionTargetSearchVisitor(service, trees)
        .scan(trees.getTree(type), new ArrayList<>());
    trees.getTree(type);

    ElementFilter.fieldsIn(type.getEnclosedElements())
        .stream().filter(ElementUtil.withAnnotation(Inject.class))
        .forEach(field->{
          // A field annotated with @Inject.
          Optional<InjectionBinding> binding = service.getInjectionBinding(config, field.asType());
          if (binding.isPresent()) {
            // we have a valid injection result; insert it
            final List<Compound> attrs = ((VarSymbol) field).getRawAttributes();
            Pointer<ExecutableElement> initializer = Pointer.pointer();
            ClassSymbol t = (ClassSymbol) type;
            final Scope members = (((ClassSymbol) type).members());
              final TreePath path = trees.getPath(type);
              if (path != null)
              path.forEach(tree->{
                final Tree.Kind k = tree.getKind();
            });
            t.members().getElements().forEach(sym->{
              log.printMessage(Kind.NOTE, "Symbol " + sym.getClass()+" : " + sym);
            });

            type.accept(new ElementKindVisitor8<Void, Void>(){
              @Override
              public Void visitExecutable(ExecutableElement e, Void aVoid) {
                initializer.in(e);
                return null;
              }
            }, null);
          } else {
            // ignore this field
          }
        });
  }

}
