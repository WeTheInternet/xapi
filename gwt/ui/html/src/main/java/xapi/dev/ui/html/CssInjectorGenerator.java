package xapi.dev.ui.html;

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
import xapi.ui.html.api.Css.CssFile;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlTemplate;
import xapi.ui.html.api.Style;
import xapi.util.api.ReceivesValue;

import static com.google.gwt.reflect.rebind.ReflectionUtilType.findType;
import static xapi.dev.ui.html.AbstractHtmlGenerator.existingTypesUnchanged;
import static xapi.dev.ui.html.AbstractHtmlGenerator.findExisting;
import static xapi.dev.ui.html.AbstractHtmlGenerator.saveGeneratedType;
import static xapi.dev.ui.html.AbstractHtmlGenerator.toHash;

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

import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class CssInjectorGenerator implements CreatesContextObject<HtmlGeneratorResult>{

  static final String GENERATED_SUFFIX = "_InjectCss";
  int ctxCnt;
  private String clsName;
  private final HtmlGeneratorContext htmlGen;
  private final SourceBuilder<UnifyAstView> out;

  public static final HtmlGeneratorResult generateSnippetProvider(final TreeLogger logger, final UnifyAstView ast, final JClassType templateType) throws UnableToCompleteException {
    final String simpleName = templateType.getSimpleSourceName()+GENERATED_SUFFIX;

    // Check if there is an existing type, and that it's generated hashes match our input type.
    final String inputHash = toHash(ast, templateType.getQualifiedSourceName());

    // Generate a new result
    final CssInjectorGenerator ctx = new CssInjectorGenerator(simpleName, ast, templateType);
    // Save the result, possibly to a new file if there are existing implementations using our default name
    return ctx.generate(logger, ast, templateType, inputHash, simpleName);
  }

  @Override
  public HtmlGeneratorResult newContext(final JClassType winner, final String pkgName, final String name) {
    return new HtmlGeneratorResult(winner, pkgName, name);
  }

  private CssInjectorGenerator(final String className, final UnifyAstView ast, final JClassType templateType) throws UnableToCompleteException {
    this.clsName = className;
    this.htmlGen = new HtmlGeneratorContext(templateType);
    this.out = new SourceBuilder<UnifyAstView>("public class "+clsName)
      .setPackage(templateType.getPackage().getName())
      .setPayload(ast);
  }

  private HtmlGeneratorResult generate(final TreeLogger logger, final UnifyAstView ast, final JClassType injectionType, final String inputHash, final String simpleName) throws UnableToCompleteException {

    final HtmlGeneratorResult existingType = findExisting(ast, this,
      injectionType.getPackage().getName(),
      X_Source.qualifiedName(injectionType.getPackage().getName(), simpleName));
    final HtmlGeneratorResult existingResult = existingTypesUnchanged(logger, ast, existingType, inputHash);
    if (existingResult != null) {
      // If our inputs are unchanged, and the target type exists, just reuse it w/out regenerating
      return existingResult;
    }

    clsName = existingType.getFinalName();
    final ClassBuffer cls = out.getClassBuffer();

    final String cssService = cls.addImport(StyleService.class);
    final String setOnce = cls.addImportStatic(RunOnce.class, "setOnce");
    final String receiver = cls.addImport(ReceivesValue.class);

    // Create a public method for callers to access
    final MethodBuffer inject = cls.createMethod("public static void inject("+cssService+" serv)");

    // Now fill out the body with our css injection that will run only once.
    final IntTo.Many<Style> styles = X_Collect.newIntMultiMap(Style.class);
    final Set<Class<? extends ClientBundle>> resourceTypes = new HashSet<Class<? extends ClientBundle>>();
    final Set<String> importTypes = new LinkedHashSet<>();
    final Set<CssFile> files = new LinkedHashSet<>();
    fillStyles(logger, styles, files, resourceTypes, importTypes, injectionType);

    // Now compute any supertypes that might want injection too
    for (final JClassType superType : injectionType.getFlattenedSupertypeHierarchy()) {
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
      final String injectCss = inject.addImportStatic(X_Html.class, "injectCss");
      inject.println(injectCss+"("+importable+".class, serv);");
    }

    if (styles.isEmpty() && resourceTypes.isEmpty()) {
      // Nothing to import.  Lets encourage inlining by printing an empty method :D
      logger.log(getLogLevel(), "Skipped style injection for style-less class "+injectionType.getQualifiedSourceName());
    } else {

      // This type actually has some @Style rules or ClientBundle instances to inject.
      inject.println("ONCE.set(serv);");

      // Now create a private field that will allow us to implement set-once semantics
      final PrintBuffer init = cls.createField(receiver+"<"+cssService+">", "ONCE")
         .makeFinal()
         .makeStatic()
         .makePrivate()
         .getInitializer()
         .println(setOnce+"(new "+receiver+"<"+cssService+">(){")
         .indent()
         .println("@Override")
         .println("public void set("+cssService+" serv) {");

      // Tear off a print buffer for the body of the method
      final PrintBuffer body = init.makeChild();

      // Close the initializer now, so we don't wind up with close-brace-soup later on.
      init.println("}")
          .outdent()
          .println("});");

      // Print all our manually defined @Style attribute
      for (final IntTo<Style> styleSet : styles.forEach()) {
        int priority = Integer.MAX_VALUE;
        final StringBuilder sheetStyle = new StringBuilder();
        final StringBuilder extraStyle = new StringBuilder();
        for (final Style style : styleSet.forEach()) {
          try {
            int pos = AbstractHtmlGenerator.fillStyles(null, sheetStyle, extraStyle, style);
            priority = Math.min(pos, priority);
          } catch (final Exception e) {
            logger.log(Type.ERROR, "Error calculating styles", e);
            throw new UnableToCompleteException();
          }
        }
        if (sheetStyle.length() > 0) {
          body.println("serv.addCss(\""+Generator.escape(sheetStyle.toString())+"\", "+priority+");");
        }
        if (extraStyle.length() > 0) {
          body.println("serv.addCss(\""+Generator.escape(extraStyle.toString())+"\", Integer.MIN_VALUE);");
        }
      }

      for (CssFile file : files) {
        final Class<? extends CssResource>[] interfaces = file.interfaces();
        final String[] fileNames = file.value();
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (int i = 0, m = fileNames.length; i < m; i++ ) {
          String fileName = fileNames[i];
          if (fileName.startsWith("/")) {
            // This is to be treated as an absolute location
            fileName = fileName.substring(1);
          } else if (fileName.startsWith("./")) {
            // Treat this file as relative to the first CssResource interface provided
            if (interfaces.length == 0) {
              logger.log(Type.ERROR, "Cannot use a relative @CssFile location using ./ without also supplying a CssResource in the interfaces annotation member. " +
                      "You supplied: "+file+".\n" +
                      "./uris are relative to the css interface\n" +
                      "/uris are absolute to the classpath\n" +
                      "and all unprefixed uris are relative to the location where the @CssFile annotation is found ("+injectionType.getQualifiedSourceName()+")");
              throw new UnableToCompleteException();
            }
            fileName = interfaces[0].getPackage().getName().replace('.', '/') + "/" + fileName;
          } else {
            // Treat this file as relative to the type we are injecting
            fileName = injectionType.getPackage().getName().replace('.', '/') + "/" + fileName;
          }
          final URL url = cl.getResource(fileName);
          if (url == null) {
            logger.log(Type.ERROR, "Unable to find resource "+fileName+" on the classpath.  Path derived from "+file+" on "+injectionType.getQualifiedSourceName());
            throw new UnableToCompleteException();
          }
          fileNames[i] = fileName;
        }

        if (interfaces.length == 0) {
          // No interfaces.  We just want to dump the whole stylesheet
        } else if (interfaces.length == 1) {
          // exactly one interface, just use it directly
        } else {
          // more than one interface, we need to generate a container type
        }
      }

      // Print injections and initializations for all ClientBundle classes
      int numResource = 0;
      final JClassType cssResource = findType(ast.getTypeOracle(), CssResource.class);
      for (final Class<? extends ClientBundle> resourceType : resourceTypes) {
        final JClassType type = findType(ast.getTypeOracle(), resourceType);
        assert type != null : "Unable to inject ClientBundle class: "+resourceType;
        final String resource = inject.addImport(resourceType);
        final String gwtCreate = inject.addImportStatic(GWT.class, "create");
        final String name = "res"+numResource++;
        body.println(resource +" "+name+" = "+gwtCreate+"("+resource+".class);");
        // Now, search the declared type for methods that are instances of CssResource, to .ensureInjected()
        for (final JMethod method : type.getMethods()) {
          if (!method.isStatic()) {
            final JClassType asType = method.getReturnType().isClassOrInterface();
            if (asType != null && asType.isAssignableTo(cssResource)) {
              body.println(name+"."+method.getName()+"().ensureInjected();");
            }
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

  private boolean hasStyle(final JClassType superType) {
    final Css css = superType.getAnnotation(Css.class);
    if (css != null) {
      return true;
    }
    final Style style = superType.getAnnotation(Style.class);
    if (style != null && style.names().length > 0) {
      return true;
    }
    final Html html = superType.getAnnotation(Html.class);
    if (html != null) {
      if (html.css().length > 0) {
        return true;
      }
      if (hasStyle(html.body())) {
        return true;
      }
      if (hasStyle(html.templates())) {
        return true;
      }
    }
    final HtmlTemplate template = superType.getAnnotation(HtmlTemplate.class);
    if (template != null && hasStyle(template)) {
      return true;
    }
    final El el = superType.getAnnotation(El.class);
    return el != null && hasStyle(el);
  }

  private boolean hasStyle(final HtmlTemplate ... templates) {
    for (final HtmlTemplate template : templates) {
      if (template.imports().length > 0 || template.references().length > 0) {
        return true;
      }
    }
    return false;
  }

  private boolean hasStyle(final El ... body) {
    for (final El el : body) {
      for (final Style style : el.style()) {
        if (style.names().length > 0) {
          return true;
        }
      }
      if (el.imports().length > 0) {
        return true;
      }
    }
    return false;
  }

  private void fillStyles(
      final TreeLogger logger,
      final Many<Style> styles,
      final Set<CssFile> files,
      final Set<Class<? extends ClientBundle>> resourceTypes,
      final Set<String> importTypes,
      final JClassType templateType) {
    final Html html = templateType.getAnnotation(Html.class);
    if (html != null) {
      fillStyles(logger, styles, files, resourceTypes, html.css());
      fillStyles(logger, styles, importTypes, html.body());
      fillStyles(logger, styles, importTypes, html.templates());
    }
    final HtmlTemplate template = templateType.getAnnotation(HtmlTemplate.class);
    if (template != null) {
      fillStyles(logger, styles, importTypes, template);
    }
    final El el = templateType.getAnnotation(El.class);
    if (el != null) {
      fillStyles(logger, styles, importTypes, el);
    }
    final Css css = templateType.getAnnotation(Css.class);
    if (css != null) {
      fillStyles(logger, styles, files, resourceTypes, css);
    }
    final Style style = templateType.getAnnotation(Style.class);
    if (style != null && style.names().length > 0) {
      styles.add(style.priority(), style);
    }
  }

  private void fillStyles(final TreeLogger logger, final Many<Style> styles, final Set<String> importTypes, final HtmlTemplate ... templates) {
    for (final HtmlTemplate template : templates) {
      for (final Import importable : template.imports()) {
        importTypes.add(importable.value().getCanonicalName());
      }
      for (final Class<?> importable : template.references()) {
        importTypes.add(importable.getCanonicalName());
      }
    }
  }

  private void fillStyles(final TreeLogger logger, final Many<Style> styles, final Set<String> importTypes, final El ... body) {
    for (final El item : body) {
      for (final Style style : item.style()) {
        if (style.names().length > 0) {
          styles.add(style.priority(), style);
        }
      }
      for (final Import importable : item.imports()) {
        importTypes.add(importable.value().getCanonicalName());
      }
    }
  }

  private void fillStyles(final TreeLogger logger,
                          final Many<Style> styles,
                          final Set<CssFile> files,
                          final Set<Class<? extends ClientBundle>> resourceTypes,
                          final Css ... csses) {
    for (final Css css : csses) {
      for (final Style style : css.style()) {
        if (style.names().length > 0) {
          styles.add(style.priority(), style);
        }
      }
      for (final Class<? extends ClientBundle> cls : css.resources()) {
        resourceTypes.add(cls);
      }
      for (final CssFile file : css.files()) {
        files.add(file);
      }
    }
  }

  protected Type getLogLevel() {
    return Type.DEBUG;
  }
}
