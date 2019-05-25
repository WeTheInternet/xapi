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
package xapi.inject;

import xapi.collect.api.Fifo;
import xapi.except.NotYetImplemented;
import xapi.util.api.ReceivesValue;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;

/**
 * Base class used for service injections.
 *
 * This class is filled out in the generator layer.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *
 * @param <T> - The type of singleton being injected
 */
public class AsyncProxy <T> {

  protected Fifo<ReceivesValue<T>> pending(){
    throw new NotYetImplemented("You must implement the pending method in generated AsyncProxy instances");
  }
  private int tries;
  public AsyncProxy() {
    tries = 2;
  }

  protected final void accept(ReceivesValue<T> callback){
    if (callback!=null)
      pending().give(callback);
  }

  public final void onFailure(Throwable reason) {
	  reason.printStackTrace();
	  GWT.log("Run async failure", reason);
    if (tries-->0){
      dispatch();
    }else{
      UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
      if (handler != null)
        handler.onUncaughtException(reason);
    }
  }

  protected final void apply(T value){
    Fifo<ReceivesValue<T>> pending = pending();
    while(!pending.isEmpty())
      pending.take().set(value);
  }

  protected void dispatch(){

  }

}