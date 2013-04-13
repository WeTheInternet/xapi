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
package xapi.collect.impl;


import xapi.collect.api.InitMap;
import xapi.util.X_Runtime;
import xapi.util.api.ConvertsValue;

/**
 * A map for singleton values; if containsKey(key) == true,
 * the value is returned from the underlying ConcurrentHashMap, even if null.
 *
 * When a key is not present, the map will call {@link #initialize(Object)},
 * which accepts the key as parameter, and return the singleton which will be set in the map.
 *
 * This map IS entirely threadsafe.
 * The initialization process is guarded by a double-checked lock,
 * which is ONLY checked in jre runtime environments,
 * and only if the multithreading system property ("xapi.multithreaded") is set.
 *
 * If you set a value manually, {@link #initialize(Object)} will not be called for that key.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *
 * @param <Key> - The type of key to use.  Be sure to implement hashCode() and equals().
 * @param <Value> - The type of values provided.
 */
public abstract class AbstractInitMap <Key, Value> implements InitMap<Key,Value>{

  /**
   * A default toString converter that just calls String.valueOf.
   *
   * This will return "null" for null.
   * But, you weren't using "null" for an id anyway, were you?
   */
  @SuppressWarnings("rawtypes")
  protected static final ConvertsValue TO_STRING = new ConvertsValue<Object,String>() {
    @Override
    public String convert(Object from) {
      return String.valueOf(from);// will return "null" for null.
    }
  };
  public static final ConvertsValue<String, String> PASS_THRU = new ConvertsValue<String,String>() {
    @Override
    public String convert(String from) {
      return from;// will return null for null.
    }
  };
  public static final ConvertsValue<Class<?>, String> CLASS_NAME = new ConvertsValue<Class<?>,String>() {
    @Override
    public String convert(Class<?> from) {
      // fail fast for gwt's sake, so we get a usable exception
      if (from == null)throw new NullPointerException();
      return from.getName();
    }
  };
  @SuppressWarnings("rawtypes") //it erases to object, we're fine
  protected static final ConvertsValue ALWAYS_NULL = new ConvertsValue() {
    @Override
    public Object convert(Object from) {
      return null;
    }
  };

  // This class is super-sourced without ConcurrentMap, for code size optimization.
  // We would inject it, but it is used in the inject subsystem;
  private final ConvertsValue<Key,String> keyProvider;

  @SuppressWarnings("unchecked") // Accepts Object in only method's only param.
  protected AbstractInitMap() {
    this(TO_STRING);
  }

  protected AbstractInitMap(ConvertsValue<Key,String> keyProvider) {
    this.keyProvider = keyProvider;
    assert keyProvider != null : "Cannot use null key provider for init map.";
  }

	private static final Object default_lock = new Object();

	public Value get(Key k) {
	  String key = keyProvider.convert(k);
		if (hasValue(key))
			return getValue(key);
		Value value;
		if (isMultiThreaded()) {
		  //double-checked lock for multithreaded enviros only
		  synchronized(getLock(key)) {
  		  if (hasValue(key))
          return getValue(key);
		    //init object
		    value = initialize(k);
		    setValue(key, value);
		  }
		}else {
		    //init object
		    value = initialize(k);
		    setValue(key, value);
		}
		return value;
	}

	@Override
	public Value put(Key key, Value value) {
	  return setValue(keyProvider.convert(key), value);
	}

  public boolean containsKey(Key key) {
    return hasValue(keyProvider.convert(key));
  }

  protected boolean isMultiThreaded() {
	  return X_Runtime.isMultithreaded();
	}

	protected Object getLock(Object forKey) {
	  return default_lock;
	}

	public String toKey(Key key) {
	  return keyProvider.convert(key);
	}

}
