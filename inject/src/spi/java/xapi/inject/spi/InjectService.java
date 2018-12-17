package xapi.inject.spi;

import xapi.fu.Out1;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 12/8/18 @ 9:13 AM.
 */
public interface InjectService {

    String MANIFEST_NAME = "xapi.inject";

    void preload(Class<?> cls);

    void setInstanceFactory(Class<?> cls, Out1<?> factory);
    void setSingletonFactory(Class<?> cls, Out1<?> factory);

    <T> Out1<T> getInstanceFactory(Class<T> cls);
    <T> Out1<T> getSingletonFactory(Class<T> cls);

    void requireInstance(Class<?> cls);
    void requireSingleton(Class<?> cls);

    void reload(Class<?> cls);

}

