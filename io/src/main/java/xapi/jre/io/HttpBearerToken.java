package xapi.jre.io;

import xapi.fu.Out1;

import java.net.URI;
import java.util.Objects;

/**
 * Supplies an opaque bearer credential for one HTTP request. The owner of the
 * credential controls storage and rotation; this helper never persists or logs it.
 * HTTPS is required unless numeric loopback development use is explicitly enabled.
 */
public final class HttpBearerToken {
    private final Out1<String> token;
    private final boolean allowLoopback;

    public HttpBearerToken(final Out1<String> token) {
        this(token, false);
    }

    private HttpBearerToken(final Out1<String> token, final boolean allowLoopback) {
        this.token = Objects.requireNonNull(token, "credential provider");
        this.allowLoopback = allowLoopback;
    }

    /** Only literal 127.0.0.1 and ::1 HTTP endpoints qualify; no DNS is performed. */
    public static HttpBearerToken forLoopbackDevelopment(final Out1<String> token) {
        return new HttpBearerToken(token, true);
    }

    /** Checks transport before the provider is consulted or a connection is opened. */
    public void validateEndpoint(final URI endpoint) {
        final String host = endpoint.getHost();
        final boolean loopback = "127.0.0.1".equals(host) || "::1".equals(host) || "[::1]".equals(host);
        if (host == null || endpoint.getUserInfo() != null || endpoint.getQuery() != null
                || endpoint.getFragment() != null || !("https".equalsIgnoreCase(endpoint.getScheme())
                || allowLoopback && loopback && "http".equalsIgnoreCase(endpoint.getScheme()))) {
            throw new IllegalArgumentException("Bearer credentials require HTTPS or explicit numeric loopback development");
        }
    }

    /**
     * Resolves a fresh credential for each request. Errors deliberately omit the
     * supplied value and provider exception, either of which may contain secrets.
     * Consumers must not forward the resulting header across redirects.
     */
    public String authorizationHeader(final URI endpoint) {
        validateEndpoint(endpoint);
        final String value;
        try {
            value = token.out1();
        } catch (final RuntimeException unavailable) {
            throw new IllegalStateException("HTTP credential unavailable");
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("HTTP credential unavailable");
        }
        boolean padding = false;
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c == '=') {
                padding = true;
            } else if (padding || !(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                    || c >= '0' && c <= '9' || c == '-' || c == '.' || c == '_'
                    || c == '~' || c == '+' || c == '/')) {
                throw new IllegalArgumentException("Invalid HTTP bearer credential");
            }
        }
        if (value.charAt(0) == '=') throw new IllegalArgumentException("Invalid HTTP bearer credential");
        return "Bearer " + value;
    }
}
