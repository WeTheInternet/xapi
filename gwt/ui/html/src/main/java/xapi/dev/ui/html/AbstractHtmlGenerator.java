package xapi.dev.ui.html;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.annotation.Generated;
import javax.inject.Named;

import xapi.annotation.compile.Import;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo.Many;
import xapi.dev.source.SourceBuilder;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlSnippet;
import xapi.ui.html.api.Style;

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

public class AbstractHtmlGenerator {

  private static final String DEFAULT_BODY_TYPE = "body";

  public static class ExistingResult {
    public ExistingResult(JClassType existing, String name) {
      this.existing = existing;
      this.name = name;
    }
    public JClassType existing;
    public String name;
  }
  
  protected String saveGeneratedType(TreeLogger logger, UnifyAstView ast, ExistingResult result, String inputHash) throws UnableToCompleteException {
    String name = result.name;
    String src = out.toString();
    final String digest = 
        Base64Utils.toBase64(Md5Utils.getMd5Digest(src.getBytes()));
    
    if (result.existing != null) {
      // Only use the existing class if the generated source exactly matches what we just generated.
      Generated gen = result.existing.getAnnotation(Generated.class);
      if (gen.value()[1].equals(digest)) {
        return result.existing.getQualifiedSourceName();
      }
    }
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    out.getClassBuffer().setSimpleName(name.replace(out.getPackage()+".", ""));
    out.getClassBuffer().addAnnotation("@"+
        out.getImports().addImport(Generated.class)+"("+
        "date=\""+df.format(new Date())+"\",\n" +
            "value={\"" + getClass().getName()+"\","+
            "\""+digest+"\", \""+inputHash+"\"})");
    StandardGeneratorContext gen = ast.getGeneratorContext();
    PrintWriter pw = gen.tryCreate(logger, out.getPackage(), out.getClassBuffer().getSimpleName());
    src = out.toString();
    pw.print(src);
    gen.commit(logger, pw);
    gen.finish(logger);
    if (logger.isLoggable(getLogLevel())) {
      logger.log(getLogLevel(), src);
    }
    try {
      return name;
    } finally {
      clear();
      out.destroy();
    }
  }

  protected Type getLogLevel() {
    return Type.TRACE;
  }

  protected static ExistingResult findExisting(UnifyAstView ast, String name) {
    JClassType existing = ast.getTypeOracle().findType(name), winner = null;
    int pos = 0;
    while (true) {
      winner = existing;
      String next = name+pos++;
      existing = ast.getTypeOracle().findType(next);
      if (existing == null) {
        return new ExistingResult(winner, name);
      } else {
        name = next;
      }
    }
  }

  protected static String toHash(UnifyAstView ast, JClassType ... types) {
    StringBuilder b = new StringBuilder();
    for (JClassType type : types) {
      b.append(ast.searchForTypeBySource(type.getQualifiedSourceName()).toSource());
    }
    return Base64Utils.toBase64(Md5Utils.getMd5Digest(b.toString().getBytes()));
  }
  
  protected static String existingTypesUnchanged(TreeLogger logger,
      UnifyAstView ast, ExistingResult result, String verify) {
    try {
      if (result.existing == null) {
        return null;
      }
      Generated gen = result.existing.getAnnotation(Generated.class);
      String hash = gen.value()[gen.value().length-1];
      if (verify.equals(hash)) {
        return result.existing.getQualifiedSourceName();
      }
    } catch (Exception e) {
      logger.log(Type.WARN, "Unknown error calculating change hashes", e);
    }
    return null;
  }
  
  protected String clsName;
  protected final SourceBuilder<UnifyAstView> out;
  
  @SuppressWarnings("unchecked")
  protected final Many<Class<?>> allTemplates = X_Collect.newStringMultiMap(Class.class.cast(Class.class));
  protected final Many<El> allElements = X_Collect.newStringMultiMap(El.class);
  protected final Many<Css> allCss = X_Collect.newStringMultiMap(Css.class);

  protected boolean renderAllChildren;
  protected String[] renderOrder;
  protected String documentType;
  
