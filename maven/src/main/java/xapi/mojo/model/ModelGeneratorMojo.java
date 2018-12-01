package xapi.mojo.model;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import xapi.annotation.model.IsModel;
import xapi.bytecode.ClassFile;
import xapi.bytecode.impl.BytecodeAdapterService;
import xapi.dev.model.HasModelFields;
import xapi.dev.model.ModelField;
import xapi.dev.scanner.X_Scanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.fu.Lazy;
import xapi.log.X_Log;
import xapi.model.api.Model;
import xapi.mojo.api.AbstractXapiMojo;
import xapi.mvn.X_Maven;
import xapi.source.api.HasQualifiedName;
import xapi.source.api.IsAnnotation;
import xapi.source.api.IsClass;
import xapi.source.api.IsMethod;
import xapi.util.X_Debug;
import xapi.util.api.ConvertsValue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map.Entry;


/**
 * Examines the classpath for interfaces extending {@link Model},
 * or any type annotated with {@link IsModel}, and then builds
 * implementation classes for the given type.
 *
 * @author <a href="mailto:james@wetheinter.net">James X. Nelson</a>
 * @version $Id$
 */
@Mojo
(
  name="modelgen"
 ,defaultPhase = LifecyclePhase.PROCESS_CLASSES
 ,requiresDependencyResolution = ResolutionScope.COMPILE
 ,threadSafe=true
)
public class ModelGeneratorMojo extends AbstractXapiMojo {


  @SuppressWarnings("unchecked")
  @Override
  protected void doExecute() throws MojoExecutionException, MojoFailureException {
    // Start by scanning the class folders
    MavenProject project = getSession().getCurrentProject();
    Build build = project.getBuild();
    ClasspathResourceMap classFolder // The classes we just compiled
      = X_Scanner.scanFolder(build.getOutputDirectory(), true, false, false, "");
    ClasspathResourceMap environment // The complete compile-scope classpath
      = X_Maven.compileScopeScanner(project, getSession());
    final BytecodeAdapterService adapter = X_Maven.compileScopeAdapter(project, getSession());

    // IdentityHashMap is faster, as it uses == for comparison,
    // and we know our ClassFiles come from the same map
    IdentityHashMap<ClassFile, Integer> models =
        new IdentityHashMap<ClassFile, Integer>();

    // While we iterate the result of the classes we just compiled,
    // The full compile classpath scan is running in separate threads
    for (ClassFile model : classFolder.findImplementationOf(Model.class)) {
      models.put(model, 1);
    }
    for (ClassFile model : classFolder.findClassAnnotatedWith(IsModel.class)) {
      if (!models.containsKey(model))
        models.put(model, 0);
    }

    // Build implementations for our models
    for (Iterator<Entry<ClassFile, Integer>> i = models.entrySet().iterator(); i.hasNext();) {
      Entry<ClassFile, Integer> entry = i.next();
      if (entry.getValue().equals(0)) {
        buildPojo(entry.getKey(), adapter, environment);
      } else {
        buildModel(entry.getKey(), adapter, environment);
      }
      i.remove();
    }

    // Run a javac for annotation processing.
  }

  private final Lazy<ConvertsValue<IsAnnotation, IsModel>> builder = Lazy.deferred1(() ->
      new ConvertsValue<IsAnnotation, IsModel>() {
        Method cls;
        @Override
        public IsModel convert(IsAnnotation from) {
          try {
            if (cls == null) {
              cls = Class.forName(IsModel.class.getName()+"Proxy", true, Thread.currentThread().getContextClassLoader())
                  .getDeclaredMethod("build", IsAnnotation.class);
            }
            return (IsModel) cls.invoke(null, from);
          } catch (Throwable e) {
            throw X_Debug.rethrow(e);
          }
        }
    }
  );

  void buildModel(ClassFile clsFile, BytecodeAdapterService adapter, ClasspathResourceMap classpath) {
    X_Log.info("Building model ",clsFile);
    IsClass cls = adapter.toClass(clsFile.getName());
    IsAnnotation modelAnno = cls.getAnnotation(IsModel.class.getName());
    HasModelFields model = new HasModelFields();
    if (modelAnno != null) {
      IsModel isModel = builder.out1().convert(modelAnno);
      model.setDefaultSerializable(isModel.serializable());
      model.setDefaultPersistence(isModel.persistence());
      model.setKey(isModel.key());
    }
    // Collect all model methods
    collectModelFields(model, cls, adapter);
  }

  private void collectModelFields(HasModelFields model, IsClass cls,
      BytecodeAdapterService adapter) {
    if (cls.isInterface()) {
      // Interface-only models don't have to look at any fields or existing methods
      // They will also be able to select superclass based on platform type
      for (IsMethod method : cls.getMethods()) {

        if (HasQualifiedName.isJavaLangObject(method.getEnclosingType()))
          continue;
        if (HasModelFields.isModel(method.getEnclosingType()))
          continue;
        ModelField field = model.getOrMakeField(normalizeName(method.getName()));
        X_Log.info("Field",field);
        boolean unknownType = true;
        for (IsAnnotation anno : method.getAnnotations()) {
          X_Log.info("Anno",anno);
        }
        if (unknownType) {
          //Unknown type; we have to guess if it's a getter, setter, adder, remover.
          Annotation guessed = guessType(method);
        }
      }
      for (IsClass iface : cls.getInterfaces()) {

      }
    } else {
      // Classes will only have abstract methods filled in, and serializers built
      for (IsMethod method : cls.getMethods()) {
        if (method.isAbstract()) {

        }
      }
    }
  }

  private Annotation guessType(IsMethod method) {
    if (method.getParameters().length==0) {
      // might be a getter or remover
      if (method.getName().startsWith("rem")||method.getName().startsWith("clear")) {
      }
    } else {
      // might be a setter, adder or remover
    }
    return null;
  }

  private String normalizeName(String name) {
    X_Log.info("Method w/ name",name);
    return name;
  }

  private void buildPojo(ClassFile key, BytecodeAdapterService adapter, ClasspathResourceMap classpath) {
    X_Log.info("Building pojo ",key);

  }

}
