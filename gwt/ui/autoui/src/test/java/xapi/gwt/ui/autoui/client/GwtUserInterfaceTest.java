package xapi.gwt.ui.autoui.client;

import xapi.ui.autoui.X_AutoUi;
import xapi.ui.autoui.api.UserInterface;
import xapi.ui.autoui.client.User;
import xapi.ui.autoui.client.UserModel;
import xapi.ui.autoui.impl.ToStringUserInterface;

import com.google.gwt.junit.client.GWTTestCase;

public class GwtUserInterfaceTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "xapi.gwt.ui.autoui.AutoUiTest";
  }

  @SuppressWarnings("unchecked")
  public void testUserToString_Magic() {
    UserInterface<User> ui = X_AutoUi.makeUi(null, UserToStringGwt.class, ToStringUserInterface.class);
    ui.renderUi(new UserModel("email", "id", "name"));
    assertEquals("id: id,\nname: name,\nemail: email,\n", ui.toString());
  }

  public void testUserToString_Factory() {
//
//    GwtReflect.magicClass(ToStringUiRenderer.class);
//    GwtReflect.magicClass(UserToStringGwt.class);
//    GwtReflect.magicClass(ToStringUserInterface.class);
//    GwtReflect.magicClass(AlwaysTrue.class);
//    GwtReflect.magicClass(User.class);
//    GwtReflect.magicClass(UserModel.class);
//
//    UserInterfaceFactory factory = X_Inject.<UserInterfaceFactory>instance(UserInterfaceFactory.class);
//    UserInterface<User> ui = factory.createUi(UserToStringGwt.class, ToStringUserInterface.class);
//    ui.renderUi(new UserModel("email", "id", "name"));
//    assertEquals("id: id,\nname: name,\nemail: email,\n", ui.toString());
  }

}
