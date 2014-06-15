package xapi.dev.ui.html;

import java.lang.reflect.Modifier;

import javax.inject.Provider;

import xapi.annotation.common.Property;
import xapi.annotation.compile.Import;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.source.X_Source;
import xapi.ui.api.StyleService;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlSnippet;
import xapi.util.api.ConvertsValue;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.jjs.UnifyAstView;

public class HtmlSnippetGenerator extends AbstractHtmlGenerator <HtmlGeneratorResult> {

  int ctxCnt;

  public static final HtmlGeneratorResult generateSnippetProvider(TreeLogger logger, UnifyAstView ast, JClassType templateType, JClassType modelType) throws UnableToCompleteException {
    String simpleName = templateType.getSimpleSourceName()+"_"+modelType.getSimpleSourceName()+"_ToHtml";

    // Check if there is an existing type, and that it's generated hashes match our input type.
    String inputHash = toHash(ast, templateType.getQualifiedSourceName(), modelType.getQualifiedSourceName());

    // Generate a new result
    HtmlSnippetGenerator ctx = new HtmlSnippetGenerator(simpleName, ast, templateType);
    // Save the result, possibly to a new file if there are existing implementations using our default name
    return ctx.generate(logger, ast, templateType, inputHash, simpleName, modelType);
  }

  @Override
  public HtmlGeneratorResult newContext(JClassType winner, String pkgName, String name) {
    return new HtmlGeneratorResult(winner, pkgName, name);
  }

  private HtmlSnippetGenerator(String className, UnifyAstView ast, JClassType templateType) throws UnableToCompleteException {
    super(className, templateType, ast);
  }

  private HtmlGeneratorResult generate(TreeLogger logger, UnifyAstView ast, JClassType templateType, String inputHash, String simpleName, JClassType modelType) throws UnableToCompleteException {

    HtmlGeneratorResult existingType = findExisting(ast, this,
      templateType.getPackage().getName(),
      X_Source.qualifiedName(templateType.getPackage().getName(), simpleName));
    HtmlGeneratorResult existingResult = existingTypesUnchanged(logger, ast, existingType, inputHash);
    if (existingResult != null) {
      // If our inputs are unchanged, and the target type exists, just reuse it w/out regenerating
      return existingResult;
    }

    initialize();
    clsName = existingType.getFinalName();
    ClassBuffer cls = out.getClassBuffer();
    String modelName = out.getImports().addImport(modelType.getQualifiedSourceName());
    cls.setSuperClass(
      out.getImports().addImport(HtmlSnippet.class)
      + "<" + modelName + ">"
    );

    String buffer = cls.addImport(DomBuffer.class);
    String cssService = cls.addImport(StyleService.class);

    String sig = cls.addImport(ConvertsValue.class) + "<" +
      modelName+", " + buffer +
    ">";

    ClassBuffer provider = cls.createInnerClass("private static final class SnippetProvider")
       .addInterface(cls.addImport(Provider.class) + " <" + sig + ">");

    cls
      .createConstructor(Modifier.PUBLIC, cssService+" cssService")
      .println("super(new SnippetProvider(cssService));");

    MethodBuffer ctor = provider.createConstructor(Modifier.PRIVATE, "final "+cssService+" cssService");

    provider.createField(Runnable.class, "init");
    ctor.println("this.init = new "+cls.addImport(Runnable.class)+"() {")
      .indent()
      .println("@Override public void run() {")
      .indent()
      .println("init = NO_OP;");
    PrintBuffer init = new PrintBuffer();
    ctor.addToEnd(init);
    ctor
      .outdent()
      .println("}")
      .outdent()
      .println("};");

    MethodBuffer converter = provider.createMethod("public "+sig+" get()");

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

    DomBuffer variables = new DomBuffer().println("init.run();");
    converter.addToEnd(variables);

    for (String key : renderOrder) {
      for (El el : htmlGen.getElements(key)) {
        for (Import importType : el.imports()) {
          addImport(importType);
        }
        String name = el.tag();
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

        for (String clsName : el.className()) {
          converter.println(".addClassName(\""+clsName+"\")");
        }

        StringBuilder immediateStyle = new StringBuilder();
        StringBuilder sheetStyle = new StringBuilder();
        try {
          fillStyles(immediateStyle, sheetStyle, el.style());
        } catch (Exception e) {
          logger.log(Type.ERROR, "Error calculating styles", e);
          throw new UnableToCompleteException();
        }

        if (immediateStyle.length() > 0) {
          converter.println(".setAttribute(\"style\", \""+Generator.escape(immediateStyle.toString())+"\")");
        }
        if (sheetStyle.length() > 0) {
          init.println("cssService.addCss(\""+Generator.escape(sheetStyle.toString())+"\", 0);");
        }

        for (Property prop : el.properties()) {
          String val = prop.value();
          if (val.startsWith("//")) {
            val = val.substring(2);
          } else if (!val.startsWith("\"")){
            val = "\""+val+"\"";
          }
          converter.println(".setAttribute(\""+prop.name()+"\", "+val+ ")");
        }

        for (String html : el.html()) {
          String escaped = escape(html, key, el.accessor());
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
    return Type.TRACE;
  }
}
