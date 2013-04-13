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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;

import xapi.annotation.inject.InstanceDefault;
import xapi.annotation.inject.InstanceOverride;
import xapi.dev.util.CurrentGwtPlatform;
import xapi.dev.util.InjectionUtils;
import xapi.platform.Platform;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.RebindMode;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

public class InstanceInjectionGenerator extends AbstractInjectionGenerator {

  /**
   * @throws ClassNotFoundException
   */
  public static RebindResult execImpl(TreeLogger logger, GeneratorContext context, JClassType type)
    throws ClassNotFoundException {
    JClassType targetType = type;
    InstanceOverride winningOverride = null;
    JClassType winningType = null;

    // Step one: determine what platform we are targeting.
    Set<Class<? extends Annotation>> allowedPlatforms = getPlatforms(context);
    boolean trace = logger.isLoggable(Type.TRACE);
    if (trace) {
      logger.log(Type.TRACE, "Allowed platforms:  "+allowedPlatforms);
      logger.log(Type.TRACE, "All Subtypes: " + Arrays.asList(type.getSubtypes()));
    }

    for (JClassType subtype : type.getSubtypes()) {
      if (trace)
        logger.log(Type.TRACE, "Type "+subtype.getJNISignature());
      InstanceDefault def = subtype.getAnnotation(InstanceDefault.class);
      if (def != null) {
        if (winningType != null)
          continue; // a default cannot possibly override anything.
        // Make sure this type is not excluded by being part of a disallowed platform
        boolean useType = true;
        for (Annotation anno : def.annotationType().getAnnotations()) {
          if (anno.annotationType().getAnnotation(Platform.class) != null) {
            if (allowedPlatforms.contains(anno.annotationType())) {
              winningType = subtype;
              continue;
            }
            useType = false;
          }
        }
        // If we made it through without continue, or setting useType to false,
        // then there is no platform specified, and this is a global default type.
        if (useType)
          winningType = subtype;
      }
      InstanceOverride override = subtype.getAnnotation(InstanceOverride.class);
      if (override != null) {
        // Prefer GwtPlatform or GwtDevPlatform,
        // and blacklist any types that are of a foreign platform type (non-gwt).
        boolean hasGwt = false, hasOther = false;
        for (Annotation annotation : subtype.getAnnotations()){
          // check each annotation to see if it is qualified with @Platform.
          Platform platform = annotation.annotationType().getAnnotation(Platform.class);
          if (platform != null) {
            // When platform is non-null, we only want to use it if it's in our allowed list.
            if (allowedPlatforms.contains(annotation.annotationType())) {
              hasGwt = true;
              break;
            }else {
              hasOther = true;
            }
          }
        }
        // if hasOther is false, or hasGwt is true, we want to use this type.
        if (hasOther && !hasGwt) {
          continue;
        }

        if (trace)
          logger.log(Type.DEBUG, "Test subtype match " + subtype + " - prodMode: " + context.isProdMode());
        if (winningOverride != null) {
          if (winningOverride.priority() > override.priority()) continue;
        }
        winningOverride = override;
        winningType = subtype;
      }
    }
    if (winningType == null) {
      winningType = targetType;// no matches, resort to instantiate the class sent.
      if (trace)
        logger.log(Type.TRACE, "No match made; resorting to GWT.create() on " + winningType+ " : ");
    }
    String packageName = type.getPackage().getName();
    ensureProviderClass(logger, packageName, type.getSimpleSourceName(), type.getQualifiedSourceName(),
      InjectionUtils.toSourceName(winningType.getQualifiedSourceName()), context);
    logger.log(Type.INFO,
      "Instance injection: " + type.getQualifiedSourceName() + " -> " + winningType.getQualifiedSourceName());
    return new RebindResult(RebindMode.USE_ALL_NEW_WITH_NO_CACHING, winningType.getQualifiedSourceName());

  }

  private static Set<Class<? extends Annotation>> getPlatforms(GeneratorContext ctx) {
    return CurrentGwtPlatform.getPlatforms(ctx);
  }

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext context, String typeName)
    throws UnableToCompleteException {
    TypeOracle oracle = context.getTypeOracle();
    logger = logger.branch(Type.INFO, "Generating instance injection for " + typeName);

    try {
      return execImpl(logger, context, oracle.getType(InjectionUtils.toSourceName(typeName)));
    } catch (NotFoundException e) {
      logger.log(Type.ERROR, "Could not find class for " + typeName, e);
    } catch (ClassNotFoundException e) {
      logger.log(Type.ERROR, "Could not find class for " + typeName, e);
    }
    throw new UnableToCompleteException();
  }

}
