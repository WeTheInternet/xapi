package xapi.ui.autoui.impl;

import org.junit.Test;

import xapi.test.Assert;
import xapi.ui.autoui.X_AutoUi;
import xapi.ui.autoui.api.UiOptions;
import xapi.ui.autoui.api.UiRendererOptions;
import xapi.ui.autoui.api.UserInterface;
import xapi.ui.autoui.client.User;
import xapi.ui.autoui.client.UserModel;

@SuppressWarnings("unchecked")
public class ToStringUserInterfaceTest {

  private static final UserModel MODEL = new UserModel("email", "id", "name");

  @UiOptions(
    fields = {"email","name","id"},
    renderers=
    @UiRendererOptions(
        renderers = ToStringUiRenderer.class,
        isWrapper = true,
        template = "$name: $value,\n"
    )
  )
  public interface UserViewWrapped extends User {}
  
  @Test
  public void testUserToStringWrapped() {
    UserInterface<User> ui = X_AutoUi.makeUi(MODEL, UserViewWrapped.class, ToStringUserInterface.class);
    Assert.assertEquals("email: email,\nname: name,\nid: id,\n", ui.toString());
  }
  

  @UiRendererOptions(
      renderers = ToStringUiRenderer.class,
      isWrapper = false,
      template = "${email.name()}: ${email},\n" +
                 "${name.name()}: ${name},\n" +
                 "${id.name()}: ${id}"
  )
  public interface UserViewNamed extends User {}

  @Test
  public void testUserToStringNamed() {
    ToStringUserInterface ui = X_AutoUi.makeUi(MODEL, UserViewNamed.class, ToStringUserInterface.class);
    Assert.assertEquals("email: email,\nname: name,\nid: id", ui.toString());
  }
  
}
