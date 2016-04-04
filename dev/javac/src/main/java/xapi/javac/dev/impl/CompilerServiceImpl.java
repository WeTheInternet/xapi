package xapi.javac.dev.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import xapi.annotation.inject.SingletonDefault;
import xapi.fu.Out2;
import xapi.fu.Rethrowable;
import xapi.io.X_IO;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.model.CompilationUnitTaskList;
import xapi.javac.dev.model.CompilerSettings;
import xapi.javac.dev.model.InjectionBinding;
import xapi.javac.dev.search.InjectionTargetSearchVisitor;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.util.X_Debug;

import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
@SingletonDefault(implFor = CompilerService.class)
public class CompilerServiceImpl implements CompilerService, Rethrowable {

  private JavacService service;

  Map<String, CompilationUnitTaskList> cups = new HashMap<>();
  Set<CompilationUnitTaskList> unfinished = new HashSet<>();
  List<Runnable> onFinish = new ArrayList<>();
  boolean finished;

  @Override
  public void init(JavacService service) {
    this.service = service;
  }

  @Override
  public Out2<Integer, URL> compileFiles(CompilerSettings settings, String ... files) {
    int result = com.sun.tools.javac.Main.compile(settings.toArguments(files), new PrintWriter(System.out));
    File f = new File(settings.getOutputDirectory());
    try {
      return Out2.out2(result, f.toURI().toURL());
    } catch (MalformedURLException e) {
      throw rethrow (e);
    }
  }

  @Override
  public void record(CompilationUnitTree cup) {
    JCCompilationUnit unit = (JCCompilationUnit) cup;
    String name = service.getQualifiedName(cup);
    if (cups.containsKey(name)) {
      cups.get(name).setUnit(unit);
    } else {
      cups.put(name, new CompilationUnitTaskList(name, unit));
    }
    CompilationUnitTaskList pcu = cups.get(name);
    final List<InjectionBinding> bindings = new InjectionTargetSearchVisitor(service, unit)
        .scan(unit, new ArrayList<>());

  }

  @Override
  public void onCompilationUnitFinished(String name, Consumer<CompilationUnitTree> callback) {
    CompilationUnitTaskList pcu = cups.get(name);
    if (pcu == null) {
      pcu = new CompilationUnitTaskList(name, null);
      cups.put(name, pcu);
      finished = false;
    }
    pcu.onFinished(callback);
  }

  @Override
  public void onFinished(Runnable r) {
    if (finished) {
      r.run();
    } else {
      onFinish.add(r);
    }
  }

  protected void finish(CompilationUnitTree cup, BasicJavacTask task) {
    String name = service.getQualifiedName(cup);
    final CompilationUnitTaskList pcu = cups.get(name);
    pcu.finish();
    boolean almostDone = unfinished.size() == 1;
    unfinished.remove(pcu);
    if (almostDone && unfinished.isEmpty()) {
      // all compilation units have been parsed.  Lets clear out all the pending units
            clearPendingUnits(task);
    }
  }

  protected void clearPendingUnits(BasicJavacTask task) {
    List<CompilationUnitTaskList> missing = new ArrayList<>();
    cups.values()
        .stream()
        .filter(pcu -> pcu.getUnit() == null)
        .forEach(missing::add);
    if (!missing.isEmpty()) {
      Context ctx = new Context(task.getContext());
      ctx.put(JavacService.class, service);
      MultiTaskListener tasks = MultiTaskListener.instance(ctx);
      tasks.add(getTaskListener(task));
      JavacProcessingEnvironment env = JavacProcessingEnvironment.instance(ctx);
      try (
          JavacFileManager jfm = new JavacFileManager(ctx, false, Charset.forName("UTF-8"))
      ) {
        List<JavaFileObject> missingFiles = new ArrayList<>();
        missing.forEach(pcu -> {
          String name = pcu.getName();
          TypeElement element = task.getElements().getTypeElement(name);
          if (element == null) {
            X_Log.info(getClass(), "No element found for ", name);
          } else {

            Name binary = env.getElementUtils().getBinaryName(element);
            String relativeName = binary.toString().split("[$]")[0];// TODO check for enclosing types instead of this hack
            JavaFileObject javaFile;
            try {
              javaFile = jfm.getJavaFileForInput(StandardLocation.SOURCE_OUTPUT, relativeName, JavaFileObject.Kind.SOURCE);
              if (javaFile == null) {
                javaFile = jfm.getJavaFileForInput(StandardLocation.SOURCE_PATH, relativeName, JavaFileObject.Kind.SOURCE);
                if (javaFile == null) {
                  javaFile = jfm.getJavaFileForInput(StandardLocation.CLASS_PATH, relativeName, JavaFileObject.Kind.SOURCE);
                  if (javaFile == null) {
                    X_Log.warn(getClass(), "Unable to find file ", relativeName, " to recompile...");
                  }
                }
              }
              if (javaFile != null){
                missingFiles.add(javaFile);
              }
            } catch (Throwable e) {
              X_Log.warn(getClass(), "Unable to compile source for "+binary, e);
              throw X_Debug.rethrow(e);
            }
            //            pcu.setUnit(cu);
            //            pcu.finish();
          }
        });
        JavaCompiler compiler = JavaCompiler.instance(ctx);
        final com.sun.tools.javac.util.List<JavaFileObject> javaFiles = com.sun.tools.javac.util.List.from(missingFiles.toArray(new JavaFileObject[missingFiles.size()]));
        compiler.compile(javaFiles);
      } catch (Throwable throwable) {
        throw X_Debug.rethrow(throwable);
      }
    }
    doFinish();
  }

