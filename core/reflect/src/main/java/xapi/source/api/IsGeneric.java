package xapi.source.api;


public interface IsGeneric extends IsType{

  /**
   * Default bounds of opaque means "just print the name in the <>".
   */
  int OPAQUE = 0;
  /**
   * Bounds of <genericName extends Type1 & Type2 & Type3>
   */
  int EXTENDS = 1;
  /**
   * Bounds of <genericName super Type1>
   * It is illegal to specify more than one generic type
   */
  int SUPER = 2;
  /**
   * A flag for generics that can be erased if we are targetting java 7.
   */
  int NILLABLE = 4;

  /**
   * @return The arbitrary name of this generic; defaults to ?
   *
   * If it is a fully qualified classname, it will be imported;
   * if the writer has access to a TypePool, it will attempt to shorten the
   * generic signature automatically.
   */
  String genericName();

  /**
   * @return The bounds strategy to use.
   * 0 = print: name == "?" ? genericTypes[0] : name
   * 1 = print: name extends join(genericTypes, "&")
   * 2 = print: name super genericTypes[0]
   * 4 = can print <> in java7 compilations
   */
  int genericBounds();

  /**
   * @return any extra types that should be added to an extends bound.
   *
   * This will be null or length == 0 unless you want <X extends Y & Z & ...> signatures.
   * The Y type is represented by this object's IsType declaration.
   * Z ... types will be returned by extended types.
   *
   * Note that using other generic names for types is possibly valid,
   * so we support it blindly and leave it up to the developer to ensure that
   * their generate signatures are valid.
   *
   * These types will identify as isPrimitive() as they will lack a package.
   */
  IsType[] extendedTypes();

}
