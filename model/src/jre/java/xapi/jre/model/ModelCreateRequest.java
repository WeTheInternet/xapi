package xapi.jre.model;

import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelList;
import xapi.model.service.ModelServiceWithCreateIfAbsent;
import xapi.source.lex.StringCharIterator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.nio.ByteBuffer;

/** Immutable policy/identity/image captured before either file or HTTP conditional-create work. */
final class ModelCreateRequest {
  final ModelManifest manifest;
  final ModelKey key;
  final String canonicalKey;
  final String type;
  final String source;
  final byte[] image;
  final boolean clientToServer;

  ModelCreateRequest(final AbstractJreModelService service, final ModelManifest registered,
      final ModelManifest selected, final Model model, final boolean clientToServer) throws IOException {
    if (selected == null || selected != registered || model == null
        || !selected.getType().equals(model.getType()) || !selected.getModelType().isInstance(model)) {
      throw new IllegalArgumentException("Conditional create requires the provider's registered manifest and model type");
    }
    if (ModelList.class.isAssignableFrom(selected.getModelType())) {
      throw new UnsupportedOperationException("Conditional creation requires a concrete root model, not a root ModelList");
    }
    key = ModelKeySnapshot.complete(model.getKey());
    type = selected.getType();
    if (!type.equals(key.getKind())) throw new IllegalArgumentException("Model type does not match its complete key");
    canonicalKey = service.keyToString(key);
    // Preserve the complete root declaration without a wire round-trip.
    manifest = freezeManifest(service, registered, selected);
    this.clientToServer = clientToServer;
    source = service.serializeCaptured(manifest, model, clientToServer).toSource();
    image = source.getBytes(StandardCharsets.UTF_8);
    if (image.length > ModelServiceWithCreateIfAbsent.MAX_MODEL_BYTES) throw new IOException("Conditional model image exceeds byte limit");
    decode(service, image); // Validate full identity and framing before the write decision.
  }

  <M extends Model> M decode(final AbstractJreModelService service, final byte[] bytes) throws IOException {
    return decode(service, bytes, clientToServer);
  }

  /** Stored bytes were serialized by this provider; decode as their receiving peer, not an HTTP response. */
  <M extends Model> M decodeFileImage(final AbstractJreModelService service, final byte[] bytes) throws IOException {
    return decode(service, bytes, !clientToServer);
  }

  private <M extends Model> M decode(final AbstractJreModelService service, final byte[] bytes,
      final boolean receivingDirection) throws IOException {
    return decodeImage(service, manifest, type, canonicalKey, bytes, receivingDirection);
  }

  static ModelManifest freezeManifest(final AbstractJreModelService service, final ModelManifest registered,
      final ModelManifest selected) {
    if (selected == null || selected != registered) throw new IllegalArgumentException("Expected registered model manifest");
    return selected.copy();
  }

  static <M extends Model> M decodeImage(final AbstractJreModelService service, final ModelManifest manifest,
      final String type, final String canonicalKey, final byte[] bytes, final boolean receivingDirection) throws IOException {
    if (bytes.length > ModelServiceWithCreateIfAbsent.MAX_MODEL_BYTES) throw new IOException("Conditional model image exceeds byte limit");
    final String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    final StringCharIterator chars = new StringCharIterator(text);
    final M model = service.deserializeCaptured(manifest, chars, receivingDirection);
    if (model == null || chars.hasNext() || !type.equals(model.getType()) || model.getKey() == null
        || !type.equals(model.getKey().getKind())
        || !canonicalKey.equals(service.keyToString(ModelKeySnapshot.complete(model.getKey())))) {
      throw new IOException("Conditional create image changed full model identity or framing");
    }
    return model;
  }
}
