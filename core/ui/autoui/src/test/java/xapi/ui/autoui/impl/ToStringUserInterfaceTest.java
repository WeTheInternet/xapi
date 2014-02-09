package xapi.ui.autoui.impl;

import org.junit.Test;

import xapi.test.Assert;
import xapi.ui.autoui.X_AutoUi;
import xapi.ui.autoui.api.UserInterface;
import xapi.ui.autoui.client.User;
import xapi.ui.autoui.client.UserModel;
import xapi.ui.autoui.client.UserView;

public class ToStringUserInterfaceTest {

  @Test
  public void testUserToString() {
    UserInterface<User> ui = X_AutoUi.makeUi(new UserModel("email", "id", "name"), UserView.class, ToStringUserInterface.class);
    Assert.assertEquals("email: email,\nname: name,\nid: id,\n", ui.toString());
  }
  
}
