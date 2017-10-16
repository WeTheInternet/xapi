package xapi.dev.gwtc.api;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xapi.annotation.compile.Dependency;
import xapi.annotation.compile.Dependency.DependencyType;
import xapi.annotation.compile.DependencyBuilder;
import xapi.annotation.compile.Resource;
import xapi.annotation.compile.ResourceBuilder;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.SourceBuilder.JavaType;
import xapi.dev.source.XmlBuffer;
import xapi.except.NotYetImplemented;
import xapi.file.X_File;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.Gwtc;
import xapi.gwtc.api.GwtcProperties;
import xapi.gwtc.api.GwtcXmlBuilder;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;
import xapi.source.X_Source;
import xapi.util.X_Debug;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static xapi.gwtc.api.GwtManifest.GEN_PREFIX;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.dev.Compiler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.tools.GWTTestSuite;
import com.google.gwt.reflect.shared.GwtReflect;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/16/17.
 */
public class GwtcProjectGeneratorAbstract implements GwtcProjectGenerator {
    private final GwtcService service;
    private ClassLoader resources;

    protected final GwtcGeneratedProject context;
    protected final String genName;
    protected final File tempDir;
    protected SourceBuilder<GwtcService> entryPoint;
    protected final Set<String> finished;
    protected GwtcEntryPointBuilder out;
    private boolean needsReportError = true;
    protected String manifestName;
    protected StringTo<Out1<String>> files;
    private MethodBuffer junitLoader;
    private final Lazy<GwtManifest> manifest;

    public GwtcProjectGeneratorAbstract(GwtcService service, ClassLoader resources, String moduleName) {
        this.service = service;
        this.resources = resources;
        context = new GwtcGeneratedProject(service, resources, moduleName);
        finished = new HashSet<>();
        files = X_Collect.newStringMap(Out1.class);


        genName = context.getGenName();
        boolean bareModule = moduleName.indexOf('.') == -1;
        final String[] genType =
            bareModule ? new String[]{GwtManifest.GEN_PREFIX, genName} :
            X_Source.splitClassName(genName);
        tempDir = X_File.createTempDir(genName);

        entryPoint = new SourceBuilder<GwtcService>("public class "+ genType[1])
            .setPackage(genType[0]);
        out = new GwtcEntryPointBuilder(entryPoint);

        context.setEntryPoint(genName);

        if (bareModule) {
            context.addGwtXmlSource(GwtManifest.GEN_PREFIX);
        } else {
            context.addGwtXmlSource("");
        }
        manifest = Lazy.deferred1(this::createManifest);
    }

    private GwtManifest createManifest() {
        final GwtManifest manifest = X_Inject.instance(GwtManifest.class);
        manifest.setModuleName(getModuleName());
        initializeManifest(manifest);
        if (manifest.getGenDir() == null) {
            manifest.setGenDir(getTempDir().getAbsolutePath());
        }
        return manifest;
    }

    /**
     * Left here so you can subclass to be able to insert common logic into your compile manifest.
     */
    protected void initializeManifest(GwtManifest manifest) {
        addGwtInherit("xapi.X_Core");
    }

    public GwtcService getService() {
        return service;
    }

    public ClassLoader getResources() {
        return resources;
    }

    public void setResources(ClassLoader resources) {
        this.resources = resources;
    }

