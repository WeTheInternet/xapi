package xapi.gwt.ui.html.impl;

import xapi.dev.source.HtmlBuffer;
import xapi.test.Assert;
import xapi.ui.autoui.client.UserModel;
import xapi.ui.html.X_Html;

import com.google.gwt.junit.client.GWTTestCase;

public class X_HtmlTest extends GWTTestCase {

  private HtmlBuffer context;

  @Override
  protected void gwtSetUp() throws Exception {
    context = new HtmlBuffer();
  }
  
  public void test_toHtml() {
    String html = X_Html.toHtml(UserToDiv.class, new UserModel("email", "id", "name"), context);
    Assert.assertEquals("", html);
  }
  
  @Override
  public String getModuleName() {
    return "xapi.gwt.ui.html.HtmlSnippetTestDependencies";
  }
}
