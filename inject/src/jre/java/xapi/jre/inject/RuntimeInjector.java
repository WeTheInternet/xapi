package xapi.jre.inject;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.annotation.reflect.KeepClass;
import xapi.bytecode.ClassFile;
import xapi.bytecode.annotation.*;
import xapi.collect.fifo.Fifo;
import xapi.collect.fifo.SimpleFifo;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.dev.scanner.impl.ClasspathScannerDefault;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.In2;
import xapi.inject.X_Inject;
import xapi.inject.api.PlatformChecker;
import xapi.inject.impl.JavaInjector;
import xapi.log.X_Log;
import xapi.platform.Platform;
import xapi.reflect.X_Reflect;
import xapi.source.X_Source;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.time.impl.ImmutableMoment;
import xapi.prop.X_Properties;
import xapi.util.X_Runtime;
import xapi.string.X_String;
import xapi.util.X_Util;

import java.io.*;
import java.net.URLClassLoader;
import java.util.*;

// TODO: move this type into somewhere with extremely severely limited classpath.
// It's nasty if we try to load it somewhere with a minimal classpath...
@KeepClass
public class RuntimeInjector implements In2<String, PlatformChecker> {

  @Override
  public void in(String targetDir, PlatformChecker selector) {

    final String cacheDir = getInjectorCacheDir();
    String prefix = cacheDir + "META-INF"+File.separator;
    if (cacheDir.endsWith(X_String.ensureEndsWith(targetDir, File.separator))) {
      targetDir = cacheDir;
    }
    writeMetaInfo(targetDir, selector, prefix+"singletons", prefix+"instances");
  }

  public static String getInjectorCacheDir() {
    String prefix = System.getProperty("xapi.injector.cache", System.getenv("xapi.injector.cache"));
    if (prefix == null) {
      try {
        // Try to find the location of the current main class, and try to use it to improve our prefix
        // (i.e. generating results somewhere usable...)
        final Class<?> mainClass = X_Reflect.getMainClass();
        if (mainClass != null) {
          final String mainLoc = X_Reflect.getFileLoc(mainClass);
          if (mainLoc != null) {
            if (mainLoc.contains("jar")) {
              return "";
            }
            return X_String.ensureEndsWith(mainLoc, File.separator);
          }
        }
      } catch (Exception ignored) {
        ignored.printStackTrace();
      }
      return ""; // uses working directory
    }
    return X_String.ensureEndsWith(prefix, File.separator);
  }

