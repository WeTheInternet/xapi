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
import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.gwt.collect.JsFifo;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;
import xapi.log.impl.AbstractLog;
import xapi.platform.GwtPlatform;
import xapi.util.api.Initable;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Our javascript-enabled console, which will log complete javascript objects, rather than a .toString() ->
 * "[Object]" mangling.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
@GwtPlatform
@SingletonOverride(implFor = LogService.class, priority=Integer.MIN_VALUE+1)
public class JsLog extends AbstractLog {

  public JsLog() {
    initialize();
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Iterable<Object> shouldIterate(Object m) {
    return m instanceof JavaScriptObject ? null : m instanceof Iterable ? (Iterable)m : m instanceof SimpleFifo ? ((SimpleFifo)m).forEach() : null;
  }

  private final native void initialize()
  /*-{
    if (!$wnd.console)
      $wnd.console = {
        log : function() {
        }
      };
    else if (!$wnd.console.log) $wnd.console.log = function() {
    };
    var logLevel;
    try {
      switch (this.@xapi.gwt.log.JsLog::initLogLevel()()) {
      case 'ALL':
        logLevel = @xapi.log.api.LogLevel::ALL;
        break;
      case 'DEBUG':
        logLevel = @xapi.log.api.LogLevel::DEBUG;
        break;
      case 'TRACE':
        logLevel = @xapi.log.api.LogLevel::TRACE;
        break;
      case 'INFO':
        logLevel = @xapi.log.api.LogLevel::INFO;
        break;
      case 'WARN':
        logLevel = @xapi.log.api.LogLevel::WARN;
        break;
      case 'ERROR':
        logLevel = @xapi.log.api.LogLevel::ERROR;
        break;
      default:
        logLevel = @xapi.log.api.LogLevel::ALL;
      }
    } catch (e) {
      $wnd.console.log('log init error', e);
    }
    this.@xapi.log.impl.AbstractLog::logLevel = logLevel;
  }-*/;

  protected String initLogLevel() {
    String qs = getQueryString().substring(1);
    for (String kvPair : qs.split("&")) {
      String[] kv = kvPair.split("=", 2);
      if (kv.length > 1 && "logLevel".equals(kv[0])) return kv[1];
    }
    return "ALL";
  }

  private native String getQueryString()
  /*-{
    return $wnd.location.search || '';
  }-*/;

  @Override
  public Object unwrap(LogLevel level, Object m) {
    return unwrapJs(m);
  }

  public native Object unwrapJs(Object m)
  /*-{
    try {
      //handy trick to unwrap boxed numbers in pretty and obf mode
      return m == undefined ? 'null'
          : m.innerHTML != undefined ? m
          : m['value'] !== undefined ? m['value']
          : m['value_0'] !== undefined ? m['value_0']
          : m['a'] !== undefined ? m['a']
          : m;
    } catch (e) {
      return m;
    }
  }-*/;

  @Override
  public Fifo<Object> newFifo() {
    return JsFifo.newFifo();
  }

  @Override
  public native void doLog(LogLevel level, Fifo<Object> o)
  /*-{
    try {
      $wnd.console.log(o);
    } catch (e) {
      if ($wnd.console) {
        $wnd.console.log = function() {
        };
      } else {
        $wnd.console = {
          log : function() {
          }
        }
      }
    }
  }-*/;

  protected native void consoleLog(Object o)
  /*-{
    try {
      $wnd.console.log(o);
    } catch (e) {//for junit / browsers w/out a console
      if (!$wnd.console)
        $wnd.console = {};
      if (!$wnd.console.log)
        $wnd.console.log = function() {}
      else
        $wnd.console.log('Error logging object', e);
    }
  }-*/;

}
