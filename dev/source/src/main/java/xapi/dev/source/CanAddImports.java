package xapi.dev.source;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/24/16.
 */
public interface CanAddImports {

  ImportSection getImports();

  default String addImport(Class<?> cls) {
    return getImports().addImport(cls);
  }

  default String addImport(String cls) {
    return getImports().addImport(cls);
  }

  default String addImportStatic(Class<?> cls, String name) {
    return getImports().addStaticImport(cls, name);
  }

  default String addImportStatic(String cls) {
    return getImports().addStaticImport(cls);
  }


}
