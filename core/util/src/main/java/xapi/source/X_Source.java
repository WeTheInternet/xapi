package xapi.source;



import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Provider;

import xapi.inject.X_Inject;
import xapi.source.api.HasQualifiedName;
import xapi.source.api.IsType;
import xapi.source.service.SourceService;
import xapi.util.X_String;
import xapi.util.api.Pair;
import xapi.util.impl.PairBuilder;

public class X_Source {

  private X_Source() {}
  
  private static final String arrays = "\\[\\]";

  private static final Provider<SourceService> service = X_Inject.singletonLazy(SourceService.class);

  public static IsType toType(Class<?> cls) {
    return service.get().toType(cls);
  }
  protected static IsType toType(String qualifiedName) {
    String pkg = toPackage(qualifiedName);
    return toType(pkg, 
        pkg.length() == 0 ? qualifiedName :
        qualifiedName.substring(pkg.length()+1));
  }
  
  public static IsType toType(String pkg, String enclosedName) {
    return service.get().toType(X_String.notNull(pkg).replace('/', '.'), enclosedName.replace('$', '.'));
  }

  /**
   * Send in com.pkg.Clazz$InnerClass
   * or com/pkg/Clazz$InnerClass
   * Get back Pair<"com.pkg", "Clazz.InnerClass"
   * @param qualifiedBinaryName - The cls.getCanonicalName, or cls.getQualifiedBinaryName
   * @return - A pair of source names ('.' delimited), [pac.kage, Enclosing.Name]
   */
  public static IsType binaryToSource(String qualifiedBinaryName) {
    int arrDepth = 0;
    while(qualifiedBinaryName.charAt(0) == '[') {
      arrDepth++;
      qualifiedBinaryName = qualifiedBinaryName.substring(1);
    }
    qualifiedBinaryName = qualifiedBinaryName.replace('/', '.');
    int lastPkg = qualifiedBinaryName.lastIndexOf('.');
    String pkg;
    if (lastPkg == -1) {
      pkg = "";
    } else {
      pkg = qualifiedBinaryName.substring(0, lastPkg);
      assert pkg.equals(pkg.toLowerCase()) :
        "Either you are using an uppercase letter in your package name (stop that!)\n" +
        "or you are sending an inner class using period encoding instead of $ (also stop that!)\n" +
        "You sent "+qualifiedBinaryName+"; expected com.package.OuterClass$InnerClass";
    }
    String enclosed = X_Modifier.toEnclosingType(qualifiedBinaryName.substring(lastPkg+1));
    return toType(pkg, X_Modifier.addArrayBrackets(enclosed, arrDepth));
  }

  public static String stripJarName(String loc) {
    int ind = loc.indexOf("jar!");
    if (ind == -1)
      return loc;
    return stripFileName(loc.substring(0, ind+3));
  }
  public static String stripFileName(String loc) {
    return loc.startsWith("file:") ?  loc.substring(5) : loc;
  }
  
  public static String stripClassExtension(String loc) {
    return loc.endsWith(".class") ?  loc.substring(0, loc.length()-6) : loc;
  }

  
  public static String primitiveToObject(String datatype) {
    if ("int".equals(datatype))
      return "Integer";
    if ("char".equals(datatype))
      return "Character";
    return Character.toUpperCase(datatype.charAt(0)) + datatype.substring(1);
  }

  public static String[] toStringCanonical(
      Class<?> ... classes) {
    // TODO move this to service, so gwt can use seedId or something more deterministic
    String[] names = new String[classes.length];
    for (int i = classes.length; i-->0;)
      names[i] = classes[i].getCanonicalName(); 
    return names;
  }
  
  public static String[] toStringBinary(
      Class<?> ... classes) {
    // TODO move this to service, so gwt can use seedId or something more deterministic
    String[] names = new String[classes.length];
    for (int i = classes.length; i-->0;)
      names[i] = classes[i].getName(); 
    return names;
  }
  public static String toStringEnclosed(Class<?> cls) {
    return cls.getCanonicalName().replace(cls.getPackage().getName()+".", "");
  }
  
