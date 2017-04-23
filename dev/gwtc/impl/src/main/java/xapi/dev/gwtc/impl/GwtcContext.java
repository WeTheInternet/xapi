package xapi.dev.gwtc.impl;

import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.Resource;
import xapi.annotation.ui.UiTemplateBuilder;
import xapi.bytecode.ClassFile;
import xapi.collect.api.InitMap;
import xapi.collect.impl.InitMapDefault;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.scanner.X_Scanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.dev.source.XmlBuffer;
import xapi.fu.MappedIterable;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.Gwtc.AncestorMode;
import xapi.gwtc.api.GwtcProperties;
import xapi.gwtc.api.GwtcXmlBuilder;
import xapi.inject.impl.SingletonProvider;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.util.X_Debug;
import xapi.util.X_Runtime;
import xapi.util.X_String;
import xapi.util.api.ConvertsValue;
import xapi.util.api.ReceivesValue;

import javax.inject.Provider;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.reflect.shared.GwtReflectJre;

@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public class GwtcContext {

  private static final ConvertsValue LIST_PROVIDER = new ConvertsValue() {
    @Override
    public Object convert(Object from) {
      return new ArrayList();
    }
  };
  private static final ConvertsValue<Package, String> PACKAGE_NAME =
      new ConvertsValue<Package, String>() {
        @Override
        public String convert(Package from) {
          return from.getName();
        }
      };
  private static final ConvertsValue<Object, String> ENTITY_NAME =
      new ConvertsValue<Object, String>() {
        @Override
        public String convert(Object from) {
          if (from instanceof Class) {
            return ((Class<?>) from).getName();
          } else if (from instanceof Method) {
            return ((Method) from).toGenericString();
          } else if (from instanceof Package) {
            return ((Package) from).getName();
          } else {
            X_Log.warn(getClass(), "Unsupported toString object type",
                from == null ? "null" : from.getClass(), from);
            return String.valueOf(from);
          }
        }
      };

  public class GwtcConverter implements ConvertsValue<Object, GwtcUnit> {

    @Override
    public GwtcUnit convert(Object from) {
      if (from instanceof Class) {
        return newGwtcData((Class<?>) from);
      } else if (from instanceof Method) {
        return newGwtcData((Method) from);
      } else if (from instanceof Package) {
        return newGwtcData((Package) from);
      } else {
        X_Log.warn(getClass(), "Unsupported toString object type", from == null ? "null" : from
            .getClass(), from);
      }
      return null;
    }

    protected GwtcUnit newGwtcData(Class<?> from) {
      return new GwtcUnit(from);
    }

    protected GwtcUnit newGwtcData(Package from) {
      return new GwtcUnit(from);
    }

    protected GwtcUnit newGwtcData(Method from) {
      return new GwtcUnit(from);
    }

  }

  protected static class GwtcUnit {
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
    protected UiTemplateBuilder html;
    protected final List<Package> packages = new ArrayList<Package>();
    protected final List<Class<?>> classes = new ArrayList<Class<?>>();
    public final Object source;
    private GwtcUnit parent;
    private Set<GwtcUnit> children = new LinkedHashSet<GwtcUnit>();

    public String generateGwtXml(Gwtc gwtc, String pkg, String name) {
      xml = GwtcXmlBuilder.generateGwtXml(gwtc, pkg, name);
      return xml.getInheritName();
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
        switch (mode){
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
      switch(type) {
        case Method:
          if (!isFindEnclosingClasses()) {
            return null;
          }
          o = c = ((Method)o).getDeclaringClass();
          if (c.isAnnotationPresent(Gwtc.class)) {
            return c;
          } else if (!findAll) {
            return null;
          }
          // fallthrough
        case Class:
          c = (Class<?>)o;
          if (isFindEnclosingClasses()) {
            Class<?> search = c;
            while (!isObjectOrNull(search.getDeclaringClass())) {
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
            while (!isObjectOrNull(search.getSuperclass())) {
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
        "GwtcUnit "+data+" already has a parent; "+ data.parent+
        "; cannot set "+this+" as new parent.";
      data.parent = this;
    }

    @Override
    public String toString() {
      return "GwtcUnit "+source+" "+type;
    }

    public Iterable<GwtcUnit> getChildren() {
      return children;
    }

    public GwtcUnitType getType() {
      return type;
    }

  }

  // =====================================================
  // ==Store the structure of related classpath entities==
  // =====================================================

  private final InitMap<Class<?>, List<Method>> methods =
      new InitMapDefault<Class<?>, List<Method>>(InitMapDefault.CLASS_NAME, LIST_PROVIDER);
  private final InitMap<Class<?>, List<Class<?>>> innerClasses =
      new InitMapDefault<Class<?>, List<Class<?>>>(InitMapDefault.CLASS_NAME, LIST_PROVIDER);
  private final InitMap<Class<?>, List<Class<?>>> superClasses =
      new InitMapDefault<Class<?>, List<Class<?>>>(InitMapDefault.CLASS_NAME, LIST_PROVIDER);
  private final InitMap<Package, List<Class<?>>> classes =
      new InitMapDefault<Package, List<Class<?>>>(PACKAGE_NAME, LIST_PROVIDER);
  private final InitMap<Package, List<Package>> packages =
      new InitMapDefault<Package, List<Package>>(PACKAGE_NAME, LIST_PROVIDER);

  private final InitMap<Object, GwtcUnit> nodes = new InitMapDefault<Object, GwtcUnit>(ENTITY_NAME, new GwtcConverter());

  // =====================================================
  // ==Store sets of all entities for run-once semantics==
  // =====================================================

  private final Set<Method> finishedMethods = new HashSet<Method>();
  private final Set<Class<?>> finishedClasses = new HashSet<Class<?>>();
  private final Set<Package> finishedPackages = new HashSet<Package>();

  // =====================================================
  // =====================================================
  // =====================================================

  public static class Dep {
    private final Dependency dependency;
    private final AnnotatedElement source;

    public Dep(Dependency dependency, AnnotatedElement source) {
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
      if (!(o instanceof Dep))
        return false;

      final Dep dep = (Dep) o;

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

  private final GwtcXmlBuilder module;
  private final Set<Dep> dependencies = new LinkedHashSet<>();
  private final Set<Resource> gwtXmlDeps = new LinkedHashSet<Resource>();
  private final Set<GwtcProperties> launchProperties = new LinkedHashSet<GwtcProperties>();
  private final Set<Object> needChildren = new HashSet<Object>();

  // =====================================================
  // ===Scan the classpath while looking added entities===
  // =====================================================

  private final Provider<ClasspathResourceMap> classpath;
  private final GwtcService gwtcService;
  private boolean debug = X_Runtime.isDebug();
  private Class<?> firstClassAdded;

  public GwtcContext(GwtcService gwtcService, ClassLoader resourceLoader) {
    this.gwtcService = gwtcService;
    // Start scanning the classpath, but don't block until we need to.
    final Callable<ClasspathResourceMap> scanner = X_Scanner.scanClassloaderAsync(resourceLoader);
    classpath = new SingletonProvider<ClasspathResourceMap>() {
      @Override
      protected ClasspathResourceMap initialValue() {
        try {
          return scanner.call();
        } catch (Exception e) {
          throw X_Debug.rethrow(e);
        }
      }
    };
    String genName = "Gwtc" + Math.abs(hashCode() - System.nanoTime());
    module = new GwtcXmlBuilder("", genName, false);
    module.addConfigurationProperty("xsiframe.failIfScriptTag", "FALSE");
    module.setPublic("public");
  }

  public static boolean isObjectOrNull(Class<?> cls) {
    return cls == Object.class || cls == null;
  }

  public boolean addClass(Class<?> cls) {
    if (finishedClasses.add(cls)) {
      if (finishedClasses.size() == 1) {
        firstClassAdded = cls;
      }
      scanClass(cls);
      return true;
    } else {
      X_Log.warn(getClass(), "Skipping class we've already seen", cls);
    }
    return false;
  }

  public boolean addMethod(Method method) {
    if (finishedMethods.add(method)) {
      scanMethod(method);
      return true;
    } else {
      X_Log.warn(getClass(), "Skipping method we've already seen", method);
      return false;
    }
  }

  public boolean addPackage(Package pkg) {
    if (finishedPackages.add(pkg)) {
      scanPackage(pkg);
      return true;
    } else {
      X_Log.trace(getClass(), "Skipping package we've already seen", pkg);
      return false;
    }
  }

  private void scanClass(Class<?> clazz) {
    GwtcUnit data = nodes.get(clazz);
    if (data == null) {
      Object ancestor = findAncestor(clazz);
      return;
    } if (data.gwtc == null) {
      return;
    }
    gwtcService.addClass(clazz);
    Object parent;
    if (data.isFindParent()) {
      parent = findAncestor(data.source);
      if (parent != null) {
        GwtcUnit parentNode = nodes.get(parent);
        data.setParent(parentNode);
        if (data.isFindAllParents()) {
          while (parent != null) {
            Object ancestor = findAncestor(parent);
            if (ancestor == null) {
              break;
            } else {
              GwtcUnit ancestorNode = nodes.get(ancestor);
              parentNode.setParent(ancestorNode);
              parent = ancestor;
              parentNode = ancestorNode;
            }
          }
        }
      }
    }

    Gwtc gwtc = clazz.getAnnotation(Gwtc.class);
    Class<?> c;
    parent = c = clazz;
    if (gwtc == null) {
      while (c != Object.class) {
        gwtc = c.getAnnotation(Gwtc.class);
        if (gwtc != null) {
          parent = c;
          maybeAddAncestors(gwtc, c);
          break;
        }
        c = c.getSuperclass();
      }
      Package pkg;
      parent = pkg = clazz.getPackage();
      if (gwtc == null) {
        gwtc = pkg.getAnnotation(Gwtc.class);
        String parentName = pkg.getName();
        search : while (gwtc == null) {
          int ind = parentName.lastIndexOf('.');
          if (ind == -1) {
            break;
          }
          parentName = parentName.substring(0, ind);
          pkg = GwtReflect.getPackage(parentName);
          while (pkg == null) {
            ind = parentName.lastIndexOf('.');
            if (ind == -1) {
              X_Log.warn("No package found for ", clazz.getPackage(), "; aborting @Gwtc search");
              break search;
            }
            parentName = parentName.substring(0, ind);
            pkg = GwtReflect.getPackage(parentName);
          }
          gwtc = pkg.getAnnotation(Gwtc.class);
        }
        if (gwtc != null) {
          parent = pkg;
          maybeAddAncestors(gwtc, pkg);
        }
      } else {
        maybeAddAncestors(gwtc, pkg);
      }
    } else {
      maybeAddAncestors(gwtc, c);
      inherit(data);
      for (Class<?> inherited : gwtc.inheritClasses()) {
        if (!nodes.containsKey(inherited)) {
          scanClass(inherited);
        }
      }
    }
    if (parent != null) {
      X_Log.trace(getClass(), "Next annotated parent of ",c,"is",parent);

    }
  }

  protected void inherit(GwtcUnit data) {
    if (data.isFindAllParents()) {

    } else if (data.isFindParent()) {
      Object o = data.getParent();
    }
  }

  private void scanMethod(Method method) {
    X_Log.trace(getClass(), "TODO: scan method ", method);
  }

  private void scanPackage(Package pkg) {
    GwtcUnit data = nodes.get(pkg);
    if (data == null) {
      Object ancestor = findAncestor(pkg);
      if (ancestor != null) {
        addPackage((Package)ancestor);
      }
      return;
    } else if (data.gwtc == null) {
      return;
    }
    Gwtc gwtc = pkg.getAnnotation(Gwtc.class);
    addGwtcPackage(gwtc, pkg, false);
    X_Log.trace(getClass(), "Parent of ",pkg,"is",data.getParent());

    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    for (ClassFile file : classpath.get().findClassesInPackage(pkg.getName())) {
      X_Log.trace(getClass(), "Checking class file ", file.getName());
      try {
        if (file.getEnclosedName().equals("package-info")) {
          X_Log.info(getClass(), "Loading package", file);
          Package p = GwtReflectJre.getPackage(file.getPackage(), cl);
          if (!finishedPackages.contains(p)) {
            gwtcService.addPackage(p, false);
          }
        } else {
          Class<?> clazz = cl.loadClass(file.getName());
          X_Log.trace(getClass(), "Loaded class", clazz);
          if (!finishedClasses.contains(clazz)) {
            X_Log.info(getClass(), "Adding class", clazz);
            gwtcService.addClass(clazz);
          }
        }
      } catch (Exception e) {
        X_Log.warn(getClass(), "Error encountered trying to load class ", file.getName(), e);
      }
    }
  }

  public void saveTo(File file, String renameTo) {
    boolean absoluteFile = renameTo.endsWith(".gwt.xml");
    if (absoluteFile) {
      renameTo = renameTo.substring(0, renameTo.length() - 8);
    }
    boolean rename = !renameTo.replace('/', '.').equals(module.getInheritName());

    final String inheritName;
    if (rename) {
      final String pkg, name;
      int ind = renameTo.lastIndexOf('.');
      boolean moveToPackage = ind > -1;
      if (moveToPackage) {
        pkg = renameTo.substring(0, ind).replace('.', '/');
        name = renameTo.substring(ind + 1);
      } else {
        pkg = "";
        name = renameTo;
      }
      if (pkg.length() == 0) {
        inheritName = name;
      } else {
        if (file.isDirectory()) {
          File f = new File(file, pkg);
          f.mkdirs();
          f.deleteOnExit();
          file = f;
        }
        inheritName = pkg + (pkg.length() == 0 ? "" : ".") + name;
      }
    } else {
      inheritName = renameTo;
    }

  }

  public void saveTo(File file) {
    saveTo(file, module.getInheritName());
  }

  public void inheritGwtXml(String inherit) {
    module.inherit(inherit);
  }

  public String getGenName() {
    return module.getInheritName();
  }

  public void setEntryPoint(String qualifiedName) {
    module.setEntryPoint(qualifiedName);
  }

  public void addGwtXmlSource(String genPrefix) {
    module.addSource(genPrefix);
  }

  public void addGwtXmlInherit(String value) {
    module.addInherit(value);
  }

  public XmlBuffer getGwtXml(GwtManifest manifest) {
    if (manifest != null) {
      manifest.getModules().forEach(module->module.addInherit(module.getInheritName()));
    }
    return module.getBuffer();
  }

  public void setRenameTo(String renameTo) {
    module.setRenameTo(renameTo);
  }

  public void addEnclosingClasses(Class<?> c) {
    c = c.getDeclaringClass();
    while (c != null) {
      Gwtc gwtc = c.getAnnotation(Gwtc.class);
      if (gwtc != null && addClass(c)) {
        addGwtcClass(gwtc, c);
      }
      c = c.getDeclaringClass();
    }
  }

  public void addSuperclasses(Class<?> c) {
    c = c.getSuperclass();
    while (c != null) {
      Gwtc gwtc = c.getAnnotation(Gwtc.class);
      if (gwtc != null && addClass(c)) {
        addGwtcClass(gwtc, c);
      }
      c = c.getSuperclass();
    }
  }

  protected void addGwtcSettings(Gwtc gwtc, AnnotatedElement clazz) {
    for (Dependency dep : gwtc.dependencies()) {
      addDependency(dep, clazz);
    }
    for (GwtcProperties prop : gwtc.propertiesLaunch()) {
      addLaunchProperty(prop);
    }
  }

  public boolean addLaunchProperty(GwtcProperties prop) {
    return launchProperties.add(prop);
  }

  public void addDependency(Dependency dep, AnnotatedElement clazz) {
    dependencies.add(new Dep(dep, clazz));
  }

  public Iterable<GwtcProperties> getLaunchProperties() {
    return launchProperties;
  }

  public MappedIterable<Dep> getDependencies() {
    return dependencies::iterator;
  }

  protected void addGwtcClass(Gwtc gwtc, Class<?> clazz) {
    // Generate a new gwt.xml file and inherit it.
    GwtcUnit node = nodes.get(clazz);
    String inheritName = node.generateGwtXml(gwtc, clazz.getPackage().getName(), clazz.getSimpleName());
    inheritGwtXml(inheritName);
    addGwtcSettings(gwtc, clazz);
  }

  protected void addGwtcPackage(Gwtc gwtc, Package pkg, boolean recursive) {
    String name = pkg.getName();
    int i = name.lastIndexOf('.');
    name = Character.toUpperCase(name.charAt(i + 1)) + name.substring(i + 2) + "_Package";
    GwtcUnit node = nodes.get(pkg);
    String inherit = node.generateGwtXml(gwtc, pkg.getName(), name);
    inheritGwtXml(inherit);
    addGwtcSettings(gwtc, pkg);
    maybeAddAncestors(gwtc, pkg);
    if (recursive) {
      needChildren.add(pkg);
    }
  }

  protected void addAllPackages(Package pkg) {
    Gwtc gwtc = pkg.getAnnotation(Gwtc.class);
    if (gwtc != null && addPackage(pkg)) {
      addGwtcPackage(gwtc, pkg, false);
    }
    String parentName = pkg.getName();
    int ind = parentName.lastIndexOf('.');
    while (ind > -1) {
      parentName = parentName.substring(0, ind);
      ind = parentName.lastIndexOf('.');
      pkg = GwtReflect.getPackage(parentName);

      X_Log.debug(getClass(), "Checking parent package", "'"+parentName+"'", pkg != null);

      if (pkg != null) {
        gwtc = pkg.getAnnotation(Gwtc.class);
        if (gwtc != null && addPackage(pkg)) {
          addGwtcPackage(gwtc, pkg, false);
        }
      }
    }
    pkg = GwtReflect.getPackage("");
    if (pkg != null) {
      gwtc = pkg.getAnnotation(Gwtc.class);
      if (gwtc != null && addPackage(pkg)) {
        addGwtcPackage(gwtc, pkg, false);
      }
    }
  }

  protected void maybeAddAncestors(Gwtc gwtc, Package pkg) {
    for (AncestorMode mode : gwtc.inheritanceMode()) {
      switch (mode) {
        case INHERIT_ONE_PARENT:
          if (addPackage(pkg)) {
            addGwtcPackage(gwtc, pkg, false);
          }
          break;
        case INHERIT_ALL_PARENTS:
          addAllPackages(pkg);
          break;
        case INHERIT_CHILDREN:
          needChildren.add(pkg);
        default:
          X_Log.trace("Unsupported ancestor mode", mode, "for package", pkg, "from", "\n" + gwtc);
      }
    }
  }

  protected void maybeAddAncestors(Gwtc gwtc, Class<?> c) {
    addGwtcClass(gwtc, c);
    for (AncestorMode mode : gwtc.inheritanceMode()) {
      switch (mode) {
        case INHERIT_ONE_PARENT:
          Package pkg = c.getPackage();
          gwtc = pkg.getAnnotation(Gwtc.class);
          if (gwtc != null && addPackage(pkg)) {
            addGwtcPackage(gwtc, pkg, false);
          }
          break;
        case INHERIT_ALL_PARENTS:
          addAllPackages(c.getPackage());
          break;
        case INHERIT_ENCLOSING_CLASSES:
          addEnclosingClasses(c);
          break;
        case INHERIT_SUPER_CLASSES:
          addSuperclasses(c);
          break;
        default:
          X_Log.warn("Unsupported mode type", mode, "for class", c);
      }
    }
  }

  private Object findAncestor(Object o) {
    if (o instanceof Method) {
      Method m = (Method)o;
      if (m.getDeclaringClass().isAnnotationPresent(Gwtc.class)) {
        return m.getDeclaringClass();
      }
      o = m.getDeclaringClass();
    }
    if (o instanceof Class) {
      return findAncestor((Class<?>)o);
    } else if (o instanceof Package) {
      return findAncestor((Package)o);
    } else {
      X_Log.error(getClass(), "Unsupported object type found while searching for ancestors", o, o.getClass());
      throw new IllegalArgumentException("Object "+o+" is not a method, class or package.");
    }
  }
  private Object findAncestor(Class<?> clazz) {
    // First check enclosing classes
    Class<?> c = clazz.getDeclaringClass();
    while (c != null) {
      if (c.isAnnotationPresent(Gwtc.class)) {
        return c;
      }
      c = c.getDeclaringClass();
    }
    Package p = clazz.getPackage();
    if (p.getAnnotation(Gwtc.class) != null) {
      return p;
    }
    Object o = findAncestor(p);
    if (o == null) {
      c = clazz.getSuperclass();
      while (c != null) {
        if (c.isAnnotationPresent(Gwtc.class)) {
          return c;
        }
        c = c.getSuperclass();
      }
    }
    return o;
  }

  private Object findAncestor(Package p) {
    String name = p.getName();
    int ind = name.lastIndexOf('.');
    while (ind > -1) {
      name = name.substring(0, ind);
      p = GwtReflect.getPackage(name);
      if (p != null && p.isAnnotationPresent(Gwtc.class)) {
        return p;
      }
      ind = name.lastIndexOf('.');
    }
    p = GwtReflect.getPackage("");
    return p == null || !p.isAnnotationPresent(Gwtc.class) ? null : p;
  }

  public void generateAll(final File dir, String moduleName, XmlBuffer head, XmlBuffer body) {
    X_Log.info(getClass(), "Generating all resources into ",dir);
    final HashSet<GwtcUnit> topLevel = new HashSet<GwtcUnit>();
    nodes.forKeys(new ReceivesValue<String>() {
      @Override
      public void set(String value) {
        GwtcUnit node = nodes.getValue(value);
        if (node.xml != null) {
          X_Log.info(getClass(), "Generating gwt.xml for ",node.source,"to",dir);
          save(node, dir);
        }
        if (node.parent == null) {
          topLevel.add(node);
        }
      }
    });
    // When generating ui templates, we simply descend from all top-level @Gwtc
    for (GwtcUnit root : topLevel) {
      if (root.gwtc != null) {
        UiTemplateGenerator gen = new UiTemplateGenerator(root);
        gen.generate(head, body);
      }
    }
  }

  private void save(GwtcUnit node, File outputFile) {
    final GwtcXmlBuilder xml = node.xml;
    outputFile = new File(outputFile, xml.getFileName());
    if (debug) {
      X_Log.info(getClass(), "Saving generated gwt.xml file",outputFile,"\n",xml.getBuffer());
    } else {
      X_Log.debug(getClass(), "Saving generated gwt.xml file",outputFile,"\n"+xml.getBuffer());
    }
    try {
      if (outputFile.exists()) {
        outputFile.delete();
      }
      outputFile.getParentFile().mkdirs();
      outputFile.createNewFile();
    } catch (IOException e) {
      X_Log.warn(getClass(),"Unable to create generated gwt.xml file", outputFile, e);
    }
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      String value = GwtcXmlBuilder.HEADER +xml.getBuffer();
      X_IO.drain(fos, new ByteArrayInputStream(value.getBytes(GwtcXmlBuilder.UTF8)));
    } catch (IOException e) {
      X_Log.warn(getClass(), "Unable to save generated gwt.xml file to ",outputFile,e,"\n"+xml.getBuffer());
    }

  }

  public void addPackages(Package pkg, GwtcServiceImpl gwtc, boolean recursive) {
    Iterable<ClassFile> iter;
    if (recursive) {
      iter = classpath.get().findClassesBelowPackage(pkg.getName());
    } else {
      iter = classpath.get().findClassesInPackage(pkg.getName());
    }
    for (ClassFile file : iter) {
      X_Log.info(getClass(), "Scanning file ",file);
      if ("package-info".equals(file.getEnclosedName())) {
        Package p = GwtReflect.getPackage(file.getPackage());
        if (!finishedPackages.contains(p)) {
          gwtcService.addPackage(p, false);
        }
      } else {
        try {
          Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(file.getName());
          if (!finishedClasses.contains(cls)) {
            gwtc.addClass(cls);
          }
        } catch (ClassNotFoundException e) {
          X_Log.warn(getClass(),"Unable to load class ",file);
        }
      }
    }
  }

  public Class<?> getFirstClassAdded() {
    return firstClassAdded;
  }
}
