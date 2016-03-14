package xapi.javac.dev.model;

import xapi.api.Scope;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;

import java.util.Properties;

/**
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
}
