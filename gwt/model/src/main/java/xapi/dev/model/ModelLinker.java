/**
 *
 */
package xapi.dev.model;

import xapi.dev.gwt.linker.StrongNameArtifact;
import xapi.dev.model.ModelField.ActionMethod;
import xapi.dev.model.ModelField.GetterMethod;
import xapi.dev.model.ModelField.SetterMethod;
import xapi.dev.source.CharBuffer;
import xapi.inject.X_Inject;
import xapi.model.api.ModelMethodType;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.impl.ClusteringPrimitiveSerializer;
import xapi.util.api.Digester;
import xapi.util.api.ValidatesValue;

import java.nio.charset.Charset;
import java.util.Set;
import java.util.SortedSet;

import static com.google.gwt.core.ext.TreeLogger.Type.WARN;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.EmittedArtifact.Visibility;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.LinkerOrder.Order;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.core.ext.linker.SyntheticArtifact;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@LinkerOrder(Order.POST)
@Shardable
public class ModelLinker extends Linker {

  @Override
  public String getDescription() {
    return "Emit a ModelModule to enable deserialization of client Models";
  }

  @Override
  public ArtifactSet link(final TreeLogger logger, final LinkerContext context, ArtifactSet artifacts, final boolean onePermutation)
      throws UnableToCompleteException {
    if (!onePermutation) {

      SortedSet<StrongNameArtifact> all = artifacts.find(StrongNameArtifact.class);
      if (all.isEmpty()) {
        logger.log(WARN, "No strongname artifacts found!");
        return artifacts;
      }
      final StrongNameArtifact strongNames = all.first();
      final SortedSet<ModelArtifact> models = artifacts.find(ModelArtifact.class);
      final CharBuffer out = new CharBuffer();
      final PrimitiveSerializer primitives = X_Inject.instance(PrimitiveSerializer.class);
      // Write the name of the Gwt module, which becomes the module name of the ModelModule
      // that we are serializing.

      out.append(primitives.serializeString(context.getModuleName()));

      final ClusteringPrimitiveSerializer clustering = new ClusteringPrimitiveSerializer(primitives, out);

      // Write the number of models to serialize, as we do in the ModelModule serialize() method
      out.append(clustering.serializeInt(models.size()));
      for (final ModelArtifact model : models) {
        out.append(clustering.serializeString(model.getTypeName()));
        out.append(clustering.serializeString(model.getTypeClass()));
        final String[] fieldNames = model.getProperties();
        out.append(clustering.serializeInt(fieldNames.length));
        for (final String fieldName : fieldNames) {
          out.append(clustering.serializeString(fieldName));
        }
        for (final String fieldName : fieldNames) {
          final ModelField field = model.fieldMap.fields.get(fieldName);
          out.append(clustering.serializeString(field.getName()));
          out.append(clustering.serializeString(field.getType()));
          out.append(clustering.serializeBooleanArray(
              field.isC2sEnabled(),
              field.isS2cEnabled(),
              field.isC2sEncrypted(),
              field.isS2cEncrypted(),
              field.isObfuscated()
              ));

          out.append(clustering.serializeInt(field.getC2sSerializer().ordinal()));
          out.append(clustering.serializeInt(field.getS2cSerializer().ordinal()));

          if (field.getPersistenceStrategy() == null) {
            out.append(clustering.serializeInt(-1));
          } else {
            out.append(clustering.serializeInt(field.getPersistenceStrategy().ordinal()));
          }

          out.append(clustering.serializeInt(field.getValidators().length));
          for (final Class<? extends ValidatesValue<?>> validator : field.getValidators()) {
            out.append(clustering.serializeString(validator.getName()));
          }

          final CharBuffer sizeBuffer = new CharBuffer();
          out.addToEnd(sizeBuffer);
          int numMethods = 0;
          for (final GetterMethod getter : field.getGetters()) {
            out.append(clustering.serializeInt(ModelMethodType.GET.ordinal()));
            if (ModelMethodType.GET.isDefaultName(getter.methodName, field.getName())) {
              out.append(clustering.serializeString(null));
            } else {
              out.append(clustering.serializeString(getter.methodName));
            }
            numMethods++;
          }
          for (final SetterMethod setter : field.getSetters()) {
            out.append(clustering.serializeInt(ModelMethodType.SET.ordinal()));
            if (ModelMethodType.SET.isDefaultName(setter.methodName, field.getName())) {
              out.append(clustering.serializeString(null));
            } else {
              out.append(clustering.serializeString(setter.methodName));
            }
            numMethods++;
          }
          for (final ActionMethod action : field.getActions()) {
            out.append(clustering.serializeInt(ModelMethodType.REMOVE.ordinal()));
            if (ModelMethodType.REMOVE.isDefaultName(action.methodName, field.getName())) {
              out.append(clustering.serializeString(null));
            } else {
              out.append(clustering.serializeString(action.methodName));
            }
            numMethods++;
          }
          sizeBuffer.append(clustering.serializeInt(numMethods));
        }
      }
      // Next, compute the strong hash for this serialization policy.
      final String policy = out.toSource();
      final Charset utf8 = Charset.forName("UTF-8");
      byte[] result = policy.getBytes(utf8);
      final Digester digest = X_Inject.instance(Digester.class);
      final String uuid = digest.toString(digest.digest(result));

      // Save the strong hash at the beginning of the policy
      out.clear();
      out.append(primitives.serializeString(uuid));
      final Set<String> names = strongNames.getStrongNames();
      out.append(primitives.serializeInt(names.size()));
      for (final String name : names) {
        out.append(primitives.serializeString(name));
      }
      out.append(policy);
      // Serialize to bytes for transfer into our output artifact
      result = out.toSource().getBytes(utf8);

      artifacts = new ArtifactSet(artifacts);

      SyntheticArtifact manifest = new SyntheticArtifact(getClass(), "xapi.rpc", result);
      manifest.setVisibility(Visibility.Deploy);
      artifacts.add(manifest);
      // add strong-named copies of the .rpc file, so competing compiles can still be located directly,
      // even if a newer compile owns the HEAD xapi.rpc file.
      for (String name : names) {
        manifest = new SyntheticArtifact(getClass(), name + ".rpc", result);
        manifest.setVisibility(Visibility.Deploy);
        artifacts.add(manifest);
      }
    }
    return artifacts;
  }
}
