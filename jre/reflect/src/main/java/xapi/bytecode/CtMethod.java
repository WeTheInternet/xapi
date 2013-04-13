package xapi.bytecode;

import xapi.source.Modifier;

public final class CtMethod extends CtBehavior {
    protected String cachedStringRep;

    /**
     * @see #make(MethodInfo minfo, CtClass declaring)
     */
    CtMethod(MethodInfo minfo, CtClass declaring) {
        super(declaring, minfo);
        cachedStringRep = null;
    }

    /**
     * Creates a public abstract method.  The created method must be
     * added to a class with <code>CtClass.addMethod()</code>.
     *
     * @param declaring         the class to which the created method is added.
     * @param returnType        the type of the returned value
     * @param mname             the method name
     * @param parameters        a list of the parameter types
     *
     * @see CtClass#addMethod(CtMethod)
     */
    public CtMethod(CtClass returnType, String mname,
                    CtClass[] parameters, CtClass declaring) {
        this(null, declaring);
        ConstPool cp = declaring.getClassFile2().getConstPool();
        String desc = Descriptor.ofMethod(returnType, parameters);
        methodInfo = new MethodInfo(cp, mname, desc);
        setModifiers(Modifier.PUBLIC | Modifier.ABSTRACT);
    }

//    /**
//     * Creates a copy of a <code>CtMethod</code> object.
//     * The created method must be
//     * added to a class with <code>CtClass.addMethod()</code>.
//     *
//     * <p>All occurrences of class names in the created method
//     * are replaced with names specified by
//     * <code>map</code> if <code>map</code> is not <code>null</code>.
//     *
//     * <p>For example, suppose that a method <code>at()</code> is as
//     * follows:
//     *
//     * <ul><pre>public X at(int i) {
//     *     return (X)super.elementAt(i);
//     * }</pre></ul>
//     *
//     * <p>(<code>X</code> is a class name.)  If <code>map</code> substitutes
//     * <code>String</code> for <code>X</code>, then the created method is:
//     *
//     * <ul><pre>public String at(int i) {
//     *     return (String)super.elementAt(i);
//     * }</pre></ul>
//     *
//     * <p>By default, all the occurrences of the names of the class
//     * declaring <code>at()</code> and the superclass are replaced
//     * with the name of the class and the superclass that the
//     * created method is added to.
//     * This is done whichever <code>map</code> is null or not.
//     * To prevent this replacement, call <code>ClassMap.fix()</code>
//     * or <code>put()</code> to explicitly specify replacement.
//     *
//     * <p><b>Note:</b> if the <code>.class</code> notation (for example,
//     * <code>String.class</code>) is included in an expression, the
//     * Javac compiler may produce a helper method.
//     * Since this constructor never
//     * copies this helper method, the programmers have the responsiblity of
//     * copying it.  Otherwise, use <code>Class.forName()</code> in the
//     * expression.
//     *
//     * @param src       the source method.
//     * @param declaring    the class to which the created method is added.
//     * @param map       the hashtable associating original class names
//     *                  with substituted names.
//     *                  It can be <code>null</code>.
//     *
//     * @see CtClass#addMethod(CtMethod)
//     * @see ClassMap#fix(String)
//     */
//    public CtMethod(CtMethod src, CtClass declaring, ClassMap map)
//        throws CannotCompileException
//    {
//        this(null, declaring);
//        copy(src, false, map);
//    }
//
//    /**
//     * Compiles the given source code and creates a method.
//     * This method simply delegates to <code>make()</code> in
//     * <code>CtNewMethod</code>.  See it for more details.
//     * <code>CtNewMethod</code> has a number of useful factory methods.
//     *
//     * @param src               the source text.
//     * @param declaring    the class to which the created method is added.
//     * @see CtNewMethod#make(String, CtClass)
//     */
//    public static CtMethod make(String src, CtClass declaring)
//        throws CannotCompileException
//    {
//        return CtNewMethod.make(src, declaring);
//    }

    /**
     * Creates a method from a <code>MethodInfo</code> object.
     *
     * @param declaring     the class declaring the method.
     * @throws CannotCompileException       if the the <code>MethodInfo</code>
     *          object and the declaring class have different
     *          <code>ConstPool</code> objects
     * @since 3.6
     */
    public static CtMethod make(MethodInfo minfo, CtClass declaring)
        throws CannotCompileException
    {
        if (declaring.getClassFile2().getConstPool() != minfo.getConstPool())
            throw new CannotCompileException("bad declaring class");

        return new CtMethod(minfo, declaring);
    }

    /**
     * Returns a hash code value for the method.
     * If two methods have the same name and signature, then
     * the hash codes for the two methods are equal.
     */
    @Override
    public int hashCode() {
        return getStringRep().hashCode();
    }

    /**
     * This method is invoked when setName() or replaceClassName()
     * in CtClass is called.
     */
    @Override
    void nameReplaced() {
        cachedStringRep = null;
    }

    /* This method is also called by CtClassType.getMethods0().
     */
    final String getStringRep() {
        if (cachedStringRep == null)
            cachedStringRep = methodInfo.getName()
                + Descriptor.getParamDescriptor(methodInfo.getDescriptor());

        return cachedStringRep;
    }

    /**
     * Indicates whether <code>obj</code> has the same name and the
     * same signature as this method.
     */
    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof CtMethod
               && ((CtMethod)obj).getStringRep().equals(getStringRep());
    }

    /**
     * Returns the method name followed by parameter types
     * such as <code>javassist.CtMethod.setBody(String)</code>.
     *
     * @since 3.5
     */
    @Override
    public String getLongName() {
        return getDeclaringClass().getName() + "."
               + getName() + Descriptor.toString(getSignature());
    }

    /**
     * Obtains the name of this method.
     */
    @Override
    public String getName() {
        return methodInfo.getName();
    }

    /**
     * Changes the name of this method.
     */
    public void setName(String newname) {
        declaringClass.checkModify();
        methodInfo.setName(newname);
    }

    /**
     * Obtains the type of the returned value.
     */
    public CtClass getReturnType() throws NotFoundException {
        return getReturnType0();
    }

    /**
     * Returns true if the method body is empty, that is, <code>{}</code>.
     * It also returns true if the method is an abstract method.
     */
    @Override
    public boolean isEmpty() {
        return true;
    }

}
