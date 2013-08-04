/*
 *
 * Copyright (c) 2006, 2010, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package java.lang;

import java.io.InputStream;
import java.net.URL;

import javax.inject.Provider;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;

/**
 * This stripped down ClassLoader class is simply here to give us cross-platform
 * support for code that might need a valid classloader.
 * 
 * xapi-gwt-reflect does call into the one and only system classloader,
 * to define mappings of java-names to runtime classes,
 * in order to enable Class.forName() and ClassLoader.loadClass();
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class ClassLoader {

    // The parent class loader for delegation
    private ClassLoader parent;

    // A JSO with all known classes;
    private JavaScriptObject classes = JavaScriptObject.createObject();
    
    /**
     * Creates a new class loader using the specified parent class loader for
     * delegation.
     *
     * @param  parent
     *         The parent class loader
     */
    protected ClassLoader(ClassLoader parent) {
      this.parent = parent;
    }

    /**
     * Creates a new class loader using the <tt>ClassLoader</tt> returned by
     * the method {@link #getSystemClassLoader()
     * <tt>getSystemClassLoader()</tt>} as the parent class loader.
     */
    protected ClassLoader() {
        this(getSystemClassLoader());
    }

    // -- Class --

    /**
     * Loads the class with the specified <a href="#name">binary name</a>.
     * This method searches for classes in the same manner as the {@link
     * #loadClass(String, boolean)} method.  It is invoked by the Java virtual
     * machine to resolve class references.  Invoking this method is equivalent
     * to invoking {@link #loadClass(String, boolean) <tt>loadClass(name,
     * false)</tt>}.  </p>
     *
     * @param  name
     *         The <a href="#name">binary name</a> of the class
     *
     * @return  The resulting <tt>Class</tt> object
     *
     * @throws  ClassNotFoundException
     *          If the class was not found
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
  return loadClass(name, false);
    }

    /**
     * Loads the class with the specified <a href="#name">binary name</a>.  The
     * default implementation of this method searches for classes in the
     * following order:
     *
     * <p><ol>
     *
     *   <li><p> Invoke {@link #findLoadedClass(String)} to check if the class
     *   has already been loaded.  </p></li>
     *
     *   <li><p> Invoke the {@link #loadClass(String) <tt>loadClass</tt>} method
     *   on the parent class loader.  If the parent is <tt>null</tt> the class
     *   loader built-in to the virtual machine is used, instead.  </p></li>
     *
     *   <li><p> Invoke the {@link #findClass(String)} method to find the
     *   class.  </p></li>
     *
     * </ol>
     *
     * <p> If the class was found using the above steps, and the
     * <tt>resolve</tt> flag is true, this method will then invoke the {@link
     * #resolveClass(Class)} method on the resulting <tt>Class</tt> object.
     *
     * <p> Subclasses of <tt>ClassLoader</tt> are encouraged to override {@link
     * #findClass(String)}, rather than this method.  </p>
     *
     * @param  name
     *         The <a href="#name">binary name</a> of the class
     *
     * @param  resolve
     *         If <tt>true</tt> then resolve the class
     *
     * @return  The resulting <tt>Class</tt> object
     *
     * @throws  ClassNotFoundException
     *          If the class could not be found
     */
    protected synchronized Class<?> loadClass(String name, boolean resolve)
  throws ClassNotFoundException
    {
  // First, check if the class has already been loaded
  Class c = null;//TODO: reimplement this goodness findLoadedClass(name);
  if (c == null) {
      try {
    if (parent != null) {
        c = parent.loadClass(name, false);
    } else {
//        c = findBootstrapClassOrNull(name);
    }
      } catch (ClassNotFoundException e) {
                // ClassNotFoundException thrown if class not found
                // from the non-null parent class loader
            }
            if (c == null) {
          // If still not found, then invoke findClass in order
          // to find the class.
          c = findClass(name);
      }
  }
//  if (resolve) {
//      resolveClass(c);
//  }
  return c;
    }

    // This method is invoked by the virtual machine to load a class.
    private synchronized Class loadClassInternal(String name)
  throws ClassNotFoundException
    {
  return loadClass(name);
    }

    /**
     * Finds the class with the specified <a href="#name">binary name</a>.
     * This method should be overridden by class loader implementations that
     * follow the delegation model for loading classes, and will be invoked by
     * the {@link #loadClass <tt>loadClass</tt>} method after checking the
     * parent class loader for the requested class.  The default implementation
     * throws a <tt>ClassNotFoundException</tt>.  </p>
     *
     * @param  name
     *         The <a href="#name">binary name</a> of the class
     *
     * @return  The resulting <tt>Class</tt> object
     *
     * @throws  ClassNotFoundException
     *          If the class could not be found
     *
     * @since  1.2
     */
    protected Class<?> findClass(String name) throws ClassNotFoundException {
  throw new ClassNotFoundException(name);
    }


    /**
     * Our ClassLoader doesn't actually load anything;
     * it just holds a jso mapping from class name to class object.
     */
    public final native Class<?> defineClass(String name, Class<?> cls)
    /*-{
      this.@java.lang.ClassLoader::classes[name] = cls;
      return cls;
    }-*/;

    /**
     * Finds a class with the specified <a href="#name">binary name</a>,
     * loading it if necessary.
     *
     * <p> This method loads the class through the system class loader (see
     * {@link #getSystemClassLoader()}).  The <tt>Class</tt> object returned
     * might have more than one <tt>ClassLoader</tt> associated with it.
     * Subclasses of <tt>ClassLoader</tt> need not usually invoke this method,
     * because most class loaders need to override just {@link
     * #findClass(String)}.  </p>
     *
     * @param  name
     *         The <a href="#name">binary name</a> of the class
     *
     * @return  The <tt>Class</tt> object for the specified <tt>name</tt>
     *
     * @throws  ClassNotFoundException
     *          If the class could not be found
     *
     * @see  #ClassLoader(ClassLoader)
     * @see  #getParent()
     */
    protected final Class<?> findSystemClass(String name)
  throws ClassNotFoundException
    {
      return getSystemClassLoader().loadClass(name);
    }

    /**
     * No-op for compatibility
     */
    protected final void setSigners(Class<?> c, Object[] signers) {
      //do nothing
    }

    // -- Resource --
    public URL getResource(String name) {
      // TODO return a magic url backed by an IO request
      return null;
    }


    /**
     * Unsupported
     */
    public InputStream getResourceAsStream(String name) {
      throw new UnsupportedOperationException();
//  URL url = getResource(name);
//  try {
//      return url != null ? url.openStream() : null;
//  } catch (IOException e) {
//      return null;
//  }
    }

    /**
     * Unsupported
     */
    public static InputStream getSystemResourceAsStream(String name) {
      throw new UnsupportedOperationException();
//        URL url = getSystemResource(name);
//        try {
//            return url != null ? url.openStream() : null;
//        } catch (IOException e) {
//            return null;
//        }
    }

    /**
     * Returns the parent class loader for delegation. Some implementations may
     * use <tt>null</tt> to represent the bootstrap class loader. This method
     * will return <tt>null</tt> in such implementations if this class loader's
     * parent is the bootstrap class loader.
     *
     */
    public final ClassLoader getParent() {
      return parent;
    }

    private static Provider<ClassLoader> systemClassloader = new Provider<ClassLoader>() {
      ClassLoader cl;
      public ClassLoader get() {
        return cl == null ? ((cl = new ClassLoader(null))) : cl;
      }
    };
    
    /**
     * A
     * @return
     */
    public static ClassLoader getSystemClassLoader() {
      // Just one classloader for everyone
      return systemClassloader.get();
    }


    // Returns true if the specified class loader can be found in this class
    // loader's delegation chain.
    boolean isAncestor(ClassLoader cl) {
  ClassLoader acl = this;
  do {
      acl = acl.parent;
      if (cl == acl) {
    return true;
      }
  } while (acl != null);
  return false;
    }

    // Returns the invoker's class loader, or null if none.
    // NOTE: This must always be invoked when there is exactly one intervening
    // frame from the core libraries on the stack between this method's
    // invocation and the desired invoker.
    static ClassLoader getCallerClassLoader() {
      return getSystemClassLoader();
    }

    protected Package getPackage(String name) {
      return Package.getPackage(name); // We just create package objects on request
    }
    protected Package[] getPackages() {
      // This used to transverse ClassLoader hierarchy,
      // but for simplicitywith sourcemaps somehow...
//    // NOTE use of more generic Reflection.getCallerClass()
//    Class caller = Reflection.getCallerClass(3);
//    // This can be null if the VM is requesting it
//    if (caller == null) {
//        return null;
//    }
//    // Circumvent security check since this is package-private
//    return caller.get sake, we only implement a single classloader for the app
      return Package.getPackages();
    }

    // Invoked in the java.lang.Runtime class to implement load and loadLibrary.
    static void loadLibrary(Class fromClass, String name,
          boolean isAbsolute) {
    }
}
