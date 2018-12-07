package xapi.dev.source;

import xapi.fu.itr.MappedIterable;

import static xapi.fu.itr.ArrayIterable.iterate;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 6/24/16.
 */
public interface CanAddImports extends HasIndent {

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

  default String addImportStatic(String cls, String name) {
    return getImports().addStaticImport(cls, name);
  }

  default String addImportStatic(String cls) {
    return getImports().addStaticImport(cls);
  }

  default String parameterizedType(Class<?> cls, String... typeParams) {
    return parameterizedType(addImport(cls), typeParams);
  }
  default String parameterizedType(String cls, String... typeParams) {

    String type;
    if (Character.isLowerCase(cls.charAt(0))) {
      type = addImport(cls);
    } else {
      type = cls;
    }
    if (typeParams.length == 0) {
      return type;
    }
    final MappedIterable<String> values = iterate(typeParams)
        .map(this::addImport);
    final String sourceName;
    if (typeParams.length > 2) {
      sourceName = type + "<\n" + values.join(",\n" + getIndent())+">";
    } else {
      sourceName = type + "<" + values.join(",") + ">";
    }

    return sourceName;
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
