package xapi.javac.dev.plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.apache.tools.ant.filters.StringInputStream;

import xapi.io.X_IO;
import xapi.javac.dev.model.GwtCreateInvocationSite;
import xapi.javac.dev.util.ClassLiteralResolver;
import xapi.javac.dev.util.GwtCreateSearchVisitor;
import xapi.log.X_Log;
import xapi.util.X_String;
import xapi.util.X_Util;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LineMap;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.file.Locations;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

public class GwtCreatePlugin implements Plugin {

  @Override
  public String getName() {
    return "GwtCreatePlugin";
  }
  
  static class GwtCreateTransformation {
    String file;
    JavacProcessingEnvironment env;
    Map<int[], GwtCreateInvocationSite> edits;
    JCCompilationUnit compilationUnit;
    
    public GwtCreateTransformation(String file,
        Map<int[], GwtCreateInvocationSite> edits, JavacProcessingEnvironment env, JCCompilationUnit compilationUnit) {
      this.file = file;
      this.env = env;
      this.compilationUnit = compilationUnit;
      this.edits = edits;
    }
    
    protected String rebind(GwtCreateInvocationSite edit) {
      return "new "+  edit.getType().toString()+"()"; // For now, no rebinding
    }
    
    public void process() {
      for (int[] pos : edits.keySet()) {
        GwtCreateInvocationSite edit = edits.get(pos);
        int start = pos[0]-7; // Remove the method name, create(
        start -= 4; // Remove the qualifying GWT. portion; TODO handle static method ref
        file = file.substring(0, start)
               + rebind(edit)
               + file.substring(pos[1]+1);
      }
        String pkg = String.valueOf(X_Util.firstNotNull(compilationUnit.getPackageName(), ""));
        // TODO get the actual resources directory in use, to check what we're actually doing
        Filer filer = env.getFiler();
        String[] path = compilationUnit.getSourceFile().getName().split("src/test/resources/");
        String name = path[path.length-1];
        FileObject res;
        X_Log.info(getClass(), path);
        try {
          res = filer.createResource(StandardLocation.CLASS_OUTPUT, pkg, name);
        } catch (IOException e) {
          X_Log.error(getClass(), "Unable to create resource "+pkg+"."+name+" to write to", e);
          throw X_Util.rethrow(e);
        }
        X_Log.info(getClass(), file);
        // Save a super-sourced copy of this file in the generated directory
        X_Log.info(getClass(), "Writing to generated file "+res.getName());
        try (OutputStream out = res.openOutputStream()){
          X_IO.drain(out, new StringInputStream(file));
        } catch (IOException e) {
          X_Log.error(getClass(), "Unable to save resource to "+res+"."+name+" to write to", e);
          throw X_Util.rethrow(e);
        }
    }
  }

  List<GwtCreateTransformation> transforms = new ArrayList<>();
  
  @Override
  public void init(final JavacTask javac, String... args) {
    
    final BasicJavacTask task = (BasicJavacTask) javac;
    final JavacProcessingEnvironment env = JavacProcessingEnvironment.instance(task.getContext());
    final Trees trees = JavacTrees.instance(task);
    task.addTaskListener(new TaskListener() {
      
      @Override
      public void started(TaskEvent taskEvent) {}
      
      @Override
      public void finished(TaskEvent taskEvent) {
        if(taskEvent.getKind() == TaskEvent.Kind.ANALYZE) {
          ClassWorldPlugin classWorld = task.getContext().get(ClassWorldPlugin.class);
          final List<GwtCreateInvocationSite> results = new LinkedList<>();
          JCCompilationUnit compilationUnit = (JCCompilationUnit) taskEvent.getCompilationUnit();
          new GwtCreateSearchVisitor().scan(compilationUnit, results);
          new ClassLiteralResolver(results, classWorld).scan(compilationUnit, null);
          if (!results.isEmpty()) {
            Map<int[], GwtCreateInvocationSite> edits = new TreeMap<>((int[] a, int[] b) -> {
              assert a.length == 2;
              assert b.length == 2;
              return a[0] < b[0]? 1 : a[0] == b[0]? 0 : -1;
            });
            // load the original source file (in UTF-8)
            results.forEach(gwt -> {
              ExpressionTree source = gwt.getInvocation();
              JCTree ast = (JCTree)source;
              DiagnosticPosition pos = ast.pos();
              int start = pos.getStartPosition();
              int end = pos.getEndPosition(compilationUnit.endPositions);
              edits.put(new int[]{start, end}, gwt);
              X_Log.info(getClass(),  start + ":"+end,gwt);
            });
            String file;
            try (InputStream in = compilationUnit.getSourceFile().openInputStream()) {
              file = X_IO.toStringUtf8(in);
            } catch (IOException e) {
              e.printStackTrace();
              throw new RuntimeException(e);
            }
            GwtCreateTransformation transform = new GwtCreateTransformation(file, edits, env, compilationUnit);
            classWorld.onFinished(() -> {
              transform.process();
            });
            classWorld.maybeFinish(task);
          }
        }
      }
    });
  }


}
