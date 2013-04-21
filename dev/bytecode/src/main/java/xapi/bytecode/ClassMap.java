package xapi.bytecode;

public class ClassMap extends java.util.HashMap<Object, Object> {
	private static final long serialVersionUID = 7429196218655312033L;
	private ClassMap parent;

    /**
     * Constructs a hash table.
     */
    public ClassMap() { parent = null; }

    ClassMap(ClassMap map) { parent = map; }

    /**
     * Maps a class name to another name in this hashtable.
     * The names are obtained with calling <code>Class.getName()</code>.
     * This method translates the given class names into the
     * internal form used in the JVM before putting it in
     * the hashtable.
     *
     * @param oldname   the original class name
     * @param newname   the substituted class name.
     */
    public void put(CtClass oldname, CtClass newname) {
        put(oldname.getName(), newname.getName());
    }

    /**
     * Maps a class name to another name in this hashtable.
     * If the hashtable contains another mapping from the same
     * class name, the old mapping is replaced.
     * This method translates the given class names into the
     * internal form used in the JVM before putting it in
     * the hashtable.
     *
     * <p>If <code>oldname</code> is identical to
     * <code>newname</code>, then this method does not
     * perform anything; it does not record the mapping from
     * <code>oldname</code> to <code>newname</code>.  See
     * <code>fix</code> method.
     *
     * @param oldname   the original class name.
     * @param newname   the substituted class name.
     * @see #fix(String)
     */
	public void put(String oldname, String newname) {
        if (oldname == newname)
            return;

        String oldname2 = toJvmName(oldname);
        String s = (String)get(oldname2);
        if (s == null || !s.equals(oldname2))
            super.put(oldname2, toJvmName(newname));
    }

    /**
     * Is equivalent to <code>put()</code> except that
     * the given mapping is not recorded into the hashtable
     * if another mapping from <code>oldname</code> is
     * already included.
     *
     * @param oldname       the original class name.
     * @param newname       the substituted class name.
     */
    public void putIfNone(String oldname, String newname) {
        if (oldname == newname)
            return;

        String oldname2 = toJvmName(oldname);
        String s = (String)get(oldname2);
        if (s == null)
            super.put(oldname2, toJvmName(newname));
    }

	protected final void put0(Object oldname, Object newname) {
        super.put(oldname, newname);
    }

    /**
     * Returns the class name to wihch the given <code>jvmClassName</code>
     * is mapped.  A subclass of this class should override this method.
     *
     * <p>This method receives and returns the internal representation of
     * class name used in the JVM.
     *
     * @see #toJvmName(String)
     * @see #toJavaName(String)
     */
    @Override
    public Object get(Object jvmClassName) {
        Object found = super.get(jvmClassName);
        if (found == null && parent != null)
            return parent.get(jvmClassName);
        else
            return found;
    }

    /**
     * Prevents a mapping from the specified class name to another name.
     */
    public void fix(CtClass clazz) {
        fix(clazz.getName());
    }

    /**
     * Prevents a mapping from the specified class name to another name.
     */
	public void fix(String name) {
        String name2 = toJvmName(name);
        super.put(name2, name2);
    }

    /**
     * Converts a class name into the internal representation used in
     * the JVM.
     */
    public static String toJvmName(String classname) {
        return Descriptor.toJvmName(classname);
    }

    /**
     * Converts a class name from the internal representation used in
     * the JVM to the normal one used in Java.
     */
    public static String toJavaName(String classname) {
        return Descriptor.toJavaName(classname);
    }
}