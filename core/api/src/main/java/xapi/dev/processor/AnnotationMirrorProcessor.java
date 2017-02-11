package xapi.dev.processor;

import xapi.annotation.reflect.MirroredAnnotation;
import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;

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
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.FileNotFoundException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Set;

/**
 * This is the annotation processor for our injection library.
 *
 * It scans for our injection annotations and writes manifests to speed up
 * injection. default implementations go in META-INF/instances or
 * META-INF/singletons.
 *
 * Platform specific injection types go into META-INF/$type/instances,
 * META-INF/$type/singletons.
 *
 * It is included in the core api because it is run on the core api; every jar
 * built will include these manifests, whether they are used at runtime or not.
 *
 * Gwt injection does not look at the manifests, and a bytecode enhancer is in
 * the works to further process jre environments, by replacing calls to X_Inject
 * with direct access of the injected type.
 *
 * Final builds (bound to pre-deploy) can also be further enhanced, by replacing
 * all references to an injected interface with it's replacement type (changing
 * all class references, and changing invokeinterface to invokespecial). This
 * may be unnecessary once javac optimize=true after replacing the X_Inject call
 * site.
 *
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */

@SupportedAnnotationTypes({ "xapi.annotation.reflect.MirroredAnnotation" })
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AnnotationMirrorProcessor extends AbstractProcessor {

  private final HashMap<String, AnnotationManifest> generatedMirrors;

  public AnnotationMirrorProcessor() {
    generatedMirrors = new HashMap<String, AnnotationManifest>();
  }

  protected Filer filer;
  private TypeMirror annoType;
  private TypeMirror stringType;
  private TypeMirror classType;
  private TypeMirror enumType;

  @Override
  public final synchronized void init(final ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    filer = processingEnv.getFiler();
    final Elements elements = processingEnv.getElementUtils();
    final Types types = processingEnv.getTypeUtils();
    annoType = elements.getTypeElement(Annotation.class.getName()).asType();
    stringType = elements.getTypeElement(String.class.getName()).asType();
    classType = types.erasure(elements.getTypeElement(Class.class.getName())
        .asType());
    enumType = types.erasure(elements.getTypeElement(Enum.class.getName())
        .asType());
  }

  @Override
  public boolean process(final Set<? extends TypeElement> annotations,
      final RoundEnvironment roundEnv) {
    for (final TypeElement anno : annotations) {
      for (final Element el : roundEnv.getElementsAnnotatedWith(anno)) {
        final MirroredAnnotation mirrorSettings = el.getAnnotation(MirroredAnnotation.class);
        final AnnotationManifest manifest = addAnnotation((TypeElement) el);
          if (mirrorSettings.generateJavaxLangModelFactory()) {
            generateJavaxFactory((TypeElement)el, manifest);
          }
          if (mirrorSettings.generateXapiBytecodeFactory()) {
            generateXapiFactory((TypeElement)el, manifest);
          }
          if (mirrorSettings.generateReflectionFactory()) {
        }
      }
    }
    for (final String key : generatedMirrors.keySet().toArray(
        new String[generatedMirrors.size()])) {
      try {
        final AnnotationManifest mirror = generatedMirrors.get(key);
        if (mirror == null) {
          continue;
        }
        final String code = mirror.generated;
        log("Generating " + key);
        final String genClass = key + "Builder";

        // Delete previous class, or build will break.
        try {
          final int ind = genClass.lastIndexOf('.');
          final String pkg = genClass.substring(0, ind);
          final String name = genClass.substring(ind+1)+".class";
          final FileObject file = filer.getResource(StandardLocation.CLASS_OUTPUT, pkg, name);
          final CharSequence content = file.getCharContent(true);
          if (code.equals(content.toString())) {
            continue;
          } else {
            file.delete();
          }
        } catch (final FileNotFoundException ignored) {
        } catch (final Exception e) {
          e.printStackTrace();
        }

        final JavaFileObject src = filer.createSourceFile(genClass);

        final Writer out = src.openWriter();
        out.write(code);
        generatedMirrors.put(key, null);
        out.close();
      } catch (final Exception e) {
        e.printStackTrace();
        log("Unable to write annotation reflection for " + key + ";");
      }
    }

    return roundEnv.processingOver();
  }

  private void generateJavaxFactory(final TypeElement el, final AnnotationManifest manifest) {

  }

  private void generateXapiFactory(final TypeElement el, final AnnotationManifest manifest) {

  }

  private AnnotationManifest addAnnotation(final TypeElement element) {
    final String annoName = element.getQualifiedName().toString();
    if (generatedMirrors.containsKey(annoName)) {
      return generatedMirrors.get(annoName);
    }
    final AnnotationManifest manifest = new AnnotationManifest(annoName);
    generatedMirrors.put(annoName, manifest);
    final PackageElement pkg = processingEnv.getElementUtils().getPackageOf(element);
    final String simpleName = element.getSimpleName().toString();
    final String builderName = simpleName + "Builder";

    final SourceBuilder<Object> sb = new SourceBuilder<Object>(
        "public class " + builderName);
    final ClassBuffer annoBuilder = sb.getClassBuffer();
    if (!pkg.isUnnamed()) {
      sb.setPackage(pkg.getQualifiedName().toString());
    }

    // Create an immutable, private class that implements the annotation.
    final ClassBuffer immutableAnno = annoBuilder
        .addAnnotation("@SuppressWarnings(\"all\")")
        .createInnerClass("private static class Immutable" + simpleName)
        .addInterfaces(annoName).makeFinal();
    immutableAnno
        .createMethod(
            "public Class<? extends java.lang.annotation.Annotation> annotationType()")
        .returnValue(annoName + ".class");
    final Fifo<String> requiredFields = new SimpleFifo<String>();
    final Fifo<String> ctorParams = new SimpleFifo<String>();
    final Types types = processingEnv.getTypeUtils();

    final MethodBuffer addValue = annoBuilder.createMethod("public " + builderName + " addValue(String key, Object value)")
        .println("switch(key){").indent();

    for (final ExecutableElement method : ElementFilter.methodsIn(element
        .getEnclosedElements())) {
      final TypeMirror returnMirror = method.getReturnType();
      final String fieldName = method.getSimpleName().toString();
      final String fieldType = annoBuilder.addImport(returnMirror.toString());
      final AnnotationValue dflt = method.getDefaultValue();
      manifest.addMethod(method.getSimpleName(), returnMirror, dflt);
      final FieldBuffer annoField = annoBuilder
          .createField(returnMirror.toString(),
              method.getSimpleName().toString(),
              Modifier.PRIVATE);
      int mod = Modifier.PUBLIC | Modifier.FINAL;
      annoField.addGetter(mod);
      final MethodBuffer setter = annoField.addSetter(mod);

      if (fieldType.contains("[]")) {
        setter.setParameters(fieldType.replace("[]", " ...") + " "+annoField.getName());
      }

      addValue
          .println("case \"" + annoField.getName()+"\":")
          .indentln(setter.getName() +"((" + returnMirror.toString()+ ")value);")
          .indentln("break;")
      ;

      final FieldBuffer field = immutableAnno
          .createField(returnMirror.toString(),
              method.getSimpleName().toString(),
              Modifier.PRIVATE | Modifier.FINAL)
          .setExactName(true);
      field.addGetter(Modifier.PUBLIC | Modifier.FINAL);

      final String param = field.getSimpleType() + " " + field.getName();
      ctorParams.give(param);
      if (dflt == null) {
        requiredFields.give(param);
      }
      switch (returnMirror.getKind()) {
      case DECLARED:
        if (types.isAssignable(returnMirror, annoType)) {
          addAnnotation((TypeElement) ((DeclaredType) returnMirror).asElement());

          if (dflt != null) {
            final AnnotationMirror value = (AnnotationMirror) dflt.getValue();
            // use this annotation mirror to create a suitable factory for defaults
          }
          manifest.setAnnoType(annoName, fieldName);
        } else if (types.isAssignable(returnMirror, classType)) {

        } else if (types.isAssignable(returnMirror, stringType)) {

        } else if (types.isAssignable(returnMirror, enumType)) {

        }
        break;
      case ARRAY:
        final TypeMirror component = ((ArrayType) returnMirror).getComponentType();
        // System.out.println(component);
        // System.out.println(classType);
        if (types.isAssignable(component, annoType)) {
          // gross.
          addAnnotation((TypeElement) ((DeclaredType) component).asElement());
//          manifest.setArrayOfAnnos(name)
        } else if (types.isAssignable(component, classType)) {

        } else if (types.isAssignable(component, stringType)) {

        } else if (types.isAssignable(component, enumType)) {

        } else {
          switch (component.getKind()) {
          case BOOLEAN:
          case BYTE:
          case CHAR:
          case SHORT:
          case INT:
          case FLOAT:
          case LONG:
          case DOUBLE:
            break;
          default:
            throw new IllegalArgumentException("Unsupported type: " + component
                + " of " + method);
          }
        }
        break;
      case BOOLEAN:
      case BYTE:
      case CHAR:
      case SHORT:
      case INT:
      case FLOAT:
      case LONG:
      case DOUBLE:
        break;
      default:
        throw new IllegalArgumentException("Unsupported type: " + returnMirror
            + " of " + method);
      }
      // field.setInitializer(dflt.toString());
    }

    addValue.println("default:")
        .indent()
        .println("assert false : \"Unhandled type \" + key + \" for type \" + getClass();")
        .throwException(IllegalArgumentException.class, "\"Invalid key: \"+key")
        .outdent()
        .println("}")
        .returnValue("this")
    ;

    if (requiredFields.size() == 0) {
      // With no required fields,
      // We can create a zero-arg constructor and static accessor method
      annoBuilder.createMethod(
          "public static " + builderName + " build" + simpleName + "()")
          .returnValue("new " + builderName + "()");
      annoBuilder.createMethod("private " + builderName + "()");
    } else {
      final SimpleFifo<String> joinable = new SimpleFifo<String>();
      final MethodBuffer ctor = annoBuilder.createMethod("private " + builderName
          + "(" + requiredFields.join(", ") + ")");
      for (String field : requiredFields.forEach()) {
        field = field.substring(field.lastIndexOf(' ') + 1);
        joinable.give(field);
        ctor.println("this." + field + " = " + field + ";");
      }
      annoBuilder.createMethod(
          "public static " + builderName + " build" + simpleName + "("
              + requiredFields.join(", ") + ")").returnValue(
          "new " + builderName + "(" + joinable.join(", ") + ")");
    }
    if (ctorParams.size() > 0) {
      final MethodBuffer ctor = immutableAnno.createMethod("private "
          + immutableAnno.getSimpleName() + "()");
      final MethodBuffer build = annoBuilder.createMethod("public " + simpleName
          + " build()");
      final SimpleFifo<String> fieldRefs = new SimpleFifo<String>();
      for (final String param : ctorParams.forEach()) {
        final int end = param.lastIndexOf(' ');
        ctor.addParameters(param);
        final String paramName = param.substring(end + 1);
        fieldRefs.give(paramName);
        ctor.println("this." + paramName + " = " + paramName + ";");
      }
      build.returnValue("new Immutable" + simpleName + "("
          + fieldRefs.join(", ") + ")");
    }
    log(sb.toString());
    manifest.generated = sb.toString();
    return manifest;
  }

  private void log(final String string) {
    if (isDebug()) {
      System.out.println(string);
    }
  }

  protected boolean isDebug() {
    return false; // X_Runtime.isDebug();
  }

}



