package xapi.bytecode;

import java.io.InputStream;
import java.net.URL;

public interface ClassPath {
    /**
     * Opens a class file.
     * This method may be called just to examine whether the class file
     * exists as well as to read the contents of the file.
     *
     * <p>This method can return null if the specified class file is not
     * found.  If null is returned, the next search path is examined.
     * However, if an error happens, this method must throw an exception
     * so that the search will be terminated.
     *
     * <p>This method should not modify the contents of the class file.
     *
     * @param classname         a fully-qualified class name
     * @return          the input stream for reading a class file
     * @see javassist.Translator
     */
    InputStream openClassfile(String classname) throws NotFoundException;

    /**
     * Returns the uniform resource locator (URL) of the class file
     * with the specified name.
     *
     * @param classname         a fully-qualified class name.
     * @return null if the specified class file could not be found.
     */
    URL find(String classname);

    /**
     * This method is invoked when the <code>ClassPath</code> object is
     * detached from the search path.  It will be an empty method in most of
     * classes.
     */
    void close();
}
