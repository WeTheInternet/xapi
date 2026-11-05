package xapi.jre.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Length-delimited, bounded storage format; never Java object deserialization. */
final class ModelTransactionCodec {
  private static final int MAGIC = 0x58545831;

  static byte[] request(final ModelTransaction transaction) throws IOException {
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    final DataOutputStream out = new DataOutputStream(bytes);
    text(out, transaction.protocol());
    text(out, transaction.manifest());
    text(out, transaction.actorScope());
    text(out, transaction.operationId());
    out.writeInt(transaction.mutations().size());
    for (final ModelMutation mutation : transaction.mutations()) {
      text(out, mutation.key().canonicalKey());
      text(out, mutation.key().relativePath());
      out.writeLong(mutation.expectedRevision());
      final byte[] value = mutation.value();
      if (value.length > FileModelTransactionStore.MAX_REQUEST_BYTES) throw new IOException("Transaction too large");
      out.writeInt(value.length);
      out.write(value);
      if (bytes.size() > FileModelTransactionStore.MAX_REQUEST_BYTES) throw new IOException("Transaction too large");
    }
    out.close();
    return bytes.toByteArray();
  }

  static byte[] receipt(final ModelTransaction transaction, final long revision) throws IOException {
    final byte[] request = request(transaction);
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    final DataOutputStream out = new DataOutputStream(bytes);
    out.writeInt(MAGIC);
    out.writeLong(revision);
    out.writeInt(request.length);
    out.write(request);
    text(out, hash(request));
    text(out, hash(bytes.toByteArray()));
    out.close();
    return bytes.toByteArray();
  }

  static ModelTransactionReceipt receipt(final byte[] encoded) throws IOException {
    if (encoded.length > FileModelTransactionStore.MAX_REQUEST_BYTES + 256) throw new IOException("Receipt too large");
    final DataInputStream in = new DataInputStream(new ByteArrayInputStream(encoded));
    if (in.readInt() != MAGIC) throw new IOException("Unsupported transaction storage format");
    final long revision = in.readLong();
    if (revision <= 0) throw new IOException("Invalid transaction revision");
    final byte[] request = bytes(in, FileModelTransactionStore.MAX_REQUEST_BYTES);
    final String fingerprint = text(in, 64);
    final int prefixLength = encoded.length - in.available();
    final String checksum = text(in, 64);
    if (in.available() != 0 || !hash(request).equals(fingerprint)
        || !hash(Arrays.copyOf(encoded, prefixLength)).equals(checksum)) throw new IOException("Corrupt transaction receipt");
    final DataInputStream fields = new DataInputStream(new ByteArrayInputStream(request));
    try {
      final String protocol = text(fields, 1024);
      final String manifest = text(fields, 1024);
      final String actor = text(fields, 4096);
      final String operation = text(fields, 1024);
      final int count = fields.readInt();
      if (count < 1 || count > FileModelTransactionStore.MAX_MUTATIONS) throw new IOException("Invalid child count");
      final List<ModelMutation> mutations = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        final ModelStoreKey key = new ModelStoreKey(text(fields, 32768), text(fields, 16384));
        mutations.add(new ModelMutation(key, fields.readLong(), bytes(fields, FileModelTransactionStore.MAX_REQUEST_BYTES)));
      }
      final ModelTransaction transaction = new ModelTransaction(protocol, manifest, actor, operation, mutations);
      if (fields.available() != 0 || !Arrays.equals(request, request(transaction))) {
        throw new IOException("Noncanonical transaction receipt");
      }
      return new ModelTransactionReceipt(transaction, fingerprint, revision);
    } catch (IllegalArgumentException invalid) {
      throw new IOException("Invalid transaction receipt", invalid);
    }
  }

  static String hash(final byte[] bytes) {
    try {
      final byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      final StringBuilder result = new StringBuilder(64);
      for (final byte value : digest) result.append(String.format("%02x", value & 255));
      return result.toString();
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  static void text(final DataOutputStream out, final String text) throws IOException {
    final byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  static String text(final DataInputStream in, final int limit) throws IOException {
    return new String(bytes(in, limit), StandardCharsets.UTF_8);
  }

  private static byte[] bytes(final DataInputStream in, final int limit) throws IOException {
    final int size = in.readInt();
    if (size < 0 || size > limit || size > in.available()) throw new IOException("Invalid field length");
    final byte[] bytes = new byte[size];
    in.readFully(bytes);
    return bytes;
  }
}
