package xapi.inject.spi;

import java.util.Iterator;

import xapi.fu.has.HasId;
import xapi.inject.spi.RuntimeInjectorAbstract.ScopeMap;

public class InjectionScope implements Iterable<InjectionScope>, HasId {

  public static String normalize(String scope) {
    if (scope.endsWith("Platform"))
      scope = scope.substring(0, scope.length()-8);
    int ind = scope.lastIndexOf('.');
    if (ind > 0) {
      scope = scope.substring(ind+1);
    }
    return scope.toLowerCase().trim();
  }

  private final String[] scopes;
  private final String id;
  private final ScopeMap map;
  InjectionScope(String id, ScopeMap scopeMap) {
    this.scopes = id.split("/");
    this.map = scopeMap;
    this.id = id;
    for (int i = 0;i < scopes.length;i++) {
      // This is where we trim the platform and packagename of platforms
      scopes[i] = normalize(scopes[i]);
      assert scopes[i].length()>0;
    }
  }

  @Override
  public final String getId() {
    return id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this)
      return true;
    if (!(obj instanceof HasId))
      return false;
    return getId().equals(((HasId)obj).getId());
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public Iterator<InjectionScope> iterator() {
    return map.new ScopeIterator(scopes);
  }

}
