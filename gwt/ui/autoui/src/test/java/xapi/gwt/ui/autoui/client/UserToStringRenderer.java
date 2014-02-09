package xapi.gwt.ui.autoui.client;

import xapi.ui.autoui.api.HasNamedValue;
import xapi.ui.autoui.api.UiRenderer;
import xapi.ui.autoui.api.UiRenderingContext;
import xapi.ui.autoui.api.UserInterface;
import xapi.ui.autoui.client.User;

import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class UserToStringRenderer implements UiRenderer<User> {

  @Override
  public UiRenderer<User> renderInto(UserInterface<?> ui, UiRenderingContext ctx, HasNamedValue<User> data) {
    if (SafeHtmlBuilder.class.isAssignableFrom(ui.getClass())) {
      SafeHtmlBuilder out = SafeHtmlBuilder.class.cast(ui);
      User user = data.getValue();
      out
        .appendHtmlConstant("<div>")
        .appendEscaped(user.name())
        .appendHtmlConstant("</div>")

        .appendHtmlConstant("<div>")
        .appendEscaped(user.id())
        .appendHtmlConstant("</div>")
        
        .appendHtmlConstant("<div>")
        .appendEscaped(user.email())
        .appendHtmlConstant("</div>")
      ;
    }
    return this;
  }
  
}