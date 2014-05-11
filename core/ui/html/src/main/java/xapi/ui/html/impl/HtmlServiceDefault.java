package xapi.ui.html.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.inject.X_Inject;
import xapi.ui.api.StyleService;
import xapi.ui.autoui.api.BeanValueProvider;
import xapi.ui.autoui.api.UserInterfaceFactory;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlService;
import xapi.ui.html.api.HtmlSnippet;

@SingletonDefault(implFor=HtmlService.class)
public class HtmlServiceDefault implements HtmlService {

  @Override
  public <T> HtmlSnippet<T> toSnippet(Class<?> templateClass, Class<? extends T> cls, StyleService<?> css) {
    /*
 We do not store a field or provider for UserInterfaceFactory,
 as this entire method needs to be swapped out by the compiler,
 so, forcing gwt to have a UserInterfaceFactory field
 will fail the compile by default.  You may manually set a gwt rebind rule
 or declare an implementor of UserInterfaceFactory annotated w/ @GwtPlatform,
 and annotated w/ @SingletonOverride to enable this method to work in Gwt *

     * you must enable a whole lot of reflection on objects for this to work,
 and will be very inefficient.  The preferred method of replacing this whole
 method with an auto-generated implementation requires no runtime reflection.
     */
    BeanValueProvider bean = X_Inject.singleton(UserInterfaceFactory.class).getBeanProvider(cls);
    return new HtmlSnippet<>(templateClass.getAnnotation(Html.class), bean, css);
  }

}
