package com.google.gwt.reflect.rebind.generators;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.PUBLIC;
import static java.lang.reflect.Modifier.STATIC;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;

import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.client.ConstPool.ArrayConsts;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.rebind.ReflectionManifest;
import com.google.gwt.reflect.rebind.ReflectionUtilType;
import com.google.gwt.reflect.rebind.generators.GwtAnnotationGenerator.GeneratedAnnotation;

@ReflectionStrategy
public class MemberGenerator {

  private static class ManifestMap {
    private final HashMap<String, MemberPoolManifest> existing = new HashMap<String, MemberGenerator.MemberPoolManifest>();
    private final HashMap<String, com.google.gwt.dev.jjs.ast.JMethod> getters = 
        new HashMap<String, com.google.gwt.dev.jjs.ast.JMethod>();
    private com.google.gwt.dev.jjs.ast.JMethod initMethod;
    
    public com.google.gwt.dev.jjs.ast.JMethod findGetterFor(UnifyAstView ast, String memberGetter) throws UnableToCompleteException {
      com.google.gwt.dev.jjs.ast.JMethod getter = getters.get(memberGetter);
      if (getter == null) {
        initMethod(ast);
        JDeclaredType type = ast.translate((JDeclaredType) initMethod.getOriginalReturnType());
        for (com.google.gwt.dev.jjs.ast.JMethod method : type.getMethods()) {
          if (method.getName().endsWith(memberGetter)) {
            getters.put(memberGetter, method);
            return method;
          }
        }
        ast.error(type, "Type "+type.getName()+" does not have member getter method "+memberGetter);
        throw new UnableToCompleteException();
      }
      return getter;
    }

    public com.google.gwt.dev.jjs.ast.JMethod initMethod(UnifyAstView ast) {
      if (initMethod == null) {
        JDeclaredType type = ast.searchForTypeBySource(ConstPool.class.getName());
        for (com.google.gwt.dev.jjs.ast.JMethod method : type.getMethods()) {
          if (method.getName().equals("getMembers")) {
            initMethod = method;
            break;
          }
        }
      }
      return initMethod;
    }
  }

  protected static class MemberPoolManifest {
    
  }
  
  public static final ReflectionStrategy DEFAULT_STRATEGY = MemberGenerator.class.getAnnotation(ReflectionStrategy.class);
  
  public static final String 
  METHOD_SPACER = "_m_",
  FIELD_SPACER = "_f_",
  CONSTRUCTOR_SPACER = "_c_";
  
  private static final String 
  GWT_REFLECT = GwtReflect.class.getName(),
  JSO = JavaScriptObject.class.getSimpleName(),
  NULL_CHECK = "@"+GWT_REFLECT+"::nullCheck(*)(o);";
  private static final Type logLevel = Type.TRACE;

  private static final ThreadLocal<ManifestMap> manifests = new ThreadLocal<ManifestMap>() {
    protected ManifestMap initialValue() {
      return new ManifestMap();
    };
  };
  public static void cleanup() {
    manifests.remove();
  }
  
  public static String getConstructorFactoryName(JClassType type, JParameter[] list) {
    StringBuilder b = new StringBuilder(type.getName());
    b.append(CONSTRUCTOR_SPACER).append(ReflectionUtilType.toUniqueFactory(list, type.getConstructors()));
    return b.toString();
  }

  public static String getFieldFactoryName(JClassType type, String name) {
    StringBuilder b = new StringBuilder(type.getName());
    b.append(FIELD_SPACER).append(name);
    return b.toString();
  }

  public static String getMethodFactoryName(JClassType type, String name,
      JParameter[] list) {
    StringBuilder b = new StringBuilder(type.getName());
    b.append(METHOD_SPACER).append(name);
    // Check for polymorphism
    JMethod[] overloads = type.getOverloads(name);
    if (overloads.length > 1) {
      // Have to use the parameters to make a unique name. 
      // Might be worth it to move this method to instance level, and use a count
      String uniqueName = ReflectionUtilType.toUniqueFactory(list, overloads);
      b.append('_').append(uniqueName);
    }
    return b.toString();
  }
  
