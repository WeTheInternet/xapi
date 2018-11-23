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
package xapi.log;

import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.inject.X_Inject;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;
import xapi.log.impl.JreLog;

/**
 * A cross platform logging class.
 *
 * It is a static utility class, and obeys the XApi naming convention,
 * which is to prefix exposed static services with X_
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class X_Log {
  //you may not have one of these; this method is static only.
  private X_Log(){}

  private static final Lazy<LogService> singleton = X_Inject.singletonLazy(LogService.class);

  public static LogLevel logLevel(){
    return service().getLogLevel();
  }
  public static boolean loggable(LogLevel logLevel){
    return service().shouldLog(logLevel);
  }
  public static void logLevel(LogLevel logLevel){
    service().setLogLevel(logLevel);
  }

  private static void log(LogService log, LogLevel l,String level, Object[] message){
    Fifo<Object> logMsg = log.newFifo();
    logMsg.give("[" +level+"]");
    for (Object m : message){
      Iterable<Object> iter = log.shouldIterate(m);
      if (iter == null) {
        logMsg.give(log.unwrap(l, m));
      } else {
        for (Object o : iter) {
          logMsg.give(log.unwrap(l, o));
        }
      }
    }
    log.doLog(l, logMsg);
  }

  public static void error(Object ... message){
    LogService service = service();
    if (service.shouldLog(LogLevel.ERROR))
      log(service, LogLevel.ERROR,"ERROR",message);
  }

  private static LogService service() {
    return singleton.isResolving() ? new JreLog() : singleton.out1();
  }

  public static void warn(Object ... message){
    LogService service = service();
    if (service.shouldLog(LogLevel.WARN))
      log(service,LogLevel.WARN,"WARN",message);
  }
  public static void info(Object ... message){
    LogService service = service();
    if (service.shouldLog(LogLevel.INFO))
      log(service,LogLevel.INFO,"INFO",message);
  }
  public static void trace(Object ... message){
    LogService service = service();
    if (service.shouldLog(LogLevel.TRACE))
      log(service,LogLevel.TRACE,"TRACE",message);
  }
  public static void debug(Object ... message){
    LogService service = service();
    if (service.shouldLog(LogLevel.DEBUG))
      log(service,LogLevel.DEBUG,"DEBUG",message);
  }
  public static void log(Class<?> caller, LogLevel info, Object ... objects) {
    // TODO adjust log-level based on caller class criteria
    Object o = new SimpleFifo<>(objects);
    switch(info) {
      case ALL:
      case DEBUG:
        debug(caller, o);
        return;
      case ERROR:
        error(caller, o);
        return;
      case INFO:
        info(caller, o);
        return;
      case TRACE:
        trace(caller, o);
        return;
      case WARN:
        warn(caller, o);
        return;
    }
  }
  public static void log(LogLevel info, Object o) {
    switch(info) {
      case ALL:
      case DEBUG:
        debug(o);
        return;
      case ERROR:
        error(o);
        return;
      case INFO:
        info(o);
        return;
      case TRACE:
        trace(o);
        return;
      case WARN:
        warn(o);
        return;
    }
  }

}
