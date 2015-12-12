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

import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;

import javax.inject.Provider;
import java.util.Iterator;

public abstract class AbstractLog implements LogService {

	protected LogLevel logLevel = LogLevel.INFO;

	@Override
	public boolean shouldLog(LogLevel logLevel) {
		return this.logLevel.ordinal() <= logLevel.ordinal();
	}

	@Override
	public void log(LogLevel level, Object o) {
		if (shouldLog(level)) {
			Fifo<Object> arr = newFifo();
			arr.give(unwrap(level, o));
			doLog(level, arr);
		}
	}

    @Override
    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
	public Iterable<Object> shouldIterate(Object m) {
	  return m instanceof Fifo ? ((Fifo)m).forEach() : m instanceof Iterable ? (Iterable)m : null;
	}

	@Override
	public Fifo<Object> newFifo() {
    return new SimpleFifo<Object>();
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
	public Object unwrap(LogLevel level, Object m) {
		// unwrap throwables and log strack trace elements
		if (m instanceof Throwable) {
			StackTraceElement[] trace = ((Throwable) m).getStackTrace();
			String serialized = m + "\n";
			if (trace != null) {
        for (StackTraceElement el : trace) {
          serialized += String.valueOf(el)+"\n";
        }
      }
			return serialized;
		} else if (m != null && m.getClass().isArray()) {
          return new SimpleFifo<Object>((Object[])m).join(", ");
        } else if (m instanceof Provider) {
          return unwrap(level, ((Provider)m).get());
        } else if (m instanceof Iterable) {
		  Iterator iter = ((Iterable)m).iterator();
		  SimpleFifo fifo = new SimpleFifo();
		  while(iter.hasNext()) {
        fifo.give(String.valueOf(iter.next()));
      }
		  return "["+fifo.join(", ")+"]";
		} else if (m instanceof Fifo)
      return "{"+((Fifo)m).join(", ")+"}";
		return m == null ? "null" : m;
	}

	protected void writeLog(LogLevel level, StringBuilder b, Object object) {
		// TODO: inspect w/ conditional reflection
		b.append(unwrap(level, object));
		b.append("\t");
	}

	@Override
	public LogLevel getLogLevel() {
		return logLevel;
	}

	@Override
	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
	}

}
