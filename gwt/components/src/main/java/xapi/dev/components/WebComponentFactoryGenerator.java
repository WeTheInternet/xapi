package xapi.dev.components;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import jsinterop.annotations.JsProperty;
import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.components.api.*;
import xapi.components.impl.JsFunctionSupport;
import xapi.components.impl.JsSupport;
import xapi.components.impl.WebComponentBuilder;
import xapi.components.impl.WebComponentSupport;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.SourceTransform;
import xapi.dev.components.graveyard.OldContainerMetadata;
import xapi.dev.ui.api.MetadataRoot;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.source.X_Source;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.Style;

import static java.lang.reflect.Modifier.PRIVATE;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.*;
import com.google.gwt.dev.util.collect.Sets;
import com.google.gwt.thirdparty.guava.common.collect.LinkedHashMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebComponentFactoryGenerator extends IncrementalGenerator {

  private static ThreadLocal<ShadowDomStyleInjectorGenerator> shadowDomGenerator = new ThreadLocal<ShadowDomStyleInjectorGenerator>() {
    @Override
    protected ShadowDomStyleInjectorGenerator initialValue() {
      return new ShadowDomStyleInjectorGenerator();
    }
  };

  static ShadowDomStyleInjectorGenerator getStyleInjectorGenerator() {
    return shadowDomGenerator.get();
  }

  private enum BuiltInType {
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
    public JType[] types;

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
    final Fifo<ShadowDomStyle> styles = extractSharedStyles(type);
    if (component == null) {
      logger.log(Type.ERROR, "Type " + type.getQualifiedSourceName()
        + " missing required annotation, " + WebComponent.class.getName());
      throw new UnableToCompleteException();
    }
    if (component.tagName().indexOf('-') == -1) {
      logger.log(Type.ERROR,
        "WebComponent for " + type.getQualifiedSourceName()
        + " has invalid tag name " + component.tagName() + "; "
        + "Custom elements must contain the - character");
      throw new UnableToCompleteException();
    }
    final String pkg = type.getPackage().getName();
    final String simple = type.getQualifiedSourceName().replace(pkg + ".", "");
    final String factoryName = toFactoryName(simple);
    final String qualifiedName = pkg + "." + factoryName;

    // TODO reenable this once we add strong hashing support to ensure types
    // have not changed.
    // if (context.tryReuseTypeFromCache(qualifiedName)) {
    // return new RebindResult(RebindMode.USE_ALL_CACHED, qualifiedName);
    // }

    final PrintWriter pw = context.tryCreate(logger, pkg, factoryName);
    if (pw == null) {
      logger.log(logLevel(), "Reusing existing class " + qualifiedName);
      return new RebindResult(RebindMode.USE_EXISTING, qualifiedName);
    }

    final SourceBuilder<OldContainerMetadata> sourceBuilder = new SourceBuilder<OldContainerMetadata>
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

    final Multimap<String, MethodData> methods = LinkedHashMultimap.create();
    final boolean hasCallbacks = type.isAssignableTo(webComponentCallback);
    final List<JClassType> flattened = new ArrayList<JClassType>(type.getFlattenedSupertypeHierarchy());
    for (int i = flattened.size(); i --> 0; ) {
      final JClassType iface = flattened.get(i);
      generateFunctionAccessors(logger, context, iface, methods, hasCallbacks, type);
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

    final String inject = out.addImport(X_Inject.class);
    for (Class<? extends ShadowDomPlugin> pluginClass : component.plugins()) {
      String plugin = out.addImport(pluginClass);

      out
          .println("builder.addShadowDomPlugin(")
          .indent()
          .print(inject)
          .print(isSingleton(pluginClass) ? ".instance" : ".singleton")
          .println("(" + plugin +".class)")
          .outdent()
          .println(");");
    }

    ShadowDom[] shadowDoms = component.shadowDom();
    if (shadowDoms.length > 0) {
      for (ShadowDom shadowDom : shadowDoms) {
        // Calculate if we need to do any css injection.
        SourceTransform shadowStyle = null;
        if (shadowDom.styles().length > 0 || styles.isNotEmpty()) {
          final Fifo<ShadowDomStyle> localStyles = new SimpleFifo<>();
          localStyles.giveAll(shadowDom.styles());
          shadowStyle = getStyleInjectorGenerator().generateShadowStyles(logger, styles, localStyles, context);
        }

        for (String template : shadowDom.value()) {
          // TODO: this is broken; the metadata no longer holds the source builder;
          // this implementation is deprecated,
          // as we now generate our components before GWT is compiled.
          OldContainerMetadata metadata = createMetadata();
          if (shadowStyle != null) {
            metadata.addModifier(shadowStyle);
          }
          metadata.setSource(()->sourceBuilder);
          template = resolveTemplate(logger, template, context, type, metadata);
          out
                .print("builder.addShadowRoot(\"")
                    .print(Generator.escape(template))
                .print("\"")
                .println()
                .println(", (host, shadow) -> {")
                .indent();

          metadata.applyModifiers(out, "shadow");

          out
              .println("return shadow;")
              .outdent()
              .println("});"); // just close out the shadowroot call
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

    // Print the querySelector method.
    final MethodBuffer querySelector = out.createMethod("public String querySelector()");
    if (component.extendProto().length > 1) {
      // We are extending an existing method.
      querySelector.returnValue("\""+component.extendProto()[1]+"[is="+component.tagName()+"]\"");
    } else {
      querySelector.returnValue("\""+component.tagName()+"\"");
    }

    final String src = sourceBuilder.toString();
    logger.log(logLevel(typeName), "\nWeb Component Factory: \n" + src);

    pw.println(src);
    context.commit(logger, pw);

    return new RebindResult(RebindMode.USE_ALL_NEW, qualifiedName);
  }

  protected OldContainerMetadata createMetadata() {
      final OldContainerMetadata container = new OldContainerMetadata();
      final MetadataRoot root = new MetadataRoot();
      container.setRoot(root);
    return container;
  }

  public static String toFactoryName(String simple) {
    return simple.replace('.', '_') + "_WebComponentFactory";
  }

  private Type logLevel() {
    return Type.TRACE;
  }

  protected boolean isSingleton(Class<? extends ShadowDomPlugin> pluginClass) {
    return
        pluginClass.getAnnotation(Singleton.class) != null ||
        pluginClass.getAnnotation(SingletonOverride.class) != null ||
        pluginClass.getAnnotation(SingletonDefault.class) != null
    ;
  }

  private Fifo<ShadowDomStyle> extractSharedStyles(JClassType type) {
    final Fifo<ShadowDomStyle> list = new SimpleFifo<>();

    final ShadowDomStyles styles = type.getAnnotation(ShadowDomStyles.class);
    if (styles != null) {
      list.giveAll(styles.value());
    }
    final ShadowDomStyle style = type.getAnnotation(ShadowDomStyle.class);
    if (style != null) {
      list.giveAll(style);
    }
    return list;
  }

  private String resolveTemplate(TreeLogger logger, String template, GeneratorContext context, JClassType type, OldContainerMetadata metadata) throws UnableToCompleteException {
    String asString;
    boolean wasHtml = false;
    if (template.trim().startsWith("<")) {
      // raw html / xapi template.
      asString = template;
    } else {
      // resource to load
        if (!template.startsWith("/")) {
          // Relative resource
          template = "/"+type.getPackage().getName().replace('.', '/')+"/"+template;
          wasHtml = template.endsWith(".html");
        }
        try (
            InputStream resource = context.getResourcesOracle().getResourceAsStream(template.substring(1))
        ) {
          if (resource == null) {
            logger.log(Type.ERROR, "Unable to find shadow root bundle "+template);
            throw new UnableToCompleteException();
          }
          asString = X_IO.toStringUtf8(resource);

        } catch (IOException e) {
          logger.log(Type.ERROR, "Error generating shadow root bundle "+template, e);
          throw new UnableToCompleteException();
        }
    }

    try {
      final UiContainerExpr container = JavaParser.parseUiContainer(asString);
      // The template parsed.
      // We are going to need to transform it into a few different forms:
      // 1) The actual html to inject, which we return
      // 2) Any stylesheets or message bundles generated by the template
      // 3) A transform function that can operate on the shadow dom element at runtime.

      metadata.setContainer(container);
      metadata.setType(type.getErasedType().getQualifiedSourceName());
      for (JClassType iface : type.getImplementedInterfaces()) {
        if (iface.getQualifiedSourceName().equals(IsWebComponent.class.getCanonicalName())) {
          final JClassType elementType = iface.isParameterized().getTypeArgs()[0];
          metadata.setElementType(elementType.getErasedType().getQualifiedSourceName());
        } else if (iface.getQualifiedSourceName().equals(IsControlledComponent.class.getCanonicalName())) {
          final JClassType elementType = iface.isParameterized().getTypeArgs()[0];
          final JClassType componentType = iface.isParameterized().getTypeArgs()[1];
          final JClassType controllerType = iface.isParameterized().getTypeArgs()[2];
          metadata.setElementType(elementType.getErasedType().getQualifiedSourceName());
          metadata.setComponentType(componentType.getErasedType().getQualifiedSourceName());
          final JClassType erased = controllerType.getErasedType();
          final JPackage pkg = erased.getPackage();
          String pkgName = pkg == null ? "" : pkg.getName();
          String clsName = X_Source.removePackage(pkgName, erased.getQualifiedSourceName());
        }
      }

      // replace the string version with a modified one that is safe to inject.
      MethodReferenceReplacementVisitor.mutateExpression(container, metadata);

      return generateComponentBinding(logger, context, type, metadata, container);


    } catch (ParseException e) {
      if (!wasHtml && metadata.isAllowedToFail()) {
        X_Log.warn(getClass(), "Unable to parse html as xapi UI.  Treating template as raw html. Set -Dxapi.log.level=TRACE to see the template");
        X_Log.trace(asString);
        if (X_Log.loggable(LogLevel.TRACE)) {
          if (!asString.equals(template)) {
            X_Log.trace("From template", template);
          }
        }
        X_Log.warn(e);
        return asString;
      } else {
        logger.log(Type.ERROR, "Unparseable xapi template:");
        if (!asString.equals(template)) {
          logger.log(Type.ERROR, "loaded from " + template);
        }
        logger.log(Type.ERROR, asString, e);
        throw new UnableToCompleteException();
      }
    }
  }

  protected String generateComponentBinding(
      TreeLogger logger,
      GeneratorContext context,
      JClassType type,
      OldContainerMetadata metadata,
      UiContainerExpr container
  ) {



    return container.toSource();
  }

  @Override
  public long getVersionId() {
    return 0;
  }

  protected Type logLevel(final String typeName) {
    return
      //        BooleanPickerElement.class.getName().equals(typeName) ? Type.INFO : Type.TRACE;
      typeName.contains("Button") ? Type.INFO : Type.DEBUG;
//       Type.DEBUG;
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

  private void generateDefaultFunctionAccessor(
      final TreeLogger logger,
      final JMethod method, final MethodData data, final ClassBuffer cls, final Set<String> helpers, JClassType rootType
  ) {
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
    List<JType> rawParams = new ArrayList<>();
    for (JType param : method.getParameterTypes()) {
      if (param instanceof JParameterizedType) {
        param = ((JParameterizedType)param).getRawType();
      }
      if (param instanceof JTypeParameter) {
        param = ((JTypeParameter)param).getErasedType();
      }
      rawParams.add(param);
    }
    if (data.useJsniWildcard) {
      out.print("*");
    } else {
      for (JType param : rawParams) {
        out.print(param.getJNISignature());
      }
    }
    final StringBuilder params = new StringBuilder();
    final Map<Character, String> boxers = new LinkedHashMap<>();
    char paramName = 'a';
    for (final JType param : rawParams) {
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
    .print("return @")
    .print(JsFunctionSupport.class.getName())
    .print("::maybeEnter(*)")
    .println("(function(" + params + "){")
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

  private void generateFunctionAccessors(
      final TreeLogger logger,
      final GeneratorContext context, final JClassType iface,
      final Multimap<String, MethodData> results, final boolean hasCallbacks, JClassType type
  ) throws UnableToCompleteException {
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
    final Set<String> helpers = new LinkedHashSet<>();
    methods:
    for (JMethod method : iface.getMethods()) {
      if (method.isStatic()) {
        continue;
      }
      if (method.getAnnotation(NativelySupported.class) != null) {
        // Do not blow away natively supported methods!
        continue;
      }
      if (method.getEnclosingType() != iface) {
        logger.log(Type.WARN, "Skipping method with bad enclosing type; " + method.getEnclosingType() + " != " + iface);
      } else {
        // Check for signature overrides
        boolean overloaded = false;
        for (MethodData methodData : results.values()) {
          if (method.getName().equals(methodData.name)) {
            if (Arrays.equals(methodData.types, method.getParameterTypes())) {
              // already handled an override of this method; skip it...
              logger.log(TreeLogger.TRACE, "Skipping overload of " + method.getJsniSignature() + "; preferring " + methodData.accessorName );
//              continue methods;
            } else {
              // There is polymorphism on this method name;
              // we are going to have to type match javascript supplied names.
              // Beware non-array-wrapped varargs; you should be sending js[]s
              logger.log(TreeLogger.WARN, "Method signature overloading " +
                  "between " + method.getJsniSignature() + " and; generating both accessors, " +
                      "but you may have choice conflict in which method is mapped to javascript");
              // TODO: handle polymorphic method signatures via a trampoline method
              overloaded = true;
//              continue methods;

            }
          }
        }

        if (type != iface)
        try {
          final JMethod overridden = type.getMethod(method.getName(), method.getParameterTypes());
          // If the root type overrides this interface method, prefer the root type...
          logger.log(TreeLogger.ERROR, "Overridden method " + overridden.getJsniSignature() + " != " + method.getJsniSignature());
          method = overridden;
        } catch (NotFoundException ignored) {
        }

        Collection<MethodData> existing = results.get(name);
        MethodData data = new MethodData(accessorName(method), method.getName());
        if (hasCallbacks) {
          data.type = BuiltInType.find(method);
          data.useJsniWildcard = true;
        }
        data.types = method.getParameterTypes();
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
          existing = Sets.add(new LinkedHashSet<>(existing), data);
          results.putAll(name, existing);
          // A default method! Let's generate a method to extract a javascript
          // function that will correctly handle un/boxing when passing values.
          if (source != null) {
            generateDefaultFunctionAccessor(logger, method, data, source.getClassBuffer(), helpers, type);
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
            existing = Sets.add(new LinkedHashSet<>(existing), data);
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
          + "one of these methods will be overridden and discarded.");
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
