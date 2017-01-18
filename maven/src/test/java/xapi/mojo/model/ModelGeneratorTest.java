package xapi.mojo.model;

import org.junit.Test;
import xapi.bytecode.ClassFile;
import xapi.bytecode.impl.BytecodeAdapterService;
import xapi.dev.scanner.X_Scanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.model.api.Model;

import static org.junit.Assert.assertNotNull;

public class ModelGeneratorTest {

  private interface TestModel extends Model {
    String getString();
  }

  @Test
  public void testModelGen() {
    ModelGeneratorMojo mojo = new ModelGeneratorMojo();
    ClasspathResourceMap map = X_Scanner.scanClassloader(getClass().getClassLoader()
        , true, true, true, "xapi");
    BytecodeAdapterService adapter = new BytecodeAdapterService();
    ClassFile cls = map.findClass(TestModel.class.getName());
    assertNotNull("No class found for " + TestModel.class.getName(), cls);
    mojo.buildModel(cls, adapter, map);
  }

}
