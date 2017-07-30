package xapi.javac.dev.template;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.Immutable;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.InjectionResolver;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.api.MagicMethodInjector;
import xapi.source.read.JavaModel.IsType;
import xapi.util.X_Debug;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
@InstanceDefault(implFor = TemplateInjector.class)
public class TemplateInjector implements MagicMethodInjector {

  private static final String KEY_START = "TemplateGenerator|";
  private JavacService service;
  private StringTo<Out1<TemplateGenerator>> generatorByTypeName;
  @Override
  public void init(JavacService service) {
    generatorByTypeName = X_Collect.newStringMap(Out1.class);
    this.service = service;
    service.readProperties((key, value)->{
      if (key.startsWith(KEY_START)) {
        key = key.substring(KEY_START.length());

        // TODO handle type assignability by using a much smarter map (AssignableMap, forthcoming)
        Class<TemplateGenerator> generator = loadClass(value);
        In2Out1<String, Class, TemplateGenerator> factory = this::createFactory;

        generatorByTypeName.put(key, factory.supply(value, generator));
      }
    });
  }

  private TemplateGenerator createFactory(String type, Class<? extends TemplateGenerator> cls) {
    final TemplateGenerator instance = X_Inject.instance(cls);
    if (cacheGenerators()) {
      generatorByTypeName.put(type, Immutable.immutable1(instance));
    }
    return instance;
  }

  protected boolean cacheGenerators() {
    return true;
  }

  private <T> Class<T> loadClass(String bit) {
    try {
      return (Class<T>) Thread.currentThread().getContextClassLoader().loadClass(bit);
    } catch (ClassNotFoundException e) {
      throw X_Debug.rethrow(e);
    }
  }

  @Override
  public boolean performInjection(
      CompilationUnitTree cup, MethodInvocationTree source, InjectionResolver resolver
  ) {
    final ExpressionTree template = source.getArguments().get(0);
    if (template instanceof LiteralTree) {
      LiteralTree literal = (LiteralTree) template;
      String value = (String) literal.getValue();
      final IsType type = service.getInvocationTargetType(cup, source);
      TemplateGenerator generator = findGenerator(cup, source, type, value);
      return generator.generateTemplate(service, cup, source, type, value, resolver);
    }
    return false;
  }

  private TemplateGenerator findGenerator(
      CompilationUnitTree cup,
      MethodInvocationTree source,
      IsType type,
      String value
  ) {
    final Out1<TemplateGenerator> generator = generatorByTypeName.get(type.toString());
    if (generator != null) {
      return generator.out1();
    }

    return (Service, Cup, Source, Type, Value, Resolver) -> false;
  }
}
