package xapi.gwt.ui.html.impl;

import xapi.inject.X_Inject;
import xapi.test.Assert;
import xapi.ui.api.StyleService;
import xapi.ui.autoui.client.User;
import xapi.ui.autoui.client.UserModel;
import xapi.ui.html.X_Html;

import com.google.gwt.junit.client.GWTTestCase;

public class X_HtmlTest extends GWTTestCase {

  private StyleService<?, ?> context;

  @Override
  protected void gwtSetUp() throws Exception {
    context = X_Inject.singleton(StyleService.class);
  }

  public void test_toHtml() {
    String html = X_Html.toHtml(UserToDiv.class, User.class, new UserModel("email", "id", "name"), context);
    Assert.assertEquals("<div>  Hello World\n</div>", html);
  }

  @Override
  public String getModuleName() {
    return "xapi.gwt.ui.html.HtmlSnippetTestDependencies";
  }
}
