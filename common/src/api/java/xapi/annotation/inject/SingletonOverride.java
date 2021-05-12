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

package xapi.annotation.inject;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import xapi.annotation.mirror.MirroredAnnotation;

/**
 * An annotation used to override a {@link SingletonDefault} used in dependency injection.
 *
 * <br/><br/>
 *
 * When the {@link xapi.inject.X_Inject} generator processes classes, it chooses a single injection target
 * based on priority.  SingletonDefault is always the lowest priority;
 * OverrideImpls are chosen based on highest {@link #priority()}.
 *
 * <br/><br/>
 *
 * The {@link #platform()} parameter currently only differentiates between gwt and pure java;
 * A service marked with {@link PlatformType#All} will be injected into all builds,
 * those marked with {@link PlatformType#GwtAll} will be in both dev mode and compiled gwt.
 * {@link PlatformType#GwtScript} is for compiled / super dev mode.
 * {@link PlatformType#GwtScript} is for dev mode only.
 * Use {@link PlatformType#Java} to include a class in META-INF/singletons, but exclude from Gwt.
 *
 * <br/><br/>
 *
 * The other platform types will be implemented as needed to support the full PlayN stack.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@Documented
@Target(value=ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MirroredAnnotation
public @interface SingletonOverride {
  /**
   * @return - The service interface or base class the inject instance implements / overrides.
   *
   * It is valid for a class (but not an interface) to select itself as the service to target.
   */
  Class<?> implFor();
  /**
   * @return - A descending-order prioritized list used to select the injected implementation.
   *
   * If you expect your override to be further overridden in submodules, pick a negative priority.
   */
  int priority() default Integer.MIN_VALUE + 2;
//  /**
//   * @return - A platform filter to exclude a given override from certain types of builds.
//   *
//   * Currently used only to differentiate between gwt and java builds.
//   *
//   * See {@link PlatformType} for details.
//   */
//  PlatformType[] platform() default PlatformType.All;

}
