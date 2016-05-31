package xapi.jre.inject;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.bytecode.ClassFile;
import xapi.bytecode.annotation.Annotation;
import xapi.bytecode.annotation.ArrayMemberValue;
import xapi.bytecode.annotation.ClassMemberValue;
import xapi.bytecode.annotation.IntegerMemberValue;
import xapi.bytecode.annotation.MemberValue;
import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.dev.scanner.api.ClasspathScanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.dev.scanner.impl.ClasspathScannerDefault;
import xapi.except.NotConfiguredCorrectly;
import xapi.inject.X_Inject;
import xapi.inject.impl.JavaInjector;
import xapi.log.X_Log;
import xapi.platform.Platform;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.time.impl.ImmutableMoment;
import xapi.util.X_Properties;
import xapi.util.X_Runtime;
import xapi.util.api.ReceivesValue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class RuntimeInjector implements ReceivesValue<String> {

  @Override
  public void set(final String targetDir) {
    final String prefix = "META-INF"+File.separator;
    writeMetaInfo(targetDir, prefix+"singletons", prefix+"instances");
  }

  @SuppressWarnings("unchecked")
  public void writeMetaInfo(String targetDir, final String singletonDir, final String instanceDir){
    if (!targetDir.endsWith(File.separator)) {
      targetDir += File.separator;
    }
    final File target = new File(targetDir).getAbsoluteFile();
    if (!target.isDirectory()) {
      if (!target.mkdirs()) {
        throw new RuntimeException("Unable to get or make jre injection generator output directory: "+
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
      .matchResource("META[-]INF/(instances|singletons)")
      .matchClassFile(".*")
      .scan(targetClassloader())
      ;
    // Only collect platform types if we are not running in a known platform.
    final String runtime[] = X_Properties.platform.get().split(",");

    final Fifo<ClassFile> defaultSingletons = new SimpleFifo<ClassFile>();
    final Fifo<ClassFile> defaultInstances = new SimpleFifo<ClassFile>();
    final Fifo<ClassFile> singletonImpls = new SimpleFifo<ClassFile>();
    final Fifo<ClassFile> instanceImpls = new SimpleFifo<ClassFile>();

    ClassFile bestMatch = null;
    final HashMap<String,ClassFile> platformMap = new HashMap<String, ClassFile>();
    String shortName = null;
    final Set<String> scopes = new LinkedHashSet<String>();
    final ArrayList<ClassFile> platforms = new ArrayList<ClassFile>();
    final Moment prepped = now();
    for (final ClassFile file : resources.findClassAnnotatedWith(Platform.class)) {
      platforms.add(file);
      for (final String platform : runtime) {
        scopes.add(platform);
        shortName = platform.substring(platform.lastIndexOf('.')+1);
        platformMap.put(file.getName(), file);
        if (file.getName().equals(runtime)) {
          bestMatch = file;
        }
        if (bestMatch == null && file.getName().endsWith(shortName)) {
          bestMatch = file;
        }
      }
    }
    final Moment scanned = now();
    if (bestMatch == null) {
      throw platformMisconfigured(shortName);
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
      if (fallbacks != null) {
        final ArrayMemberValue arr = (ArrayMemberValue)fallbacks;
        for (final MemberValue v : arr.getValue()) {
          final String name = ((ClassMemberValue)v).getValue();
          final ClassFile fallback = platformMap.get(name);
          if (fallback != null) {
            if (scopes.add(fallback.getName())) {
              remainder.add(fallback);
            }
          }
        }
      }
      remainder.remove(next);
    }
    final Moment checked = now();
    for (final ClassFile cls : resources.findClassAnnotatedWith(
      SingletonDefault.class, SingletonOverride.class,
      InstanceDefault.class, InstanceOverride.class
      )) {
      Annotation anno = cls.getAnnotation(SingletonDefault.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms)) {
        defaultSingletons.give(cls);
      }
      anno = cls.getAnnotation(SingletonOverride.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms)) {
        singletonImpls.give(cls);
      }
      anno = cls.getAnnotation(InstanceDefault.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms)) {
        defaultInstances.give(cls);
      }
      anno = cls.getAnnotation(InstanceOverride.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms)) {
        instanceImpls.give(cls);
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
      JavaInjector.registerSingletonProvider(iface, injectionTargets.get(iface).getName());
    }
    try {
      writeMeta(injectionTargets, new File(target, singletonDir));
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

      if (newPriority.getValue() > oldPriority.getValue()){
        injectionTargets.put(clsName, cls);
      }
    }
    for (final String iface : injectionTargets.keySet()) {
      JavaInjector.registerInstanceProvider(iface, injectionTargets.get(iface).getName());
    }
    try{
      writeMeta(injectionTargets, new File(target, instanceDir));
    } catch (final Throwable e) {
      X_Log.warn("Trouble encountered writing instance meta to ",new File(target, instanceDir),e);
    }
    final Moment finished = now();

    if (X_Runtime.isDebug()) {
      X_Log.info("Multithreaded? ", X_Runtime.isMultithreaded());
      X_Log.info("Prepped: ", X_Time.difference(start, prepped));
      X_Log.info("Scanned: ", X_Time.difference(prepped, scanned));
      X_Log.info("Checked: ", X_Time.difference(scanned, checked));
      X_Log.info("Mapped: ", X_Time.difference(checked, mapped));
      X_Log.info("Analyzed: ", X_Time.difference(mapped, startInject));
      X_Log.info("Injected: ", X_Time.difference(startInject, finished));
      X_Log.info("Total: ", X_Time.difference(start, finished));
    }

  }
  /**
   * @return
   */
  protected ClassLoader targetClassloader() {
    return Thread.currentThread().getContextClassLoader();
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
    return new NotConfiguredCorrectly("Could not find platform annotation for " +
      "current runtime "+platform+"; please ensure this class is on the " +
      "classpath, and that it is annotated with @Platform");
  }
  protected void writeMeta(final Map<String, ClassFile> injectables,
      final File target) {
    X_Log.info(getClass(), "Writing meta to ",target.getAbsoluteFile());

    if (!target.exists()) {
      if (!target.mkdirs()) {
        throw new RuntimeException("Unable to create meta info directory for "+target.getAbsolutePath());
      }
    }

    mainLoop:
    for (final String iface : injectables.keySet()){
      final File metaInf = new File(target, iface);
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
