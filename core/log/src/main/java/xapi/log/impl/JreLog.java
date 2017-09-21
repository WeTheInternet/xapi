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
package xapi.log.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.api.Fifo;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;
import xapi.util.X_Namespace;
import xapi.util.X_Runtime;

import java.io.PrintStream;

@SingletonDefault(implFor=LogService.class)
public class JreLog extends AbstractLog{

  public JreLog() {

    logLevel = LogLevel.valueOf(
        X_Runtime.isDebug() ?
        // System.getProperty is a GWT magic method; it must use string constants (even though X_Runtime.isDebug will also be a constant...
        System.getProperty(X_Namespace.PROPERTY_LOG_LEVEL, "ALL") :
        System.getProperty(X_Namespace.PROPERTY_LOG_LEVEL, "INFO")
    );
  }

  public void doLog(LogLevel level, Fifo<Object> array)
  {
    StringBuilder b = new StringBuilder();
    while(!array.isEmpty()){
      writeLog(level, b, array.take());
    }
    final String logString = b.toString();
    printLogString(level, logString);
  }

  protected void printLogString(LogLevel level, String logString) {
    (level == LogLevel.ERROR ? errorStream() : logStream()).println(logString);
  }

  protected PrintStream errorStream() {
    return System.err;
  }

  protected PrintStream logStream() {
    return System.out;
  }

  @Override
  public Object unwrap(LogLevel level, Object m) {
    if (m instanceof Class) {
      Class<?> c = (Class<?>)m;
      Class<?> enclosing = c;
      while (enclosing.isAnonymousClass() && enclosing.getEnclosingClass() != null) {
        enclosing = enclosing.getEnclosingClass();
      }
      final StackTraceElement[] traces = new Throwable().getStackTrace();
      for (StackTraceElement trace : traces) {
        // try for exact match first
        if (trace.getClassName().equals(c.getName())) {
          return " "+trace;
        }
      }
      // loosen up and try enclosing types...
      for (StackTraceElement trace : traces) {
        final String cls = trace.getClassName();
        if (cls.contains(enclosing.getName())) {
          while (c.getEnclosingClass() != null) {
            c = c.getEnclosingClass();
            if (trace.getClassName().equals(c.getName())) {
              return " " + trace;
            }

          }
        }
      }
      return c.getCanonicalName();
    }
    return super.unwrap(level, m);
  }

}
