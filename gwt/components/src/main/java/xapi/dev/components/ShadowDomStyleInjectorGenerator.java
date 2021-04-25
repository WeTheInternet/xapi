package xapi.dev.components;

import elemental.dom.Element;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.Fifo;
import xapi.collect.api.IntTo;
import xapi.collect.impl.SimpleFifo;
import xapi.components.api.ShadowDomStyle;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.PrintBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.SourceTransform;
import xapi.fu.In1;
import xapi.fu.In2.In2Unsafe;
import xapi.inject.X_Inject;
import xapi.source.X_Modifier;
import xapi.string.X_String;

import static xapi.fu.In2Out1.with2;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/7/16.
 */
public class ShadowDomStyleInjectorGenerator {

  protected class InjectionResult {
    private Class<? extends ClientBundle> bundle;
    private LinkedHashSet<Class<? extends CssResource>> resources;
    private String providerClass;

    public InjectionResult(ShadowDomStyle style, String genClass) {
      providerClass = genClass;
      bundle = style.bundle();
      resources = new LinkedHashSet<>();
      for (Class<? extends CssResource> cls : style.styles()) {
        resources.add(cls);
      }
    }

    public Class<? extends ClientBundle> getBundle() {
      return bundle;
    }

    public String getProviderClass() {
      return providerClass;
    }

    public Set<Class<? extends CssResource>> getResources() {
      return resources;
    }

    protected boolean matches(ShadowDomStyle style) {
      if (style.bundle() != bundle) {
        return false;
      }
      if (style.styles().length != resources.size()) {
        return false;
      }
      final Set<Class<? extends CssResource>> test = new LinkedHashSet<>(resources);
      final Set<Class<? extends CssResource>> seen = new HashSet<>(); // in case someone sends the same class twice...
      for (Class<? extends CssResource> cls : style.styles()) {
          // make sure we ignore duplicate classes
        if (seen.add(cls)) {
          // make sure we have the class you are requesting
          if (!test.remove(cls)) {
            return false;
          }
        }
      }
      // make sure we don't have more classes than you (else we would add extra css, and our signatures would not line up).
      return test.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof InjectionResult))
        return false;

      final InjectionResult that = (InjectionResult) o;

