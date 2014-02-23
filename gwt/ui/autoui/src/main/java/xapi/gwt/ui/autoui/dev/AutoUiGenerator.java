package xapi.gwt.ui.autoui.dev;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.PRIVATE;
import static java.lang.reflect.Modifier.STATIC;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.annotation.Generated;
import javax.inject.Named;

import xapi.collect.impl.SimpleFifo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.source.write.MappedTemplate;
import xapi.ui.autoui.api.AlwaysTrue;
import xapi.ui.autoui.api.BeanValueProvider;
import xapi.ui.autoui.api.UiOptions;
import xapi.ui.autoui.api.UiRenderer;
import xapi.ui.autoui.api.UiRendererOptions;
import xapi.ui.autoui.api.UiRendererSelector;
import xapi.ui.autoui.api.UiRenderingContext;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.user.server.Base64Utils;
import com.google.gwt.util.tools.shared.Md5Utils;

public class AutoUiGenerator {

  private static final JType[] EMPTY_PARAMS = new JType[0];
  private static final Set<String> PRIMITIVES = ImmutableSet.<String>builder()
      .add(Boolean.class.getName())
      .add(Byte.class.getName())
      .add(Character.class.getName())
      .add(Short.class.getName())
      .add(Integer.class.getName())
      .add(Long.class.getName())
      .add(Float.class.getName())
      .add(Double.class.getName())
      .add(String.class.getName())
      .build();
  
  final Map<String, JMethod> methods = new LinkedHashMap<String, JMethod>();
  final String clsName;
  final Map<String, Class<?>> factories = new HashMap<String, Class<?>>();
  final Map<String, Integer> templates = new HashMap<String, Integer>();
  int ctxCnt;
  final SourceBuilder<UnifyAstView> out;
  
  public static final String generateUiProvider(TreeLogger logger, UnifyAstView ast, JClassType uiModel, JClassType uiType) {
    AutoUiGenerator ctx = new AutoUiGenerator(ast, uiModel, uiType);
    String src = ctx.out.toString();
    final String digest = 
        Base64Utils.toBase64(Md5Utils.getMd5Digest(src.getBytes()));
    
    String name = ctx.out.getQualifiedName();
    JClassType existing = ast.getTypeOracle().findType(name), winner = null;
    int pos = 0;
    while (true) {
      winner = existing;
      String next = name+pos++;
      existing = ast.getTypeOracle().findType(next);
      if (existing == null) {
        break;
      } else {
        name = next;
      }
    }
    if (winner != null) {
      // Only use the existing class if the source exactly matches what we just generated.
      Generated gen = existing.getAnnotation(Generated.class);
      if (gen.value()[1].equals(digest)) {
        return name;
      }
    }
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    ctx.out.getClassBuffer().setSimpleName(name.replace(ctx.out.getPackage()+".", ""));
    ctx.out.getClassBuffer().addAnnotation("@"+
        ctx.out.getImports().addImport(Generated.class)+"("+
        "date=\""+df.format(new Date())+"\",\n" +
    		"value={\"" + AutoUiGenerator.class.getName()+"\","+
    		"\""+digest+"\"})");
    StandardGeneratorContext gen = ast.getGeneratorContext();
    PrintWriter pw = gen.tryCreate(logger, ctx.out.getPackage(), ctx.out.getClassBuffer().getSimpleName());
    src = ctx.out.toString();
    pw.print(src);
    gen.commit(logger, pw);
    gen.finish(logger);
    logger.log(Type.INFO, src);
    try {
      return name;
    } finally {
      ctx.methods.clear();
      ctx.factories.clear();
      ctx.templates.clear();
      ctx.out.destroy();
    }
  }
  
