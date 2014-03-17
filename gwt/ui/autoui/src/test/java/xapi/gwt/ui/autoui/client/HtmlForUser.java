package xapi.gwt.ui.autoui.client;

import xapi.gwt.ui.autoui.api.El;
import xapi.ui.autoui.client.User;

public interface HtmlForUser extends User {

  @El(
    tag="a",
    html="<$this href='mailto:$value'>$value</$this>"
  )
  public String email();

  @El(
    tag="a",
    html="<$this href='/u/$value'>$value</$this>"
  )
  public String id();

  @El
  public String name();
  
}
