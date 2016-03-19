package xapi.javac.dev.api;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import xapi.inject.X_Inject;
import xapi.javac.dev.model.InjectionBinding;
import xapi.javac.dev.model.XApiInjectionConfiguration;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import java.util.Optional;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/12/16.
 */
public interface JavacService {

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

}
