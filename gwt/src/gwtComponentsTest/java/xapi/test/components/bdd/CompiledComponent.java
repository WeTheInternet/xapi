package xapi.test.components.bdd;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import xapi.dev.components.WebComponentFactoryGenerator;
import xapi.dev.gwtc.api.GwtcService;
import xapi.gwtc.api.GwtManifest;

import java.io.File;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/21/16.
 */
public class CompiledComponent {

  private final GwtcService gwtc;
  private final CompilationUnit parsed;
  private final ClassOrInterfaceDeclaration cls;
  private final GwtManifest manifest;

  public CompiledComponent(
      ClassOrInterfaceDeclaration cls,
      CompilationUnit parsed,
      GwtcService gwtc,
      GwtManifest manifest
  ) {
    this.cls = cls;
    this.parsed = parsed;
    this.gwtc = gwtc;
    this.manifest = manifest;
  }

  public GwtcService getGwtc() {
    return gwtc;
  }

  public CompilationUnit getParsed() {
    return parsed;
  }

  protected String getWebComponentFactorySimpleName() {
    return WebComponentFactoryGenerator.toFactoryName(cls.getName());
  }

  public File getWebComponentFactoryFile() {
    String simpleName = getWebComponentFactorySimpleName();
    String resourcePath = cls.getPackageAsPath() + simpleName + ".java";
    return new File(getGwtc().inGeneratedDirectory(getManifest(), resourcePath));
  }

  public GwtManifest getManifest() {
    return manifest;
  }
}
