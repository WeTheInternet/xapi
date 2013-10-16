
package java.lang.reflect;

import java.lang.annotation.Annotation;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.reflect.client.MemberMap;


/**
 * A <code>Field</code> provides information about, and dynamic access to, a
 * single field of a class or an interface.  The reflected field may
 * be a class (static) field or an instance field.
 *
 * <p>A <code>Field</code> permits widening conversions to occur during a get or
 * set access operation, but throws an <code>IllegalArgumentException</code> if a
 * narrowing conversion would occur.
 *
 * @see Member
 * @see java.lang.Class
 * @see java.lang.Class#getFields()
 * @see java.lang.Class#getField(String)
 * @see java.lang.Class#getDeclaredFields()
 * @see java.lang.Class#getDeclaredField(String)
 *
 * @author Kenneth Russell
 * @author Nakul Saraiya
 */
public class Field extends AccessibleObject implements Member {

    private Class   clazz;
    private String    name;
    private Class   type;
    private int     modifiers;
    private transient String    signature;

    private JavaScriptObject accessor;

    // Generics infrastructure

    private String getGenericSignature() {return signature;}

    public Field(Class returnType, Class declaringClass,
      String name, int modifiers, JavaScriptObject accessor) {
      this.clazz = declaringClass;
      this.name = name;
      this.type = returnType;
      this.modifiers = modifiers;
      this.signature = "";
      this.accessor = accessor;
    }


    /**
     * Returns the <code>Class</code> object representing the class or interface
     * that declares the field represented by this <code>Field</code> object.
     */
    public Class<?> getDeclaringClass() {
  return clazz;
    }

    /**
     * Returns the name of the field represented by this <code>Field</code> object.
     */
    public String getName() {
  return name;
    }

    /**
     * Returns the Java language modifiers for the field represented
     * by this <code>Field</code> object, as an integer. The <code>Modifier</code> class should
     * be used to decode the modifiers.
     *
     * @see Modifier
     */
    public int getModifiers() {
  return modifiers;
    }

    /**
     * Returns <tt>true</tt> if this field represents an element of
     * an enumerated type; returns <tt>false</tt> otherwise.
     *
     * @return <tt>true</tt> if and only if this field represents an element of
     * an enumerated type.
     * @since 1.5
     */
    public boolean isEnumConstant() {
        return (getModifiers() & Modifier.ENUM) != 0;
    }

    /**
     * Returns <tt>true</tt> if this field is a synthetic
     * field; returns <tt>false</tt> otherwise.
     *
     * @return true if and only if this field is a synthetic
     * field as defined by the Java Language Specification.
     * @since 1.5
     */
    public boolean isSynthetic() {
        return Modifier.isSynthetic(getModifiers());
    }

    /**
     * Returns a <code>Class</code> object that identifies the
     * declared type for the field represented by this
     * <code>Field</code> object.
     *
     * @return a <code>Class</code> object identifying the declared
     * type of the field represented by this object
     */
    public Class<?> getType() {
  return type;
    }

    /**
     * Returns a <tt>Type</tt> object that represents the declared type for
     * the field represented by this <tt>Field</tt> object.
     *
     * <p>If the <tt>Type</tt> is a parameterized type, the
     * <tt>Type</tt> object returned must accurately reflect the
     * actual type parameters used in the source code.
     *
     * <p>If the type of the underlying field is a type variable or a
     * parameterized type, it is created. Otherwise, it is resolved.
     *
     * @return a <tt>Type</tt> object that represents the declared type for
     *     the field represented by this <tt>Field</tt> object
     * @throws GenericSignatureFormatError if the generic field
     *     signature does not conform to the format specified in the Java
     *     Virtual Machine Specification, 3rd edition
     * @throws TypeNotPresentException if the generic type
     *     signature of the underlying field refers to a non-existent
     *     type declaration
     * @throws MalformedParameterizedTypeException if the generic
     *     signature of the underlying field refers to a parameterized type
     *     that cannot be instantiated for any reason
     * @since 1.5
     */
    public Type getGenericType() {
//  if (getGenericSignature() != null)
//      return getGenericInfo().getGenericType();
//  else
      return getType();
    }


