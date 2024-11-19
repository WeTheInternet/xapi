package xapi.gwt.ui.html.impl;

import elemental.html.StyleElement;
import xapi.annotation.inject.SingletonOverride;
import xapi.ui.api.StyleCacheService;
import xapi.ui.html.api.GwtStyles;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/8/17.
 */
@SingletonOverride(implFor = StyleCacheService.class)
public class TestStyleCache implements StyleCacheService<StyleElement, GwtStyles> {
    @Override
    public StyleElement injectStyle(
        Class<? extends GwtStyles> bundle, Class<?>[] styles
    ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerStyle(
        Class<? extends GwtStyles> bundle, String css, Class<?>[] styles
    ) {
        throw new UnsupportedOperationException();
    }
}
