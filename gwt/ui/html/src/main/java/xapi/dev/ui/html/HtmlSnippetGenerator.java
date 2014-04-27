package xapi.dev.ui.html;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import javax.annotation.Generated;
import javax.inject.Named;
import javax.inject.Provider;

import xapi.annotation.common.Property;
import xapi.annotation.compile.Import;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.DomBuffer;
import xapi.dev.source.HtmlBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.source.X_Source;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlSnippet;
import xapi.ui.html.api.Style;
import xapi.util.api.ConvertsValue;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.jjs.UnifyAstView;
import com.google.gwt.user.server.Base64Utils;
import com.google.gwt.util.tools.shared.Md5Utils;

public class HtmlSnippetGenerator extends AbstractHtmlGenerator {

  int ctxCnt;
  
  public static final String generateSnippetProvider(TreeLogger logger, UnifyAstView ast, JClassType templateType, JClassType modelType) throws UnableToCompleteException {
    String simpleName = templateType.getSimpleSourceName()+"_"+modelType.getSimpleSourceName()+"_ToHtml";
    
    // Check if there is an existing type, and that it's generated hashes match our input type.
    String inputHash = toHash(ast, templateType, modelType);
    ExistingResult existingType = findExisting(ast, X_Source.qualifiedName(templateType.getPackage().getName(), simpleName));
    String existingResult = existingTypesUnchanged(logger, ast, existingType, inputHash);
    if (existingResult != null) {
    // If our inputs are unchanged, and the target type exists, just reuse it w/out regenerating
      return existingResult;
    }
    // Generate a new result
    HtmlSnippetGenerator ctx = new HtmlSnippetGenerator(logger, simpleName, ast, templateType, modelType);
    // Save the result, possibly to a new file if there are existing implementations using our default name
    return ctx.saveGeneratedType(logger, ast, existingType, inputHash);
  }

  private HtmlSnippetGenerator(TreeLogger logger, String className, UnifyAstView ast, JClassType templateType, JClassType modelType) throws UnableToCompleteException {
    super(className, templateType, ast);
    
    ClassBuffer cls = out.getClassBuffer();
    String modelName = out.getImports().addImport(modelType.getQualifiedSourceName());
    cls.setSuperClass(
      out.getImports().addImport(HtmlSnippet.class)
      + "<" + modelName + ">"
    );
    
    String buffer = cls.addImport(DomBuffer.class);
    String htmlBuffer = cls.addImport(HtmlBuffer.class);
    
    String sig = cls.addImport(ConvertsValue.class) + "<" +
      modelName+", " + buffer +
    ">";
    
    ClassBuffer provider = cls.createInnerClass("private static final class SnippetProvider")
       .addInterface(cls.addImport(Provider.class) + " <" + sig + ">");
    
    cls
      .createConstructor(Modifier.PUBLIC, htmlBuffer+" buffer")
      .println("super(new SnippetProvider(buffer));");
    
    MethodBuffer ctor = provider.createConstructor(Modifier.PRIVATE, "final "+htmlBuffer+" buffer");
    
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
    if (!"body".equals(documentType)) {
      converter.print("\""+documentType+"\"");
    }
    converter.println( ");");
    
    DomBuffer variables = new DomBuffer().println("init.run();");
    converter.addToEnd(variables);
    
    for (String key : renderOrder) {
      El[] els = allElements.get(key).toArray();
      for (El el : els) {
        for (Import importType : el.imports()) {
          addImport(importType);
        }
        String name = el.tag();
        converter.println("out").indent();
        if (name.length()>0) {
          converter.println(".makeTag(\""+name+"\")");
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
          init.println("buffer.getHead().addCss(\""+Generator.escape(sheetStyle.toString())+"\");");
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
    
  }

}