  private AutoUiGenerator(UnifyAstView ast, JClassType uiModel, JClassType uiType) {
    clsName = uiType.getSimpleSourceName()+"_"+uiModel.getSimpleSourceName()+"_Factory";
    out = new SourceBuilder<UnifyAstView>(
        "public final class "+clsName);
    out.setPackage(uiModel.getPackage().getName());
    out.setPayload(ast);
    
    JPrimitiveType voidType = ast.getProgram().getTypeVoid();
    SimpleFifo<String> simpleNames = new SimpleFifo<String>();
    
    extractAllMethods(uiModel, voidType);
    
    simpleNames.giveAll(methods.keySet());
    
    String uiCls = out.getImports().addImport(uiType.getErasedType().getQualifiedSourceName());

    ClassBuffer beanCls = 
        out.getClassBuffer().createInnerClass("private static final class Bean")
        .setSuperClass(out.getImports().addImport(BeanValueProvider.class));
    
    List<String> inits = new ArrayList<String>();
    if (uiModel.isAnnotationPresent(UiOptions.class)) {
      UiOptions opts = uiModel.getAnnotation(UiOptions.class);
      if (opts.fields().length > 0) {
        simpleNames.clear();
        simpleNames.giveAll(opts.fields());
      }
      for (UiRendererOptions renderer : opts.renderers()) {
        inits.addAll(addClassRenderer(renderer, out, ast));
      }
    }
    
    if (uiModel.isAnnotationPresent(UiRendererOptions.class)) {
      inits.addAll(addClassRenderer(uiModel.getAnnotation(UiRendererOptions.class), out, ast));
    }
    
    for (JMethod method : methods.values()) {
      if (method.isAnnotationPresent(UiRendererOptions.class)) {
        inits.addAll(addMethodRenderer(method, out, ast));
      }
    }
    

    MethodBuffer builder = out.getClassBuffer().createMethod(
        "public static final "+ uiCls + " newUi()");
    
    builder
      .println("final Bean bean = new Bean();")
      .println("final "+uiCls+" ui = new "+uiCls+"();")
      .println("ui.setRenderers(new " + builder.addImport(UiRenderingContext.class) +"[]{")
      .indent()
    ;
    for (int i = 0, m = inits.size()-1; i <= m; i ++) {
      String init = inits.get(i);
      builder
        .print(init)
        .println(i == m ? "" : ",");
    }
    builder
      .outdent()
      .println("});")
      .returnValue("ui");
    
    MethodBuffer beanCtor = beanCls
        .createConstructor(PRIVATE)
        .print("setChildKeys(new String[]{");
    if (!simpleNames.isEmpty()) {
      beanCtor
      .println()
      .indentln("\""+simpleNames.join("\", \"")+"\"");
    }
    beanCtor.println("});");
    
    MethodBuffer valueProvider = beanCls
        .createMethod("protected Object valueOf(String name, Object o)")
        .setUseJsni(true)
        .println("switch(name) {")
        .indent()
        .println("case 'this': return o;")
        // TODO prefer @Named value
        .println("case 'this.name()': return o.@java.lang.Object::getClass()().@java.lang.Class::getName()();");
    for (String name : methods.keySet()) {
      JMethod method = methods.get(name);
      valueProvider.println("case '"+name+".name()': return '"+name+"';");
      String rootMethodClass = getMethodRoot(method);
      valueProvider.println("case '"+name+"': return o.@"+rootMethodClass+"::" +
      		method.getName()+"()();");
    }
    valueProvider
        .println("default: return @"+BeanValueProvider.class.getName()+
            "::illegalArg(Ljava/lang/String;)(name);")
    		.outdent()
    		.println("};");
  }
  

  private String getMethodRoot(JMethod method) {
    JClassType winner = method.getEnclosingType();
    if (winner.isInterface() != null) {
      for (JClassType type : winner.getImplementedInterfaces()) {
        if (type.findMethod(method.getName(), EMPTY_PARAMS) != null) {
          if (type.isAssignableFrom(winner)) {
            winner = type;
          }
        }
      }
    }
    return winner.getErasedType().getQualifiedSourceName();
  }


  private void extractAllMethods(JClassType uiModel, JPrimitiveType voidType) {
    for (JClassType type : uiModel.getFlattenedSupertypeHierarchy()) {
      for (JMethod method : type.getMethods()) {
        if (isGetterMethod(method, voidType)) {
          String name = toSimpleName(method);
          if (!methods.containsKey(name)) {
            methods.put(name, method);
          }
        }
      }
    }
  }


  protected String getStaticInstance(Class<?> selector, SourceBuilder<UnifyAstView> out) {
    String name = selector.getSimpleName().toUpperCase();
    int tries = 0;
    while (factories.containsKey(name)) {
      if (factories.get(name).getCanonicalName().equals(selector.getCanonicalName())) {
        return name;
      }
      name = selector.getSimpleName().toUpperCase() + tries++;
    }
    out.getClassBuffer().createField(
      selector, name, PRIVATE | FINAL | STATIC)
      .setInitializer("new "+out.getImports().addImport(selector)+"()");
    factories.put(name, selector);
    return name;
  }


