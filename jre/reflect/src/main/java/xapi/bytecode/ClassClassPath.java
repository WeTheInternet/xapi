package xapi.bytecode;

import java.io.InputStream;
import java.net.URL;

public class ClassClassPath implements ClassPath {
    private Class<?> thisClass;

    /** Creates a search path.
     *
     * @param c     the <code>Class</code> object used to obtain a class
     *              file.  <code>getResourceAsStream()</code> is called on
     *              this object.
     */
    public ClassClassPath(Class<?> c) {
        thisClass = c;
    }

    ClassClassPath() {
        /* The value of thisClass was this.getClass() in early versions:
         *
         *     thisClass = this.getClass();
         *
         * However, this made openClassfile() not search all the system
         * class paths if javassist.jar is put in jre/lib/ext/
         * (with JDK1.4).
         */
        this(java.lang.Object.class);
    }

    /**
     * Obtains a class file by <code>getResourceAsStream()</code>.
     */
    public InputStream openClassfile(String classname) {
        String jarname = "/" + classname.replace('.', '/') + ".class";
        return thisClass.getResourceAsStream(jarname);
    }

    /**
     * Obtains the URL of the specified class file.
     *
     * @return null if the class file could not be found.
     */
    public URL find(String classname) {
        String jarname = "/" + classname.replace('.', '/') + ".class";
        return thisClass.getResource(jarname);
    }

    /**
     * Does nothing.
     */
    public void close() {
    }

    @Override
    public String toString() {
        return thisClass.getName() + ".class";
    }
}
