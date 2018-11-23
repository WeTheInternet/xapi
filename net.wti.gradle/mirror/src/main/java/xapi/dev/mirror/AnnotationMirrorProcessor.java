package xapi.dev.mirror;

import xapi.annotation.reflect.MirroredAnnotation;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.FieldBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.fu.Do;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
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
import java.io.Serializable;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
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
@SupportedOptions(AnnotationMirrorProcessor.DISABLE_OPTION)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class AnnotationMirrorProcessor extends AbstractProcessor {

    public static final String DISABLE_OPTION = "xapi.no.javac.plugin";

    private final HashMap<String, AnnotationManifest> generatedMirrors;
    private boolean disabled;

    public AnnotationMirrorProcessor() {
        generatedMirrors = new HashMap<>();
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
        disabled = "true".equals(processingEnv.getOptions().getOrDefault(DISABLE_OPTION, "false"));
    }

    @Override
    public boolean process(final Set<? extends TypeElement> annotations,
                           final RoundEnvironment roundEnv) {
        if (disabled) {
            return roundEnv.processingOver();
        }
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
        for (final String key : generatedMirrors.keySet().toArray(new String[0])) {
            try {
                final AnnotationManifest mirror = generatedMirrors.get(key);
                if (mirror == null) {
                    continue;
                }
                final String code = mirror.generated;
                log("Generating " + key);
                final String genClass = builderName(key);

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

    private String builderName(String key) {
        return key + "_Builder";
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
        final String builderName = builderName(simpleName);

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
            .addInterface(annoName).makeFinal();
        immutableAnno
            .createMethod(
                "public Class<? extends java.lang.annotation.Annotation> annotationType()")
            .returnValue(annoName + ".class");
        final ChainBuilder<String> requiredFields = Chain.startChain();
        final ChainBuilder<String> ctorParams = Chain.startChain();
        final Types types = processingEnv.getTypeUtils();

        final MethodBuffer toString = immutableAnno
            .createMethod("public String toString()")
            .print("return new StringBuilder(\"") .print(simpleName) .println("\")")
            .indent();

        final MethodBuffer copyMethod = annoBuilder.createMethod(
            "public static " + builderName + " copy" + simpleName + "()")
            .addParameter(simpleName, "from")
            .print("return new " + builderName + "(");

        final MethodBuffer duplicateMethod = annoBuilder.createMethod(
            "public " + builderName + " duplicate()")
            .print("return new " + builderName + "(");

        final MethodBuffer addValue = annoBuilder.createMethod("public " + builderName + " addValue(String key, Object value)")
            .println("switch(key){").indent();

        Do afterLoop = Do.NOTHING;
        String duplicateComma = "";
        final List<ExecutableElement> methods = ElementFilter.methodsIn(element.getEnclosedElements());
        boolean many = methods.size() > 2;
        for (final ExecutableElement method : methods) {
            final TypeMirror returnMirror = method.getReturnType();
            final String fieldName = method.getSimpleName().toString();
            final String fieldType = annoBuilder.addImport(returnMirror.toString());
            final AnnotationValue dflt = method.getDefaultValue();
            manifest.addMethod(method.getSimpleName(), returnMirror, dflt);
            final FieldBuffer annoField = annoBuilder.createField(fieldType, fieldName, Modifier.PRIVATE);
            int mod = Modifier.PUBLIC | Modifier.FINAL;
            annoField.addGetter(mod);
            final MethodBuffer setter = annoField.addSetter(mod);


            if (fieldType.contains("[]")) {
                setter.setParameters(fieldType.replace("[]", " ...") + " "+annoField.getName());
            }
            addValue
                .println("case \"" + annoField.getName()+"\":")
                .indentln(setter.getName() +"((" + fieldType + ")value);")
                .indentln("break;")
            ;

            final FieldBuffer field = immutableAnno
                .createField(fieldType,
                    fieldName,
                    Modifier.PRIVATE | Modifier.FINAL)
                .setExactName(true);
            field.addGetter(Modifier.PUBLIC | Modifier.FINAL);

            if (many) {
                toString.print(".append(\"\\n\")");
            }
            // TODO: actually make the toString emit valid annotation source code instead of this crap:
            toString.append(".append(\"")
                .print(fieldName)
                .print(" : \" + ")
                .print(fieldName)
                .println(")");

            final String param = field.getSimpleType() + " " + field.getName();
            ctorParams.add(param);
            if (dflt == null) {
                requiredFields.add(param);
                copyMethod.print(duplicateComma + "from." + fieldName + "()");
                duplicateMethod.print(duplicateComma + fieldName);
                duplicateComma = ", ";
            } else {
                afterLoop = afterLoop.doAfter(()->{
                    copyMethod.indentln("." + setter.getName() + "(from." + fieldName + "())");
                    duplicateMethod.indentln("." + setter.getName() + "(" + fieldName + ")");
                });
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

        copyMethod.println(")");
        duplicateMethod.println(")");
        afterLoop.done();
        copyMethod.println(";");
        duplicateMethod.println(";");
        toString.println(".toString();");

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
            final ChainBuilder<String> joinable = Chain.startChain();
            final MethodBuffer ctor = annoBuilder.createMethod("private " + builderName
                + "(" + requiredFields.join(", ") + ")");
            for (String field : requiredFields) {
                field = field.substring(field.lastIndexOf(' ') + 1);
                joinable.add(field);
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
            final ChainBuilder<String> fieldRefs = Chain.startChain();
            for (final String param : ctorParams) {
                final int end = param.lastIndexOf(' ');
                ctor.addParameters(param);
                final String paramName = param.substring(end + 1);
                fieldRefs.add(paramName);
                ctor.println("this." + paramName + " = " + paramName + ";");
            }
            build.returnValue("new Immutable" + simpleName + "("
                + fieldRefs.join(", ") + ")");
        }
        // add this last, to avoid clouding the namespace
        immutableAnno.addInterface(Serializable.class);
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
