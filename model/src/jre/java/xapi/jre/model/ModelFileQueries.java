package xapi.jre.model;

import xapi.model.api.Model;
import xapi.model.api.ModelQuery;
import xapi.model.api.ModelQueryResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ArrayList;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.NoSuchFileException;

/** Synchronous enumeration and decoding. The provider owns scheduling, scope and callbacks. */
final class ModelFileQueries {
  interface Decoder<M extends Model> { M decode(File file) throws IOException; }

  private static final Comparator<File> BY_NAME = Comparator.comparing(File::getName);

  static <M extends Model> ModelQueryResult<M> typed(final File root, final String type,
      final Class<M> modelClass, final ModelQuery<M> query, final boolean guarded,
      final Decoder<M> decoder) throws IOException {
    final ModelQueryResult<M> result = new ModelQueryResult<>(modelClass);
    File directory = bucket(root, query.getNamespace(), type, query.getAncestor());
    rejectInternal(root, directory);
    if (guarded) validateDirectory(root, directory);
    final File[] files = records(directory, query.getCursor(), guarded);
    final int count = Math.min(query.getPageSize(), files.length);
    for (int i = 0; i < count; i++) result.addModel(decoder.decode(files[i]));
    if (count < files.length) result.setCursor(files[count].getName());
    return result;
  }

  static ModelQueryResult<Model> untyped(final File root, final ModelQuery<Model> query,
      final boolean guarded, final Decoder<Model> decoder) throws IOException {
    if (query.getLimit() < 1) throw new IOException("Untyped model query limit must be positive");
    final UntypedModelCursor cursor = UntypedModelCursor.decode(query.getCursor());
    final ModelQueryResult<Model> result = new ModelQueryResult<>(null);
    final File namespace = query.getNamespace().isEmpty() ? root : new File(root, query.getNamespace());
    rejectInternal(root, namespace);
    if (guarded) validateDirectory(root, namespace);
    final File[] types = guarded ? guardedTypes(root, namespace) : namespace.listFiles(file -> file.isDirectory()
        && !isInternal(root, file)
        && !AtomicModelFile.isTemporary(file));
    if (types == null) {
      if (guarded && Files.exists(namespace.toPath(), LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("Unable to enumerate model namespace");
      }
      return result;
    }
    Arrays.sort(types, BY_NAME);
    for (final File type : types) {
      final int compared = cursor == null ? 1 : type.getName().compareTo(cursor.type);
      if (compared < 0) continue;
      if (guarded) validateDirectory(root, type);
      for (final File file : records(type, cursor != null && compared == 0 ? cursor.record : null, guarded)) {
        if (result.getSize() >= query.getLimit()) {
          result.setCursor(UntypedModelCursor.encode(type.getName(), file.getName()));
          return result;
        }
        result.addModel(decoder.decode(file));
      }
    }
    return result;
  }

  static File bucket(final File root, final String namespace, final String type,
      xapi.model.api.ModelKey parent) {
    File directory = namespace.isEmpty() ? root : new File(root, namespace);
    directory = new File(directory, type);
    int depth = 0;
    while (parent != null) {
      if (++depth > 64) throw new IllegalArgumentException("Model ancestor depth exceeds bound");
      directory = new File(directory, parent.getKind());
      if (parent.isComplete()) directory = new File(directory, parent.getId());
      parent = parent.getParent();
    }
    return directory;
  }

  static void validateDirectory(final File root, final File directory) throws IOException {
    final Path base = root.toPath().toAbsolutePath().normalize();
    final Path path = directory.toPath().toAbsolutePath();
    if (!path.equals(path.normalize()) || !path.startsWith(base)
        || path.startsWith(base.resolve(FileModelTransactionStore.INTERNAL_DIRECTORY))) {
      throw new IOException("Query path escapes model scope");
    }
    for (Path part = path; part != null && part.startsWith(base); part = part.getParent()) {
      if (Files.isSymbolicLink(part)) throw new IOException("Query path uses a symbolic link");
      try {
        if (!Files.readAttributes(part, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isDirectory()) {
          throw new IOException("Query parent is not a directory");
        }
      } catch (NoSuchFileException absent) { /* A genuinely absent bucket is an empty query. */ }
    }
  }

  private static void rejectInternal(final File root, final File directory) throws IOException {
    if (directory.toPath().toAbsolutePath().normalize().startsWith(root.toPath().toAbsolutePath().normalize()
        .resolve(FileModelTransactionStore.INTERNAL_DIRECTORY))) throw new IOException("Reserved model metadata directory");
  }

  private static boolean isInternal(final File root, final File file) {
    return file.toPath().toAbsolutePath().normalize().equals(root.toPath().toAbsolutePath().normalize()
        .resolve(FileModelTransactionStore.INTERNAL_DIRECTORY));
  }

  private static File[] guardedTypes(final File root, final File directory) throws IOException {
    final ArrayList<File> result = new ArrayList<>();
    final java.nio.file.DirectoryStream<Path> opened = openDirectory(directory);
    if (opened == null) return new File[0];
    try (java.nio.file.DirectoryStream<Path> entries = opened) {
      for (final Path entry : entries) {
        final File file = entry.toFile();
        if (isInternal(root, file) || AtomicModelFile.isTemporary(file)) continue;
        final BasicFileAttributes attributes = Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (attributes.isSymbolicLink()) throw new IOException("Query type uses a symbolic link");
        if (attributes.isDirectory()) result.add(file);
      }
    }
    return result.toArray(new File[0]);
  }

  private static File[] records(final File directory, final String cursor, final boolean guarded) throws IOException {
    if (guarded) {
      final ArrayList<File> result = new ArrayList<>();
      final java.nio.file.DirectoryStream<Path> opened = openDirectory(directory);
      if (opened == null) return new File[0];
      try (java.nio.file.DirectoryStream<Path> entries = opened) {
        for (final Path entry : entries) {
          final File file = entry.toFile();
          if (AtomicModelFile.isTemporary(file)) continue;
          final BasicFileAttributes attributes = Files.readAttributes(entry, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
          if (attributes.isDirectory()) continue;
          if (!attributes.isRegularFile()) throw new IOException("Query record is not a regular file");
          if (cursor == null || file.getName().compareTo(cursor) >= 0) result.add(file);
        }
      }
      final File[] files = result.toArray(new File[0]); Arrays.sort(files, BY_NAME); return files;
    }
    final File[] files = directory.listFiles(file -> AtomicModelFile.isModelFile(file)
        && (cursor == null || file.getName().compareTo(cursor) >= 0));
    if (files == null) {
      if (guarded && Files.exists(directory.toPath(), LinkOption.NOFOLLOW_LINKS)) {
        throw new IOException("Unable to enumerate model bucket");
      }
      return new File[0];
    }
    Arrays.sort(files, BY_NAME);
    return files;
  }

  private static java.nio.file.DirectoryStream<Path> openDirectory(final File directory) throws IOException {
    try { return Files.newDirectoryStream(directory.toPath()); }
    catch (NoSuchFileException absent) { return null; }
  }
}
