package xapi.bytecode.impl;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import xapi.source.X_Source;
import xapi.source.api.IsClass;

public class BytecodeAdapterServiceTest {

  private static BytecodeAdapterService service;
  @BeforeClass
  public static void extractClassFile() {
    service = new BytecodeAdapterService();
  }
  
  @Test
  public void testClassFile() {
    IsClass asClass = service.toClass(getClass().getName());
    Assert.assertEquals(asClass.getPackage(), getClass().getPackage().getName());
    Assert.assertEquals(asClass.getEnclosedName(), X_Source.classToEnclosedSourceName(getClass()));
    Assert.assertEquals(asClass.getModifier(), getClass().getModifiers());
  }
  
}
