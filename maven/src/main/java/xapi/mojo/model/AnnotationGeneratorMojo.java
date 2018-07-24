package xapi.mojo.model;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import xapi.annotation.reflect.MirroredAnnotation;
import xapi.bytecode.ClassFile;
import xapi.bytecode.impl.BytecodeAdapterService;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.log.X_Log;
import xapi.mojo.api.AbstractXapiMojo;
import xapi.mvn.X_Maven;
import xapi.source.X_Modifier;
import xapi.source.api.IsAnnotation;
import xapi.source.api.IsAnnotationValue;
import xapi.source.api.IsClass;
import xapi.source.api.IsMethod;
import xapi.source.api.IsType;
import xapi.source.api.Primitives;
import xapi.util.X_Debug;
import xapi.util.X_Namespace;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This goal will lookup all annotation that are themselves
 * annotated with {@link MirroredAnnotation}, and then
 * generate runtime annotation proxy classes to enable
 * creating instances of annotations at runtime, either
 * manually through AnnoNameBuilder objects,
 * or by wrapping {@link IsAnnotation} instances into AnnoNameProxy objects.
 *
 * @author <a href="mailto:james@wetheinter.net">James X. Nelson</a>
 * @version $Id$
 */
@Mojo(
    name="annogen"
    ,aggregator=true
    ,defaultPhase = LifecyclePhase.GENERATE_SOURCES
    ,requiresProject=true
    ,requiresDependencyCollection=ResolutionScope.COMPILE_PLUS_RUNTIME
    ,threadSafe = true
)
@Execute(phase=LifecyclePhase.PROCESS_CLASSES)
public class AnnotationGeneratorMojo extends AbstractXapiMojo{