/**
 * Rather than parse through the clunky javax.lang.model,
 * we create a simple, ugly, but straightforward collection of
 * all of the fields of a known type into the annotation manifest object.
 *
 * If a given field type is present, the corresponding type map will have a
 * key for the name of the field, and a value string to refine the given
 * type (like specifying the generic bounds of a class, the subclass of Enum,
 * or, in the case of other annotations, the name of a static, runtime method that
 * produces the given annotation value.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
class AnnotationManifest {
  AnnotationManifest(final String name) {
    this.name = name;
  }
  public void setAnnoType(final String annoName, final String fieldName) {
    if (annotationFields == null) {
      annotationFields = new HashMap<String, String>();
    }
    annotationFields.put(fieldName, annoName);
  }
  public void addMethod(final Name simpleName, final TypeMirror returnMirror,
      final AnnotationValue dflt) {
    if (dflt != null) {
      defaultValues.put(simpleName.toString(), dflt);
    }

  }
  String name;
  String generated;
  final HashMap<String, AnnotationValue> defaultValues = new HashMap<String, AnnotationValue>();
  HashMap<String, String> annotationFields;
  HashMap<String, String> annotationArrayFields;
  HashMap<String, String> classFields;
  HashMap<String, String> classArrayFields;
  HashMap<String, String> enumFields;
  HashMap<String, String> enumArrayFields;
  HashMap<String, String> stringFields;
  HashMap<String, String> stringArrayFields;
  HashMap<String, String> primitiveFields;
  HashMap<String, String> primitivArrayFields;

}
