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
package xapi.test.gwt.reflect;

import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;

import com.google.gwt.junit.client.GWTTestCase;

public class ServiceLoaderTest extends GWTTestCase {

  private static ServiceLoaderTest theOne;
  MagicClassTest testAsField;

  Class<MagicClassTest> testThroughMethod(Class<MagicClassTest> cls) {
    return cls;
  }

  public void testCompile() {
    theOne = this;
    // try {
    // Object o = Object.class.newInstance();
    // } catch (InstantiationException e) {
    // e.printStackTrace();
    // } catch (IllegalAccessException e) {
    // e.printStackTrace();
    // }
    // test = new SplitPointTest();
    try {
      Class<MagicClassTest> cls = X_Reflect.magicClass(MagicClassTest.class);
      X_Log.info("Test package: " + X_Reflect.getPackage(cls));
      System.err.println(cls);
      testAsField = X_Inject.instance(X_Reflect.magicClass(MagicClassTest.class));
      X_Log.info("Test direct method injection ", testAsField);
      MagicClassTest instance = X_Reflect.magicClass(MagicClassTest.class).newInstance();
      X_Log.info("Test Class.newInstance()", instance);

      assert testAsField != null : "Null instance returned";
    } catch (Exception e) {
      X_Log.error(e);
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getModuleName() {
    return "xapi.MagicClassTest";
  }

  public static void finish() {
    if (theOne != null) theOne.finishTest();
  }

}
