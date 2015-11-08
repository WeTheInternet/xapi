/**
 *
 */
package xapi.test.model;

import org.junit.Assert;
import org.junit.Test;

import xapi.annotation.model.GetterFor;
import xapi.annotation.model.GetterForBuilder;
import xapi.annotation.model.SetterFor;
import xapi.annotation.model.SetterForBuilder;
import xapi.model.api.ModelManifest;
import xapi.model.api.ModelManifest.MethodData;
import xapi.model.content.ModelContent;
import xapi.model.impl.ModelUtil;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelManifestSerializationTest {

  @Test
  public void testSerializationRoundTrip() {
    final ModelManifest manifest = ModelUtil.createManifest(ModelContent.class);
    final String asString = ModelManifest.serialize(manifest);
    final ModelManifest asManifest = ModelManifest.deserialize(asString);
    System.out.println(asString);
    Assert.assertEquals(manifest, asManifest);
  }

  @Test
  public void testEquality() throws Throwable{
    final ModelManifest manifest1 = ModelUtil.createManifest(ModelContent.class);
    final ModelManifest manifest2 = ModelUtil.createManifest(ModelContent.class);
    Assert.assertEquals(manifest1, manifest2);
  }

  @Test
  public void testModelFieldEquality() throws Throwable{
    final MethodData method1 = new MethodData("getName");
    final MethodData method2 = new MethodData("getName");
    Assert.assertEquals(method1, method2);
  }

  @Test
  public void testModelFieldInequality() throws Throwable{
    final GetterFor getter = GetterForBuilder.buildGetterFor().setValue("name").build();
    final SetterFor setter = SetterForBuilder.buildSetterFor().setValue("name").build();
    final MethodData method1 = new MethodData("name", "id", getter, null, null);
    final MethodData method2 = new MethodData("name", "id", null, setter, null);
    Assert.assertNotEquals(method1, method2);
    Assert.assertEquals(method1.getName(), method2.getName());
  }

  @Test
  @GetterFor("fake")
  public void testInequality() throws Throwable{
    final ModelManifest manifest1 = ModelUtil.createManifest(ModelContent.class);
    final ModelManifest manifest2 = ModelUtil.createManifest(ModelContent.class);
    final GetterFor getter = getClass().getMethod("testInequality").getAnnotation(GetterFor.class);
    Assert.assertNotNull(getter);
    manifest2.addProperty("getFake", "id", getter, null, null);
    Assert.assertNotEquals(manifest1, manifest2);
  }

}
