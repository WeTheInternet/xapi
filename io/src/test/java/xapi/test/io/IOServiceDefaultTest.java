package xapi.test.io;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import com.sun.net.httpserver.HttpServer;
import xapi.io.api.DelegatingIOCallback;
import xapi.io.api.IOCallback;
import xapi.io.api.IOMessage;
import xapi.io.api.IORequest;
import xapi.io.api.LineReader;
import xapi.io.api.RequestFailed;
import xapi.jre.io.IOServiceDefault;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class IOServiceDefaultTest {

    private static final String COLLECTIONS = "xinject.singletons.xapi.collect.service.CollectionService";
    private static final String TIME = "xinject.singletons.xapi.time.service.TimeService";
    private static String previousCollections;
    private static String previousTime;

    @BeforeClass
    public static void configureJreProviders() {
        // io:main deliberately has no platform injector/scanner. Select its existing
        // Java providers explicitly for this HTTP integration test.
        previousCollections = System.setProperty(COLLECTIONS, "xapi.collect.impl.CollectionServiceDefault");
        previousTime = System.setProperty(TIME, "xapi.time.impl.TimeServiceDefault");
    }

    @AfterClass
    public static void restoreProviderProperties() {
        restore(COLLECTIONS, previousCollections);
        restore(TIME, previousTime);
    }

    private static void restore(final String key, final String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private static class ReaderService extends IOServiceDefault {
        String read(final ByteArrayInputStream input, final IOCallback<IOMessage<String>> callback) throws IOException {
            return drainInput(input, callback);
        }
    }

    private static class Lines extends DelegatingIOCallback<IOMessage<String>> implements LineReader {
        final List<String> lines = new ArrayList<>();
        boolean started;
        boolean ended;

        Lines() {
            super(message -> {});
        }

        @Override
        public void onStart() {
            started = true;
        }

        @Override
        public void onLine(final String line) {
            assertTrue(started);
            lines.add(line);
        }

        @Override
        public void onEnd() {
            ended = true;
        }
    }

    @Test
    public void preservesUtf8AndEveryLineEnding() throws IOException {
        final String text = "caf\u00e9 \u65e5\u672c\u8a9e \ud83c\udf31\r\nsecond\n\nthird\rfourth\r\n";
        final ByteArrayInputStream input = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        assertEquals(text, new ReaderService().read(input, new DelegatingIOCallback<>(message -> {})));
    }

    @Test
    public void lineCallbacksKeepStrippedLinesWithoutChangingResponse() throws IOException {
        final String text = "one\r\ntwo\n\nthree\rfour";
        final Lines callback = new Lines();
        assertEquals(text, new ReaderService().read(
                new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), callback));
        assertEquals(Arrays.asList("one", "two", "", "three", "four"), callback.lines);
        assertTrue(callback.ended);
    }

    @Test
    public void preservesBodiesLargerThanReaderBufferAndEmptyBodies() throws IOException {
        final StringBuilder text = new StringBuilder();
        for (int i = 0; i < 9000; i++) {
            text.append("\u00e9\r\n");
        }
        final ReaderService service = new ReaderService();
        assertEquals(text.toString(), service.read(new ByteArrayInputStream(
                text.toString().getBytes(StandardCharsets.UTF_8)), new DelegatingIOCallback<>(message -> {})));
        assertEquals("", service.read(new ByteArrayInputStream(new byte[0]), new DelegatingIOCallback<>(message -> {})));
    }

    @Test
    public void httpErrorsKeepStatusAndExactBodyInOneFailureCallback() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        final String body = "missing caf\u00e9\r\nwith details\n";
        server.createContext("/", exchange -> {
            final int status = Integer.parseInt(exchange.getRequestURI().getPath().substring(1));
            final byte[] bytes = status == 501 ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            final CompletableFuture<IOMessage<String>> successful = new CompletableFuture<>();
            final IORequest<String> ok = new IOServiceDefault().get(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/200", null,
                    new DelegatingIOCallback<>(successful::complete, successful::completeExceptionally));
            ok.response();
            assertEquals(body, successful.get(5, TimeUnit.SECONDS).body());
            assertTrue(ok.isSuccess());
            assertFalse(ok.isPending());
            for (final int status : new int[]{404, 500, 501, 503}) {
                final AtomicInteger success = new AtomicInteger();
                final AtomicInteger errors = new AtomicInteger();
                final CompletableFuture<Throwable> result = new CompletableFuture<>();
                final IORequest<String> request = new IOServiceDefault().post("http://127.0.0.1:" + server.getAddress().getPort() + "/" + status,
                        "request", null, new DelegatingIOCallback<>(message -> success.incrementAndGet(), failure -> {
                            errors.incrementAndGet();
                            result.complete(failure);
                        }));
                request.response();
                final Throwable failure = result.get(5, TimeUnit.SECONDS);
                if (!(failure instanceof RequestFailed)) {
                    throw new AssertionError("Expected an HTTP response failure", failure);
                }
                final IOMessage<String> response = ((RequestFailed) failure).getResponse();
                assertEquals(status, response.statusCode());
                assertEquals(status == 501 ? "" : body, response.body());
                assertFalse(request.isSuccess());
                assertFalse(request.isPending());
                assertEquals(0, success.get());
                assertEquals(1, errors.get());
            }
        } finally {
            server.stop(0);
        }
    }
}
