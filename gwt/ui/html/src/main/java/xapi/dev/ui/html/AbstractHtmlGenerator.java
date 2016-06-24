package xapi.dev.ui.html;

import xapi.annotation.compile.Generated;
import xapi.annotation.compile.Import;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.source.SourceBuilder;
import xapi.except.NotYetImplemented;
import xapi.source.X_Source;
import xapi.source.write.MappedTemplate;
import xapi.source.write.StringerMatcher;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlTemplate;
import xapi.ui.html.api.Style;
import xapi.util.X_String;
import xapi.util.api.ConvertsValue;
import xapi.util.impl.LazyProvider;

import static xapi.collect.X_Collect.newStringMap;
import static xapi.ui.html.api.HtmlSnippet.appendTo;

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

import javax.inject.Provider;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Predicate;

public abstract class AbstractHtmlGenerator <Ctx extends HtmlGeneratorResult> implements CreatesContextObject<Ctx> {

  protected static final String KEY_FROM = "from";

  private static final Provider<Boolean> isDev = new LazyProvider<>(new Provider<Boolean>() {
    @Override
    public Boolean get() {
      return true;
    }
  });

  protected static <Ctx extends HtmlGeneratorResult> Ctx existingTypesUnchanged(final TreeLogger logger,
      final UnifyAstView ast, final Ctx result, final String verify) {
    if (isDev.get()) {
      // During development, never reuse existing types, as we're likely changing generators
      return null;
    }
    try {
      if (result.getSourceType() == null) {
        return null;
      }
      final Generated gen = result.getSourceType().getAnnotation(Generated.class);
      if (gen == null) {
        return null;
      }
      final String hash = gen.value()[gen.value().length-1];
      if (verify.equals(hash)) {
        return result;
      }
    } catch (final Exception e) {
      logger.log(Type.WARN, "Unknown error calculating change hashes", e);
    }
    return null;
  }

  protected static String toHash(final UnifyAstView ast, final String ... types) throws UnableToCompleteException {
    final StringBuilder b = new StringBuilder();
    for (final String type : types) {
      b.append(ast.searchForTypeBySource(type).toSource());
    }
    return Base64Utils.toBase64(Md5Utils.getMd5Digest(b.toString().getBytes()));
  }

  protected String clsName;

  protected String documentType;

  protected final HtmlGeneratorContext htmlGen;

  protected final SourceBuilder<UnifyAstView> out;

  protected boolean renderAllChildren;

  protected String[] renderOrder;

  public AbstractHtmlGenerator(final String clsName, final JClassType templateType, final UnifyAstView ast) {
    this.clsName = clsName;
    htmlGen = new HtmlGeneratorContext(templateType);
    this.out = new SourceBuilder<UnifyAstView>("public class "+clsName)
      .setPackage(templateType.getPackage().getName())
      .setPayload(ast);
  }
  protected void addEl(final String name, final El el) {
    htmlGen.addEl(name, el);
  }

  protected void addHtml(final String name, final Html html, final IntTo<String> elOrder) {
    if (html == null) {
      return;
    }
    elOrder.add(name);
  }

  public void addImport(final Import importType) {
    if (importType.staticImport().length()==0) {
      out.getImports().addImport(importType.value());
    } else {
      out.getImports().addStaticImport(importType.value(), importType.staticImport());
    }
  }
  protected void clear() {
    htmlGen.clear();
  }
  /**
   * @param key
   * @param accessor
   * @param template
   * @return
   */
  protected ConvertsValue<String, String> createProvider(final String key,
      final String accessor, final HtmlTemplate template) {
    switch (key) {
    case HtmlTemplate.KEY_VALUE:
      return new ConvertsValue<String, String>() {
        @Override
        public String convert(final String from) {
          if (accessor.equals(El.DEFAULT_ACCESSOR)) {
            return "".equals(key) ? KEY_FROM :
              KEY_FROM+"."+key+(key.endsWith("()") ? "" : "()");
          } else {
            return accessor;
          }
        }
      };
    default:
      return new ConvertsValue<String, String>() {
        @Override
        public String convert(final String from) {
          return from.startsWith(key) ?
            from.substring(key.length()) :
              from;
        }
      };
    case HtmlTemplate.KEY_CHILDREN:
    case HtmlTemplate.KEY_PARENT:
    case HtmlTemplate.KEY_CONTEXT:
      throw new NotYetImplemented("Key "+key+" not yet implemented in "+getClass()+".createProvider()");

    }
  }

