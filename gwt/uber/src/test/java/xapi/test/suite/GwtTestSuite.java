package xapi.test.suite;

import xapi.test.gwt.model.ModelGwtTest;

import com.google.gwt.junit.tools.GWTTestSuite;

public class GwtTestSuite extends GWTTestSuite{

  public GwtTestSuite() {
    super("XApi Gwt Testing");
    addTest(new ModelGwtTest());
  }

}
