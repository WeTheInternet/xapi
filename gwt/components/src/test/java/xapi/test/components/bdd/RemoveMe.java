package xapi.test.components.bdd;

import org.junit.Test;
import xapi.dev.gwtc.api.GwtcProjectGenerator;
import xapi.dev.gwtc.api.GwtcService;
import xapi.fu.Out2;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.GwtManifest.CleanupMode;
import xapi.gwtc.api.ObfuscationLevel;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.api.JavacService;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

import com.google.gwt.core.ext.TreeLogger.Type;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 4/4/17.
 */
public class RemoveMe {

    interface BundleService {
        void addBundle(String uuid, String type);

        int size();

        Iterable<Bundle> findBundle(Query query);
        void clear();
    }
    static class Bundle {
        String device_uuid;
        String sensor_type;
        int sensor_value;
        long sensor_read_time;

        public Bundle(String uuid, String type, int value) {
            this.device_uuid = uuid;
            this.sensor_type = type;
            this.sensor_value = value;
            sensor_read_time = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return
                "{\n" +
                    "  \"device_uuid\": \"" + device_uuid + "\",\n" +
                    "  \"sensor_type\": \"" + sensor_type + "\",\n" +
                    "  \"sensor_value\": " + sensor_value + ",\n" +
                    "  \"sensor_reading_time\": " + sensor_read_time + "\n" +
                    "}";
        }
    }
    static class Query {
        String device_uuid;
        String sensor_type;
        long start_time;
        long end_time;
    }

    private class VectorBundleService extends ListBundleService {

        @Override
        protected List<String> createList() {
            return new Vector<>();
        }

        @Override
        public Iterator<String> iterator() {
            // Vectors real iterator will throw concurrent modification exception.
            return new Iterator<String>() {
                int i;
                @Override
                public boolean hasNext() {
                    return i < bundles.size();
                }

                @Override
                public String next() {
                    return bundles.get(i++);
                }
            };
        }
    }
    private class ListBundleService implements BundleService, Iterable<String> {

        List<String> bundles = createList();

        protected List<String> createList() {
            return new CopyOnWriteArrayList<>();
        }

        @Override
        public void addBundle(String uuid, String type) {
            bundles.add(newBundle(uuid, type).toString());
        }

        @Override
        public int size() {
            return bundles.size();
        }

        @Override
        public Iterator<String> iterator() {
            return bundles.iterator();
        }

        @Override
        public Iterable<Bundle> findBundle(Query query) {
            List<Bundle> results = new ArrayList<>();
            for(String storebundle : this) {
                storebundle = storebundle.trim();

                //remove the {} from the bundle to enable further parsing.
                storebundle = storebundle.substring(1,storebundle.length() - 1);
                //parse retrieved bundle for desired device_uuid and sensor_type and sensor_reading_time values from qbundle.
                String[] bpairs = storebundle.split(",");
                String uuidbpair = bpairs[0].trim();
                String stypebpair = bpairs[1].trim();

                String svalbpair = bpairs[2].trim();
                String srtimebpair = bpairs[3].trim();
                String srbtime = srtimebpair.split(":")[1].trim();


                //remove the double quotes from the decimal characters and then parse them into an int
                long srbtimei = Long.parseLong(srbtime);

                //generate a boolean that determines if the current store bundle is time bound by the query bundles start and end time values.
                boolean istimebound = false;
                if(srbtimei > query.start_time && srbtimei < query.end_time) istimebound = true;

                //generate a triple conditional for identifying and selecting only those bundles that satisfy all three desired attributes.
                if(uuidbpair.split(":")[1].trim().equals(query.device_uuid) && stypebpair.split(":")[1].trim().equals(query.sensor_type) && istimebound) {
                    results.add(new Bundle(query.device_uuid, query.sensor_type, Integer.parseInt(svalbpair)));
                }
            }

            return results;
        }

        @Override
        public void clear() {
            bundles.clear();
        }
    }

    private class MapBundleService implements BundleService {

