package com.google.gwt.reflect.rebind;

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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ReflectionManifest {

  private static final ThreadLocal <Map<String, ReflectionManifest>> cache = new ThreadLocal<Map<String,ReflectionManifest>>() {
    @Override
    protected java.util.Map<String,ReflectionManifest> initialValue() {
      return new HashMap<String, ReflectionManifest>();
    };
  };

  public static void cleanup() {
    cache.remove();
  }

  public static ReflectionManifest getReflectionManifest(final TreeLogger logger, final String clsName,
      final StandardGeneratorContext ctx) throws UnableToCompleteException {
    final Map<String, ReflectionManifest> map = cache.get();
    ReflectionManifest manifest = map.get(clsName);
    if (manifest == null) {
      final TypeOracle oracle = ctx.getTypeOracle();
      final JClassType type = oracle.findType(clsName.replace('$', '.'));
      if (type == null) {
        logger.log(Type.ERROR, "Could not find type "+clsName+" while getting reflection manifest.");
        throw new UnableToCompleteException();
      }
      final ReflectionStrategy strategy = getStrategy(logger, type);
      manifest = new ReflectionManifest(strategy, type);
      map.put(clsName, manifest);
    }
    return manifest;
  }


  private static ReflectionStrategy getStrategy(final TreeLogger logger, JClassType type) {
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
  public transient Map<JClassType, Annotation[]> innerClasses = new LinkedHashMap<JClassType, Annotation[]>();

  private final ReflectionStrategy strategy;

  public ReflectionManifest(final ReflectionStrategy strategy, final JClassType type) {
    assert strategy != null;
    this.strategy = strategy;
    this.type = type;
    read(type);
  }

  protected void read(final JClassType type) {
    declaredMethods.clear();
    declaredConstructors.clear();
    declaredFields.clear();
    final String typeName = type.getQualifiedSourceName();
    for (final JField field : type.getFields()) {
      if (field.getName().equals("serialVersionUID")) {
        continue;
      }
      if (field.getEnclosingType().getQualifiedSourceName().equals(typeName)) {
        GwtRetention retention = field.getAnnotation(GwtRetention.class);
        if (retention == null) {
          retention = strategy.fieldRetention();
        }
        declaredFields.put(field.getName(), new ReflectionUnit<JField>(field, retention));
      }
    }
    for (final JConstructor ctor : type.getConstructors()) {
      if (ctor.getEnclosingType().getQualifiedSourceName().equals(typeName)) {
        GwtRetention retention = ctor.getAnnotation(GwtRetention.class);
        if (retention == null) {
          retention = strategy.fieldRetention();
        }
        declaredConstructors.put(ctor.getJsniSignature(), new ReflectionUnit<JConstructor>(ctor, retention));
      }
    }
    final Set<String> uniqueNames = Sets.newHashSet();
    for (final JMethod method : type.getMethods()) {
      if (method.getEnclosingType().getQualifiedSourceName().equals(typeName)) {
        GwtRetention retention = method.getAnnotation(GwtRetention.class);
        if (retention == null) {
          retention = strategy.methodRetention();
        }
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

  public boolean isOverloaded(final String methodName) {
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

  public GwtRetention getRetention(final JConstructor ctor) {
    GwtRetention retention = ctor.getAnnotation(GwtRetention.class);
    if (retention == null) {
      if (strategy.memberRetention().length == 0) {
        retention = strategy.constructorRetention();
      } else {
        retention = strategy.memberRetention()[0];
      }
    }
    return retention;
  }

  public GwtRetention getRetention(final JField field) {
    GwtRetention retention = field.getAnnotation(GwtRetention.class);
    if (retention == null) {
      if (strategy.memberRetention().length == 0) {
        retention = strategy.fieldRetention();
      } else {
        retention = strategy.memberRetention()[0];
      }
    }
    return retention;
  }

  public GwtRetention getRetention(final JMethod method) {
    GwtRetention retention = method.getAnnotation(GwtRetention.class);
    if (retention == null) {
      if (strategy.memberRetention().length == 0) {
        retention = strategy.methodRetention();
      } else {
        retention = strategy.memberRetention()[0];
      }
    }
    return retention;
  }


  public ArrayList<JMethod> getMethodsNamed(
      final TreeLogger logger, final String name, final boolean declaredOnly, final GeneratorContext ctx) {
    final ArrayList<JMethod> list = new ArrayList<JMethod>();
    JClassType from = type;
    final HashSet<String> seen = new HashSet<String>();
    while (from != null) {
      for (final JMethod method : from.getMethods()) {
        if (method.getName().equals(name)) {
          if (seen.add(ReflectionUtilType.toJsniClassLits(method.getParameterTypes()))) {
            list.add(method);
          }
        }
      }
      if (declaredOnly) {
        return list;
      }
      from = from.getSuperclass();
    }
    return list;
  }

}