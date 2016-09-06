package xapi.javac.dev.plugin;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.api.JavacService;

import static xapi.javac.dev.api.CompilerService.compileServiceFrom;

public class XapiCompilerPlugin implements Plugin {

  private TaskListener listener;
  private Trees trees;
  private JavacService service;

  @Override
  public String getName() {
    return "XapiCompilerPlugin";
  }

  @Override
  public void init(JavacTask javac, String... args) {
    if ("true".equals(System.getProperty("xapi.no.javac.plugin"))) {
      return;
    }
    final BasicJavacTask task = (BasicJavacTask)javac;
    trees = Trees.instance(javac);
    service = JavacService.instanceFor(javac);
    CompilerService compilerService = compileServiceFrom(service);
    task.addTaskListener(compilerService.getTaskListener(task));
  }

}