    /**
     * Compares this <code>Field</code> against the specified object.  Returns
     * true if the objects are the same.  Two <code>Field</code> objects are the same if
     * they were declared by the same class and have the same name
     * and type.
     */
    @Override
    public boolean equals(Object obj) {
  if (obj != null && obj instanceof Field) {
      Field other = (Field)obj;
      return (getDeclaringClass() == other.getDeclaringClass())
                && (getName() == other.getName())
                && (getType() == other.getType());
  }
  return false;
    }

    /**
     * Returns a hashcode for this <code>Field</code>.  This is computed as the
     * exclusive-or of the hashcodes for the underlying field's
     * declaring class name and its name.
     */
    @Override
    public int hashCode() {
  return getDeclaringClass().getName().hashCode() ^ getName().hashCode();
    }

    /**
     * Returns a string describing this <code>Field</code>.  The format is
     * the access modifiers for the field, if any, followed
     * by the field type, followed by a space, followed by
     * the fully-qualified name of the class declaring the field,
     * followed by a period, followed by the name of the field.
     * For example:
     * <pre>
     *    public static final int java.lang.Thread.MIN_PRIORITY
     *    private int java.io.FileDescriptor.fd
     * </pre>
     *
     * <p>The modifiers are placed in canonical order as specified by
     * "The Java Language Specification".  This is <tt>public</tt>,
     * <tt>protected</tt> or <tt>private</tt> first, and then other
     * modifiers in the following order: <tt>static</tt>, <tt>final</tt>,
     * <tt>transient</tt>, <tt>volatile</tt>.
     */
    @Override
    public String toString() {
  int mod = getModifiers();
  return (((mod == 0) ? "" : (Modifier.toString(mod) + " "))
      + getTypeName(getType()) + " "
      + getTypeName(getDeclaringClass()) + "."
      + getName());
    }

    /**
     * Returns a string describing this <code>Field</code>, including
     * its generic type.  The format is the access modifiers for the
     * field, if any, followed by the generic field type, followed by
     * a space, followed by the fully-qualified name of the class
     * declaring the field, followed by a period, followed by the name
     * of the field.
     *
     * <p>The modifiers are placed in canonical order as specified by
     * "The Java Language Specification".  This is <tt>public</tt>,
     * <tt>protected</tt> or <tt>private</tt> first, and then other
     * modifiers in the following order: <tt>static</tt>, <tt>final</tt>,
     * <tt>transient</tt>, <tt>volatile</tt>.
     *
     * @return a string describing this <code>Field</code>, including
     * its generic type
     *
     * @since 1.5
     */
    public String toGenericString() {
  int mod = getModifiers();
  Type fieldType = getGenericType();
  return (((mod == 0) ? "" : (Modifier.toString(mod) + " "))
      +  ((fieldType instanceof Class) ?
    getTypeName((Class)fieldType): fieldType.toString())+ " "
      + getTypeName(getDeclaringClass()) + "."
      + getName());
    }

    protected native Object nativeGet(Object obj)
    /*-{
      return this.@java.lang.reflect.Field::accessor.getter(obj);
     }-*/;
    
    protected native void nativeSet(Object obj, Object value)
    /*-{
      this.@java.lang.reflect.Field::accessor.setter(obj, value);
     }-*/;
    
    protected void throwIllegalArg(Class received) throws IllegalArgumentException {
      throw new IllegalArgumentException("Cannot access field "+this+" as "+received.getName()+", "
          + "as the underlying field is of type "+getType().getName());
    }
    protected void throwIllegalAccess(Object received) throws IllegalAccessException  {
      throw new IllegalAccessException("Cannot set final "+this+" to "+received);
    }
    protected void throwNullNotAllowed() throws IllegalArgumentException {
      throw new IllegalArgumentException("Cannot set null values to "+this+".");
    }

    protected void maybeThrowNullGet(Object o) throws IllegalArgumentException {
      maybeThrowNullGet(o, nullNotAllowed());
    }