  public String generateConstructorFactory(TreeLogger logger, ReflectionGeneratorContext ctx,
      JConstructor ctor, String factory, ReflectionManifest manifest) throws UnableToCompleteException {
    JClassType type = ctor.getEnclosingType();
    String pkg = type.getPackage().getName();
    factory = factory.replace('.', '_');
    SourceBuilder<?> out = ctx.tryCreate(PUBLIC | FINAL, pkg, factory);
    
    if (out == null) {
      // TODO some kind of test to see if structure has changed...
      return pkg + "." + factory;
    }
    
    String simpleName = out.getImports().addImport(type.getQualifiedSourceName());
    
    ClassBuffer cb = out.getClassBuffer();
    
    cb.createConstructor(Modifier.PRIVATE);
    cb.createField("Constructor <"+simpleName+">", "ctor", PRIVATE | STATIC);
    
    MethodBuffer instantiator = cb
        .addImports(Constructor.class, GwtReflect.class)
        .createMethod("public static Constructor <"+simpleName+"> instantiate()")
        .println("if (ctor == null) {")
        .indent()
        .println("ctor = new Constructor<"+simpleName+">(")
        .print(ctor.getEnclosingType().getQualifiedSourceName()+".class, ")
        .print(ReflectionUtilType.getModifiers(ctor)+", ")
        .println("invoker(), ")
        ;
    ConstPoolGenerator.getGenerator();
    GwtRetention retention = manifest.getRetention(ctor);
    
    out.getImports().addStatic(ArrayConsts.class.getCanonicalName()+".EMPTY_CLASSES");
    JParameter[] params = ctor.getParameters();
    instantiator
      .addImports(JavaScriptObject.class)
      .addImports(ArrayConsts.class);
    

    if (retention.annotationRetention() > 0) {
      Annotation[] annos = ReflectionUtilType.extractAnnotations(retention.annotationRetention(), ctor);
      if (annos.length == 0) {
        out.getImports().addStatic(ArrayConsts.class.getCanonicalName()+".EMPTY_ANNOTATIONS");
        instantiator.print("EMPTY_ANNOTATIONS, ");
      } else {
        ctx.getConstPool().arrayOfAnnotations(logger, ctx.getGeneratorContext(), instantiator, annos);
        instantiator.print(", ");
      }
    } else {
      out.getImports().addStatic(ArrayConsts.class.getCanonicalName()+".EMPTY_ANNOTATIONS");
      instantiator.print("EMPTY_ANNOTATIONS, ");
    }
    appendClassArray(instantiator, params, ctx);
    
    instantiator
      .print(", EMPTY_CLASSES ")
      .println(");")
      .outdent()
      .println("}")
      .returnValue("ctor");

    createInvokerMethod(cb, type, type, "new", ctor.getParameters(), true);
    
    if (isDebug(type, ReflectionStrategy.CONSTRUCTOR)) {
      logger.log(Type.INFO, out.toString());
    }
    
    ctx.commit(logger);
    
    return out.getQualifiedName();
  }

