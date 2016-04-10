package xapi.javac.dev.api;

import com.sun.source.tree.CompilationUnitTree;
import xapi.inject.X_Inject;
import xapi.javac.dev.model.JavaDocument;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
public interface SourceTransformationService {

  static SourceTransformationService instanceFrom(JavacService service) {
    return service.getOrCreate(
        SourceTransformationService.class,
        cls->{
          final SourceTransformationService inst = X_Inject.instance(SourceTransformationService.class);
          inst.init(service);
          return inst;
        }
    );
  }

  void init(JavacService service);

  InjectionResolver createInjectionResolver(CompilationUnitTree cup);

  void requestOverwrite(CompilationUnitTree cup, int startPos, int endPos, String newSource);

  void recordRepackage(JavaDocument doc, String oldPackage, String newPackage);
}
