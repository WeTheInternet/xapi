package xapi.dev.model;

import static xapi.source.X_Source.primitiveToObject;

import java.util.Iterator;

import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.ServerToClient;
import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.dev.model.ModelField.GetterMethod;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.source.api.IsType;

public class ModelGenerator {

  public final static class ModelSerializerResult {
    public MethodBuffer clientDeserializer;
    public MethodBuffer clientSerializer;
    public MethodBuffer clientInstantiator;

    public MethodBuffer serverDeserializer;
    public MethodBuffer serverSerializer;
    public MethodBuffer serverInstantiator;
  }

  private final ClassBuffer cb;

  public ModelGenerator(final SourceBuilder<?> builder) {
    this.cb = builder.getClassBuffer();
  }

  public void createFactory(final String qualifiedSourceName) {
    cb.createMethod("static "+qualifiedSourceName+" newInstance()")
      .println("return new "+cb.getSimpleName()+"();");
  }

  public MethodBuffer createMethod(final String returnType, final String methodName, final String params) {
    return cb.createMethod(
      "public " +returnType +" " + methodName + "( "+params +" )");
  }

  public void setSuperClass(final String qualifiedSourceName) {
    cb.setSuperClass(qualifiedSourceName);
  }

  public void generateModel(final IsType type, final HasModelFields fields) {

    // Write getters for all fields.
    for (final ModelField field : fields.getAllFields()) {
      if (field.getType() == null) {
        throw new RuntimeException();
      }
      for (final GetterMethod getter : field.getGetters()) {
        final String datatype = getter.returnType.getQualifiedName();
        final MethodBuffer mb = createMethod(datatype,
          getter.methodName, "");
        final IsType returnType = getter.returnType;
        if (returnType.isPrimitive()) {
          mb.print("return this.<" + primitiveToObject(datatype)+">getProperty(\"" );
          mb.println(field.getName() + "\", "+ getDefaultValue(datatype) +");");
        } else {
          mb.println("return this.<" + datatype + ">getProperty(\"" + field.getName() + "\");");
        }
      }

    }
  }

  private String getDefaultValue(final String type) {
    switch (type) {
      case "boolean":
        return "false";
      case "char":
        return "'\0'";
      case "byte":
      case "short":
        return "0";
      case "long":
        return "0L";
      case "float":
        return "0f";
      case "double":
        return "0.";
    }
    return "null";
  }

  public ModelSerializerResult generateSerializers(final IsType type, final HasModelFields fields) {
    final Iterator<ModelField> serializable = fields.getAllSerializable().iterator();
    if (!serializable.hasNext()) {
      return null;
    }

    final Fifo<ModelField> toServer = X_Collect.newFifo();
    final Fifo<ModelField> toClient = X_Collect.newFifo();

    final ServerToClient clientReceives = fields.getDefaultToClient();
    final ClientToServer serverReceives = fields.getDefaultToServer();

    final boolean toClientEnabled = clientReceives != null && clientReceives.enabled();

    final boolean toServerEnabled = serverReceives != null && serverReceives.enabled();

    boolean balanced = true;
    while(serializable.hasNext()) {
      final ModelField field = serializable.next();
      boolean addedClient = false;
      if (toClientEnabled) {
        // need read only, unless overridden
        final ServerToClient server = field.getServerToClient();
        if (server == null || server.enabled()) {
          toClient.give(field);
          addedClient = true;
        }
      }
      if (toServerEnabled) {
        // need write only, unless overridden.
        final ClientToServer client = field.getClientToServer();
        if (client == null || client.enabled()) {
          toServer.give(field);
        } else if (addedClient) {
          balanced = false;
        }
      }
    }

    final ModelSerializerResult result =
      writeClientSerializer(balanced, toClient.forEach(), toServer.forEach());
    if (balanced) {
      // A balanced serializer, by default, doesn't need a custom server serializer;
      // it can just re-use the same serializer on client and server.
      return result;
    }
    return writeServerSerializer(result, toClient.forEach(), toServer.forEach());
  }

  protected ModelSerializerResult writeClientSerializer(final boolean balanced,
    final Iterable<ModelField> toClient, final Iterable<ModelField> toServer) {
    final ModelSerializerResult result = new ModelSerializerResult();
    return result;
  }

  protected ModelSerializerResult writeServerSerializer(final ModelSerializerResult result,
    final Iterable<ModelField> toClient, final Iterable<ModelField> toServer) {

    return result;
  }

}
