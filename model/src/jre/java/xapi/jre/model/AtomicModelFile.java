package xapi.jre.model;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;

/** Linux single-record replacement, deliberately separate from multi-record transactions. */
class AtomicModelFile {

  static final String TEMP_PREFIX = ".xapi-write-";
  static final String TEMP_SUFFIX = ".tmp";

  static boolean isTemporary(File file) {
    return file.getName().startsWith(TEMP_PREFIX) && file.getName().endsWith(TEMP_SUFFIX);
  }

  static boolean isModelFile(File file) {
    return file.isFile() && !isTemporary(file);
  }

  void replace(Path destination, byte[] serialized) throws IOException {
    Path target = destination.toAbsolutePath();
    if (isTemporary(target.toFile())) {
      throw new IOException("Reserved model temporary-file name: " + target);
    }
    ensureDirectory(target.getParent());
    PosixFileAttributes previous = null;
    if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
      previous = Files.readAttributes(target, PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      if (!previous.isRegularFile()) {
        throw new IOException("Model target must be a regular file: " + target);
      }
    }
    Path temporary = Files.createTempFile(target.getParent(), TEMP_PREFIX, TEMP_SUFFIX);
    boolean replacementAttempted = false;
    IOException reportedFailure = null;
    try {
      if (previous != null) {
        PosixFileAttributeView attributes = Files.getFileAttributeView(temporary, PosixFileAttributeView.class);
        attributes.setOwner(previous.owner());
        attributes.setGroup(previous.group());
        attributes.setPermissions(previous.permissions());
      }
      try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
        write(channel, serialized);
        forceFile(channel);
      }
      beforeReplace(temporary, target);
      replacementAttempted = true;
      try {
        atomicReplace(temporary, target);
      } catch (AtomicMoveNotSupportedException unsupported) {
        replacementAttempted = false;
        throw unsupported;
      }
      forceDirectory(target.getParent());
    } catch (IOException failure) {
      reportedFailure = replacementAttempted ? new ModelPersistenceException(target, failure) : failure;
      throw reportedFailure;
    } finally {
      // Never delete another writer's temporary file. Crash remnants stay query-invisible.
      try {
        Files.deleteIfExists(temporary);
      } catch (IOException cleanupFailure) {
        if (reportedFailure != null) {
          reportedFailure.addSuppressed(cleanupFailure);
        } else {
          throw new ModelPersistenceException(target, cleanupFailure);
        }
      }
    }
  }

  void write(FileChannel channel, byte[] serialized) throws IOException {
    ByteBuffer bytes = ByteBuffer.wrap(serialized);
    while (bytes.hasRemaining()) {
      channel.write(bytes);
    }
  }

  void forceFile(FileChannel channel) throws IOException {
    channel.force(true);
  }

  void beforeReplace(Path temporary, Path target) throws IOException {
  }

  void atomicReplace(Path temporary, Path target) throws IOException {
    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }

  void forceDirectory(Path directory) throws IOException {
    try (FileChannel channel = FileChannel.open(directory, StandardOpenOption.READ)) {
      channel.force(true);
    }
  }

  private void ensureDirectory(Path directory) throws IOException {
    Files.createDirectories(directory);
    // Also covers directories left present by an earlier failed creation/force attempt.
    // Synchronizing only the leaf cannot durably establish a new ancestor's name.
    for (Path ancestor = directory; ancestor != null; ancestor = ancestor.getParent()) {
      forceDirectory(ancestor);
    }
    if (!Files.isDirectory(directory)) {
      throw new IOException("Model parent is not a directory: " + directory);
    }
  }
}
