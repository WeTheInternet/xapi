package net.wti.gradle.settings.api;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Encodes logical schema values when they must be used as a single filesystem name.
 *
 * <p>Portable names are left readable. Names which Windows cannot represent, or which
 * begin with the escape marker, are encoded as lowercase UTF-8 hex after {@code ~}.
 * The reserved marker makes the mapping reversible without colliding with an existing
 * portable logical name.</p>
 */
public final class SchemaPathCodec {

    private static final char ESCAPE = '~';
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private SchemaPathCodec() {
    }

    public static String encodeFileName(final CharSequence logicalName) {
        if (logicalName == null) {
            throw new IllegalArgumentException("A schema filesystem name cannot be null");
        }
        final String logical = logicalName.toString();
        if (isPortableFileName(logical) && logical.charAt(0) != ESCAPE) {
            return logical;
        }

        final byte[] bytes = logical.getBytes(StandardCharsets.UTF_8);
        final StringBuilder encoded = new StringBuilder(1 + bytes.length * 2);
        encoded.append(ESCAPE);
        for (byte value : bytes) {
            final int unsigned = value & 0xff;
            encoded.append(HEX[unsigned >>> 4]);
            encoded.append(HEX[unsigned & 0xf]);
        }
        return encoded.toString();
    }

    public static String decodeFileName(final CharSequence fileName) {
        if (fileName == null) {
            throw new IllegalArgumentException("A schema filesystem name cannot be null");
        }
        final String encoded = fileName.toString();
        if (encoded.isEmpty() || encoded.charAt(0) != ESCAPE) {
            return encoded;
        }
        if ((encoded.length() & 1) == 0) {
            throw new IllegalArgumentException("Malformed schema filesystem name " + encoded);
        }

        final ByteArrayOutputStream decoded = new ByteArrayOutputStream((encoded.length() - 1) / 2);
        for (int index = 1; index < encoded.length(); index += 2) {
            final int high = Character.digit(encoded.charAt(index), 16);
            final int low = Character.digit(encoded.charAt(index + 1), 16);
            if (high == -1 || low == -1) {
                throw new IllegalArgumentException("Malformed schema filesystem name " + encoded);
            }
            decoded.write((high << 4) | low);
        }
        return new String(decoded.toByteArray(), StandardCharsets.UTF_8);
    }

    public static boolean isPortableFileName(final CharSequence candidate) {
        if (candidate == null || candidate.length() == 0) {
            return false;
        }

        for (int index = 0; index < candidate.length(); index++) {
            final char character = candidate.charAt(index);
            if (character < 32 || character == '"' || character == '*' || character == '/' ||
                    character == ':' || character == '<' || character == '>' || character == '?' ||
                    character == '\\' || character == '|') {
                return false;
            }
        }

        final char last = candidate.charAt(candidate.length() - 1);
        if (last == '.' || last == ' ') {
            return false;
        }

        final String name = candidate.toString();
        if (".".equals(name) || "..".equals(name)) {
            return false;
        }
        final int extension = name.indexOf('.');
        final String stem = (extension == -1 ? name : name.substring(0, extension)).toUpperCase(Locale.ROOT);
        if ("CON".equals(stem) || "PRN".equals(stem) || "AUX".equals(stem) || "NUL".equals(stem)) {
            return false;
        }
        return !isNumberedDevice(stem, "COM") && !isNumberedDevice(stem, "LPT");
    }

    private static boolean isNumberedDevice(final String stem, final String prefix) {
        return stem.length() == 4 && stem.startsWith(prefix) && stem.charAt(3) >= '1' && stem.charAt(3) <= '9';
    }
}
