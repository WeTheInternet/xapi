/*
 * Copyright 2012, We The Internet Ltd.
 * 
 * All rights reserved.
 * 
 * Distributed under a modified BSD License as follow:
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution, unless otherwise agreed to in a written document signed by a director of We The
 * Internet Ltd.
 * 
 * Neither the name of We The Internet nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package xapi.test.gwt.inject;

import javax.inject.Provider;

import xapi.annotation.inject.SingletonDefault;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.log.api.LogService;
import xapi.util.api.ReceivesValue;

import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;

// import xapi.test.suite.ServiceLoaderTest;

public class SplitPointTest {

  public static interface ServiceTestInterface {
    HTML service();
  }
  /**
   * import java.util and c.g.g.client.ui
   * 
   */
  @SingletonDefault(implFor = ServiceTestInterface.class)
  public static class ServiceTestImplementation implements ServiceTestInterface {
    @Override
    public HTML service() {
      HTML button = new HTML("Injection Success!");
      RootPanel.get().add(button);
      return button;
    }
  }
  public static interface ImportTestInterface {
    void service();
  }
  @SingletonDefault(implFor = StartTest.class)
  public static class StartTest implements ReceivesValue<LogService> {
    @Override
    public void set(LogService value) {
      injectImportInterface(value);
      importAll();
    }
  }
  @SingletonDefault(implFor = ServiceTestCallback.class)
  public static class ServiceTestCallback implements ReceivesValue<ServiceTestInterface> {
    @Override
    public void set(ServiceTestInterface testService) {
      // ServiceLoaderTest.assertNotNull("X_Inject did not return an injected ServiceTestInterface",testService);
      // ServiceLoaderTest.assertNotSame("X_Inject did not replace ServiceTestInterface",
      // ServiceTestInterface.class, testService.getClass());
      HTML result = testService.service();
      if (result.getHTML().contains("Success")) {
        xapi.log.X_Log.info("Injection Success!");
      } else {
        xapi.log.X_Log.info("Test Failure!");
      }
      //
      // xapi.log.X_Log.info(X_Inject.singleton(ServiceTestInterface.class));

      // Now test that code is in the correct splitpoint
      if (GWT.isScript()) {

      }
      if (onDone != null) {
        onDone.run();
        onDone = null;
      }
    }
  }

  private static Runnable onDone;

  public void test(Runnable onDone) {
    SplitPointTest.onDone = onDone;
    // start with a regular gwt code split, which forces all our code into split points
    // this makes it more difficult for the auto-generated code splitting to make clean cuts
    InstanceInterface instance = X_Inject.instance(InstanceInterface.class);
    instance.test();
    com.google.gwt.core.client.GWT.runAsync(X_Inject.class, new RunAsyncCallback() {
      @Override
      public void onSuccess() {
        X_Inject.singletonAsync(LogService.class, StartTest.class);
        X_Inject.singletonAsync(ImportTestInterface.class, ImportTestReceiver.class);
        X_Inject.singletonAsync(ImportTestInterface.class, ImportTestCallback.class);
        X_Inject.singletonAsync(ServiceTestInterface.class, ServiceTestCallback.class);
      }

      @Override
      public void onFailure(Throwable reason) {
      }
    });
  }

  protected static void importAll() {
    com.google.gwt.core.client.GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

      @Override
      public void onUncaughtException(Throwable e) {
        X_Log.error("Uncaught Error", e);

      }
    });
  }

  private static void injectImportInterface(LogService value) {
    Provider<ServiceTestInterface> service = X_Inject.singletonLazy(ServiceTestInterface.class);
    Provider<LogService> singleton = X_Inject.singletonLazy(LogService.class);
    xapi.log.X_Log.info("X_Log inject", service.get());
    xapi.log.X_Log.info("Consistency test: ", value == singleton.get());

  }

}