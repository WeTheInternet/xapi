package com.google.gwt.reflect.rebind;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.rebind.injectors.MagicClassInjector;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

public class ReflectionManifest {
  
  private static final ThreadLocal <Map<String, ReflectionManifest>> cache = new ThreadLocal<Map<String,ReflectionManifest>>() {
    protected java.util.Map<String,ReflectionManifest> initialValue() {
      return new HashMap<String, ReflectionManifest>();
    };
  };
  
  public static ReflectionManifest getReflectionManifest(TreeLogger logger, String clsName,
      StandardGeneratorContext ctx) throws UnableToCompleteException {
    Map<String, ReflectionManifest> map = cache.get();
    ReflectionManifest manifest = map.get(clsName);
    if (manifest == null) {
      TypeOracle oracle = ctx.getTypeOracle();
      JClassType type = oracle.findType(clsName.replace('$', '.'));
      if (type == null) {
        logger.log(Type.ERROR, "Could not find type "+clsName+" while getting reflection manifest.");
        throw new UnableToCompleteException();
      }
      ReflectionStrategy strategy = getStrategy(logger, type);
      manifest = new ReflectionManifest(strategy, type);
      map.put(clsName, manifest);
    }
    return manifest;
  }
  

  private static ReflectionStrategy getStrategy(TreeLogger logger, JClassType type) {
    ReflectionStrategy strategy = type.getAnnotation(ReflectionStrategy.class);
    if (strategy == null) {
      strategy = type.getPackage().getAnnotation(ReflectionStrategy.class);
      while (strategy == null && type.getSuperclass() != null) {
        type = type.getSuperclass();
        strategy = type.getAnnotation(ReflectionStrategy.class);
      }
      if (strategy == null) {
        strategy = MagicClassInjector.getDefaultStrategy();
      }
    }
    return strategy;
  }

  private final Map<String, ReflectionUnit<JMethod>> declaredMethods = Maps.newLinkedHashMap();
  private final Map<String, ReflectionUnit<JConstructor>> declaredConstructors = Maps.newLinkedHashMap();
  private final Map<String, ReflectionUnit<JField>> declaredFields = Maps.newLinkedHashMap();
  private final Set<String> overloadedMethods = Sets.newHashSet();
  
  private transient com.google.gwt.core.ext.typeinfo.JClassType type;
  
  @Deprecated
  public transient Map<JMethod, Annotation[]> methods = new LinkedHashMap<JMethod, Annotation[]>();
  @Deprecated
  public transient Map<JField, Annotation[]> fields = new LinkedHashMap<JField, Annotation[]>();
  @Deprecated
  public transient Map<JConstructor, Annotation[]> constructors = new LinkedHashMap<JConstructor, Annotation[]>();
  @Deprecated
  public transient Map<JClassType, Annotation[]> innerClasses = new LinkedHashMap<JClassType, Annotation[]>();
  @Deprecated
  protected transient List<Annotation> annotations = new ArrayList<Annotation>();
  
  private ReflectionStrategy strategy;
  
  public ReflectionManifest(ReflectionStrategy strategy, JClassType type) {
    assert strategy != null;
    this.strategy = strategy;
    this.type = type;
    read(type);
  }

  protected void read(JClassType type) {
    declaredMethods.clear();
    declaredConstructors.clear();
    declaredFields.clear();
    final String typeName = type.getQualifiedSourceName();
    for (JField field : type.getFields()) {
      if (field.getName().equals("serialVersionUID"))
        continue;
      if (field.getEnclosingType().getQualifiedSourceName().equals(typeName)) {
        GwtRetention retention = field.getAnnotation(GwtRetention.class);
        if (retention == null)
          retention = strategy.fieldRetention();
        declaredFields.put(field.getName(), new ReflectionUnit<JField>(field, retention));
      }
    }
    for (JConstructor ctor : type.getConstructors()) {
      if (ctor.getEnclosingType().getQualifiedSourceName().equals(typeName)) {
        GwtRetention retention = ctor.getAnnotation(GwtRetention.class);
        if (retention == null)
          retention = strategy.fieldRetention();
        declaredConstructors.put(ctor.getJsniSignature(), new ReflectionUnit<JConstructor>(ctor, retention));
      }
    }
    Set<String> uniqueNames = Sets.newHashSet();
    for (JMethod method : type.getMethods()) {
      if (method.getEnclosingType().getQualifiedSourceName().equals(typeName)) {
        GwtRetention retention = method.getAnnotation(GwtRetention.class);
        if (retention == null)
          retention = strategy.methodRetention();
        declaredMethods.put(method.getJsniSignature(), new ReflectionUnit<JMethod>(method, retention));
        if (!uniqueNames.add(method.getName())) {
          overloadedMethods.add(method.getName());
        }
      }
    }
  }
  
  public ReflectionStrategy getStrategy() {
    return strategy;
  }

  public com.google.gwt.core.ext.typeinfo.JClassType getType() {
    return type;
  }
  
  public boolean isOverloaded(String methodName) {
    return overloadedMethods.contains(methodName);
  }
  
  public Collection<ReflectionUnit<JConstructor>> getConstructors() {
    return declaredConstructors.values();
  }
  
  public Collection<ReflectionUnit<JField>> getFields() {
    return declaredFields.values();
  }

  public Collection<ReflectionUnit<JMethod>> getMethods() {
    return declaredMethods.values();
  }
  
  public GwtRetention getRetention(JConstructor ctor) {
    GwtRetention retention = ctor.getAnnotation(GwtRetention.class);
    if (retention == null) {
      retention = strategy.constructorRetention();
    }
    return retention;
  }
  
  public GwtRetention getRetention(JField field) {
    GwtRetention retention = field.getAnnotation(GwtRetention.class);
    if (retention == null) {
      retention = strategy.fieldRetention();
    }
    return retention;
  }

  public GwtRetention getRetention(JMethod method) {
    GwtRetention retention = method.getAnnotation(GwtRetention.class);
    if (retention == null) {
      retention = strategy.methodRetention();
    }
    return retention;
  }


  public ArrayList<JMethod> getMethodsNamed(
      TreeLogger logger, String name, boolean declaredOnly, GeneratorContext ctx) {
    ArrayList<JMethod> list = new ArrayList<JMethod>();
    JClassType from = type;
    HashSet<String> seen = new HashSet<String>();
    while (from != null) {
      for (JMethod method : from.getMethods()) {
        if (method.getName().equals(name)) {
          if (seen.add(ReflectionUtilType.toJsniClassLits(method.getParameterTypes())))
            list.add(method);
        }
      }
      if (declaredOnly)
        return list;
      from = from.getSuperclass();
    }
    return list;
  }

}