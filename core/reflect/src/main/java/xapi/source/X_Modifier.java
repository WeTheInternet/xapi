package xapi.source;

public final class X_Modifier {

  private X_Modifier() {}

  public static final int ABSTRACT  = 0x0400;

  public static final int ANNOTATION = 0x2000;

  public static final int BRIDGE    = 0x0040;     // for method_info

  public static final int ENUM      = 0x4000;

  public static final int FINAL     = 0x0010;

  public static final int INTERFACE = 0x0200;

  public static final int NATIVE    = 0x0100;

  public static final int PRIVATE   = 0x0002;

  public static final int PROTECTED = 0x0004;

  public static final int PUBLIC    = 0x0001;

  public static final int STATIC    = 0x0008;

  public static final int STRICT    = 0x0800;

  public static final int SUPER     = 0x0020;

  public static final int SYNCHRONIZED = 0x0020;

  public static final int SYNTHETIC = 0x1000;

  // There is not a java.lang.reflect.Modifier field for default methods, so we use this unused value instead.
  public static final int DEFAULT = 0x10000;

  public static final int TRANSIENT = 0x0080;

  public static final int VARARGS   = 0x0080;     // for method_info

  public static final int VOLATILE  = 0x0040;

  public static final int PUBLIC_ABSTRACT  = PUBLIC | ABSTRACT;
  public static final int PUBLIC_FINAL  = PUBLIC | FINAL;
  public static final int PUBLIC_STATIC = PUBLIC | STATIC;
  public static final int PRIVATE_FINAL  = PRIVATE | FINAL;
  public static final int PRIVATE_STATIC = PRIVATE | STATIC;

  public static final int PUBLIC_STATIC_FINAL  = PUBLIC | FINAL | STATIC;
  public static final int PRIVATE_STATIC_FINAL  = PRIVATE | FINAL | STATIC;

  public static String addArrayBrackets(String addTo, int arrDepth) {
    if (arrDepth<1)return addTo;
    StringBuilder b = new StringBuilder(addTo);
    while(arrDepth-->0)b.append("[]");
    return b.toString();
  }

  public static String classModifiers(int protection) {
    return
      modifierToProtection(protection) +
      (
        isStatic(protection) ? "static "
          : isAbstract(protection) ? "abstract " : ""
        ) +
        (isFinal(protection) ? "final " : "")
        ;
  }

  /**
   * Clears a specified bit in <code>accflags</code>.
   */
  public static int clear(int accflags, int clearBit) {
      return accflags & ~clearBit;
  }

  public static boolean contains(int mod1, int mod2) {
    return (mod1 & mod2) != 0;
  }

  public static String fieldModifiers(int protection) {
    return
      modifierToProtection(protection) +
      (
        isStatic(protection) ? " static" : "") +
        (isFinal(protection) ? " final" :
          isVolatile(protection) ? " volatile" : "") +
        (isTransient(protection) ? " transient" : "")
        ;
  }

  /**
   * Returns true if the modifiers include the <tt>abstract</tt>
   * modifier.
   */
  public static boolean isAbstract(int mod) {
      return (mod & X_Modifier.ABSTRACT) != 0;
  }

  /**
   * Returns true if the modifiers include the <tt>annotation</tt>
   * modifier.
   *
   * @since 3.2
   */
  public static boolean isAnnotation(int mod) {
      return (mod & X_Modifier.ANNOTATION) != 0;
  }

  /**
   * Returns true if the modifiers include the <tt>enum</tt>
   * modifier.
   *
   * @since 3.2
   */
  public static boolean isEnum(int mod) {
      return (mod & X_Modifier.ENUM) != 0;
  }