  public String generateFieldFactory(TreeLogger logger, GeneratorContext ctx,
      JField field, String factoryName, ReflectionManifest manifest) throws UnableToCompleteException {
    String pkg = field.getEnclosingType().getPackage().getName();
    JClassType enclosingType = field.getEnclosingType();
    JType fieldType = field.getType().getErasedType();
    String jni = field.getType().getJNISignature();
    factoryName = factoryName.replace('.', '_');
    
    SourceBuilder<JField> out = new SourceBuilder<JField>
    ("public final class "+factoryName).setPackage(pkg);

    out.getClassBuffer().createConstructor(PRIVATE);
    
    PrintWriter pw = ctx.tryCreate(logger, pkg, factoryName);
    if (pw == null) {
      if (isDebug(enclosingType, ReflectionStrategy.FIELD)) {
        logger.log(Type.INFO, "Skipped writing field for "+factoryName+", as factory already exists");
      }
      return out.getQualifiedName();
    }
    
    ClassBuffer cb = out.getClassBuffer();


    GwtRetention retention = manifest.getRetention(field);
    final boolean hasAnnos = retention.annotationRetention() > 0;
    final Annotation[] annos;
    if (hasAnnos) {
      annos = ReflectionUtilType.extractAnnotations(retention.annotationRetention(), field);
      generateGetAnnos(logger, out, annos, ctx);
    } else {
      annos = new Annotation[0];
      generateGetAnnos(logger, out, new Annotation[0], ctx);
    }
    
    final String ref = (field.isStatic() ? "" : "o.")+"@"+enclosingType.getQualifiedSourceName()+"::"+field.getName();
    MethodBuffer accessor = cb
        .createMethod("private static JavaScriptObject getAccessor()")
        .setUseJsni(true)
        .println("return {").indent();
    if (hasAnnos) {
      accessor
        .println("annos: function() {")
        .indent()
          .print("return ");
      if (annos.length == 0) {
        accessor.println("[];");
      } else {
        accessor
          .print("@")
          .print(out.getQualifiedName())
          .println("::allAnnos()();");
        
      }
      accessor.outdent().println("},");
    }
    accessor
        .println("getter: function(o) {");
    if (!field.isStatic()) {
      accessor.indentln(NULL_CHECK);
    }
    boolean isPrimitive = field.getType().isPrimitive() != null;
    
    
    accessor.indent().print("return ");
    accessor.append(ref);
    
    accessor
      .println(";")
      .outdent()
      .print("}");
    if (field.isFinal()) {
      accessor.println().outdent().println("};");
    } else {
      accessor.println(", setter: function(o, v) {");
      if (!field.isStatic())
        accessor.indentln(NULL_CHECK);
      accessor.indentln(ref+" = ");
      
      StringBuilder unboxer = new StringBuilder();
      unboxer.append("v"); 
      
      accessor
          .indentln(unboxer+";")
          .println("}")
          .outdent().println("};")
      ;
    }
    
    MethodBuffer instantiate = cb
      .createMethod("public static Field instantiate()")
      .print("return new ")
      .addImports(Field.class, JavaScriptObject.class);
    
    if (isPrimitive) {
      switch (jni.charAt(0)) {
      case 'Z':
        instantiate.addImport("java.lang.reflect.Boolean_Field");
        instantiate.print("Boolean_Field(");
        break;
      case 'B':
        instantiate.addImport("java.lang.reflect.Byte_Field");
        instantiate.print("Byte_Field(");
        break;
      case 'S':
        instantiate.addImport("java.lang.reflect.Short_Field");
        instantiate.print("Short_Field(");
        break;
      case 'C':
        instantiate.addImport("java.lang.reflect.Char_Field");
        instantiate.print("Char_Field(");
        break;
      case 'I':
        instantiate.addImport("java.lang.reflect.Int_Field");
        instantiate.print("Int_Field(");
        break;
      case 'J':
        accessor.addAnnotation(UnsafeNativeLong.class);
        instantiate.addImport("java.lang.reflect.Long_Field");
        instantiate.print("Long_Field(");
        break;
      case 'F':
        instantiate.addImport("java.lang.reflect.Float_Field");
        instantiate.print("Float_Field(");
        break;
      case 'D':
        instantiate.addImport("java.lang.reflect.Double_Field");
        instantiate.print("Double_Field(");
        break;
      default:
        logger.log(Type.ERROR, "Bad primitive type in field generator "+fieldType.getQualifiedSourceName());
        throw new UnableToCompleteException();
      }
    } else {
        String imported = instantiate.addImport(fieldType.getQualifiedSourceName());
        instantiate.print("Field("+imported+".class, ");
    }
    
    String enclosing = instantiate.addImport(field.getEnclosingType().getQualifiedSourceName());
    instantiate
        .print(enclosing+".class, ")
        .print("\""+field.getName()+"\", ")
        .print(ReflectionUtilType.getModifiers(field)+", getAccessor());");
     
    String src = out.toString();
    pw.println(src);
    if (isDebug(enclosingType, ReflectionStrategy.FIELD)) {
      logger.log(Type.INFO, "Field wrapper for "+field.toString()+"\n"+src);
    }
    
    ctx.commit(logger, pw);
    return out.getQualifiedName();
  }

