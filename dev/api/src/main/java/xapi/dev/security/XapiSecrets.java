package xapi.dev.security;

import xapi.annotation.inject.SingletonDefault;
import xapi.dev.X_Dev;
import xapi.dev.impl.ReflectiveMavenLoader;
import xapi.fu.Lazy;
import xapi.fu.MappedIterable;
import xapi.inject.X_Inject;
import xapi.mvn.api.MvnDependency;
import xapi.reflect.X_Reflect;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.function.Consumer;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/17/17.
 */
public interface XapiSecrets {

    // Yes, we're putting this here so you can use reflection.
    // how else do you talk to other classworlds?
    String fieldName = "INSTANCE";
    Lazy<XapiSecrets> INSTANCE = Lazy.deferred1(XapiSecretInstaller::create);

    /**
     * Provides a universal parent classloader to use for all processes.
     *
     * If you are using this, you should either install the XapiSecurityManager via system property:
     * -Djava.security.manager=xapi.security.XapiSecurityManager
     * or by immediately calling {@link XapiSecrets#enterUniverse(Runnable, Consumer)} in your java main.
     *
     * To avoid errors, you are recommended to implement Runnable in a class that is not your java main,
     * then call {@link XapiSecurityManager#getSecrets()} followed by {@link XapiSecrets#universalParent()}
     * to get the classloader you want to be the root of your application.
     *
     * Then, using that classloader, load your Runnable type reflectively,
     * and then invoke {@link XapiSecrets#enterUniverse(Runnable, Consumer)} with any sane executor
     * (Runnable::run is a fine executor for a main).
     *
     * This will ensure that everything called within that task has the universal parent as its class loader.
     *
     */
    URLClassLoader universalParent();

    static void enterUniverse(Runnable task, Consumer<Runnable> executor) {
        synchronized (XapiSecrets.class) {
            final XapiSecrets secrets = XapiSecurityManager.getSecrets();
            final ClassLoader source = Thread.currentThread().getContextClassLoader();
            final ClassLoader universal = secrets.universalParent();
            if (universal == null) {
                executor.accept(task);
            } else {
                if (X_Reflect.isParentOrSelf(universal)) {
                    executor.accept(task);
                } else {
                    // alright... we weren't yet running with our universal loader. Lets get on it.
                    Runnable callback = ()->executor.accept(task);
                    Thread bigBang = new Thread(callback);
                    bigBang.setName("Xapiverse");
                    // Now... we need a classloader who will prefer looking in universal loader,
                    // then fallback to the source loader
                    bigBang.setContextClassLoader(universal);
                    bigBang.setDaemon(true);
                    bigBang.start();
                }
            }
        }

    }
}

@SingletonDefault(implFor = XapiSecrets.class)
class XapiSecretsDefault implements XapiSecrets {

    private static final URLClassLoader classloader;
    static {
        final ReflectiveMavenLoader loader = new ReflectiveMavenLoader();
        String universalArtifact = X_Properties.getProperty(X_Namespace.PROPERTY_UNIVERSAL_COORDS,
            "net.wetheinter:xapi-dev-api:" + X_Namespace.XAPI_VERSION);
        final MvnDependency coords = MvnDependency.fromCoords(universalArtifact);
        final URL[] urls = loader.downloadDependency(coords)
            .map(MappedIterable::mapped)
            .out1()
            .map(X_Dev::ensureProtocol)
            .mapUnsafe(URL::new)
            .toArray(URL[]::new);
        classloader = new URLClassLoader(urls, null);
    }

    @Override
    public URLClassLoader universalParent() {
        return classloader;
    }
}

class XapiSecretInstaller {

    public static XapiSecrets create() {

        XapiSecrets secrets = null;
        // First, we check the system classloader for an instance of XapiSecrets that is already installed.
        final ClassLoader cl = ClassLoader.getSystemClassLoader();
        try {
            final Class<?> loaded = cl.loadClass(XapiSecrets.class.getName());
            secrets = extractSecrets(cl, loaded);
        } catch (ClassNotFoundException keepLooking) {
            // nope, not in here.  Check for ExtensibleClassLoader parent, who might know about a calling thread
            final ClassLoader search = Thread.currentThread().getContextClassLoader();
            // no point looking if we've reached the system classloader...
            if (search != cl) {

            }
        }

        if (secrets == null) {

            secrets = X_Inject.singleton(XapiSecrets.class);
        }

        return secrets;
    }

    private static XapiSecrets extractSecrets(ClassLoader cl, Class<?> loaded) throws ClassNotFoundException {
        final Class<?> lazyCls = cl.loadClass(Lazy.class.getName());
        if (lazyCls == Lazy.class) {
            // we speak the same classworld. Yay.
//            Lazy<XapiSecrets> secrets = lazyCls.getField("")
        }
        return null;
    }
}
