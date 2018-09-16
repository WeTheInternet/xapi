package xapi.dev.gwtc.api;

import xapi.annotation.common.Property;
import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.Resource;
import xapi.bytecode.ClassFile;
import xapi.collect.api.InitMap;
import xapi.collect.impl.InitMapDefault;
import xapi.dev.scanner.X_Scanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.dev.source.XmlBuffer;
import xapi.fu.In1Out1;
import xapi.fu.MappedIterable;
import xapi.fu.data.SetLike;
import xapi.fu.iterate.SizedIterable;
import xapi.fu.java.X_Jdk;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.Gwtc.AncestorMode;
import xapi.gwtc.api.GwtcProperties;
import xapi.gwtc.api.GwtcXmlBuilder;
import xapi.inject.impl.SingletonProvider;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.util.X_Debug;
import xapi.util.X_Runtime;
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

/**
 * This is a giant bag of state dedicated to "things we're adding to a GwtCompilation".
 *
 * When this bag of state is effectively empty, we _should_ be able to skip creating any files,
 * and just supply the correct values to make the compiler run entirely on existing code.
 */
@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public class GwtcGeneratedProject {

  private static final In1Out1 LIST_PROVIDER = ignored->new ArrayList();
  private static final In1Out1<Package, String> PACKAGE_NAME = Package::getName;
  private static final In1Out1<Object, String> ENTITY_NAME = from -> {
      if (from instanceof Class) {
        return ((Class<?>) from).getName();
      } else if (from instanceof Method) {
        return ((Method) from).toGenericString();
      } else if (from instanceof Package) {
        return ((Package) from).getName();
      } else {
        X_Log.warn(GwtcGeneratedProject.class, "Unsupported toString object type",
            from == null ? "null" : from.getClass(), from);
        return String.valueOf(from);
      }
  };

  // =====================================================
  // ==Store the structure of related classpath entities==
  // =====================================================

  private final InitMap<Class<?>, List<Method>> methods =
      new InitMapDefault<>(InitMapDefault.CLASS_NAME, LIST_PROVIDER);
  private final InitMap<Class<?>, List<Class<?>>> innerClasses =
      new InitMapDefault<>(InitMapDefault.CLASS_NAME, LIST_PROVIDER);
  private final InitMap<Class<?>, List<Class<?>>> superClasses =
      new InitMapDefault<>(InitMapDefault.CLASS_NAME, LIST_PROVIDER);
  private final InitMap<Package, List<Class<?>>> classes =
      new InitMapDefault<>(PACKAGE_NAME, LIST_PROVIDER);
  private final InitMap<Package, List<Package>> packages =
      new InitMapDefault<>(PACKAGE_NAME, LIST_PROVIDER);

  private final InitMap<Object, GwtcUnit> nodes = new InitMapDefault<Object, GwtcUnit>(ENTITY_NAME, new GwtcReflectionConverter());

  // =====================================================
  // ==Store sets of all entities for run-once semantics==
  // =====================================================

  private final Set<Method> finishedMethods = new HashSet<Method>();
  private final Set<Class<?>> finishedClasses = new HashSet<Class<?>>();
  private final Set<Package> finishedPackages = new HashSet<Package>();

  // =====================================================
  // =====================================================
  // =====================================================

  private final GwtcXmlBuilder module;
  private final Set<AnnotatedDependency> dependencies = new LinkedHashSet<>();
  private final SetLike<Property> configProps = X_Jdk.setLinked();
  private final SetLike<Property> props = X_Jdk.setLinked();
  private final SetLike<Property> systemProps = X_Jdk.setLinked();
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

  public GwtcGeneratedProject(GwtcService gwtcService, ClassLoader resourceLoader, String moduleName) {
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
    final String[] fullName = X_Source.splitClassName(moduleName);
    module = new GwtcXmlBuilder(fullName[0], fullName[1], false);
    module.addConfigurationProperty("xsiframe.failIfScriptTag", "FALSE");
    module.setPublic("public");
  }

  public static boolean isObjectOrNull(Class<?> cls) {
    return cls == Object.class || cls == null;
  }

  public boolean addClass(Class<?> cls) {
    return addClass(cls, true);
  }
  protected boolean addClass(Class<?> cls, boolean warnDup) {
    if (finishedClasses.add(cls)) {
      if (finishedClasses.size() == 1) {
        firstClassAdded = cls;
      }
      scanClass(cls);
      return true;
    } else if (warnDup) {
      X_Log.warn(GwtcGeneratedProject.class, "Skipping class we've already seen", cls);
    }
    return false;
  }

  public boolean addMethod(Method method) {
    if (finishedMethods.add(method)) {
      scanMethod(method);
      return true;
    } else {
      X_Log.warn(GwtcGeneratedProject.class, "Skipping method we've already seen", method);
      return false;
    }
  }

  public boolean addPackage(Package pkg) {
    if (finishedPackages.add(pkg)) {
      scanPackage(pkg);
      return true;
    } else {
      X_Log.trace(GwtcGeneratedProject.class, "Skipping package we've already seen", pkg);
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
    addClass(clazz, false);
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
      X_Log.trace(GwtcGeneratedProject.class, "Next annotated parent of ",c,"is",parent);

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
            addPackage(p);
          }
        } else {
          Class<?> clazz = cl.loadClass(file.getName());
          X_Log.trace(getClass(), "Loaded class", clazz);
          if (!finishedClasses.contains(clazz)) {
            X_Log.info(getClass(), "Adding class", clazz);
            addClass(clazz);
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
    module.addInherit(inherit);
  }

  public MappedIterable<String> getInheritedGwtXml() {
    return module.getInherits();
  }

  public String getGenName() {
    return module.getInheritName();
  }

  public void setEntryPoint(String qualifiedName) {
    module.setEntryPoint(qualifiedName);
  }

  public String getEntryPoint() {
    return module.getEntryPoint();
  }

  public void addGwtXmlSource(String path) {
    module.addSource(path);
  }

  public void addGwtXmlInherit(String value) {
    module.addInherit(value);
  }

  public XmlBuffer getGwtXml(GwtManifest manifest) {
    if (manifest != null) {
      manifest.getModules().forEach(mod-> {
        manifest.getEntryPoints().forEach(module::addEntryPoint);
        mod.addInherit(module.getInheritName());
      });
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
    for (Property configProp : gwtc.propertiesGwtConfiguration()) {
      addConfigProperty(configProp);
    }

  }

  public boolean addConfigProperty(Property configProp) {
    return configProps.add(configProp);
  }

  public boolean addProperty(Property configProp) {
    return props.add(configProp);
  }

  public boolean addSystemProperty(Property configProp) {
    return systemProps.add(configProp);
  }

  public boolean addLaunchProperty(GwtcProperties prop) {
    return launchProperties.add(prop);
  }

  public void addDependency(Dependency dep, AnnotatedElement clazz) {
    dependencies.add(new AnnotatedDependency(dep, clazz));
  }

  public Iterable<GwtcProperties> getLaunchProperties() {
    return launchProperties;
  }

  public MappedIterable<AnnotatedDependency> getDependencies() {
    return dependencies::iterator;
  }

  public SizedIterable<Property> getConfigProps() {
    return configProps;
  }

  public SizedIterable<Property> getProps() {
    return props;
  }

  public SizedIterable<Property> getSystemProps() {
    return systemProps;
  }

  public void addGwtcClass(Gwtc gwtc, Class<?> clazz) {
    // Generate a new gwt.xml file and inherit it.
    GwtcUnit node = nodes.get(clazz);
    String inheritName = node.generateGwtXml(clazz.getPackage().getName(), clazz.getSimpleName() + "Module", gwtc);
    inheritGwtXml(inheritName);
    addGwtcSettings(gwtc, clazz);
  }

  public void addGwtcPackage(Gwtc gwtc, Package pkg, boolean recursive) {
    String name = pkg.getName();
    int i = name.lastIndexOf('.');
    name = Character.toUpperCase(name.charAt(i + 1)) + name.substring(i + 2) + "_Package";
    GwtcUnit node = nodes.get(pkg);
    String inherit = node.generateGwtXml(pkg.getName(), name, gwtc);
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
        if (!node.hasParent()) {
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
      X_Log.info(GwtcGeneratedProject.class, "Saving generated gwt.xml file",outputFile,"\n",xml.getBuffer());
    } else {
      X_Log.debug(GwtcGeneratedProject.class, "Saving generated gwt.xml file",outputFile,"\n"+xml.getBuffer());
    }
    try {
      if (outputFile.exists()) {
        outputFile.delete();
      }
      outputFile.getParentFile().mkdirs();
      outputFile.createNewFile();
    } catch (IOException e) {
      X_Log.warn(GwtcGeneratedProject.class,"Unable to create generated gwt.xml file", outputFile, e);
    }
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      String value = GwtcXmlBuilder.HEADER +xml.getBuffer();
      X_IO.drain(fos, new ByteArrayInputStream(value.getBytes(GwtcXmlBuilder.UTF8)));
    } catch (IOException e) {
      X_Log.warn(GwtcGeneratedProject.class, "Unable to save generated gwt.xml file to ",outputFile,e,"\n"+xml.getBuffer());
    }

  }

  public void addPackages(Package pkg, GwtcProjectGenerator gwtc, boolean recursive) {
    Iterable<ClassFile> iter;
    if (recursive) {
      iter = classpath.get().findClassesBelowPackage(pkg.getName());
    } else {
      iter = classpath.get().findClassesInPackage(pkg.getName());
    }
    for (ClassFile file : iter) {
      X_Log.info(GwtcGeneratedProject.class, "Scanning file ",file);
      if ("package-info".equals(file.getEnclosedName())) {
        Package p = GwtReflect.getPackage(file.getPackage());
        if (!finishedPackages.contains(p)) {
          addPackage(p);
        }
      } else {
        try {
          Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(file.getName());
          if (!finishedClasses.contains(cls)) {
            addClass(cls);
          }
        } catch (ClassNotFoundException e) {
          X_Log.warn(GwtcGeneratedProject.class,"Unable to load class ",file);
        }
      }
    }
  }

  public Class<?> getFirstClassAdded() {
    return firstClassAdded;
  }

  public void setEntryPointPackage(String entryPackage) {
    if (entryPackage == null) {
      return;
    }
    if (getEntryPoint() == null) {
      throw new IllegalStateException("Cannot set entry point package before setting entry point!");
    }
    if (getEntryPoint().startsWith(entryPackage)) {
      return;
    }
    setEntryPoint(X_Source.qualifiedName(entryPackage, getEntryPoint()));
  }
}