  public String generateMethodFactory(TreeLogger logger, GeneratorContext ctx,
      JMethod method, String factoryName, ReflectionManifest manifest) throws UnableToCompleteException {
    JClassType type = method.getEnclosingType();
    final String pkg = type.getPackage().getName();
    factoryName = factoryName.replace('.', '_');

    PrintWriter pw = ctx.tryCreate(logger, pkg, factoryName);
    if (pw == null) {
      return (pkg.length()==0?"":pkg+".")+factoryName;
    }
    
    SourceBuilder<JMethod> out = new SourceBuilder<JMethod>
      ("public final class "+factoryName+" extends Method").setPackage(pkg);
    ClassBuffer cb = out.getClassBuffer().addImports(Method.class);
    
    
    createInvokerMethod(cb, type, method.getReturnType(), method.getName(), method.getParameters(), method.isStatic());
    
    cb
      .createMethod("public static "+factoryName+" instantiate()")
      .returnValue("new "+factoryName+"()");
    
    cb.createConstructor(Modifier.PRIVATE).println("super(invoker());");
    
    GwtRetention retention = manifest.getRetention(method);
    
    if (retention.annotationRetention() > 0) {
      Annotation[] annos = ReflectionUtilType.extractAnnotations(retention.annotationRetention(), method);
      generateGetAnnos(logger, out, annos, ctx);
    } else {
      generateGetAnnos(logger, out, new Annotation[0], ctx);
    }
    
    generateGetParams(logger, cb, method.getParameters());
    generateGetExceptions(logger, cb, method.getThrows());
    generateGetReturnType(logger, cb, method);
    generateGetName(logger, cb, method);
    generateGetModifier(logger, cb, ReflectionUtilType.getModifiers(method));
    generateGetDeclaringClass(logger, cb, method.getEnclosingType(), "?");
    
    
    String src = out.toString();
    if (isDebug(type, ReflectionStrategy.METHOD)) {
      logger.log(Type.INFO, "Method wrapper for "+method.getReadableDeclaration()+"\n"+src);
    }
    
    pw.println(src);
    
    
    ctx.commit(logger, pw);
    
    return out.getQualifiedName();
  }

  protected void appendClassArray(MethodBuffer out, JParameter[] params,
      ReflectionGeneratorContext ctx) {
    int i = params.length;
    String[] names = new String[i];
    for (;i-->0;) {
      names[i] = params[i].getType().getErasedType().getQualifiedSourceName();
    }
    ConstPoolGenerator constPool = ctx.getConstPool();
    constPool.arrayOfClasses(ctx.getLogger(), out, names);
  }

  private void generateGetAnnos(TreeLogger logger, SourceBuilder<?> sb, Annotation[] annos, GeneratorContext ctx) throws UnableToCompleteException {
    if (annos.length == 0)return;
    
    MethodBuffer getAnno = sb.getClassBuffer().createMethod(
        "public <T extends Annotation> T getAnnotation(Class<T> cls)")
        .setUseJsni(true);
    sb.getClassBuffer().createMethod(
        "public Annotation[] getAnnotations()")
        .returnValue("allAnnos()");
        
    MethodBuffer getAnnos = sb.getClassBuffer().createMethod(
        "private static Annotation[] allAnnos()")
        .println("return new Annotation[]{");
    getAnnos.addImport(Annotation.class);
    if (annos.length > 0) {
      GeneratedAnnotation gen = GwtAnnotationGenerator.generateAnnotationProvider(logger, sb, annos[0], ctx);
      getAnnos.println(gen.providerClass()+"."+gen.providerMethod()+"()");
      getAnnos.addImport(gen.providerQualifiedName());
      getAnno
        .println("switch (@"+ConstPool.class.getName()+"::constId(Ljava/lang/Class;)(cls)) {")
        .indent()
        .print("case @"+ConstPool.class.getName()+"::constId(Ljava/lang/Class;)")
        .println("(@" + gen.getAnnoName() + "::class) :")
        .indentln(" return @" + gen.providerQualifiedName() + "::" + gen.providerMethod()+"()();");
      for (int i = 1, m = annos.length; i < m; i++ ) {
        gen = GwtAnnotationGenerator.generateAnnotationProvider(logger, sb, annos[i], ctx);
        getAnnos.println(", "+gen.providerClass()+"."+gen.providerMethod()+"()");
        getAnnos.addImport(gen.providerQualifiedName());
        getAnno
          .print("case @"+ConstPool.class.getName()+"::constId(Ljava/lang/Class;)")
          .println("(@" + gen.getAnnoName() + "::class) :")
          .indentln(" return @" + gen.providerQualifiedName() + "::" + gen.providerMethod()+"()();");
      }
      getAnno.outdent().println("}");
    }
    getAnno
      .println("return null;");
    getAnnos
      .println("};");
  }
  

  
  private void generateGetDeclaringClass(TreeLogger logger, ClassBuffer cb, JClassType type, String generic) {
    MethodBuffer getDeclaringClass = cb.createMethod("public Class<"+generic+ "> getDeclaringClass()");
    if (type.isPrivate()) {
      getDeclaringClass
        .setUseJsni(true)
        .returnValue("@"+type.getQualifiedSourceName()+"::class");
    } else {
      getDeclaringClass
        .returnValue(type.getQualifiedSourceName()+".class");
    }
  }

