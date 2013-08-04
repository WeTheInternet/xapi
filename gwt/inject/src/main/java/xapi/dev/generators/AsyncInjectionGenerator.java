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
import com.google.gwt.user.rebind.SourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;

public class AsyncInjectionGenerator extends AbstractInjectionGenerator{

  /**
   * @throws ClassNotFoundException
   */
  public static RebindResult execImpl(TreeLogger logger, GeneratorContext context, JClassType type) throws ClassNotFoundException, UnableToCompleteException{
    logger.log(Type.TRACE, "Async Inject For "+ type.getSimpleSourceName());
    PlatformSet platforms = CurrentGwtPlatform.getPlatforms(context);
    JClassType targetType = type;
    String simpleName = type.getSimpleSourceName();
    SingletonOverride winningOverride=null;
    JClassType winningType=null;
    for (JClassType subtype : type.getSubtypes()){
      if (winningType==null){
        SingletonDefault singletonDefault = subtype.getAnnotation(SingletonDefault.class);
        if (singletonDefault!=null && platforms.isAllowedType(subtype)){
          winningType = subtype;
          continue;
        }
      }
      SingletonOverride override = subtype.getAnnotation(SingletonOverride.class);
      if (override != null){
        logger.log(Type.DEBUG,"Got subtype "+subtype+" : "+" - prodMode: "+context.isProdMode());
        if (platforms.isAllowedType(subtype)) {
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
      //TODO sanity check here, or at least log a warning
    }
    String generatedName = "AsyncFor_"+simpleName;
    String packageName = type.getPackage().getName();
    ensureAsyncInjected(logger, packageName, type.getName(), winningType.getQualifiedBinaryName(), context);
//    ensureCallbackClass(logger, packageName,type.getQualifiedSourceName(),type.getSimpleSourceName(), winningType.getQualifiedSourceName(), context);
    packageName = packageName+".impl";
    PrintWriter printWriter = context.tryCreate(logger, packageName, generatedName);
    if (printWriter == null) {
      return new RebindResult(RebindMode.USE_EXISTING, packageName+"."+generatedName);
    }
    SourceFileComposerFactory composer =
        new SourceFileComposerFactory(packageName, generatedName);
    composer.setPrivacy("public final");
    composer.addImplementedInterface("ReceivesValue<ReceivesValue<" +simpleName+">>"
        );
    composer.addImport(type.getQualifiedSourceName());
    composer.addImport(ReceivesValue.class.getName());

    SourceWriter sw = composer.createSourceWriter(context, printWriter);
    sw.println();

    sw.indent();

    sw.println("@Override");
    sw.println("public final void set(ReceivesValue<" +simpleName+"> x) {");
    sw.indent();

//      sw.println(packageName+"."+generatedCallbackName(simpleName)+".deferred.push(x);");
//      sw.println(packageName+"."+generatedCallbackName(simpleName)+".go();");

    sw.outdent();
    sw.println("}");
    sw.outdent();

    sw.commit(logger);

    return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING, packageName+"."+generatedName);

  }

  protected static boolean isAsyncProvided(TreeLogger logger,String packageName, String simpleSourceName, GeneratorContext context) {
    try{
    JClassType type = context.getTypeOracle().findType(packageName+"."+InjectionUtils.generatedAsyncProviderName(simpleSourceName));
      return type != null;
    }catch(Exception e){
      return false;
    }
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