    public void addClass(Class<?> clazz) {
        if (!finished.add(clazz.getName())) {
            return;
        }
        addGwtModules(clazz);
        if (EntryPoint.class.isAssignableFrom(clazz)) {
            try {
                addMethod(clazz.getMethod("onModuleLoad"));
            } catch (Exception e) {
                X_Log.error(GwtcProjectGenerator.class, "Could not extract onModuleLoad method from ", clazz, e);
            }
        } else if (GWTTestCase.class.isAssignableFrom(clazz)) {
            addGwtTestCase(clazz.asSubclass(GWTTestCase.class));
        } else if (GWTTestSuite.class.isAssignableFrom(clazz)) {
            addGwtTestSuite(clazz.asSubclass(GWTTestSuite.class));
        } else if (RunAsyncCallback.class.isAssignableFrom(clazz)) {
            addAsyncBlock(clazz.asSubclass(RunAsyncCallback.class));
        } else {
            // Check if this class has methods annotated w/ junit @Test
            for (Method m : clazz.getMethods()) {
                if (m.getAnnotation(Test.class) != null) {
                    finished.remove(clazz.getName());
                    addJUnitClass(clazz);
                    return;
                }
            }
            try {
                addMethod(clazz.getMethod("main", String[].class));
            } catch (Exception ignored){
                X_Log.warn(getClass(), "Class",clazz," was added to Gwtc, "
                    + "but that class was not a subclass of EntryPoint, RunAsync,"
                    + " GWTTestCase, GWTTestSuite, nor did it have a main method, or"
                    + " any JUnit 4 annotated @Test method.");
            }
        }
    }

    public GwtcGeneratedProject getProject() {
        return context;
    }

    public String getSuggestedRoot() {
        Class<?> cls = context.getFirstClassAdded();
        if (cls != null) {
            while (cls.getEnclosingClass() != null) {
                cls = cls.getEnclosingClass();
            }
            final String loc = X_Reflect.getSourceLoc(context.getFirstClassAdded());
            if (loc != null) {
                return loc;
            }
        }
        return ".";
    }

    @Override
    public void generateAll(File source, String moduleName, XmlBuffer head, XmlBuffer body) {
        context.generateAll(source, moduleName, head, body);
    }

    public void addGwtModules(Class<?> clazz) {
        context.addClass(clazz);
    }

    public void addClasspath(Class<?> cls) {

        String loc = X_Reflect.getFileLoc(cls);

        final DependencyBuilder builder = DependencyBuilder
            .buildDependency(loc)
            .setDependencyType(DependencyType.ABSOLUTE);

        context.addDependency(builder.build(), cls);
        // if loc is a src/main/java or src/test/java, also include resources module:
        String unixed = loc.replace('\\', '/');
        int index = unixed.indexOf("target/classes");
        if (index != -1) {

            context.addDependency(builder
                .setValue(unixed.replace("target/classes", "src/main/java"))
                .build(), cls);

            context.addDependency(builder
                .setValue(unixed.replace("target/classes", "src/main/resources"))
                .build(), cls);

        } else if (unixed.contains("target/test-classes")) {

            context.addDependency(builder
                .setValue(unixed.replace("target/test-classes", "src/test/java"))
                .build(), cls);

            context.addDependency(builder
                .setValue(unixed.replace("target/test-classes", "src/test/resources"))
                .build(), cls);
            context.addDependency(builder
                .setValue(unixed.replace("target/test-classes", "src/main/java"))
                .build(), cls);
            context.addDependency(builder
                .setValue(unixed.replace("target/test-classes", "src/main/resources"))
                .build(), cls);

        }
    }

    public String getEntryPoint() {
        return entryPoint.toString();
    }

    public XmlBuffer getGwtXml(GwtManifest manifest) {
        return context.getGwtXml(manifest);
    }

    public String getModuleName() {
        return genName;
    }


    @Override
    public File getTempDir() {
        return tempDir;
    }

    public void addDependency(Dependency dep, AnnotatedElement clazz) {
        context.addDependency(dep, clazz);
    }

    protected void inheritGwtXml(Class<?> clazz, Resource build) {
        context.addGwtXmlInherit(build.value());
    }

    protected ClassBuffer classBuffer() {
        return entryPoint.getClassBuffer();
    }


    protected String generateSetupMethod(String field, String type, Class<?> clazz, List<Method> befores) {
        if (befores.isEmpty()) {
            return "";
        }
        String methodName = type+"TestRun_"+field;
        String simpleName = entryPoint.getImports().addImport(clazz);
        MethodBuffer runner = entryPoint.getClassBuffer().createMethod(
            "private final void "+methodName+"("+simpleName+" field)");

        for (Method before : befores) {
            runner.println("field."+before.getName()+"();");
        }

        return methodName;
    }

