package xapi.javac.dev.impl;

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
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import xapi.annotation.inject.SingletonDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.In1;
import xapi.fu.Out2;
import xapi.fu.Rethrowable;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.api.SourceTransformationService;
import xapi.javac.dev.model.CompilationUnitTaskList;
import xapi.javac.dev.model.CompilerSettings;
import xapi.javac.dev.model.InjectionBinding;
import xapi.javac.dev.model.JavaDocument;
import xapi.javac.dev.search.InjectionTargetSearchVisitor;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.util.X_Debug;

import static xapi.fu.In2.ignoreFirst;

import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.File;
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
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
@SingletonDefault(implFor = CompilerService.class)
public class CompilerServiceImpl implements CompilerService, Rethrowable {

  private JavacService service;

  Map<String, CompilationUnitTaskList> cups = new HashMap<>();
  SortedMap<String, JavaDocument> documentsByType = new TreeMap<>();
  StringTo<StringTo<JavaDocument>> documentsInUnit = X_Collect.newStringDeepMap(JavaDocument.class);
  Set<CompilationUnitTaskList> unfinished = new HashSet<>();
  List<Runnable> onFinish = new ArrayList<>();
  List<In1<JavaDocument>> listeners = new ArrayList<>();
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
  public void record(CompilationUnitTree cup, TypeElement typeElement) {
    getOrMakeDocument(cup, typeElement);
    final List<InjectionBinding> bindings = new InjectionTargetSearchVisitor(service, cup)
        .scan(cup, new ArrayList<>());

  }

  private JavaDocument getOrMakeDocument(CompilationUnitTree cup, TypeElement typeElement) {
    String typeName = typeElement.getQualifiedName().toString();
    JavaDocument doc = documentsByType.get(typeName);
      doc = new JavaDocument(doc, service, cup, typeElement);
      documentsByType.put(typeName, doc);

    String cupName = doc.getCompilationUnitName();
    documentsInUnit.get(cupName).put(typeName, doc);

    if (cups.containsKey(cupName)) {
      final CompilationUnitTaskList existing = cups.get(cupName);
      existing.setUnit(cup);
    } else {
      cups.put(cupName, new CompilationUnitTaskList(cupName, cup));
    }

    CompilationUnitTaskList pcu = cups.get(cupName);
    pcu.onFinished(In1.ignored(()->
        documentsInUnit.get(cupName)
            // forBoth accepts an In2 type, but we only want to act on the second type, which is a JavaDocument,
            // so we use In2.ignoreFirst to call the finish() method on each document
            .forBoth(ignoreFirst(JavaDocument::finish))
    ));
    return doc;
  }

  @Override
  public void peekOnCompiledUnits(In1<JavaDocument> listener) {
    listeners.add(listener);
    documentsByType.values().forEach(listener.toConsumer());
  }

  @Override
  public void onCompilationUnitFinished(String name, In1<CompilationUnitTree> callback) {
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
            final javax.lang.model.element.Name qualified = element.getQualifiedName();
            Name binary = env.getElementUtils().getBinaryName(element);
            String relativeName = binary.toString().split("[$]")[0];// TODO check for enclosing types instead of this nasty hack
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
  public void overwriteCompilationUnit(JavaDocument doc, String newSource) {

    String pkg = doc.getPackageName();
    String fileName = doc.getFileName();
    final CompilationUnit parsed = doc.getAst();
    final String originalSource = doc.getSource();
    final PackageDeclaration packageDcl = parsed.getPackage();
    String currentPackage = packageDcl.getName().getName();
    final String distPackage = outputPackage();
    final String tmpPackage = workingPackage();
    final NameExpr parsedName = parsed.getPackage().getName();
    if (currentPackage.startsWith(tmpPackage)) {
      // already running in tmp.  check if this file has been finalized or not.

      parsedName.setName(parsedName.getName().replace(tmpPackage+".", distPackage+"."));

    } else if (currentPackage.startsWith(distPackage)) {
      // document has been finalized.
      // we may want to add listeners who only want to see a document after it has stabilized
      doc.finalize(service);
    } else {
      // move the file into /tmp package and recompile until it becomes stable.
      parsedName.setName(X_Source.qualifiedName(tmpPackage, parsedName.getName()));
    }
    if (!parsedName.getName().equals(pkg)) {
        // mutate all packages starting with the original package.
        // TODO: store up references to all original packages with a manager who can, at any time,
        // find all the import statements belonging to a given prefix,
        parsed.getImports().stream()
            .filter(importDecl -> importDecl.getName().getName().startsWith(pkg))
            .forEach(importDecl -> {
              String name = importDecl.getName().getName();
              name = name.replace(pkg, parsedName.getName());
              final NameExpr nameExpr = new NameExpr(name);
              importDecl.setName(nameExpr);
            });
      SourceTransformationService sources = service.getSourceTransformService();
      sources.recordRepackage(doc, pkg, parsedName.getName());
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
            doc.getSourceFile()
        );
        try (
            Writer writer = output.openWriter()
        ) {
          writer.append(finalSource);
        }
        if (isGreedyCompiler()) {
          onCompilationUnitFinished(doc.getAstName(), In1.noop());
        }
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }

  @Override
  public boolean isGreedyCompiler() {
    return true;
  }

  @Override
  public JavaDocument getDocument(CompilationUnitTree cup) {
    final String type = service.getQualifiedName(cup);
    return documentsByType.computeIfAbsent(type, ignored->
      getOrMakeDocument(cup, service.getElements().getTypeElement(type))
    );
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
          record(e.getCompilationUnit(), e.getTypeElement());
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
