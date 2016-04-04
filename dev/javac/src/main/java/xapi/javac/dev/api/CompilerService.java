package xapi.javac.dev.api;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskListener;
import xapi.fu.Out2;
import xapi.inject.X_Inject;
import xapi.javac.dev.model.CompilerSettings;
import xapi.util.X_String;

import java.net.URL;
import java.util.function.Consumer;

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
    return compileFiles(settings, X_String.classesToQualified(classes));
  }

  static CompilerService compileServiceFrom(JavacService service) {
    return service.getOrCreate(CompilerService.class, cls->{
      final CompilerService result = X_Inject.singleton(CompilerService.class);
      result.init(service);
      return result;
    });
  }

  void init(JavacService service);

  void record(CompilationUnitTree cup);

  void onCompilationUnitFinished(String name, Consumer<CompilationUnitTree> callback);

  void onFinished(Runnable r);

  TaskListener getTaskListener(JavacTask task);

  void overwriteCompilationUnit(CompilationUnitTree cup, String newSource);

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
}
