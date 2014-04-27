package xapi.ui.html.impl;

import javax.inject.Provider;

import xapi.annotation.inject.SingletonDefault;
import xapi.dev.source.HtmlBuffer;
import xapi.inject.X_Inject;
import xapi.ui.autoui.api.BeanValueProvider;
import xapi.ui.autoui.api.UserInterfaceFactory;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.HtmlService;
import xapi.ui.html.api.HtmlSnippet;

@SingletonDefault(implFor=HtmlService.class)
public class HtmlServiceDefault implements HtmlService {

  private static final Provider<UserInterfaceFactory> ui = X_Inject.singletonLazy(UserInterfaceFactory.class);
  
  @Override
  public <T> HtmlSnippet<T> toSnippet(Class<?> templateClass, Class<? extends T> cls, HtmlBuffer buffer) {
    BeanValueProvider bean = ui.get().getBeanProvider(cls);
    return new HtmlSnippet<>(templateClass.getAnnotation(Html.class), bean, buffer);
  }

}
