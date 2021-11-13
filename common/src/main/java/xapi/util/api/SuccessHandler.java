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
package xapi.util.api;
/**
 * A simple success callback, compatible with gwt AsyncCallback
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 * @param <Type> - The type of object / signal to be sent on success
 */
public interface SuccessHandler <Type> {
  /**
   * Called when an asynchronous process successfully returns.
   * @param t - An object to process on success.
   */
  public void onSuccess(Type t);

  @SuppressWarnings("rawtypes")
  final SuccessHandler NO_OP = new DoNothing();

  static <Type, Err extends Throwable> SuccessHandler<Type> handler(SuccessHandler<Type> onSuccess, ErrorHandler<Err> onFailure) {
    class WithErrorHandler implements SuccessHandler<Type>, ErrorHandler<Err> {

      @Override
      public void onSuccess(Type t) {
        try {
          onSuccess.onSuccess(t);
        } catch (Throwable e) {
          onFailure.onError((Err) e);
        } finally {
          synchronized (onSuccess) {
            onSuccess.notifyAll();
          }
        }
      }

      @Override
      public void onError(Err e) {
        onFailure.onError(e);
      }
    }
    return new WithErrorHandler();
  }
}
class DoNothing implements SuccessHandler<Object>{
  @Override
  public void onSuccess(Object t) {}
}
