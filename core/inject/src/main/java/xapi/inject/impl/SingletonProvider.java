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
package xapi.inject.impl;

import xapi.util.api.ProvidesValue;
import xapi.util.impl.ImmutableProvider;

import javax.inject.Provider;

/**
 * A proxy class used to create a lazy-loading provider, with an efficient proxy (self-overwriting) proxy.
 * .get() calls {@link #initialValue()} on first load and replaces the initializer with a pojo getter.
 * This allows for easy wrapping of lazily-loaded singletons without null-checking on every access.
 * @param <X> - The type of data this var returns.
 *
 * @author James X Nelson (james@wetheinter.net, @ajax)
 */
public abstract class SingletonProvider<X> implements Provider<X>, ProvidesValue<X>{

  public class NullCheckOnGet implements Provider<X>{
    /**
     * Synchronized so multi-threaded platforms don't get into race conditions.
     * Has no effect in javascript {yet!}
     */
    @Override
    public final X get() {
      // perform a volatile read.  Much cheaper than a full synchronization
      Provider<X> provider = proxy;

      if (provider == this) {// double checked lock part 1
        //start lock; could be multiple threads here at once
        synchronized (this) {//compiles out of gwt
          provider = proxy; // another volatile read, in case we just blocked on the synchro
          if (provider == this) { // double checked lock part 2
            // we won the race condition
            X init = initialValue();//try to init,
            if (null == init) {
              return null;//pay for null-checks + synchro until inited
            }
            proxy = createImmutableProvider(init);//replace proxy with an immutable provider
            onCreate(init);//call on create inside synchro block.
            //it would be nice if we could do this outside the synchro block,
            //but then any threads who lose the race condition will get back
            //a partially initialized instance
            return init;
          } else {
            //another thread has already inited while we waited.
            return provider.get();//just return whatever the new proxy supplies
          }
        }
      } else {
        return proxy.get();
      }
    }
  }

  /**
   * The proxy object will override itself on first load with a bare pojo-based provider.
   *
   * In subclasses like @LazyPojo, this proxy object is toggled to allow for efficient get() when we can.
   *
   */
  protected volatile Provider<X> proxy;

	public SingletonProvider() {
	  proxy = new NullCheckOnGet();
	}


	/**
	 * Return a new proxy provider which just returns the set field.
	 *
	 * NEVER return an instance of {@link NullCheckOnGet},
	 * or you will contract a case of recursion sickness.
	 *
	 * @param init - A non-null value to provide immutably
	 * @return - An immutable provider.
	 *
	 * IF SUBCLASSES RETURN A PROVIDER THAT IS NOT A SUBCLASS OF
	 * {@link ImmutableProvider}, THEN YOU MUST OVERRIDE {@link #isSet()} AS WELL.
	 */

	protected Provider<X> createImmutableProvider(X init) {
	  assert init != null : "Do not send null values to the immutable provider; " +
	  		"the singleton will then return null forever.";
    return new ImmutableProvider<X>(init);
  }
  /**
   * Tells whether or not this singleton is already set by instanceof checking
   * the pojo class return from {@link #createImmutableProvider(Object)}.
   *
   * IF YOU OVERRIDE {@link #createImmutableProvider(Object)}, YOU MUST ALSO OVERRIDE isSet().
   * @return - Whether or not our proxy class matches the expected immutable class.
   */
	public boolean isSet(){
    return proxy instanceof ImmutableProvider;
  }

  /**
   * Called whenever the object is initialized.
   * Will be called on the first successful {@link #get()},
   * and every successful get() after a call to {@link #reset()}
   * @param init
   */
  protected void onCreate(X init) {
  }

  public void reset() {
    proxy = this;
  }

  /**
	 * Called once, on first .get(). Override to provide new value, or use @LazyPojo to set(X) externally.
	 * @return A singleton object that will be saved and returned on every subsequent .get()
	 */
	protected abstract X initialValue();
	/**
	 * This method is final so the compiler can optimize the call.
	 *
	 * This method is synchronized and null checks while the singleton is uninitialized,
	 * but once inited, it is unsynchronized with direct access, null-check free access to singleton.
	 */
	public final X get() {
		return proxy.get();
	}
}
