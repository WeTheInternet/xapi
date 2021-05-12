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
package xapi.log.api;

import xapi.collect.fifo.Fifo;

public interface LogService {

  Object unwrap(LogLevel level, Object m);

  void doLog(LogLevel level, Fifo<Object> o);
  void log(LogLevel level, Object o);

  default void log(LogLevel level, Object ... o) {
    log(level, newFifo().giveAll(o));
  }

  LogLevel getLogLevel();

  void setLogLevel(LogLevel logLevel);

  boolean shouldLog(LogLevel level);

  //allow javascript to return an array for better console logging
  Fifo<Object> newFifo();

  Iterable<Object> shouldIterate(Object m);

  default void error(Class<?> cls, String string) {
    log(LogLevel.ERROR, cls, string);
  }

  default void warn(Class<?> cls, String string) {
    log(LogLevel.WARN, cls, string);
  }
  default void info(Class<?> cls, String string) {
    log(LogLevel.INFO, cls, string);
  }

  default void trace(Class<?> cls, String string) {
    log(LogLevel.TRACE, cls, string);
  }

  default void debug(Class<?> cls, String string) {
    log(LogLevel.DEBUG, cls, string);
  }

  default void spam(Class<?> cls, String string) {
    log(LogLevel.ALL, cls, string);
  }


  default void error(Class<?> cls, String string, Throwable e) {
    log(LogLevel.ERROR, cls, string, e);
  }

  default void warn(Class<?> cls, String string, Throwable e) {
    log(LogLevel.WARN, cls, string, e);
  }
  default void info(Class<?> cls, String string, Throwable e) {
    log(LogLevel.INFO, cls, string, e);
  }

  default void trace(Class<?> cls, String string, Throwable e) {
    log(LogLevel.TRACE, cls, string, e);
  }

  default void debug(Class<?> cls, String string, Throwable e) {
    log(LogLevel.DEBUG, cls, string, e);
  }

  default void spam(Class<?> cls, String string, Throwable e) {
    log(LogLevel.ALL, cls, string, e);
  }


}
