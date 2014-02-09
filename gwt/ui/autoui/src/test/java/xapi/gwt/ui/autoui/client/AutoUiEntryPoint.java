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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.reflect.client.GwtReflect;

public class AutoUiEntryPoint implements EntryPoint {

  @Override
  public void onModuleLoad() {

    // When bypassing the magic-method, we need a lot of reflection to make a template work.
    GwtReflect.magicClass(UserToStringGwt.class);
    GwtReflect.magicClass(AlwaysTrue.class);
    GwtReflect.magicClass(ToStringUiRenderer.class);
    GwtReflect.magicClass(ToStringUserInterface.class);
    GwtReflect.magicClass(UserModel.class);

    Class<?> c = UserToStringGwt.class.getEnclosingClass();
    UserInterfaceFactory factory = X_Inject.<UserInterfaceFactory>instance(UserInterfaceFactory.class);
    UserInterface<User> ui = factory.createUi(UserToStringGwt.class, ToStringUserInterface.class);
    X_Log.info(getClass(), ui, ""+ui.renderUi(new UserModel("email", "id","name")));
    
    

  }
}
