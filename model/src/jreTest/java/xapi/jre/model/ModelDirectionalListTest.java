package xapi.jre.model;

import org.junit.Test;
import xapi.dev.source.CharBuffer;
import xapi.model.api.*;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelText;
import xapi.model.impl.ModelUtil;
import xapi.model.test.api.ModelTestDirections;
import xapi.model.tools.ModelSerializerDefault;
import xapi.model.tools.PrimitiveSerializerDefault;
import xapi.model.user.ModelUser;
import xapi.source.lex.StringCharIterator;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

/** Exercises HTTP-client directional serialization without opening a socket or starting a server. */
public class ModelDirectionalListTest {
  private static final class Server extends ModelServiceJre {
    private final ModelModule module;
    Server(final ModelModule module) {
      this.module = module;
      for (ModelManifest manifest : module.getManifests()) { register(manifest.getModelType()); registerManifest(manifest); }
    }
    @Override public ModelModule getModelModule() { return module; }
  }

  private static ModelModule module(final String enabledDirection) throws Exception {
    final ModelModule module = new ModelModule(); module.setModuleName("direction-list-test"); module.setUuid("direction-list-v1");
    for (Class<? extends Model> type : Arrays.asList(ModelContent.class, ModelList.class, ModelText.class, ModelUser.class)) {
      module.addManifest(ModelUtil.createManifest(type));
    }
    final ModelManifest content = module.getManifest("content");
    content.getMethodData("children").addAnnotations(ModelTestDirections.class.getMethod(enabledDirection).getAnnotations());
    final ArrayList<String> order = new ArrayList<>(Arrays.asList(content.getPropertyNames()));
    order.remove("children"); order.remove("permaLink"); order.add(0, "children"); order.add(1, "permaLink");
    for (int i = 0; i < order.size(); i++) content.getPropertyNames()[i] = order.get(i);
    return ModelSerializerDefault.deserializeModule(ModelSerializerDefault.serialize(module));
  }

  private static ModelContent content(final ModelServiceJre server) {
    final ModelContent parent = server.create(ModelContent.class); parent.setKey(server.newKey("fixture", "content", "parent")); parent.setPermaLink("following-field-sentinel");
    final ModelContent child = server.create(ModelContent.class); child.setKey(parent.getKey().getChild("content", "child")); child.setText("inline-value");
    final ModelList<ModelContent> list = server.create(ModelList.class); list.setModelType(ModelContent.class); list.add(child); parent.setChildren(list); return parent;
  }

  @Test public void clientToServerDisabledListWritesOnlyNullAndPreservesFollowingField() throws Exception {
    final ModelModule module = module("getServerToClient"); final Server server = new Server(module);
    final ModelServiceHttp client = new ModelServiceHttp("https://example.test/model", module);
    final ModelContent parent = content(server);
    final String wire = client.serialize(module.getManifest("content"), parent).toSource();
    final StringCharIterator input = new StringCharIterator(wire); final ModelContent loaded = server.deserialize(module.getManifest("content"), input);
    assertFalse(input.hasNext()); assertNull(loaded.getChildren()); assertEquals(parent.getPermaLink(), loaded.getPermaLink());
    final StringCharIterator enabled = new StringCharIterator(server.serialize(module.getManifest("content"), parent).toSource());
    final ModelContent allowed = client.deserialize(module.getManifest("content"), enabled);
    assertFalse(enabled.hasNext()); assertEquals("inline-value", allowed.getChildren().first().getText()); assertEquals(parent.getPermaLink(), allowed.getPermaLink());
  }

  @Test public void serverToClientDisabledListWritesOnlyNullAndPreservesFollowingField() throws Exception {
    final ModelModule module = module("getClientToServer"); final Server server = new Server(module);
    final ModelServiceHttp client = new ModelServiceHttp("https://example.test/model", module);
    final ModelContent parent = content(server);
    final StringCharIterator input = new StringCharIterator(server.serialize(module.getManifest("content"), parent).toSource());
    final ModelContent loaded = client.deserialize(module.getManifest("content"), input);
    assertFalse(input.hasNext()); assertNull(loaded.getChildren()); assertEquals(parent.getPermaLink(), loaded.getPermaLink());
    final StringCharIterator enabled = new StringCharIterator(client.serialize(module.getManifest("content"), parent).toSource());
    final ModelContent allowed = server.deserialize(module.getManifest("content"), enabled);
    assertFalse(enabled.hasNext()); assertEquals("inline-value", allowed.getChildren().first().getText()); assertEquals(parent.getPermaLink(), allowed.getPermaLink());
  }

  @Test public void manifestlessDisabledListPlaceholderHasExactlyTheNullMarker() {
    class Serializer extends ModelSerializerDefault<ModelContent> {
      String placeholder(final xapi.model.api.ModelSerializationContext context) {
        final CharBuffer out = new CharBuffer(); writeNullPlaceholder(out, ModelList.class, context.getPrimitives(), context); return out.toSource();
      }
    }
    final Server service = new Server(new ModelModule());
    final String wire = new Serializer().placeholder(new ModelSerializationContext(new CharBuffer(), service, null));
    final PrimitiveSerializerDefault primitives = new PrimitiveSerializerDefault(); final StringCharIterator chars = new StringCharIterator(wire);
    assertEquals(-2, primitives.deserializeInt(chars)); assertFalse(chars.hasNext());
  }
}
