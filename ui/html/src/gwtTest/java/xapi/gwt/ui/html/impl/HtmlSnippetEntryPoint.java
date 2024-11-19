package xapi.gwt.ui.html.impl;

import xapi.inject.X_Inject;
import xapi.ui.api.StyleService;
import xapi.ui.autoui.client.UserModel;
import xapi.ui.html.X_Html;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Window;

public class HtmlSnippetEntryPoint implements EntryPoint {

  private StyleService<?, ?> context = X_Inject.singleton(StyleService.class);

  @Override
  public void onModuleLoad() {
    String html = X_Html.toHtml(UserToDiv.class, UserModel.class, new UserModel("email", "id", "name"), context);
    Document.get().getBody().setInnerHTML(html);
    Window.alert(html);
  }
}