  @Override
  @SuppressWarnings("unchecked")
  public void doExecute() throws MojoExecutionException, MojoFailureException {
    ClasspathResourceMap scanner = X_Maven.compileScopeScanner(getProject(), getSession());
    BytecodeAdapterService adapter = X_Maven.compileScopeAdapter(getProject(), getSession());
    Map<String, SourceBuilder<IsClass>> generated = new HashMap<String, SourceBuilder<IsClass>>();
    X_Log.trace(AnnotationGeneratorMojo.class, "Scanning compile scope dependencies", scanner);
    for (ClassFile mirror : scanner.findClassAnnotatedWith(MirroredAnnotation.class)) {
      X_Log.trace(AnnotationGeneratorMojo.class, "Adding annotation type", mirror);
      addType(mirror.getName(), mirror.getPackage(), generated, adapter);
    }

    final String[] cp = getAdditionalClasspath();
    X_Log.trace(AnnotationGeneratorMojo.class, "Compiling generated annotations with classpath",cp);
    ArrayList<Runnable> compileTasks = new ArrayList<Runnable>();

    for (String type : generated.keySet()) {
      final SourceBuilder<IsClass> builder = generated.get(type);
      final IsClass cls = builder.getPayload();
      final String clsName = cls.getSimpleName().replace("[]", "");
      ClassBuffer out = builder.getClassBuffer();
      MethodBuffer factory = out
            .createMethod("public static "+clsName+" build()")
            .addParameters(IsAnnotation.class.getName()+" from")
            .println(IsAnnotationValue.class.getSimpleName()+" __value;")
            .println(clsName+"Proxy proxy = new "+clsName+"Proxy(from);")
            ;
      MethodBuffer ctor = out.createMethod("private "+clsName+"Proxy()")
          .addParameters(IsAnnotation.class.getName()+" from");
      boolean firstDefault = true;
      for (IsMethod method : cls.getMethods()) {
        if (method.getEnclosingType().getQualifiedName().equals("java.lang.Object")) {
          continue;
        }
        String returnType = out.addImport(method.getReturnType().toString());
        IsClass returnClass = adapter.toClass(method.getReturnType().getQualifiedName().replace("[]", ""));
        if (!method.getName().equals("annotationType")) {
          out
            .createField(returnType, method.getName())
            .setExactName(true)
            .setModifier(X_Modifier.PRIVATE)
            .addGetter(X_Modifier.PUBLIC | X_Modifier.FINAL)
          ;
          IsAnnotationValue value = method.getDefaultValue();
          if (value != null) {
            // Set field default value
            if (firstDefault) {
              ctor.println(IsAnnotationValue.class.getSimpleName()+" __value;");
              firstDefault = false;
            }
            ctor.println("__value = from.getDefaultValue(from.getMethod(\"" +method.getName()+"\"));");
            if (method.getReturnType().getSimpleName().contains("[]")) {
              // TODO implement array builders
            } else {

            }
            ctor.println("this."+method.getName()+" = "+
                getExtractor(out, returnClass, method, returnType)+";");
          }
          // Add field setter in main factory method.
          // IsAnnotation.getValue() will return the default value if not set.
          factory.println("__value = from.getValue(from.getMethod(\"" +method.getName()+"\"));");
          factory.println("proxy."+method.getName()+" = " +
          		getExtractor(out, returnClass, method, returnType)+";");
        }
      }
      factory.returnValue("proxy");

      // Saves the source file to generated-sources directory, and returns a compile job
      String javaName = cls.getQualifiedName().replace("[]", "")+"Proxy";
      String source = builder.toString();
      File file = saveModel(javaName, source, true);
//      compileTasks.add(prepareCompile(file, javaName, source, true, cp));
    }
    ArrayList<Thread> runningTasks = new ArrayList<Thread>();
    // Actually run the compile.  We defer the compile until all source is generated.
    for (Runnable task : compileTasks) {
      Thread t = new Thread(task);
      t.setDaemon(true);
      t.start();
      runningTasks.add(t);
    }
    final long deadline = System.currentTimeMillis()+10000;
    while (!runningTasks.isEmpty()) {
      for (Iterator<Thread> iter = runningTasks.iterator();iter.hasNext();) {
        if (!iter.next().isAlive()) {
          iter.remove();
        }
      }
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw X_Debug.rethrow(e);
      }
      if (System.currentTimeMillis() > deadline) {
        throw new MojoExecutionException("Failed to compile annotation in 10 seconds.");
      }
    }
  }

  private String getExtractor(ClassBuffer out, IsClass returnClass,
      IsMethod method, String returnType) {
    if (returnClass.isAnnotation()) {
      String proxyType = out.addImport(returnClass.getQualifiedName()+"Proxy");
      return proxyType+".build((IsAnnotation)__value.getRawValue())";
    } else if (returnClass instanceof Primitives) {
      Primitives primitive = (Primitives) returnClass;
      return "(" +primitive.getObjectName()+")__value.getRawValue()";
    } else {
      return "(" +returnType+")__value.getRawValue()";
    }
  }

  private String[] getAdditionalClasspath() {
    return new String[]{
       findArtifact("net.wetheinter", "xapi-dev-bytecode", "jar", X_Namespace.XAPI_VERSION)
       ,findArtifact("net.wetheinter", "xapi-core-reflect", "jar", X_Namespace.XAPI_VERSION)
    };
  }

  private void addType(String annoName, String annoPackage,
      Map<String, SourceBuilder<IsClass>> generated,
      BytecodeAdapterService adapter) {
    SourceBuilder<IsClass> generator = generated.get(annoName);
    if (generator == null) {
      IsClass anno = adapter.toClass(annoName);
      generator = new SourceBuilder<IsClass>("public final class "+anno.getSimpleName()+"Proxy");
      generator
        .setPackage(annoPackage)
        .setPayload(anno)
        .getClassBuffer()
        .addInterface(annoName)
        .addImports(Annotation.class, IsAnnotation.class, IsAnnotationValue.class)
        .addAnnotation("@SuppressWarnings(\"all\")")// Ugly, but we don't need warnings from generated code
        .addAnnotation(generatedAnnotation())
      ;
      String clsName = generator.getImports().addImport(anno.getQualifiedName());
      generated.put(annoName, generator);
      for (IsMethod method : anno.getMethods()) {
        if (method.getEnclosingType().getQualifiedName().equals("java.lang.Object")) {
          continue;
        }
        if (method.getName().equals("annotationType")) {
          generator.getClassBuffer()
          .createMethod("public Class<? extends Annotation> annotationType()")
          .setModifier(X_Modifier.PUBLIC_FINAL)
          .returnValue(clsName+".class");
        }
        IsType ret = method.getReturnType();
        IsClass retClass = adapter.toClass(ret.getQualifiedName());
        if (retClass.isAnnotation()) {
          addType(retClass.getQualifiedName().replace("[]", ""), retClass.getPackage(), generated, adapter);
        }
      }
    }
  }

}
