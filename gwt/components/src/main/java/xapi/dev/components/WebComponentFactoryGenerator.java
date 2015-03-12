package xapi.dev.components;

import static java.lang.reflect.Modifier.PRIVATE;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.dev.util.collect.Sets;
import com.google.gwt.thirdparty.guava.common.collect.HashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import xapi.components.api.JsoConsumer;
import xapi.components.api.JsoSupplier;
import xapi.components.api.NativelySupported;
import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentCallback;
import xapi.components.api.WebComponentFactory;
import xapi.components.api.WebComponentMethod;
import xapi.components.impl.JsSupport;
import xapi.components.impl.WebComponentBuilder;
import xapi.components.impl.WebComponentSupport;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.Style;

public class WebComponentFactoryGenerator extends IncrementalGenerator {

  private static enum BuiltInType {
    // Uses non-standard bean-casing so we can use .name() to generate code to
    // match methods
    // in the WebComponentSupport class
    created, attached, detached, attributeChanged;

    public static BuiltInType find(final JMethod method) throws UnableToCompleteException {
      switch (method.getName()) {
      // TODO use complete signature here
      case "onAttached":
        return attached;
      case "onDetached":
        return detached;
      case "onCreated":
        return created;
      case "onAttributeChanged":
        return attributeChanged;
      default:
        return null;
      }
    }

  }

  private static class MethodData {

    private final String accessorName;
    private String       name;
    private String       getterName;
    private String       setterName;
    private String       getterClass;
    private String       setterClass;
    private BuiltInType  type;
    private boolean      enumerable   = false;
    private boolean      configurable = true;
    private boolean      writeable    = false;
    public String        valueClass;
    public boolean       mapToAttribute;
    public boolean useJsniWildcard;

    public MethodData(final String name) {
      this.accessorName = this.name = name;
    }

    public MethodData(final String accessorName, final String name) {
      this.accessorName = accessorName;
      this.name = name;
    }

    private boolean isProperty() {
      return getterName != null || setterName != null;
    }

  }

  private static final Pattern BEAN_NAME = Pattern.compile("(is|get|set)(.+)");
  private JClassType           stringType;
  private JClassType           webComponentCallback;

  private static final String BOX_HELPER = "@" + JsSupport.class.getName()
    + "::";

  private static final String JSO_PARAM  =
    "Lcom/google/gwt/core/client/JavaScriptObject;";

