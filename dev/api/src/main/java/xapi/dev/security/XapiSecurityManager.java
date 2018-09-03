package xapi.dev.security;

import xapi.fu.Lazy;
import xapi.fu.X_Fu;
import xapi.fu.iterate.ArrayIterable;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;
import xapi.util.X_Util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/18/17.
 */
public class XapiSecurityManager extends SecurityManager {

    private static final Lazy<XapiSecurityManager> INSTANCE = Lazy.deferred1(XapiSecurityManager::createManager);

    private static final int SLOT_SECRETS = 0;
    private static final int UNIVERSE_SIZE = 1;

    private final Lazy<XapiSecrets> secrets;
    private final Object[] universe;

    XapiSecurityManager(Object from) {
        super();
        secrets = Lazy.deferred1(this::createSecrets);

        if (from instanceof XapiSecurityManager) {
            universe = ((XapiSecurityManager) from).universe;
            return;
        }
        // if it wasn't a xapi manager our classlaoder understands, resort to reflection
        final Object[] result;
        try {
            final Field declared = from.getClass().getDeclaredField("universe");
            declared.setAccessible(true);
            result = (Object[]) declared.get(from);
        } catch (Throwable failure) {
            // normal in cases when a different security manager is present.
            X_Util.maybeRethrow(failure);
            universe = new Object[UNIVERSE_SIZE];
            X_Log.trace(XapiSecurityManager.class, "Initializing new universe", failure);
            return;
        }
        // proxy everything in universe :-(
        universe = proxyEverything(result);
    }

    private Object[] proxyEverything(Object[] result) {
        final Object[] all = new Object[result.length];
        try {
            final Object proxied = proxySecrets(result[0]);
            all[SLOT_SECRETS] = proxied;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw X_Fu.rethrow(e);
        }
        return all;
    }

    private Object proxySecrets(Object o) throws ClassNotFoundException, NoSuchMethodException {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final Class secretClass = cl.loadClass(XapiSecrets.class.getName());
        final Method universalParent = secretClass.getMethod("universalParent");
        return Proxy.newProxyInstance(cl, new Class[]{secretClass}, (inst, method, args)->{
            if (method.getName().equals("universalParent")) {
                return universalParent.invoke(o);
            }
            if (method.isDefault()) {
                final Class<?>[] paramTypes = ArrayIterable.iterate(args)
                    .map(Object::getClass)
                    .map2(X_Reflect::rebase, o.getClass().getClassLoader())
                    .toArray(Class[]::new);
                // now, we also need to proxy args... :-/
                final Object[] params = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    params[i] = maybeProxy(paramTypes[i], args[i]);
                }
                return X_Reflect.invokeDefaultMethod(o.getClass(), method.getName(), paramTypes, o, params);
            }
            throw new UnsupportedOperationException(method.toGenericString() + " unsupported");
        });
    }

    private Object maybeProxy(Class<?> type, Object arg) {
        if (type.isPrimitive() || type.isInstance(arg)) {
            return arg;
        }
        if (type.getName().startsWith("java")) {
            return arg;
        }
        // TODO: try loading a generated type.getName() + "InvHandler" invocation handler,
        // so we can at least make xapi.fu automatically supported...
        throw new UnsupportedOperationException("Proxy for " + type.getName() + " not yet implemented");
    }

    XapiSecurityManager() {
        super();
        secrets = Lazy.deferred1(this::createSecrets);
        universe = new Object[UNIVERSE_SIZE];
    }

    protected XapiSecrets createSecrets() {
        Object secrets = universe[SLOT_SECRETS];
        if (secrets instanceof XapiSecrets) {
            return (XapiSecrets) secrets;
        }
        if (secrets == null) {
            final XapiSecrets s = X_Inject.singleton(XapiSecrets.class);
            universe[SLOT_SECRETS] = s;
            return s;
        }
        // create a proxy
        return X_Inject.singleton(XapiSecrets.class);
    }

    public static XapiSecrets getSecrets() {
        final XapiSecurityManager inst = INSTANCE.out1();
        // TODO be able to control who gets to touch secrets...
        return inst.secrets.out1();
    }

    private static XapiSecurityManager createManager() {
        final SecurityManager sec = System.getSecurityManager();
        final XapiSecurityManager manager;
        if (sec == null) {
            manager = new XapiSecurityManager();
            System.setSecurityManager(manager);
        } else if (sec instanceof XapiSecurityManager) {
            manager = (XapiSecurityManager) sec;
        } else if (
            sec.getClass().getName().equals(XapiSecurityManager.class.getName())
            || sec.getClass().getSuperclass().equals(XapiSecurityManager.class.getName())
        ) {
            // crap... a security manager from a different classloader... TODO: find a good auto-proxy lib (or write one)
            manager = new XapiDelegatingSecurityManager(sec);
        } else {
            manager = new XapiDelegatingSecurityManager(sec);
        }
        System.setSecurityManager(manager);

        return manager;
    }

    public static XapiSecurityManager getManager() {
        return INSTANCE.out1();
    }
}