  /**
   * @param text
   * @return
   */
  protected String escape(final String text) {
    return Generator.escape(text);
  }

  @SuppressWarnings("unchecked")
  protected String escape(String text, final String key, final String accessor) {
    if (text.length() == 0) {
      return "";
    }

    final HtmlGeneratorNode node = htmlGen.allNodes.get(key);
    if (node.hasTemplates()) {
      final StringTo<Object> references = newStringMap(Object.class);
      for(final HtmlTemplate template : node.getTemplates()) {
        final String name = template.name();
        if (template.inherit() && template.name().length() == 0) {
          continue;
        }
        if (!references.containsKey(name)) {
          references.put(name.equals("") ? "$this" : name,
              createProvider(key, accessor, template));
        }
      }
      if (!references.isEmpty()) {
        final StringerMatcher matcher = new StringerMatcher() {
          @Override
          public String toString(final Object o) {
            return escape(String.valueOf(o));
          }

          @Override
          public Predicate<String> matcherFor(String value) {
            return null;
          }
        };
        final MappedTemplate apply = new MappedTemplate(text, matcher, references.keyArray()) {
          @Override
          protected Object retrieve(final String key, final Object object) {
            final ConvertsValue<String, String> converter = (ConvertsValue<String, String>) object;
            return converter.convert(key);
          }
        };
        text = apply.applyMap(references.entries());
      }
    }

    return replace$value(text, key, accessor);
  }

  protected String replace$value(String text, final String key, final String accessor) {
    String ref = null;
    int ind = text.indexOf("$value");
    if (ind == -1) {
      return "\""+escape(text)+"\"";
    }
    final StringBuilder b = new StringBuilder("\"");
    int was;
    for (;;){
      was = ind;
      ind = text.indexOf("$value");
      if (ind == -1) {
        break;
      }
      if (ref == null) {
        if (accessor.equals(El.DEFAULT_ACCESSOR)) {
          ref = getDefaultKey() + ("".equals(key) ? "" : "."+key+"()");
        } else {
          ref = accessor;
        }
      }
      if (ind > 0) {
        b.append(escape(text.substring(0, ind)));
      }
      if ("".equals(ref)) {
        b.append("\"");
      } else {
        b.append("\" + "+ ref + " + \"");
      }
      text = text.substring(6);
    }
    if (was > -1) {
      b.append(escape(text));
    }
    b.append("\"");
    return b.toString();
  }

  private String getDefaultKey() {
    return KEY_FROM;
  }

  protected void fillMembers(JClassType templateType, final IntTo<String> elOrder) {
    for (final JClassType type : templateType.getFlattenedSupertypeHierarchy()) {
      for (final JMethod method : type.getMethods()) {
        if (method.isAnnotationPresent(Html.class) || method.isAnnotationPresent(El.class)) {
          final String name = toSimpleName(method);
          if (!elOrder.contains(name)) {
            elOrder.add(name);
//            Html html = method.getAnnotation(Html.class);
//            addHtml(name, html, elOrder);
//            addEl(name, method.getAnnotation(El.class));
            htmlGen.addCss(name, method.getAnnotation(Css.class));
            htmlGen.addImport(name, method.getAnnotation(Import.class));
            htmlGen.addMethod(name, method);
          }
        }
      }
    }

    templateType = templateType.getSuperclass();
    if (templateType != null && !templateType.getQualifiedSourceName().equals("java.lang.Object")) {
      fillMembers(templateType, elOrder);
    }
  }

  protected static int fillStyles(final Appendable immediateStyle,
                                  final Appendable sheetStyle, final Appendable extraStyle, final Style... styles) throws IOException {
    int priority = Integer.MAX_VALUE;
    for (final Style style : styles) {
      priority = Math.min(priority, style.priority());
      final String[] names = style.names();
      if (names.length == 0) {
        if (immediateStyle != null) {
          String extra = appendTo(immediateStyle, style);
          if (X_String.isNotEmpty(extra)) {
            extraStyle.append(extra).append("\n");
          }
        }
      } else if (sheetStyle != null){
        for (int i = 0, m = names.length; i < m; i++) {
          if (i > 0) {
            sheetStyle.append(", ");
          }
          sheetStyle.append(names[i]);
        }
        sheetStyle.append("{\n");
        String extra = appendTo(sheetStyle, style);
        sheetStyle.append("\n}\n");
        if (X_String.isNotEmpty(extra)) {
          extraStyle.append(extra).append("\n");
        }
      }
    }
    return priority;
  }

