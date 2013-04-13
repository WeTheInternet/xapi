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

package xapi.annotation.inject;

/**
 * A common PlatformType enum to target dependency injection overrides to specific platforms.
 * <pre>
 * All - Inject into every build
 *
 * GwtAll - Inject into all gwt builds
 * GwtScript - Only inject into compiled gwt
 * GwtDev - Only inject into dev mode gwt
 * Flash - Only inject into flash gwt builds
 * Ios - Only inject into ios gwt builds
 *
 * Java - Only inject in pure java mode
 * Android - Only inject into android builds
 * WebApp - Only inject into web apps
 * Appengine - Only inject into appengine web apps
 * Desktop - Only inject into desktop java apps
 * </pre>
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public enum PlatformType{
  /** Inject into all implementations */
  All{
    @Override
    public boolean isJava() {
      return true;
    }
  },
  /** Inject into all gwt implementations, (everything but java and android) */
  GwtAll,
  /** Inject into compiled gwt only */
  GwtScript,
  /** Inject into dev-mode gwt only */
  GwtDev{
    @Override
    public boolean isJava() {
      return true;
    }
  },
  /** Inject into all non-gwt (pure java) builds*/
  Java{
    @Override
    public boolean isGwt() {
      return false;
    }
    @Override
    public boolean isJava() {
      return true;
    }
  }
  /** Inject into pure android only*/
  ,Android{
    @Override
    public boolean isGwt() {
      return false;
    }
    @Override
    public boolean isJava() {
      return true;
    }
  }
  /** Inject into flash builds of gwt (not implemented yet)*/
  ,Flash
  /** Inject into ios builds of playn (not implemented yet)*/
  ,Ios
  /** Inject into webapps only (not implemented yet)*/
  ,WebApp{
    @Override
    public boolean isGwt() {
      return false;
    }
    @Override
    public boolean isJava() {
      return true;
    }
  }
  /** Inject into appengine webapps only (not implemented yet)*/
  ,Appengine{
    @Override
    public boolean isGwt() {
      return false;
    }
    @Override
    public boolean isJava() {
      return true;
    }
  }
  /** Inject into desktop apps only (not implemented yet)*/
  ,Desktop{//This may get extended into Linux/Mac/Windows as well.
    @Override
    public boolean isGwt() {
      return false;
    }
    @Override
    public boolean isJava() {
      return true;
    }
  }
  ;
  /**
   * @return true if this service implementation is to be used in gwt.
   */
  public boolean isGwt() {
    return true;
  }
  /**
   * @return true if this service implementation is to be used in pure java.
   */
  public boolean isJava() {
    return false;
  }
}