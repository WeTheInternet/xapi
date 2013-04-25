package xapi.dev.source;

import java.lang.reflect.Modifier;

public class FieldBuffer extends MemberBuffer<FieldBuffer> {

  private final ClassBuffer cls;
  private final String simpleType;
  private final String name;
  private final String methodName;// Camel-case form of name, for getField, setField methods.

  private String initializer;
  private MethodBuffer get;
  private MethodBuffer set;
  private MethodBuffer add;
  private MethodBuffer remove;
  private MethodBuffer clear;
  private boolean fluent = true;
  private String origIndent;
  private boolean exact;
  
  public FieldBuffer(ClassBuffer enclosingClass, String type, String name) {
    this(enclosingClass, type, name, INDENT);
  }
  
  public FieldBuffer(ClassBuffer enclosingClass, String type, String name, String indent) {
    this.cls = enclosingClass;
    System.out.println("Field "+type+" "+name);
    this.simpleType = enclosingClass.addImport(type);
    this.name = name;
    this.methodName = Character.toUpperCase(name.charAt(0)) + (name.length()==0?"":name.substring(1));
    origIndent = indent;
    this.indent = indent + INDENT;
  }
  
  public FieldBuffer setInitializer(String initializer) {
    this.initializer = initializer;
    return this;
  }

  public FieldBuffer addGetter(int modifiers) {
    if (get == null)
      get = initGetter();
    get.visitModifier(modifiers, cls.context);
    return this;
  }
  
  protected MethodBuffer initGetter() {
    return cls.createMethod("public "+simpleType+" " +getterName()+"()")
        .returnValue(name);
  }

  protected String getterName() {
    return exact ? name : "get"+methodName;
  }

  public FieldBuffer addClearer() {
    if (clear == null)
      clear = initClearer();
    return this;
  }
  
  protected MethodBuffer initClearer() {
    return cls.createMethod("public "+simpleType+" get"+methodName+"()")
        .returnValue(name);
  }

  public FieldBuffer addSetter(int modifier) {
    if (set == null)
      set = initSetter();
    set.visitModifier(modifier, cls.context);
    return this;
  }
  
  protected MethodBuffer initSetter() {
    return cls.createMethod(
        "public "+fluentReturnType()+" "+setterName()+"(" +simpleType+" " +name + ")"
      ).println("this."+name+" = "+name+";")
      .returnValue(fluentReturnValue());
  }

  protected String setterName() {
    return exact ? name : "set"+methodName;
  }
  
  public FieldBuffer addAdder() {
    if (add == null) {
      add = initAdder();
    }
    return this;
  }
  
  protected MethodBuffer initAdder() {
    return cls.createMethod(
        "public "+fluentReturnType()+" add"+methodName+"(" +simpleType+" " +name + ")"
        ).println("this."+name+" = "+name+";")
        .returnValue(fluentReturnValue());
  }

  public FieldBuffer remover() {
    if (remove == null)
      remove = initRemover();
    return this;
  }
  
  protected MethodBuffer initRemover() {
    return cls.createMethod(
        "public "+fluentReturnType()+" remove"+methodName+"(" +simpleType+" " +name + ")"
        ).println("this."+name+" = null;")
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

  public void setFluent(boolean fluent) {
    this.fluent = fluent;
  }
  
  @Override
  public String addImport(Class<?> cls) {
    return this.cls.addImport(cls);
  }
  
  public String addImport(String cls) {
    return this.cls.addImport(cls);
  };

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder(NEW_LINE+origIndent);
    if (annotations.size() > 0) {
      for (String anno : annotations)
        b.append('@').append(anno).append(NEW_LINE+origIndent);
    }
    b.append(Modifier.toString(modifier));
    b.append(" ");
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
    b.append(name);
    
    if (initializer != null && initializer.length() > 0) {
      b.append(" = ").append(initializer);
      if (!initializer.trim().endsWith(";"))
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
    return name;
  }
  
}
