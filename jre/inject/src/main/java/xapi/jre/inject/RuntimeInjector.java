package xapi.jre.inject;

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

public class RuntimeInjector implements ReceivesValue<String> {
  
  @Override
  public void set(String targetDir) {
    String prefix = "META-INF"+File.separator;
    writeMetaInfo(targetDir, prefix+"singletons", prefix+"instances");
  }
  
  @SuppressWarnings("unchecked")
  public void writeMetaInfo(String targetDir, String singletonDir, String instanceDir){
    if (!targetDir.endsWith(File.separator))
      targetDir += File.separator;
    File target = new File(targetDir).getAbsoluteFile();
    if (!target.isDirectory())
      if (!target.mkdirs())
        throw new RuntimeException("Unable to get or make jre injection generator output directory: "+
            target.getAbsolutePath());

    ClasspathScanner scanner;
    try {
      scanner = X_Inject.instance(ClasspathScanner.class);
    } catch (Exception e) {
      scanner = new ClasspathScannerDefault();
    }
    Moment start = now();
	ClasspathResourceMap resources = scanner
      .scanAnnotations(
        Platform.class,
        SingletonDefault.class, SingletonOverride.class,
        InstanceDefault.class, InstanceOverride.class
      )
      .matchResource("META[-]INF/(instances|singletons)")
      .matchClassFile(".*")
      .scan(Thread.currentThread().getContextClassLoader())
      ;
    // Only collect platform types if we are not running in a known platform.
    String runtime[] = X_Properties.platform.get().split(",");

    Fifo<ClassFile> defaultSingletons = new SimpleFifo<ClassFile>();
    Fifo<ClassFile> defaultInstances = new SimpleFifo<ClassFile>();
    Fifo<ClassFile> singletonImpls = new SimpleFifo<ClassFile>();
    Fifo<ClassFile> instanceImpls = new SimpleFifo<ClassFile>();

    ClassFile bestMatch = null;
    HashMap<String,ClassFile> platformMap = new HashMap<String, ClassFile>();
    String shortName = null;
    Set<String> scopes = new LinkedHashSet<String>();
    ArrayList<ClassFile> platforms = new ArrayList<ClassFile>();
    Moment prepped = now();
    for (ClassFile file : resources.findClassAnnotatedWith(Platform.class)) {
      platforms.add(file);
      for (String platform : runtime) {
        scopes.add(platform);
        shortName = platform.substring(platform.lastIndexOf('.')+1);
        platformMap.put(file.getName(), file);
        if (file.getName().equals(runtime)) {
          bestMatch = file;
        }
        if (bestMatch == null && file.getName().endsWith(shortName))
          bestMatch = file;
          }
    }
    Moment scanned = now();
    if (bestMatch == null) {
      throw platformMisconfigured(shortName);
    }
    MemberValue fallbacks;
    LinkedHashSet<ClassFile> remainder = new LinkedHashSet<ClassFile>();
    remainder.add(bestMatch);
    while (remainder.size()>0) {
      ClassFile next = remainder.iterator().next();
      Annotation anno = next.getAnnotation("xapi.platform.Platform");
      if (anno == null) {
        throw platformMisconfigured(next.getName());
      }
      fallbacks = anno.getMemberValue("fallback");
      if (fallbacks != null) {
        ArrayMemberValue arr = (ArrayMemberValue)fallbacks;
        for (MemberValue v : arr.getValue()) {
          String name = ((ClassMemberValue)v).getValue();
          ClassFile fallback = platformMap.get(name);
          if (fallback != null) {
            if (scopes.add(fallback.getName())) {
              remainder.add(fallback);
            }
          }
        }
      }
      remainder.remove(next);
    }
    Moment checked = now();
    for (ClassFile cls : resources.findClassAnnotatedWith(
      SingletonDefault.class, SingletonOverride.class,
      InstanceDefault.class, InstanceOverride.class
      )) {
      Annotation anno = cls.getAnnotation(SingletonDefault.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms))
          defaultSingletons.give(cls);
      anno = cls.getAnnotation(SingletonOverride.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms))
        singletonImpls.give(cls);
      anno = cls.getAnnotation(InstanceDefault.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms))
        defaultInstances.give(cls);
      anno = cls.getAnnotation(InstanceOverride.class.getName());
      if (anno != null && allowedPlatform(cls, scopes, platforms))
        instanceImpls.give(cls);
    }
    Moment mapped = now();

    Map<String, ClassFile> injectionTargets = new HashMap<String,ClassFile>();
    //determine injection by priority.  Defaults first
    for (ClassFile cls : defaultSingletons.forEach()){
      Annotation impl = cls.getAnnotation(SingletonDefault.class.getName());
      ClassMemberValue value = (ClassMemberValue)impl.getMemberValue("implFor");
      injectionTargets.put(value.getValue(), cls);
    }
    //now scan overrides
    for (ClassFile cls : singletonImpls.forEach()){
      Annotation impl = cls.getAnnotation(SingletonOverride.class.getName());
      ClassMemberValue value = (ClassMemberValue)impl.getMemberValue("implFor");
      String clsName = value.getValue();
      ClassFile existing  = injectionTargets.get(value.getValue());
      if (existing  == null){
        injectionTargets.put(clsName, cls);
        continue;
      }


      ClassFile previous = injectionTargets.get(clsName);
      // previous value was a default
      if (previous == null){
        injectionTargets.put(clsName, cls);
        continue;
      }
      Annotation oldOverride = previous.getAnnotation(SingletonOverride.class.getName());
      if (oldOverride == null) {
        injectionTargets.put(clsName, cls);
        continue;
      }
      IntegerMemberValue oldPriority = (IntegerMemberValue)oldOverride.getMemberValue("priority");
      IntegerMemberValue newPriority = (IntegerMemberValue)impl.getMemberValue("priority");
      if (newPriority == null)
        continue;
      if (oldPriority == null || newPriority.getValue() > oldPriority.getValue()){
        injectionTargets.put(clsName, cls);
      }
    }
    Moment startInject = now();
    for (String iface : injectionTargets.keySet()) {
      JavaInjector.registerSingletonProvider(iface, injectionTargets.get(iface).getName());
    }
    try {
      writeMeta(injectionTargets, new File(target, singletonDir));
    } catch( Exception e) {e.printStackTrace();}
    
    injectionTargets.clear();

    //determine injection by priority
    for (ClassFile cls : defaultInstances.forEach()){
      Annotation impl = cls.getAnnotation(InstanceDefault.class.getName());
      ClassMemberValue value = (ClassMemberValue)impl.getMemberValue("implFor");
      injectionTargets.put(value.getValue(), cls);
    }
    for (ClassFile cls : instanceImpls.forEach()){
      Annotation impl = cls.getAnnotation(InstanceOverride.class.getName());
      ClassMemberValue value = (ClassMemberValue)impl.getMemberValue("implFor");
      String clsName = value.getValue();
      ClassFile existing  = injectionTargets.get(value.getValue());
      if (existing  == null){
        injectionTargets.put(clsName, cls);
        continue;
      }
      ClassFile previous = injectionTargets.get(clsName);
      // previous value was a default
      if (previous == null){
        injectionTargets.put(clsName, cls);
        continue;
      }
      Annotation oldOverride = previous.getAnnotation(InstanceOverride.class.getName());
      if (oldOverride == null) {
        injectionTargets.put(clsName, cls);
        continue;
      }
      IntegerMemberValue oldPriority = (IntegerMemberValue)oldOverride.getMemberValue("priority");
      IntegerMemberValue newPriority = (IntegerMemberValue)impl.getMemberValue("priority");

      if (newPriority.getValue() > oldPriority.getValue()){
        injectionTargets.put(clsName, cls);
      }
    }
    for (String iface : injectionTargets.keySet()) {
      JavaInjector.registerInstanceProvider(iface, injectionTargets.get(iface).getName());
    }
    try{ 
      writeMeta(injectionTargets, new File(target, instanceDir));
    } catch (Throwable e) {
      X_Log.warn("Trouble encountered writing instance meta to ",new File(target, instanceDir),e);
    }
    Moment finished = now();
    
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
  private final double init = System.nanoTime();
  private Moment now() {
    return new ImmutableMoment(System.currentTimeMillis()+ Math.abs(System.nanoTime()-init)/100000000.0);
  }
  private boolean allowedPlatform(ClassFile cls, Set<String> scopes, Iterable<ClassFile> platforms) {
    // first, if the given class file has a platform we are using, return true
    for (String allowed : scopes) {
      if (cls.getAnnotation(allowed) != null) {
        return true;
      }
    }
    // next, check if the type has any known platforms
    for (ClassFile platform : platforms) {
      if (cls.getAnnotation(platform.getName())!=null) {
        System.out.println("Skipping "+cls.getName()+" for having a platform " +
        		"the does not match the current runtime.");
        return false;
      }
    }
    // this is a universal class (no platform specified)
    return true;
  }
  private NotConfiguredCorrectly platformMisconfigured(String platform) {
    return new NotConfiguredCorrectly("Could not find platform annotation for " +
      "current runtime "+platform+"; please ensure this class is on the " +
      "classpath, and that it is annotated with @Platform");
  }
  protected void writeMeta(Map<String, ClassFile> injectables,
      File target) {
    X_Log.info("Writing meta to ",target.getAbsoluteFile());

    if (!target.exists())
      if (!target.mkdirs())
        throw new RuntimeException("Unable to create meta info directory for "+target.getAbsolutePath());

    mainLoop:
    for (String iface : injectables.keySet()){
      File metaInf = new File(target, iface);
      String impl = injectables.get(iface).getName();
      X_Log.debug("Injecting ",iface," -> ",impl);
      try{
        if (metaInf.exists()){
          // when the file exists, we must append to the top of it,
          // but only if it doesn't already contain our target.
          BufferedReader reader = new BufferedReader(new FileReader(metaInf));
          ArrayList<String> lines = new ArrayList<String>();
          try {
            String line;
            while((line = reader.readLine()) != null){
              if (line.equals(impl))
                continue mainLoop;
            }
          }finally {
            reader.close();
          }
          BufferedWriter writer = new BufferedWriter(new FileWriter(metaInf));
          try {

          writer.append(impl);
          writer.append("\n");
          for (String line : lines){
            writer.append(line);
            writer.append("\n");
          }
          writer.flush();
          }finally {
            writer.close();
          }
        }else{
          if (!metaInf.createNewFile())
            throw new RuntimeException("Unable to create meta info for "+metaInf.getAbsolutePath()+".  " +
                "Please ensure java has write permissions for "+metaInf.getParent());
          FileWriter writer = new FileWriter(metaInf);
          try {
            writer.append(impl);
            writer.flush();
          }finally {
            writer.close();
          }
        }
      }catch(Exception e){
        throw new RuntimeException("Unable to create meta info for "+metaInf.getAbsolutePath(), e);
      }
    }

  }



}