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
 * Redistribution in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution, unless otherwise
 * agreed to in a written document signed by a director of We The Internet Ltd.
 *
 * Neither the name of We The Internet nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * Public displays of software using this code may choose to credit the contributors,
 * but are not required to give attribution.
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
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *
 */
package xapi.inject;

import javax.inject.Provider;

import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.annotation.reflect.KeepMethod;
import xapi.except.NotConfiguredCorrectly;
import xapi.inject.api.Injector;
import xapi.inject.impl.JavaInjector;
import xapi.util.api.ReceivesValue;
import static xapi.util.X_Runtime.*;

/**
 * A static accessor class for in an instance of {@link Injector}.
 *
 * <br/><br/>
 *
 * Classes marked with {@link SingletonDefault} or {@link SingletonOverride} are usable by X_Inject.
 * Once a class has been marked as a service provider for a given interface or class,
 * it can be used in cross-platform code as follows:
 *
 * <br/><br/>
 *
 * static interface MyService{}
 *
 * <br/><br/>
 *
 * <pre>@SingletonDefault(implFor=MyService.class)</pre>
 * static class MyServiceImpl implements MyService{}
 *
 * <br/><br/>
 *
 * MyService service = X_Inject.singleton(MyService.class);//returns MyServiceImpl
 *
 * <br/><br/>
 *
 * All actual injection occurs in implementations of {@link Injector}.
 *
 * <br/><br/>
 *
 * The pure-java solution uses {@link JavaInjector}, which is based on the
 * jre6 {@link ServiceLoader}, with some tweaks to match the gwt injector behavior.
 * Files with the name equal to the interface or class being injected are written
 * to META-INF/singletons for services to be instantiated only once per runtime.
 * META-INF/instances is for objects that should be new copies for every request.
 *
 * A third form as META-INF/threadlocal may be provided for thread-local or
 * process-local services, to help separated code share state without explicit
 * reference passing.  This option will be considered if the concurrency model
 * is able to offer lifecycle events to ensure the gc of process-local singletons.
 *
 * <br/><br/>
 *
 * The gwt solution uses a generated extension of {@link JsInjector} using {@link AsyncProxy}.
 * See {@link xapi.dev.generators.GwtDevGenerator} and
 * its subclass {@link xapi.dev.generators.AbstractInjectionGenerator}
 * in the xapi-gwt-inject module.
 *
 * <br/><br/>
 *
 * Unlike ServiceLoader, which presents an iterable of available services,
 * X_Inject currently only returns the highest priority (top of the list) service,
 * and it only returns lazily-loaded singletons (which work with code splitting).
 *
 * <br/><br/>
 *
 * Note, all static service accessors in XApi are prefixed with X_,
 * to make exposed services easier to discover in autocomplete.
 *
 * <br/><br/>
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@KeepMethod
public class X_Inject{

  private X_Inject() {}
  
  @KeepMethod
  public static <T> T instance(Class<? super T> cls){
    if (isJava()) {
      return JavaInjector.instance(cls);
    } else
      throw notConfiguredCorrectly();
  }
 /**
   * Returns an injected singleton service implementation object.
   *
   * WARNING: When running in compiled gwt mode with code splitting,
   * if you use the asynchronous provider method
   * {@link #singletonAsync(Class, ReceivesValue)}, then the synchronous providers,
   * {@link #singleton(Class)} and {@link #singletonLazy(Class)} will return null until
   * the code split containing the service is loaded.
   *
   * TODO: use AsyncProxy from
   *
   * If you have ANY doubt about whether a service is already loaded or not,
   * please prefer the async provider method {@link #singletonAsync(Class, ReceivesValue)
   *
   * <br/><br/>
   *
   * @param cls - The service interface / base class to inject.
   * @return - An instance of the requested class, based on DI annotations.
   *
   * <br/><br/>
   *
   * There must be at least one class on your classpath which is
   * annotated with {@link SingletonDefault} or {@link SingletonOverride},
   * and which implements or overrides the service being provided.
   *
   * <br/><br/>
   *
   * You are recommended to use only interfaces in service definitions,
   * but you are free to use classes if you wish.
   *
   * <br/><br/>
   *
   * It is valid to annotate a class as it's own default implementation.
   *
   */
  @KeepMethod
  public static <T> T singleton(Class<? super T> cls){
    if (isJava()) {
      Provider<T> provider = JavaInjector.singletonLazy(cls);
      return provider.get();
    } else
      throw notConfiguredCorrectly();
  }
  /**
   *
   * Asynchronously loads an injected singleton service implementation object.
   * This method uses gwt code splitting to put each service into it's own island loader.
   * It will create a split for every service, but with the closure compiler and CodeSplitter2,
   * the compiler will do a very good job of modularizing and optimizing split points.
   *
   * <br/><br/>
   *
   * WARNING: If you use async loading for a service provider in one place,
   * and then use synchronous methods {@link #singleton(Class)} or {@link #singletonLazy(Class)},
   * the synchronous methods will return null in gwt production mode until after the split point
   * is loaded.  If you have any doubts about whether a given service is already loaded,
   * always prefer this asynchronous provider method instead of the sync ones.
   *
   * <br/><br/>
   *
   * In compiled gwt, this is treated as a magic method which generators provider classes.
   * In gwt dev mode, it defers to a single class which provides all injected object.
   * In pure java mode, it relies on java.util.ServiceLoader.
   * Running either gwt dev or a full gwt compile will generate META-INF/singletons for you.
   *
   * <br/><br/>
   *
   * @param cls - The service interface / base class to inject.
   * @param callback - The {@link ReceivesValue} to notify when the object is loaded.
   *
   * <br/><br/>
   *
   * There must be at least one class on your classpath which is
   * annotated with {@link SingletonDefault} or {@link SingletonOverride},
   * and which implements or overrides the service being provided.
   *
   * <br/><br/>
   *
   * You are recommended to use only interfaces in service definitions,
   * but you are free to use classes if you wish.
   *
   * <br/><br/>
   *
   * It is valid to annotate a class as it's own default implementation.
   *
   */
  @KeepMethod
  public static <T> void singletonAsync(Class<? super T> cls, ReceivesValue<T> callback){
    if (isJava()) {
      Provider<T> provider = JavaInjector.singletonLazy(cls);
      callback.set(provider.get());
    } else
      throw notConfiguredCorrectly();
  }

  @KeepMethod
  public static <T> void singletonAsync(Class<? super T> cls,Class<? extends ReceivesValue<T>> callback){
    if (isJava()){
      // We expose a Class<? super T> api for all injection methods,
      // but in the case of async callback injection, we have to allow
      // extensions of ReceivesValue, so concrete classes will map correctly.
      // Thus, we must coerce the bounds of the callback class (mostly for gwt)
      @SuppressWarnings("unchecked")
      Class<? super ReceivesValue<T>> receiverCls = Class.class.cast(callback);
      // we normally enforce "give supertype, get subtype", but in the case of callbacks,
      // it's "give supertype<T> and a concrete subclass of ReceivesValue<T>,
      // and we'll inject the callback and the singleton.
      ReceivesValue<T> receiver = singleton(receiverCls);
      singletonAsync(cls, receiver);
    }
    else
      throw  notConfiguredCorrectly();
  }
  /**
   * This is a magic method, like gwt.create.
   * It is replaced in UnifyAst with references to generated classes.
   *
   * In pure java implementations,
   * it defers to either a generated provider class for gwt-dev,
   * and to ServiceLoader.load() for pure java implementations.
   *
   * @param cls - The interface or base class to be injected
   * @return - A provider which will return the loaded injected class.
   *
   * WARNING: In gwt production mode,
   * if you use code splitting to move a provider into it's own async splitpoint,
   * any other split points which use synchronous access will return null until loaded.
   *
   * If you have any doubts about the timing and load sequence,
   * always prefer the {@link #singletonAsync(Class, ReceivesValue)} method.
   *
   */
  @KeepMethod
  public static <T> Provider<T> singletonLazy(Class<? super T> cls) {
    if (isJava())
      return JavaInjector.singletonLazy(cls);
    else
      throw  notConfiguredCorrectly();
  }

  public static void initialize(Object o) {
//      JavaInjector.initialize(o);
  }
  private static NotConfiguredCorrectly  notConfiguredCorrectly() {
    return new NotConfiguredCorrectly("xapi-gwt(-api) must be above gwt-dev on classpath");
  }

}
