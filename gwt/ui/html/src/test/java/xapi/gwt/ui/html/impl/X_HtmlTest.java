package xapi.gwt.ui.html.impl;

import com.google.gwt.junit.client.GWTTestCase;

import xapi.inject.X_Inject;
import xapi.test.Assert;
import xapi.ui.api.StyleService;
import xapi.ui.autoui.client.UserModel;
import xapi.ui.html.X_Html;

public class X_HtmlTest extends GWTTestCase {

  private StyleService<?> context;

  @Override
  protected void gwtSetUp() throws Exception {
    context = X_Inject.singleton(StyleService.class);
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