    protected String generateTestRunner(Class<?> clazz, List<Method> beforeClasses,
                                        List<Method> befores, List<Method> tests, List<Method> afters,
                                        List<Method> afterClasses) {
        String field = out.formatInstanceProvider(clazz);
        String methodName = "testRun_"+field;

        MethodBuffer runner = classBuffer().createMethod("private final void "+methodName);
        // Setup
        for (Method beforeClass : beforeClasses) {
            String shortName = runner.addImport(beforeClass.getDeclaringClass());
            runner.println(shortName+"."+beforeClass.getName()+"();");
        }
        String shortName = runner.addImport(clazz);
        String beforeMethod = generateSetupMethod(field, "before", clazz, befores);
        String afterMethod = generateSetupMethod(field, "after", clazz, afters);
        for (Method test : tests) {
            String testName = methodName+"_"+test.getName();
            MethodBuffer testRunner = entryPoint.getClassBuffer().createMethod
                ("private final void "+testName);
            if (!beforeMethod.isEmpty()) {
                testRunner.println(beforeMethod+"("+field+");");
            }
            Test testAnno = test.getAnnotation(Test.class);
            testRunner.startTry();
            testRunner.println(field+"."+test.getName()+"();");
            if (testAnno.expected() != Test.None.class) {
                String assertClass = testRunner.addImport(Assert.class);
                String exceptionType = testRunner.addImport(testAnno.expected());
                testRunner.println(assertClass+".fail(\""
                    + "expected exception "+exceptionType+ "\");");
                testRunner.startCatch(exceptionType);
            }
            String errorName = testRunner.startCatch("Throwable");
            ensureReportError();
            testRunner.println("reportError("+shortName+".class,"
                + "\""+test.toGenericString()+"\", "+errorName+ ");");
            if (!afterMethod.isEmpty()) {
                testRunner.startFinally();
                testRunner.println(afterMethod+"("+field+");");
            }
            testRunner.endTry();
            runner.println(testName+"();");
        }
        for (Method afterClass : afterClasses) {
            shortName = runner.addImport(afterClass.getDeclaringClass());
            runner.println(shortName+"."+afterClass.getName()+"();");
        }

        return methodName+"();";
    }

    protected void ensureReportError() {
        if (needsReportError) {
            needsReportError = false;
            generateReportError(entryPoint.getClassBuffer());
        }
    }

    public void copyModuleTo(String module, GwtManifest manifest) {
        String from = manifestName == null ? genName : manifestName;
        File f = new File(tempDir, from+".gwt.xml");
        if (f.exists()) {
            int ind = module.lastIndexOf('.');
            String path;
            if (ind == -1) {
                path = "";
            } else {
                path = module.substring(0, ind).replace('.', '/');
            }
            f = new File(tempDir, path);
            f.mkdirs();
            final XmlBuffer xml = context.getGwtXml(manifest);
            saveGwtXmlFile(xml, module.substring(ind+1), manifest);
        }
    }


    @Override
    public void createFile(String pkg, String filename, Out1<String> sourceProvider) {
        String fqcn = pkg + File.separatorChar + filename;
        assert !files.containsKey(fqcn) : "Already created file with the qualified name " + fqcn;
        files.put(fqcn, sourceProvider);
    }

    @Override
    public MethodBuffer addMethodToEntryPoint(String methodDef) {
        return entryPoint.getClassBuffer().createMethod(methodDef);
    }

    @Override
    public void addGwtInherit(String inherit) {
        context.addGwtXmlInherit(inherit);
    }

    @Override
    public MethodBuffer getOnModuleLoad() {
        return out.out;
    }


    @Override
    public SourceBuilder createJavaFile(String pkg, String filename, JavaType type) {
        final SourceBuilder builder = new SourceBuilder();
        type.initialize(builder, pkg, filename);
        createFile(pkg.replace('.', '/'), filename+".java", builder::toSource);
        return builder;
    }

    @Override
    public String modifyPackage(String pkgToUse) {
        // exists in case subclasses want to repackage things...
        return pkgToUse;
    }

