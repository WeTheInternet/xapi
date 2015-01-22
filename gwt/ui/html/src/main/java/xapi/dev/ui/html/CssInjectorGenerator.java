package xapi.dev.ui.html;

import static com.google.gwt.reflect.rebind.ReflectionUtilType.findType;

import static xapi.dev.ui.html.AbstractHtmlGenerator.existingTypesUnchanged;
import static xapi.dev.ui.html.AbstractHtmlGenerator.findExisting;
import static xapi.dev.ui.html.AbstractHtmlGenerator.saveGeneratedType;
import static xapi.dev.ui.html.AbstractHtmlGenerator.toHash;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import xapi.annotation.compile.Import;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.IntTo.Many;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.source.X_Source;
import xapi.time.impl.RunOnce;
import xapi.ui.api.StyleService;
import xapi.ui.html.X_Html;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlTemplate;
import xapi.ui.html.api.Style;
import xapi.util.api.ReceivesValue;

public class CssInjectorGenerator implements CreatesContextObject<HtmlGeneratorResult>{

  static final String GENERATED_SUFFIX = "_InjectCss";
  int ctxCnt;
  private String clsName;
  private HtmlGeneratorContext htmlGen;
  private SourceBuilder<UnifyAstView> out;

  public static final HtmlGeneratorResult generateSnippetProvider(TreeLogger logger, UnifyAstView ast, JClassType templateType) throws UnableToCompleteException {
    String simpleName = templateType.getSimpleSourceName()+GENERATED_SUFFIX;

    // Check if there is an existing type, and that it's generated hashes match our input type.
    String inputHash = toHash(ast, templateType.getQualifiedSourceName());

    // Generate a new result
    CssInjectorGenerator ctx = new CssInjectorGenerator(simpleName, ast, templateType);
    // Save the result, possibly to a new file if there are existing implementations using our default name
    return ctx.generate(logger, ast, templateType, inputHash, simpleName);
  }

  @Override
  public HtmlGeneratorResult newContext(JClassType winner, String pkgName, String name) {
    return new HtmlGeneratorResult(winner, pkgName, name);
  }

  private CssInjectorGenerator(String className, UnifyAstView ast, JClassType templateType) throws UnableToCompleteException {
    this.clsName = className;
    this.htmlGen = new HtmlGeneratorContext(templateType);
    this.out = new SourceBuilder<UnifyAstView>("public class "+clsName)
      .setPackage(templateType.getPackage().getName())
      .setPayload(ast);
  }

  private HtmlGeneratorResult generate(TreeLogger logger, UnifyAstView ast, JClassType injectionType, String inputHash, String simpleName) throws UnableToCompleteException {

    HtmlGeneratorResult existingType = findExisting(ast, this,
      injectionType.getPackage().getName(),
      X_Source.qualifiedName(injectionType.getPackage().getName(), simpleName));
    HtmlGeneratorResult existingResult = existingTypesUnchanged(logger, ast, existingType, inputHash);
    if (existingResult != null)
      // If our inputs are unchanged, and the target type exists, just reuse it w/out regenerating
      return existingResult;

    clsName = existingType.getFinalName();
    ClassBuffer cls = out.getClassBuffer();

    String cssService = cls.addImport(StyleService.class);
    String setOnce = cls.addImportStatic(RunOnce.class, "setOnce");
    String receiver = cls.addImport(ReceivesValue.class);

    // Create a public method for callers to access
    MethodBuffer inject = cls.createMethod("public static void inject("+cssService+" serv)");

    // Now fill out the body with our css injection that will run only once.
    IntTo.Many<Style> styles = X_Collect.newIntMultiMap(Style.class);
    Set<Class<? extends ClientBundle>> resourceTypes = new HashSet<Class<? extends ClientBundle>>();
    Set<String> importTypes = new LinkedHashSet<String>();
    fillStyles(logger, styles, resourceTypes, importTypes, injectionType);

    // Now compute any supertypes that might want injection too
    for (JClassType superType : injectionType.getFlattenedSupertypeHierarchy()) {
      if (!superType.getQualifiedSourceName().equals(injectionType.getQualifiedSourceName())) {
        if (hasStyle(superType)) {
          importTypes.add(superType.getQualifiedSourceName());
        }
      }
    }

    // Always print calls to inject style for any explicit imports, and for all supertypes
    // If these types lack style, their inject methods will no-op and get erased by the compiler
    for (String importable : importTypes) {
      importable = inject.addImport(importable);
      String injectCss = inject.addImportStatic(X_Html.class, "injectCss");
      inject.println(injectCss+"("+importable+".class, serv);");
    }

    if (styles.isEmpty() && resourceTypes.isEmpty()) {
      // Nothing to import.  Lets encourage inlining by printing an empty method :D
      logger.log(getLogLevel(), "Skipped style injection for style-less class "+injectionType.getQualifiedSourceName());
    } else {

      // This type actually has some @Style rules or ClientBundle instances to inject.
      inject.println("ONCE.set(serv);");

      // Now create a private field that will allow us to implement set-once semantics
      PrintBuffer init = cls.createField(receiver+"<"+cssService+">", "ONCE")
         .makeFinal()
         .makeStatic()
         .makePrivate()
         .getInitializer()
         .println(setOnce+"(new "+receiver+"<"+cssService+">(){")
         .indent()
         .println("@Override")
         .println("public void set("+cssService+" serv) {");

      // Tear off a print buffer for the body of the method
      PrintBuffer body = new PrintBuffer();
      init.addToEnd(body);

      // Close the initializer now, so we don't wind up with close-brace-soup later on.
      init.println("}")
          .outdent()
          .println("});");

      // Print all our manually defined @Style attribute
      for (IntTo<Style> styleSet : styles.forEach()) {
        int priority = 0;
        StringBuilder sheetStyle = new StringBuilder();
        for (Style style : styleSet.forEach()) {
          priority = style.priority();
          try {
            AbstractHtmlGenerator.fillStyles(null, sheetStyle, style);
          } catch (Exception e) {
            logger.log(Type.ERROR, "Error calculating styles", e);
            throw new UnableToCompleteException();
          }
        }
        if (sheetStyle.length() > 0) {
          body.println("serv.addCss(\""+Generator.escape(sheetStyle.toString())+"\", "+priority+");");
        }
      }

      // Print injections and initializations for all ClientBundle classes
      int numResource = 0;
      JClassType cssResource = findType(ast.getTypeOracle(), CssResource.class);
      for (Class<? extends ClientBundle> resourceType : resourceTypes) {
        JClassType type = findType(ast.getTypeOracle(), resourceType);
        assert type != null : "Unable to inject ClientBundle class: "+resourceType;
        String resource = inject.addImport(resourceType);
        String gwtCreate = inject.addImportStatic(GWT.class, "create");
        String name = "res"+numResource++;
        init.println(resource +" "+name+" = "+gwtCreate+"("+resource+".class);");
        // Now, search the declared type for methods that are instances of CssResource, to .ensureInjected()
        for (JMethod method : cssResource.getMethods()) {
          if (method.getReturnType().isClassOrInterface().isAssignableTo(cssResource)) {
            init.println(name+"."+method.getName()+"().ensureInjected();");
          }
        }
      }

    }

    try {
      return saveGeneratedType(logger, getLogLevel(), getClass(), ast, out, existingType, inputHash);
    } finally {
      ast.getGeneratorContext().finish(logger);
    }
  }

