package com.google.gwt.emul.java.util;


import javax.inject.Provider;

// import xapi.util.api.ReceivesValue;
// implements , ReceivesValue<Class<S>>

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ServiceConfigurationError;

import com.google.gwt.core.shared.GWT;


public class ServiceLoader<S>
    implements Iterable<S>
{

    protected Provider<S> theProvider;

    public ServiceLoader() {
    }

    /**
     */
    public void reload() {
    }


    private static void fail(Class<?> service, String msg)
    throws ServiceConfigurationError
    {
  throw new ServiceConfigurationError(service.getName() + ": " + msg);
    }


    /**
     * Lazily loads the available providers of this loader's service.
     *
     * <p> The iterator returned by this method first yields all of the
     * elements of the provider cache, in instantiation order.  It then lazily
     * loads and instantiates any remaining providers, adding each one to the
     * cache in turn.
     *
     * <p> To achieve laziness the actual work of parsing the available
     * provider-configuration files and instantiating providers must be done by
     * the iterator itself.  Its {@link java.util.Iterator#hasNext hasNext} and
     * {@link java.util.Iterator#next next} methods can therefore throw a
     * {@link ServiceConfigurationError} if a provider-configuration file
     * violates the specified format, or if it names a provider class that
     * cannot be found and instantiated, or if the result of instantiating the
     * class is not assignable to the service type, or if any other kind of
     * exception or error is thrown as the next provider is located and
     * instantiated.  To write robust code it is only necessary to catch {@link
     * ServiceConfigurationError} when using a service iterator.
     *
     * <p> If such an error is thrown then subsequent invocations of the
     * iterator will make a best effort to locate and instantiate the next
     * available provider, but in general such recovery cannot be guaranteed.
     *
     * <blockquote style="font-size: smaller; line-height: 1.2"><span
     * style="padding-right: 1em; font-weight: bold">Design Note</span>
     * Throwing an error in these cases may seem extreme.  The rationale for
     * this behavior is that a malformed provider-configuration file, like a
     * malformed class file, indicates a serious problem with the way the Java
     * virtual machine is configured or is being used.  As such it is
     * preferable to throw an error rather than try to recover or, even worse,
     * fail silently.</blockquote>
     *
     * <p> The iterator returned by this method does not support removal.
     * Invoking its {@link java.util.Iterator#remove() remove} method will
     * cause an {@link UnsupportedOperationException} to be thrown.
     *
     * @return  An iterator that lazily loads providers for this loader's
     *          service
     */
    public Iterator<S> iterator() {
  return new Iterator<S>() {
    Provider<?> provider = theProvider;
      public boolean hasNext() {
        return provider != null;
      }

      public S next() {
        if (provider != null){
          S ret = (S) provider.get();
          provider = null;
          return ret;
        }
    throw new NoSuchElementException();
      }

      public void remove() {
        provider = null;
      }

  };
    }

    /**
     * Creates a new service loader for the given service type, using the
     * current thread's {@linkplain java.lang.Thread#getContextClassLoader
     * context class loader}.
     *
     * <p> An invocation of this convenience method of the form
     *
     * <blockquote><pre>
     * ServiceLoader.load(<i>service</i>)</pre></blockquote>
     *
     * is equivalent to
     *
     * <blockquote><pre>
     * ServiceLoader.load(<i>service</i>,
     *                    Thread.currentThread().getContextClassLoader())</pre></blockquote>
     *
     * @param  service
     *         The interface or abstract class representing the service
     *
     * @return A new service loader
     */
    public static <S> ServiceLoader<S> load(Class<S> service) {
      ServiceLoader<S> loader = GWT.<ServiceLoader<S>>create(ServiceLoader.class);
      loader.set(service);//let the generated class process this service interface.
      return loader;
    }

    public static <S> ServiceLoader<S> loadInstalled(Class<S> service) {
      return load(service);//only one service loader in gwt
    }
    public static <S> ServiceLoader<S> load(Class<S> service,
        ClassLoader loader)
     {
      return load(service);//ignore classloader in gwt
     }

    /**
     * Returns a string describing this service.
     *
     * @return  A descriptive string
     */
    public String toString() {
  return "java.util.ServiceLoader[" + theProvider + "]";
    }

    public void set(Class cls){
      //the generator will be overwriting this callsite for us...
    }

}
