
package java.lang.reflect;

import java.lang.annotation.Annotation;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;

/**
 * <code>Constructor</code> provides information about, and access to, a single
 * constructor for a class.
 *
 * <p><code>Constructor</code> permits widening conversions to occur when matching the
 * actual parameters to newInstance() with the underlying
 * constructor's formal parameters, but throws an
 * <code>IllegalArgumentException</code> if a narrowing conversion would occur.
 *
 * @param <T> the class in which the constructor is declared
 *
 * @see Member
 * @see java.lang.Class
 * @see java.lang.Class#getConstructors()
 * @see java.lang.Class#getConstructor(Class[])
 * @see java.lang.Class#getDeclaredConstructors()
 *
 * @author  Kenneth Russell
 * @author  Nakul Saraiya
 */
public class Constructor<T> extends AccessibleObject implements
                                                    GenericDeclaration,
                                                    Member {
  private static final Class[] EMPTY_CLASSES = new Class[0];
  private static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
  
    private Class<T>    clazz;
    private int     slot;
    private Class[]   parameterTypes;
    private Class[]   exceptionTypes;
    private int     modifiers;
    private transient String    signature;

    private JavaScriptObject method;
    private Annotation[] annos;

    private static final int LANGUAGE_MODIFIERS =
      Modifier.PUBLIC   | Modifier.PROTECTED  | Modifier.PRIVATE;

    protected Constructor() {
      this.parameterTypes = exceptionTypes = EMPTY_CLASSES;
      this.annos = EMPTY_ANNOTATIONS;
    }
    /**
     * Public constructor to allow gwt to create constructors anywhere
     */
    public Constructor(Class<T> from, int modifiers, JavaScriptObject method, Annotation[] annos, 
        Class<?>[] params, Class<?>[] exceptions) {
      this.clazz = from;
      this.modifiers = modifiers;
      // TODO implement these
      this.signature = "";
      this.method = method;
      this.annos = annos;
      this.parameterTypes = params;
      this.exceptionTypes = exceptions;
    }

    /**
     * Pure JRE must copy constructors to manage accessibility.
     *
     * We, on the other hand, can stick with a single object and uncap access.
     */
    Constructor<T> copy() {
        return this;
    }

    /**
     * Returns the <code>Class</code> object representing the class that declares
     * the constructor represented by this <code>Constructor</code> object.
     */
    public Class<T> getDeclaringClass() {
  return clazz;
    }

    /**
     * Returns the name of this constructor, as a string.  This is
     * always the same as the simple name of the constructor's declaring
     * class.
     */
    public String getName() {
  return getDeclaringClass().getSimpleName();
    }

    /**
     * Returns the Java language modifiers for the constructor
     * represented by this <code>Constructor</code> object, as an integer. The
     * <code>Modifier</code> class should be used to decode the modifiers.
     *
     * @see Modifier
     */
    public int getModifiers() {
  return modifiers;
    }

    /**
     * Returns an array of <tt>TypeVariable</tt> objects that represent the
     * type variables declared by the generic declaration represented by this
     * <tt>GenericDeclaration</tt> object, in declaration order.  Returns an
     * array of length 0 if the underlying generic declaration declares no type
     * variables.
     *
     * @return an array of <tt>TypeVariable</tt> objects that represent
     *     the type variables declared by this generic declaration
     * @throws GenericSignatureFormatError if the generic
     *     signature of this generic declaration does not conform to
     *     the format specified in the Java Virtual Machine Specification,
     *     3rd edition
     * @since 1.5
     */
    public TypeVariable<Constructor<T>>[] getTypeParameters() {
//      if (getSignature() != null) {
//  return (TypeVariable<Constructor<T>>[])getGenericInfo().getTypeParameters();
//      } else
          return (TypeVariable<Constructor<T>>[])new TypeVariable[0];
    }


    /**
     * Returns an array of <code>Class</code> objects that represent the formal
     * parameter types, in declaration order, of the constructor
     * represented by this <code>Constructor</code> object.  Returns an array of
     * length 0 if the underlying constructor takes no parameters.
     *
     * @return the parameter types for the constructor this object
     * represents
     */
    public Class<?>[] getParameterTypes() {
  return (Class<?>[]) parameterTypes;//used to be a clone.
  //We should make this immutable...  or implement clone.
    }


    /**
     * Returns an array of <tt>Type</tt> objects that represent the formal
     * parameter types, in declaration order, of the method represented by
     * this <tt>Constructor</tt> object. Returns an array of length 0 if the
     * underlying method takes no parameters.
     *
     * <p>If a formal parameter type is a parameterized type,
     * the <tt>Type</tt> object returned for it must accurately reflect
     * the actual type parameters used in the source code.
     *
     * <p>If a formal parameter type is a type variable or a parameterized
     * type, it is created. Otherwise, it is resolved.
     *
     * @return an array of <tt>Type</tt>s that represent the formal
     *     parameter types of the underlying method, in declaration order
     * @throws GenericSignatureFormatError
     *     if the generic method signature does not conform to the format
     *     specified in the Java Virtual Machine Specification, 3rd edition
     * @throws TypeNotPresentException if any of the parameter
     *     types of the underlying method refers to a non-existent type
     *     declaration
     * @throws MalformedParameterizedTypeException if any of
     *     the underlying method's parameter types refer to a parameterized
     *     type that cannot be instantiated for any reason
     * @since 1.5
     */
    public Type[] getGenericParameterTypes() {
//  if (getSignature() != null)
//      return getGenericInfo().getParameterTypes();
//  else
      return getTypeParameters();
    }


    /**
     * Returns an array of <code>Class</code> objects that represent the types
     * of exceptions declared to be thrown by the underlying constructor
     * represented by this <code>Constructor</code> object.  Returns an array of
     * length 0 if the constructor declares no exceptions in its <code>throws</code> clause.
     *
     * @return the exception types declared as being thrown by the
     * constructor this object represents
     */
    public Class<?>[] getExceptionTypes() {
  return (Class<?>[])exceptionTypes;//.clone();
    }


    /**
     * Returns an array of <tt>Type</tt> objects that represent the
     * exceptions declared to be thrown by this <tt>Constructor</tt> object.
     * Returns an array of length 0 if the underlying method declares
     * no exceptions in its <tt>throws</tt> clause.
     *
     * <p>If an exception type is a parameterized type, the <tt>Type</tt>
     * object returned for it must accurately reflect the actual type
     * parameters used in the source code.
     *
     * <p>If an exception type is a type variable or a parameterized
     * type, it is created. Otherwise, it is resolved.
     *
     * @return an array of Types that represent the exception types
     *     thrown by the underlying method
     * @throws GenericSignatureFormatError
     *     if the generic method signature does not conform to the format
     *     specified in the Java Virtual Machine Specification, 3rd edition
     * @throws TypeNotPresentException if the underlying method's
     *     <tt>throws</tt> clause refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if
     *     the underlying method's <tt>throws</tt> clause refers to a
     *     parameterized type that cannot be instantiated for any reason
     * @since 1.5
     */
      public Type[] getGenericExceptionTypes() {
//    Type[] result;
//    if (getSignature() != null &&
//        ( (result = getGenericInfo().getExceptionTypes()).length > 0  ))
//        return result;
//    else
        return getExceptionTypes();
      }

    /**
     * Compares this <code>Constructor</code> against the specified object.
     * Returns true if the objects are the same.  Two <code>Constructor</code> objects are
     * the same if they were declared by the same class and have the
     * same formal parameter types.
     */
    @Override
    public boolean equals(Object obj) {
  if (obj != null && obj instanceof Constructor) {
      Constructor other = (Constructor)obj;
      if (getDeclaringClass() == other.getDeclaringClass()) {
    /* Avoid unnecessary cloning */
    Class[] params1 = parameterTypes;
    Class[] params2 = other.parameterTypes;
    if (params1.length == params2.length) {
        for (int i = 0; i < params1.length; i++) {
      if (params1[i] != params2[i])
          return false;
        }
        return true;
    }
      }
  }
  return false;
    }

    /**
     * Returns a hashcode for this <code>Constructor</code>. The hashcode is
     * the same as the hashcode for the underlying constructor's
     * declaring class name.
     */
    @Override
    public int hashCode() {
  return getDeclaringClass().getName().hashCode();
    }

    /**
     * Returns a string describing this <code>Constructor</code>.  The string is
     * formatted as the constructor access modifiers, if any,
     * followed by the fully-qualified name of the declaring class,
     * followed by a parenthesized, comma-separated list of the
     * constructor's formal parameter types.  For example:
     * <pre>
     *    public java.util.Hashtable(int,float)
     * </pre>
     *
     * <p>The only possible modifiers for constructors are the access
     * modifiers <tt>public</tt>, <tt>protected</tt> or
     * <tt>private</tt>.  Only one of these may appear, or none if the
     * constructor has default (package) access.
     */
    @Override
    public String toString() {
  try {
      StringBuffer sb = new StringBuffer();
      int mod = getModifiers() & LANGUAGE_MODIFIERS;
      if (mod != 0) {
    sb.append(Modifier.toString(mod) + " ");
      }
      sb.append(Field.getTypeName(getDeclaringClass()));
      sb.append("(");
      Class[] params = parameterTypes; // avoid clone
      for (int j = 0; j < params.length; j++) {
    sb.append(Field.getTypeName(params[j]));
    if (j < (params.length - 1))
        sb.append(",");
      }
      sb.append(")");
      Class[] exceptions = exceptionTypes; // avoid clone
      if (exceptions.length > 0) {
    sb.append(" throws ");
    for (int k = 0; k < exceptions.length; k++) {
        sb.append(exceptions[k].getName());
        if (k < (exceptions.length - 1))
      sb.append(",");
    }
      }
      return sb.toString();
  } catch (Exception e) {
      return "<" + e + ">";
  }
    }

    /**
     * Returns a string describing this <code>Constructor</code>,
     * including type parameters.  The string is formatted as the
     * constructor access modifiers, if any, followed by an
     * angle-bracketed comma separated list of the constructor's type
     * parameters, if any, followed by the fully-qualified name of the
     * declaring class, followed by a parenthesized, comma-separated
     * list of the constructor's generic formal parameter types.  A
     * space is used to separate access modifiers from one another and
     * from the type parameters or return type.  If there are no type
     * parameters, the type parameter list is elided; if the type
     * parameter list is present, a space separates the list from the
     * class name.  If the constructor is declared to throw
     * exceptions, the parameter list is followed by a space, followed
     * by the word &quot;<tt>throws</tt>&quot; followed by a
     * comma-separated list of the thrown exception types.
     *
     * <p>The only possible modifiers for constructors are the access
     * modifiers <tt>public</tt>, <tt>protected</tt> or
     * <tt>private</tt>.  Only one of these may appear, or none if the
     * constructor has default (package) access.
     *
     * @return a string describing this <code>Constructor</code>,
     * include type parameters
     *
     * @since 1.5
     */
    public String toGenericString() {
  try {
      StringBuilder sb = new StringBuilder();
      int mod = getModifiers() & LANGUAGE_MODIFIERS;
      if (mod != 0) {
    sb.append(Modifier.toString(mod) + " ");
      }
      Type[] typeparms = getTypeParameters();
      if (typeparms.length > 0) {
    boolean first = true;
    sb.append("<");
    for(Type typeparm: typeparms) {
        if (!first)
      sb.append(",");
        if (typeparm instanceof Class)
      sb.append(((Class)typeparm).getName());
        else
      sb.append(typeparm.toString());
        first = false;
    }
    sb.append("> ");
      }
      sb.append(Field.getTypeName(getDeclaringClass()));
      sb.append("(");
      Type[] params = getGenericParameterTypes();
      for (int j = 0; j < params.length; j++) {
    sb.append((params[j] instanceof Class)?
        Field.getTypeName((Class)params[j]):
        (params[j].toString()) );
    if (j < (params.length - 1))
        sb.append(",");
      }
      sb.append(")");
      Type[] exceptions = getGenericExceptionTypes();
      if (exceptions.length > 0) {
    sb.append(" throws ");
    for (int k = 0; k < exceptions.length; k++) {
        sb.append((exceptions[k] instanceof Class)?
            ((Class)exceptions[k]).getName():
            exceptions[k].toString());
        if (k < (exceptions.length - 1))
      sb.append(",");
    }
      }
      return sb.toString();
  } catch (Exception e) {
      return "<" + e + ">";
  }
    }

    /**
     * Uses the constructor represented by this <code>Constructor</code> object to
     * create and initialize a new instance of the constructor's
     * declaring class, with the specified initialization parameters.
     * Individual parameters are automatically unwrapped to match
     * primitive formal parameters, and both primitive and reference
     * parameters are subject to method invocation conversions as necessary.
     *
     * <p>If the number of formal parameters required by the underlying constructor
     * is 0, the supplied <code>initargs</code> array may be of length 0 or null.
     *
     * <p>If the constructor's declaring class is an inner class in a
     * non-static context, the first argument to the constructor needs
     * to be the enclosing instance; see <i>The Java Language
     * Specification</i>, section 15.9.3.
     *
     * <p>If the required access and argument checks succeed and the
     * instantiation will proceed, the constructor's declaring class
     * is initialized if it has not already been initialized.
     *
     * <p>If the constructor completes normally, returns the newly
     * created and initialized instance.
     *
     * @param initargs array of objects to be passed as arguments to
     * the constructor call; values of primitive types are wrapped in
     * a wrapper object of the appropriate type (e.g. a <tt>float</tt>
     * in a {@link java.lang.Float Float})
     *
     * @return a new object created by calling the constructor
     * this object represents
     *
     * @exception IllegalAccessException    if this <code>Constructor</code> object
     *              enforces Java language access control and the underlying
     *              constructor is inaccessible.
     * @exception IllegalArgumentException  if the number of actual
     *              and formal parameters differ; if an unwrapping
     *              conversion for primitive arguments fails; or if,
     *              after possible unwrapping, a parameter value
     *              cannot be converted to the corresponding formal
     *              parameter type by a method invocation conversion; if
     *              this constructor pertains to an enum type.
     * @exception InstantiationException    if the class that declares the
     *              underlying constructor represents an abstract class.
     * @exception InvocationTargetException if the underlying constructor
     *              throws an exception.
     * @exception ExceptionInInitializerError if the initialization provoked
     *              by this method fails.
     */
    @UnsafeNativeLong
    public T newInstance(Object ... initargs)
  throws InstantiationException, IllegalAccessException,
               IllegalArgumentException, InvocationTargetException
    {
      return create(method, initargs);
    }

    @UnsafeNativeLong
    private native T create(JavaScriptObject func, Object[] args)
    /*-{
      return func.apply(null, args);
     }-*/;

    /**
     * Returns <tt>true</tt> if this constructor was declared to take
     * a variable number of arguments; returns <tt>false</tt>
     * otherwise.
     *
     * @return <tt>true</tt> if an only if this constructor was declared to
     * take a variable number of arguments.
     * @since 1.5
     */
    public boolean isVarArgs() {
        return (getModifiers() & Modifier.VARARGS) != 0;
    }

    /**
     * Returns <tt>true</tt> if this constructor is a synthetic
     * constructor; returns <tt>false</tt> otherwise.
     *
     * @return true if and only if this constructor is a synthetic
     * constructor as defined by the Java Language Specification.
     * @since 1.5
     */
    public boolean isSynthetic() {
        return Modifier.isSynthetic(getModifiers());
    }

    int getSlot() {
        return slot;
    }

   String getSignature() {
      return signature;
   }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
        if (annotationClass == null) throw new NullPointerException();
        for (Annotation a : annos) {
          if (a.annotationType() == annotationClass)
            return (A)a;
        }
        return null;
    }

    /**
     * @since 1.5
     */
    @Override
    public Annotation[] getDeclaredAnnotations()  {
        return annos;
    }

    /**
     * Returns an array of arrays that represent the annotations on the formal
     * parameters, in declaration order, of the method represented by
     * this <tt>Constructor</tt> object. (Returns an array of length zero if the
     * underlying method is parameterless.  If the method has one or more
     * parameters, a nested array of length zero is returned for each parameter
     * with no annotations.) The annotation objects contained in the returned
     * arrays are serializable.  The caller of this method is free to modify
     * the returned arrays; it will have no effect on the arrays returned to
     * other callers.
     *
     * @return an array of arrays that represent the annotations on the formal
     *    parameters, in declaration order, of the method represented by this
     *    Constructor object
     * @since 1.5
     */
    public Annotation[][] getParameterAnnotations() {
        Annotation[][] result = new Annotation[0][0];
        return result;
    }
}