package xapi.inject.impl;

import static xapi.util.X_Namespace.DEFAULT_INSTANCES_LOCATION;
import static xapi.util.X_Namespace.DEFAULT_SINGLETONS_LOCATION;
import static xapi.util.X_Namespace.PROPERTY_INJECTOR;
import static xapi.util.X_Namespace.PROPERTY_INSTANCES;
import static xapi.util.X_Namespace.PROPERTY_RUNTIME_META;
import static xapi.util.X_Namespace.PROPERTY_SINGLETONS;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.inject.Provider;

import xapi.collect.api.InitMap;
import xapi.collect.impl.AbstractInitMap;
import xapi.collect.impl.InitMapDefault;
import xapi.inject.api.Injector;
import xapi.log.X_Log;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;
import xapi.log.impl.JreLog;
import xapi.util.X_Runtime;
import xapi.util.api.ConvertsValue;
import xapi.util.api.ReceivesValue;
import xapi.util.impl.ImmutableProvider;

public class JreInjector implements Injector{

  static final String DEFAULT_INJECTOR = "xapi.jre.inject.RuntimeInjector";

  private static final class RuntimeProxy
    extends SingletonProvider<ReceivesValue<String>>
    implements ReceivesValue<String>
  {
    @SuppressWarnings("unchecked")
    @Override
    protected ReceivesValue<String> initialValue() {
      try {
        String injector = System.getProperty(
          PROPERTY_INJECTOR,
          DEFAULT_INJECTOR);
      Class<?> cls =
        Class.forName(injector);
        return (ReceivesValue<String>)cls.newInstance();
      }catch(ClassNotFoundException e) {

      }catch(Exception e) {
        e.printStackTrace();
        Thread t = Thread.currentThread();
        t.getUncaughtExceptionHandler().uncaughtException(t, e);
      }
      return this;

    };
    @Override
    public void set(String value) {
      //no-op implementation.
      
    }
  };

  private static final RuntimeProxy runtimeInjector = new RuntimeProxy();

  private boolean initOnce = true;

	private static final SingletonProvider<String> instanceUrlFragment
		= new SingletonProvider<String>(){
			@Override
      protected String initialValue() {
				String value = System.getProperty(PROPERTY_INSTANCES,
						DEFAULT_INSTANCES_LOCATION);
				return value.endsWith("/")?value:value+"/";
			};
		};
		private static final SingletonProvider<String> singletonUrlFragment
		= new SingletonProvider<String>(){
			@Override
      protected String initialValue() {
				String value = System.getProperty(PROPERTY_SINGLETONS,
						DEFAULT_SINGLETONS_LOCATION);
				return value.endsWith("/")?value:value+"/";
			};
		};

	private AbstractInitMap<Class<?>, Provider<?>> instanceProviders
	 = InitMapDefault.createInitMap(AbstractInitMap.CLASS_NAME,
	   new ConvertsValue<Class<?>,Provider<?>>() {
	     public Provider<?> convert(Class<?> clazz) {
	       //First, lookup META-INF/instances for a replacement.
	       final String target;
	       try {
	         target = lookup(clazz, instanceUrlFragment.get(), JreInjector.this, instanceProviders);
	       } catch (Exception e) {
	         throw new RuntimeException("Could not find instance provider for "+clazz.getName(),e);
	       }
	       try{
	         final Class<?> cls =
	           Class.forName(target, true, clazz.getClassLoader());
	         return new Provider<Object>(){
	           @Override
	           public Object get() {
	             try{
	               return cls.newInstance();
	             }catch(Exception e){
	               e.printStackTrace();
	               throw new RuntimeException("Could not instantiate new instance of "+cls.getName()+" : "+target,e);
	             }
	           }
	         };
	       }catch(Exception e){
	         if (instanceProviders.containsKey(clazz))
	           return instanceProviders.get(clazz);
	         throw new RuntimeException("Could not create instance provider for "+clazz.getName()+" : "+target,e);
	       }
	     }
	 });
	/**
	 * Rather than use java.util.ServiceLoader, which only works in Java >= 6,
	 * and which can cause issues with android+proguard,
	 * we use our own simplified version of ServiceLoader,
	 * which can allow us to change the target directory from META-INF/singletons,
	 * and caches singletons internally.
	 *
	 * Note that this method will use whatever ClassLoader loaded the key class.
	 *
	 */
	private InitMap<Class<?>, Provider<Object>> singletonProviders =
	InitMapDefault.createInitMap(AbstractInitMap.CLASS_NAME, new
	  ConvertsValue<Class<?>,Provider<Object>>() {
	  public Provider<Object> convert(Class<?> clazz) {
	     //TODO: optionally run through java.util.ServiceLoader,
      //in case client code already uses ServiceLoader directly (unlikely edge case)
      String target = null;
      try{
        //First, lookup META-INF/singletons for a replacement.
        target = lookup(clazz, singletonUrlFragment.get(), JreInjector.this, singletonProviders);
        return
          new ImmutableProvider<Object>(Class.forName(target, true, clazz.getClassLoader())
          .newInstance());
      }catch(Throwable e){
        if (singletonProviders.containsKey(clazz))
          return singletonProviders.get(clazz);
        //Try to log the exception, but do not recurse into X_Inject methods
        if (clazz == LogService.class){
            LogService serv = new JreLog();
            ImmutableProvider<Object> provider = new ImmutableProvider<Object>(serv);
            singletonProviders.setValue(clazz.getName(), provider);
            return provider;
          }
        e.printStackTrace();
        String message = "Could not instantiate singleton for "+clazz.getName()+" using "+target;
        tryLog(message, e);
        throw new RuntimeException(message,e);
      }

	  };
	});
	private void tryLog(String message, Throwable e) {
		try{
			LogService log = (LogService) singletonProviders.get(LogService.class).get();
			log.log(LogLevel.ERROR,message);
		}catch(Exception ex){
		  System.err.println(message);
			ex.printStackTrace();
		}
	}

