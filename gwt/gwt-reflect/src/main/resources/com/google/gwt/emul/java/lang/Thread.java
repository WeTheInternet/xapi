package java.lang;

import java.lang.Thread.State;
import java.lang.reflect.Array;
import java.util.concurrent.locks.LockSupport;

import javax.inject.Provider;

import com.google.gwt.user.client.Timer;


/**
 * A <i>thread</i> is a thread of execution in a program. The Java 
 * Virtual Machine allows an application to have multiple threads of 
 * execution running concurrently. 
 * <p>
 * Compiled GWT does not have support for concurrency or blocking.
 * Although sleep can be achieved with a java applet,
 * it still causes many browsers to block, which will only waste cpu, cause huge glitches
 * and lead to generally terrible user experience.
 * <p>
 * So, for now, a Thread is simply a Timer-backed compatibility class.
 * <p>
 * Work is being done, however, to implement a special generator to enable multi-threading.
 * <p>
 * Using the reflection support, we can take translate a Class&lt;? extends Thread>
 * into a runAsync block of code which exports all reflection data needed to implement run()
 * as javascript commands into a web worker, which can be sent the script, a blob of
 * constants, and then accept "foreign object messages", to be able to operate on Objects 
 * from the root page, and return Objects that make sense in the root page context.
 * <p>
 * It will actually be a subclass of Thread, called GwtThread, which will launch 
 * a dedicated compile for that thread (it's own "classloader"), and the proxy
 * instance owned by the root page will handle de/serialization between threads.
 * <p>
 * This communication will likely be done with typed in() and out() methods,
 * such that it will be trivial to stream objects through a thread (especially nice
 * once lambdas are supported in GWT).
 * <p>
 * In future iterations, a generic ReflectionThread could be implemented to work in
 * multi-classloader threaded environments; by transpiling the run method into reflection statements,
 * it will be possible to write code as if it is all running in the same environment,
 * but behind the scenes, just reduce everything to constants and reflection api calls.
 * 
 */
public
class Thread implements Runnable {
    static {
      //prepare our virtual thread manager
    }

    private char  name[];
    private int         priority = NORM_PRIORITY;
    private Runnable target;

    /**
     * The minimum priority that a thread can have. 
     */
    public final static int MIN_PRIORITY = 1;
   /**
     * The default priority that is assigned to a thread. 
     */
    public final static int NORM_PRIORITY = 5;
    /**
     * The maximum priority that a thread can have. 
     */
    public final static int MAX_PRIORITY = 10;

    /* If stop was called before start */
    private boolean stopBeforeStart;
    
    private static Provider<Thread> currentThread = new Provider<Thread>(){
      private transient Thread t;
      public Thread get() {
        return t == null ? (new Thread(null, "root") {
          protected void init(Runnable target, String name, long stackSize) {
            t = this;// first one is root!
            // note that init is called synchronously from all Thread constructors.
            this.cl = ClassLoader.getSystemClassLoader();
            super.init(target, name, stackSize);
          };
        }) : t;
      };
    };

//    /* Remembered Throwable from stop before start */
//    private Throwable throwableFromStop;

    /* Whether or not the Thread has been completely constructed;
     * init or clone method has successfully completed */
    private Thread me;    // null

    private State state;
    
    protected ClassLoader cl;
    
    /**
     * Returns a reference to the currently executing thread object.
     *
     * @return  the currently executing thread.
     */
    public static Thread currentThread(){
      return currentThread.get();
    }

    /**
     * Causes the currently executing thread object to temporarily pause 
     * and allow other threads to execute. 
     */
    public static void yield(){
      // does nothing in gwt.
      // might be used to ping workers to tell them to pause processing
    }

