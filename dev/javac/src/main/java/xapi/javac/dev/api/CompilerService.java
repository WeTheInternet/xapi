package xapi.javac.dev.api;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskListener;
import xapi.fu.In1;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.Out2;
import xapi.inject.X_Inject;
import xapi.javac.dev.model.CompilerSettings;
import xapi.javac.dev.model.CompilerSettings.ImplicitMode;
import xapi.javac.dev.model.CompilerSettings.ProcessorMode;
import xapi.javac.dev.model.JavaDocument;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;
import xapi.util.X_Debug;
import xapi.util.X_String;

import static xapi.util.X_String.classToSourceFiles;

import javax.annotation.processing.Processor;
import javax.lang.model.element.TypeElement;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
public interface CompilerService {

  default CompilerSettings defaultSettings() {
    return new CompilerSettings();
  }

  Out2<Integer, URL> compileFiles(CompilerSettings settings, String ... files);

  default Out2<Integer, URL> compileClasses(CompilerSettings settings, Class ... classes) {
    return compileFiles(settings, X_String.classesToSourceFiles(classes));
  }

  static CompilerService compileServiceFrom(JavacService service) {
    return service.getOrCreate(CompilerService.class, cls->{
      final CompilerService result = X_Inject.singleton(CompilerService.class);
      result.init(service);
      return result;
    });
  }

  void init(JavacService service);

  void record(CompilationUnitTree cup, TypeElement typeElement);

  void peekOnCompiledUnits(In1<JavaDocument> callback);

  void onCompilationUnitFinished(String name, In1<CompilationUnitTree> callback);

  void onFinished(Runnable r);

  TaskListener getTaskListener(JavacTask task);

  void overwriteCompilationUnit(JavaDocument doc, String newSource);

  /**
   * When a magic method emits code that contains more magic methods,
   * we must recompile the source potentially many times,
   * so we create iterations of the file in a tmp package,
   * which you can easily exclude from final output in your build tool
   * (a cleanup mechanism is forthcoming)
   */
  default String workingPackage() {
    return "tmp";
  }

  /**
   * The final output of generated code will be made a subpackage of the original source.
   * This allows both original source and generated source to coexist nicely.
   *
   * A final dist build will do a massive whole-world compile
   * where final permutations of generated types can be promoted to the live source,
   * which will have all references updated to use dist.*.com.original.pkg.name.
   *
   * Although it may seem nice to promote the generated types on top of the original location,
   * you will encounter trouble as soon as you want to touch classes in java.* packages,
   * as ClassLoader class will refuse to load them unless they come from signed java jars.
   *
   * The counter to the java.* package name issue is that native methods must match signatures,
   * so classes with native methods [*1] cannot be repackaged
   * (thus, you cannot overwrite a java.* class with native methods, and expect it to work).
   *
   * [*1]: GWT native methods are fine to repackage, provided jsni references are updated correctly.
   *
   * Given all the issues either way, a mechanism will eventually be implemented to configure these options,
   * with the current default to prefer adding the extra package names
   */
  default String outputPackage() {
    return "dist";
  }

  boolean isGreedyCompiler();

  JavaDocument getDocument(CompilationUnitTree cup);

  default CompilerSettings settingsForClass(Class<?> cls) {
    String loc = X_Reflect.getFileLoc(cls);
    boolean test = loc.contains("test-classes");
    final CompilerSettings settings = defaultSettings()
          .setTest(test)
          .setClearGenerateDirectory(false)
          .resetGenerateDirectory()
          .setImplicitMode(ImplicitMode.CLASS)
          .setProcessorMode(ProcessorMode.Both);

    if (test) {
      settings.setOutputDirectory(loc);
      loc = loc.replace("target/test-classes", "target/generated-test-sources/test-annotations");
      settings.setGenerateDirectory(loc);
      loc = loc.replace("target/generated-test-sources/test-annotations", "src/test/java");
      settings.setSourceDirectory(loc);
    } else {
      settings.setOutputDirectory(loc);
      loc = loc.replace("target/classes", "target/generated-sources/annotations");
      settings.setGenerateDirectory(loc);
      loc = loc.replace("target/generated-sources/annotations", "src/main/java");
      settings.setSourceDirectory(loc);
    }
    return settings;
  }

  default Out2<Integer,URL> runAnnotationProcessor(Class<?> cls,
                                                   Class<? extends Processor> ... processors) {

    final CompilerSettings settings = settingsForClass(cls);
    if (processors != null && processors.length > 0) {
      String path = X_String.joinClasses(File.pathSeparator, X_Reflect::getFileLoc, processors);
      settings.setProcessorPath(path);
    }

    String loc = X_Reflect.getFileLoc(cls);
    boolean test = loc.contains("test-classes");
    if (test) {
      loc = loc.replace("target/test-classes", "src/test/java");
    } else {
      loc = loc.replace("target/classes", "src/main/java");
    }

    return compileFiles(settings, loc + classToSourceFiles(cls));

  }

  @SuppressWarnings("varargs")
  default Out2<Integer, URL> processAnnotationsAndRun(Class<?> clsIn,
                                                      In2Unsafe<ClassLoader, Class<?>> callback,
                                                      Class<? extends Processor> ... processors) {
    final Out2<Integer, URL> result = runAnnotationProcessor(clsIn, processors);

    assert result.out1() == 0 : "Annotation processing failed";

    final URLClassLoader cl = new URLClassLoader(new URL[]{result.out2()}, Thread.currentThread().getContextClassLoader());
    Thread runIn = new Thread(()->{
      try {
        callback.in(cl, cl.loadClass(clsIn.getCanonicalName()));
      } catch (ClassNotFoundException e) {
        X_Log.error(getClass(), "Compilation did not succeed for class", clsIn, e );
        throw X_Debug.rethrow(e);
      }
    });
    runIn.setContextClassLoader(cl);
    runIn.start();
    try {
      runIn.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw X_Debug.rethrow(e);
    }

    return result;
  }
}
