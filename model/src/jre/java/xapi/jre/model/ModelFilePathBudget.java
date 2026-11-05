package xapi.jre.model;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/** Conservative pre-publication limits for the default Linux provider on ext4/XFS. */
final class ModelFilePathBudget {
  private static final int COMPONENT_BYTES = 255;
  // Linux PATH_MAX includes the terminating NUL. Java's absolute path does not.
  private static final int ABSOLUTE_PATH_BYTES = 4095;

  private ModelFilePathBudget() {}

  static void validateWrite(final Path target) throws IOException {
    final Path absolute = target.toAbsolutePath().normalize();
    nativeLength(absolute);
    final Path parent = absolute.getParent();
    // Reserve a full legal component for createTempFile's sibling name, instead of
    // assuming the implementation's current random suffix length. This also covers
    // pending, sequence, receipts and hashed revision/identity destinations.
    if (parent == null || nativeLength(parent) + 1 + COMPONENT_BYTES > ABSOLUTE_PATH_BYTES) {
      throw new IOException("Model path leaves insufficient space for an atomic temporary sibling");
    }
  }

  private static int nativeLength(final Path path) throws IOException {
    if (path.getFileSystem() != FileSystems.getDefault()
        || !"file".equals(path.toUri().getScheme())) {
      throw new IOException("Model transaction path budget requires the default Linux file provider");
    }
    // On the supported default Linux provider, URI escapes represent native pathname
    // bytes. Counting decoded escapes handles multibyte names and a non-UTF-8 native
    // encoding without mistaking UTF-16 String.length() for the on-disk byte length.
    String raw = path.toUri().getRawPath();
    if (raw.endsWith("/") && raw.length() > 1) raw = raw.substring(0, raw.length() - 1);
    int total = 0;
    int component = 0;
    for (int i = 0; i < raw.length(); i++) {
      final char next = raw.charAt(i);
      if (next == '/') {
        component = 0;
      } else {
        if (next == '%') {
          if (i + 2 >= raw.length()) throw new IOException("Invalid native model path encoding");
          i += 2;
        } else if (next > 127) {
          throw new IOException("Cannot establish native model path byte length");
        }
        if (++component > COMPONENT_BYTES) throw new IOException("Model path component exceeds the pilot byte limit");
      }
      if (++total > ABSOLUTE_PATH_BYTES) throw new IOException("Model absolute path exceeds the pilot byte limit");
    }
    return total;
  }
}
