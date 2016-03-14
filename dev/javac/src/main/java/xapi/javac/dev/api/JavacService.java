package xapi.javac.dev.api;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.util.Context;
import xapi.inject.X_Inject;

import javax.lang.model.type.TypeMirror;

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

  String getPackageName(CompilationUnitTree cu);

  TypeMirror findType(ExpressionTree init);

  void init(Context context);

  ClassTree getClassTree(CompilationUnitTree compilationUnit);
}
