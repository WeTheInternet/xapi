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

import xapi.gwt.log.DevLog;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;
import xapi.test.Assert;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.reflect.shared.GwtReflect;

public class ReflectionTest extends GWTTestCase {

  Class<MagicClassTest> testThroughMethod(Class<MagicClassTest> cls) {
    return cls;
  }

  public void testCompile() {
    LogService log = new DevLog();
    log.setLogLevel(LogLevel.INFO);
    try {
      Class<MagicClassTest> cls = GwtReflect.magicClass(MagicClassTest.class);
      GWT.log("Test Class.newInstance() "+cls.getName());
      MagicClassTest instance = cls.newInstance();
      Assert.assertNotNull("Null instance returned",instance);
      assert instance instanceof MagicClassTest;
      log.log(LogLevel.INFO, instance);
    } catch (Exception e) {
      log.log(LogLevel.ERROR, e);
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getModuleName() {
    return "xapi.ReflectionTest";
  }

}
