package xapi.jre.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Opaque ordered (type, first-unreturned record) cursor for flat untyped file queries. */
final class UntypedModelCursor {
  private static final String PREFIX = "uq1:";
  private static final int MAX_CURSOR_CHARS = 16384;
  final String type;
  final String record;

  private UntypedModelCursor(final String type, final String record) {
    this.type = type;
    this.record = record;
  }

  static UntypedModelCursor decode(final String cursor) throws IOException {
    if (cursor == null) return null;
    if (!cursor.startsWith(PREFIX)) {
      throw new IOException("Unsupported untyped model cursor; explicitly restart without a cursor");
    }
    if (cursor.length() > MAX_CURSOR_CHARS) throw new IOException("Untyped model cursor exceeds size bound");
    final String[] parts = cursor.substring(PREFIX.length()).split(":", -1);
    if (parts.length != 2) throw new IOException("Malformed untyped model cursor");
    return new UntypedModelCursor(part(parts[0]), part(parts[1]));
  }

  static String encode(final String type, final String record) throws IOException {
    validate(type); validate(record);
    return PREFIX + encodePart(type) + ":" + encodePart(record);
  }

  private static String encodePart(final String value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String part(final String encoded) throws IOException {
    try {
      final byte[] bytes = Base64.getUrlDecoder().decode(encoded);
      if (bytes.length > 4096) throw new IOException("Untyped model cursor component exceeds size bound");
      final String value = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
      validate(value);
      if (!encoded.equals(encodePart(value))) throw new IOException("Noncanonical untyped model cursor");
      return value;
    } catch (IllegalArgumentException | CharacterCodingException malformed) {
      throw new IOException("Malformed untyped model cursor", malformed);
    }
  }

  private static void validate(final String value) throws IOException {
    if (value.isEmpty() || ".".equals(value) || "..".equals(value) || value.indexOf('/') >= 0
        || value.indexOf('\0') >= 0
        || !StandardCharsets.UTF_8.newEncoder().canEncode(value) || value.getBytes(StandardCharsets.UTF_8).length > 4096) {
      throw new IOException("Invalid untyped model cursor component");
    }
  }
}
