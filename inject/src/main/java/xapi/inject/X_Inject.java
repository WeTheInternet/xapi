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

import xapi.annotation.compile.MagicMethod;
import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.annotation.reflect.KeepMethod;
import xapi.except.NotConfiguredCorrectly;
import xapi.fu.In1;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.inject.api.Injector;
import xapi.inject.impl.JavaInjector;
import xapi.util.api.ReceivesValue;

import java.util.ServiceLoader;

import static xapi.util.X_Runtime.isJava;

///
/// A static accessor class for in an instance of {@link Injector}.
///
///
/// Classes marked with {@link SingletonDefault} or {@link SingletonOverride} are usable by X_Inject.
/// Once a class has been marked as a service provider for a given interface or class,
/// it can be used in cross-platform code as follows:
///
///
/// ```
/// static interface MyService{}
///
/// @SingletonDefault(implFor=MyService.class)
/// static class MyServiceImpl implements MyService{}
///
///
/// MyService service = X_Inject.singleton(MyService.class);//returns MyServiceImpl
/// ```
///
/// All actual injection occurs in implementations of {@link Injector}.
///
///
/// The pure-java solution uses {@link JavaInjector}, which is based on the
/// jre6 {@link ServiceLoader}, with some tweaks to match the gwt injector behavior.
/// Files with the name equal to the interface or class being injected are written
/// to META-INF/singletons for services to be instantiated only once per runtime.
/// META-INF/instances is for objects that should be new copies for every request.
///
/// A third form as META-INF/threadlocal may be provided for thread-local or
/// process-local services, to help separated code share state without explicit
/// reference passing.  This option will be considered if the concurrency model
/// is able to offer lifecycle events to ensure the gc of process-local singletons.
///
///
/// Unlike ServiceLoader, which presents an iterable of available services,
/// X_Inject currently only returns the highest priority (top of the list) service,
/// and it only returns lazily-loaded singletons (which work with code splitting).
///
///
/// Note, all static service accessors in XApi are prefixed with X_,
/// to make exposed services easier to discover in autocomplete.
///
///
/// @author James X. Nelson (james@wetheinter.net, @james)
///
@KeepMethod
public class X_Inject{

  public static <T, C extends Class<? extends T>> void initialize(C cls, final T o) {
    // Filled in by javac plugin.  See xapi-dev-javac
  }

  @KeepMethod
  @MagicMethod(doNotVisit=true)
  public static <T, Generic extends T> T instance(final Class<Generic> cls){
    if (isJava()) {
      return xapi.inject.impl.JavaInjector.instance(cls);
    } else {
      throw notConfiguredCorrectly();
    }
  }
  ///
  /// Returns an injected singleton service implementation object.
  ///
  /// WARNING: When running in compiled gwt mode with code splitting,
  /// if you use the asynchronous provider method
  /// {@link #singletonAsync(Class, In1)}, then the synchronous providers,
  /// {@link #singleton(Class)} and {@link #singletonLazy(Class)} will return null until
  /// the code split containing the service is loaded.
  ///
  /// TODO: use AsyncProxy from
  ///
  /// If you have ANY doubt about whether a service is already loaded or not,
  /// please prefer the async provider method {@link #singletonAsync(Class, In1)
  ///
  /// @param cls - The service interface / base class to inject.
  /// @return - An instance of the requested class, based on DI annotations.
  ///
  /// There must be at least one class on your classpath which is
  /// annotated with {@link SingletonDefault} or {@link SingletonOverride},
  /// and which implements or overrides the service being provided.
  ///
  /// You are recommended to use only interfaces in service definitions,
  /// but you are free to use classes if you wish.
  ///
  /// It is valid to annotate a class as it's own default implementation.
  ///
  ///
  @KeepMethod
  @MagicMethod(doNotVisit=true)
  public static <T, C extends Class<? extends T>> T singleton(C cls){
    if (isJava()) {
      final Out1<T> provider = xapi.inject.impl.JavaInjector.singletonLazy(cls);
      return provider.out1();
    } else {
      throw notConfiguredCorrectly();
    }
  }
  @KeepMethod
  @MagicMethod(doNotVisit=true)
  public static <T, C extends T> void singletonAsync(final Class<? extends C> cls,final Class<? extends In1<T>> callback){
    if (isJava()){
      final In1<T> receiver = singleton(callback);
      singletonAsync(cls, receiver);
    } else {
      throw  notConfiguredCorrectly();
    }
  }

  ///
  ///
  /// Asynchronously loads an injected singleton service implementation object.
  /// This method uses gwt code splitting to put each service into it's own island loader.
  /// It will create a split for every service, but with the closure compiler and CodeSplitter2,
  /// the compiler will do a very good job of modularizing and optimizing split points.
  ///
  /// WARNING: If you use async loading for a service provider in one place,
  /// and then use synchronous methods {@link #singleton(Class)} or {@link #singletonLazy(Class)},
  /// the synchronous methods will return null in gwt production mode until after the split point
  /// is loaded.  If you have any doubts about whether a given service is already loaded,
  /// always prefer this asynchronous provider method instead of the sync ones.
  ///
  /// In compiled gwt, this is treated as a magic method which generators provider classes.
  /// In gwt dev mode, it defers to a single class which provides all injected object.
  /// In pure java mode, it relies on java.util.ServiceLoader.
  /// Running either gwt dev or a full gwt compile will generate META-INF/singletons for you.
  ///
  /// @param cls - The service interface / base class to inject.
  /// @param callback - The {@link ReceivesValue} to notify when the object is loaded.
  ///
  /// There must be at least one class on your classpath which is
  /// annotated with {@link SingletonDefault} or {@link SingletonOverride},
  /// and which implements or overrides the service being provided.
  ///
  /// You are recommended to use only interfaces in service definitions,
  /// but you are free to use classes if you wish.
  ///
  /// It is valid to annotate a class as it's own default implementation.
  ///
  ///
  @KeepMethod
  @MagicMethod(doNotVisit=true)
  public static <T, C extends Class<? extends T>> void singletonAsync(final C cls, final In1<T> callback){
    if (isJava()) {
      final Lazy<T> provider = xapi.inject.impl.JavaInjector.singletonLazy(cls);
      callback.in(provider.out1());
    } else {
      throw notConfiguredCorrectly();
    }
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
   * always prefer the {@link #singletonAsync(Class, In1)} method.
   *
   */
  @KeepMethod
  @MagicMethod(doNotVisit=true)
  public static <Type, Generic extends Type> Lazy<Type> singletonLazy(final Class<Generic> cls) {
    if (isJava()) {
      return xapi.inject.impl.JavaInjector.singletonLazy(cls);
    } else {
      throw  notConfiguredCorrectly();
    }
  }

  private static NotConfiguredCorrectly  notConfiguredCorrectly() {
    return new NotConfiguredCorrectly("xapi-gwt(-api) must be above gwt-dev on classpath");
  }
  private X_Inject() {}

  /** TODO: use well-known scopes, like UserAgent or Language to create "scope worlds".

  public static enum InjectionScopes {
    // Some handy default scopes to share.
    // You are free to use any scope you want;
    // the Global scope will be checked by default if there is no match
    // in the requested scope.
    UserAgent, Language, Authenticated, Request, User, Global;
  }
  // public static <T> T scopedInject(Class scope, Class<T> type);
  // public static <T> void scopedInjectAsync(Class scope, Class<T> type, ReceivesValue<T> callback);

   */
}