        private final Map<String, List<Bundle>> bundles = createMap();
        volatile int items;
        protected Map<String, List<Bundle>> createMap() {
            return new ConcurrentHashMap<>();
        }

        protected List<Bundle> createList() {
            return new CopyOnWriteArrayList<>();
        }

        @Override
        public void addBundle(String uuid, String type) {
            items++;
            final Bundle bundle = newBundle(uuid, type);
            bundles.computeIfAbsent(uuid, u->createList())
                .add(bundle);
        }

        @Override
        public int size() {
            return items;
        }

        @Override
        public List<Bundle> findBundle(Query query) {
            final List<Bundle> results = bundles.get(query.device_uuid);
            if (results == null) {
                return Collections.emptyList();
            }
            return results
                .stream()
                .filter(b->
                        b.sensor_type.equals(query.sensor_type) &&
                        b.sensor_read_time >= query.start_time &&
                        b.sensor_read_time <= query.end_time
                ).collect(Collectors.toList());
        }

        @Override
        public synchronized void clear() {
            items = 0;
            bundles.clear();
        }
    }
    private class HashTableBundleService extends MapBundleService {

        @Override
        protected Map<String, List<Bundle>> createMap() {
            return new Hashtable<>();
        }

        @Override
        protected List<Bundle> createList() {
            return new Vector<>();
        }
    }

    private volatile int runningThreads;

    @Test(timeout = 600_000)
    public void stressTest() throws InterruptedException {
        BundleService service = new MapBundleService();
        BundleService table = new HashTableBundleService();
        BundleService list = new ListBundleService();
        BundleService vector = new VectorBundleService();
        stressTest("conchashmap   ", service, 1, 1, 10_000);
        stressTest("hash table    ", table, 1, 1, 10_000);
        stressTest("conchashmap   ", service, 10, 10, 100_000);
        stressTest("hash table    ", table, 10, 10, 100_000);
        stressTest("conchashmap   ", service, 100, 100, 1_000_000);
        stressTest("hash table    ", table, 100, 100, 1_000_000);
        stressTest("conclist      ", list, 1, 1, 1_000);
        stressTest("conclist      ", list, 10, 10, 2_000);
        stressTest("conclist      ", list, 100, 100, 4_000);
        stressTest("vector        ", vector, 1, 1, 1000);
        stressTest("vector        ", vector, 10, 10, 2000);
        stressTest("vector        ", vector, 100, 100, 4000);
    }

    private void stressTest(String type, BundleService service, int numWriters, int numReaders, int numResults) throws InterruptedException {
        final long memory = Runtime.getRuntime().freeMemory();
        runningThreads = 0;
        AtomicInteger finished = new AtomicInteger(0);
        startThreads(service, numWriters, finished);
        final long start = System.nanoTime();
        int wait = 10;
        long waited = 1;
        while (finished.intValue() < numResults) {
            synchronized (finished) {
                finished.wait(wait);
                wait = Math.min(1_100, wait * 2);
            }
            long now = TimeUnit.NANOSECONDS.toSeconds(
                System.nanoTime() - start
            );
            if (now > waited) {
                waited = now;
                System.out.println(String.format(
                    "%s waited %ds, read %d items, wrote %d items",
                    type,
                    waited,
                    finished.get(),
                    service.size()
                ));
            }
        }
        System.out.println(
            String.format(
            "%s got %d results in %sns using %d writers and %d readers",
                type, numResults, (System.nanoTime() - start), numWriters, numReaders)
        );
        finished.set(-1);
        service.clear();
        do {
            System.gc();
            if (runningThreads == 0) {
                return;
            }
            Thread.sleep(100);
        } while (Runtime.getRuntime().freeMemory() < (memory * .8 ));
    }

    private void startReaders(BundleService service, int i, AtomicInteger finished) {
        runningThreads++;
        new Thread() {

            @Override
            public void run() {
                while (finished.get() > -1) {
                    final Iterable<Bundle> bundles = service.findBundle(newQuery());
                    finished.incrementAndGet();
                    synchronized (finished) {
                        finished.notifyAll();
                    }
                    Thread.yield();
                }
                runningThreads--;
            }

            {
                setDaemon(true);
                setName("Reader" + i);
                start();
            }
        };
    }