    /** 
     * Causes the currently executing thread to sleep (temporarily cease 
     * execution) for the specified number of milliseconds, subject to 
     * the precision and accuracy of system timers and schedulers. The thread 
     * does not lose ownership of any monitors.
     *
     * @param      millis   the length of time to sleep in milliseconds.
     * @exception  InterruptedException if any thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     * @see        Object#notify()
     */
    public static void sleep(long millis) throws InterruptedException{
      sleep(millis, 0);
    }
    public static void sleep(long millis, int nanos) throws InterruptedException{
      
    }


    /**
     * Initializes a Thread.

     * @param target the object whose run() method gets called
     * @param name the name of the new Thread
     * @param stackSize the desired stack size for the new thread, or
     *        zero to indicate that this parameter is to be ignored.
     */
    protected void init(Runnable target, String name,
                      long stackSize) {
  Thread parent = currentThread();
  this.cl = parent.cl;
  this.priority = parent.getPriority();
  this.name = name.toCharArray();
  this.target = target;
  setPriority(priority);
  this.me = this;
    }

    /**
     * Allocates a new <code>Thread</code> object. This constructor has 
     * the same effect as <code>Thread(null, null,</code>
     * <i>gname</i><code>)</code>, where <b><i>gname</i></b> is 
     * a newly generated name. Automatically generated names are of the 
     * form <code>"Thread-"+</code><i>n</i>, where <i>n</i> is an integer. 
     *
     * @see     #Thread(ThreadGroup, Runnable, String)
     */
    public Thread() {
  init(null, "Thread-" + hashCode(), 0);
    }

    /**
     * Allocates a new <code>Thread</code> object. This constructor has 
     * the same effect as <code>Thread(null, target,</code>
     * <i>gname</i><code>)</code>, where <i>gname</i> is 
     * a newly generated name. Automatically generated names are of the 
     * form <code>"Thread-"+</code><i>n</i>, where <i>n</i> is an integer. 
     *
     * @param   target   the object whose <code>run</code> method is called.
     * @see     #Thread(ThreadGroup, Runnable, String)
     */
    public Thread(Runnable target) {
  init(target, "Thread-" + hashCode(), 0);
    }

    /**
     * Allocates a new <code>Thread</code> object.
     *
     * @param   target   the object whose <code>run</code> method is called.
     * @param   name     the name of the new thread.
     */
    public Thread(Runnable target, String name) {
  init(target, name, 0);
    }

    /**
     * Causes this thread to begin execution; 
     * This is achieved in gwt using javascript timeouts;
     * the scheduler tries to virtualize some functionality of threads,
     * but cannot achieve proper blocking, so beware discrepancies with normal java threading.
     * 
     * In particular, {@link #sleep(long)} and {@link #yield()} will both throw unchecked exceptions,
     * which our scheduling virtualizer will catch to determine how long to wait until servicing a thread again.
     * 
     * It is never legal to start a thread more than once.
     * In particular, a thread may not be restarted once it has completed
     * execution.
     *
     * @exception  IllegalThreadStateException  if the thread was already
     *               started.
     * @see        #run()
     * @see        #stop()
     */
    public synchronized void start() {
        if (!stopBeforeStart) {
          //add current thread to scheduler
          new Timer() {
            @Override
            public void run() {
              if (stopBeforeStart)return;
              Provider<Thread> parent = currentThread;
              try {
                currentThread = new Provider<Thread>() {
                  public Thread get() {
                    return Thread.this;
                  }
                };
                Thread.this.run();
              } finally {
                currentThread = parent;
              }
            }
          }.schedule(0);
        }
    }


    /**
     * If this thread was constructed using a separate 
     * <code>Runnable</code> run object, then that 
     * <code>Runnable</code> object's <code>run</code> method is called; 
     * otherwise, this method does nothing and returns. 
     * <p>
     * Subclasses of <code>Thread</code> should override this method. 
     *
     * @see     #start()
     * @see     #stop()
     * @see     #Thread(ThreadGroup, Runnable, String)
     */
    public void run() {
  if (target != null) {
      target.run();
  }
    }