  public static URL[] getUrls(ClassLoader classLoader) {
    ArrayList<URL> urls = new ArrayList<URL>();
    ClassLoader system = ClassLoader.getSystemClassLoader();
    while (classLoader != null && classLoader != system) {
      if (classLoader instanceof URLClassLoader) {
        Collections.addAll(urls, 
            ((URLClassLoader)classLoader).getURLs());
        classLoader = classLoader.getParent();
      }
    }
    return urls.toArray(new URL[urls.size()]);
  }
  public static URL classToUrl(String binaryName, ClassLoader loader) {
    return loader.getResource(binaryName.replace('.', '/')+".class");
  }
  public static String classToEnclosedSourceName(
      Class<?> cls) {
    return cls.getCanonicalName().substring(1+cls.getPackage().getName().length());
  }
  public static boolean typesEqual(IsType[] one, IsType[] two) {
    if (one == null)
      return two == null;
    if (one.length != two.length)
      return false;
    for (int i = 0, m = one.length; i < m; ++i) {
      if (!one[i].equals(two[i]))
        return false;
    }
    return true;
  }
  public static boolean typesEqual(IsType[] one, Class<?> ... two) {
    if (one == null)
      return two == null;
    if (one.length != two.length)
      return false;
    for (int i = 0, m = one.length; i < m; ++i) {
      if (!one[i].getQualifiedName().equals(two[i].getCanonicalName()))
        return false;
    }
    return true;
  }

  public static boolean typesEqual(Class<?>[] one, IsType ... two) {
    if (one == null)
      return two == null;
    if (one.length != two.length)
      return false;
    for (int i = 0, m = one.length; i < m; ++i) {
      if (!one[i].getCanonicalName().equals(two[i].getQualifiedName()))
        return false;
    }
    return true;
  }
  
  public static boolean typesEqual(Class<?>[] one, Class<?> ... two) {
    if (one == null)
      return two == null;
    if (one.length != two.length)
      return false;
    for (int i = 0, m = one.length; i < m; ++i) {
      if (one[i] != two[i])
        return false;
    }
    return true;
  }
  
  public static boolean typesAssignable(Class<?>[] subtypes, Class<?> ... supertypes) {
    if (subtypes == null)
      return supertypes == null;
    if (subtypes.length != supertypes.length)
      return false;
    for (int i = 0, m = subtypes.length; i < m; ++i) {
      if (!subtypes[i].isAssignableFrom(supertypes[i]))
        return false;
    }
    return true;
  }
  public static IsType[] toTypes(String[] from) {
    IsType[] types = new IsType[from.length];
    for (int i = 0, m = from.length; i < m; ++i) {
      String type = from[i];
      types[i] = toType(type);
    }
    return types;
  }
  public static String toPackage(String cls) {
    int lastPeriod = cls.lastIndexOf('.');
    while (lastPeriod != -1) {
      if (Character.isUpperCase(cls.charAt(lastPeriod+1)))
        lastPeriod = cls.lastIndexOf('.', lastPeriod-1);
      else break;
    }
    if (lastPeriod == -1)
      return "";
    else
      return cls.substring(0, lastPeriod);
  }
  public static Pair<String, Integer> extractArrayDepth(String from) {
    int arrayDepth = 0;
    while (from.matches(".*"+arrays)) {
      arrayDepth ++;
      from = from.replaceFirst(arrays, "");
    }
    return PairBuilder.pairOf(from, arrayDepth);
  }
  
  public static boolean isJavaLangObject(HasQualifiedName type) {
    return type.getQualifiedName().equals("java.lang.Object");
  }
  public static String qualifiedName(String pkg, String enclosed) {
    return X_String.isEmpty(pkg) ? enclosed : pkg + "." + enclosed;
  }

}