  @SuppressWarnings("unchecked")
  public void writeMetaInfo(String targetDir, PlatformChecker checker, final String singletonDir, final String instanceDir){
    if (!targetDir.endsWith(File.separator)) {
      targetDir += File.separator;
    }
    targetDir = targetDir.replace('\\', '/');

    File relative = new File(targetDir);

    if (X_Source.hasMainPath(targetDir)) {
      Class<?> mainClass = null;
      try {
        mainClass = X_Reflect.getMainClass();
      } catch (Exception ignored){
        ignored.printStackTrace();
      }
      if (mainClass != null) {
        final String mainLoc = X_Reflect.getFileLoc(mainClass);
        relative = new File(mainLoc.replace("file:", ""));
      }
    }
    if (relative.isFile()) {
      // Our search landed on a file (likely a jar);
      // revert to trusting relative directories, and assume semi-sane $PWD
      relative = new File(targetDir);
    }
    final File target = relative.getAbsoluteFile();
    if (target.getAbsolutePath().contains(".jar")) {
      final String prop = System.getProperty("xapi.injector.cache", System.getenv("xapi.injector.cache"));
      throw new Error("Illegal injection target " + target + "; (" + relative + ")\n" +
          (
              X_String.isEmptyTrimmed(prop)
              ?
              "Consider setting xapi.injector.cache property to a writable directory!"
              : "The configured xapi.injector.cache property of " + prop + "is invalid"
          )
      );
    }
    if (!target.isDirectory()) {
      if (!target.mkdirs()) {
        throw new Error("Unable to get or make jre injection generator output directory: "+
            target.getAbsolutePath());
      }
    }

    ClasspathScanner scanner;
    try {
      scanner = X_Inject.instance(ClasspathScanner.class);
    } catch (final Exception e) {
      scanner = new ClasspathScannerDefault();
    }
    final Moment start = now();
	final ClasspathResourceMap resources = scanner
      .scanAnnotations(
        Platform.class,
        SingletonDefault.class, SingletonOverride.class,
        InstanceDefault.class, InstanceOverride.class
      )
      // TODO: add |xapi, and use manifests describing injection strategies
      .matchResource("META[-]INF/(instances|singletons)")
      .matchClassFile(".*")
      .scan(targetClassloader())
      ;
    // Only collect platform types if we are not running in a known platform.
    final String[] runtime = checker.getRuntime();

    final Fifo<ClassFile> defaultSingletons = new SimpleFifo<ClassFile>();
    final Fifo<ClassFile> defaultInstances = new SimpleFifo<ClassFile>();
    final Fifo<ClassFile> singletonImpls = new SimpleFifo<ClassFile>();
    final Fifo<ClassFile> instanceImpls = new SimpleFifo<ClassFile>();

    ClassFile bestMatch = null;
    final HashMap<String,ClassFile> platformMap = new HashMap<String, ClassFile>();
    String shortName = X_Properties.platform.out1();

    final Set<String> scopes = new LinkedHashSet<String>();
    final ArrayList<ClassFile> platforms = new ArrayList<ClassFile>();
    final Moment prepped = now();
    for (final ClassFile file : resources.findClassAnnotatedWith(Platform.class)) {
      platforms.add(file);
      for (final String platform : runtime) {
        scopes.add(platform);
        shortName = platform.substring(platform.lastIndexOf('.')+1);
        platformMap.put(file.getName(), file);
        if (file.getName().equals(platform)) {
          bestMatch = file;
        }
        if (bestMatch == null && file.getName().endsWith(shortName)) {
          bestMatch = file;
        }
      }
    }
    final Moment scanned = now();
    if (bestMatch == null) {
      final NotConfiguredCorrectly error = platformMisconfigured(shortName);
      error.printStackTrace();
      return;
    }
    MemberValue fallbacks;
    final LinkedHashSet<ClassFile> remainder = new LinkedHashSet<ClassFile>();
    remainder.add(bestMatch);
    while (remainder.size()>0) {
      final ClassFile next = remainder.iterator().next();
      final Annotation anno = next.getAnnotation("xapi.platform.Platform");
      if (anno == null) {
        throw platformMisconfigured(next.getName());
      }
      fallbacks = anno.getMemberValue("fallback");
      List<String> names = new ArrayList<>();
      names.add(anno.getTypeName());
      if (fallbacks != null) {
        final ArrayMemberValue arr = (ArrayMemberValue)fallbacks;
        for (final MemberValue v : arr.getValue()) {
          final String name = ((ClassMemberValue)v).getValue();
          final ClassFile fallback = platformMap.get(name);
          if (fallback != null) {
            names.add(name);
            if (scopes.add(fallback.getName())) {
              remainder.add(fallback);
            }
          }
        }
      }
      checker.addPlatform(targetClassloader(), anno.getTypeName(), names);
      remainder.remove(next);
    }
    final Moment checked = now();
    for (final ClassFile cls : resources.findClassAnnotatedWith(
      SingletonDefault.class, SingletonOverride.class,
      InstanceDefault.class, InstanceOverride.class
      )) {

      Annotation anno = cls.getAnnotation(SingletonDefault.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms)) {
        if (needsSingletonBinding(implFor(anno))) {
          defaultSingletons.give(cls);
        }
      }
      anno = cls.getAnnotation(SingletonOverride.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms)) {
        if (needsSingletonBinding(implFor(anno))) {
          singletonImpls.give(cls);
        }
      }
      anno = cls.getAnnotation(InstanceDefault.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms)) {
        if (needsInstanceBinding(implFor(anno))) {
          defaultInstances.give(cls);
        }
      }
      anno = cls.getAnnotation(InstanceOverride.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms)) {
        if (needsInstanceBinding(implFor(anno))) {
          instanceImpls.give(cls);
        }
      }
    }
    final Moment mapped = now();

    final Map<String, ClassFile> injectionTargets = new HashMap<String,ClassFile>();
    //determine injection by priority.  Defaults first
    for (final ClassFile cls : defaultSingletons.forEach()){
      final Annotation impl = cls.getAnnotation(SingletonDefault.class.getName());
      final ClassMemberValue value = (ClassMemberValue)impl.getMemberValue("implFor");
      injectionTargets.put(value.getValue(), cls);
    }
    //now scan overrides
    for (final ClassFile cls : singletonImpls.forEach()){
      final Annotation impl = cls.getAnnotation(SingletonOverride.class.getName());
      final ClassMemberValue value = (ClassMemberValue)impl.getMemberValue("implFor");
      final String clsName = value.getValue();
      final ClassFile existing  = injectionTargets.get(value.getValue());
      if (existing  == null){
        injectionTargets.put(clsName, cls);
        continue;
      }


      final ClassFile previous = injectionTargets.get(clsName);
      // previous value was a default
      if (previous == null){
        injectionTargets.put(clsName, cls);
        continue;
      }
      final Annotation oldOverride = previous.getAnnotation(SingletonOverride.class.getName());
      if (oldOverride == null) {
        injectionTargets.put(clsName, cls);
        continue;
      }
      final IntegerMemberValue oldPriority = (IntegerMemberValue)oldOverride.getMemberValue("priority");
      final IntegerMemberValue newPriority = (IntegerMemberValue)impl.getMemberValue("priority");
      if (newPriority == null) {
        continue;
      }
      if (oldPriority == null || newPriority.getValue() > oldPriority.getValue()){
        injectionTargets.put(clsName, cls);
      }
    }
    final Moment startInject = now();
    for (final String iface : injectionTargets.keySet()) {
      try {
        JavaInjector.registerSingletonProvider(targetClassloader(), iface, injectionTargets.get(iface).getName());
      } catch (Throwable t) {
        if (X_Util.unwrap(t) instanceof InterruptedException) {
          break;
        }
        X_Log.warn(RuntimeInjector.class, "Error attempting to bind instance ", iface, t);
      }
    }
    try {
      writeMeta(injectionTargets, target, singletonDir);
    } catch( final Exception e) {e.printStackTrace();}

    injectionTargets.clear();

    //determine injection by priority
    for (final ClassFile cls : defaultInstances.forEach()){
      final Annotation impl = cls.getAnnotation(InstanceDefault.class.getName());
      final ClassMemberValue value = (ClassMemberValue)impl.getMemberValue("implFor");
      injectionTargets.put(value.getValue(), cls);
    }
    for (final ClassFile cls : instanceImpls.forEach()){
      final Annotation impl = cls.getAnnotation(InstanceOverride.class.getName());
      final ClassMemberValue value = (ClassMemberValue)impl.getMemberValue("implFor");
      final String clsName = value.getValue();
      final ClassFile existing  = injectionTargets.get(value.getValue());
      if (existing  == null){
        injectionTargets.put(clsName, cls);
        continue;
      }
      final ClassFile previous = injectionTargets.get(clsName);
      // previous value was a default
      if (previous == null){
        injectionTargets.put(clsName, cls);
        continue;
      }
      final Annotation oldOverride = previous.getAnnotation(InstanceOverride.class.getName());
      if (oldOverride == null) {
        injectionTargets.put(clsName, cls);
        continue;
      }
      final IntegerMemberValue oldPriority = (IntegerMemberValue)oldOverride.getMemberValue("priority");
      final IntegerMemberValue newPriority = (IntegerMemberValue)impl.getMemberValue("priority");

      int oldP = oldPriority == null ? Integer.MIN_VALUE : oldPriority.getValue();
      int newP = newPriority == null ? Integer.MIN_VALUE : newPriority.getValue();

      if (newP > oldP){
        injectionTargets.put(clsName, cls);
      }
    }
    for (final String iface : injectionTargets.keySet()) {
      try {
        JavaInjector.registerInstanceProvider(targetClassloader(), iface, injectionTargets.get(iface).getName());
      } catch (Throwable t) {
        if (X_Util.unwrap(t) instanceof InterruptedException) {
          break;
        }
        X_Log.warn(RuntimeInjector.class, "Error attempting to bind instance ", iface, t);
      }
    }
    try{
      writeMeta(injectionTargets, target, instanceDir);
    } catch (final Throwable e) {
      X_Log.warn("Trouble encountered writing instance meta to ",new File(target, instanceDir),e);
    }
    final Moment finished = now();

    scanner.shutdown();
    resources.stop();

    final Moment shutdown = now();

    if (X_Runtime.isDebug()) {
      X_Log.info("Multithreaded? ", X_Runtime.isMultithreaded());
      X_Log.info("Prepped: ", X_Time.difference(start, prepped));
      X_Log.info("Scanned: ", X_Time.difference(prepped, scanned));
      X_Log.info("Checked: ", X_Time.difference(scanned, checked));
      X_Log.info("Mapped: ", X_Time.difference(checked, mapped));
      X_Log.info("Analyzed: ", X_Time.difference(mapped, startInject));
      X_Log.info("Injected: ", X_Time.difference(startInject, finished));
      X_Log.info("Shutdown: ", X_Time.difference(finished, shutdown));
      X_Log.info("Total: ", X_Time.difference(start, shutdown));
    }

  }

  private ClassMemberValue implFor(Annotation anno) {
    return (ClassMemberValue) anno.getMemberValue("implFor");
  }

  protected boolean needsSingletonBinding(ClassMemberValue cls) {
    String name = "META-INF/singletons/" + cls.getValue();
    return targetClassloader().getResource(name) == null;
  }

  protected boolean needsInstanceBinding(ClassMemberValue cls) {
    String name = "META-INF/instances/" + cls.getValue();
    return targetClassloader().getResource(name) == null;
  }

  /**
   * @return
   */
  protected ClassLoader targetClassloader() {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl.getClass().getName().contains("gradle")) {
      cl = RuntimeInjector.class.getClassLoader();
    }
    return cl;
  }

  private final double init = System.nanoTime();
  private Moment now() {
    return new ImmutableMoment(System.currentTimeMillis()+ Math.abs(System.nanoTime()-init)/100000000.0);
  }
  private boolean allowedPlatform(final ClassFile cls, final Set<String> scopes, final Iterable<ClassFile> platforms) {
    // first, if the given class file has a platform we are using, return true
    for (final String allowed : scopes) {
      if (cls.getAnnotation(allowed) != null) {
        return true;
      }
    }
    // next, check if the type has any known platforms
    for (final ClassFile platform : platforms) {
      if (cls.getAnnotation(platform.getName())!=null) {
        System.out.println("Skipping "+cls.getName()+" for having a platform " +
        		"the does not match the current runtime.");
        return false;
      }
    }
    // this is a universal class (no platform specified)
    return true;
  }
  private NotConfiguredCorrectly platformMisconfigured(final String platform) {
    String errMsg = "Could not find platform annotation for " +
            "current runtime " + platform + "; please ensure this class is on the " +
            "classpath, and that it is annotated with @Platform. ";
    Throwable trace = new Throwable(errMsg);
    trace.fillInStackTrace();
    trace.printStackTrace();
    final NotConfiguredCorrectly error = new NotConfiguredCorrectly(errMsg
        +Arrays.asList(trace.getStackTrace())
    );
    throw error;
  }
  protected void writeMeta(
      final Map<String, ClassFile> injectables,
      final File target,
      String instanceDir
  ) {
    File dir = new File(instanceDir);
    if (!dir.isAbsolute()) {
      dir = new File(target, instanceDir);
    }
    X_Log.info(RuntimeInjector.class, "Writing meta to ",dir.getAbsoluteFile());

    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        throw new RuntimeException("Unable to create meta info directory for "+dir.getAbsolutePath());
      }
    }

    if (dir.getAbsolutePath().contains("jar")) {
      throw new IllegalArgumentException("Cannot write meta info to jar " + dir);
    }

    mainLoop:
    for (final String iface : injectables.keySet()){
      final File metaInf = new File(dir, iface);
      final String impl = injectables.get(iface).getName();
      X_Log.debug("Injecting ",iface," -> ",impl);
      try{
        if (metaInf.exists()){
          // when the file exists, we must append to the top of it,
          // but only if it doesn't already contain our target.
          final BufferedReader reader = new BufferedReader(new FileReader(metaInf));
          final ArrayList<String> lines = new ArrayList<String>();
          try {
            String line;
            while((line = reader.readLine()) != null){
              if (line.equals(impl)) {
                continue mainLoop;
              }
            }
          }finally {
            reader.close();
          }
          final BufferedWriter writer = new BufferedWriter(new FileWriter(metaInf));
          try {

          writer.append(impl);
          writer.append("\n");
          for (final String line : lines){
            writer.append(line);
            writer.append("\n");
          }
          writer.flush();
          }finally {
            writer.close();
          }
        }else{
          if (!metaInf.createNewFile()) {
            throw new RuntimeException("Unable to create meta info for "+metaInf.getAbsolutePath()+".  " +
                "Please ensure java has write permissions for "+metaInf.getParent());
          }
          final FileWriter writer = new FileWriter(metaInf);
          try {
            writer.append(impl);
            writer.flush();
          }finally {
            writer.close();
          }
        }
      }catch(final Exception e){
        throw new RuntimeException("Unable to create meta info for "+metaInf.getAbsolutePath(), e);
      }
    }

  }



}
