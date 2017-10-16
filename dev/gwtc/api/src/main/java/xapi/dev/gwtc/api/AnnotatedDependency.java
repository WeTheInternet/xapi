package xapi.dev.gwtc.api;

import xapi.annotation.compile.Dependency;

import java.lang.reflect.AnnotatedElement;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/15/17.
 */
public class AnnotatedDependency {
  private final Dependency dependency;
  private final AnnotatedElement source;

  public AnnotatedDependency(Dependency dependency, AnnotatedElement source) {
    this.dependency = dependency;
    this.source = source;
  }

  public Dependency getDependency() {
    return dependency;
  }

  public AnnotatedElement getSource() {
    return source;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof AnnotatedDependency))
      return false;

    final AnnotatedDependency dep = (AnnotatedDependency) o;

    if (!dependency.equals(dep.dependency))
      return false;
    return source.equals(dep.source);

  }

  @Override
  public int hashCode() {
    int result = dependency.hashCode();
    result = 31 * result + source.hashCode();
    return result;
  }
}
