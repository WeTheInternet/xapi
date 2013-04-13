/*
 * Copyright 2012, We The Internet Ltd.
 *
 * All rights reserved.
 *
 * Distributed under a modified BSD License as follow:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution, unless otherwise agreed to in a written document signed by a director of We The
 * Internet Ltd.
 *
 * Neither the name of We The Internet nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package xapi.dev.generators;

import xapi.annotation.inject.SingletonDefault;
import xapi.annotation.inject.SingletonOverride;
import xapi.dev.util.CurrentGwtPlatform;
import xapi.dev.util.InjectionCallbackArtifact;
import xapi.dev.util.InjectionUtils;
import xapi.dev.util.PlatformSet;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;

public class AsyncProxyGenerator {

  /**
   * @throws ClassNotFoundException
   */
  public static InjectionCallbackArtifact setupAsyncCallback(TreeLogger logger,
      GeneratorContext context, JClassType singletonType, JDeclaredType callbackType)
      throws ClassNotFoundException, UnableToCompleteException {
    PlatformSet platforms = CurrentGwtPlatform.getPlatforms(context);

    logger =
        logger.branch(Type.TRACE, "Binding callback " + callbackType.getName() + " to "
            + singletonType.getQualifiedSourceName());

    JClassType winningCallback = null;
    SingletonOverride winningCallbackOverride = null;
    logger.log(Type.WARN, "Checking " + callbackType.getName() + " : "
        + callbackType.getJavahSignatureName());
    JClassType providerType =
        context.getTypeOracle().findType(InjectionUtils.toSourceName(callbackType.getName()));
    for (JClassType subtype : providerType.getSubtypes()) {
      if (winningCallback == null) {
        SingletonDefault singletonDefault = subtype.getAnnotation(SingletonDefault.class);
        // make sure this default is not for a different platform.
        if (singletonDefault != null && platforms.isAllowedType(subtype)) {
          winningCallback = subtype;
          continue;
        }
      }
      SingletonOverride override = subtype.getAnnotation(SingletonOverride.class);
      if (override != null) {
        logger.log(Type.DEBUG, "Got subtype " + subtype + " : " + " - prodMode: " + context.isProdMode());
        if (platforms.isAllowedType(subtype)) {
          if (winningCallbackOverride != null) {
            if (winningCallbackOverride.priority() > override.priority())
              continue;
          }
          winningCallbackOverride = override;
          winningCallback = subtype;
        }
      }
    }
    if (winningCallback == null) {
      logger.log(Type.WARN, "No callback type override found, using "
          + providerType.getQualifiedSourceName() + " : " + providerType.getJNISignature());
      winningCallback = providerType;// no matches, resort to instantiate the class sent.
      // TODO sanity check here
    }

    JClassType targetType = singletonType;
    SingletonOverride winningOverride = null;
    JClassType winningType = null;
    for (JClassType subtype : singletonType.getSubtypes()) {
      if (winningType == null) {
        SingletonDefault singletonDefault = subtype.getAnnotation(SingletonDefault.class);
        if (singletonDefault != null && platforms.isAllowedType(subtype)) {
          winningType = subtype;
          continue;
        }
      }
      SingletonOverride override = subtype.getAnnotation(SingletonOverride.class);
      if (override != null) {
        logger.log(Type.DEBUG, "Got subtype " + subtype + " - prodMode: " + context.isProdMode());
        if (platforms.isAllowedType(subtype)) {
          if (winningOverride != null) {
            if (winningOverride.priority() > override.priority())
              continue;
          }
          winningOverride = override;
          winningType = subtype;
        }
      }
    }
    if (winningType == null) {
      winningType = targetType;// no matches, resort to instantiate the class sent.
      // TODO sanity check here
    }

    InjectionCallbackArtifact artifact =
        AbstractInjectionGenerator.ensureAsyncInjected(logger, singletonType.getPackage().getName(), singletonType.getName(),
            winningType.getQualifiedSourceName(), context);
    // TODO: switch between service-centric splits, or callback-centric splits.
    artifact.addCallback(winningCallback.getQualifiedSourceName());
    context.commitArtifact(logger, artifact);

    return artifact;
  }

}