  @Override
  public RebindResult generateIncrementally(final TreeLogger logger,
    final GeneratorContext context, final String typeName)
      throws UnableToCompleteException {
    final JClassType type = context.getTypeOracle().findType(typeName);
    final WebComponent component = type.getAnnotation(WebComponent.class);
    if (component == null) {
      logger.log(Type.ERROR, "Type " + type.getQualifiedSourceName()
        + " missing required annotation, " + WebComponent.class.getName());
      throw new UnableToCompleteException();
    }
    if (component.tagName().indexOf('-') == -1) {
      logger.log(Type.ERROR,
        "WebCompoenent for " + type.getQualifiedSourceName()
        + " has invalid tag name " + component.tagName() + "; "
        + "Custom elements must contain the - character");
      throw new UnableToCompleteException();
    }
    final String pkg = type.getPackage().getName();
    final String simple = type.getQualifiedSourceName().replace(pkg + ".", "");
    final String factoryName = simple.replace('.', '_') + "_WebComponentFactory";
    final String qualifiedName = pkg + "." + factoryName;

    // TODO reenable this once we add strong hashing support to ensure types
    // have not changed.
    // if (context.tryReuseTypeFromCache(qualifiedName)) {
    // return new RebindResult(RebindMode.USE_ALL_CACHED, qualifiedName);
    // }

    final PrintWriter pw = context.tryCreate(logger, pkg, factoryName);
    if (pw == null) {
      return new RebindResult(RebindMode.USE_EXISTING, qualifiedName);
    }

    final SourceBuilder<JClassType> sourceBuilder = new SourceBuilder<JClassType>
    ("public final class " + factoryName)
    .setPackage(pkg);
    final ClassBuffer out =
      sourceBuilder.getClassBuffer()
      .addInterface(
        WebComponentFactory.class.getCanonicalName() + "<" + simple + ">");
    final String builder = out.addImport(WebComponentBuilder.class);
    final String support = out.addImport(WebComponentSupport.class);
    final String jso = out.addImport(JavaScriptObject.class);
    final String selfType = out.addImport(typeName);
    final String proto = generatePrototypeAccessor(out, component.extendProto(), jso);

    stringType = context.getTypeOracle().findType("java.lang.String");
    webComponentCallback = context.getTypeOracle().findType(WebComponentCallback.class.getName());

    final Multimap<String, MethodData> methods = HashMultimap.create();
    final boolean hasCallbacks = type.isAssignableTo(webComponentCallback);
    final List<JClassType> flattened = new ArrayList<JClassType>(type.getFlattenedSupertypeHierarchy());
    for (int i = flattened.size(); i --> 0; ) {
      final JClassType iface = flattened.get(i);
      generateFunctionAccessors(logger, context, iface, methods, hasCallbacks);
    }

    final String supplier = out.addImport(Supplier.class);
    out
    .createField(supplier + "<" + simple + ">", "ctor")
    .makeStatic()
    .makePrivate();
    // Initialize the web component in a static block
    out
    .println("static {")
    .indent();
    if (hasCssToInject(type)) {
      final String injectCss = out.addImportStatic("xapi.elemental.X_Elemental.injectCss");
      out.println(injectCss+"("+selfType+".class);");
    }
    out
    .println(builder + " builder = " + builder + ".create(" + proto + ");");

    if (component.extendProto().length > 1) {
      out
        .println("builder.setExtends(\"" + component.extendProto()[1] + "\");");
    }
    final Set<String> seen = new HashSet<>();
    for (int i = flattened.size(); i --> 0; ) {
      final JClassType key = flattened.get(i);
      for (final MethodData method : methods.get(key.getQualifiedSourceName())) {
        if (method.isProperty()) {
          printPropertyAccess(logger, out, method, builder, seen);
        } else {
          printValueAccess(logger, out, method, builder, seen);
        }
      }
    }
    out
    .print("ctor = " + support + ".register(")
    .print("\"" + component.tagName() + "\"")
    .println(", builder.build());")

    .outdent()
    .println("}")
    .createMethod(
      "public " + simpleName(type) + " newComponent()")
      .returnValue("ctor.get()");
    ;
    final String src = sourceBuilder.toString();
    logger.log(logLevel(typeName), "\nWeb Component Factory: \n" + src);

    pw.println(src);
    context.commit(logger, pw);

    return new RebindResult(RebindMode.USE_ALL_NEW, qualifiedName);
  }

  @Override
  public long getVersionId() {
    return 0;
  }

  protected Type logLevel(final String typeName) {
    return
      //        BooleanPickerElement.class.getName().equals(typeName) ? Type.INFO : Type.TRACE;
      Type.DEBUG;
  }

  private String accessorName(final JMethod method) {
    final StringBuilder b = new StringBuilder(method.getName());
    for (final JParameter param : method.getParameters()) {
      b.append('_').append(param.getName());
    }
    return b.toString();
  }

  private String debean(final String name) {
    final Matcher matcher = BEAN_NAME.matcher(name);
    if (matcher.matches()) {
      final String match = matcher.group(2);
      return Character.toLowerCase(match.charAt(0)) + (match.length() > 0 ? match.substring(1) : "");
    }
    return name;
  }

