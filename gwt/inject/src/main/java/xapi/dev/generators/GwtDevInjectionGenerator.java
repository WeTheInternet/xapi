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
package xapi.dev.generators;

import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashMap;

import javax.inject.Provider;

import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.util.GwtInjectionMap;
import xapi.dev.util.InjectionUtils;
import xapi.inject.api.Injector;
import xapi.util.X_Runtime;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;


/**
 * This is the generator invoked only for gwt-dev mode.
 *
 * All functionality carried out here is done with magic methods in gwt-prod mode.
 *
 * This method will create an instance of {@link Injector} based on class injection annotations.
 * {@link SingletonDefault} is used to define a default static singleton service.
 * {@link SingletonOverride} is used to inject overrides on top of static singleton services.
 * {@link InstanceOverride} is used to inject prioritized instances using GWT.create().
 *
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
public class GwtDevInjectionGenerator extends AbstractInjectionGenerator {

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    String simpleName = "JsInjector";
    String generatedClassName = packageName + "." + simpleName;
    logger.log(Type.DEBUG, "Generating X_Inject platform");
    // only ever do this extremely expensive operation (iterating all Types) once
    GwtInjectionMap gwtInjectionMap = getInjectionMap(logger, context);

    PrintWriter printWriter = context.tryCreate(logger, packageName, simpleName);
    if (printWriter == null) {
      return new RebindResult(RebindMode.USE_EXISTING, generatedClassName);
    }
    SourceBuilder<GwtInjectionMap> sb = new SourceBuilder<GwtInjectionMap>(
      "public class "+simpleName + " implements "+Injector.class.getName()
      )
      .setPayload(gwtInjectionMap)
      .setPackage(packageName)
    ;
    sb.getImports().addImports(
        "xapi.util.api.ReceivesValue"
        ,HashMap.class.getName()
        ,Provider.class.getName()
        ,GWT.class.getName()
      )
      ;
    //Let's disable this for now; we can write META-INF manually outside of gwt compile
//    for (Entry<Class<?>, JClassType> entry : gwtInjectionMap.getJavaSingletons()) {
//      Class<?> cls = entry.getKey();
//      JClassType impl = entry.getValue();
//      // write meta info for each class during gwt compile;
//      // necessary for gwt users who forego regular dev mode.
//      tryWriteMetaInf(logger, cls, impl, context);
//    }
    Set<Entry<Class<?>, JClassType>> gwtSingletons = gwtInjectionMap.getGwtSingletons();
    for (Entry<Class<?>, JClassType> entry : gwtSingletons) {
      Class<?> cls = entry.getKey();
      JClassType impl = entry.getValue();
      // make sure our provider class is available
      ensureProviderClass(logger, cls.getPackage().getName(), cls.getSimpleName(), cls
          .getCanonicalName(), InjectionUtils.toSourceName(impl.getQualifiedSourceName()), context);
      ensureAsyncInjected(logger, cls.getPackage().getName(), cls.getName(), impl.getQualifiedSourceName(), context);
//      ensureCallbackClass(logger, cls.getPackage().getName(),cls.getCanonicalName(), cls.getSimpleName(), impl
//          .getQualifiedSourceName(), context);
    }
    ClassBuffer cb = sb.getClassBuffer();

    cb.createMethod("private static final void throwNotFound(boolean type, Class<?> cls)")

      .println("String typeName =  type?\"Singleton\":\"Instance\";")
      //TODO use a debug level flag to use much smaller strings here...
      .println("throw new RuntimeException(\"JsInjector did not have a registered \"+" +
      "typeName+\" for \"+cls.getName()+\".\\n\"+")
      .println("\"Please ensure you have at least one class which implements or extends \"" +
      "+cls.getName()+\" and it is marked with either @\"+type+\"Default or @" +
      "\"+type+\"Override.\"+")
      .println("\"Also, be sure to check for invalidated units " +
      "that may have removed your injection target from compile.\");");
    ;

    // Since gwt dev is the only user of this service,
    // we try to avoid jsni, and instead just use a hash map to provider instance
    cb
      .println("private final HashMap<Class<?>, Provider<?>> singletons;")
      .println("private final HashMap<Class<?>, Provider<?>> instances;")
      .println()
      // We setup all known injection types in the constructor,
      // which will load all the classes eagerly.
      // The runtime hit is better than anomalies from using jsni and lazy loading
      .println("public "+simpleName+"() {")
      .indent()
        .println("singletons = new HashMap<Class<?>, Provider<?>>();")
        .println("instances = new HashMap<Class<?>, Provider<?>>();")
    ;
    for (Entry<Class<?>, JClassType> entry : gwtSingletons) {
      Class<?> cls = entry.getKey();
      cb
        .println("setSingletonFactory("+cls.getCanonicalName()+".class, ")
        .indentln(cls.getPackage().getName()+"."+InjectionUtils.generatedProviderName(cls.getSimpleName())+".theProvider);")
      ;
    }
    for (Entry<Class<?>, JClassType> entry : gwtInjectionMap.getGwtInstances()) {
      cb
        .println("setInstanceFactory("+entry.getKey().getCanonicalName()+".class, ")
        .indent()
          .println("new Provider() {")
          .indent()
            .println("public Object get() {")
            .indentln("return GWT.create("+entry.getValue().getQualifiedSourceName()+".class);")
            .println("}")
          .outdent()
          .println("}")
        .outdent()
        .println(");")
      ;
    }
    // End our constructor
    cb
      .outdent()
      .println("}")
    ;

    // Print setters for factories, to allow runtime injection support.
    cb
    .createMethod("public <T> void setSingletonFactory(Class<T> cls, Provider<T> provider)")
    .addAnnotation("Override")
    .println("singletons.put(cls, provider);")
    ;
    cb
    .createMethod("public <T> void setInstanceFactory(Class<T> cls, Provider<T> provider)")
    .addAnnotation("Override")
    .println("instances.put(cls, provider);")
    ;

    // Print the factory provider methods, which throw exception instead of return null
    cb
      .createMethod("public <T> Provider<T> getSingletonFactory(Class<T> cls)")
//      .addAnnotation("Override")
      .addAnnotation("SuppressWarnings({\"rawtypes\", \"unchecked\"})")
      .println("Provider p = singletons.get(cls);")
      .println("if (p == null) throwNotFound(true, cls);")
      .println("return (Provider<T>)p;")
    ;
    cb
      .createMethod("public <T> Provider<T> getInstanceFactory(Class<T> cls)")
//      .addAnnotation("Override")
      .addAnnotation("SuppressWarnings({\"rawtypes\", \"unchecked\"})")
      .println("Provider p = instances.get(cls);")
      .println("if (p == null) throwNotFound(false, cls);")
      .println("return (Provider<T>)p;")
    ;


    cb
      .createMethod("public final <T> T provide(Class<? extends T> cls)")
      .addAnnotation("@Override")
      .println("return getSingletonFactory(cls).get();")
    ;
    cb
      .createMethod("public final <T> T create(Class<? extends T> cls)")
      .addAnnotation("@Override")
      .println("return getInstanceFactory(cls).get();")
    ;

    if (X_Runtime.isDebug()) {
      logger.log(Type.INFO, "Dumping javascript injector (trace level logging)");
      if (logger.isLoggable(Type.TRACE))
        logger.log(Type.TRACE, sb.toString());
    }

    printWriter.append(sb.toString());
    context.commit(logger, printWriter);

    return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING, generatedClassName);
  }

}