	private static String lookup(Class<?> cls, String relativeUrl, JreInjector injector, InitMap<Class<?>,?> map) throws IOException {
		String name = cls.getName();
		ClassLoader loader = cls.getClassLoader();
		if (!relativeUrl.endsWith("/"))
			relativeUrl+="/";
		URL resource = loader.getResource(relativeUrl+name);
		if (resource == null) {
		  if (injector.initOnce) {
		    injector.initOnce = false;
		    injector.init(cls, map);
		    resource = loader.getResource(relativeUrl+name);
		  }
		  if (resource == null) {
		    return name;
		  }
		}
		InputStream stream = resource.openStream();
		byte[] into = new byte[stream.available()];
		stream.read(into);
		try{
			return new String(into).split("\n")[0];
		}finally{
			stream.close();
		}
	}


	@Override
	@SuppressWarnings("unchecked")
	  public <T> T create(Class<? super T> cls) {
	    try {
	      return (T) instanceProviders.get(cls).get();
	    } catch (Exception e) {
	      if (initOnce) {
	        X_Log.warn("Instance provider failed; attempting runtime injection", e);
	        initOnce = false;
	        init(cls, instanceProviders);
	        return create(cls);
	      }
			//Try to log the exception, but do not recurse into X_Inject methods
			String message = "Could not instantiate instance for "+cls.getName();
			tryLog(message, e);
			throw new RuntimeException(message,e);
	    }
	  }


	  @SuppressWarnings("unchecked")
	public <T> T provide(Class<? super T> cls){
	  try {
		  return (T) singletonProviders.get(cls).get();
	  } catch (Exception e) {
	    if (initOnce) {
	      X_Log.warn("Singleton provider failed; attempting runtime injection", e);
	      initOnce = false;
	      init(cls, singletonProviders);
	      return provide(cls);
	    }

			//Try to log the exception, but do not recurse into X_Inject methods

			String message = "Could not instantiate singleton for "+cls.getName();
			tryLog(message, e);
			throw new RuntimeException(message,e);
	  }
	}

    protected void init(Class<?> on, InitMap<Class<?>, ?> map) {

	    X_Log.warn(getClass(), "X_Inject encountered a class without injection metadata:",on);
	    if (!"false".equals(System.getProperty("xinject.no.runtime.injection"))) {
	      X_Log.info(getClass(), "Attempting runtime injection.");
	      try {
	        runtimeInjector.get().set(
	          System.getProperty(PROPERTY_RUNTIME_META, "target/classes")
	        );
	        X_Log.info(getClass(), "Runtime injection success.");
	      }catch (Exception e) {
	        X_Log.warn(getClass(), "Runtime injection failure.",e);
	      }
	    }
	  }
    
    @Override
    public <T> void setInstanceFactory(Class<T> cls, Provider<T> provider) {
      if (X_Runtime.isDebug())
        X_Log.debug("Setting instance factory for ",cls);
      instanceProviders.put(cls, provider);
    }
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" }) // Our target is already erased to Object
    public <T> void setSingletonFactory(Class<T> cls, Provider<T> provider) {
      if (X_Runtime.isDebug())
        X_Log.debug("Setting singleton factory for ",cls);
      singletonProviders.put(cls, (Provider)provider);
    }


	public void initialize(Object o) {
	}

}
