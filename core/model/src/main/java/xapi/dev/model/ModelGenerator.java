package xapi.dev.model;

import xapi.annotation.model.ClientToServer;
import xapi.annotation.model.ServerToClient;
import xapi.collect.X_Collect;
import xapi.collect.api.*;
import xapi.dev.model.ModelField.GetterMethod;
import xapi.dev.source.ClassBuffer;
import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.source.api.HasTypeParams;
import xapi.source.api.IsParameterizedType;
import xapi.source.api.IsType;
import xapi.source.api.IsTypeArgument;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import static xapi.source.X_Source.primitiveToObject;

public class ModelGenerator {

  public final static class ModelSerializerResult {
    public MethodBuffer clientDeserializer;
    public MethodBuffer clientSerializer;
    public MethodBuffer clientInstantiator;

    public MethodBuffer serverDeserializer;
    public MethodBuffer serverSerializer;
    public MethodBuffer serverInstantiator;
  }

  private static class DefaultProvider {
    private final Predicate<String> matcher;
    private final In2Out1<MethodBuffer, GetterMethod, String> initializer;

    private DefaultProvider(Predicate<String> matcher, In2Out1<MethodBuffer, GetterMethod, String> initializer) {
      this.matcher = matcher;
      this.initializer = initializer;
    }

    public boolean tryMatch(String type) {
      return matcher.test(type);
    }
  }

  private final ClassBuffer cb;

  private List<DefaultProvider> providers;

  public ModelGenerator(final SourceBuilder<?> builder) {
    this.cb = builder.getClassBuffer();
    providers = new ArrayList<>();
    initializeDefaults(providers);
  }

  protected void initializeDefaults(List<DefaultProvider> providers) {
    providers.add(
        new DefaultProvider(
            s -> s.contains("[]"), (mb, data) -> {
              String datatype = mb.addImport(data.returnType.getQualifiedName());
              String reduced = datatype.replace("[]", "");
              int slice = reduced.indexOf('<');
              if (slice != -1) {
                reduced = reduced.substring(0, slice) + reduced.substring(reduced.lastIndexOf('>') + 1);
              }
              final String array = mb.addImport(Array.class);
              return
                  "return ("+ datatype +")" + // we need to cast because array.newInstance returns Object
                  array + ".newInstance(" + reduced + // only replaces one [], thus allowing int[][] to become Array.newInstance(int[].class, 0);
                                                        ".class, 0);";
            }
        )
    );
    providers.add(newCollectionType(IntTo.class, "newList"));
    providers.add(newCollectionType(StringTo.class, "newStringMap"));
    providers.add(newCollectionType(ClassTo.class, "newClassMap"));
    providers.add(newCollectionType(IntTo.Many.class, "newIntMultiMap"));
    providers.add(newCollectionType(StringTo.Many.class, "newStringMultiMap"));
    providers.add(newCollectionType(ClassTo.Many.class, "newClassMultiMap"));

    providers.add(newMapType(ObjectTo.class, "newMap"));
    providers.add(newMapType(ObjectTo.Many.class, "newMultiMap"));

    // TODO consider allowing primitive wrapper types to be initialized to non-null
    // This should be done via a NotNull annotation.
//    providers.add(
//        new DefaultProvider(
//            new Predicate<String>() {
//              @Override
//              public boolean test(String s) {
//                switch (s.toString()) {
//                  case "java.lang.Byte":
//                  case "java.lang.Short":
//                  case "java.lang.Character":
//                  case "java.lang.Integer":
//                  case "java.lang.Long":
//                  case "java.lang.Float":
//                  case "java.lang.Double":
//                    return true;
//                }
//                return false;
//              }
//            },
//            new ConvertsTwoValues<MethodBuffer, GetterMethod, String>() {
//              @Override
//              public String convert(MethodBuffer one, GetterMethod two) {
//                return "return new "+two.returnType.getSimpleName()+"(0);";
//              }
//            }
//        )
//    );
    providers.add(
        new DefaultProvider(
            s -> "java.lang.Boolean".equals(s),
            (one, two) -> "return false;"
        )
    );
  }

  protected DefaultProvider newCollectionType(final Class<?> cls, final String method) {
    return new DefaultProvider(
        s -> s.split("<")[0].equals(cls.getCanonicalName()),
        (buffer, mthd) -> {
          final String collect = buffer.addImport(X_Collect.class);
          String component;
          if (mthd.componentGetter == null || mthd.componentGetter[0] == null) {
            assert mthd.generics.length == 1 : "Expected only one type parameter for a collections class "+cls;
            component = buffer.addImport(mthd.generics[0].getRawType().getQualifiedName()) + ".class";
          } else {
            component = mthd.componentGetter[0];
          }
          return "return "+collect+"."+method+"("+component+");";
        }
    );
  }

  protected DefaultProvider newMapType(final Class<?> cls, final String method) {
    return new DefaultProvider(
        s -> s.split("<")[0].equals(cls.getCanonicalName()),
        (one, mthd) -> {
          final String collect = one.addImport(X_Collect.class);
          assert mthd.generics.length == 2 : "Expected exactly two generic types for a map class "+cls;
          String keyType, valueType = keyType = null;
          if (mthd.componentGetter != null) {
            if (mthd.componentGetter[0] != null) {
              valueType = mthd.componentGetter[0];
            }
            if (mthd.componentGetter.length > 1 && mthd.componentGetter[1] != null) {
              keyType = mthd.componentGetter[1];
            }
          }
          if (keyType == null) {
            keyType = one.addImport(mthd.generics[0].getRawType().getQualifiedName()) + ".class";
          }
          if (valueType == null) {
            valueType = one.addImport(mthd.generics[1].getRawType().getQualifiedName()) + ".class";
          }
          return "return "+collect+"."+method+"("+keyType+", "+valueType+");";
        }
    );
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

  public void generateModel(IsType sourceType, final IsType type, final HasModelFields fields) {

    // Write getters for all fields.
    for (final ModelField field : fields.getAllFields()) {
      if (field.getType() == null) {
        throw new RuntimeException("No type found for field "+field.getName()+" in "+type+".  " +
            "Perhaps you are lacking a getter/model annotation?  Check that get/set method names match.");
      }
      for (final GetterMethod getter : field.getGetters()) {
        final String qualified = getter.returnType.toSource();
        final String imported = cb.addImport(qualified);
        final MethodBuffer mb = createMethod(imported,
          getter.methodName, "");
        final IsType returnType = getter.returnType;
        if (returnType.isPrimitive() && !returnType.getSimpleName().contains("[]")) {
          mb.print("return this.<" + primitiveToObject(imported)+">getProperty(\"" );
          mb.println(field.getName() + "\", "+ getDefaultValue(imported) +");");
        } else {
          String getterName = "getProperty";
          boolean isMulti = field.isListType() || field.isMapType();
          if (isMulti) {
            getterName = "getOrSaveProperty";
          }
          mb.print("return this.<" + imported + ">"+ getterName + "(\"" + field.getName() + "\"");
          boolean didWork = false;
          for (DefaultProvider defaultProvider : providers) {
            if (defaultProvider.tryMatch(qualified)) {
              didWork = true;
              final String provider = mb.addImport(Out1.class);
              mb.print(", ")
                  .print("new ").print(provider).print("<").print(imported).println(">() {")
                  .indent()
                  .print("public ").print(imported).println(" out1() {")
                  .indent()
                  .println(defaultProvider.initializer.io(mb, getter))
                  .outdent()
                  .println("}")
                  .outdent()
                  .println("}")
              ;

              break;
            }
          }
          if (!didWork && isMulti) {

          }
          mb.println(");");
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
