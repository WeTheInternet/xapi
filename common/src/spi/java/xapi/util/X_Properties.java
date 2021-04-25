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

package xapi.util;

import xapi.annotation.compile.MagicMethod;
import xapi.constants.X_Namespace;
import xapi.fu.In1Out1;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.Out1.Out1Unsafe;
import xapi.prop.impl.PropertyServiceDefault;
import xapi.util.service.PropertyService;

/**
 * A collection of static strings and other properties used throughout the app.
 *
 * You are recommended to import this (and all classes beginning with "X_") as static imports.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *
 */
public final class X_Properties {

  private X_Properties() {}

	public static final Lazy<String> platform = Lazy.deferred1(() ->
	    getProperty(X_Namespace.PROPERTY_PLATFORM, "xapi.platform.JrePlatform")
	);

	private static final PropertyService service;
	static {
	  // We cannot use our normal injection service to provide the properties
	  // service, as our injection behavior is configurable by these properties.
	  final String propClass = System.getProperty(X_Namespace.PROPERTY_PROVIDER,
	    "xapi.prop.impl.PropertyServiceDefault");
	  PropertyService instance = null;
	  ClassLoader cl = null;
	  try {
	    cl = X_Properties.class.getClassLoader();
	  } catch (final Exception e) {}
	  try {
	    final Class<?> cls =
	      Class.forName(propClass, true, cl);
	    instance = (PropertyService)cls.newInstance();
	  }catch(final ClassCastException e) {
	    // No hope of getting a logger at this point...
	    System.err.println("Could not load "+propClass+";" +
	    		(X_Runtime.isDebug()?" it does not implement " +
	    		"PropertyService, or the classloader which loaded X_Properties is " +
	    		"unable to load this class.":""));
	  }catch(final Throwable e) {
	    e.printStackTrace();
	    System.err.println("Unknown error loading "+propClass+": "+e);
	  }
	  if (instance == null) {
      service = new PropertyServiceDefault();
    } else {
      service = instance;
    }
	}

	@MagicMethod(doNotVisit=true)
	public static String getProperty(final String property) {
	  return service.getProperty(property);
	}

  	@SuppressWarnings("DefaultAnnotationParam")
	@MagicMethod(doNotVisit=true)
	public static String getProperty(final String property, final String dflt) {
	  return service.getProperty(property, dflt);
	}

	public static String getProperty(final String property, final Out1<String> dflt) {
	  return service.getProperty(property, dflt);
	}

	public static <I1> String getProperty(final String property, final In1Out1<I1, String> dflt, I1 in1) {
	  return service.getProperty(property, dflt.supply(in1));
	}

	public static String getPropertyUnsafe(final String property, final Out1Unsafe<String> dflt) {
	  return service.getProperty(property, dflt);
	}

	public static <I1> String getPropertyUnsafe(final String property, final In1Out1Unsafe<I1, String> dflt, I1 in1) {
	  return service.getProperty(property, dflt.supply(in1));
	}
	// TODO: generate n-arity InNOut1<I1, I2, IN, String> getProperty (I1 i1, I2 i2, IN in) methods.

	public static void setProperty(final String property, final String value) {
	  service.setProperty(property, value);
	}

}
