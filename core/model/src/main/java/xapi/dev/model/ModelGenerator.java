package xapi.dev.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.Serializable;
import xapi.annotation.model.ServerToClient;
import xapi.collect.X_Collect;
import xapi.collect.api.Fifo;
import xapi.dev.model.ModelField.GetterMethod;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.source.X_Source;
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

  private ClassBuffer cb;

  public ModelGenerator(SourceBuilder<?> builder) {
    this.cb = builder.getClassBuffer();
  }

  public void createFactory(String qualifiedSourceName) {
    cb.createMethod("static "+qualifiedSourceName+" newInstance()")
      .println("return new "+cb.getSimpleName()+"();");
  }

  public MethodBuffer createMethod(String returnType, String methodName, String params) {
    return cb.createMethod(
      "public " +returnType +" " + methodName + "( "+params +" )");
  }

  public void setSuperClass(String qualifiedSourceName) {
    cb.setSuperClass(qualifiedSourceName);
  }

  public String generateModel(IsType type, HasModelFields fields) {

    // Write getters for all fields.
    for (ModelField field : fields.getAllFields()) {
      if (field.getType() == null) {
        throw new RuntimeException();
      }
      Set<String> publicGetters = new HashSet<String>();
      for (GetterMethod getter : field.getGetters()) {
        String datatype = getter.returnType.getQualifiedName();
        MethodBuffer mb = createMethod(datatype,
          getter.methodName, "");
        mb.println("return this.<" +
          (getter.returnType.isPrimitive() ? X_Source.primitiveToObject(datatype) : datatype)
          +">getProperty(\"" + field.getName() + "\");");
      }

    }

    return "";
  }

  public ModelSerializerResult generateSerializers(IsType type, HasModelFields fields) {
    Iterator<ModelField> serializable = fields.getAllSerializable().iterator();
    if (!serializable.hasNext())
      return null;

    Fifo<ModelField> toServer = X_Collect.newFifo();
    Fifo<ModelField> toClient = X_Collect.newFifo();

    ServerToClient clientReceives = fields.getDefaultToClient();
    ClientToServer serverReceives = fields.getDefaultToServer();
    Serializable defaultSerializable = fields.getDefaultSerializable();

    boolean toClientEnabled = defaultSerializable == null ?
        // not default serializable, so if and only if @C2S.enabled == true
        clientReceives != null && clientReceives.enabled():
        // class is default serializable, anything but @C2S.enabled == false
        clientReceives == null || clientReceives.enabled();

    boolean toServerEnabled = defaultSerializable == null ?
        // not default serializable, so if and only if @S2C.enabled == true
        serverReceives != null && serverReceives.enabled():
        // class is default serializable, anything but @S2C.enabled == false
        serverReceives == null || serverReceives.enabled();

    boolean balanced = true;
    while(serializable.hasNext()) {
      ModelField field = serializable.next();
      boolean addedClient = false;
      if (toClientEnabled) {
        // need read only, unless overridden
        ServerToClient server = field.getServerToClient();
        if (server == null || server.enabled()) {
          toClient.give(field);
          addedClient = true;
        }
      }
      if (toServerEnabled) {
        // need write only, unless overridden.
        ClientToServer client = field.getClientToServer();
        if (client == null || client.enabled()) {
          toServer.give(field);
        } else if (addedClient)
          balanced = false;
      }
    }

    ModelSerializerResult result =
      writeClientSerializer(balanced, toClient.forEach(), toServer.forEach());
    if (balanced) {
      // A balanced serializer, by default, doesn't need a custom server serializer;
      // it can just re-use the same serializer on client and server.
      return result;
    }
    return writeServerSerializer(result, toClient.forEach(), toServer.forEach());
  }

  protected ModelSerializerResult writeClientSerializer(boolean balanced,
    Iterable<ModelField> toClient, Iterable<ModelField> toServer) {
    ModelSerializerResult result = new ModelSerializerResult();
    return result;
  }

  protected ModelSerializerResult writeServerSerializer(ModelSerializerResult result,
    Iterable<ModelField> toClient, Iterable<ModelField> toServer) {

    return result;
  }

}
