package xapi.javac.dev.plugin;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.processing.Filer;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import xapi.javac.dev.util.NameUtil;
import xapi.log.X_Log;
import xapi.util.X_Debug;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacFiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;

public class ClassWorldPlugin implements Plugin {

  Map<String, PendingCompilationUnit> cups = new HashMap<>();
  Set<PendingCompilationUnit> unfinished = new HashSet<>();
  List<Runnable> onFinish = new ArrayList<>();
  boolean finished;
  private TaskListener listener;
  
  @Override
  public String getName() {
    return "ClassWorldPlugin";
  }

  public void onCompilationUnitFinished(String name, Consumer<JCCompilationUnit> callback) {
    PendingCompilationUnit pcu = cups.get(name);
    if (pcu == null) {
      pcu = new PendingCompilationUnit(name, null);
      cups.put(name, pcu);
      finished = false;
    }
    pcu.onFinished(callback);
  }
  
  public void onFinished(Runnable r) {
    if (finished) {
      r.run();
    } else {
      onFinish.add(r);
    }
  }
  
  @Override
  public void init(JavacTask javac, String... args) {
    final BasicJavacTask task = (BasicJavacTask)javac;
    listener = new TaskListener() {
      @Override
      public void started(TaskEvent e) {
        task.getContext().put(ClassWorldPlugin.class, ClassWorldPlugin.this);
        if (e.getKind() == Kind.ANALYZE) {
          JCCompilationUnit unit = (JCCompilationUnit) e.getCompilationUnit();
          String name = NameUtil.getName(unit);
          PendingCompilationUnit pcu;
          if (cups.containsKey(name)) {
            pcu = cups.get(name);
            pcu.setUnit(unit);
          } else {
            pcu = new PendingCompilationUnit(name, unit);
            cups.put(name, pcu);
          }
          unfinished.add(pcu);
        }
      }
      
      @Override
      public void finished(TaskEvent e) {
        if (e.getKind() == Kind.ANALYZE) {
          JCCompilationUnit unit = (JCCompilationUnit) e.getCompilationUnit();
          String name = NameUtil.getName(unit);
          if (cups.containsKey(name)) {
            cups.get(name).setUnit(unit);
          } else {
            cups.put(name, new PendingCompilationUnit(name, unit));
          }
          PendingCompilationUnit pcu = cups.get(name);
          pcu.finish();
          boolean almostDone = unfinished.size() == 1;
          unfinished.remove(pcu);
          if (almostDone && unfinished.isEmpty()) {
            // all compilation units have been parsed.  Lets clear out all the pending units
            clearPendingUnits(task);
          }
        }
      }
    };
    task.addTaskListener(listener);
  }
  
  public void maybeFinish(BasicJavacTask task) {
    if (!finished && unfinished.isEmpty()) {
      clearPendingUnits(task);
    }
  }

  protected void clearPendingUnits(BasicJavacTask task) {
    List<PendingCompilationUnit> missing = new ArrayList<>();
    cups.values()
      .stream()
      .filter(pcu -> pcu.getUnit() == null)
      .forEach(pcu -> {
        missing.add(pcu);
    });
    if (!missing.isEmpty()) {
      Context ctx = new Context(task.getContext());
      ctx.put(ClassWorldPlugin.class, this);
      MultiTaskListener tasks = MultiTaskListener.instance(ctx);
      tasks.add(listener);
      JavaCompiler compiler = JavaCompiler.instance(ctx);
      JavacProcessingEnvironment env = JavacProcessingEnvironment.instance(ctx);
      try (
          JavacFileManager jfm = new JavacFileManager(ctx, false, Charset.forName("UTF-8"))
      ) {
        missing.forEach(pcu -> {
          String name = pcu.getName();
          TypeElement element = task.getElements().getTypeElement(name);
          if (element == null) {
            X_Log.info(getClass(), "No element found for ", name);
          } else {
            Name binary = env.getElementUtils().getBinaryName(element);
            String relativeName = binary.toString().split("[$]")[0];// TODO check for enclosing types instead of this hack
            JavaFileObject javaFile;
            JCCompilationUnit cu;
            try {
              javaFile = jfm.getJavaFileForInput(StandardLocation.CLASS_PATH, relativeName, JavaFileObject.Kind.SOURCE);
              compiler.compile(com.sun.tools.javac.util.List.of(javaFile));
            } catch (Throwable e) {
              X_Log.warn(getClass(), "Unable to compile source for "+binary, e);
              throw X_Debug.rethrow(e);
            }
//            pcu.setUnit(cu);
//            pcu.finish();
          }
        });
      }finally {}
    }
    finished = true;
    onFinish.forEach(r -> r.run());
    onFinish.clear();
  }

  public Trees getTrees() {
    return null;
  }

}
