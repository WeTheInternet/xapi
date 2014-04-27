package xapi.gwt.ui.html.impl;

import xapi.gwt.ui.autoui.client.ToHtmlUiRenderer;
import xapi.ui.autoui.api.UiRendererOptions;
import xapi.ui.autoui.client.User;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

@UiRendererOptions(
    renderers=ToHtmlUiRenderer.class,
    isWrapper=true,
    template="<div title='$name'>$value</div>"
    )
@ReflectionStrategy(
    keepEverything=true, annotationRetention=ReflectionStrategy.RUNTIME
)
@Html(
  body=@El(
    html="Hello World"
  )
)
public interface UserToDiv extends User{
  
  @UiRendererOptions(
    renderers=ToHtmlUiRenderer.class,
    template="<h1 title='${id}'>$value</h1>"
  )
  String id();
  
  String name();
  
  String email();
  
}