  protected static <Ctx> Ctx findExisting(final UnifyAstView ast, final CreatesContextObject<Ctx> creator, final String pkgName, String name) {
    if (name.indexOf('.') == -1) {
      name = X_Source.qualifiedName(pkgName, name);
    }
    JClassType existing = ast.getTypeOracle().findType(name), winner = null;
    int pos = 0;
    while (true) {
      winner = existing;
      final String next = name+pos++;
      existing = ast.getTypeOracle().findType(next);
      if (existing == null) {
        return creator.newContext(winner, pkgName, name);
      }
    }
  }

  public Iterable<El> getElements(final String key) {
    return htmlGen.allNodes.get(key).getElements();
  }

  protected Type getLogLevel() {
    return Type.DEBUG;
  }

  public Iterable<HtmlTemplate> getTemplates(final String key) {
    return htmlGen.allNodes.get(key).getTemplates();
  }

  protected void initialize() {

//    clear();

    renderAllChildren = true;
    renderOrder = new String[0];
    documentType = Html.ROOT_ELEMENT;
    final IntTo<String> elOrder = X_Collect.newList(String.class);
    Html html = null;
    for (final JClassType type : htmlGen.cls.getFlattenedSupertypeHierarchy()) {
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
      addHtml("", html, elOrder);
      renderAllChildren = html.renderOrder().length == 0;
    }
    fillMembers(htmlGen.cls, elOrder);

    if (renderAllChildren) {
      renderOrder = elOrder.toArray();
    }
  }

  protected static <Ctx extends HtmlGeneratorResult> Ctx saveGeneratedType(
      final TreeLogger logger, final Type logLevel, final Class<?> generatorClass, final UnifyAstView ast,
      final SourceBuilder<?> out, final Ctx result, final String inputHash) throws UnableToCompleteException {
    final String name = result.getFinalName();
    String src = out.toString();
    final String digest =
        Base64Utils.toBase64(Md5Utils.getMd5Digest(src.getBytes()));

    if (result.getSourceType() != null) {
      // Only use the existing class if the generated source exactly matches what we just generated.
      final Generated gen = result.getSourceType().getAnnotation(Generated.class);
      if (gen != null && gen.value()[1].equals(digest)) {
        return result;
      }
    }
    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    out.getClassBuffer().setSimpleName(name.replace(out.getPackage()+".", ""));
    out.getClassBuffer().addAnnotation("@"+
    out.getImports().addImport(Generated.class)+"("+
    "date=\""+df.format(new Date())+"\",\n" +
        "value={\"" + generatorClass.getName()+"\","+
        "\""+digest+"\", \""+inputHash+"\"})");
    final StandardGeneratorContext gen = ast.getGeneratorContext();
    final PrintWriter pw = gen.tryCreate(logger, out.getPackage(), out.getClassBuffer().getSimpleName());
    src = out.toString();
    pw.print(src);
    gen.commit(logger, pw);

    if (logger.isLoggable(logLevel)) {
      logger.log(logLevel, src);
    }
    try {
      return result;
    } finally {
      out.destroy();
    }
  }

  protected String toSimpleName(final JMethod method) {
//    if (method.isAnnotationPresent(Named.class))
//      return method.getAnnotation(Named.class).value();
    return method.getName();
  }

  protected String toSimpleName(final String name) {
//    if (name.startsWith("get") || name.startsWith("has")) {
//      if (name.length() > 3 && Character.isUpperCase(name.charAt(3)))
//        return Character.toLowerCase(name.charAt(3)) +
//            (name.length() > 4 ? name.substring(4) : "");
//    } else if (name.startsWith("is")) {
//      if (name.length() > 2 && Character.isUpperCase(name.charAt(2)))
//        return Character.toLowerCase(name.charAt(2)) +
//            (name.length() > 3 ? name.substring(3) : "");
//    }
    return name;
  }

}
