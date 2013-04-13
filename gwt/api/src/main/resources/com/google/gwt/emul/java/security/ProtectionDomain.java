package java.security;

import java.security.CodeSource;

/** 
 * Very simple emulation of a class' ProtectionDomain;
 * only supported to get jre-compliant access to CodeSource
 */

public class ProtectionDomain {

    /* CodeSource */
    private CodeSource codesource ;

    /**
     */
    public ProtectionDomain(String codeSource) {
      codesource = new CodeSource(codeSource);
    }

    /**
     * Returns the CodeSource of this domain.
     * @return the CodeSource of this domain which may be null.
     * @since 1.2
     */
    public final CodeSource getCodeSource() {
  return this.codesource;
    }


    /**
     * Returns the ClassLoader of this domain.
     * @return the ClassLoader of this domain which may be null.
     *
     * @since 1.4
     */
    public final ClassLoader getClassLoader() {
      return Class.class.getClassLoader();
    }
}
