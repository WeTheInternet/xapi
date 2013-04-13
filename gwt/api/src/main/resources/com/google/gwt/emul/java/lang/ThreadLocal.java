package java.lang;
import java.io.Serializable;

/**
 * This is a gwt-only mirror of a ThreadLocal map.
 * 
 * Although it may be possible to use html5 workers to mimic threads,
 * for now, this object is a simple get/set bean, to replicate behavior of ThreadLocal.
 * 
 * This will allow pure java clients to enable multithreading,
 * so all core code will be threadsafe when html5 web workers can support using compiled gwt.
 * 
 * For optimal efficiency, this will likely require compiling multiple independant gwt modules
 * which simply export a common javascript api to communicate across windows.
 * 
 * Due to web workers not sharing memory space or objects access across multiple threads,
 * each ThreadLocal instance will still be nothing more than a singleton bean wrapper.
 * 
 * 
 */
public class ThreadLocal<T> implements Serializable{
    
  private static final long serialVersionUID = 6815410897499755586L;
  
  /**
   * The singleton bean value we will be wrapping.
   */
  protected T value;
  /**
   * A boolean used so we can mimic accurate ThreadLocal behavior,
   * which is to only call {@link #initialValue()} once, and once only,
   * until {@link #remove()} is called.
   * 
   * To override this behavior and call {@link #initialValue()} until it returns non-null,
   * just subclass ThreadLocal and define a method,
   * protected void setInitialValue(){
   *   T value = initialValue();
   *   set(value);
   *   return value;
   * }
   * 
   */
  protected transient boolean isInitialized = false;
  
    /**
     * Returns the current thread's "initial value" for this
     * thread-local variable.  This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, unless the thread previously invoked the {@link #set}
     * method, in which case the <tt>initialValue</tt> method will not
     * be invoked for the thread.  Normally, this method is invoked at
     * most once per thread, but it may be invoked again in case of
     * subsequent invocations of {@link #remove} followed by {@link #get}.
     *
     * <p>This implementation simply returns <tt>null</tt>; if the
     * programmer desires thread-local variables to have an initial
     * value other than <tt>null</tt>, <tt>ThreadLocal</tt> must be
     * subclassed, and this method overridden.  Typically, an
     * anonymous inner class will be used.
     *
     * @return the initial value for this thread-local
     */
    protected T initialValue() {
        return null;
    }

    /**
     * Creates a thread local variable.
     */
    public ThreadLocal() {
    }

    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * @return the current thread's value of this thread-local
     */
    public T get() {
      return value==null?setInitialValue():value;
    }

    /**
     * Variant of set() to establish initialValue. Used instead
     * of set() in case user has overridden the set() method.
     *
     * @return the initial value
     */
    protected T setInitialValue() {
      if (!isInitialized){
        value = initialValue();
        isInitialized = true;
      }
      return value;
    }

    /**
     * Sets the current thread's copy of this thread-local variable
     * to the specified value.  Most subclasses will have no need to 
     * override this method, relying solely on the {@link #initialValue}
     * method to set the values of thread-locals.
     *
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    public void set(T value) {
        this.value = value;
        isInitialized=true;
    }

    /**
     * Removes the current thread's value for this thread-local
     * variable.  If this thread-local variable is subsequently
     * {@linkplain #get read} by the current thread, its value will be
     * reinitialized by invoking its {@link #initialValue} method,
     * unless its value is {@linkplain #set set} by the current thread
     * in the interim.  This may result in multiple invocations of the
     * <tt>initialValue</tt> method in the current thread.
     *
     * @since 1.5
     */
     public void remove() {
       isInitialized = false;
       value = null;
     }
}
