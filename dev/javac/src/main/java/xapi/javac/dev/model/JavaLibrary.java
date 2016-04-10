package xapi.javac.dev.model;

import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.javac.dev.api.JavacService;

/**
 * A collection of {@link JavaDocument}s.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/9/16.
 */
@InstanceDefault(implFor = JavaLibrary.class)
public class JavaLibrary {

  private final StringTo<JavaDocument> cache;

  public JavaLibrary() {
    this(X_Collect.newStringMap(JavaDocument.class));
  }

  public JavaLibrary(StringTo<JavaDocument> cache) {this.cache = cache;}

  public void initialize(JavacService service) {
    service.getCompilerService().peekOnCompiledUnits(cache.adapter(JavaDocument::getTypeName));
  }
}