    /**
     * This method is called by the system to give a Thread
     * a chance to clean up before it actually exits.
     */
    private void exit() {
  /* Aggressively null out all reference fields */
  target = null;
  /* Speed the release of some of these resources */
        uncaughtExceptionHandler = null;
        //TODO: remove this thread from all active maps
    }

    /** 
     * Forces the thread to stop executing.
     * 
     * In gwt, this merely prevents the thread from ever starting.
     */
    @Deprecated
    public final void stop() {
      stopBeforeStart = true;
      exit();
    }

    @Deprecated
    public final synchronized void stop(Throwable obj) {
      stopBeforeStart = true;
      exit();
    }


    /**
     * Interrupts this thread.
     * 
     * Does nothing in gwt.
     */
    public void interrupt() {
      
    }

    /**
     * Tests whether the current thread has been interrupted.  The
     * <i>interrupted status</i> of the thread is cleared by this method.  In
     * other words, if this method were to be called twice in succession, the
     * second call would return false (unless the current thread were
     * interrupted again, after the first call had cleared its interrupted
     * status and before the second call had examined it).
     *
     * <p>A thread interruption ignored because a thread was not alive 
     * at the time of the interrupt will be reflected by this method 
     * returning false.
     *
     * @return  <code>true</code> if the current thread has been interrupted;
     *          <code>false</code> otherwise.
     * @see #isInterrupted()
     * @revised 6.0
     */
    public static boolean interrupted() {
  return currentThread().isInterrupted(true);
    }

    /**
     * Tests whether this thread has been interrupted.  The <i>interrupted
     * status</i> of the thread is unaffected by this method.
     *
     * <p>A thread interruption ignored because a thread was not alive 
     * at the time of the interrupt will be reflected by this method 
     * returning false.
     *
     * @return  <code>true</code> if this thread has been interrupted;
     *          <code>false</code> otherwise.
     * @see     #interrupted()
     * @revised 6.0
     */
    public boolean isInterrupted() {
  return isInterrupted(false);
    }

    /**
     * Tests if some Thread has been interrupted.  The interrupted state
     * is reset or not based on the value of ClearInterrupted that is
     * passed.
     */
    private boolean isInterrupted(boolean ClearInterrupted){
      return false;//TODO: store a flag
    }
    /**
     * Tests if this thread is alive. A thread is alive if it has 
     * been started and has not yet died. 
     *
     * @return  <code>true</code> if this thread is alive;
     *          <code>false</code> otherwise.
     */
    public final boolean isAlive(){
      return true;//TODO: store a flag
    }

    @Deprecated
    public final void suspend() {
      //remove this thread from the running list
    }

    @Deprecated
    public final void resume() {
      //add this thread back to the running list
    }

