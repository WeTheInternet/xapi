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
package xapi.inject.api;

import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.fu.Out1;
import xapi.inject.impl.JavaInjector;

/**
 * A simple injection interface used for java-based dependency injection.
 *
 * This interface is used to separate gwt-dev mode from pure java.
 *
 * Gwt-based injection uses magic methods and generated classes,
 * because each injected service requires a specific method as a provider.
 * In order for code splitting to work, we cannot reference all provider
 * classes in a single method.  For regular JRE implementations, however,
 * it is perfectly fine to access whatever class we want whenever we want.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public interface Injector {


  /**
   * Synchronously returns a singleton which implements or extends this method's generic type.
   *
   * For gwt-dev mode, this class is generated from the annotations
   * {@link SingletonDefault} and {@link SingletonOverride}
   *
   * For java mode, the {@link JavaInjector} uses runtime lookup of resources in
   * META-INF/singletons or META-INF/singletons to find bindings for your classes.
   *
   * If you enable runtime injection, the tool will do a once-per-runtime scan for annotations,
   * and will generate the bindings in memory.  For maximum performance in
   * production, you will want to use the preloading strategy to read in a
   * manifest of all injectable classes and prepare providers before they are
   * requested.
   *
   * A maven plugin will be provided to create this manifest for you.
   *
   * All libraries exporting functionality should include specific META-INF
   * bindings for defaults at least; the META-INF resources will only be checked
   * if the given type is not already bound.
   *
   * @param cls - The service interface or base service class to provide
   * @return - A singleton instance of the service class.
   */
  <T, C extends Class<? extends T>> T provide(C cls);

  <T, C extends Class<? extends T>> T create(C cls);

  <T> void setSingletonFactory(Class<T> cls, Out1<T> provider);

  <T> void setInstanceFactory(Class<T> cls, Out1<T> provider);

}
