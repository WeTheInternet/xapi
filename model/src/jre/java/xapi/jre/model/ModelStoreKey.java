package xapi.jre.model;

import java.util.Objects;

/** A complete canonical model identity and its existing relative file location. */
public final class ModelStoreKey {
  private final String canonicalKey;
  private final String relativePath;

  public ModelStoreKey(final String canonicalKey, final String relativePath) {
    this.canonicalKey = text(canonicalKey, 8192, "canonical key");
    this.relativePath = text(relativePath, 4096, "relative path");
    for (final String segment : relativePath.split("/", -1)) {
      if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)
          || segment.indexOf('\\') >= 0 || segment.indexOf('\0') >= 0
          || segment.startsWith(AtomicModelFile.TEMP_PREFIX) && segment.endsWith(AtomicModelFile.TEMP_SUFFIX)) {
        throw new IllegalArgumentException("Invalid model path");
      }
    }
    if (relativePath.equals(FileModelTransactionStore.INTERNAL_DIRECTORY)
        || relativePath.startsWith(FileModelTransactionStore.INTERNAL_DIRECTORY + "/")) {
      throw new IllegalArgumentException("Reserved model path");
    }
  }

  static String text(final String value, final int limit, final String label) {
    Objects.requireNonNull(value, label);
    if (value.isEmpty() || value.length() > limit || value.indexOf('\0') >= 0
        || !java.nio.charset.StandardCharsets.UTF_8.newEncoder().canEncode(value)) {
      throw new IllegalArgumentException("Invalid " + label);
    }
    return value;
  }

  public String canonicalKey() { return canonicalKey; }
  public String relativePath() { return relativePath; }
}
