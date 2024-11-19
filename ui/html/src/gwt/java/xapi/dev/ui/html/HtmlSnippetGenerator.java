package xapi.dev.ui.html;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.jjs.UnifyAstView;

import java.lang.reflect.Modifier;

import xapi.annotation.common.Property;
import xapi.annotation.compile.Import;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.fu.Out1;
import xapi.source.X_Source;
import xapi.ui.api.StyleService;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlSnippet;
import xapi.util.api.ConvertsValue;

public class HtmlSnippetGenerator extends AbstractHtmlGenerator <HtmlGeneratorResult> {

  int ctxCnt;

  public static final HtmlGeneratorResult generateSnippetProvider(final TreeLogger logger, final UnifyAstView ast, final JClassType templateType, final JClassType modelType) throws UnableToCompleteException {
    final String simpleName = templateType.getSimpleSourceName()+"_"+modelType.getSimpleSourceName()+"_ToHtml";

    // Check if there is an existing type, and that it's generated hashes match our input type.
    final String inputHash = toHash(ast, templateType.getQualifiedSourceName(), modelType.getQualifiedSourceName());

    // Generate a new result
    final HtmlSnippetGenerator ctx = new HtmlSnippetGenerator(simpleName, ast, templateType);
    // Save the result, possibly to a new file if there are existing implementations using our default name
    return ctx.generate(logger, ast, templateType, inputHash, simpleName, modelType);
  }

  @Override
  public HtmlGeneratorResult newContext(final JClassType winner, final String pkgName, final String name) {
    return new HtmlGeneratorResult(winner, pkgName, name);
  }

  private HtmlSnippetGenerator(final String className, final UnifyAstView ast, final JClassType templateType) throws UnableToCompleteException {
    super(className, templateType, ast);
  }

  private HtmlGeneratorResult generate(final TreeLogger logger, final UnifyAstView ast, final JClassType templateType, final String inputHash, final String simpleName, final JClassType modelType) throws UnableToCompleteException {

    final HtmlGeneratorResult existingType = findExisting(ast, this,
      templateType.getPackage().getName(),
      X_Source.qualifiedName(templateType.getPackage().getName(), simpleName));
    final HtmlGeneratorResult existingResult = existingTypesUnchanged(logger, ast, existingType, inputHash);
    if (existingResult != null) {
      // If our inputs are unchanged, and the target type exists, just reuse it w/out regenerating
      return existingResult;
    }

    initialize();
    clsName = existingType.getFinalName();
    final ClassBuffer cls = out.getClassBuffer();
    final String modelName = out.getImports().addImport(modelType.getQualifiedSourceName());
    cls.setSuperClass(
      out.getImports().addImport(HtmlSnippet.class)
      + "<" + modelName + ">"
    );

    final String buffer = cls.addImport(DomBuffer.class);
    final String cssService = cls.addImport(StyleService.class);

    final String sig = cls.addImport(ConvertsValue.class) + "<" +
      modelName+", " + buffer +
    ">";

    final ClassBuffer provider = cls.createInnerClass("private static final class SnippetProvider")
       .addInterface(cls.addImport(Out1.class) + " <" + sig + ">");

    cls
      .createConstructor(Modifier.PUBLIC, cssService+" cssService")
      .println("super(new SnippetProvider(cssService));");

    final MethodBuffer ctor = provider.createConstructor(Modifier.PRIVATE, "final "+cssService+" cssService");

    provider.createField(Runnable.class, "init");
    ctor.println("this.init = new "+cls.addImport(Runnable.class)+"() {")
      .indent()
      .println("@Override public void run() {")
      .indent()
      .println("init = NO_OP;");
    final PrintBuffer init = new PrintBuffer();
    ctor.addToEnd(init);
    ctor
      .outdent()
      .println("}")
      .outdent()
      .println("};");

    final MethodBuffer converter = provider.createMethod("public "+sig+" out1()");

    converter
      .println("return new "+sig+"() {")
      .indent()
      .println("@Override")
      .println("public " + buffer + " convert("+modelName+" from) {")
      .indent()
      .print(buffer+" out = new "+buffer+"(")
    ;
    if (!Html.ROOT_ELEMENT.equals(documentType)) {
      if (documentType.charAt(0)=='#') {
        converter.print("from."+documentType.substring(1)+"()");
      } else {
        converter.print("\""+documentType+"\"");
      }
    }
    converter.println( ");");

    final DomBuffer variables = new DomBuffer().println("init.run();");
    converter.addToEnd(variables);

    for (final String key : renderOrder) {
      for (final El el : htmlGen.getElements(key)) {
        for (final Import importType : el.imports()) {
          addImport(importType);
        }
        final String name = el.tag();
        converter.println("out").indent();
        if (name.length()>0) {
          if (name.charAt(0)=='#') {
            // The tagname is a method reference...
            // TODO validate the method name, and allow for full @fully.qualified.Names::toMethods()
            converter.println(".makeTag(from."+name.substring(1)+"())");
          } else {
            converter.println(".makeTag(\""+name+"\")");
          }
        }

        for (final String clsName : el.className()) {
          converter.println(".addClassName(\""+clsName+"\")");
        }

        final StringBuilder immediateStyle = new StringBuilder();
        final StringBuilder sheetStyle = new StringBuilder();
        final StringBuilder extraStyle = new StringBuilder();
        try {
          fillStyles(immediateStyle, sheetStyle, extraStyle, el.style());
        } catch (final Exception e) {
          logger.log(Type.ERROR, "Error calculating styles", e);
          throw new UnableToCompleteException();
        }

        if (immediateStyle.length() > 0) {
          converter.println(".setAttribute(\"style\", \""+Generator.escape(immediateStyle.toString())+"\")");
        }
        if (sheetStyle.length() > 0) {
          init.println("cssService.addCss(\""+Generator.escape(sheetStyle.toString())+"\", 0);");
        }
        if (extraStyle.length() > 0) {
          init.println("cssService.addCss(\""+Generator.escape(extraStyle.toString())+"\", Integer.MIN_VALUE);");
        }

        for (final Property prop : el.properties()) {
          String val = prop.value();
          if (val.startsWith("//")) {
            val = val.substring(2);
          } else if (!val.startsWith("\"")){
            val = "\""+val+"\"";
          }
          converter.println(".setAttribute(\""+prop.name()+"\", "+val+ ")");
        }

        for (final String html : el.html()) {
          final String escaped = escape(html, key, el.accessor());
          if (escaped.length() > 0) {
            converter.println(".println("+ escaped + ")");
          }
        }
        converter.outdent().println(";");
      }
    }

    converter.returnValue("out");

    converter
      .outdent()
      .println("}")
      .outdent()
      .println("};");

    try {
      return saveGeneratedType(logger, getLogLevel(), getClass(), ast, out, existingType, inputHash);
    } finally {
      clear();
      ast.getGeneratorContext().finish(logger);
    }
  }

  @Override
  protected Type getLogLevel() {
    return Type.DEBUG;
  }
}