    /**
     * Changes the priority of this thread. 
     */
    public final void setPriority(int newPriority) {
      if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
          throw new IllegalArgumentException();
      }
      priority = newPriority;
    }

    /**
     * Returns this thread's priority.
     *
     * @return  this thread's priority.
     * @see     #setPriority
     */
    public final int getPriority() {
  return priority;
    }

    /**
     * Changes the name of this thread to be equal to the argument 
     * <code>name</code>. 
     * <p>
     * First the <code>checkAccess</code> method of this thread is called 
     * with no arguments. This may result in throwing a 
     * <code>SecurityException</code>. 
     *
     * @param      name   the new name for this thread.
     * @exception  SecurityException  if the current thread cannot modify this
     *               thread.
     * @see        #getName
     * @see        #checkAccess()
     */
    public final void setName(String name) {
  this.name = name.toCharArray();
    }

    /**
     * Returns this thread's name.
     *
     * @return  this thread's name.
     * @see     #setName(String)
     */
    public final String getName() {
  return String.valueOf(name);
    }




    /**
     * Waits at most <code>millis</code> milliseconds for this thread to 
     * die. A timeout of <code>0</code> means to wait forever. 
     *
     * @param      millis   the time to wait in milliseconds.
     * @exception  InterruptedException if any thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     */
    public final synchronized void join(long millis) 
    throws InterruptedException {
      
      //TODO: virtualize join by having the task manager ignore Threads waiting on others.
      
      Thread t = currentThread();
      //make t wait on this
      if (t == this)
        return;
      
    }


    /**
     * Waits for this thread to die. 
     *
     * @exception  InterruptedException if any thread has interrupted
     *             the current thread.  The <i>interrupted status</i> of the
     *             current thread is cleared when this exception is thrown.
     */
    public final void join() throws InterruptedException {
  join(0);
    }

    /**
     * Prints a stack trace of the current thread to the standard error stream.
     * This method is used only for debugging. 
     *
     * @see     Throwable#printStackTrace()
     */
    public static void dumpStack() {
  new Exception("Stack trace").printStackTrace();
    }


    /**
     * Returns a string representation of this thread, including the 
     * thread's name, priority, and thread group.
     *
     * @return  a string representation of this thread.
     */
    public String toString() {
      return "Thread[" + getName() + "," + getPriority() + "," + 
                "" + "]";
    }

    /**
     * Returns the identifier of this Thread.  
     * 
     * In gwt, we simply use the thread's hashcode
     *
     * @return this thread's ID.
     * @since 1.5
     */
    public long getId() {
        return hashCode();//we can safely use hashcode in gwt
    }

    /**
     * A thread state.  A thread can be in one of the following states: 
     * <ul>
     * <li>{@link #NEW}<br>
     *     A thread that has not yet started is in this state.
     *     </li>
     * <li>{@link #RUNNABLE}<br>
     *     A thread executing in the Java virtual machine is in this state. 
     *     </li>
     * <li>{@link #BLOCKED}<br>
     *     A thread that is blocked waiting for a monitor lock 
     *     is in this state. 
     *     </li>
     * <li>{@link #WAITING}<br>
     *     A thread that is waiting indefinitely for another thread to 
     *     perform a particular action is in this state. 
     *     </li>
     * <li>{@link #TIMED_WAITING}<br>
     *     A thread that is waiting for another thread to perform an action 
     *     for up to a specified waiting time is in this state. 
     *     </li>
     * <li>{@link #TERMINATED}<br> 
     *     A thread that has exited is in this state.
     *     </li>
     * </ul>
     *
     * <p>
     * A thread can be in only one state at a given point in time. 
     * These states are virtual machine states which do not reflect
     * any operating system thread states.
     * 
     * @since   1.5
     * @see #getState
     */
    public enum State {
        /**
         * Thread state for a thread which has not yet started.
         */
        NEW,
        
        /**
         * Thread state for a runnable thread.  A thread in the runnable
         * state is executing in the Java virtual machine but it may
         * be waiting for other resources from the operating system
         * such as processor.
         */
        RUNNABLE,
        
        /**
         * Thread state for a thread blocked waiting for a monitor lock.
         * A thread in the blocked state is waiting for a monitor lock
         * to enter a synchronized block/method or 
         * reenter a synchronized block/method after calling
         * {@link Object#wait() Object.wait}.
         */
        BLOCKED,
    
        /**
         * Thread state for a waiting thread.
         * A thread is in the waiting state due to calling one of the 
         * following methods:
         * <ul>
         *   <li>{@link Object#wait() Object.wait} with no timeout</li>
         *   <li>{@link #join() Thread.join} with no timeout</li>
         * </ul>
         * 
         * <p>A thread in the waiting state is waiting for another thread to
         * perform a particular action.  
         *
         * For example, a thread that has called <tt>Object.wait()</tt>
         * on an object is waiting for another thread to call 
         * <tt>Object.notify()</tt> or <tt>Object.notifyAll()</tt> on 
         * that object. A thread that has called <tt>Thread.join()</tt> 
         * is waiting for a specified thread to terminate.
         */
        WAITING,
        
        /**
         * Thread state for a waiting thread with a specified waiting time.
         * A thread is in the timed waiting state due to calling one of 
         * the following methods with a specified positive waiting time:
         * <ul>
         *   <li>{@link #sleep Thread.sleep}</li>
         *   <li>{@link Object#wait(long) Object.wait} with timeout</li>
         *   <li>{@link #join(long) Thread.join} with timeout</li>
         * </ul>
         */
        TIMED_WAITING,

        /**
         * Thread state for a terminated thread.
         * The thread has completed execution.
         */
        TERMINATED;
    }

  /**
   * Returns the state of this thread.
   * This method is designed for use in monitoring of the system state,
   * not for synchronization control.
   * 
   * @return this thread's state.
   * @since 1.5
   */
  public State getState() {
      return state;
  }

    
    /**
     * Interface for handlers invoked when a <tt>Thread</tt> abruptly 
     * terminates due to an uncaught exception. 
     * <p>When a thread is about to terminate due to an uncaught exception
     * the Java Virtual Machine will query the thread for its
     * <tt>UncaughtExceptionHandler</tt> using 
     * {@link #getUncaughtExceptionHandler} and will invoke the handler's
     * <tt>uncaughtException</tt> method, passing the thread and the
     * exception as arguments.
     * If a thread has not had its <tt>UncaughtExceptionHandler</tt>
     * explicitly set, then its <tt>ThreadGroup</tt> object acts as its
     * <tt>UncaughtExceptionHandler</tt>. If the <tt>ThreadGroup</tt> object
     * has no
     * special requirements for dealing with the exception, it can forward 
     * the invocation to the {@linkplain #getDefaultUncaughtExceptionHandler 
     * default uncaught exception handler}.
     *
     * @see #setDefaultUncaughtExceptionHandler
     * @see #setUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    public interface UncaughtExceptionHandler { 
        /** 
         * Method invoked when the given thread terminates due to the
         * given uncaught exception.
         * <p>Any exception thrown by this method will be ignored by the
         * Java Virtual Machine.
         * @param t the thread
         * @param e the exception
         */
        void uncaughtException(Thread t, Throwable e);
    }

    // null unless explicitly set
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    // null unless explicitly set
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    /**
     * Set the default handler invoked when a thread abruptly terminates
     * due to an uncaught exception, and no other handler has been defined
     * for that thread. 
     */
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
         defaultUncaughtExceptionHandler = eh;
     }

    /**
     * Returns the default handler invoked when a thread abruptly terminates
     * due to an uncaught exception. If the returned value is <tt>null</tt>,
     * there is no default.
     * @since 1.5
     * @see #setDefaultUncaughtExceptionHandler
     */
    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler(){
        return defaultUncaughtExceptionHandler;
    }

    /**
     * Returns the handler invoked when this thread abruptly terminates
     * due to an uncaught exception. If this thread has not had an
     * uncaught exception handler explicitly set then this thread's
     * <tt>ThreadGroup</tt> object is returned, unless this thread
     * has terminated, in which case <tt>null</tt> is returned.
     */
    public UncaughtExceptionHandler getUncaughtExceptionHandler() { 
        return uncaughtExceptionHandler != null ?
            uncaughtExceptionHandler : getDefaultUncaughtExceptionHandler();
    }

    /**
     * Set the handler invoked when this thread abruptly terminates
     * due to an uncaught exception. 
     */
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) { 
        uncaughtExceptionHandler = eh;
    }

    /**
     * Dispatch an uncaught exception to the handler. This method is 
     * intended to be called only by the JVM.
     */
    private void dispatchUncaughtException(Throwable e) {
        getUncaughtExceptionHandler().uncaughtException(this, e);
    }
    
    public ClassLoader getContextClassLoader(){
      return cl;
    }
    public void setContextClassLoader(ClassLoader cl){
      this.cl = cl;
    }
}