  private boolean hasStyle(JClassType superType) {
    Css css = superType.getAnnotation(Css.class);
    if (css != null)
      return true;
    Style style = superType.getAnnotation(Style.class);
    if (style != null && style.names().length > 0)
      return true;
    Html html = superType.getAnnotation(Html.class);
    if (html != null) {
      if (html.css().length > 0)
        return true;
      if (hasStyle(html.body()))
        return true;
      if (hasStyle(html.templates()))
        return true;
    }
    HtmlTemplate template = superType.getAnnotation(HtmlTemplate.class);
    if (template != null && hasStyle(template))
      return true;
    El el = superType.getAnnotation(El.class);
    return el != null && hasStyle(el);
  }

  private boolean hasStyle(HtmlTemplate ... templates) {
    for (HtmlTemplate template : templates) {
      if (template.imports().length > 0 || template.references().length > 0)
        return true;
    }
    return false;
  }

  private boolean hasStyle(El ... body) {
    for (El el : body) {
      for (Style style : el.style()) {
        if (style.names().length > 0)
          return true;
      }
      if (el.imports().length > 0)
        return true;
    }
    return false;
  }

  private void fillStyles(TreeLogger logger, Many<Style> styles, Set<Class<? extends ClientBundle>> resourceTypes, Set<String> importTypes, JClassType templateType) {
    Html html = templateType.getAnnotation(Html.class);
    if (html != null) {
      fillStyles(logger, styles, resourceTypes, html.css());
      fillStyles(logger, styles, importTypes, html.body());
      fillStyles(logger, styles, importTypes, html.templates());
    }
    HtmlTemplate template = templateType.getAnnotation(HtmlTemplate.class);
    if (template != null) {
      fillStyles(logger, styles, importTypes, template);
    }
    El el = templateType.getAnnotation(El.class);
    if (el != null) {
      fillStyles(logger, styles, importTypes, el);
    }
    Css css = templateType.getAnnotation(Css.class);
    if (css != null) {
      fillStyles(logger, styles, resourceTypes, css);
    }
    Style style = templateType.getAnnotation(Style.class);
    if (style != null && style.names().length > 0) {
      styles.add(style.priority(), style);
    }
  }

  private void fillStyles(TreeLogger logger, Many<Style> styles, Set<String> importTypes, HtmlTemplate ... templates) {
    for (HtmlTemplate template : templates) {
      for (Import importable : template.imports()) {
        importTypes.add(importable.value().getCanonicalName());
      }
      for (Class<?> importable : template.references()) {
        importTypes.add(importable.getCanonicalName());
      }
    }
  }

  private void fillStyles(TreeLogger logger, Many<Style> styles, Set<String> importTypes, El ... body) {
    for (El item : body) {
      for (Style style : item.style()) {
        if (style.names().length > 0) {
          styles.add(style.priority(), style);
        }
      }
      for (Import importable : item.imports()) {
        importTypes.add(importable.value().getCanonicalName());
      }
    }
  }

  private void fillStyles(TreeLogger logger, Many<Style> styles, Set<Class<? extends ClientBundle>> resourceTypes, Css ... csses) {
    for (Css css : csses) {
      for (Style style : css.style()) {
        if (style.names().length > 0) {
          styles.add(style.priority(), style);
        }
      }
      for (Class<? extends ClientBundle> cls : css.resources()) {
        resourceTypes.add(cls);
      }
    }
  }

  protected Type getLogLevel() {
    return Type.TRACE;
  }
}