  protected String toSimpleName(JMethod method) {
    if (method.isAnnotationPresent(Named.class)) {
      return method.getAnnotation(Named.class).value();
    }
    String name = method.getName();
    if (name.startsWith("get") || name.startsWith("has")) {
      if (name.length() > 3 && Character.isUpperCase(name.charAt(3))) {
        return Character.toLowerCase(name.charAt(3)) + 
            (name.length() > 4 ? name.substring(4) : "");
      }
    } else if (name.startsWith("is")) {
      if (name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
        return Character.toLowerCase(name.charAt(2)) + 
            (name.length() > 3 ? name.substring(3) : "");
      }
    }
    return name;
  }


  private List<String> addClassRenderer(UiRendererOptions renderer, SourceBuilder<UnifyAstView> out, UnifyAstView ast) {
    return addRenderer(renderer.isWrapper()?"bean.rebaseAll()":"bean", "",  renderer, out, ast);
  }

  private List<String> addMethodRenderer(JMethod method, SourceBuilder<UnifyAstView> out,  UnifyAstView ast) {
    UiRendererOptions anno = method.getAnnotation(UiRendererOptions.class);
    String name = toSimpleName(method);
    return addRenderer("bean.rebase(\"" +name+"\")", name, anno, out, ast);
  }

  @SuppressWarnings("rawtypes")
  private List<String> addRenderer(String bean, String path, UiRendererOptions renderer,
      SourceBuilder<UnifyAstView> out, UnifyAstView ast) {
    Class<? extends UiRendererSelector> selector = renderer.selector();
    final String template = getTemplate(renderer, out);
    final List<String> inits = new ArrayList<String>();
    for (Class<? extends UiRenderer> renderCls : renderer.renderers()) {
      String name = "ctx"+ctxCnt++;
      MethodBuffer provider = out.getClassBuffer().createMethod(PRIVATE | FINAL | STATIC, UiRenderingContext.class, name, 
          out.getImports().addImport(BeanValueProvider.class)+" bean");
      final String ctxCls = out.getImports().addImport(UiRenderingContext.class);
      provider.println(ctxCls +" ctx = new "+ctxCls+"(" +
          getStaticInstance(renderCls, out)+");");

      provider.println("ctx.setBeanProvider(bean);");
      if (renderer.isHead()) {
        provider.println("ctx.setHead(true);");
      } else if (renderer.isTail()) {
        provider.println("ctx.setTail(true);");
      }
      if (renderer.isWrapper()) {
        provider.println("ctx.setWrapper(true);");
      }
      if (selector != AlwaysTrue.class) {
        provider.println("ctx.setSelector("+getStaticInstance(selector, out)+");");
      }
      if (!"".equals(path)) {
        provider.println("ctx.setName(\""+path+"\");");
      }
      provider.println(template);
      
      provider.println("return ctx;");
      
      inits.add(name+"("+bean+")");
    }
    return inits;
  }

  private String getTemplate(UiRendererOptions renderer, SourceBuilder<UnifyAstView> out) {
    final String t = renderer.template();
    if (t.length() > 0) {
      // Assemble all the keys to be used in the template.
      SimpleFifo<String> replaceables = new SimpleFifo<String>();
      for (String key : renderer.templatekeys()) {
        if (t.contains(key)) {
          replaceables.give(key);
        }
      }
      int depth = 10;
      for (String key : methods.keySet()) {
        if (t.contains("${"+key+"}")) {
          replaceables.give("${"+key+"}");
        }
        if (t.contains("${"+key+".name()}")) {
          replaceables.give("${"+key+".name()}");
        }
        JMethod method = methods.get(key);
        if (isNonPrimitive(method, out.getPayload())) {
          // create nested template field
        }
      }
      String initializer = 
      "new "+
          out.getImports().addImport(MappedTemplate.class)+"(\""+Generator.escape(t)+"\", new String[]{"+
          (replaceables.isEmpty()?"":"\""+replaceables.join("\", \"")+"\"")
          +"});";
      final int key;
      if (templates.containsKey(initializer)) {
        key = templates.get(initializer);
      } else {
        key = templates.size();
        templates.put(initializer, key);
        out.getClassBuffer().createField(MappedTemplate.class, "TEMPLATE_"+key, PRIVATE | STATIC | FINAL)
          .setInitializer(initializer);
      }
      return "ctx.setTemplate(TEMPLATE_"+key+");";
    } else {
      return "";
    }
  }

  private boolean isNonPrimitive(JMethod method, UnifyAstView ast) {
    JType returnType = method.getReturnType();
    return returnType.isPrimitive() == null 
        && !PRIMITIVES.contains(returnType.getQualifiedSourceName());
  }

  private boolean isGetterMethod(JMethod method, JPrimitiveType voidType) {
    return method.isPublic() && method.getParameters().length == 0 && method.getReturnType() != voidType;
  }

}
