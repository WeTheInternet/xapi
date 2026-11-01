package xapi.jre.model;

import xapi.model.api.ModelKey;
import xapi.model.impl.ModelKeyDefault;

import java.io.IOException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Capture all identity fields before a wire codec can normalize inconsistent child namespaces. */
final class ModelKeySnapshot {
  static ModelKey complete(final ModelKey original) throws IOException {
    if (original == null) throw new IOException("Missing model identity");
    final Set<ModelKey> seen = Collections.newSetFromMap(new IdentityHashMap<ModelKey, Boolean>());
    ModelKey captured = null, previous = null;
    String namespace = null;
    int depth = 0;
    for (ModelKey part = original; part != null;) {
      if (++depth > 64 || !seen.add(part)) throw new IOException("Model ancestor depth or cycle is invalid");
      final String owner = part.getNamespace(), kind = part.getKind(), id = part.getId();
      final int keyType = part.getKeyType();
      final ModelKey parent = part.getParent();
      if (!part.isComplete() || owner == null || kind == null || kind.isEmpty() || id == null || id.isEmpty()
          || namespace != null && !namespace.equals(owner)) throw new IOException("Incomplete or inconsistent model identity");
      namespace = owner;
      final ModelKey copy = new ModelKeyDefault(owner, kind, id).setKeyType(keyType);
      if (captured == null) captured = copy; else previous.setParent(copy);
      previous = copy; part = parent;
    }
    return captured;
  }
}
