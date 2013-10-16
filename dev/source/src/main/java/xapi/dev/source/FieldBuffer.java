package xapi.dev.source;

import java.lang.reflect.Modifier;

import xapi.source.read.JavaLexer;
import xapi.source.read.JavaLexer.TypeDef;
import xapi.source.read.JavaVisitor.TypeData;
import xapi.source.write.Template;

/**
 * A field buffer is used to add a field to a generated class.
 *
 * The field definition itself is exported during .toString(),
 * but this buffer also exposes functionality to auto-generate
 * getter, setter, adder, remover and clear methods.
 *
 * The current implementation translates arrays in return types
 * into ArrayList that returns .toArray() copies of elements.
 *
 * This allows you to implement a varargs setter (which clears before add),
 * and varargs adders and removers.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class FieldBuffer extends MemberBuffer<FieldBuffer> {

  public static interface TypeDataGenerator {
    public String generate(TypeData type, String name);
  }

  protected static class TypeDataTemplateGenerator
  extends Template
  implements TypeDataGenerator
  {
    public TypeDataTemplateGenerator(String template) {
      super(template, "<>", "$");
    }

    @Override
    public String generate(TypeData type, String name) {
      return apply(type.getSimpleName(), name);
    }

  }

  private final ClassBuffer cls;
  private final TypeData fieldType;// The type of the field itself
  private final TypeDef methodType;// The type to expose on methods
  private final String fieldName;
  private final String simpleType;// Shortest form possible for use in source
  private final String methodFragment;// Camel-case form of name, for getField, setField methods.

  private PrintBuffer initializer;
  private MethodBuffer get;
  private MethodBuffer set;
  private MethodBuffer add;
  private MethodBuffer remove;
  private MethodBuffer clear;
  private boolean fluent = true;
  private boolean exact;

  public FieldBuffer(ClassBuffer enclosingClass, String type, String name) {
    this(enclosingClass, type, name, INDENT);
  }

  public FieldBuffer(ClassBuffer enclosingClass, String type, String name, String indent) {
    super(indent);
    this.cls = enclosingClass;
    this.fieldName = name;
    this.methodFragment = Character.toUpperCase(name.charAt(0)) + (name.length()==0?"":name.substring(1));
    this.indent = indent + INDENT;
    // The type to expose on methods; usually == fieldType, unless exposing []
    this.simpleType = cls.addImport(type);
    this.methodType = JavaLexer.extractType(simpleType, 0);
    this.fieldType = initGenerator(this.methodType);
  }

  protected TypeData initGenerator(TypeData originalType) {
    if (originalType.arrayDepth > 0) {
      //
    }
    return originalType;
  }

  public FieldBuffer setInitializer(String initializer) {
    this.initializer = new PrintBuffer();
    this.initializer.print(initializer);
    return this;
  }
  
  public PrintBuffer getInitializer() {
    if (initializer == null) {
      initializer = new PrintBuffer();
      initializer.indent = indent;
      initializer.println().indent();
    }
    return initializer;
  }

  public FieldBuffer addGetter(int modifiers) {
    if (get == null)
      get = initGetter();
    get.visitModifier(modifiers, cls.context);
    return this;
  }

  protected MethodBuffer initGetter() {
    return cls.createMethod("public "+methodType+" " +getterName()+"()")
        .returnValue(fieldName);
  }

  protected TypeDataTemplateGenerator generateGetter() {
    if (methodType.isArray()) {

    }
    return new TypeDataTemplateGenerator("return $;");

  }

  protected String getterName() {
    return exact ? fieldName : (fieldType.clsName.equalsIgnoreCase("boolean") ? "is" :"get")+methodFragment;
  }

  public FieldBuffer addClearer() {
    if (clear == null)
      clear = initClearer();
    return this;
  }

  protected MethodBuffer initClearer() {
    return cls.createMethod("public "+simpleType+" get"+methodFragment+"()")
        .returnValue(fieldName);
  }

  public FieldBuffer addSetter(int modifier) {
    if (set == null)
      set = initSetter();
    set.visitModifier(modifier, cls.context);
    return this;
  }

  protected MethodBuffer initSetter() {
    MethodBuffer setter = cls.createMethod(
      "public "+fluentReturnType()+" "+setterName()+"(" +simpleType+" " +fieldName + ")"
      ).println("this."+fieldName+" = "+fieldName+";");
    if (isFluent())
      setter.returnValue(fluentReturnValue());
    return setter;
  }

  protected String setterName() {
    return exact ? fieldName : "set"+methodFragment;
  }

  public FieldBuffer addAdder() {
    if (add == null) {
      add = initAdder();
    }
    return this;
  }

  protected MethodBuffer initAdder() {
    return cls.createMethod(
        "public "+fluentReturnType()+" add"+methodFragment+"(" +simpleType+" " +fieldName + ")"
        ).println("this."+fieldName+" = "+fieldName+";")
        .returnValue(fluentReturnValue());
  }

  public FieldBuffer remover() {
    if (remove == null)
      remove = initRemover();
    return this;
  }

  protected MethodBuffer initRemover() {
    return cls.createMethod(
        "public "+fluentReturnType()+" remove"+methodFragment+"(" +simpleType+" " +fieldName + ")"
        ).println("this."+fieldName+" = null;")
        .returnValue(fluentReturnValue());
  }


  protected String fluentReturnType() {
    return (fluent?cls.getSimpleName():"void");
  }

  protected String fluentReturnValue() {
    return (fluent?"this":"");
  }

  public boolean isFluent() {
    return fluent;
  }

  public FieldBuffer setFluent(boolean fluent) {
    this.fluent = fluent;
    return this;
  }

  @Override
  public String addImport(Class<?> cls) {
    return this.cls.addImport(cls);
  }

  @Override
  public String addImport(String cls) {
    if (cls.replace(this.cls.getPackage()+".", "").indexOf('.')==-1)return cls;
    return this.cls.addImport(cls);
  };
  
  @Override
  public String addImportStatic(Class<?> cls, String name) {
    return this.cls.addImportStatic(cls, name);
  }
  
  @Override
  public String addImportStatic(String cls) {
    return this.cls.addImportStatic(cls);
  }

  @Override
  public String toString() {
    if (fieldType == TypeData.NONE)
      return super.toString();

    StringBuilder b = new StringBuilder(NEW_LINE+origIndent);
    if (annotations.size() > 0) {
      for (String anno : annotations)
        b.append('@').append(anno).append(NEW_LINE+origIndent);
    }
    String mods = Modifier.toString(modifier);
    if (mods.length() > 0)
      b.append(mods).append(" ");
    //generics
    if (generics.size() > 0) {
      b.append("<");
      String prefix = "";
      for (String generic : generics) {
        b.append(prefix);
        b.append(generic);
        prefix = ", ";
      }
      b.append("> ");
    }
    //field type
    b.append(simpleType).append(" ");
    //field name
    b.append(fieldName);
    String init = initializer == null ? "" : initializer.toString();
    if (init.length() > 0) {
      b.append(" = ").append(init);
      if (!init.trim().endsWith(";"))
        b.append(";");
    } else
      b.append(";");
    b.append("\n");
    return b.toString()+super.toString();
  }

  public FieldBuffer setExactName(boolean exact) {
    this.exact = exact;
    return this;
  }

  public String getSimpleType() {
    return simpleType;
  }

  public String getName() {
    return fieldName;
  }

}
