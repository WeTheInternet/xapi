package xapi.dev.gwtc.api;

import xapi.annotation.ui.UiTemplate_Builder;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.Gwtc.AncestorMode;
import xapi.gwtc.api.GwtcXmlBuilder;
import xapi.util.X_Runtime;
import xapi.string.X_String;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.reflect.shared.GwtReflect;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/15/17.
 */
class GwtcUnit {
  private final GwtcUnitType type;

    public GwtcUnit(Class<?> from) {
    gwtc = from.getAnnotation(Gwtc.class);
    source = from;
    type = GwtcUnitType.Class;
  }

  public GwtcUnit(Method from) {
    gwtc = from.getAnnotation(Gwtc.class);
    source = from;
    type = GwtcUnitType.Method;
  }

  public GwtcUnit(Package from) {
    gwtc = from.getAnnotation(Gwtc.class);
    source = from;
    type = GwtcUnitType.Package;
  }

  protected final Gwtc gwtc;
  protected GwtcXmlBuilder xml;
  protected UiTemplate_Builder html;
  protected final List<Package> packages = new ArrayList<Package>();
  protected final List<Class<?>> classes = new ArrayList<Class<?>>();
  public final Object source;
  private GwtcUnit parent;
  private Set<GwtcUnit> children = new LinkedHashSet<GwtcUnit>();

  public String generateGwtXml(String pkg, String name, Gwtc gwtc) {
    xml = GwtcXmlBuilder.generateGwtXml(pkg, name, "", isDebug(), gwtc);
    xml.addSource("");
    return xml.getInheritName();
  }

  protected boolean isDebug() {
    return parent == null ? X_Runtime.isDebug() : parent.isDebug();
  }

  public boolean isFindAllParents() {
    for (AncestorMode mode : gwtc.inheritanceMode()) {
      if (mode == AncestorMode.INHERIT_ALL_PARENTS) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("incomplete-switch")
  public boolean isFindParent() {
    for (AncestorMode mode : gwtc.inheritanceMode()) {
      switch (mode) {
        case INHERIT_ALL_PARENTS:
        case INHERIT_ONE_PARENT:
          return true;
      }
    }
    return false;
  }

  public boolean isFindChild() {
    for (AncestorMode mode : gwtc.inheritanceMode()) {
      if (mode == AncestorMode.INHERIT_CHILDREN) {
          return true;
      }
    }
    return false;
  }

  public boolean isFindEnclosingClasses() {
    for (AncestorMode mode : gwtc.inheritanceMode()) {
      if (mode == AncestorMode.INHERIT_ENCLOSING_CLASSES) {
        return true;
      }
    }
    return false;
  }

  public boolean isFindSuperClasses() {
    for (AncestorMode mode : gwtc.inheritanceMode()) {
      if (mode == AncestorMode.INHERIT_SUPER_CLASSES) {
        return true;
      }
    }
    return false;
  }

  public void setParent(GwtcUnit parentNode) {
    parent = parentNode;
    parent.children.add(this);
  }

  public boolean hasParent() {
    return parent != null;
  }

  /**
   * Finds the next parent element annotated with @Gwtc.
   * <br/>
   * This method should NOT be used to recurse parent hierarchy;
   * instead use {@link #getParent()}
   *
   * @return the next parent with @Gwtc, if there is one.
   */
  public Object getParent() {
    if (!isFindParent()) {
      return null;
    }
    final boolean findAll = isFindAllParents();
    Object o = source;
    Class<?> c;
    switch (type) {
      case Method:
        if (!isFindEnclosingClasses()) {
          return null;
        }
        o = c = ((Method) o).getDeclaringClass();
        if (c.isAnnotationPresent(Gwtc.class)) {
          return c;
        } else if (!findAll) {
          return null;
        }
        // fallthrough
      case Class:
        c = (Class<?>) o;
        if (isFindEnclosingClasses()) {
          Class<?> search = c;
          while (!GwtcGeneratedProject.isObjectOrNull(search.getDeclaringClass())) {
            search = search.getDeclaringClass();
            if (search.isAnnotationPresent(Gwtc.class)) {
              return search;
            }
            if (!findAll) {
              break;
            }
          }
        }
        if (isFindSuperClasses()) {
          Class<?> search = c;
          while (!GwtcGeneratedProject.isObjectOrNull(search.getSuperclass())) {
            search = search.getSuperclass();
            if (search.isAnnotationPresent(Gwtc.class)) {
              return search;
            }
            if (!findAll) {
              break;
            }
          }
        }
        o = c.getPackage();
        // fallthrough
      case Package:
        Package p = (Package) o;
        String pkg = p.getName();
        if ("".equals(pkg)) {
          return null;
        }
        do {
          pkg = X_String.chopOrReturnEmpty(pkg, ".");
          p = GwtReflect.getPackage(pkg);
          if (p != null) {
            if (p.isAnnotationPresent(Gwtc.class)) {
              return p;
            }
            if (!findAll) {
              return null;
            }
          }
        } while (pkg.length() > 0);
    }
    return null;
  }

  public void addChild(GwtcUnit data) {
    children.add(data);
    assert data.parent == null || data.parent == this :
        "GwtcUnit " + data + " already has a parent; " + data.parent +
            "; cannot set " + this + " as new parent.";
    data.parent = this;
  }

  @Override
  public String toString() {
    return "GwtcUnit " + source + " " + type;
  }

  public Iterable<GwtcUnit> getChildren() {
    return children;
  }

  public GwtcUnitType getType() {
    return type;
  }

}
