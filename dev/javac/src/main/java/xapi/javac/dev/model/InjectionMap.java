package xapi.javac.dev.model;

import xapi.api.Scope;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;

import java.util.Optional;
import java.util.Properties;

/**
 * TODO:  For types mapped as a service / singleton,
 * and annotated w/ @XApi,
 * we should generate an X_ServName class for ServName or ServNameService interfaces,
 * which performs the static binding and automatically maps the instance methods to static ones.
 *
 * In order to add extra methods to the X_ServName static class,
 * the ServName interface can define a static method that can (optionally)
 * take a ServName instance as the first param:
 *
 * interface ServName {
 *
 *   String doStuff(int i);
 *
 *   static String otherStuff(boolean b) {
 *     return b?"yea":"nay";
 *   }
 *
 *   static String otherStuff(ServName service, boolean b, int i) {
 *      return otherStuff(b) + service.doStuff(i);
 *   }
 *
 * }
 *
 * should generate:
 *
 * public final class X_ServName {
 *
 *   private static final X_ServName INST = X_Inject.singleton(X_ServName.class);
 *
 *   private X_ServName(){}
 *
 *   static String doStuff(int 1) {
 *     return INST.doStuff(i);
 *   }
 *
 *   static String otherStuff(boolean b) {
 *     return ServName.otherStuff(b);
 *   }
 *
 *   static String otherStuff(boolean b, int i) {
 *     return ServName.otherStuff(INST, b, b);
 *   }
 *
 * }
 *
 * in the future,
 * when a dist compile occurs,
 * the final type injected could elevate all instance methods to static ones,
 * and rewrite the slower interface method invocations with slightly faster
 * (and slightly better to optimize) static methods in the X_ServName class itself,
 * so code which can be further inlined,
 * potentially as far as erasing the service interface entirely
 * (if the service instance has any mutable fields, it will not likely be erasable).
 *
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/13/16.
 */
public class InjectionMap {

  StringTo<StringTo<InjectionBinding>> injections;

  public InjectionMap() {
    injections = X_Collect.newStringDeepMap(InjectionBinding.class);
  }

  public void registerInjection(String scopeClass, InjectionBinding binding) {
    if (scopeClass == null) {
      scopeClass = Scope.class.getName();
    }
    injections.get(scopeClass).put(binding.getInjectionType(), binding);
  }

  public void loadFromProperties(Properties properties) {

  }

  public void fillProperties(Properties properties) {
    injections.entries().forEach(scopeMap->{
      String scope = scopeMap.getKey();
      final StringTo<InjectionBinding> values = scopeMap.getValue();
      values.entries().forEach(result->{
        String injectionType = result.getKey();
        final InjectionBinding binding = result.getValue();



      });
    });
  }

  public Optional<InjectionBinding> getBinding(String scopeClass, String typeName) {
    if (injections.containsKey(scopeClass)) {
      return Optional.ofNullable(injections.get(scopeClass).get(typeName));
    }
    return Optional.empty();
  }
}
