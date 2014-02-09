package xapi.gwt.ui.autoui.client;

import xapi.ui.autoui.api.UiRendererOptions;
import xapi.ui.autoui.client.User;
import xapi.ui.autoui.impl.ToStringUiRenderer;

import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

@UiRendererOptions(
    renderers=ToStringUiRenderer.class,
    isWrapper=true,
    template="<div>$name: $value</div>"
    )
@ReflectionStrategy(
    keepEverything=true, annotationRetention=ReflectionStrategy.RUNTIME
)
interface UserToHtmlGwt extends User{}