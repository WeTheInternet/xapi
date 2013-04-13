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
package xapi.test.gwt.inject.cases;

import com.google.gwt.core.shared.GWT;

import xapi.annotation.inject.SingletonDefault;
import xapi.log.X_Log;
import xapi.test.gwt.inject.SplitPointTest.ImportTestInterface;


/**
 * imports a pile of c.g.g.client.ui, to get interwoven dependencies with ServiceTestImplementation
 * 
 * This whole class is supposed to be clobbered by {@link DeferredBindingOverride},
 * as it is replaced by GWT.create
 */

@SingletonDefault(implFor=ImportTestInterface.class)
public class ImportTestImplementation implements ImportTestInterface{
  @Override
  public void service() {
	  if (GWT.isClient()){
	    X_Log.info("Failed to override ImportTestImplementation with DeferredBindingOverride",this);
	    pullInImports();
	  }else{
		X_Log.info("Success injecting singleton test in JRE runtime");
	  }
  }
  protected void pullInImports(){
    com.google.gwt.user.client.ui.RootPanel.get().add(new com.google.gwt.user.cellview.client.CellTable<String>(1));
    com.google.gwt.user.client.ui.RootPanel.getBodyElement().setInnerHTML("Test failed; did not override ImportTestImplementation");
  }
}