  private void generateDefaultFunctionAccessor(final TreeLogger logger,
    final JMethod method, final MethodData data, final ClassBuffer cls, final Set<String> helpers) {
    final String qualified = method.getEnclosingType().getQualifiedSourceName();
    final String typeName = cls.addImport(qualified);
    cls.addImport(JavaScriptObject.class);
    final MethodBuffer out =
      cls
      .createMethod(
        "public static JavaScriptObject " + data.accessorName + "()")
        .addParameters(typeName + " o")
        .makeJsni()
        .addImports(JavaScriptObject.class)
        .print("var func = o.@" + qualified + "::" + method.getName() + "(");
    if (data.useJsniWildcard) {
      out.print("*");
    } else {
      for (final JType param : method.getParameterTypes()) {
        out.print(param.getJNISignature());
      }
    }
    final StringBuilder params = new StringBuilder();
    final Map<Character, String> boxers = new LinkedHashMap<>();
    char paramName = 'a';
    for (final JType param : method.getParameterTypes()) {
      // out.print(param.getErasedType().getJNISignature());
      if (params.length() > 0) {
        params.append(',');
      }
      params.append(paramName);
      final String boxingPrefix = maybeBoxPrefix(logger, param, false, out, cls, helpers);
      final String boxingSuffix = maybeBoxSuffix(logger, param, false, out);
      boxers.put(paramName, boxingPrefix + paramName + boxingSuffix);
      paramName++;
    }
    out
    .println(");")
    .println("return $entry(function(" + params + "){")
    .indent();
    final String boxReturnPrefix = maybeBoxPrefix(logger, method.getReturnType(), true, out, cls, helpers);
    final String boxReturnSuffix = maybeBoxSuffix(logger, method.getReturnType(), true, out);
    final boolean hasReturn = method.getReturnType() != JPrimitiveType.VOID;
    if (hasReturn) {
      // Non void return type; we may need to box/unbox the result
      out.print("var ret = " + boxReturnPrefix);
    }
    if (hasReturn) {
      out.indent();
    }
    out
    .println("func(this");
    for (final Character c : boxers.keySet()) {
      BuiltInType type = data.type;
      if (type == null) {
        type = BuiltInType.attributeChanged;
      }
      out.print(", ");
      switch (type) {
      case attributeChanged:
        out.println(boxers.get(c));
        break;
      default:
        // for onCreated, onAttached and onDetached, we supply this reference
        // as the element argument to the method, as a convenience
        out.println("this");
        break;
      }
    }
    out.print(")");
    if (hasReturn) {
      out.println().outdent().print(boxReturnSuffix);
    }
    out.println(";");
    if (hasReturn) {
      out.println("return ret;");
    }
    out
    .outdent()
    .println("});");
  }

