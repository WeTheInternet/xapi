package xapi.gwt.ui.autoui.client;

import xapi.ui.autoui.api.UiRendererOptions;
import xapi.ui.autoui.client.User;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

@UiRendererOptions(
    renderers=ToHtmlUiRenderer.class,
    isWrapper=true,
    template="<div title='$name'>$value</div>"
    )
@ReflectionStrategy(
    keepEverything=true, annotationRetention=ReflectionStrategy.RUNTIME
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