    protected void maybeThrowNullGet(Object o, boolean noNull) throws IllegalArgumentException {
      if (noNull && o == null)
        throw new IllegalArgumentException("Cannot get null values from "+this+".");
    }

    protected void maybeThrowNull(Object obj) {
      if (!Modifier.isStatic(getModifiers()) && obj == null)
        throw new NullPointerException();
    }

    protected void maybeThrowNull(Object obj, Object value) throws IllegalArgumentException{
      maybeThrowNull(obj);
      if (value == null) {
        if (nullNotAllowed()) {
          throw new IllegalArgumentException("Cannot set null to field "+this);
        }
      } else {
        maybeThrowNotAssignable(value);
      }
    }
    
    protected void maybeThrowNotAssignable(Object value) throws IllegalArgumentException {
      if (isNotAssignable(value.getClass())) {
        throw new IllegalArgumentException("Cannot assign object "+value+" of type "+value.getClass()
            +" to field "+this);
      }
    }

    protected void maybeThrowFinal(Object value) throws IllegalAccessException {
      if (Modifier.isFinal(getModifiers())) {
        throw new IllegalAccessException("Cannot assign object "+value+" of type "+value.getClass()
            +" to final field "+this);
      }
    }
    
    protected boolean isNotAssignable(Class<?> c) {
      // TODO remove the need for this .isPrimitive() using subclasses of Field
      return !getType().isPrimitive() && !getType().isAssignableFrom(c);
    }
    
    protected boolean nullNotAllowed() {
      return false;
    }

    public final Object get(Object obj)
        throws IllegalArgumentException, IllegalAccessException {
      maybeThrowNull(obj);
      return nativeGet(obj);
    }

    public boolean getBoolean(Object obj)
  throws IllegalArgumentException, IllegalAccessException
  {
    throwIllegalArg(boolean.class);
    return false;
  }

    public byte getByte(Object obj)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(byte.class);
      return 0;
    }

    public char getChar(Object obj)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(char.class);
      return 0;
    }

    public short getShort(Object obj)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(short.class);
      return 0;
    }

    public int getInt(Object obj)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(int.class);
      return 0;
    }

    public long getLong(Object obj)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(long.class);
      return 0;
    }

    public float getFloat(Object obj)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(float.class);
      return 0;
    }

    public double getDouble(Object obj)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(double.class);
      return 0;
    }

    public final void set(Object obj, Object value)
  throws IllegalArgumentException, IllegalAccessException {
      maybeThrowNull(obj, value);
      maybeThrowFinal(value);
      nativeSet(obj, value);
    }
    
    public void setBoolean(Object obj, boolean z)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(boolean.class);
    }

    public void setByte(Object obj, byte b)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(byte.class);
    }

    public void setChar(Object obj, char c)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(char.class);
    }

    public void setShort(Object obj, short s)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(short.class);
    }

    public void setInt(Object obj, int i)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(int.class);
    }

    public void setLong(Object obj, long l)
  throws IllegalArgumentException, IllegalAccessException {
        throwIllegalArg(long.class);
    }

    public void setFloat(Object obj, float f)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(float.class);
    }

    public void setDouble(Object obj, double d)
  throws IllegalArgumentException, IllegalAccessException {
      throwIllegalArg(double.class);
    }

    /*
     * Utility routine to paper over array type names
     */
    static String getTypeName(Class type) {
  if (type.isArray()) {
      try {
    Class cl = type;
    int dimensions = 0;
    while (cl.isArray()) {
        dimensions++;
        cl = cl.getComponentType();
    }
    StringBuffer sb = new StringBuffer();
    sb.append(cl.getName());
    for (int i = 0; i < dimensions; i++) {
        sb.append("[]");
    }
    return sb.toString();
      } catch (Throwable e) { /*FALLTHRU*/ }
  }
  return type.getName();
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null)
            throw new NullPointerException();
        return MemberMap.getAnnotation(accessor, annotationClass);
    }

    /**
     * @since 1.5
     */
    @Override
    public Annotation[] getDeclaredAnnotations()  {
      return MemberMap.getAnnotations(accessor, new Annotation[0]);
    }

}
