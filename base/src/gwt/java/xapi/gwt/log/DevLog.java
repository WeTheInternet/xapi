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
package xapi.gwt.log;

import xapi.annotation.inject.SingletonOverride;
import xapi.collect.fifo.Fifo;
import xapi.collect.fifo.SimpleFifo;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;
import xapi.platform.GwtDevPlatform;

import com.google.gwt.core.shared.GWT;

@GwtDevPlatform
@SingletonOverride(implFor=LogService.class,priority=Integer.MIN_VALUE/2)
public class DevLog extends JsLog{

  public DevLog() {
    consoleLog("dev mode logger created");
  }

  @Override
  public void doLog(LogLevel level, Fifo<Object> array)
  {
    if (!shouldLog(level))
      return;
    StringBuilder b = new StringBuilder();
    int i = 0;
    for (int s = array.size();i<s;i++){
      Object item = array.take();
      if (item == null){
        consoleLog("null");
        continue;
      }
      String clsName = item.getClass().getName();
      if (!clsName.startsWith("java.lang."))//don't print class names for primitives
        consoleLog(clsName);
      consoleLog(item);
      writeLog(level, b, item);
    }
    //print to gwt / System.out
    GWT.log(b.toString());
    if (i > 1)
      consoleLog("");
  }

  @Override
  public Fifo<Object> newFifo() {
    return new SimpleFifo<Object>();
  }

  @Override
  public Object unwrap(LogLevel level, Object m) {
    return String.valueOf(m);
  }
}