  @Override
  public void overwriteCompilationUnit(CompilationUnitTree cup, String newSource) {
    String pkg = service.getPackageName(cup);
    String fileName = service.getFileName(cup);
    final CompilationUnit parsed;
    final String originalSource;
    try (
        InputStream in = cup.getSourceFile().openInputStream()
    ){
      parsed = JavaParser.parse(X_IO.toStreamUtf8(newSource), "UTF-8");
      originalSource = X_IO.toStringUtf8(in);
    } catch (IOException | ParseException e) {
      throw X_Debug.rethrow(e);
    }
    final PackageDeclaration packageDcl = parsed.getPackage();
    String currentPackage = packageDcl.getName().getName();
    final String distPackage = outputPackage();
    final String tmpPackage = workingPackage();
    final NameExpr parsedName = parsed.getPackage().getName();
    if (currentPackage.startsWith(tmpPackage)) {
      // already running in tmp.  check if this file has been finalized or not.

      parsedName.setName(parsedName.getName().replace(tmpPackage+".", distPackage+"."));

    } else if (currentPackage.startsWith(distPackage)) {
      // throw exception... do not overwrite stuff in final output location!
    } else {
      // move the file into /tmp package and recompile until it becomes stable.
      parsedName.setName(X_Source.qualifiedName(tmpPackage, parsedName.getName()));
    }
    if (!parsedName.getName().equals(pkg)) {
      try {
        parsed.getImports().add(JavaParser.parseImport("import "+pkg+".*;"));
      } catch (ParseException e) {
       throw rethrow(e);
      }
    }
    String finalSource = parsed.toSource(service.getTransformer());
    if (!originalSource.equals(finalSource)) {
      // TODO add Generated annotation which includes hashes of original files, and steps along the ways
      final JavaFileManager filer = service.getFiler();
      try {
        final FileObject output = filer.getFileForOutput(
            StandardLocation.SOURCE_OUTPUT,
            parsedName.getName(),
            fileName + ".java",
            cup.getSourceFile()
        );
        try (
            Writer writer = output.openWriter()
        ) {
          writer.append(finalSource);
        }
        // TODO: add to compile queue
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }

  protected void doFinish() {
    finished = true;
    final List<Runnable> copy;
    synchronized (onFinish) {
      copy = new ArrayList<>(onFinish);
      onFinish.clear();
    }
    copy.forEach(Runnable::run);
    copy.clear();
  }

  @Override
  public TaskListener getTaskListener(JavacTask task) {
    BasicJavacTask javacTask = (BasicJavacTask) task;
    return new TaskListener() {
      @Override
      public void started(TaskEvent e) {
        javacTask.getContext().put(JavacService.class, service);
        if (e.getKind() == Kind.ANALYZE) {
          record(e.getCompilationUnit());
        }
      }

      @Override
      public void finished(TaskEvent e) {
        if (e.getKind() == Kind.ANALYZE) {
          finish(e.getCompilationUnit(), javacTask);
          clearPendingUnits(javacTask);
        }
      }
    };
  }
}
