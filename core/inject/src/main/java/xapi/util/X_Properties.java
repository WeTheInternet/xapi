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

import xapi.inject.impl.SingletonProvider;
import xapi.platform.JrePlatform;
import xapi.util.impl.PropertyServiceDefault;
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

	public static final SingletonProvider<String> platform = new SingletonProvider<String>(){
	  @Override
	  protected String initialValue() {
	    return getProperty(X_Namespace.PROPERTY_PLATFORM, JrePlatform.class.getName());
	  };
	};

	private static final PropertyService service;
	static {
	  // We cannot use our normal injection service to provide the properties
	  // service, as our injection behavior is configurable by these properties.
	  String propClass = System.getProperty(X_Namespace.PROPERTY_PROVIDER,
	    PropertyServiceDefault.class.getName());
	  PropertyService instance = null;
	  ClassLoader cl = null;
	  try {
	    cl = X_Properties.class.getClassLoader();
	  } catch (Exception e) {}
	  try {
	    Class<?> cls =
	      Class.forName(propClass, true, cl);
	    instance = (PropertyService)cls.newInstance();
	  }catch(ClassCastException e) {
	    // No hope of getting a logger at this point...
	    System.err.println("Could not load "+propClass+";" +
	    		(X_Runtime.isDebug()?" it does not implement " +
	    		"PropertyService, or the classloader which loaded X_Properties is " +
	    		"unable to load this class.":""));
	  }catch(Throwable e) {
	    e.printStackTrace();
	    System.err.println("Unknown error loading "+propClass+": "+e);
	  }
	  if (instance == null)
	    service = new PropertyServiceDefault();
	  else
	    service = instance;
	}

	public static String getProperty(String property) {
	  return service.getProperty(property);
	}

	public static String getProperty(String property, String dflt) {
	  return service.getProperty(property, dflt);
	}

	public static void setProperty(String property, String value) {
	  service.setProperty(property, value);
	}

}
