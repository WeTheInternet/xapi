package xapi.dev.processor;

import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashMap;
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

import xapi.annotation.reflect.MirroredAnnotation;
import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;

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
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class AnnotationMirrorProcessor extends AbstractProcessor {
  
  private final HashMap<String, AnnotationManifest> generatedMirrors;
  private final HashMap<String, String> xapiProxy;
  private final HashMap<String, String> javaxProxy;

  public AnnotationMirrorProcessor() {
    generatedMirrors = new HashMap<String, AnnotationManifest>();
    xapiProxy = new HashMap<String, String>();
    javaxProxy = new HashMap<String, String>();
  }

  protected Filer filer;
  private TypeMirror annoType;
  private TypeMirror stringType;
  private TypeMirror classType;
  private TypeMirror enumType;

  @Override
  public final synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    filer = processingEnv.getFiler();
    Elements elements = processingEnv.getElementUtils();
    Types types = processingEnv.getTypeUtils();
    annoType = elements.getTypeElement(Annotation.class.getName()).asType();
    stringType = elements.getTypeElement(String.class.getName()).asType();
    classType = types.erasure(elements.getTypeElement(Class.class.getName())
        .asType());
    enumType = types.erasure(elements.getTypeElement(Enum.class.getName())
        .asType());
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {
    for (TypeElement anno : annotations) {
      for (Element el : roundEnv.getElementsAnnotatedWith(anno)) {
        MirroredAnnotation mirrorSettings = el.getAnnotation(MirroredAnnotation.class);
        AnnotationManifest manifest = addAnnotation((TypeElement) el);
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
    for (String key : generatedMirrors.keySet().toArray(
        new String[generatedMirrors.size()])) {
      try {
        AnnotationManifest mirror = generatedMirrors.get(key);
        if (mirror == null)
          continue;
        String code = mirror.generated;
        log("Generating " + key);
        String genClass = key + "Builder";
        
        // Delete previous class, or build will break.
        try {
          int ind = genClass.lastIndexOf('.');
          String pkg = genClass.substring(0, ind);
          String name = genClass.substring(ind+1)+".class";
          FileObject file = filer.getResource(StandardLocation.CLASS_OUTPUT, pkg, name);
          CharSequence content = file.getCharContent(true);
          if (code.equals(content.toString())) {
            continue;
          } else {
            file.delete();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
        
        JavaFileObject src = filer.createSourceFile(genClass);
        
        Writer out = src.openWriter();
        out.write(code);
        generatedMirrors.put(key, null);
        out.close();
      } catch (Exception e) {
        e.printStackTrace();
        log("Unable to write annotation reflection for " + key + ";");
      }
    }

    return roundEnv.processingOver();
  }

  private void generateJavaxFactory(TypeElement el, AnnotationManifest manifest) {
    
  }

  private void generateXapiFactory(TypeElement el, AnnotationManifest manifest) {
    
  }

  private AnnotationManifest addAnnotation(TypeElement element) {
    String annoName = element.getQualifiedName().toString();
    if (generatedMirrors.containsKey(annoName))
      return generatedMirrors.get(annoName);
    AnnotationManifest manifest = new AnnotationManifest(annoName);
    generatedMirrors.put(annoName, manifest);
    PackageElement pkg = processingEnv.getElementUtils().getPackageOf(element);
    String simpleName = element.getSimpleName().toString();
    String builderName = simpleName + "Builder";

    SourceBuilder<Object> sb = new SourceBuilder<Object>(
        "public class " + builderName);
    ClassBuffer annoBuilder = sb.getClassBuffer();
    if (!pkg.isUnnamed())
      sb.setPackage(pkg.getQualifiedName().toString());

    // Create an immutable, private class that implements the annotation.
    ClassBuffer immutableAnno = annoBuilder
        .addAnnotation("@SuppressWarnings(\"all\")")
        .createInnerClass("private static class Immutable" + simpleName)
        .addInterfaces(annoName).makeFinal();
    immutableAnno
        .createMethod(
            "public Class<? extends java.lang.annotation.Annotation> annotationType()")
        .returnValue(annoName + ".class");
    Fifo<String> requiredFields = new SimpleFifo<String>();
    Fifo<String> ctorParams = new SimpleFifo<String>();
    Types types = processingEnv.getTypeUtils();
    for (ExecutableElement method : ElementFilter.methodsIn(element
        .getEnclosedElements())) {
      String fieldName = method.getSimpleName().toString();
      AnnotationValue dflt = method.getDefaultValue();
      TypeMirror returnMirror = method.getReturnType();
      manifest.addMethod(method.getSimpleName(), returnMirror, dflt);
      annoBuilder
          .createField(returnMirror.toString(),
              method.getSimpleName().toString(),
              Modifier.PRIVATE)
          .addGetter(Modifier.PUBLIC | Modifier.FINAL)
          .addSetter(Modifier.PUBLIC | Modifier.FINAL);

      FieldBuffer field = immutableAnno
          .createField(returnMirror.toString(),
              method.getSimpleName().toString(),
              Modifier.PRIVATE | Modifier.FINAL)
          .setExactName(true)
          .addGetter(Modifier.PUBLIC | Modifier.FINAL);

      String param = field.getSimpleType() + " " + field.getName();
      ctorParams.give(param);
      if (dflt == null) {
        requiredFields.give(param);
      }
      switch (returnMirror.getKind()) {
      case DECLARED:
        if (types.isAssignable(returnMirror, annoType)) {
          addAnnotation((TypeElement) ((DeclaredType) returnMirror).asElement());
          
          if (dflt != null) {
            AnnotationMirror value = (AnnotationMirror) dflt.getValue();
            // use this annotation mirror to create a suitable factory for defaults
          }
          manifest.setAnnoType(annoName, fieldName);
        } else if (types.isAssignable(returnMirror, classType)) {

        } else if (types.isAssignable(returnMirror, stringType)) {

        } else if (types.isAssignable(returnMirror, enumType)) {

        }
        break;
      case ARRAY:
        TypeMirror component = ((ArrayType) returnMirror).getComponentType();
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
    if (requiredFields.size() == 0) {
      // With no required fields,
      // We can create a zero-arg constructor and static accessor method
      annoBuilder.createMethod(
          "public static " + builderName + " build" + simpleName + "()")
          .returnValue("new " + builderName + "()");
      annoBuilder.createMethod("private " + builderName + "()");
    } else {
      SimpleFifo<String> joinable = new SimpleFifo<String>();
      MethodBuffer ctor = annoBuilder.createMethod("private " + builderName
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
      MethodBuffer ctor = immutableAnno.createMethod("private "
          + immutableAnno.getSimpleName() + "()");
      MethodBuffer build = annoBuilder.createMethod("public " + simpleName
          + " build()");
      SimpleFifo<String> fieldRefs = new SimpleFifo<String>();
      for (String param : ctorParams.forEach()) {
        int end = param.lastIndexOf(' ');
        ctor.addParameters(param);
        String paramName = param.substring(end + 1);
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

  private void log(String string) {
//    if (X_Runtime.isDebug()) {
      System.out.println(string);
//    }
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
  AnnotationManifest(String name) {
    this.name = name;
  }
  public void setAnnoType(String annoName, String fieldName) {
    if (annotationFields == null)
      annotationFields = new HashMap<String, String>();
    annotationFields.put(fieldName, annoName);
  }
  public void addMethod(Name simpleName, TypeMirror returnMirror,
      AnnotationValue dflt) {
    if (dflt != null)
      defaultValues.put(simpleName.toString(), dflt);
    
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