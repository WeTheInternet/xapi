package xapi.dev.source;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/24/16.
 */
public interface CanAddImports {

  CanAddImports NO_OP = new NoOpImports();

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

class NoOpImports implements CanAddImports {

  @Override
  public ImportSection getImports() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String addImport(Class<?> cls) {
    return cls.getCanonicalName();
  }

  @Override
  public String addImport(String cls) {
    return cls;
  }

  @Override
  public String addImportStatic(Class<?> cls, String name) {
    return cls.getCanonicalName() + "." + name;
  }

  @Override
  public String addImportStatic(String cls) {
    return cls;
  }
}
