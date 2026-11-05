package xapi.test.io;

import org.junit.Test;
import xapi.jre.io.HttpBearerToken;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class HttpBearerTokenTest {
    private final URI secure = URI.create("https://example.test/model");

    @Test public void resolvesCurrentCredentialWithoutCachingOrEchoingSecrets() {
        final AtomicReference<String> current = new AtomicReference<>("first-test-token");
        final HttpBearerToken token = new HttpBearerToken(current::get);
        assertEquals("Bearer first-test-token", token.authorizationHeader(secure));
        current.set("second.test_token+/==");
        assertEquals("Bearer second.test_token+/==", token.authorizationHeader(secure));
        assertFalse(token.toString().contains(current.get()));
    }

    @Test public void rejectsPlaintextAndAmbiguousEndpointsBeforeResolvingCredential() {
        final AtomicInteger consulted = new AtomicInteger();
        final HttpBearerToken token = new HttpBearerToken(() -> { consulted.incrementAndGet(); return "test-token"; });
        for (final String url : new String[]{"http://127.0.0.1/model", "http://192.168.1.2/model",
                "https://person:secret@example.test/model", "https://example.test/model?token=x",
                "https://example.test/model#fragment", "file:///model"}) {
            reject(() -> token.authorizationHeader(URI.create(url)));
        }
        assertEquals(0, consulted.get());
    }

    @Test public void developmentOptInAcceptsOnlyExactNumericLoopback() {
        final HttpBearerToken token = HttpBearerToken.forLoopbackDevelopment(() -> "test-token");
        assertEquals("Bearer test-token", token.authorizationHeader(URI.create("http://127.0.0.1:1234/model")));
        assertEquals("Bearer test-token", token.authorizationHeader(URI.create("http://[::1]:1234/model")));
        for (final String host : new String[]{"localhost", "127.0.0.2", "127.1", "127.0.0.1.example.test", "192.168.1.2"}) {
            reject(() -> token.authorizationHeader(URI.create("http://" + host + "/model")));
        }
    }

    @Test public void invalidOrUnavailableCredentialsFailWithoutIncludingValueOrCause() {
        for (final String value : new String[]{null, "", "secret token", "secret\r\nInjected: value", "=secret", "secret=padding", "secret\u2603"}) {
            final HttpBearerToken token = new HttpBearerToken(() -> value);
            final RuntimeException failure = reject(() -> token.authorizationHeader(secure));
            if (value != null && !value.isEmpty()) assertFalse(failure.toString().contains(value));
            assertNull(failure.getCause());
        }
        final RuntimeException failure = reject(() -> new HttpBearerToken(() -> {
            throw new IllegalStateException("private-test-secret");
        }).authorizationHeader(secure));
        assertFalse(failure.toString().contains("private-test-secret"));
        assertNull(failure.getCause());
    }

    private static RuntimeException reject(final Runnable action) {
        try { action.run(); fail("Expected rejection"); return null; }
        catch (final RuntimeException expected) { return expected; }
    }
}