    protected void saveGwtXmlFile(XmlBuffer xml, String moduleName, GwtManifest manifest) {
        saveTempFile(GwtcXmlBuilder.HEADER+xml, new File(service.inGeneratedDirectory(manifest, moduleName.replace('.', '/')+".gwt.xml")));
    }

    protected void saveTempFile(String value, File dest) {
        X_Log.trace(getClass(), "saving generated file to",dest);
        final boolean success = dest.getParentFile().mkdirs();
        assert success || dest.getParentFile().exists() : "Unable to create parent directories for " + dest.getAbsolutePath();
        try (FileWriter out = new FileWriter(dest)) {
            out.append(value);
        } catch (IOException e) {
            X_Log.warn(getClass(), "Error saving generated file ",dest,"\n"+value);
            throw X_Debug.rethrow(e);
        }
    }

    @Override
    public void addAsyncBlock(Class<? extends RunAsyncCallback> asSubclass) {
        // TODO: add this RunAsyncCallback to our generated EntryPoint
        throw new NotYetImplemented("RunAsyncCallback not yet implemented");
    }

    @Override
    public void addGwtTestCase(Class<? extends GWTTestCase> subclass) {
        // TODO: setup a GWTTestSuite to add this test case to
        throw new NotYetImplemented("GwtTestCase not yet implemented");
    }

    @Override
    public void addGwtTestSuite(Class<? extends GWTTestSuite> asSubclass) {
        // TODO: setup a run config for this GWTTestSuite
        throw new NotYetImplemented("GwtTestSuite not yet implemented");
    }

    @Override
    public void addMethod(Method method) {
        addMethod(method, false);
    }

    @Override
    public void addMethod(Method method, boolean onNewInstance) {
        // TODO move allll the compile generation to a different service
        // so this service can be concerned only with compilation
        if (Modifier.isStatic(method.getModifiers())) {
            // print a call to a static method
            out.println(out.formatStaticCall(method));
        } else {
            // print a call to an instance method; creating an instance if necessary.
            out.println(out.formatInstanceCall(method, onNewInstance));
        }
    }

    @Override
    public void addPackage(Package pkg, boolean recursive) {
        if (!finished.add(pkg.getName())) {
            return;
        }
        Gwtc gwtc = pkg.getAnnotation(Gwtc.class);
        context.addPackages(pkg, this, recursive);
        if (gwtc != null) {
            context.addGwtcPackage(gwtc, pkg, recursive);
        }
    }

    public void addGwtcClass(Class<?> cls, Gwtc ... gwtcs) {
        for (Gwtc gwtc : gwtcs) {
            context.addGwtcClass(gwtc, cls);
        }

    }

    @Override
    public String generateCompile(GwtManifest manifest) {
        assert tempDir.exists() : "No usable directory " + tempDir.getAbsolutePath();
        if (manifest.getModuleName() == null) {
            manifest.setModuleName(genName);
            manifestName = genName;
        } else {
            assert context.getInheritedGwtXml().noneMatch(manifest.getModuleName()::equals)
                : "Do not inherit the gwt xml of the module you are generating! (bad name: " + manifest.getModuleName() + ")";
            manifestName = manifest.getModuleName();
            context.setRenameTo(manifest.getModuleName());
        }
        if (manifest.getModuleShortName() != null) {
            context.setRenameTo(manifest.getModuleShortName());
        }
        String entryPackage = manifestName;
        int endInd = entryPackage.lastIndexOf('.');
        if (endInd != -1) {
            entryPackage = entryPackage.substring(0, endInd);
            if (entryPoint.getPackage() == null || entryPoint.getPackage().equals(GEN_PREFIX)) {
                entryPoint.setPackage(entryPackage);
            } else if (!entryPoint.getPackage().startsWith(entryPackage)){
                entryPackage = (entryPackage + "." + entryPoint.getPackage())
                    .replace(GEN_PREFIX+"."+GEN_PREFIX, GEN_PREFIX);
                if (context.getEntryPoint().startsWith(GEN_PREFIX)) {
                    context.setEntryPoint(context.getEntryPoint().substring(GEN_PREFIX.length()+1));
                }
            }
            entryPoint.setPackage(entryPackage);
            context.setEntryPointPackage(entryPackage);
        }
        String entryPointLocation = service.inGeneratedDirectory(manifest, entryPoint.getQualifiedName().replace('.', '/')+".java");
        XmlBuffer xml = getGwtXml(manifest);
        saveGwtXmlFile(xml, manifest.getModuleName(), manifest);
        manifest.getModules().forEach(mod->{
            saveGwtXmlFile(mod.getBuffer(), mod.getInheritName(), manifest);
        });
        saveTempFile(entryPoint.toString(), new File(entryPointLocation));
        files.forBoth((path, body)->
            saveTempFile(body.out1(), new File(service.inGeneratedDirectory(manifest, path)))
        );
        X_Log.info(GwtcProjectGeneratorAbstract.class, "Generated entry point", "\n", getEntryPoint());
        X_Log.info(GwtcProjectGeneratorAbstract.class, "Generated module", "\n", getGwtXml(manifest));
        return prepareCompile(manifest);
    }

