package xapi.dev.impl;

import xapi.dev.api.MavenLoader;
import xapi.fu.Lazy;
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.fu.iterate.ArrayIterable;
import xapi.io.X_IO;
import xapi.log.X_Log;
import xapi.mvn.api.MvnDependency;
import xapi.reflect.X_Reflect;
import xapi.time.X_Time;
import xapi.util.X_Debug;
import xapi.util.X_Namespace;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class exists as a means to be able to download / use X_Maven,
 * even if your runtime classpath does not include it (and the giant pile
 * of dependencies we have to inherit from apache to use it).
 *
 * If you have X_Maven on classpath, we will use it reflectively to download dependencies.
 * If you do not, we will download the xapi-dev uber jar to a well known /tmp file location,
 * fire up a thread using it as classloader, and then access X_Maven through that thread,
 * returning an iterable of strings of jar files that correspond to the requested dependency.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/22/17.
 */
public class ReflectiveMavenLoader implements MavenLoader {


    interface MavenLoaderChannel {
        /**
         * Send a maven:coordinate:string to the loader to start downloading.
         *
         * We return an int so we can defer the final resolution until the result is requested.
         * This allows the caller to use this API synchronously or asynchronously
         */
        Out1<String[]> requestCoordinate(String coord);

        default String[] resolveCoordinateNow(String coord, long blocksFor, TimeUnit unit) throws TimeoutException {
            Out1<String[]> value = requestCoordinate(coord);
            if (unit == null) {
                // infinite blocking.
                return value.out1();
            } else {
                Mutable<String[]> result = new Mutable<>();
                X_Time.doLaterUnsafe(()->{
                    // rather than depending on other threads to be available,
                    // we should instead have a single shared interrupter thread,
                    // which will interrupt us after the given time period,
                    // so we can use concurrency primitives to do the work on the calling thread
                    // (rather than do a bunch of extra work just to wait to start work,
                    // and risk bugs from use of static ThreadLocals)
                    final String[] urls = value.out1();
                    result.in(urls);
                    synchronized (result) {
                        result.notify();
                    }
                });
                // Set a wakeup call.
                long millisToWait = unit.toMillis(blocksFor);
                long deadline = System.currentTimeMillis() + millisToWait;
                while (result.isNull() && System.currentTimeMillis() <= deadline) {
                    synchronized (result) {
                        try {
                            result.wait(deadline - millisToWait);
                            millisToWait = deadline - System.currentTimeMillis();
                        } catch (InterruptedException e) {
                            // Make any callers bocking on us able to see the correct interrupt status
                            if (result.isNonNull()) {
                                // If we were interrupted because we were finished,
                                // we should return successfully.
                                return result.out1();
                            }
                            Thread.currentThread().interrupt();
                            throw new RuntimeException(e);
                        }
                    }
                }
                if (System.currentTimeMillis() >= deadline) {
                    throw new TimeoutException("Waited " + blocksFor + " "+ unit + " but ReflectiveMavenLoader did not complete");
                }
                return result.out1();
            }
        }
    }

    private static final Lazy<MavenLoaderChannel> devJar = Lazy.deferred1Unsafe(()-> {
        String home = System.getProperty("user.home", System.getenv("user.home"));
        URL jarUrl = null;
        if (home != null) {
            File f = new File(home, ".m2/repository/net/wetheinter/xapi-dev/" +
                X_Namespace.XAPI_VERSION);
            File jarFile = new File(f, "xapi-dev-" + X_Namespace.XAPI_VERSION + ".jar");
            if (jarFile.exists()) {
                X_Log.info(ReflectiveMavenLoader.class, "Loading xapi-dev from local repo");
                jarUrl = jarFile.toURI().toURL();
            } else {
                // We're going to have to pull a copy down from maven central.
                String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
                File tmp = new File(tmpDir, "xapi");
                if (!tmp.exists()) {
                    boolean result = tmp.mkdirs();
                    if (!result) {
                        X_Log.warn(ReflectiveMavenLoader.class, "Unable to create tmpDir: ", tmp);
                    }
                }
                // Okay, lets check for the jar we want in the tmpDir, and only download if needed.
                String jarName = "xapi-dev-" + X_Namespace.XAPI_RELEASE_VERSION + ".jar";
                jarFile = new File(tmp, jarName);
                if (!jarFile.exists()) {
                    // no dice.  lets run a download...
                    String centralUrl = "http://central.maven.org/maven2/net/wetheinter/xapi-dev/"
                        + X_Namespace.XAPI_RELEASE_VERSION + "/xapi-dev-" + X_Namespace.XAPI_RELEASE_VERSION + ".jar";
                    jarUrl = new URL(centralUrl);

                    try (
                        FileOutputStream fout = new FileOutputStream(jarFile)
                    ) {
                        X_IO.drain(fout, jarUrl.openConnection().getInputStream());
                    }
                }
                jarUrl = jarFile.toURI().toURL();
            }
        }
        assert jarUrl != null : "No jar url...";

        // set the classloader with xapi-dev artifact...
        URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, null);
        final Class<?> threadClass = loader.loadClass(MavenLoaderThread.class.getName());
        final Class<?> out1Class = loader.loadClass(Out1.class.getName());

        Method loadCoords = X_Reflect.getPublicMethod(threadClass,"loadCoords", String.class);
        Method out1 = out1Class.getMethod("out1");
        Thread downloadThread = (Thread) threadClass.newInstance();
        downloadThread.setContextClassLoader(loader);
        downloadThread.start();
        return coord -> {
            try {
                Object result = loadCoords.invoke(downloadThread, coord);
                return Out1.out1Unsafe(()->{
                    String[] urls = (String[]) out1.invoke(result);
                    return urls;
                });
            } catch (Exception e) {
                throw X_Debug.rethrow(e);
            }
        };
    });

    @Override
    public Out1<Iterable<String>> downloadDependency(MvnDependency dependency) {
        X_Log.trace(ReflectiveMavenLoader.class, "Requested dependency", dependency);
        if (canDownloadFromMaven()) {
            // Huzzah; X_Maven is already on our classpath.  Lets use it reflectively.
            try {
                final Class<?> X_Maven = Thread.currentThread().getContextClassLoader()
                    .loadClass("xapi.mvn.X_Maven");
                final Method downloadDependencies = X_Maven.getMethod(
                    "downloadDependencies",
                    MvnDependency.class
                );
                Object result = downloadDependencies.invoke(null, dependency);
                return (Out1<Iterable<String>>) result;
            } catch (Exception e) {
                throw new IllegalStateException("Could not load X_Maven reflectively", e);
            }
        } else {
            // X_Maven is not on our classpath.  We'll need to download a copy / check local repo
            // Because we have not deployed xapi-dev to central recently, we will prefer looking
            // in the local repository first, using standard file locations.
            final MavenLoaderChannel devLoader = devJar.out1();
            final String coord = dependency.toCoords();
            final Out1<String[]> results = devLoader.requestCoordinate(coord);
            // At long last...  Our result!
            return results.map(ArrayIterable::iterate);
        }
    }

    protected boolean canDownloadFromMaven() {
        try {
            Thread.currentThread().getContextClassLoader()
                .loadClass("xapi.mvn.X_Maven");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}