  private void generateGetExceptions(TreeLogger logger, ClassBuffer cb, JClassType[] exceptions) {
    if (exceptions.length == 0)return;
    MethodBuffer getExceptions = cb
        .createMethod("public Class<?>[] getExceptionTypes()")
        .println("return new Class<?>[]{");
    if (exceptions.length > 0) {
      getExceptions.println(toClass(exceptions[0]));
      for (int i = 1, m = exceptions.length; i < m; i ++) {
        getExceptions.println(", "+toClass(exceptions[i]));
      }
    }
    getExceptions.println("};");
  }

  private void generateGetModifier(TreeLogger logger, ClassBuffer cb, int mod) {
    cb.createMethod("public int getModifiers()").returnValue(Integer.toString(mod));
  }

  private void generateGetName(TreeLogger logger, ClassBuffer cb, JMethod method) {
    cb.createMethod("public String getName()").returnValue("\""+method.getName()+ "\"");
  }

  private void generateGetParams(TreeLogger logger, ClassBuffer cb, JParameter[] params) {
    if (params.length == 0) {
      return;
    }
    MethodBuffer getParameters = cb
      .createMethod("public Class<?>[] getParameterTypes()")
      .println("return new Class<?>[]{");
    if (params.length > 0) {
      getParameters.println(toClass(params[0]));
      for (int i = 1, m = params.length; i < m; i ++) {
        getParameters.println(", "+toClass(params[i]));
      }
    }
    getParameters.println("};");
  }

  private void generateGetReturnType(TreeLogger logger, ClassBuffer cb, JMethod method) {
    cb.createMethod("public Class<?> getReturnType()").returnValue(
        method.getReturnType().getErasedType().getQualifiedSourceName()+".class");
  }
  
  private void maybeFinishBoxing(PrintBuffer invoke, JType returnType) {
    JPrimitiveType prim = returnType.isPrimitive();
    if (prim != null) {
      switch (prim) {
        case BOOLEAN:
          invoke.print(" ? @java.lang.Boolean::TRUE : @java.lang.Boolean::FALSE");
        case VOID:
          return;
        default:
          invoke.print(")");
      }
    }
  }

  private void maybeFinishUnboxing(StringBuilder b, JType returnType) {
    JPrimitiveType type = returnType.isPrimitive();
    if (type != null)
    switch(type) {
      case BOOLEAN:
        b.append(".@java.lang.Boolean::booleanValue()()");
        break;
      case CHAR:
        b.append(".@java.lang.Character::charValue()()");
        break;
      case LONG:
        b.append(")");
        break;
      case BYTE:
      case DOUBLE:
      case INT:
      case FLOAT:
      case SHORT:
        b.append(".@java.lang.Number::doubleValue()()");
        break;
      default:
    }
  }

  private void maybeStartBoxing(PrintBuffer invoke, JType returnType) {
    JPrimitiveType prim = returnType.isPrimitive();
    if (prim != null) {
      switch (prim) {
      case LONG:
        invoke.print("@"+GWT_REFLECT+"::boxLong(J)(");
        break;
      case BYTE:
        invoke.print("@java.lang.Byte::new(B)(");
        break;
      case CHAR:
        invoke.print("@java.lang.Character::new(C)(");
        break;
      case DOUBLE:
        invoke.print("@java.lang.Double::new(D)(");
        break;
      case FLOAT:
        invoke.print("@java.lang.Float::new(F)(");
        break;
      case INT:
        invoke.print("@java.lang.Integer::new(I)(");
        break;
      case SHORT:
        invoke.print("@java.lang.Short::new(S)(");
        break;
      default:
      }
    }
  }
  private void maybeStartUnboxing(StringBuilder b, JType returnType) {
    if (JPrimitiveType.LONG == returnType.isPrimitive()) {
        b.append("@"+GWT_REFLECT+"::unboxLong(Ljava/lang/Number;)(");
    }
  }
  protected JMethodCall checkConstPool(UnifyAstView ast, JMethodCall callSite, JExpression classRef,
      JExpression ... args) throws UnableToCompleteException {
    com.google.gwt.dev.jjs.ast.JMethod initPool = getMemberPoolInit(ast);

    JMethodCall getMemberPool = new JMethodCall(initPool.getSourceInfo(), null, initPool);
    getMemberPool.addArg(classRef);
    
    ManifestMap map = manifests.get();
    com.google.gwt.dev.jjs.ast.JMethod getter = map.findGetterFor(ast, memberGetter());
    
    JMethodCall checkPool = new JMethodCall(initPool.getSourceInfo(), getMemberPool, getter);
    for (JExpression arg : args) {
      checkPool.addArg(arg);
    }
    
    return checkPool;
  }

