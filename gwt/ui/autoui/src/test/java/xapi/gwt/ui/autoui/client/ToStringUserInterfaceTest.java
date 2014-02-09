package xapi.gwt.ui.autoui.client;

import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.ui.autoui.api.AlwaysTrue;
import xapi.ui.autoui.api.UserInterface;
import xapi.ui.autoui.api.UserInterfaceFactory;
import xapi.ui.autoui.client.User;
import xapi.ui.autoui.client.UserModel;
import xapi.ui.autoui.impl.ToStringUiRenderer;
import xapi.ui.autoui.impl.ToStringUserInterface;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.user.client.Window;

public class ToStringUserInterfaceTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "xapi.gwt.ui.autoui.AutoUiTest";
  }
  
  public void testUserToString() {
    
    GwtReflect.magicClass(ToStringUiRenderer.class);
    GwtReflect.magicClass(UserToStringGwt.class);
    GwtReflect.magicClass(ToStringUserInterface.class);
    GwtReflect.magicClass(AlwaysTrue.class);
    GwtReflect.magicClass(UserModel.class);
    
    Class<?> c = UserToStringGwt.class.getEnclosingClass();
    UserInterfaceFactory factory = X_Inject.<UserInterfaceFactory>instance(UserInterfaceFactory.class);
    UserInterface<User> ui = factory.createUi(UserToStringGwt.class, ToStringUserInterface.class);
    ui.renderUi(new UserModel("email", "id", "name"));
    assertEquals("id: id,\nname: name,\nemail: email,\n", ui.toString());
  }
  
}
