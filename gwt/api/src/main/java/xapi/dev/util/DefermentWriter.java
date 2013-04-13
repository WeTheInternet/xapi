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
package xapi.dev.util;

import com.google.gwt.user.rebind.SourceWriter;
/**
 * A utility class for wrapping injected content within a deferred command.
 *
 * It takes a standard gwt-dev {@link SourceWriter}, and allows you to choose from a number of deferment
 * strategies; its primary use is in the core api is to enforce asynchronicity when using callbacks
 * during singleton injection.   If a splitpoint is already loaded when you request the code,
 * it will immediately execute your callback.
 *
 * Using deferment allows you to ensure asynchronous timing remains deterministic.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *
 */
public class DefermentWriter {

  public static enum DefermentStrategy{
    /**
     * The default deferment strategy: none.
     * If your code depends on asynchronocity, you must choose an explicit deferment strategy
     */
    NONE,
    /**
     * Uses Scheduler.get().scheduleFinally() to defer your callback.
     * This will ensure that a split point already loaded will execute in the same javascript
     * execution thread, but after all other code on the stack is executed.
     */
    FINALLY,
    /**
     * Uses Scheduler.get().scheduleDeferred() to push your callback into a new javascript
     * execution thread.  This is best for most use case that do not depend on updating
     * the DOM before the UI thread paints the screen.
     */
    DEFERRED,
    /**
     * Uses new Timer().schedule() to defer callbacks; this is the more heavyweight option,
     * but it does ensure a completely clean stack; best used for heavyweight processes.
     */
    TIMER,
    /**
     * Uses X_Process.defer(), which is not yet in the public API, but handles
     * execution in any runtime environment.  Use this if you need the generated code to run in a
     * non-gwt JRE.
     */
    X_PROCESS
  }

  //The source writer to which we will append deferment boilerplate
  private SourceWriter writer;
  //Default strategy is do nothing.
  private DefermentStrategy strategy = DefermentStrategy.NONE;
  /**
   * Create a new deferment writer to inject boilerplate for pushing code into the future.
   * @param writer - The source writer in which to inject code.
   */
  public DefermentWriter(SourceWriter writer) {
    this.writer = writer;
  }
  /**
   * @param strategy the strategy to set. {@see DefermentStrategy}
   */
  public void setStrategy(DefermentStrategy strategy) {
    this.strategy = strategy;
  }

  /**
   * Print the opening block of the chosen deferment strategy.
   */
  public void printStart(){
    switch (strategy){
      case NONE:
        return;
      case X_PROCESS:
        writer.println("X_Process.defer(new Runnable(){");
        writer.indent();
        writer.println("public void run(){");
        writer.indent();
        break;
      case DEFERRED:
        writer.println("Scheduler.get().scheduleDeferred(new ScheduledCommand(){");
        writer.indent();
        writer.println("public void execute(){");
        writer.indent();
        break;
      case TIMER:
        writer.println("new Timer(){");
        writer.indent();
        writer.println("public void run(){");
        writer.indent();
        break;
      case FINALLY:
        writer.println("Scheduler.get().scheduleFinally(new ScheduledCommand(){");
        writer.indent();
        writer.println("public void execute(){");
        writer.indent();
        break;
    }
  }
  /**
   * Print the closing block of the given deferment strategy.
   */
  public void printFinish(){
    switch (strategy){
      case NONE:
        return;
      case DEFERRED:
      case FINALLY:
      case X_PROCESS:
        writer.println("}");
        writer.outdent();
        writer.println("});");
        writer.outdent();
        break;
      case TIMER:
        writer.println("}");
        writer.outdent();
        writer.println("}.schedule(0);");
        writer.outdent();
        break;
    }
  }

}