  protected void createInvokerMethod(ClassBuffer cb, JClassType type, JType returnType, 
      String methodName, JParameter[] params, boolean isStatic) {
    boolean hasLong = returnType.getJNISignature().equals("J");
    
    StringBuilder functionSig = new StringBuilder();
    StringBuilder jsniSig = new StringBuilder();
    StringBuilder arguments = new StringBuilder();
    // Fill in parameter data
    boolean isNotCtor = !"new".equals(methodName);
    assert isStatic || isNotCtor : "Constructors must be static!";
    
    for (int i = 0, m = params.length; i < m; i++) {
      JType param = params[i].getType();
      jsniSig.append(param.getJNISignature());
      char varName = Character.toUpperCase(Character.forDigit(10+i, 36));
      if (isNotCtor || i > 0) {
        functionSig.append(", ");
      }
      functionSig.append(varName);
      if (i > 0) {
        arguments.append(", ");
      }
      maybeStartUnboxing(arguments, param);
      arguments.append(varName);
      maybeFinishUnboxing(arguments, param);
      hasLong |= "J".equals(param.getJNISignature());
    }
    
    MethodBuffer invoker = cb.addImports(JavaScriptObject.class)
        .createMethod("private static "+JSO+" "+ "invoker()")
        .setUseJsni(true)
        .print("return function(");
    if (isNotCtor) {
      invoker.print("o");
    }
      if (hasLong)
        invoker.addAnnotation(UnsafeNativeLong.class);
      invoker.println(functionSig + ") {");
      if (!isStatic) {
        invoker.indentln(NULL_CHECK);
      }
      // Build the structure of the method invoker javascript function
      if (isReturnable(returnType)) invoker.print("return ");
      maybeStartBoxing(invoker, returnType);
      if (!isStatic){
        invoker.print("o.");
      }
      invoker
        .indent()
        .print("@").print(type.getQualifiedSourceName())
        .print("::")
        .print(methodName)
        .print("(")
        .print(jsniSig.toString())
        .print(")(")
        .print(arguments.toString())
        .print(")");
      maybeFinishBoxing(invoker, returnType);
      
      invoker
        .println(";")
        .outdent()
        .println("};");
      
  }
  
  protected com.google.gwt.dev.jjs.ast.JMethod getMemberPoolInit(UnifyAstView ast) {
    ManifestMap map = manifests.get();
    return map.initMethod(ast);
  }
  protected boolean isDebug(JClassType type, int memberType) {
    ReflectionStrategy strategy = type.getAnnotation(ReflectionStrategy.class);
    if (strategy == null) {
      strategy = type.getPackage().getAnnotation(ReflectionStrategy.class);
    }
    if (strategy == null) {
      return false;
    }
    return (strategy.debug() & memberType) > 0;
  }
  
  protected boolean isReturnable(JType returnType) {
    return !"V".equals(returnType.getJNISignature());
  }
  
  protected String memberGetter() {
    throw new UnsupportedOperationException("memberGetting not implemented by "+getClass().getName());
  }

  protected boolean shouldFailIfMissing(TreeLogger logger, UnifyAstView ast, JClassLiteral classLit) {
    PropertyOracle properties = ast.getGeneratorContext().getPropertyOracle();
    try {
      if ("true".equals(properties.getConfigurationProperty("gwt.reflect.never.fail").getValues().get(0))) {
        return false;
      }
    } catch (BadPropertyValueException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }

  protected String toClass(JClassType param) {
    return param.getErasedType().getQualifiedSourceName()+".class";
  }
  
  

  protected String toClass(JParameter param) {
    return param.getType().getErasedType().getQualifiedSourceName()+".class";
  }

  Type logLevel() {
    return logLevel;
  }

}