      if (bundle != null ? !bundle.equals(that.bundle) : that.bundle != null)
        return false;
      return resources != null ? resources.equals(that.resources) : that.resources == null;

    }

    @Override
    public int hashCode() {
      int result = bundle != null ? bundle.hashCode() : 0;
      result = 31 * result + (resources != null ? resources.hashCode() : 0);
      return result;
    }

    public String inject(String into) {
      return providerClass + ".inject(" + into + ");";
    }
  }

  private ClassTo<String> resourceProviders = X_Collect.newClassMap(String.class);
  private ClassTo.Many<InjectionResult> cssInjectors = X_Collect.newClassMultiMap(InjectionResult.class);


  public SourceTransform generateShadowStyles(
      TreeLogger logger,
      Fifo<ShadowDomStyle> sharedStyles,
      Fifo<ShadowDomStyle> localStyles,
      GeneratorContext context
  ) {

    Fifo<SourceTransform> injectors = new SimpleFifo<>();

    extractInjectors(logger, sharedStyles, context, injectors::give);
    extractInjectors(logger, localStyles, context, injectors::give);


    return into-> {
      StringBuilder b = new StringBuilder();
      injectors.transform(with2(SourceTransform::transform, into), b::append);
      return b.toString();
    }; // "styleProvider.addTo("+into+")"
  }

  private void extractInjectors(
      TreeLogger logger,
      Fifo<ShadowDomStyle> allStyle,
      GeneratorContext context,
      In1<SourceTransform> callback
  ) {

    Map<ShadowDomStyle, String> missingProviders = new LinkedHashMap<>();
    allStyle.out(style->{
      final IntTo<InjectionResult> existing = cssInjectors.get(style.bundle());
      if (existing.forMatches(
          // test if we match.  Shorthand for injector->injector.matches(style)
          with2(InjectionResult::matches, style),

          item->callback.in(into->into + into + ");"))
      ) {
        return; // returns from this lambda, which is performing iteration.
      }
      // No luck finding an existing match.  Lets start generating...
      final String location = style.resourceInstanceExpression();
      if (location.isEmpty()) {
        // No user-specified injection location; lets find one
        missingProviders.put(style, findResourceInstance(logger, style, context));
      } else {
        // We trust the user to give us a fully qualified expression
        missingProviders.put(style, location);
      }
    });

    // Anything in the missingProviders map must be generated.
    final In2Unsafe<ShadowDomStyle, String> buildProvider = (style, location)->{
      final InjectionResult result = generateInjector(logger, style, context, location);
      cssInjectors.add(style.bundle(), result);
      callback.in(result::inject);
    };
    missingProviders.entrySet().forEach(buildProvider.mapAdapter());

  }

  private InjectionResult generateInjector(
      TreeLogger logger,
      ShadowDomStyle style,
      GeneratorContext context,
      String location
  ) throws UnableToCompleteException {


    Set<String> methodsToUse = new LinkedHashSet<>();
    final Method[] methods = style.bundle().getMethods();
    for (Class<? extends CssResource> cls : style.styles()) {
      boolean success = false;
      for (Method method : methods) {
        if (method.getParameters().length == 0 && cls.isAssignableFrom(method.getReturnType())) {
          methodsToUse.add(method.getName());
          success = true;
        }
      }
      if (!success) {
        logger.log(Type.ERROR, "Unable to find CssResource type " + cls.getCanonicalName()+" in resource class " + style.bundle().getCanonicalName());
        throw new UnableToCompleteException();
      }
    }

    // Alright, we have our list of method names to invoke on the resource to create our css.
    // Lets find/create a provider class.

    String genPkg = style.bundle().getPackage().getName();
    StringBuilder nameBuilder = new StringBuilder(style.bundle().getCanonicalName().replace(genPkg+".", "").replaceAll("[.]", "__"));
    nameBuilder.append("__With");
    methodsToUse.forEach(name->nameBuilder.append("_").append(name));
    String genName = nameBuilder.toString();
    String genClass = (genPkg.isEmpty() ? "" : genPkg + ".") + genName;
    // Search for an existing type...
    final InjectionResult result = new InjectionResult(style, genClass);

    if (context.getTypeOracle().findType(genPkg, genName) != null) {
      return result;
    }

    // Check if it was already generated, but is not yet committed to the TypeOracle.
    final PrintWriter pw = context.tryCreate(logger, genPkg, genName);
    if (pw == null) {
      // The type was already generated, but not yet committed.
      return result;
    }

    // No luck, lets generate!
    SourceBuilder src = new SourceBuilder("public class " + genName)
        .setPackage(genPkg);

    String ele = src.addImport(Element.class);
    String resourceType = src.addImport(result.bundle);
    // TODO: right now, you must have this on your classpath and in your gwt module....
    // Lets move this out into a leaner module (like this one)
    // Or have our method supply a StyleService, and just use that.
    String X_Elemental = src.addImport("xapi.elemental.X_Elemental");
    List<String> styleClasses = new ArrayList<>();
    for (Class<? extends CssResource> cls : style.styles()) {
      styleClasses.add(src.addImport(cls));
    }
    In1<PrintBuffer> printResourceClasses = o->
        styleClasses.forEach(type->
          o .print(", ")
            .print(type)
            .print(".class")
        );
    // create a static block of code to register our css with the elemental service
    // we may later move the service to a method parameter, and use RunOnce semantics to enable just-in-time injection.

    final ClassBuffer out = src.getClassBuffer();
    out
        .println("static {")
        .indent()
        .println(resourceType + " res = " + location + ";")
        .println("String css = \"\";")
    ;
    methodsToUse.forEach(name->{
      out
          .println("css += res." + name + "().getText();")
//          .indentln(".replaceAll(\"[\\\\\\\\]00\", \"\\\\\\\\\");")
      ;
    });
    out
        .print(X_Elemental)
        .print(".getElementalService().registerStyle(")
        .print(resourceType)
        .print(".class, css")
    ;
    printResourceClasses.in(out);

    out
        .println(");")
        .outdent()
        .println("}")
      ;

    final MethodBuffer inject = out.createMethod("public static void inject(" + ele + " into)");
    inject
        .println("into.appendChild(")
        .indent()
        .print(X_Elemental)
        .print(".getElementalService().injectStyle(")
        .print(resourceType)
        .print(".class")
    ;
    printResourceClasses.in(inject);
    inject
        .println(")")
        .outdent()
        .println(");");

    // All done!  let's write out and commit our result

    String code = src.toString();
    pw.append(code);
    context.commit(logger, pw);



    return result;
  }

  private String findResourceInstance(TreeLogger logger, ShadowDomStyle style, GeneratorContext context) {
    final Class<? extends ClientBundle> cls = style.bundle();
    String location = resourceProviders.get(cls);
    if (X_String.isNotEmpty(location)) {
      return location;
    }
    for (Field field : cls.getFields()) {
      if (field.getType() == cls) {
        // Lets use this field.
        return cls.getCanonicalName() + "." + field.getName();
      }
    }
    for (Method method : cls.getMethods()) {
      if (X_Modifier.isStatic(method.getModifiers()) && method.getReturnType() == cls) {
        // Lets use this static method.
        return cls.getCanonicalName() + "." + method.getName();
      }
    }
    // Now that we have tried exact matches, lets try for assignability matches
    for (Field field : cls.getFields()) {
      if (cls.isAssignableFrom(field.getType())) {
        // Lets use this field.
        return cls.getCanonicalName() + "." + field.getName();
      }
    }
    for (Method method : cls.getMethods()) {
      if (X_Modifier.isStatic(method.getModifiers()) && cls.isAssignableFrom(method.getReturnType())) {
        // Lets use this static method.
        return cls.getCanonicalName() + "." + method.getName();
      }
    }

    // Ok, no providers available on fields.  We are going to have to generate a provider instance...
    // Luckily, X_Inject.singleton will do this nicely for us, creating a single instance backed by GWT.create
    location = X_Inject.class.getCanonicalName() + ".singleton(" + cls.getCanonicalName()+".class)";
    resourceProviders.put(cls, location);
    return location;
  }
}
