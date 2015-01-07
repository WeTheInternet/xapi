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

import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.dev.util.CurrentGwtPlatform;
import xapi.dev.util.InjectionUtils;
import xapi.dev.util.PlatformSet;
import xapi.inject.impl.SingletonInitializer;
import xapi.log.X_Log;
import xapi.source.read.SourceUtil;
import xapi.util.api.ReceivesValue;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

public class SyncInjectionGenerator extends AbstractInjectionGenerator{


  /**
   * @throws ClassNotFoundException
   */
  public static RebindResult execImpl(TreeLogger logger, GeneratorContext context, JClassType type) throws ClassNotFoundException{
    PlatformSet allowed = CurrentGwtPlatform.getPlatforms(context);
    JClassType targetType = type;
    String simpleName = "SingletonFor_"+type.getSimpleSourceName();
    SingletonOverride winningOverride=null;
    JClassType winningType=null;
    boolean trace = logger.isLoggable(Type.TRACE);
    for (JClassType subtype : type.getSubtypes()){
      if (winningType==null){
        SingletonDefault singletonDefault = subtype.getAnnotation(SingletonDefault.class);
        if (singletonDefault!=null){
          if (allowed.isAllowedType(subtype))
            winningType = subtype;
          continue;
        }
      }
      SingletonOverride override = subtype.getAnnotation(SingletonOverride.class);
      if (override != null){
        if (trace)
          logger.log(Type.DEBUG,"Got subtype "+subtype+" : "+" - prodMode: "+context.isProdMode());

        if (allowed.isAllowedType(subtype)) {
          if (winningOverride!=null){
            if (winningOverride.priority()>override.priority())
              continue;
          }
          winningOverride = override;
          winningType = subtype;
        }
      }
    }
    if (winningType==null){
      winningType = targetType;//no matches, resort to instantiate the class sent.
      //TODO sanity check here
    }
    if (trace)
    X_Log.info("Singleton Injection Winner: "+winningType.getName());
    String packageName = type.getPackage().getName();
        ensureProviderClass(logger, packageName,type.getSimpleSourceName(),type.getQualifiedSourceName(), SourceUtil.toSourceName(winningType.getQualifiedSourceName()), context);
    packageName = packageName+".impl";
    PrintWriter printWriter = context.tryCreate(logger, packageName, simpleName);
    if (printWriter == null) {
      return new RebindResult(RebindMode.USE_EXISTING, packageName+"."+simpleName);
    }
    ClassSourceFileComposerFactory composer = new ClassSourceFileComposerFactory(packageName, simpleName);
    composer.setSuperclass(SingletonInitializer.class.getName()+
        "<" + type.getQualifiedSourceName()+">");
    composer.addImport(ReceivesValue.class.getName());

    SourceWriter sw = composer.createSourceWriter(context, printWriter);
    sw.println();
    //TODO: also check if compile is set to async = false


    //if async is already generated when this singleton access occurs,
    //but we haven't yet injected the callback which accesses the global singleton,
    //we'll have to route our get() through the existing async callback block
    //to prevents code splitting from falling apart. This will, at worst,
    //cause providers to return null until the service is actually initialized.
    if (
        context.isProdMode()
        && isAsyncProvided(logger,packageName, type.getSimpleSourceName(),context)
        && !isCallbackInjected(logger, packageName, type.getSimpleSourceName(), context)
    ){

      //this edge case happens when a service is accessed synchronously
      //inside of its own asynchronous callback
      //TODO use GWT's AsyncProxy class to queue up requests...
        logger.log(Type.WARN, "Generating interim singleton provider");
      sw.indent();
      sw.println("private static "+type.getQualifiedSourceName()+" value = null;");
      sw.println("@Override");
      sw.println("public final "+type.getQualifiedSourceName()+" initialValue(){");
      sw.indent();
      sw.println("if (value!=null)return value;");
      sw.println(packageName+"."+InjectionUtils.generatedAsyncProviderName(type.getSimpleSourceName()));
      sw.indent();

      sw.print(".request(new ReceivesValue<");
      sw.println(type.getQualifiedSourceName()+">(){");
        sw.indent();
        sw.println("@Override");
        sw.print("public void set(");
        sw.println(type.getQualifiedSourceName()+" x){");
        sw.indent();
        sw.println("value=x;");
        sw.outdent();
        sw.println("}");
        sw.outdent();

      sw.println("});");
      sw.outdent();
      sw.println("return value;");
    }else{
      //all non-prod or non-async providers can safely access the singleton directly
      sw.println("@Override");
      sw.println("public final "+type.getQualifiedSourceName()+" initialValue(){");
      sw.indent();
      //normal operation; just wrap the static final singleton provider.
      sw.print("return "+type.getPackage().getName()+"."+InjectionUtils.generatedProviderName(type.getSimpleSourceName()));
      sw.println(".theProvider.get();");
    }
    sw.outdent();
    sw.println("}");
    sw.println();

    sw.commit(logger);
    //TODO: implement caching once this code is finalized
    return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING, packageName+"."+simpleName);

  }

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    TypeOracle oracle = context.getTypeOracle();
    logger.log(Type.TRACE,"Generating singleton for "+typeName);

    try {
      return execImpl(logger, context, oracle.getType(SourceUtil.toSourceName(typeName)));
       } catch (NotFoundException e) {
      logger.log(Type.ERROR, "Could not find class for "+typeName,e);
    }
 catch (ClassNotFoundException e) {
   logger.log(Type.ERROR, "Could not find class for "+typeName,e);
    }
    throw new UnableToCompleteException();
  }

}