  private void generateFunctionAccessors(final TreeLogger logger,
    final GeneratorContext context, final JClassType iface,
    final Multimap<String, MethodData> results, final boolean hasCallbacks) throws UnableToCompleteException {
    if (iface.getMethods().length == 0) {
      return;
    }

    final JParameterizedType asParam = iface.isParameterized();
    final JClassType[] paramTypes = asParam == null ? new JClassType[0] : asParam.getTypeArgs();

    final String pkg = iface.getPackage().getName();
    final String simple = iface.getQualifiedSourceName().replace(pkg + ".", "");
    final String result = simple.replace('.', '_') + simplify(paramTypes) + "_JsFunctionAccess";
    final String name = iface.getQualifiedSourceName();
    final String qualified = pkg + "." + result;

    SourceBuilder<PrintWriter> source = null;
    // TODO reenable once we can check input source files for freshness
    // if (!context.tryReuseTypeFromCache(qualified)) {
    final PrintWriter pw = context.tryCreate(logger, pkg, result);
    if (pw != null) {
      source = new SourceBuilder<PrintWriter>
      ("public final class " + result)
      .setPackage(pkg)
      .setPayload(pw);
      source.getClassBuffer().createConstructor(PRIVATE);
    }
    final Set<String> helpers = new HashSet<String>();
    for (final JMethod method : iface.getMethods()) {
      if (method.isStatic()) {
        continue;
      }
      if (method.getAnnotation(NativelySupported.class) != null) {
        // Do not blow away natively supported methods!
        continue;
      }
      if (method.getEnclosingType() == iface) {
        Collection<MethodData> existing = results.get(name);
        MethodData data = new MethodData(accessorName(method), method.getName());
        if (hasCallbacks) {
          data.type = BuiltInType.find(method);
        }
        final WebComponentMethod metaData =
          method.getAnnotation(WebComponentMethod.class);
        if (metaData != null) {
          if (!metaData.name().isEmpty()) {
            data.name = metaData.name();
          }
          data.useJsniWildcard = metaData.useJsniWildcard();
          data.mapToAttribute = metaData.mapToAttribute();
          data.configurable = metaData.configurable();
          data.enumerable = metaData.enumerable();
          data.writeable = metaData.writeable();
        }
        if (method.isDefaultMethod()) {
          data.valueClass = qualified;
          existing = Sets.add((Set<MethodData>) existing, data);
          results.putAll(name, existing);
          // A default method! Let's generate a method to extract a javascript
          // function that will correctly handle un/boxing when passing values.
          if (source != null) {
            generateDefaultFunctionAccessor(logger, method, data, source.getClassBuffer(), helpers);
          }
        } else {
          // An abstract method should be treated like a JsType method;
          // if it's a getter or a setter, try to use element attributes
          final String debeaned = debean(method.getName());
          if (metaData == null || metaData.name().isEmpty()) {
            data.name = debeaned;
          }
          for (final MethodData previous : results.values()) {
            if (previous.name.equals(data.name)) {
              if (previous.isProperty()) {
                data = previous;
              } else {
                logger.log(Type.ERROR, "Duplicate property definitions found for web component member with "
                  + "name [" + previous.name + "].  Conflict between " + previous.accessorName + " and "
                  + data.accessorName);
                throw new UnableToCompleteException();
              }
            }
          }
          final JsProperty prop = method.getAnnotation(JsProperty.class);
          if (prop != null || isBeanFormat(method)) {
            // Explicitly a js property, or it looks like a bean. Lets wire it
            // up!
            if (method.getParameterTypes().length == 0) {
              // Getter
              data.getterName = "get_" + method.getName();
              data.getterClass = qualified;
              if (source != null) {
                generateGetter(logger, debeaned, data, method, source, helpers);
              }
            } else {
              // Setter
              data.setterName = "set_" + method.getName();
              data.setterClass = qualified;
              if (source != null) {
                generateSetter(logger, debeaned, data, method, source, helpers);
              }
            }
            existing = Sets.add((Set<MethodData>) existing, data);
            results.putAll(name, existing);
          } else {
            logger.log(Type.WARN, "Unable to generate web component implementation for "
              + method.getReadableDeclaration() +
              " of " + method.getEnclosingType().getQualifiedSourceName()
              + ".  The underlying method will only work correctly "
              + "if supplied by the underlying native element");
          }
        }
      }
    }
    if (source != null) {
      final String src = source.toString();
      source.getPayload().println(src);
      logger.log(logLevel(iface.getQualifiedSourceName()), src);
      context.commit(logger, source.getPayload());
    }

  }

  private void generateGetter(final TreeLogger logger, final String debeaned, final MethodData data, final JMethod method,
    final SourceBuilder<PrintWriter> source, final Set<String> helpers) {
    final MethodBuffer out = source.getClassBuffer()
      .createMethod("public static JavaScriptObject get_" + method.getName())
      .addImports(JavaScriptObject.class)
      .makeJsni();
    final String boxingPrefix = maybeBoxPrefix(logger, method.getReturnType(), true, out, source.getClassBuffer(), helpers);
    final String boxingSuffix = maybeBoxSuffix(logger, method.getReturnType(), true, out);
    out
    .println("return function() {")
    .indent()
    .print("return ");
    if (data.mapToAttribute) {
      out.print(boxingPrefix)
      // our boxing code will automatically handle primitive conversion
      .print("this.getAttribute('" + debeaned + "')")
      .print(boxingSuffix);
    } else {
      out.print(boxingPrefix)
      .print("this.__" + debeaned)
      .print(boxingSuffix);
    }
    out
    .println(";")
    .outdent()
    .println("}");

  }