    private Query newQuery() {
        final String uuid;
        if (knownUuids.isEmpty() || Math.random() < .3) {
            uuid = UUID.randomUUID().toString();//
        } else {
            uuid = knownUuids.keySet().iterator().next();
        }
        final Query query = new Query();
        query.device_uuid = uuid;
        query.sensor_type = "temperature";
        query.start_time = System.currentTimeMillis() + skewTime();
        query.end_time = query.start_time + Math.abs(skewTime());
        return query;
    }

    private long skewTime() {
        return (long)(10_000_000 * (0.5 - Math.random()));
    }

    private void startThreads(BundleService service, int i, AtomicInteger finished) {
        while (i-->0) {
            startWriters(service, i, finished);
            startReaders(service, i, finished);
        }

    }
    private void startWriters(BundleService service, int i, AtomicInteger finished) {
        runningThreads++;
        new Thread(){
            @Override
            public void run() {
                while (finished.get() > -1) {
                    service.addBundle(newUuid(), "temperature");
                    Thread.yield();
                }
                runningThreads--;
            }

            {
                setName("Writer"+i);
                setDaemon(true);
                start();
            }
        };
    }

    private Map<String, String> knownUuids = new ConcurrentHashMap<>();

    private String newUuid() {
        final String uuid = UUID.randomUUID().toString();
        knownUuids.put(uuid, uuid);
        return uuid;
    }

    private Bundle newBundle(String uuid, String type) {
        return new Bundle(uuid, type, (int)(Integer.MAX_VALUE * Math.random()));
    }



    public void test() throws MalformedURLException, InterruptedException {

        final URL[] urls = new URL[]{
            new URL("file:/repo/net/wetheinter/xapi-gwt/0.6-SNAPSHOT/xapi-gwt-0.6-SNAPSHOT.jar"),
            new URL("file:/opt/gwt/build/lib/gwt-dev.jar"),
            new URL("file:/opt/gwt/build/lib/gwt-user.jar"),
            new URL("file:/opt/lib/system-property-permtations/src/main/java/"),
            new URL("file:/tmp/colin/"),
        };

        JavacService javac = X_Inject.instance(JavacService.class);
        CompilerService com = javac.getCompilerService();

        final Out2<Integer, URL> res = com.compileFiles(
            com.defaultSettings().setOutputDirectory("/opt/lib/system-property-permtations/src/main/java"),
            "/opt/lib/system-property-permtations/src/main/java"
        );
        System.out.println(res.out1());
        System.out.println(res.out2());
//
        URLClassLoader cl = new URLClassLoader(urls);
        Thread t = new Thread(()->{
            final Class<?> clazz;
            try {
                clazz = cl.loadClass("com.colinalworth.gwt.sysprops.client.SystemPropertyBasedPermutations");
            } catch (Exception e) {
                throw new RuntimeException("Test project not on classpath", e);
            }
            final GwtcService comp = X_Inject.instance(GwtcService.class);
            final GwtcProjectGenerator project = comp.getProject("SysTest");
            final GwtManifest manifest = project.getManifest();
            project.addGwtInherit("com.colinalworth.gwt.sysprops.SystemPropertyBasedPermutations");
            manifest.addDependency("/opt/lib/system-property-permtations/src/main/java");
            manifest.setUseCurrentJvm(true);
            manifest.setObfuscationLevel(ObfuscationLevel.PRETTY);
            manifest.setLogLevel(Type.TRACE);
            manifest.setWarDir("/opt/lib/colin/out");
            manifest.setGenDir("/opt/lib/colin/build");
            manifest.setCleanupMode(CleanupMode.NEVER_DELETE);
            final int result = comp.compile(manifest);
            assertEquals("Compile failed; check previous logs", 0, result);
        });
        t.setContextClassLoader(cl);
        t.start();
        t.join();
    }

}
