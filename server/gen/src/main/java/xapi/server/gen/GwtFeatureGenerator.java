package xapi.server.gen;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.JsonContainerExpr;
import com.github.javaparser.ast.expr.JsonPairExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.dev.api.ApiGeneratorContext;
import xapi.dev.api.ClasspathProvider;
import xapi.dev.gwtc.api.GwtcProjectGenerator;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.source.MethodBuffer;
import xapi.dev.ui.api.ComponentBuffer;
import xapi.dev.ui.api.ContainerMetadata;
import xapi.dev.ui.api.ContainerMetadata.MetadataRoot;
import xapi.dev.ui.api.UiComponentGenerator;
import xapi.dev.ui.api.UiFeatureGenerator;
import xapi.dev.ui.api.UiVisitScope;
import xapi.dev.ui.impl.UiGeneratorTools;
import xapi.except.NotYetImplemented;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;
import xapi.fu.iterate.ArrayIterable;
import xapi.fu.iterate.Chain;
import xapi.fu.iterate.ChainBuilder;
import xapi.gwtc.api.GwtManifest;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.scope.X_Scope;
import xapi.server.api.ModelGwtc;
import xapi.server.api.Route;
import xapi.server.api.Route.RouteType;
import xapi.server.gen.WebAppComponentGenerator.WebAppGeneratorScope;
import xapi.source.X_Source;
import xapi.util.X_String;
import xapi.util.X_Util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public class GwtFeatureGenerator extends UiFeatureGenerator {

    private final WebAppComponentGenerator owner;
    private final WebAppGeneratorScope scope;

    public GwtFeatureGenerator(WebAppComponentGenerator owner, WebAppGeneratorScope scope) {
        this.owner = owner;
        this.scope = scope;
    }

    @Override
    public UiVisitScope startVisit(
        UiGeneratorTools service,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata container,
        UiAttrExpr attr
    ) {
        if (attr.getExpression() instanceof JsonContainerExpr) {
            JsonContainerExpr array = (JsonContainerExpr) attr.getExpression();
            if (!array.isArray()) {
                throw new IllegalArgumentException("gwt feature only accepts array json types; you sent " + service.debugNode(array));
            }
            array.getValues().forAll(expr->{
               if (!(expr instanceof UiContainerExpr)) {
                 throw badGwtc(service, expr);
               }
               addCompilation(service, generator, source, container, (UiContainerExpr) expr);
            });
        } else if (attr.getExpression() instanceof UiContainerExpr) {
            addCompilation(service, generator, source, container, (UiContainerExpr) attr.getExpression());
        } else {
            // No other types supported at this time...  Consider allowing a typename for a class containing an @Gwtc annotation,
            // or for a name of gwt.xml to locate on the classpath... though, that would be strange to put here, where
            // compile definitions go (you'd want to shorthand where compile invocations go, such as `script = $gwt(com.fu.MyModule)`
            throw badGwtc(service, attr.getExpression());
        }
        return UiVisitScope.FEATURE_NO_CHILDREN;
    }

    private IllegalArgumentException badGwtc(UiGeneratorTools service, Expression expression) {
        final String msg = "gwt feature only supports element <gwtc /> or array [ <gwtc />, <gwtc /> ] expressions";
        X_Log.error(GwtFeatureGenerator.class, msg, "you sent", service.debugNode(expression));
        return new IllegalArgumentException(msg);
    }

    private void addCompilation(
        UiGeneratorTools service,
        UiComponentGenerator generator,
        ComponentBuffer source,
        ContainerMetadata container,
        UiContainerExpr gwtc
    ) {
        final ApiGeneratorContext ctx = source.getRoot().getContext();
        final Maybe<UiAttrExpr> attrModule = gwtc.getAttribute("module");
        final Maybe<UiAttrExpr> attrName = gwtc.getAttribute("name");
        final Maybe<UiAttrExpr> attrClasspath = gwtc.getAttribute("classpath");
        final Maybe<UiAttrExpr> attrSource = gwtc.getAttribute("source");
        final Maybe<UiAttrExpr> attrInherit = gwtc.getAttribute("inherit");
        final String module = attrModule
            .mapIfAbsent(()->attrModule.getOrThrow(()->new IllegalArgumentException("A <gwtc /> element must have at least a name or module")))
            .mapNullSafe(UiAttrExpr::getExpression)
            .mapNullSafe(e->service.resolveString(ctx, e))
            .get();
        final Maybe<String> moduleShortName = gwtc.getAttribute("moduleShortName")
            .mapNullSafe(UiAttrExpr::getExpression)
            .mapNullSafe(e -> service.resolveString(ctx, e));
        final String name = attrName
            .mapNullSafe(WebAppGenerator::getExpressionSerialized, service, ctx)
            .get();

        final Expression classpath = attrClasspath
            .mapNullSafe(UiAttrExpr::getExpression)
            .getOrThrow(() -> new IllegalArgumentException("A <gwtc /> element must have a classpath feature"));

        final List<String> sources = attrSource
            .mapNullSafe(UiAttrExpr::getExpression)
            .mapNullSafe(ASTHelper::extractStrings)
            .ifAbsentSupply(()-> Collections.emptyList());

        // Hokay!  The classpath and sources we'll want to handle directly, as they don't map straight into GwtManifest,
        // however, all the rest of the settings will be applied through reflection.  Less hand written code, less errors.

        final MethodBuffer mb = owner.getScope().getInstallMethod();
        final ContainerMetadata root = source.getRoot();
        final String var = root.newVarName("gwtc" + X_Source.javaSafeName(name));
        final String comp = root.newVarName("compiler" + X_Source.javaSafeName(name));
        final String proj = root.newVarName("project" + X_Source.javaSafeName(name));
        final String mod = root.newVarName("module" + X_Source.javaSafeName(name));
        final String man = source.getRoot().newVarName("manifest" + X_Source.javaSafeName(name));
        final String model = mb.addImport(ModelGwtc.class);
        final String manifest = mb.addImport(GwtManifest.class);
        final String compiler = mb.addImport(GwtcService.class);
        final String project = mb.addImport(GwtcProjectGenerator.class);
        final String create = mb.addImportStatic(X_Model.class, "create");

        mb.println("final String " + mod + " = \"" + module + "\";");
        mb.println("final " + model + " " + var + " = app.getGwtModule(" + mod + ");");
        mb.println("final " + compiler + " " + comp + " = " + var + ".getOrCreateService();");
        mb.println("final " + project + " " + proj + " = " + comp + ".getProject(" + mod + ");");
        mb.println("final " + manifest + " " + man + " = " + proj + ".getManifest();");
        mb.println(var + ".setManifest(" + man + ");");

            final String moduleName = X_Util.firstNotNull(module,name);
        if (attrInherit.isPresent()) {
            final IntTo<String> inherits = service.resolveToLiterals(ctx, attrInherit.get().getExpression());
            for (String inherit : inherits) {
                mb.println(proj + ".addGwtInherit(\"" + inherit + "\");");
            }
        }

        findEntryPoints(name, module, gwtc).forAll(
            cls ->
                mb.println(proj + ".addClass(" + mb.addImport(cls) + ".class);")
        );
        findGwtXmls(name, module, gwtc).forAll(
            cls ->
                mb.println(proj + ".addGwtInherit(\"" + cls + "\");")
        );
        if (module != null || name != null) {
            mb.println(man + ".setModuleName(\"" + moduleName + "\");");
        }
        if (moduleShortName.isPresent()) {
            mb.println(man + ".setModuleShortName(\"" + moduleShortName + "\");");
        } else if (name != null) {
            mb.println(man + ".setModuleShortName(\"" + name + "\");");
        }

        // Lets add sources first
        for (String s : sources) {
            // TODO: allow specifying other web-app components as source?
            mb.println(man+".addSource(\"" + X_Source.escape(s) + "\");");
        }
        includeClasspath(service, ctx, mb, man, classpath);


        Class<GwtManifest> cls = GwtManifest.class;
        boolean hadPort = false;
        for (Field field : cls.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())){
                continue;
            }
            String setterName = "set" + X_String.toTitleCase(field.getName());
            try {
                cls.getMethod(setterName, field.getType());
            } catch (NoSuchMethodException ignored) {
                continue;
            }
            final Maybe<UiAttrExpr> attr = gwtc.getAttribute(field.getName());
            if (attr.isAbsent()) {
                continue;
            }
            if ("setPort".equals(setterName)) {
                hadPort = true;
            }
            // Next, add this field to the generated output, formatting it based on the field type.
            printManifestSetter(service, ctx, mb, man, field.getType(), setterName, attr.get());
        }
        if (!hadPort) {
            mb.println(man+".setPort(app.getPort());");
        }

        /*

        manifest.setLogLevel(Type.TRACE);
        manifest.setSourceLevel(SourceLevel.JAVA8);
        manifest.setPort(app.getPort());
        manifest.setModuleName("WTI");
        manifest.setUseCurrentJvm(true);
        manifest.setRecompile(true);

        manifest.setObfuscationLevel(ObfuscationLevel.PRETTY);
        manifest.setMethodNameMode(MethodNameMode.ABBREVIATED);
        manifest.setCleanupMode(CleanupMode.NEVER_DELETE);
        */
        mb.println("app.getOrCreateGwtModules().put(\"" + name + "\", " + var + ");");

        // Now, add the route:
        String routeName = root.newVarName("route" + name);
        String route = mb.addImport(Route.class);
        mb.println(route + " " + routeName + " = " + create + "(" + route + ".class);");
        mb.println(routeName + ".setPath(\"/**/" + name + "/*\");");
        mb.println(routeName + ".setRouteType(" + mb.addImport(RouteType.class) + ".Gwt);");
        mb.println(routeName + ".setPayload(\"" + name + "\");");
        mb.println("app.getRoute().add(" + routeName + ");");
        /*

    Route gwtcRoute = X_Model.create(Route.class);
    gwtcRoute.setPath("/* * /XapiLang/*");
    gwtcRoute.setRouteType(RouteType.Gwt);
    gwtcRoute.setPayload("XapiLang");
    app.getRoute().add(gwtcRoute);
        */

    }

    private void includeClasspath(
        UiGeneratorTools service,
        ApiGeneratorContext ctx,
        MethodBuffer mb,
        String manifest,
        Expression sourceExpr
    ) {
        // Now, dependencies... quite a bit trickier, since we want quite a bit more than just file system args.
        // This includes: References to named classpath, $maven("group:artifact:id") coordinates,
        // and, yes, (someday) values dependent on runtime info, like `$user.workDir/some/path/to.jar`
        // For now, we'll only handle the first two, since we actually need them right now.
        // The rest of the magic is just a nicety that we won't need until CollIDE is upgraded
        final Expression expr = service.resolveVar(ctx, sourceExpr);
        if (expr instanceof JsonContainerExpr) {
            final JsonContainerExpr json = (JsonContainerExpr) expr;
            if (!json.isArray()) {
                throw new IllegalArgumentException("gwt classpath does not support json object; you sent " + service.debugNode(sourceExpr) + " resolved to " + service.debugNode(expr));
            }
            // hokay, for a json array, just iterate items and include each (through recursion).
            for (JsonPairExpr pair : json.getPairs()) {
                includeClasspath(service, ctx, mb, manifest, pair.getValueExpr());
            }
            return;
        }

        if (expr instanceof MethodCallExpr) {
            // if we have a method call, the only ones we support are $maven() and $classpath();
            // all other $references that are still unresolved will be considered runtime variables,
            // and we will have to emit runtime producers that defer until the moment before we
            // actually need the classpath to run to check the runtime scope for variable values.
            final MethodCallExpr call = (MethodCallExpr) expr;
            if (call.getName().equals("$maven")) {

                String depField = owner.getScope().getInstallLayer().newFieldName("dep");
                String depType = mb.addImport("xapi.maven.model.MvnDependency");
                String modelCreate = mb.addImport(X_Model.class) + ".create";
        /*
            final MvnDependency dep = X_Model.create(MvnDependency.class);
            final Out1<Iterable<String>> deps = X_Maven.downloadDependencies(dep);
        */

            } else if (call.getName().equals("$classpath")) {
                // definition of classpath might exist in another file;
                // lets defer processing these until the current generator pass is complete.
                // we are not emitting any state anyone cares about here; so long as we complete
                // before the return statement is printed, we should be fine :-)
                assert call.getArgs().size() == 1 : "$classpath expects exactly one argument";
                final Expression cp = service.resolveVar(ctx, call.getArg(0));
                String cpName = service.resolveString(ctx, cp);
                final Maybe<GeneratedClasspathInfo> classpath = owner.getGenerator().getClasspath(cpName);
                if (classpath.isPresent()) {
                    addClasspath(service, ctx, manifest, mb, classpath.get());
                } else {
                    service.getGenerator().onFinish(WebAppGenerator.PRIORITY_RESOLVE_GWT, ()->{
                        // hokay... all classpaths should have been visited by now.
                        if (classpath.isAbsent()) {
                            assert owner.getGenerator().getClasspath(cpName).isAbsent();
                            throw new IllegalArgumentException("Cannot find classpath with name " + cpName);
                        }
                        addClasspath(service, ctx, manifest, mb, classpath.get());
                    });
                }
                return;

            } else {
                throw new IllegalArgumentException("GwtFeatureGenerator cannot handle classpath element " + service.debugNode(sourceExpr) + " resolved to " + service.debugNode(expr));
            }
        }

        // Alright, not an array, not a method, must be a string.  Lets have a peak...
        String item = service.resolveString(ctx, expr);

        if (looksLikeCoordinate(item)){
            // do a runtime maven import
        } else if (item.contains("$")) {
            // Still contains a $; assume a runtime variable to import...
            // For now, we'll just ignore... ($static is, for example, used...
            mb.println(manifest+".addDependency(\"" + X_Source.escape(item) + "\");");
        } else {
            // add a file entry (just add a string and let runtime figure out what to do
            mb.println(manifest+".addDependency(\"" + X_Source.escape(item) + "\");");
        }


    }

    protected void addClasspath(
        UiGeneratorTools service,
        ApiGeneratorContext ctx,
        String manifest,
        MethodBuffer mb,
        GeneratedClasspathInfo classpath
    ) {

        // TODO factor this out into local var
        String scopeVar = mb.addImportStatic(X_Scope.class, "currentScope");
        mb.println(manifest + ".addDependencies(" + classpath.inherit(mb) + ".getPaths(" + scopeVar + "()));");
    }

    private boolean looksLikeCoordinate(String s) {
        int first = s.indexOf(':');
        int last = s.lastIndexOf(':');
        return first != -1 && first != last;
    }

    // TODO: make this more generic and move it somewhere it belongs
    private void printManifestSetter(
        UiGeneratorTools service,
        ApiGeneratorContext ctx,
        MethodBuffer mb,
        String varName,
        Class<?> type,
        String setterName,
        UiAttrExpr attr
    ) {
        final Expression expr = attr.getExpression();
        if (type.isPrimitive()) {
            // annoying, but necessary
            String val = service.resolveString(ctx, expr);
            if (type == int.class) {
                mb.println(varName + "." + setterName + "(" + Integer.parseInt(val) + ");");
            } else if (type == long.class) {
                mb.println(varName + "." + setterName + "(" + Long.parseLong(val) + "L);");
            } else if (type == float.class) {
                mb.println(varName + "." + setterName + "(" + Float.parseFloat(val) + "f);");
            } else if (type == double.class) {
                mb.println(varName + "." + setterName + "(" + Double.parseDouble(val) + ");");
            } else if (type == char.class) {
                assert val.length() == 1 : "Not a valid char value: " + val + " from: " + service.debugNode(expr);
                mb.println(varName + "." + setterName + "(" + val.charAt(0) + ");");
            } else if (type == boolean.class) {
                // for boolean, we will coerce null to true, to match html "naked attributes".
                // <div checked /> is a valid way to say <div checked=true />
                mb.println(varName + "." + setterName + "(" + !"false".equals(val) + ");");
            } else if (type == short.class) {
                mb.println(varName + "." + setterName + "((short)" + Short.parseShort(val) + ");");
            } else if (type == byte.class) {
                mb.println(varName + "." + setterName + "((byte)" + Byte.parseByte(val) + ");");
            }
        } else if (type == String.class) {
            // easy peasy, escape too easy
            String val = service.resolveString(ctx, expr);
            mb.println(varName + "." + setterName + "(\"" + X_Source.escape(val) + "\");");
        } else if (type.isEnum()){
            String val = service.resolveString(ctx, expr);
            // ok, enums we will import and reference directly:
            String enumType = mb.addImport(type);
            assert ArrayIterable.iterate(type.getEnumConstants())
                .anyMatch(e->((Enum)e).name().equals(val))
                : "No enum constant " + val + " in " + type.getCanonicalName();
            mb.println(varName + "." + setterName + "(" + enumType + "." + val + ");");
        } else if (IntTo.class.isAssignableFrom(type)){
            // We'll want to accept [] and other expressions: json {} object will be turned into [{}]
            String intTo = mb.addImport(IntTo.class);
            String create = mb.addImportStatic(X_Collect.class, "newList");
            final TypeVariable<? extends Class<?>> componentType = type.getTypeParameters()[0];
            String component = mb.addImport(componentType.getGenericDeclaration()); // should be string, but lets be safe, anyway
            String listVar = owner.getScope().getInstallLayer().newFieldName("list" + attr.getName());
            mb.println(intTo + " " + listVar + " = " + create + "(" + component + ".class);");

            final Expression resolved = service.resolveVar(ctx, expr);
            if (JsonContainerExpr.isArray(resolved)) {
                // hokay! we have an array in the source.  The tricky bit is if it's an array of arrays...
                // which we will ignore for now, because we don't need a general-purpose solution.
                for (JsonPairExpr pair : ((JsonContainerExpr) resolved).getPairs()) {
                    printManifestSetter(service, ctx, mb, listVar, componentType.getGenericDeclaration(), "add", UiAttrExpr.of(attr.getNameString(), pair.getValueExpr()));
                }

            } else {
                // a single item to turn into the desired type...
                printManifestSetter(service, ctx, mb, listVar, componentType.getGenericDeclaration(), "add", UiAttrExpr.of(attr.getNameString(), resolved));
            }
        } else if (type.isArray()) {
            throw new NotYetImplemented("Array types not yet implemented; don't need right now, will do if someone asks");
        } else {
            throw new NotYetImplemented("Type " + type.getCanonicalName() + " not yet implemented");
        }
    }

    private MappedIterable<String> findEntryPoints(String name, String module, UiContainerExpr gwtc) {
        ChainBuilder<String> entryPoints = Chain.startChain();
        tryAddName(entryPoints, name);
        if (!module.equals(name)) {
            tryAddName(entryPoints, module);
        }
        // TODO: also look for things like entryPoint=[...] features (later; robust is not needed now)
        return entryPoints;
    }
    private MappedIterable<String> findGwtXmls(String name, String module, UiContainerExpr gwtc) {
        ChainBuilder<String> gwtXmls = Chain.startChain();
        tryAddGwtXml(gwtXmls, name);
        if (!module.equals(name)) {
            tryAddGwtXml(gwtXmls, module);
        }
        // TODO: also look for things like gwtXml=[...] features (later; robust is not needed now)
        return gwtXmls;
    }

    private void tryAddName(ChainBuilder<String> entryPoints, String name) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL cls = cl.getResource(name.replace('.', '/') + ".class");
        if (cls != null) {
            try {
                final Class<?> clazz = cl.loadClass(name);
                if (EntryPoint.class.isAssignableFrom(clazz)) {
                    entryPoints.add(name);
                    return;
                }
            } catch (ClassNotFoundException ignored) {}
        }
        final URL source = cl.getResource(name.replace('.', '/') + ".java");
        if (source != null) {
            try {
                final TypeDeclaration type = JavaParser.parse(source.openStream()).getPrimaryType();
                if (type instanceof ClassOrInterfaceDeclaration) {
                    for (ClassOrInterfaceType impl : ((ClassOrInterfaceDeclaration) type).getImplements()) {
                        if (impl.getName().endsWith("EntryPoint")) {
                            entryPoints.add(name);
                            return;
                        }
                    }

                }
            } catch (Exception ignored) {}
        }
    }

    private void tryAddGwtXml(ChainBuilder<String> gwtXmls, String name) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL cls = cl.getResource(name.replace('.', '/') + ".gwt.xml");
        if (cls != null) {
            gwtXmls.add(name);
            return;
        }
        final URL source = cl.getResource(name.replace('.', '/') + ".gwtxml");
        if (source != null) {
            gwtXmls.add(name);
        }
    }
}
