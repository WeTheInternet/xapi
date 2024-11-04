/*
 * Copyright 2012, We The Internet Ltd.
 *
 * All rights reserved.
 *
 * Distributed under a modified BSD License as follow:
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution, unless otherwise
 * agreed to in a written document signed by a director of We The Internet Ltd.
 *
 * Neither the name of We The Internet nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package xapi.dev.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

/**
 * A handy state-blob of all available types from the TypeOracle or runtime
 * reflection.  It is an expensive operation to iterate all types per generator,
 * so we store the state of active permutation targets here
 * and wrap the instance in a lazy singleton.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
public class GwtInjectionMap {

  final Map<Class<?>,JClassType> defaultSingletons = new HashMap<Class<?>,JClassType>();
  final Map<Class<?>,JClassType> defaultInstances = new HashMap<Class<?>,JClassType>();
  final Map<Class<?>,JClassType> gwtSingletons = new HashMap<Class<?>,JClassType>();
  final Map<Class<?>,JClassType> gwtInstances = new HashMap<Class<?>,JClassType>();
  final Map<String,InjectionCallbackArtifact> injectionArtifacts = new HashMap<String,InjectionCallbackArtifact>();
  private final PlatformSet platforms;

  public GwtInjectionMap(TreeLogger logger, GeneratorContext context) {
    this.platforms = CurrentGwtPlatform.getPlatforms(context);
    init(logger, context);
  }

  // visible for testing
  protected void init(TreeLogger logger, GeneratorContext context) {
    TypeOracle oracle = context.getTypeOracle();


    JClassType[] types = oracle.getTypes();
    for (JClassType type : types) {
      SingletonDefault defaultSingleton = type.getAnnotation(SingletonDefault.class);
      if (defaultSingleton != null && platforms.isAllowedType(type)) {
        JClassType old = defaultSingletons.get(defaultSingleton.implFor());
        if (old == null) {
          defaultSingletons.put(defaultSingleton.implFor(), type);
        } else {
          JClassType better = platforms.prefer(type, old, SingletonDefault.class);
          if (better == type) {
            defaultSingletons.put(defaultSingleton.implFor(), type);
          }
        }
      }

      InstanceDefault instanceDefault = type.getAnnotation(InstanceDefault.class);
      if (instanceDefault != null && platforms.isAllowedType(type)) {
        JClassType old = defaultInstances.get(instanceDefault.implFor());
        if (old == null) {
          defaultInstances.put(instanceDefault.implFor(), type);
        } else {
          JClassType better = platforms.prefer(type, old, InstanceDefault.class);
          if (better == type) {
            defaultInstances.put(instanceDefault.implFor(), type);
          }
        }
      }

      extractSingletonOverrides(logger, type, context);
      extractInstanceOverrides(logger, type, context);

    }
    if (logger.isLoggable(Type.TRACE)) {
      dumpMaps(logger);
    }
  }

  void dumpMaps(TreeLogger logger) {
    logger.log(Type.TRACE, "***************************");
    logger.log(Type.TRACE, "Dumping gwt injection map:");
    logger.log(Type.TRACE, "***************************");

    TreeLogger
    branch = logger.branch(Type.TRACE, "SingletonDefaults:");
    for (Class<?> key: defaultSingletons.keySet()) {
      branch.log(Type.TRACE, key.getName()+" -> "+defaultSingletons.get(key).getQualifiedSourceName());
    }
    branch = logger.branch(Type.TRACE, "InstanceDefaults:");
    for (Class<?> key: defaultInstances.keySet()) {
      branch.log(Type.TRACE, key.getName()+" -> "+defaultInstances.get(key).getQualifiedSourceName());
    }

    branch = logger.branch(Type.TRACE, "SingletonOverrides:");
    for (Class<?> key: gwtSingletons.keySet()) {
      branch.log(Type.TRACE, key.getName()+" -> "+gwtSingletons.get(key).getQualifiedSourceName());
    }
    branch = logger.branch(Type.TRACE, "InstanceOverrides:");
    for (Class<?> key: gwtInstances.keySet()) {
      branch.log(Type.TRACE, key.getName()+" -> "+gwtInstances.get(key).getQualifiedSourceName());
    }
  }

  protected void extractSingletonOverrides(TreeLogger logger, JClassType type, GeneratorContext context) {
    SingletonOverride singletonOverride = type.getAnnotation(SingletonOverride.class);
    if (singletonOverride == null) return;
    if (platforms.isAllowedType(type)) {
      JClassType override = gwtSingletons.get(singletonOverride.implFor());
      if (override == null) {
        gwtSingletons.put(singletonOverride.implFor(), type);
      } else {
        // TODO: have a config setting for "prefer platform or prefer priority";
        // currently, we prefer platform matches first, then sorted by priority.
        // The flag would make this check only look at priority, and ignore platform.
        JClassType best = platforms.prefer(type, override, SingletonOverride.class);
        logger.log(Type.WARN, best.getSimpleSourceName()+" chosen out of " +
        		type.getSimpleSourceName()+" and "+override.getSimpleSourceName());
        if (best == type) {
          gwtSingletons.put(singletonOverride.implFor(), type);
        }
      }
    }
  }

  protected void extractInstanceOverrides(TreeLogger logger, JClassType type, GeneratorContext context) {
    InstanceOverride instanceOverride = type.getAnnotation(InstanceOverride.class);
    if (instanceOverride != null) {
      if (platforms.isAllowedType(type)) {
        JClassType override = gwtInstances.get(instanceOverride.implFor());
        if (override == null) {
          gwtInstances.put(instanceOverride.implFor(), type);
        } else {
          JClassType best = platforms.prefer(type, override, InstanceOverride.class);
          if (best == type) {
            gwtInstances.put(instanceOverride.implFor(), type);
          }
        }
      }
    }
  }


  public synchronized Set<Entry<Class<?>,JClassType>> getGwtSingletons() {
    HashSet<Class<?>> keys = new HashSet<Class<?>>();
    // add anything missing to overrides map.
    keys.addAll(defaultSingletons.keySet());
    keys.removeAll(gwtSingletons.keySet());
    for (Class<?> key : keys) {
      gwtSingletons.put(key, defaultSingletons.get(key));
    }
    return gwtSingletons.entrySet();
  }

  public synchronized Set<Entry<Class<?>,JClassType>> getGwtInstances() {
    HashSet<Class<?>> keys = new HashSet<Class<?>>();
    // add anything missing to overrides map.
    keys.addAll(defaultInstances.keySet());
    keys.removeAll(gwtInstances.keySet());
    for (Class<?> key : keys) {
      gwtInstances.put(key, defaultInstances.get(key));
    }
    return gwtInstances.entrySet();
  }

  public InjectionCallbackArtifact getOrCreateArtifact(GeneratorContext ctx, String packageName,
    String className) {
    InjectionCallbackArtifact artifact = new InjectionCallbackArtifact(packageName, className);
    synchronized (injectionArtifacts) {
      if (injectionArtifacts.containsKey(artifact.getCanonicalName())) {
        return injectionArtifacts.get(artifact.getCanonicalName());
      }
      injectionArtifacts.put(artifact.getCanonicalName(), artifact);
    }
    return artifact;
  }

  public Iterable<InjectionCallbackArtifact> getArtifacts() {
    return injectionArtifacts.values();
  }
}