  private String generatePrototypeAccessor(final ClassBuffer out,
    final String[] extendProto,
    final String jso) {
    out
    .createMethod("private static native " + jso + " proto()")
    .setUseJsni(true)
    .println("return Object.create(" + extendProto[0] + ".prototype);");
    return "proto()";
  }

  private void generateSetter(final TreeLogger logger, final String debeaned, final MethodData data, final JMethod method,
    final SourceBuilder<PrintWriter> source, final Set<String> helpers) {
    final MethodBuffer out = source.getClassBuffer()
      .createMethod("public static JavaScriptObject set_" + method.getName())
      .addImports(JavaScriptObject.class)
      .makeJsni();
    final String boxingPrefix = maybeBoxPrefix(logger, method.getParameterTypes()[0], false, out, source.getClassBuffer(), helpers);
    final String boxingSuffix = maybeBoxSuffix(logger, method.getParameterTypes()[0], false, out);
    final boolean fluent = method.getReturnType() != JPrimitiveType.VOID;
    assert !fluent || isAssignableFrom(method.getReturnType(), method.getEnclosingType()) : "Cannot implement fluent method "
    + method.getJsniSignature();
    out
    .println("return function(i) {")
    .indent();
    if (data.mapToAttribute) {
      out
      .println("var val = i == null ? null : "+boxingPrefix + "i" + boxingSuffix+";")
      .println("if (val == null) {")
      .indentln("this.removeAttribute('"+debeaned+"');")
      .println("} else {")
      .indentln("this.setAttribute('" + debeaned + "', val);")
      .println("}");
    } else {
      out
      .print("this.__").print(debeaned)
      .print(" = i == null ? null : ")
      .print(boxingPrefix).print("i").print(boxingSuffix)
      .println(";");
    }

    if (fluent) {
      out.println("return this;");
    }

    out
    .outdent()
    .println("}");

  }

