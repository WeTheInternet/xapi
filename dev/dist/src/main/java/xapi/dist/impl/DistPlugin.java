package xapi.dist.impl;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.api.JavacService;

import static xapi.javac.dev.api.CompilerService.compileServiceFrom;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/12/17.
 */
public class DistPlugin implements Plugin {

    private TaskListener listener;
    private Trees trees;
    private JavacService service;
    private DistGenerator generator;

    @Override
    public String getName() {
        return "DistPlugin";
    }

    @Override
    public void init(JavacTask javac, String... strings) {
        final BasicJavacTask task = (BasicJavacTask)javac;
        trees = Trees.instance(javac);
        generator = DistGenerator.GENERATOR.get();
        service = JavacService.instanceFor(javac);
        CompilerService compilerService = compileServiceFrom(service);
        task.addTaskListener(compilerService.getTaskListener(task));
    }
}
