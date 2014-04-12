package xapi.gwt.ui.autoui.client;

import xapi.log.X_Log;
import xapi.ui.autoui.X_AutoUi;
import xapi.ui.autoui.client.User;
import xapi.ui.autoui.client.UserModel;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.ui.RootPanel;

public class HtmlSnippetEntryPoint implements EntryPoint {

  @SuppressWarnings("unchecked")
  @Override
  public void onModuleLoad() {
    SafeHtmlUserInterface<User> ui;

//    // When bypassing the magic-method, we need a lot of reflection to make a template work.
//    GwtReflect.magicClass(UserToDiv.class);
//    GwtReflect.magicClass(AlwaysTrue.class);
//    GwtReflect.magicClass(ToHtmlUiRenderer.class);
//    GwtReflect.magicClass(SafeHtmlUserInterface.class);
//    GwtReflect.magicClass(UserModel.class);
//    GwtReflect.magicClass(User.class);
//    
//
//    UserInterfaceFactory factory = X_Inject.<UserInterfaceFactory>instance(UserInterfaceFactory.class);
//    ui = factory.createUi(UserToDiv.class, SafeHtmlUserInterface.class);
    ui = X_AutoUi.<User, SafeHtmlUserInterface<User>>makeUi(null, UserToDiv.class, SafeHtmlUserInterface.class);
    
    X_Log.info(getClass(), ui, ""+ui.renderUi(new UserModel("email", "id","name")));
    
    RootPanel.getBodyElement().setInnerHTML(ui.getSafeHtmlBuilder().toSafeHtml().asString());

  }
}
