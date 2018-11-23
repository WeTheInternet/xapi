package xapi.gwt.ui.autoui.client;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.reflect.shared.GwtReflect;

import xapi.inject.X_Inject;
import xapi.ui.autoui.X_AutoUi;
import xapi.ui.autoui.api.AlwaysTrue;
import xapi.ui.autoui.api.UserInterface;
import xapi.ui.autoui.api.UserInterfaceFactory;
import xapi.ui.autoui.client.User;
import xapi.ui.autoui.client.UserModel;
import xapi.ui.autoui.impl.ToStringUiRenderer;
import xapi.ui.autoui.impl.ToStringUserInterface;

public class GwtUserInterfaceTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "xapi.gwt.ui.autoui.AutoUiTest";
  }

  @SuppressWarnings("unchecked")
  public void testUserToString_Magic() {
    UserInterface<User> ui = X_AutoUi.makeUi(new UserModel("email", "id", "name"), UserToStringGwt.class, ToStringUserInterface.class);
    assertEquals("id: id,\nname: name,\nemail: email,\n", ui.toString());
  }

  public void testUserToString_Factory() {

    GwtReflect.magicClass(ToStringUiRenderer.class);
    GwtReflect.magicClass(UserToStringGwt.class);
    GwtReflect.magicClass(ToStringUserInterface.class);
    GwtReflect.magicClass(AlwaysTrue.class);
    GwtReflect.magicClass(User.class);
    GwtReflect.magicClass(UserModel.class);

    UserInterfaceFactory factory = X_Inject.instance(UserInterfaceFactory.class);
    UserInterface<User> ui = factory.createUi(UserToStringGwt.class, ToStringUserInterface.class);
    ui.renderUi(new UserModel("email", "id", "name"));
    assertEquals("id: id,\nname: name,\nemail: email,\n", ui.toString());
  }

}
