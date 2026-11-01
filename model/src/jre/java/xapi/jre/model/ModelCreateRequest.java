package xapi.jre.model;

import xapi.dev.source.CharBuffer;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelList;
import xapi.model.service.ModelServiceWithCreateIfAbsent;
import xapi.source.lex.StringCharIterator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
    // Freeze property order, generic metadata and directional rules using the existing manifest codec.
    final String policy = ModelManifest.serialize(new CharBuffer(), selected, service.primitiveSerializer()).toSource();
    manifest = ModelManifest.deserialize(new StringCharIterator(policy), service.primitiveSerializer());
    if (manifest.getModelType() != selected.getModelType()) throw new IllegalArgumentException("Manifest class changed during capture");
    this.clientToServer = clientToServer;
    source = service.serializeCaptured(manifest, model, clientToServer).toSource();
    image = source.getBytes(StandardCharsets.UTF_8);
    if (image.length > ModelServiceWithCreateIfAbsent.MAX_MODEL_BYTES) throw new IOException("Conditional model image exceeds byte limit");
    decode(service, image); // Validate full identity and framing before the write decision.
  }

  <M extends Model> M decode(final AbstractJreModelService service, final byte[] bytes) throws IOException {
    if (bytes.length > ModelServiceWithCreateIfAbsent.MAX_MODEL_BYTES) throw new IOException("Conditional model image exceeds byte limit");
    final StringCharIterator chars = new StringCharIterator(new String(bytes, StandardCharsets.UTF_8));
    final M model = service.deserializeCaptured(manifest, chars, clientToServer);
    if (model == null || chars.hasNext() || !type.equals(model.getType()) || model.getKey() == null
        || !type.equals(model.getKey().getKind())
        || !canonicalKey.equals(service.keyToString(ModelKeySnapshot.complete(model.getKey())))) {
      throw new IOException("Conditional create image changed full model identity or framing");
    }
    return model;
  }
}
