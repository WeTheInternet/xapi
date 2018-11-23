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
package xapi.inject.impl;

import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.inject.api.Injector;
import xapi.log.X_Log;
import xapi.util.X_Runtime;
import xapi.util.X_Util;

import java.lang.reflect.Method;

public class JavaInjector {

  /**
   * This is a wrapper proxy for any types that want a lazy-loaded provider.
   *
   * Subclasses may want to customize the behavior of this provider, perhaps
   * to implement hotswapping or class preloading.
   *
   * @author "James X. Nelson (james@wetheinter.net)"
   *
   * @param <X>
   */
  private static class LazySingletonInjector <X> extends Lazy<X> {
    public <C extends Class<? extends X>> LazySingletonInjector(C cls) {
      super(()->singleton.out1().provide(cls));
    }
  }

  /**
     * A singleton wrapper class to allow lazy loading of the XInjector.
     *
     * @author James X. Nelson (james@wetheinter.net, @james)
     */
    private static class XInjectSingleton extends Lazy<Injector>{

      public XInjectSingleton() {
        super(()->{
            //This method is only ever called once.
            if (X_Runtime.isGwt()){
              // gwt-dev mode
              // allow the generator to create a custom injector.
              // in gwt-dev, we can't super-source or use magic methods.
              try {
                // use reflection here so jres without gwt on classpath don't mind
                Class<?> cls = Class.forName("com.google.gwt.core.shared.GWT");
                Method m = cls.getDeclaredMethod("create", Class.class);
                return (Injector)m.invoke(null, Injector.class);
              }catch (Exception e) {
                throw X_Util.rethrow(e);
              }
            }
            // pure jre
            try {
              return (Injector)Class.forName("xapi.inject.impl.JreInjector").newInstance();
            } catch (Exception e) {
              throw X_Util.rethrow(e);
            }
        });
      }
    }

  /**
   * Our lazily-loaded injector singleton.
   */
  private static final XInjectSingleton singleton = new XInjectSingleton();
  public static <T, C extends Class<? extends T>> T instance(C cls) {
    return singleton.out1().create(cls);
  }

  public static <T, C extends Class<? extends T>> Lazy<T> singletonLazy(C cls) {
    return new LazySingletonInjector<>(cls);
  }


  public static void initialize(Object o) {
//    singleton.get().initialize(o);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void registerSingletonProvider(ClassLoader loader, String iface, final String name) {
    try {
      final Class cls = loader.loadClass(iface);
      singleton.out1().setSingletonFactory(cls, ()-> {
            try {
              return loader.loadClass(name).newInstance();
            } catch (Exception e) {
              X_Log.error("Unable to instantiate ", name, " using Class.newInstance", e);
              throw X_Util.rethrow(e);
            }
          }
        );
    } catch (NoClassDefFoundError e) {
      X_Log.error(e, "Cannot create interface class ",iface, "while registering singleton provider");
    } catch (ClassNotFoundException e) {
      X_Log.error(e, "Cannot create interface class ",iface, "while registering singleton provider");
    }

  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void registerInstanceProvider(ClassLoader loader, String iface, final String name) {
    try {
      final Class key = loader.loadClass(iface);
      final Class cls = loader.loadClass(name);
      singleton.out1().setInstanceFactory(key, ()->{
          try {
            return cls.newInstance();
          } catch (Exception e) {
            X_Log.error("Unable to instantiate ",name," using Class.newInstance", e);
            throw X_Util.rethrow(e);
          }
      });
    } catch (NoClassDefFoundError e) {
      X_Log.error(JavaInjector.class, e, "Cannot create instance class ", name, "while registering instance provider");
    } catch (ClassNotFoundException e) {
      X_Log.error(JavaInjector.class, "Cannot create instance class ", name, "while registering instance provider");
    }
  }

}
