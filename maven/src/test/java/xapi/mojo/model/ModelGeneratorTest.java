package xapi.mojo.model;

import org.junit.Test;

import xapi.bytecode.ClassFile;
import xapi.bytecode.impl.BytecodeAdapterService;
import xapi.dev.scanner.X_Scanner;
import xapi.dev.scanner.impl.ClasspathResourceMap;
import xapi.log.X_Log;
import xapi.model.api.Model;

public class ModelGeneratorTest {

  private static interface TestModel extends Model {
    String getString();
  }
  
  @Test
  public void testModelGen() {
    ModelGeneratorMojo mojo = new ModelGeneratorMojo();
    ClasspathResourceMap map = X_Scanner.scanClassloader(getClass().getClassLoader()
        , true, true, true, "xapi");
    BytecodeAdapterService adapter = new BytecodeAdapterService();
    ClassFile cls = map.findClass(TestModel.class.getName());
    X_Log.info(getClass(), "test model:", cls);
    mojo.buildModel(cls, adapter, map);
  }
  
}
