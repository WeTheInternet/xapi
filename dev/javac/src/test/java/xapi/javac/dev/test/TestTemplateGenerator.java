package xapi.javac.dev.test;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import xapi.annotation.inject.InstanceDefault;
import xapi.javac.dev.api.InjectionResolver;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.template.TemplateGenerator;
import xapi.source.read.JavaModel.IsType;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
@InstanceDefault(implFor = TestTemplateGenerator.class)
public class TestTemplateGenerator implements TemplateGenerator {
  @Override
  public boolean generateTemplate(
      JavacService service,
      CompilationUnitTree cup,
      MethodInvocationTree source,
      IsType type,
      String value,
      InjectionResolver resolver
  ) {
    value = value.replaceAll("([$])([A-Za-z][A-Za-z0-9_]+)", " $2 ");
    resolver.replace(source, "new Test(){{" + value + "}}");
    return true;
  }
}