  private boolean hasCssAnnotations(final JClassType type) {
    if (hasStyleAnnotations(type)) {
      return true;
    }
    final Html html = type.getAnnotation(Html.class);
    if (html != null) {
      if (html.css().length > 0) {
        return true;
      }
      for (final El el : html.body()) {
        if (hasStyle(el)) {
          return true;
        }
      }
    }
    final El el = type.getAnnotation(El.class);
    if (hasStyle(el)) {
      return true;
    }
    for (final JMethod method : type.getMethods()) {
      if (hasStyle(method.getAnnotation(El.class))) {
        return true;
      }
      if (hasStyleAnnotations(method)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasCssToInject(final JClassType type) {
    for (final JClassType subtype : type.getFlattenedSupertypeHierarchy()) {
      if (hasCssAnnotations(subtype)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasStyle(final El el) {
    return el != null && el.style().length > 0;
  }

  private boolean hasStyleAnnotations(final HasAnnotations member) {
    return member.isAnnotationPresent(Css.class) || member.isAnnotationPresent(Style.class);
  }

  private boolean isAssignableFrom(final JType returnType, final JClassType enclosingType) {
    return returnType instanceof JClassType ? enclosingType.isAssignableFrom((JClassType) returnType) : false;
  }
  private boolean isBeanFormat(final JMethod method) {
    if (method.getParameterTypes().length == 0) {
      // This, if anything, is a getter.
      return method.getReturnType() != JPrimitiveType.VOID;
    }
    return method.getParameterTypes().length == 1;
  }

  private String maybeBoxPrefix(final TreeLogger logger, final JType type, final boolean jsToJava, final MethodBuffer out, final ClassBuffer enclosing, final Set<String> helpers) {
    if (type.isPrimitive() == null) {
      // The type is not primitive. If it maps to the object form of a
      // primitive, we must box it if primitive
      switch (type.getQualifiedSourceName()) {
      case "elemental.js.util.JsArrayOfString":
        if (jsToJava) {
          return BOX_HELPER + "unboxArrayOfString("
            + JSO_PARAM + "Ljava/lang/String;)(";
        } else {
          return BOX_HELPER + "boxArray("
            + JSO_PARAM + "Ljava/lang/String;)(";
        }
      case "elemental.js.util.JsArrayOfInt":
        if (jsToJava) {
          return BOX_HELPER + "unboxArrayOfInt("
            + JSO_PARAM + "Ljava/lang/String;)(";
        } else {
          return BOX_HELPER + "boxArray("
            + JSO_PARAM + "Ljava/lang/String;)(";
        }
      case "elemental.js.util.JsArrayOfNumber":
        if (jsToJava) {
          return BOX_HELPER + "unboxArrayOfNumber("
            + JSO_PARAM + "Ljava/lang/String;)(";
        } else {
          return BOX_HELPER + "boxArray("
            + JSO_PARAM + "Ljava/lang/String;)(";
        }
      case "elemental.js.util.JsArrayOfBoolean":
        if (jsToJava) {
          return BOX_HELPER + "unboxArrayOfNumber("
            + JSO_PARAM + "Ljava/lang/String;)(";
        } else {
          return BOX_HELPER + "boxArray("
            + JSO_PARAM + "Ljava/lang/String;)(";
        }
      case "java.lang.Long":
      case "java.lang.Boolean":
      case "java.lang.Byte":
      case "java.lang.Short":
      case "java.lang.Character":
      case "java.lang.Integer":
      case "java.lang.Float":
      case "java.lang.Double":
        return BOX_HELPER + "box" + simpleName(type) + "("
        + JSO_PARAM + ")(";
      default:
        if (type instanceof JClassType) {
          final JClassType asClass = ((JClassType) type).getErasedType();
          try {
            if (jsToJava) {
              JMethod valueOf;
              try {
                // Prefer fromString(String) as this allows enums to override .toString()
                // fromString is also preferable as it better matches .toString()
                valueOf = asClass.getMethod("fromString", new JType[] { stringType });
              } catch (final NotFoundException e) {
                // Also accept enum default method, valueOf
                valueOf = asClass.getMethod("valueOf", new JType[] { stringType });
              }
              // In order to guard against null/empty values being sent to these methods,
              // we will actually generate a helper method to perform the empty-value check,
              // and simply return null if the appropriate value is not sent.
              // If you want to handle empty values, use an empty string, which will still be
              // passed into the associated methods.
              final String typeName = enclosing.addImport(asClass.getQualifiedSourceName());
              final String methodName = asClass.getSimpleSourceName() +"_"+valueOf.getName()+"_Helper";

              if (helpers.add(methodName)) {

                final MethodBuffer helper = enclosing.createMethod
                  ("private static "+typeName+" "+methodName+"(String value)")
                  .makeJsni()
                  .print("return value == null ? null : ");

                if (valueOf.isStatic()) {
                  // if valueOf is static, we can just invoke it directly
                  helper.println("@" + asClass.getQualifiedSourceName() + "::"+valueOf.getName()+"(Ljava/lang/String;)(value);");
                } else {
                  // however, if fromString is instance level, we must construct a
                  // new instance
                  if (asClass.getConstructor(new JType[0]) == null) {
                    logger.log(
                      Type.WARN,
                      "Found method "+valueOf.getName()+"(String) in type "
                        + asClass.getQualifiedSourceName()
                        + ",  but could not use it for autoboxing because the method is "
                        + "instance level and there is no zero-arg constructor available "
                        + "to instantiate the given type");
                    helper.println("null;");
                    return "";
                  } else {
                    // we only support 0-arg constructors here
                    helper.println("@" + asClass.getQualifiedSourceName() + "::new()().@"
                      + asClass.getQualifiedSourceName() + "::"+valueOf.getName()+"(Ljava/lang/String;)(value)");
                  }
                }
              }
              return "@" + enclosing.getQualifiedName()+"::"+methodName+"(Ljava/lang/String;)(";
            }
          } catch (final NotFoundException ignored) {
            // If there is no valueOf(String) method,
            // we just don't perform any boxing.
          }
        }
      }
    } else {
      // The type is primitive, we must unbox whatever is given to us
      switch (type.isPrimitive()) {
      case BOOLEAN:
        return BOX_HELPER + "unboxBoolean(" + JSO_PARAM + ")(";
      case BYTE:
        return BOX_HELPER + "unboxByte(" + JSO_PARAM + ")(";
      case SHORT:
        return BOX_HELPER + "unboxShort(" + JSO_PARAM + ")(";
      case CHAR:
        return BOX_HELPER + "unboxCharacter(" + JSO_PARAM + ")(";
      case INT:
        return BOX_HELPER + "unboxInteger(" + JSO_PARAM + ")(";
      case LONG:
        out.addAnnotation(UnsafeNativeLong.class);
        if (jsToJava) {
          return BOX_HELPER + "unboxLongNative(" + JSO_PARAM + ")(";
        }
        return BOX_HELPER + "unboxLong(" + JSO_PARAM + ")(";
      case FLOAT:
        return BOX_HELPER + "unboxFloat(" + JSO_PARAM + ")(";
      case DOUBLE:
        return BOX_HELPER + "unboxDouble(" + JSO_PARAM + ")(";
      default:
      }
    }
    return "";
  }

  private String maybeBoxSuffix(final TreeLogger logger, final JType type, final boolean jsToJava, final MethodBuffer out) {
    if (type.isPrimitive() == null) {
      // The type is not primitive. If it maps to the object form of a
      // primitive, we must box it if primitive
      switch (type.getQualifiedSourceName()) {
      case "elemental.js.util.JsArrayOfString":
      case "elemental.js.util.JsArrayOfInt":
      case "elemental.js.util.JsArrayOfNumber":
      case "elemental.js.util.JsArrayOfBoolean":
        if (jsToJava) {
          return ", this.joiner)";
        } else {
          return ", i && i.joiner)";
        }

      case "java.lang.Long":
      case "java.lang.Boolean":
      case "java.lang.Byte":
      case "java.lang.Short":
      case "java.lang.Character":
      case "java.lang.Integer":
      case "java.lang.Float":
      case "java.lang.Double":
        return ")";
      default:
        if (type instanceof JClassType) {
          final JClassType asClass = ((JClassType) type).getErasedType();
          try {
            boolean hasFromString = false;
            final boolean hasValueOf = false;
            try {
              asClass.getMethod("fromString", new JType[] { stringType });
              hasFromString = true;
            } catch (final NotFoundException ignored) {}
            if (jsToJava) {
              if (hasFromString) {
                return ")";
              }
              asClass.getMethod("valueOf", new JType[] { stringType });
              return ")";
            } else {
              JMethod name;
              if (hasFromString) {
                try {
                  name = asClass.getMethod("toString", new JType[0]);
                } catch (final NotFoundException e) {
                  name = asClass.getMethod("name", new JType[0]);
                }
              } else {
                name = asClass.getMethod("name", new JType[0]);
              }
              if (name.isStatic()) {
                // if name is static, we can't invoke it as a suffix, and, it
                // really should never be static
                logger.log(Type.WARN, "Unable to use method "+name.getName()+"() from " + asClass.getQualifiedSourceName() +
                  " in web component factory");
                return "";
              } else {
                return ".@" + asClass.getQualifiedSourceName() + "::"+name.getName()+"()()";
              }
            }
          } catch (final NotFoundException ignored) {
            // If there is no valueOf(String) method,
            // we just don't perform any boxing.
          }
        }
      }
    } else {
      // The type is primitive, we must unbox whatever is given to us
      switch (type.isPrimitive()) {
      case BOOLEAN:
      case BYTE:
      case SHORT:
      case CHAR:
      case INT:
      case LONG:
      case FLOAT:
      case DOUBLE:
        return ")";
      default:
      }
    }
    return "";
  }

  private void printPropertyAccess(final TreeLogger logger, final ClassBuffer staticOut, final MethodData method, final String builder, final Set<String> seen) {
    String name = method.name;
    final String constName = "CONST_"+name.toUpperCase();

    if (seen.add(constName)) {
      staticOut.createField(String.class, constName)
      .makeStatic()
      .makeFinal()
      .makePrivate()
      .setInitializer("\"" + method.name + "\"");
    }

    name = "applyProperty_"+method.name;
    final MethodBuffer out = staticOut.createMethod("private static " + builder + " " + name)
      .addParameters(builder+" builder");
    staticOut.println(name+"(builder);");
    out
    .print("builder.addProperty(")
    .println(constName+", ");
    if (method.getterName != null) {
      final String jsoSupplier = staticOut.addImport(JsoSupplier.class);
      final String cls = staticOut.addImport(method.getterClass);
      out.println("new " + jsoSupplier + "(" + cls + "." + method.getterName + "()), ");
    } else {
      out.println("null, ");
    }
    if (method.setterName != null) {
      final String jsoConsumer = staticOut.addImport(JsoConsumer.class);
      final String cls = staticOut.addImport(method.setterClass);
      out.println("new " + jsoConsumer + "(" + cls + "." + method.setterName + "()), ");
    } else {
      out.println("null, ");
    }
    out
    .print(method.enumerable + ", ")
    .println(method.configurable + ");");
    out.returnValue("builder");
  }

  private void printValueAccess(final TreeLogger logger, final ClassBuffer staticOut, final MethodData method, final String builder, final Set<String> seen) {

    String name = method.name;
    final String constName = "CONST_"+name.toUpperCase();

    if (seen.add(constName)) {
      staticOut.createField(String.class, constName)
      .makeStatic()
      .makeFinal()
      .makePrivate()
      .setInitializer("\"" + method.name + "\"");
    }

    if (!seen.add(name)) {
      if (method.type == null) {
        // built-ins it's ok to have multiples
        logger.log(Type.WARN, "Found duplicate key for "+method.name+" in "+staticOut.getQualifiedName()+"; "
          + "one of these methods will be overridden an discarded.");
        // TODO: fix all warnings and escalate this to a compile-breaking error
      }
      int suffix = 0;
      while (seen.contains(name+suffix)) {
        suffix++;
      }
      name = name+suffix;
      seen.add(name);
    }
    name = "applyValue_"+name;
    final MethodBuffer out = staticOut.createMethod("private static " + builder + " " + name)
      .addParameters(builder+" builder");
    staticOut.println(name+"(builder);");


    final String shortName = out.addImport(method.valueClass);
    if (method.type == null) {
      out
      .print("builder.addValue(")
      .print(constName+", ")
      .print(shortName + "." + method.accessorName + "(null),")
      .print(method.enumerable + ", ")
      .print(method.configurable + ", ")
      .println(method.writeable + ");");
    } else {
      // This is a built-in type. We should attach the correct callback.
      out
      .print("builder."+method.type.name() + "Callback(")
      .print(shortName + "." + method.accessorName + "(null)")
      .println(");");
    }
    out.returnValue("builder");
  }

  private String simpleName(final JType type) {
    String binary = type.getQualifiedBinaryName();
    final int last = binary.lastIndexOf('.');
    if (last != -1) {
      binary = binary.substring(last + 1);
    }
    return binary.replace('$', '.');
  }

  private String simplify(final JClassType[] typeParams) {
    final StringBuilder b = new StringBuilder();
    for (int i = 0; i < typeParams.length; i++) {
      if (i > 0) {
        b.append("_");
      }
      final JClassType typeParam = typeParams[i];
      final String pkg = typeParam.getPackage().getName();
      for (final String chunk : pkg.split("[.]")) {
        b.append(chunk.charAt(0)).append('_');
      }
      b.append(typeParam.getQualifiedSourceName().replace(pkg+".", "").replace('.', '_'));
    }
    return b.toString();
  }

}
