package xapi.javac.dev.api;

import com.github.javaparser.ast.visitor.TransformVisitor.Transformer;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.inject.X_Inject;
import xapi.javac.dev.model.InjectionBinding;
import xapi.javac.dev.model.XApiInjectionConfiguration;
import xapi.javac.dev.template.TemplateTransformer;
import xapi.source.read.JavaModel;
import xapi.source.read.JavaModel.IsType;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileManager;
import java.util.Optional;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/12/16.
 */
public interface JavacService {

  static JavacService instanceFor(JavacTask task) {
    if (task instanceof BasicJavacTask) {
      return instanceFor(((BasicJavacTask)task).getContext());
    }
    throw new UnsupportedOperationException("Unable to create javac service from JavacTask " + task);
  }

  static JavacService instanceFor(Context context) {
    JavacService instance = context.get(JavacService.class);
    if (instance == null) {
      instance = X_Inject.instance(JavacService.class);
      context.put(JavacService.class, instance);
    }
    instance.init(context);
    return instance;
  }
  static JavacService instanceFor(ProcessingEnvironment processingEnv) {
    if (processingEnv instanceof JavacProcessingEnvironment) {
      return instanceFor(((JavacProcessingEnvironment)processingEnv).getContext());
    }
    throw new UnsupportedOperationException("Unable to create javac service from processing environment " + processingEnv);
  }

  String getPackageName(CompilationUnitTree cu);

  TypeMirror findType(ExpressionTree init);

  void init(Context context);

  ClassTree getClassTree(CompilationUnitTree compilationUnit);

  Optional<InjectionBinding> getInjectionBinding(XApiInjectionConfiguration config, TypeMirror type);

  InjectionBinding createInjectionBinding(VariableTree node);

  InjectionBinding createInjectionBinding(MethodTree node);

  IsType getInvocationTargetType(CompilationUnitTree cup, MethodInvocationTree node);

  JavaModel.IsNamedType getName(CompilationUnitTree cup, MethodInvocationTree node);

  ClassTree getEnclosingClass(CompilationUnitTree cup, Tree node);

  void readProperties(In2<String, String> in);

  <T, C extends Class<T>> T remember(C cls, T value);

  <T, C extends Class<T>> T recall(C cls);

  default <T, C extends Class<T>> T getOrCreate(C cls, In1Out1<C, T> factory) {
    T inst = recall(cls);
    if (inst == null) {
      inst = factory.io(cls);
      remember(cls, inst);
    }
    return inst;
  }

  default Transformer getTransformer() {
    return getOrCreate(Transformer.class, cls->new TemplateTransformer());
  }

  default JavaFileManager getFiler() {
    return recall(JavaFileManager.class);
  }

  String getFileName(CompilationUnitTree cup);

  String getQualifiedName(CompilationUnitTree cup, ClassTree classTree);

  default String getQualifiedName(CompilationUnitTree cup) {
    return getQualifiedName(cup, getClassTree(cup));
  }

}
