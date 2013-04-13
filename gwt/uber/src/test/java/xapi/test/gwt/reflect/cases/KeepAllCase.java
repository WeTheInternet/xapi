package xapi.test.gwt.reflect.cases;

import xapi.annotation.reflect.KeepAnnotation;
import xapi.annotation.reflect.KeepClass;
import xapi.annotation.reflect.KeepConstructor;
import xapi.annotation.reflect.KeepField;
import xapi.annotation.reflect.KeepMethod;

@KeepClass
@KeepMethod
@KeepAnnotation
@KeepField
@KeepConstructor
public class KeepAllCase {

  private int privateInt;
  protected int protectedInt;
  public int publicInt;
  int packageInt;

  public KeepAllCase() {
  }

  /**
   * @return the privateInt
   */
  public int getPrivateInt() {
    return privateInt;
  }

  /**
   * @param privateInt the privateInt to set
   */
  public void setPrivateInt(int privateInt) {
    this.privateInt = privateInt;
  }


}