  public AbstractHtmlGenerator(String clsName, JClassType templateType, UnifyAstView ast) {
    this.clsName = clsName;
    this.out = new SourceBuilder<UnifyAstView>("public class "+clsName)
      .setPackage(templateType.getPackage().getName())
      .setPayload(ast);
    
    clear();
    
    renderAllChildren = true;
    renderOrder = new String[0];
    documentType = DEFAULT_BODY_TYPE;
    IntTo<String> elOrder = X_Collect.newList(String.class);
    Html html = null;
    for (JClassType type : templateType.getFlattenedSupertypeHierarchy()) {
      if (html == null && type.isAnnotationPresent(Html.class)) {
        html = type.getAnnotation(Html.class);
      }
      if (type.isAnnotationPresent(Import.class)) {
        addImport(type.getAnnotation(Import.class));
      }
    }
    if (html != null) {
      documentType = html.document();
      renderOrder = html.renderOrder();
      addHtml(html, "", elOrder);
      renderAllChildren = html.renderOrder().length == 0;
    }
    fillMembers(templateType, elOrder);
    
    for (String name : allTemplates.keys()) {
      IntTo<Class<?>> templates = allTemplates.get(name);
      for (Class<?> cls : templates.forEach()) {
        html = cls.getAnnotation(Html.class);
        if (html != null) {
          addHtml(html, name, elOrder);
        }
        El el = cls.getAnnotation(El.class);
        if (el != null) {
          addEl(name, el);
        }
      }
    }
    
    if (renderAllChildren) {
      renderOrder = elOrder.toArray();
    }
  }

  protected void clear() {
    allCss.clear();
    allTemplates.clear();
    allElements.clear();
  }

  protected String escape(String html, String key, String accessor) {
    if (html.length() == 0) {
      return "";
    }
    String ref = null;
    int ind = html.indexOf("$value");
    if (ind == -1) {
      return "\""+Generator.escape(html)+"\"";
    }
    StringBuilder b = new StringBuilder("\"");
    int was;
    for (;;){
      was = ind;
      ind = html.indexOf("$value");
      if (ind == -1) {
        break;
      }
      if (ref == null) {
        if (accessor.equals(El.DEFAULT_ACCESSOR)) {
          ref = "".equals(key) ? "from" : "from."+key+"()";
        } else {
          ref = accessor;
        }
      }
      if (ind > 0) {
        b.append(Generator.escape(html.substring(0, ind)));
      }
      if ("".equals(ref)) {
        b.append("\"");
      } else {
        b.append("\" + "+ ref + " + \"");
      }
      html = html.substring(6);
    }
    if (was > -1) {
      b.append(Generator.escape(html));
    }
    b.append("\"");
    return b.toString();
  }

  protected void addHtml(Html html, String name, IntTo<String> elOrder) {
    if (html == null) {
      return;
    }
    for (Css css : html.css()) {
      allCss.get(Integer.toString(css.priority())).add(css);
    }
    allElements.get(name).addAll(html.body());
    elOrder.add(name);
  }

  protected void fillMembers(JClassType templateType, IntTo<String> elOrder) {
    for (JClassType type : templateType.getFlattenedSupertypeHierarchy()) {
      for (JMethod method : type.getMethods()) {
        if (method.isAnnotationPresent(Html.class) || method.isAnnotationPresent(El.class)) {
          String name = toSimpleName(method);
          if (!elOrder.contains(name)) {
            elOrder.add(name);
            Html html = method.getAnnotation(Html.class);
            addHtml(html, name, elOrder);
            addEl(name, method.getAnnotation(El.class));
          }
        }
      }
    }
  
    
    templateType = templateType.getSuperclass();
    if (templateType != null && !templateType.getQualifiedSourceName().equals("java.lang.Object")) {
      fillMembers(templateType, elOrder);
    }
  }

  protected void addEl(String name, El el) {
    if (el != null) {
      allElements.get(name).add(el);
    }
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
  
  protected void fillStyles(Appendable immediateStyle,
      Appendable sheetStyle, Style[] styles) throws IOException {
    for (Style style : styles) {
      String[] names = style.names();
      if (names.length == 0) {
        HtmlSnippet.appendTo(immediateStyle, style);
      } else {
        for (int i = 0, m = names.length; i < m; i++) {
          if (i > 0) {
            sheetStyle.append(", ");
          }
          sheetStyle.append(names[i]);
        }
        sheetStyle.append("{\n");
        HtmlSnippet.appendTo(sheetStyle, style);
        sheetStyle.append("\n}\n");
      }
    }
  }

  public void addImport(Import importType) {
    if (importType.staticImport().length()==0) {
      out.getImports().addImport(importType.value());
    } else {
      out.getImports().addStatic(importType.value(), importType.staticImport());
    }
  }

  
}