  /**
   * Returns true if the modifiers include the <tt>final</tt>
   * modifier.
   */
  public static boolean isFinal(int mod) {
      return (mod & X_Modifier.FINAL) != 0;
  }
  /**
   * Returns true if the modifiers include the <tt>interface</tt>
   * modifier.
   */
  public static boolean isInterface(int mod) {
      return (mod & X_Modifier.INTERFACE) != 0;
  }
  /**
   * Returns true if the modifiers include the <tt>native</tt>
   * modifier.
   */
  public static boolean isNative(int mod) {
      return (mod & X_Modifier.NATIVE) != 0;
  }
  /**
   * Returns true if the access flags include neither public, protected,
   * or private.
   */
  public static boolean isPackage(int accflags) {
      return (accflags & (X_Modifier.PROTECTED | X_Modifier.PUBLIC | X_Modifier.PRIVATE)) == 0;
  }
  /**
   * Returns true if the access flags include the private bit.
   */
  public static boolean isPrivate(int accflags) {
      return (accflags & X_Modifier.PRIVATE) != 0;
  }
  /**
   * Returns true if the access flags include the protected bit.
   */
  public static boolean isProtected(int accflags) {
      return (accflags & X_Modifier.PROTECTED) != 0;
  }
  /**
   * Returns true if the access flags include the public bit.
   */
  public static boolean isPublic(int accflags) {
      return (accflags & X_Modifier.PUBLIC) != 0;
  }
  /**
   * Returns true if the modifiers include the <tt>static</tt>
   * modifier.
   */
  public static boolean isStatic(int mod) {
      return (mod & X_Modifier.STATIC) != 0;
  }
  public static boolean isDefaultVirtual(int mod) {
      return (mod & X_Modifier.DEFAULT) != 0;
  }
  /**
   * Returns true if the modifiers include the <tt>strictfp</tt>
   * modifier.
   */
  public static boolean isStrict(int mod) {
      return (mod & X_Modifier.STRICT) != 0;
  }
  /**
   * Returns true if the modifiers include the <tt>synchronized</tt>
   * modifier.
   */
  public static boolean isSynchronized(int mod) {
      return (mod & X_Modifier.SYNCHRONIZED) != 0;
  }
  /**
   * Returns true if the modifiers include the <tt>transient</tt>
   * modifier.
   */
  public static boolean isTransient(int mod) {
      return (mod & X_Modifier.TRANSIENT) != 0;
  }
  /**
   * Returns true if the modifiers include the <tt>volatile</tt>
   * modifier.
   */
  public static boolean isVolatile(int mod) {
      return (mod & X_Modifier.VOLATILE) != 0;
  }
  public static String methodModifiers(int protection) {
    return
      modifierToProtection(protection) +
      (
        isStatic(protection) ? " static"
        : isAbstract(protection) ? " abstract" : ""
      ) +
      (isFinal(protection) ? " final" : "") +
      (isDefaultVirtual(protection) ? " default" : "") +
      (isSynchronized(protection) ? " synchronized" : "") +
      (isNative(protection) ? " native" : "") +
      (isStrict(protection) ? " strictfp" : "")
      ;
  }
  public static String modifierToProtection(int protection) {
    return
      isPublic(protection) ? "public " :
      isProtected(protection) ? "protected " :
      isPrivate(protection) ? "private " :
      "";
  }
  /**
   * Clears the public, protected, and private bits.
   */
  public static int setPackage(int accflags) {
      return (accflags & ~(PROTECTED | PUBLIC | PRIVATE));
  }
  /**
   * Returns only the public, protected, and private bits.
   */
  public static int onlyPrivacy(int accflags) {
      return (accflags & (PROTECTED | PUBLIC | PRIVATE));
  }
  public static boolean isValidPrivacy(int mod) {
    mod = onlyPrivacy(mod);
    return mod == PUBLIC || mod == PROTECTED || mod == PRIVATE;
  }
  public static int validatePrivacy(int mod) {
    assert isValidPrivacy(mod) :
        "Invalid privacy " + mod + "(" + Integer.toString(mod, 2) + ") only one of the last three bits may be set";
    return mod;
  }
  /**
   * Truns the private bit on.  The protected and private bits are
   * cleared.
   */
  public static int setPrivate(int accflags) {
      return (accflags & ~(X_Modifier.PROTECTED | X_Modifier.PUBLIC)) | X_Modifier.PRIVATE;
  }
  /**
   * Truns the protected bit on.  The protected and public bits are
   * cleared.
   */
  public static int setProtected(int accflags) {
      return (accflags & ~(X_Modifier.PRIVATE | X_Modifier.PUBLIC)) | X_Modifier.PROTECTED;
  }
  /**
   * Truns the public bit on.  The protected and private bits are
   * cleared.
   */
  public static int setPublic(int accflags) {
      return (accflags & ~(X_Modifier.PRIVATE | X_Modifier.PROTECTED)) | X_Modifier.PUBLIC;
  }
  public static String toEnclosingType(String clsNoPackage) {
    return clsNoPackage.replace('$', '.');
  }

  public static String binaryNameToPackage(String clsName) {
    int ind = clsName.lastIndexOf('/');
    if (ind == -1) {
      return "";
    } else {
      return clsName.substring(0, ind).replace('/', '.');
    }
  }

  public static String sourceNameToPackage(String clsName) {
    int ind = clsName.lastIndexOf('.');
    if (ind == -1) {
      return "";
    } else {
      return clsName.substring(0, ind);
    }
  }

  public static String binaryNameToEnclosed(String name) {
    int ind = name.lastIndexOf('/');
    if (ind != -1) {
      name = name.substring(ind+1);
    }
    return name.replace('$', '.');
  }

  public static String sourceNameToEnclosed(String name) {
    int ind = name.lastIndexOf('.');
    if (ind != -1) {
      name = name.substring(ind+1);
    }
    return name.replace('$', '.');
  }

}
