/**
 *
 */
package xapi.model.api;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.dev.source.CharBuffer;
import xapi.inject.X_Inject;
import xapi.model.X_Model;
import xapi.model.impl.ClusteringPrimitiveDeserializer;
import xapi.model.impl.ClusteringPrimitiveSerializer;
import xapi.source.api.CharIterator;
import xapi.source.impl.StringCharIterator;
import xapi.util.X_Debug;
import xapi.util.api.Digester;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * This class represents a module of model types; it contains the {@link ModelManifest}s for all types within this module.
 * In practice, this is used for servers to be able keep in sync with multiple clients; by having the client transmit
 * the strong hash of the ModelModule, the server can load the correct serialization policy to be able to understand
 * any number of clients.
 * <p>
 * Whenever the server loads the ModelModule, it should save a copy into its datastore, so that, if updated,
 * clients who present a previous strong hash can still function correctly.  The server will generally be deployed
 * with a module for each client compilation, which it can save while it is still on the classpath; then, should a
 * new version be deployed, the policy on the classpath will be stale, but can be loaded from the datastore.
 * <p>
 * Using this technique will allow the server to understand version-skewed clients; however, care must be taken
 * that a breaking change across versions will not lead to server errors.  Using generic .getAllProperties() methods
 * will help avoid interface-based changes, but there will still be a problem when an incompatible change is made.
 * <p>
 * To mitigate this, we will (in the future) first create the ability to invalidate serialization policies that
 * are too old; this can be done with an annatation, @BreakingChange, which signals to the serialization policy
 * builder that the given type cannot be safely used before the current version.  This annotaiton will accept
 * an optional hash key to state that "all versions at or before the specified key should be invalidated".
 * <p>
 * Once we are able to safely prevent breaking changes from causing server errors (we can just send back an
 * error message telling the client to update), then we will build a @MigrationStrategy, which will include
 * instructions on how to transform a stale model into an acceptable format.  This annotation will point to
 * helper classes that exist which can transform a given model.
 * <p>
 * Using a migration strategy will easily facilitate simple field renames or removals, but for more complex situations
 * like adding a field that must be populated from external sources, the {@link ModelMigration} interface will take
 * an extra context variable (like an HttpSession) to help the server fill in any data that is not within scope.
 * <p>
 * When it is not possible to migrate a field, then a @BreakingChange should be used to force the client to update.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelModule implements Serializable {

  private static final long serialVersionUID = 977462783892289984L;

  private final StringTo<ModelManifest> manifests;
  private final IntTo<String> strongNames;
  private String uuid;
  private String moduleName;
  private transient String serialized;

  public ModelModule() {
    manifests = X_Collect.newStringMap(ModelManifest.class);
    strongNames = X_Collect.newList(String.class);
  }

  public ModelModule addManifest(final ModelManifest manifest) {
    manifests.put(manifest.getType(), manifest);
    return this;
  }

  public ModelModule addStrongName(final String strongName) {
    strongNames.add(strongName);
    return this;
  }

  public ModelManifest getManifest(final String modelType) {
    return manifests.get(modelType);
  }

  public String[] getStrongNames() {
    return strongNames.toArray();
  }
  /**
   * @return -> uuid
   */
  public String getUuid() {
    if (uuid == null) {
      uuid = computeUuid(X_Inject.instance(PrimitiveSerializer.class));
    }
    return uuid;
  }

  protected String computeUuid(final PrimitiveSerializer primitives) {
    // Compute the checksum of the policy itself.  That checksum will become our UUID,
    // which will then be appended before the policy itself.
    final String result = calculateSerialization(primitives);
    final Digester digest = X_Inject.instance(Digester.class);
    byte[] asBytes;
    try {
      asBytes = result.getBytes("UTF-8");
    } catch (final UnsupportedEncodingException e) {
      throw X_Debug.rethrow(e);
    }
    final byte[] uuid = digest.digest(asBytes);
    return digest.toString(uuid);
  }

  /**
   * @param uuid -> set uuid
   */
  public void setUuid(final String uuid) {
    this.uuid = uuid;
  }

  public static String serialize(final ModelModule module) {
    final CharBuffer buffer = new CharBuffer();
    serialize(buffer, module, X_Inject.instance(PrimitiveSerializer.class));
    return buffer.toString();
  }

  public static CharBuffer serialize(final CharBuffer into, final ModelModule module, final PrimitiveSerializer primitives) {
    // Append the uuid, as a string (using a leading size for deserialization purposes)
    into.append(primitives.serializeString(module.getUuid()));
    into.append(primitives.serializeInt(module.strongNames.size()));
    for (final String strongName : module.strongNames.forEach()) {
      into.append(primitives.serializeString(strongName));
    }
    into.append(module.calculateSerialization(primitives));

    return into;
  }

  protected String calculateSerialization(PrimitiveSerializer primitives) {
    if (serialized != null) {
      return serialized;
    }
    // We will build our policy in our own buffer, so we can safely use it to calculate our strong hash later
    final CharBuffer policy = new CharBuffer();

    policy.append(primitives.serializeString(getModuleName()));

    primitives = new ClusteringPrimitiveSerializer(primitives, policy);
    // Directly append the policy to the result (the string is not wrapped),
    // however, we do append the size of the manifests so we know when to stop deserializing
    policy.append(primitives.serializeInt(manifests.size()));
    for (final ModelManifest manifest : manifests.values()) {
      // TODO: collect up reused strings like classnames, and append them into a "classpool",
      // to reduce the total size of the policy
      ModelManifest.serialize(policy, manifest, primitives);
    }

    serialized = policy.toString();
    return serialized;
  }

  public static ModelModule deserialize(final String chars) {
    final StringCharIterator iter = new StringCharIterator(chars);
    return deserialize(iter, X_Inject.instance(PrimitiveSerializer.class));
  }

  public static ModelModule deserialize(final CharIterator chars, PrimitiveSerializer primitives) {
    final ModelModule module = new ModelModule();
    module.setUuid(primitives.deserializeString(chars));
    int numStrongNames = primitives.deserializeInt(chars);
    while (numStrongNames --> 0) {
      module.strongNames.add(primitives.deserializeString(chars));
    }
    module.setModuleName(primitives.deserializeString(chars));
    primitives = new ClusteringPrimitiveDeserializer(primitives, chars) {
      @Override
      @SuppressWarnings("unchecked")
      public <C> Class<C> deserializeClass(final CharIterator c) {
        // might be able to avoid this eager register() call...
        final Class<C> cls = super.deserializeClass(c);
        if (Model.class.isAssignableFrom(cls)) {
          X_Model.getService().register(Class.class.cast(cls));
        } else if (cls.isArray()) {
          Class<?> component = cls.getComponentType();
          while (component.isArray()) {
            component = cls.getComponentType();
          }
          if (Model.class.isAssignableFrom(component)) {
            X_Model.getService().register(Class.class.cast(component));
          }
        }
        return cls;
      }
    };
    int manifests = primitives.deserializeInt(chars);
    while (manifests --> 0) {
      final ModelManifest manifest = ModelManifest.deserialize(chars, primitives);
      module.manifests.put(manifest.getType(), manifest);
    }
    return module;
  }

  @Override
  public int hashCode() {
    return getUuid().hashCode();
  }

  /**
   * This implementation is EXTREMELY INEFFICIENT, and should only be used for unit tests;
   * this object should not be treated as a map key, but if you absolutely must, you should
   * consider using a mapping structure that allows you to provide a more efficient equality check.
   */
  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof ModelModule) {
      final ModelModule other = (ModelModule) obj;
      if (manifests.size() != other.manifests.size()) {
        return false;
      }
      if (!Objects.equals(moduleName, other.moduleName)) {
        return false;
      }
      if (!Objects.equals(getUuid(), other.getUuid())) {
        return false;
      }
      for (final Entry<String, ModelManifest> entry : manifests.entries()) {
        final ModelManifest compare = other.manifests.get(entry.getKey());
        if (!Objects.equals(compare, entry.getValue())) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * @return -> moduleName
   */
  public String getModuleName() {
    return moduleName;
  }

  /**
   * @param moduleName -> set moduleName
   */
  public void setModuleName(final String moduleName) {
    this.moduleName = moduleName;
  }
}
