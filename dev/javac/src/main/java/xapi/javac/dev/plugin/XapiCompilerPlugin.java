package xapi.javac.dev.plugin;

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
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.model.InjectionBinding;
import xapi.javac.dev.model.PendingCompilationUnit;
import xapi.javac.dev.search.InjectionTargetSearchVisitor;
import xapi.javac.dev.util.NameUtil;
import xapi.log.X_Log;
import xapi.util.X_Debug;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class XapiCompilerPlugin implements Plugin {

  Map<String, PendingCompilationUnit> cups = new HashMap<>();
  Set<PendingCompilationUnit> unfinished = new HashSet<>();
  List<Runnable> onFinish = new ArrayList<>();
  boolean finished;
  private TaskListener listener;
  private Trees trees;
  private JavacService service;

  @Override
  public String getName() {
    return "XapiCompilerPlugin";
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
    trees = Trees.instance(javac);
    service = JavacService.instanceFor(((BasicJavacTask) javac).getContext());
    listener = new TaskListener() {
      @Override
      public void started(TaskEvent e) {
        task.getContext().put(XapiCompilerPlugin.class, XapiCompilerPlugin.this);
        if (e.getKind() == Kind.ANALYZE) {
          JCCompilationUnit unit = (JCCompilationUnit) e.getCompilationUnit();
          processUnit(unit, task);
        }
      }

      @Override
      public void finished(TaskEvent e) {
        if (e.getKind() == Kind.ANALYZE) {
          clearPendingUnits(task);
        }
      }
    };
    task.addTaskListener(listener);
  }

  protected void processUnit(JCCompilationUnit unit, BasicJavacTask task) {
    String name = NameUtil.getName(unit);
    if (cups.containsKey(name)) {
      cups.get(name).setUnit(unit);
    } else {
      cups.put(name, new PendingCompilationUnit(name, unit));
    }
    PendingCompilationUnit pcu = cups.get(name);
    final List<InjectionBinding> bindings = new InjectionTargetSearchVisitor(service, trees)
        .scan(unit, new ArrayList<>());
    pcu.finish();
    boolean almostDone = unfinished.size() == 1;
    unfinished.remove(pcu);
    if (almostDone && unfinished.isEmpty()) {
      // all compilation units have been parsed.  Lets clear out all the pending units
//      clearPendingUnits(task);
    }
  }

  protected void clearPendingUnits(BasicJavacTask task) {
    List<PendingCompilationUnit> missing = new ArrayList<>();
    cups.values()
      .stream()
      .filter(pcu -> pcu.getUnit() == null)
      .forEach(missing::add);
    if (!missing.isEmpty()) {
      Context ctx = new Context(task.getContext());
      ctx.put(XapiCompilerPlugin.class, this);
      MultiTaskListener tasks = MultiTaskListener.instance(ctx);
      tasks.add(listener);
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
                javaFile = jfm.getJavaFileForInput(StandardLocation.CLASS_PATH, relativeName, JavaFileObject.Kind.SOURCE);
                missingFiles.add(javaFile);
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

  public Trees getTrees() {
    return trees;
  }

}
