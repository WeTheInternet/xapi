package xapi.dev.processor;

import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileObject;

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

  private final HashMap<String, String> generatedMirrors;

  public AnnotationMirrorProcessor() {
    generatedMirrors = new HashMap<String, String>();
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
    classType = 
        types.erasure(
            elements.getTypeElement(Class.class.getName()).asType()
            );
    enumType = 
        types.erasure(
            elements.getTypeElement(Enum.class.getName()).asType()
            );
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {
    for (TypeElement anno : annotations) {
      for (Element el : roundEnv.getElementsAnnotatedWith(anno)) {
        addAnnotation((TypeElement) el);
      }
    }
    for (String key : generatedMirrors.keySet().toArray(
        new String[generatedMirrors.size()])) {
      try {
        String code = generatedMirrors.get(key);
        if (code == null)
          continue;
        log("Generating "+key);
        String genClass = key + "Builder";
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

  private void addAnnotation(TypeElement element) {
    String name = element.getQualifiedName().toString();
    System.out.println("Add Anno: "+name);
    if (generatedMirrors.containsKey(name))
      return;
    PackageElement pkg = processingEnv.getElementUtils().getPackageOf(element);
    String annoName = element.getQualifiedName().toString();
    String simpleName = element.getSimpleName().toString();
    String builderName = simpleName + "Builder";

    SourceBuilder<Object> sb = new SourceBuilder<Object>("public class "+builderName);
    ClassBuffer annoBuilder = sb.getClassBuffer();
    if (!pkg.isUnnamed())
      sb.setPackage(pkg.getQualifiedName().toString());
    
    // Create an immutable, private class that implements the annotation.
    ClassBuffer immutableAnno = annoBuilder
      .createInnerClass("private static class Immutable"+simpleName)
      .addInterfaces(annoName)
      .makeFinal()
    ;
    immutableAnno
      .createMethod("public Class<? extends java.lang.annotation.Annotation> annotationType()")
      .returnValue(annoName+".class")
    ;
    Fifo<String> immutables = new SimpleFifo<String>();
    Fifo<String> ctorParams = new SimpleFifo<String>();
    Elements elements = processingEnv.getElementUtils();
    Types types = processingEnv.getTypeUtils();
    for (ExecutableElement method : ElementFilter.methodsIn(element
        .getEnclosedElements())) {
      AnnotationValue dflt = method.getDefaultValue();
      TypeMirror returnMirror = method.getReturnType();

      annoBuilder
        .createField(returnMirror.toString(), method.getSimpleName().toString())
        .addGetter(Modifier.PUBLIC | Modifier.FINAL)
        .addSetter(Modifier.PUBLIC | Modifier.FINAL)
        ;
      
      FieldBuffer field = immutableAnno
        .createField(returnMirror.toString(), method.getSimpleName().toString())
        .setExactName(true)
        .addGetter(Modifier.PUBLIC | Modifier.FINAL)
      ;
      
      String param = field.getSimpleType()+" "+field.getName();
      ctorParams.give(param);
      if (dflt == null) {
        immutables.give(param);
      } 
        switch (returnMirror.getKind()) {
        case DECLARED:
          if (types.isAssignable(returnMirror, annoType)) {
            addAnnotation(
                (TypeElement)((DeclaredType)returnMirror).asElement()
                );
            if (dflt != null) {
              AnnotationMirror value = (AnnotationMirror) dflt.getValue();
              // use this annotation mirror to create a suitable default value factory
            }
          } else if (types.isAssignable(returnMirror, classType)) {
            
          } else if (types.isAssignable(returnMirror, stringType)) {

          } else if (types.isAssignable(returnMirror, enumType)) {
            
          }
          break;
        case ARRAY:
          TypeMirror component = ((ArrayType)returnMirror).getComponentType();
          System.out.println(component);
          System.out.println(classType);
          if (types.isAssignable(component, annoType)) {
            // gross.  
            addAnnotation(
                (TypeElement)((DeclaredType)component).asElement()
                );
          } else if (types.isAssignable(component, classType)) {
            
          } else if (types.isAssignable(component, stringType)) {

          } else if (types.isAssignable(component, enumType)) {
            
          } else {
            switch(component.getKind()) {
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
                throw new IllegalArgumentException("Unsupported type: " + component+" of "+method);
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
          throw new IllegalArgumentException("Unsupported type: "+returnMirror+" of "+method);
        }
//        field.setInitializer(dflt.toString());
      }
      
//      if (returnMirror instanceof ReferenceType) {
//        // We must take special care; class values of annotations cannot be
//        // loaded normally.
//        if (returnMirror instanceof ArrayType) {
//
//        } else {
//          assert returnMirror instanceof DeclaredType : "Unsupported annotation method type: "
//              + returnMirror + " on " + element;
//          Element asType = processingEnv.getTypeUtils().asElement(returnMirror);
//          switch (asType.getKind()) {
//          case ANNOTATION_TYPE:
//            // An annotation type has to be reparsed
//          case ENUM:
//          case CLASS:
//          default:
//            break;
//          }
//        }
//      } else {
//        PrimitiveType primitive = processingEnv.getTypeUtils()
//            .getPrimitiveType(returnMirror.getKind());
//        assert primitive != null;
//        // Plain ol' primitive; we can safely check default value in TypeMirror
//        if (dflt == null) {
//          // A required primitive
//        } else {
//          // An optional primitive
//        }
//      }
    if (ctorParams.size() > 0) {
      MethodBuffer ctor = immutableAnno.createMethod("private "+immutableAnno.getSimpleName()+"()");
      MethodBuffer build = annoBuilder.createMethod("public "+simpleName+" build()");
      SimpleFifo<String> fieldRefs = new SimpleFifo<String>();
      for (String param : ctorParams.forEach()) {
        int end = param.lastIndexOf(' ');
        ctor.addParameters(param);
        String paramName = param.substring(end+1);
        fieldRefs.give(paramName);
        ctor.println("this."+paramName+" = "+paramName+";");
      }
      build.returnValue("new Immutable"+simpleName+"("+fieldRefs.join(", ")+")");
    }
    log(sb.toString());
    generatedMirrors.put(name, sb.toString());
  }

  private void log(String string) {
    // if (X_Runtime.isDebug()) {
    System.out.println(string);
    // }
  }

}
