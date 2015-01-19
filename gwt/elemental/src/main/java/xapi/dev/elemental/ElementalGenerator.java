/**
 *
 */
package xapi.dev.elemental;

import static xapi.util.X_String.join;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xapi.collect.impl.AbstractMultiInitMap;
import xapi.dev.elemental.ElementalGeneratorContext.ElementalGeneratorResult;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.ui.html.AbstractHtmlGenerator;
import xapi.dev.ui.html.HtmlGeneratorNode;
import xapi.elemental.api.ElementalService;
import xapi.elemental.api.PotentialNode;
import xapi.elemental.impl.LazyHtmlConverter;
import xapi.elemental.impl.LexerForMarkup;
import xapi.source.X_Source;
import xapi.time.impl.RunOnce;
import xapi.ui.html.X_Html;
import xapi.ui.api.Widget;
import xapi.ui.html.api.El;
import xapi.ui.html.api.HtmlSnippet;
import xapi.ui.html.api.HtmlTemplate;
import xapi.ui.html.api.NoUi;
import xapi.ui.html.api.Style;
import xapi.util.X_Debug;
import xapi.util.api.ConvertsValue;
import xapi.util.api.MergesValues;
import xapi.util.api.ReceivesValue;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dev.jjs.UnifyAstView;

import elemental.dom.Element;

