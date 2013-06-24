package xapi.mojo.model;

import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.Ignore;
import org.junit.Test;

import xapi.bytecode.ClassFile;
import xapi.bytecode.impl.BytecodeAdapterService;
import xapi.dev.X_Dev;
import xapi.dev.scanner.ClasspathResourceMap;
import xapi.log.X_Log;
import xapi.model.api.Model;

public class ModelGeneratorTest {

  private static interface TestModel extends Model {
    String getString();
  }
  
  @Test
  public void testModelGen() {
    ModelGeneratorMojo mojo = new ModelGeneratorMojo();
    ClasspathResourceMap map = X_Dev.scanClassloader(Thread.currentThread().getContextClassLoader()
        , true, true, true, "xapi");
    BytecodeAdapterService adapter = new BytecodeAdapterService();
    ClassFile cls = map.findClass(TestModel.class.getName());
    X_Log.info(cls.getAccessFlags());
    mojo.buildModel(cls, adapter, map);
  }
  
}