    protected String prepareCompile(GwtManifest manifest) {

        String gwtHome = X_Properties.getProperty("gwt.home");
        if (manifest.getCompileDirectory() == null) {

            GwtcProperties defaultProp = service.getDefaultLaunchProperties();
            Type level = manifest.getLogLevel();
            String warDir = manifest.getWarDir();
            for (GwtcProperties prop : context.getLaunchProperties()) {

                if (prop.obfuscationLevel() != defaultProp.obfuscationLevel()) {
                    manifest.setObfuscationLevel(prop.obfuscationLevel());
                }

                if (prop.logLevel() != defaultProp.logLevel()) {
                    if (level.isLowerPriorityThan(prop.logLevel())) {
                        level = prop.logLevel();
                    }
                }

                if (!prop.warDir().equals(GwtcProperties.DEFAULT_WAR)) {
                    warDir = prop.warDir();
                }
            }
            if (warDir == null) {
                warDir = GwtcProperties.DEFAULT_WAR;
            }
            manifest.setLogLevel(level);
            if (manifest.getSystemProperties().noneMatch(String::startsWith, "xapi.log.level")) {
                manifest.addSystemProp("xapi.log.level="+level.name());
            }
            manifest.setWarDir(warDir);

            if (warDir.contains("/tmp/")) {
                File f = tempDir;
                try {
                    String tempCanonical = f.getCanonicalPath();
                    if (!warDir.contains(tempCanonical)) {
                        warDir = warDir.replaceAll("/tmp/", tempCanonical + File.separator);
                    }
                    manifest.setWarDir( warDir );
                    warDir = new File(warDir).getCanonicalPath();
                    manifest.setWarDir( warDir );
                    X_Log.info(getClass(), "Manifest WAR: ",manifest.getWarDir());
                    final boolean made = new File(warDir).mkdirs();
                    if (!made) {
                        X_Log.warn(getClass(), "Unable to create temporary war directory for GWT compile",
                            "You will likely get an unwanted war folder in the directory you executed this program \ncheck " + warDir+"");
                    }
                } catch (IOException e) {
                    X_Log.warn(getClass(), "Unable to create temporary war directory for GWT compile",
                        "You will likely get an unwanted war folder in the directory you executed this program \ncheck " + warDir+"", e);
                    X_Debug.maybeRethrow(e);
                }
            }
            if (manifest.getUnitCacheDir() == null) {
                try {
                    File f = X_File.createTempDir("gwtc-"+manifest.getModuleName()+"UnitCache", manifest.isDisableUnitCache());
                    if (f != null) {
                        manifest.setUnitCacheDir(f.getCanonicalPath());
                    }
                } catch (IOException e) {
                    X_Log.warn("Unable to create unit cache work directory for GWT compile",
                        "You will likely get unwanted gwtUnitcache folders in the directory you executed this program");
                }
            }
            Set<Dependency> dups = new HashSet<>();
            for (AnnotatedDependency dependency : context.getDependencies()) {
                if (dependency.getDependency().dependencyType() == DependencyType.RELATIVE
                    || dups.add(dependency.getDependency())) {
                    manifest.addDependencies(service.resolveDependency(manifest, dependency));
                }
            }
            if (gwtHome == null) {
                final Enumeration<URL> compilerLoc;
                try {
                    compilerLoc = Compiler.class.getClassLoader().getResources(Compiler.class.getName().replace(
                        '.',
                        '/'
                    ) + ".class");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                URL gwtHomeLocation;
                while (compilerLoc.hasMoreElements()) {
                    gwtHomeLocation = compilerLoc.nextElement();
                    if (gwtHomeLocation == null) {
                        X_Log.warn("Unable to find gwt home from System property gwt.home, "
                            , "nor from looking up the gwt compiler class from classloader.  Defaulting to ./lib");
                        gwtHome = X_File.getPath(".");
                    } else {
                        gwtHome = gwtHomeLocation.toExternalForm();
                        if (gwtHome.contains("jar!")) {
                            gwtHome = gwtHome.split("jar!")[0]+"jar";
                        }
                        gwtHome = gwtHome.replace("file:", "").replace("jar:", "");
                        if (manifest.getGwtVersion().length() == 0) {
                            if (gwtHome.contains("gwt-dev.jar")) {
                                manifest.setGwtVersion("");
                            } else {
                                manifest.setGwtVersion(service.extractGwtVersion(gwtHome));
                            }
                        }
                        int ind = gwtHome.lastIndexOf("gwt-dev");
                        if (ind == -1) {
                            continue;
                        }
                        gwtHome = gwtHome.substring(0, ind-1);
                    }
                }
            }
            X_Properties.setProperty("gwt.home", gwtHome);
        }

        generateWar(manifest);
        return gwtHome;
    }
//
//    @Override
//    public boolean addJUnitClass(Class<?> clazz) {
//        if (!finished.add(clazz.getName())) {
//            X_Log.info(getClass(), "Skipped JUnit 4 class",clazz);
//            return false;
//        }
//        search: {
//            for (Method m : clazz.getMethods()) {
//                if (m.isAnnotationPresent(Test.class)) {
//                    break search;
//                }
//            }
//            return false;
//        }
//        Gwtc gwtc = clazz.getAnnotation(Gwtc.class);
//        X_Log.info(getClass(), "generating JUnit class", clazz, "?"+(gwtc != null));
//        if (gwtc != null) {
//            context.addGwtcClass(gwtc, clazz);
//        }
//        addGwtModules(clazz);
//        X_Log.info(getClass(), "added test class for JUnit 4",clazz);
//        ensureReportError();
//        inheritGwtXml(clazz, ResourceBuilder.buildResource("org.junit.JUnit4").build());
//        inheritGwtXml(clazz, ResourceBuilder.buildResource("com.google.gwt.core.Core").build());
//        ClassBuffer cb = classBuffer();
//        String simple = cb.addImport(clazz);
//        String methodName = "add"+simple+"Tests";
//        String gwt = cb.addImport(GWT.class);
//        String callback = cb.addImport(RunAsyncCallback.class);
//        String magic = cb.addImportStatic(GwtReflect.class, "magicClass");
//        cb.createMethod("void "+methodName)
//            .println(gwt+".runAsync("+simple+".class,")
//            .indent()
//            .println("new "+callback+ "() {")
//            .indent()
//            .println("public void onSuccess() {")
//            .indent()
//            .println(magic+"("+simple+".class);")
//            .startTry()
//            .println("junit.addTests("+simple+".class);")
//            .startCatch("Throwable", "e")
//            .println("junit.print(\"Error adding "+simple+" to unit test\", e);")
//            .endTry()
//            .outdent()
//            .println("}")
//            .println()
//            .println("public void onFailure(Throwable reason) {")
//            .indent()
//            .println("junit.print(\"Error loading "+simple+"\", reason);")
//            .outdent()
//            .println("}")
//            .outdent()
//            .println("}")
//            .outdent()
//            .println(");")
//        ;
//
//        junitLoader.println(methodName+"();");
//        return true;
//    }

//    Other version of addJunitClass.  Not sure which is better, so leaving both and will sort out later
    @Override
    public boolean addJUnitClass(Class<?> clazz) {
        inheritGwtXml(clazz, ResourceBuilder.buildResource("org.junit.JUnit4").build());
        X_Log.info(getClass(), "Adding class", clazz," to junit 4 module");
        addDependency(
            DependencyBuilder.buildDependency("gwt-reflect")
                .setGroupId("net.wetheinter")
                .setVersion(X_Namespace.GWT_VERSION)
                .setClassifier("tests")
                .setDependencyType(DependencyType.MAVEN)
                .build()
            , clazz);
        List<Method>
            beforeClass = new ArrayList<>(),
            before = new ArrayList<>(),
            test = new ArrayList<>(),
            after = new ArrayList<>(),
            afterClass = new ArrayList<>()
                ;
        List<Class<?>> hierarchy = new LinkedList<>();
        {
            Class<?> c = clazz;
            while (c != Object.class) {
                hierarchy.add(0, c);
                c = c.getSuperclass();
            }
        }
        for (Class<?> c : hierarchy) {
            for (Method method : c.getMethods()) {
                // TODO: move all junit-y things out to a test-module
                if (method.getAnnotation(Test.class) != null) {
                    test.add(method);
                } else if (method.getAnnotation(Before.class) != null) {
                    before.add(method);
                } else if (method.getAnnotation(BeforeClass.class) != null) {
                    beforeClass.add(method);
                } else if (method.getAnnotation(After.class) != null) {
                    after.add(method);
                } else if (method.getAnnotation(AfterClass.class) != null) {
                    afterClass.add(method);
                }
            }
        }
        out.println(generateTestRunner(clazz, beforeClass, before, test, after, afterClass));
        return true;
    }


//    @Override
//    protected void generateReportError(ClassBuffer classBuffer) {
//        super.generateReportError(classBuffer);
//        addClass(JUnit4Runner.class);
//        junitLoader = classBuffer.createInnerClass("private final class JUnit extends JUnitUi")
//            .createMethod("public void loadAllTests()");
//
//        classBuffer.createField(JUnitUi.class, "junit")
//            .setModifier(Modifier.FINAL | Modifier.PRIVATE)
//            .setInitializer("new JUnit()");
//
//        out.println("junit.onModuleLoad();");
//    }
    protected void generateReportError(ClassBuffer classBuffer) {
        classBuffer.createMethod("private static void reportError"
            + "(Class<?> clazz, String method, Throwable e)")
            .println("String error = method+\" failed test\";")
            .println("System.err.println(error);")
            .println("e.printStackTrace();");
    }

    protected void generateWar(GwtManifest manifest) {
        String warDir = manifest.getWarDir();
        String moduleName = manifest.getModuleName();

        final XmlBuffer
            buffer = new XmlBuffer("html"),
            head = buffer.makeTag("head"),
            body= buffer.makeTag("body")
                ;
        buffer.printBefore(getHostPageDocType());

        manifest.setIncludeGenDir(false);
        final File source = new File(manifest.getGenDir());
        manifest.addSource(source.getAbsolutePath());

        generateAll(source, moduleName, head, body);

        head.makeTag("script")
            .setAttribute("type", "text/javascript")
            .setAttribute("src", getScriptLocation(moduleName));

        String hostPage = buffer.toString();
        X_File.saveFile(warDir +"/public", "index.html", hostPage);
        X_Log.info(GwtcProjectGeneratorAbstract.class, "Generated host page:\n", hostPage);
        X_Log.info("Generate war into ", warDir);
    }

    protected String getHostPageDocType() {
        return "<!doctype html>\n";
    }

    private String getScriptLocation(String genName) {
        return //genName+"/"+
            genName+".nocache.js";
    }

    @Override
    public GwtManifest getManifest() {
        return manifest.out1();
    }
}
