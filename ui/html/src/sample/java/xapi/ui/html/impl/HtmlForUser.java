package xapi.ui.html.impl;

import xapi.ui.autoui.client.User;
import xapi.ui.html.api.El;

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