/**
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class ElementalGenerator extends AbstractHtmlGenerator<ElementalGeneratorResult> {

  protected static class GeneratedState {
    boolean needsServiceField;
    public String serviceType;
  }
  private static final String KEY_ELEMENT = "_el";
  private static final String SUFFIX_MODEL_TO_ELEMENT = "__toHtml";
  public static final String FIELD_STYLIZE = "__stylize";
  private static final String METHOD_TO_POTENTIAL = "_serialize";
  public static final String METHOD_IMPORT = "__import";
  private static final Pattern PATTERN_VALUE = Pattern.compile(HtmlTemplate.KEY_VALUE.replace("$", "[$]"));
  private static final Pattern PATTERN_REFERENCE = Pattern.compile(
    "(?:@)(([a-zA-Z][a-zA-Z0-9_$]*[.]?)+)");
  protected static final String KEY_SERVICE = "_elemental";

  private final ElementalGeneratorContext ctx;
  private final TreeLogger logger;

  /**
   * @param logger
   * @param clsName
   * @param templateType
   * @param ast
   * @param ctx
   */
  public ElementalGenerator(TreeLogger logger, String clsName,
      JClassType templateType,
      UnifyAstView ast, ElementalGeneratorContext ctx) {
    super(clsName, templateType, ast);
    this.logger = logger;
    this.ctx = ctx;
  }

  /**
   * @param logger
   * @param modelType
   * @param ast
   * @param ctx
   * @param enclosingMethod
   * @return
   * @throws UnableToCompleteException
   */
  public static ElementalGeneratorResult generateProvider(
      TreeLogger logger,
      ModelProvider model,
      JClassType templateType,
      UnifyAstView ast,
      ElementalGeneratorContext ctx) throws UnableToCompleteException {

    String implName = toImplName(logger, model.getModelPackage(), model.getModelQualifiedName(), templateType.getQualifiedSourceName());
    ElementalGeneratorResult existing = ctx.findExistingProvider(implName);
    if (existing != null) {
      return existing;
    }
    ElementalGenerator gen = new ElementalGenerator(logger, implName, templateType, ast, ctx);

    return gen.generate(logger, model, templateType, ast);
  }


  private ElementalGeneratorResult generate(TreeLogger logger, ModelProvider model, JClassType templateType, UnifyAstView ast) throws UnableToCompleteException {

    boolean modelIsTemplate = model.getModelQualifiedName().equals(templateType.getQualifiedSourceName());
    String inputHash;
    if (modelIsTemplate) {
      inputHash = toHash(ast, templateType.getQualifiedSourceName());
    } else {
      inputHash = toHash(ast, model.getModelQualifiedName(), templateType.getQualifiedSourceName());
    }
    ElementalGeneratorResult existingType = findExisting(ast, out.getPackage(), out.getQualifiedName());
    existingType.setSourceType(templateType);
    ElementalGeneratorResult existingResult = existingTypesUnchanged(logger, ast, existingType, inputHash);
    // Check if there is an existing type, and that it's generated hashes match our input type.
    // update classname in case there is a stale existing provider
    if (existingResult != null) {
      // If our inputs are unchanged, and the target type exists, just reuse it w/out regenerating
      return existingResult;
    }

    clsName = existingType.getFinalName();

    initialize();

    logger.log(getLogLevel(), "Generating elemental toElement for "+clsName);

    SourceBuilder<UnifyAstView> src = this.out;
    ClassBuffer out = src.getClassBuffer();

    // Add the correct converter interface

    final String
      typeConverter = out.addImport(ConvertsValue.class),
      typeElement = out.addImport(Element.class),
      typeService = out.addImport(ElementalService.class),
      typeModel = out.addImport(model.getModelQualifiedName()),
      typePotentialElement = out.addImport(PotentialNode.class),
      potential = typePotentialElement+"<"+typeElement+">",
      qualifiedConverter = typeConverter +" <"+typeModel+", "+potential+">";

    out.addInterface(qualifiedConverter);
    MethodBuffer convert = out
        .createMethod("public " + potential + " convert(" + typeModel + " from)");

    boolean
      // Create a template for the messages class.
      isTemplateMessages = templateType.isAssignableTo(ctx.getTypeMessages()),
      isTemplateView = templateType.isAssignableTo(ctx.getTypeView()),
      // Create a template for the widget class.
      // Must implement .getElement()
      isTemplateWidget = templateType.isAssignableTo(ctx.getTypeWidget())
    ;
    String templateClass = null;
    if (isTemplateMessages) {
      isTemplateWidget = true; // Using Messages forces the implementation of Widget
    }
    ElementalGeneratorResult templateGen = generateTemplateClass(logger, ast, templateType, modelIsTemplate, inputHash);
    templateClass = out.addImport(templateGen.getFinalName());

    if (isTemplateWidget) {
      assert templateType.isParameterized() == null ||
          templateType.isParameterized().isAssignableTo(ctx.getTypeElement()) :
          "Cannot implement a Widget<E> where E does not implement "
          + "elemental.dom.Node, you sent "+templateType.getParameterizedQualifiedSourceName();
    }


    if (isTemplateView) {
      // Create a template for the view class.
    }

    MethodBuffer doImport = out.createMethod("public static void "+METHOD_IMPORT+"("+typeService+" "+KEY_SERVICE+")");

    if (templateClass == null) {
      convert.returnValue(KEY_SERVICE+".newNode(\"div\");");
    } else {
      convert.returnValue(templateClass + "."
          + SUFFIX_MODEL_TO_ELEMENT + "("
            + KEY_FROM + ", " + KEY_SERVICE + ")");

      doImport.println(templateClass+"."+FIELD_STYLIZE+".set("+KEY_SERVICE+");");
    }

    out.createField(ElementalService.class, KEY_SERVICE);
    out
      .createConstructor(Modifier.PUBLIC, typeService+" service")
      .println("this." + KEY_SERVICE + " = service;");
    // Check the type of the class we are implementing.

    existingType.setTemplateName(templateGen.getFinalName());
    try {
      return saveGeneratedType(logger, getLogLevel(), getClass(), ast, src, existingType, inputHash);
    } finally {
      clear();
    }
  }

  private ElementalGeneratorResult generateTemplateClass(
      TreeLogger logger,
      UnifyAstView ast,
      JClassType type,
      boolean modelIsTemplate,
      String inputHash) throws UnableToCompleteException {
    String templateName =
      X_Source.qualifiedName(type.getPackage().getName(),
          toTemplateName(logger, type));

    // Check for existing types
    ElementalGeneratorResult existing = ctx.findExistingProvider(templateName);
    if (existing != null) {
      return existing;
    }
    inputHash = toHash(ast, type.getQualifiedSourceName());
    final ElementalGeneratorResult
      templateType = findExisting(ast, this, type.getPackage().getName(), templateName),
      existingResult = existingTypesUnchanged(logger, ast, templateType, inputHash);

    // Check if there is an existing type, and that it's generated hashes match our input type.
    if (existingResult != null) {
      // If our inputs are unchanged, and the target type exists, just reuse it w/out regenerating
      return existingResult;
    }

    SourceBuilder<ElementalGeneratorResult> buffer =
      new SourceBuilder<ElementalGeneratorResult>("public class "+templateType.getFinalName())
        .setPackage(type.getPackage().getName())
        .setPayload(templateType);

    return printProviderTemplate(
        logger.branch(Type.DEBUG, "Generating element template for "+type.getName()),
        ast, type, inputHash, buffer);
  }

  protected ElementalGeneratorResult printProviderTemplate(
      TreeLogger logger,
      UnifyAstView ast,
      JClassType templateType,
      String hash,
      SourceBuilder<ElementalGeneratorResult> src) throws UnableToCompleteException {
    ClassBuffer out = src.getClassBuffer();
    ElementalGeneratorResult res = src.getPayload();
    HtmlGeneratorNode root = res.getRoot(templateType, ast.getTypeOracle());
    String
      potential = out.addImport(PotentialNode.class),
      element = out.addImport(Element.class),
      potentialElement = potential + "<" + element + ">",
      classElemental = out.addImport(ElementalService.class),
      classXHtml = out.addImport(X_Html.class),
      classReceiver = out.addImport(ReceivesValue.class),
      receivesElemental = classReceiver + "<"+classElemental+">",
      classRunOnce = out.addImport(RunOnce.class),
      classTemplate = out.addImport(templateType.getQualifiedSourceName());

    GeneratedState generatedState = new GeneratedState();
    generatedState.serviceType = classElemental;

    PrintBuffer stylizeField = out
        .createField(receivesElemental, FIELD_STYLIZE)
        .makePrivate()
        .makeStatic()
        .makeFinal()
        .getInitializer()
          .println(classRunOnce +".setOnce(")
          .indent()
            .println("new "+receivesElemental+"() {")
            .indent()
              .println("public void set("+classElemental+" "+KEY_SERVICE+") {")
              .indent();

    // We tear off a pointer inside the current method
    PrintBuffer styleInit = new PrintBuffer(5);
    styleInit.println(classXHtml+".injectCss("+classTemplate+".class, "+KEY_SERVICE+");");
    stylizeField.addToEnd(styleInit);

    stylizeField .outdent()
              .println("}")
              .outdent()
            .println("}")
            .outdent()
          .print(")");

    String applyStyle = FIELD_STYLIZE+".set("+KEY_SERVICE+");";


    // For implementations of Messages, we just return a single potentialElement
    MethodBuffer toElement = out.createMethod(
        "public static " + potentialElement + " "+ SUFFIX_MODEL_TO_ELEMENT + "(" +
        "final "+classTemplate+" " + KEY_FROM +
        ", "+
        "final "+classElemental+" " + KEY_SERVICE +
        ")")
        .println(applyStyle)
        .println(potentialElement+" "+KEY_ELEMENT+";"
    );

    boolean staticProvider = true;// root.isEmpty();
    if (staticProvider) {
      // Empty root means the simplest possible action.
      // Create a static final LazyElement to supply queries for html
      String elementConverter = out.addImport(ConvertsValue.class)
          +"<" + classTemplate + ", Element>";
      PrintBuffer init = out
          .createField(elementConverter, "_FACTORY_")
          .makePrivate()
          .makeStatic()
          .makeFinal()
          .getInitializer();

      if (root.isDynamic()) {
        init.println("new "+elementConverter+"() {")
          .indent()
          .println("public Element convert("+classTemplate+" "+KEY_FROM+") {")
          .indentln("return "+METHOD_TO_POTENTIAL+ "(" + KEY_FROM + ").getElement();")

          .println("}")
          .outdent()
          .println("};");
      } else {
        String
          lazyConverter = out.addImport(LazyHtmlConverter.class)
          +"<" + classTemplate + ", "+ out.addImport(Element.class) + ">";
        String converter = out.addImport(ConvertsValue.class)
            +"<" + classTemplate + ", String>";
        init.println("new "+lazyConverter+"(")
            .indent()
            .println("new " + converter+"() {")
            .indent()
            .println("public String merge("
              +classTemplate+" "+KEY_FROM+", "
              +classElemental+" "+KEY_SERVICE
            +"){")

            .indentln("return "+METHOD_TO_POTENTIAL+ "(" + KEY_FROM + ","+ KEY_SERVICE+ ").toSource();")
          .println("}")
          .outdent()
          .print("}")
          .outdent()
          .println(");");
      }


      toElement.println(KEY_ELEMENT +" = "
            + KEY_SERVICE+ ".newNode(")
            .indentln("_CLONE.setInitializer("
              + KEY_SERVICE+".asConverter()).merge("+ KEY_FROM + ", "+KEY_SERVICE + ")")
            .println(");");
    } else {
      // There is a root tag, we might not get away w/ a static provider
    }

    final String tag = root.rootElementTag();
    MethodBuffer toHtml = out.createMethod(
        "public static "+ potentialElement + " " + METHOD_TO_POTENTIAL +
          "("
            + "final "+classTemplate+" " + KEY_FROM + ","
            + "final "+classElemental+" "+KEY_SERVICE
          + ")")
          .print(potentialElement+" "+KEY_ELEMENT+" = ")
          .println(KEY_SERVICE+".newNode(\""+tag+"\");")
          ;
    String elementKey = KEY_ELEMENT;
    root.setNameElement(elementKey);
    for (El el : root.getElements()) {
      elementKey = printElement(logger, el, root, out, toHtml, styleInit, null, elementKey, KEY_FROM, generatedState);
    }
    root.setNameElement(elementKey);

    StringBuilder localStyle = new StringBuilder();
    for (Style style : root.getStyles()) {
      printStyle(logger, style, localStyle);
    }
    if (localStyle.length() > 0){
      // Apply the given style to the potential element.
      // TODO Create a method to apply the given style to the potential element
      toHtml.println(elementKey+".setStyle(\""+
        Generator.escape(localStyle.toString())
        + "\");");
    }

    for (HtmlTemplate template : root.getTemplates()) {

      for (Class<?> ref : template.references()) {
        String finalName = toImplSuffix(ref.getCanonicalName());
        ElementalGeneratorResult templateRef = ctx.findExistingProvider(finalName);
        JClassType foreignType = ast.getTypeOracle().findType(ref.getPackage().getName(),
          X_Source.classToEnclosedSourceName(ref));
        if (templateRef == null) {
          String pkg = foreignType.getPackage().getName();
          templateRef = generateProvider(logger, new ModelProviderImpl(pkg, foreignType.getQualifiedSourceName().replace(pkg+".", "")), foreignType, ast, ctx);
        }
        String templateCls = out.addImport(templateRef.getFinalName());
        styleInit.println(templateCls +"." +METHOD_IMPORT+"("+KEY_SERVICE+");");
        if (!template.inherit()) {
          // TODO: check templateRef for usage of $children magic phrase
          String provider = out.addImport(templateRef.getTemplateName());
          String methodName = provider+"."+METHOD_TO_POTENTIAL;

          HtmlGeneratorNode templateRoot = templateRef.getRoot(foreignType, ast.getTypeOracle());
          String from = templateRef.isTypeAssignable(templateType) ? KEY_FROM : "null";
          if (templateRoot != null && templateRoot.hasChildren()) {
            String newKey =
                (elementKey = root.getNameElement())  +"_";

            toHtml.println(potential+" "+newKey+" = "+methodName+"("+from+","+KEY_SERVICE+");");

            root.setNameElement(elementKey+"_");
            toHtml.println(elementKey+".addChild(")
            .indentln(newKey)
            .println(");");
            elementKey = newKey;
          } else {

            toHtml.println(root.getNameElement()+".addChild(")
            .indentln(methodName+"("+ from + ","+KEY_SERVICE+")")
            .println(");");
          }
        }
      }
    }
    Set<String> finished = new HashSet<>();
    AbstractMultiInitMap<String, ElementalGeneratorResult, JClassType> map =
      AbstractMultiInitMap.stringMultiInitMap(new ConvertsValue<JClassType, ElementalGeneratorResult>() {
        @Override
        public ElementalGeneratorResult convert(JClassType from) {
          try {
            ElementalGeneratorResult provider = generateProvider(logger, new ModelProviderImpl(from), from, ast, ctx);
            String template = src.getImports().addImport(provider.getFinalName());
            styleInit.println(template+"."+METHOD_IMPORT+"("+KEY_SERVICE+");");
            return provider;
          } catch (UnableToCompleteException e) {
            throw X_Debug.rethrow(e);
          }
        }

      });
    final String messagesQname = templateType.getQualifiedSourceName();
    for (JClassType type : templateType.getFlattenedSupertypeHierarchy()) {
      if (type.getQualifiedSourceName().equals(Object.class.getCanonicalName())) {
        continue;
      }
      for (JMethod method : type.getMethods()) {
        if (method.isAnnotationPresent(NoUi.class)) {
          continue;
        }
        // TODO handle parameter types that are @Named

        if (method.getParameters().length==0) {
          if (isWidget_getElement(method)) {

          } else if (finished.add(method.getName())) {
            final String provider, methodType = method.getEnclosingType().getErasedType().getQualifiedSourceName();
            ElementalGeneratorResult result;
            if (methodType.equals(messagesQname)) {
              result = res;
            } else {
              result = map.merge(methodType, method.getEnclosingType());
              provider = result.getTemplateName();

              String methodName = toHtml.addImport(provider)+"."+METHOD_TO_POTENTIAL;
              String from =
                method.getEnclosingType()
                  .getErasedType().isAssignableFrom(templateType.getErasedType()) ? KEY_FROM : "null";
              String makeDeep =
                result.getRoot(method.getEnclosingType(), ast.getTypeOracle()).hasChildren() ? ", true" : "";
              toHtml.println(root.getNameElement()+".addChild(")
              .indentln(methodName+"("+ from + "," + KEY_SERVICE + ")"+makeDeep)
              .println(");");
              continue;
            }
            HtmlGeneratorNode node = root.getNode(method.getName());
//            if (node == null) {
//              continue;
//            }
            String returnType = out.addImport(
              method.getReturnType().getQualifiedSourceName()
            );
//          For non-Messages based classes, we'll want to inspect the return type
//          and the parameter type arguments, for matches we can make between
//          single-abstract method types (Provider<X>/Consumer<X>)
//          method.getReturnType().isParameterized().getTypeArgs();
            // Create a public static method which accepts a NodeBuilder, and a string,
            // and appends our template result to said builder.

            String makeDeep = "";//result.getRoot(method.getEnclosingType(), ast.getTypeOracle()).hasChildren() ? ", true" : "";
            String methodName = method.getName()+SUFFIX_MODEL_TO_ELEMENT;
            toHtml.println(root.getNameElement()+".addChild(")
              .indentln(methodName+"(" + KEY_FROM +")"+makeDeep)
              .println(");");
            MethodBuffer printEl = out.createMethod("public static final "+ potentialElement+ " "+methodName+"()")
              .addParameters(classTemplate+" "+KEY_FROM)
              .println(potentialElement+" "+KEY_ELEMENT+" = ");
            String tagName = "";
            if (node.rootElementTag().length() > 0) {
              if (node.rootElementTag().charAt(0)=='#') {
                tagName = "from."+node.rootElementTag().substring(1)+"()";
              } else {
                tagName = "\""+node.rootElementTag()+"\"";
              }
            }
            printEl
              .indentln("new "+potentialElement+"("+tagName+");");

            // Apply any important style
            for (El el : node.getElements()) {
              printElement(logger, el, node, out, printEl, styleInit, method, KEY_ELEMENT, KEY_FROM, generatedState);
            }
            printEl.returnValue(KEY_ELEMENT);
          }

        } else {
          // If a non-zero argument length takes the exact type used as the return type.
          logger.log(Type.WARN, "Skipping method without zero-arg parameters: "+method.getReadableDeclaration());
        }
      }
    }

    toHtml.returnValue(KEY_ELEMENT + ";");
    toElement.returnValue(KEY_ELEMENT);

    try {
      return saveGeneratedType(logger, getLogLevel(), getClass(), ast, src, res, hash);
    } finally {
      clear();
    }
  }

  private boolean isWidget_getElement(JMethod method) {
    return method.getEnclosingType().getQualifiedSourceName()
      .equals(Widget.class.getCanonicalName())
      && method.getName().equals("getElement");
  }

  private String printElement(
      TreeLogger logger,
      El el,
      HtmlGeneratorNode node,
      ClassBuffer out,
      MethodBuffer printEl,
      PrintBuffer styleInit,
      JMethod method,
      UnifyAstView ast,
      String keyElement,
      String keyFrom,
      GeneratedState generatedState) throws UnableToCompleteException {
    String newKey = keyElement;
    String methodName = method == null ? null : method.getName();
    for (String html : el.html()) {
      String result = translate(
        node.escape(html, methodName == null ? "" : methodName, keyFrom+".")
      );
      if (result.length() > 0 ) {
        boolean is$value = HtmlTemplate.KEY_VALUE.equals(result);
        String[] bits = PATTERN_VALUE.split(result);
        String keyService = "_"+KEY_SERVICE;
        if (!generatedState.needsServiceField) {
          generatedState.needsServiceField = true;
          styleInit.println(keyService+"="+KEY_SERVICE+";");
          out.createField(generatedState.serviceType, keyService)
            .makeStatic()
            .makePrivate();
        }
        for (int i = 0, m = bits.length; i<m || is$value; ++i) {
          if (is$value || i > 0 || bits[i].length()==0) {
            if (method == null) {
              logger.log(Type.ERROR, "Cannot use "+HtmlTemplate.KEY_VALUE+" in the context of a class");
              throw new UnableToCompleteException();
            } else {
              JClassType cls = method.getReturnType().isClassOrInterface();
              String invocation = keyFrom+"."+method.getName()+"()";
              if (cls == null) {
                if (method.getReturnType().isPrimitive()!= null) {
                  printEl.println(keyElement+".append("
                    // Appending raw string to template here
                    + keyService+"."+ElementalService.METHOD_ENHANCE_MARKUP+"("
                      + "String.valueOf("+invocation+")"
                    +")"
                  + ");");
                } else {
                  logger.log(Type.ERROR,
                    "Method return type "+method.getReturnType()
                    +" in " + method.getJsniSignature()
                    +" is not supported by ElementalGenerator");
                  throw new UnableToCompleteException();
                }
              } else {
                if (method.getReturnType().isClassOrInterface().isAssignableTo(ctx.getTypeCharSequence())) {
                  // Wrap this w/ runtime enhancement of text
                  printEl.println(keyElement+".append("
                    + keyService+"."+ElementalService.METHOD_ENHANCE_MARKUP+"("+invocation+ ")"
                  + ");");
                } else if (method.getReturnType().isClassOrInterface().isAssignableTo(ctx.getTypePotentialElement())) {
                  printEl.println(keyElement+".addChild("
                    + keyService+"."+ElementalService.METHOD_ENHANCE_MARKUP+"("+invocation+ ")"
                  + ");");
                } else {
                  for (Class<?> toHtml : el.useToHtml()) {
                    if (cls.getQualifiedBinaryName().equals(toHtml.getName())) {
                      printEl.print(keyElement+".append(");
                      // Print a X_Html.toHtml() method call here.
                      String xhtml = printEl.addImport(X_Html.class);
                      String type = out.addImport(method.getReturnType().getQualifiedSourceName());
                      printEl.print(xhtml+".toHtml("+type+".class, "+invocation+", "+keyService+")");
                   
                      printEl.println(");");
                      break;
                    }
                  }
                }
                // TODO handle element, by using callbacks in potential node,
                // This can be done by inserting tagged elements to replace once attached.
              }
            }
          }
          if (is$value) {
            break;
          }
          String encoded = bits[i];
          int ind = encoded.indexOf(HtmlTemplate.KEY_CHILDREN);
          if (ind == -1) {
            // If this encoded bit matches a @fully.qualified.Name,
            // then we should add the foreign class as a child element here.
            Matcher matcher = PATTERN_REFERENCE.matcher(encoded);
            if (matcher.matches()) {
              // check the group that matches, and construct a call to the
              // referenced classes .toElement
              String val = matcher.group(1);

              JClassType type = ast.getTypeOracle().findType(val);
              ElementalGeneratorResult res = generateProvider(logger, new ModelProviderImpl(type), type, ast, ctx);
              String imported = printEl.addImport(type.getQualifiedSourceName());
              styleInit.println(printEl.addImport(res.getFinalNameQualified())
                +"."+METHOD_IMPORT+"("+KEY_SERVICE+");");
              String source =
                method.getReturnType().isClassOrInterface().isAssignableTo(type)
                  ? keyFrom+"."+method.getName()+"()"
                : type.isAssignableTo(ctx.getTypeMessages())
                  ? printEl.addImport(GWT.class)+ ".<"
                    + imported+ ">create("
                    +imported+".class)"
                : type.isAssignableFrom(method.getEnclosingType())
                  ? keyFrom
                : "null";
              printEl.println(keyElement+".addChild("
                + printEl.addImport(res.getTemplateName())
                + "."+METHOD_TO_POTENTIAL+"("
                  + source + ", "+KEY_SERVICE
                + "));");
            } else {
              printEl.println(keyElement+".append("
                + keyService+"."+ElementalService.METHOD_ENHANCE_MARKUP+"("
                  + "\""+encoded+"\""
                +")"
              + ");");
            }
          } else {
            int len = HtmlTemplate.KEY_CHILDREN.length();
            if (ind > 0) {
              printEl.println(keyElement+".append("
                + keyService+"."+ElementalService.METHOD_ENHANCE_MARKUP+"("
                  + "\""+encoded.substring(0, ind)+"\""
                +")"
              + ");");
            }
            newKey = keyElement+"_";
            String potential = printEl.addImport(PotentialNode.class)+"<"+printEl.addImport(Element.class)+">";
            printEl.println(potential+" "+newKey+" = "+KEY_SERVICE+"."+"newNode();");
            printEl.println(keyElement+".addChild("+newKey+", true);");
            if (ind < encoded.length()-len) {
              printEl.println(keyElement+".append("
                + keyService+"."+ElementalService.METHOD_ENHANCE_MARKUP+"("
                  + "\""+encoded.substring(ind+len)+"\""
                +")"
              + ");");
            }
          }
        }
      }
    }
    StringBuilder b = new StringBuilder();
    String[] classes = el.className();
    for (int i = 0, m = classes.length; i < m; ++i) {
      if (i > 0) {
        b.append(" ");
      }
      b.append(classes[i].startsWith(".")?classes[i].substring(1):classes[i]);
    }
    for (Entry<String, String> prop : htmlGen.getProperties(el).entrySet()) {
      if ("class".equals(prop.getKey())) {
        if (b.length() > 0) {b.append(" ");}
        b.append(prop.getValue());
      } else {
        printEl.println(keyElement+".setAttribute(\""+prop.getKey()+"\""
            + ", \""+prop.getValue()+ "\");");
      }
    }
    if (b.length() > 0) {
      printEl.println(keyElement+".setClass(\""+b+"\");");
    }

    StringBuilder localStyle = new StringBuilder();

    for (Style style : node.getStyles()) {
      printStyle(logger, style, localStyle);
    }
    if (localStyle.length() > 0){
      // Apply the given style to the potential element.
      // TODO Create a method to apply the given style to the potential element
      printEl.println(keyElement+".setStyle(\""+
          translate(localStyle.toString())
      + "\");");
    }

    return newKey;
  }

  private String translate(String encoded) {
    return Generator.escape(new LexerForMarkup()
      .setLinkAttributes("class=\"local-link\"")
      .lex(encoded)
      .toSource());
  }

  private void printStyle(
      TreeLogger logger,
      Style style,
      StringBuilder localStyle,
      PrintBuffer styleInit) {
    boolean isLocal = style.names().length == 0;
    if (!isLocal) {
      return;
    }
    HtmlSnippet.appendTo(localStyle, style);
  }

  @Override
  protected Type getLogLevel() {
    return Type.INFO;
//    return Type.DEBUG;
  }

  @Override
  protected ElementalGeneratorResult saveGeneratedType(
    TreeLogger logger,
    UnifyAstView ast,
    SourceBuilder<?> out,
    ElementalGeneratorResult result,
    String inputHash) throws UnableToCompleteException {
    logger.log(Type.INFO, "Saving generated type "+ out.getQualifiedName());
    return super.saveGeneratedType(logger, ast, out, result, inputHash);
  }

  private static String toImplName(TreeLogger logger, String modelPackage, String ... types) {
    StringBuilder b = new StringBuilder();
    for (int i = 0, m = types.length; i < m; i++) {
      if (i > 0) {
        if (types[i].equals(types[i-1])) {
          continue;
        }
        b.append("_");
      }
      b.append(types[i].replace(modelPackage+".", "").replace('.', '_'));
    }
    return toImplSuffix(b.toString());
  }

  private static String toImplSuffix(String value) {
    return value+"_ElementalImpl";
  }

  private static String toTemplateName(TreeLogger logger, JClassType templateType) {
    return templateType.getSimpleSourceName()+"_TemplateImpl";
  }

  @Override
  public ElementalGeneratorResult newContext(JClassType winner, String pkgName, String name) {
    ElementalGeneratorResult result = new ElementalGeneratorResult(winner, pkgName, name);
    ctx.setExistingProvider(name, result);
    return result;
  }

}
