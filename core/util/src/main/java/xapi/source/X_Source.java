package xapi.source;

import xapi.inject.X_Inject;
import xapi.source.api.HasQualifiedName;
import xapi.source.api.IsType;
import xapi.source.service.SourceService;
import xapi.util.X_String;

import javax.inject.Provider;
import javax.validation.constraints.NotNull;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;

import static xapi.util.X_String.isEmpty;
import static xapi.util.X_String.toTitleCase;

public class X_Source {

  private X_Source() {}

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
    return cls.getCanonicalName().replace(cls.getPackage().getName()+".", "");
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
  public static String toPackageFromBinary(String cls) {
    int lastPeriod = cls.lastIndexOf('.');
    if (lastPeriod == -1) {
      return "";
    }
    return cls.substring(0, lastPeriod);
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

    public static boolean isJavaLangObject(HasQualifiedName type) {
    return type.getQualifiedName().equals("java.lang.Object");
  }
  public static String qualifiedName(String pkg, String enclosed) {
    return isEmpty(pkg) ? enclosed : isEmpty(enclosed) ? pkg : enclosed.startsWith(pkg+".") ? enclosed : pkg + "." + enclosed;
  }
  public static String[] splitClassName(String providerName) {
    int was, is = was = providerName.lastIndexOf('.');
    if (was == -1) {
      return new String[]{"", providerName};
    }
    while (is != -1) {
      if (Character.isLowerCase(providerName.charAt(is+1))) {
        // the dot is before a lower-case value.  Use the next position as match
        return new String[]{
            providerName.substring(0, was),
            providerName.substring(was+1)
        };
      } else {
        was = is;
        is = providerName.lastIndexOf('.', was-1);
      }
    }
    if (Character.isLowerCase(providerName.charAt(0))) {
      return new String[]{
          providerName.substring(0, was),
          providerName.substring(was+1)
      };
    }
    return new String[]{"", providerName};
  }

  public static String normalizeNewlines(String template) {
    // to remain GWT-compatible, we will use string regex methods.
    // a jvm-only implementation could be made using precompiled Patterns,
    // but it's likely not worth the nano seconds it will save.
    return template.replaceAll("\r\n?", "\n");
  }

  public static String javaQuote(String unescaped) {
    if (unescaped.endsWith(";")) {
      unescaped = unescaped.substring(0, unescaped.length()-1);
    }
    if (unescaped.startsWith("\"")) {
      unescaped = unescaped.substring(1);
    }
    if (unescaped.endsWith("\"")) {
      unescaped = unescaped.substring(0, unescaped.length()-1);
    }
    return "\"" + escape(unescaped) + "\"";
  }
  public static String escape(final String unescaped) {
    int extra = 0;
    for (int in = 0, n = unescaped.length(); in < n; ++in) {
      switch (unescaped.charAt(in)) {
        case '\r':
          String normalized = normalizeNewlines(unescaped);
          if (!normalized.equals(unescaped)) {
            return escape(normalized);
          }
        case '\0':
        case '\n':
        case '\"':
        case '\\':
          ++extra;
          break;
      }
    }
    if (extra == 0) {
      return unescaped;
    }
    final char[] oldChars = unescaped.toCharArray();
    final char[] newChars = new char[oldChars.length + extra];
    for (int in = 0, out = 0, n = oldChars.length; in < n; ++in, ++out) {
      char c = oldChars[in];
      switch (c) {
        case '\r':
          newChars[out++] = '\\';
          c = 'r';
          break;
        case '\0':
          newChars[out++] = '\\';
          c = '0';
          break;
        case '\n':
          newChars[out++] = '\\';
          c = 'n';
          break;
        case '\"':
          newChars[out++] = '\\';
          c = '"';
          break;
        case '\\':
          newChars[out++] = '\\';
          c = '\\';
          break;
      }
      newChars[out] = c;
    }
    return String.valueOf(newChars);
  }

  public static String toCamelCase(String name) {
    if (name == null || name.isEmpty()) {
      return "";
    }

    while (name.startsWith("-")) {
      name = name.substring(1);
    }

    String val = toTitleCase(name);
    int nextDash = val.indexOf('-');
    if (nextDash == -1) {
      return val;
    }
    StringBuilder result = new StringBuilder();
    int prev = 0;
    while (nextDash != -1) {

      result.append(toTitleCase(val.substring(prev, nextDash)));
      prev = nextDash+1; // skip writing the -
      nextDash = val.indexOf('-', prev);
    }
    if (prev < val.length()) {
      result.append(toTitleCase(val.substring(prev)));
    }
    return result.toString();
  }

    public static String removePackage(String pkgName, @NotNull String typeName) {
        assert typeName != null : "Do not send null typenames to X_Source.removePackage";
        return pkgName == null ? typeName :
              typeName.replace(pkgName + ".", "");
    }

    public static String enclosedNameFlattened(String pkg, String fullyQualified) {
        String enclosed = removePackage(pkg, fullyQualified);
        return enclosed.replace('.', '_');
    }

    public static String javaSafeName(String path) {
      if(isEmpty(path)) {
        return "";
      }
      final char[] chars = path.toCharArray();
      boolean safe = true;
      if (!Character.isJavaIdentifierStart(chars[0])) {
        chars[0] = '_';
        safe = false;
      }
      for (int i = chars.length; i-->1;) {
        if (!Character.isJavaIdentifierPart(chars[i])) {
          chars[i] = '_';
          safe = false;
        }
      }
      if (safe) {
        return path;
      }
      return new String(chars);
    }

  public static String pathToLogLink(String pkg, String file) {
      return pathToLogLink(pkg, file, null);
  }
  public static String pathToLogLink(String pkg, String file, Integer line) {
      if (line == null) {
          line = 27;
      }
      return X_Source.qualifiedName(pkg, "(" + (file.contains(".") ? file : file + ".java") + ":" + line + ")");
  }

  public static String pathToLogLink(String path) {
    return pathToLogLink(path, (Integer)null);
  }
  public static String pathToLogLink(String path, Integer line) {
    String unixed = path.replace('\\', '/');
    String lastBit = unixed.substring(unixed.lastIndexOf('/') + 1);
    String prefix = unixed.substring(0, unixed.length() - lastBit.length()
        // We don't advocate for using the default "" package, but we do guard for it
        - (lastBit.length() == unixed.length() ? 0 : 1));
    // If we have the path of the document, we can render a link that intellij will pick up.
    String linkToDoc = prefix.replace('/', '.') + // make it look like a java qualified name
        "(" + lastBit + ":" + (line == null ? 27 : line) + ")"; // print the file name and the line number to jump to source
    // TODO: bother with other IDEs :-)
    return linkToDoc;
  }

    public static String raw(String type) {
      return type == null ? null :
          type.split("<")[0].trim();
    }
}
