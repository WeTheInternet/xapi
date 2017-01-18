package xapi.javac.dev.processor;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.plugin.Transformer;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.List;
import xapi.annotation.api.XApi;
import xapi.except.NotYetImplemented;
import xapi.fu.Pointer;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.model.InjectionBinding;
import xapi.javac.dev.model.XApiInjectionConfiguration;
import xapi.javac.dev.search.InjectionTargetSearchVisitor;
import xapi.javac.dev.template.TemplateTransformer;
import xapi.javac.dev.util.ElementUtil;
import xapi.source.X_Source;
import xapi.util.X_Debug;

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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementKindVisitor8;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
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

    annotations.forEach(annotation->roundEnv.getElementsAnnotatedWith(annotation).forEach(element->{

      XApi xapi = element.getAnnotation(XApi.class);

      if (xapi.value().length > 0) {
        for (String path : xapi.value()) {
          String pkg = "";
          if (path.startsWith("./")) {
            path = path.substring(2);
            final PackageElement packageElement = elements.getPackageOf(element);
            if (packageElement != null) {
              pkg = packageElement.getQualifiedName().toString();
            }
          }
          int index = path.indexOf('/');
          while (index != -1) {
            String subpath = path.substring(0, index);
            path = path.substring(index+1);
            index = path.indexOf('/');
            if (pkg.isEmpty()) {
              pkg = subpath;
            } else {
              pkg = pkg + "." + subpath;
            }
          }
          if ("*".equals(path)) {
            // match everything in this package
            throw new NotYetImplemented("* file pattern matching not yet supported by XApi annotation processor.");
          } else {
            // An actual filename.
            final Location location = StandardLocation.SOURCE_PATH;
            final FileObject resource;
            if (path.indexOf('.') == -1) {
              path = path + ".xapi";
            }
            try {
              resource = filer.getResource(location, pkg, path);
              final CompilationUnit result = JavaParser.parse(
                  resource.openInputStream(),
                  Charset.defaultCharset().name()
              );
              Transformer transformer = new TemplateTransformer();
              String source = result.toSource(transformer);

              final JavaFileObject file = filer.createSourceFile(X_Source.qualifiedName(
                  pkg,
                  path.replace(".xapi", "") // remove file extension
              ), element);
              try (Writer out = file.openWriter()) {
                out.append(source);
              }
            } catch (IOException | ParseException e) {
              throw X_Debug.rethrow(e);
            }
          }
        }

      }

      if (Math.random() > -1) {
        return;
      }
      final CompilationUnitTree cup = null; // TODO possibly delete this... :-/
      processAnnotation(cup, element);
    }));
    if (roundEnv.processingOver()) {
      // we are done collecting up XApi annotations; lets generate some code!
      annotatedTypes.forEach(type->generate(null, type));
    }
    return true;
  }

  private void processAnnotation(CompilationUnitTree cup, Element element) {
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
        annotatedTypes.add(new XApiInjectionConfiguration(xapi, name.toString(), element, cup));
        break;
      default:
        throw new UnsupportedOperationException("Unsupported location of XApi annotation ("+element.getKind()+") : " + element);
    }
  }

  private void generate(CompilationUnitTree cup, XApiInjectionConfiguration config) {
    final Element type = config.getElement();

    final ClassTree tr = trees.getTree((TypeElement) type);
    final java.util.List<InjectionBinding> results = new InjectionTargetSearchVisitor(service, cup)
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
