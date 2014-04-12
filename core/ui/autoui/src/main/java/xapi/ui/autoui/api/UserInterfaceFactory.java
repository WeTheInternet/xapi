package xapi.ui.autoui.api;

public interface UserInterfaceFactory {

  <T, U extends UserInterface<T>> U createUi(Class<? extends T> type, Class<? super U> uiType);
  
  BeanValueProvider getBeanProvider(Class<?> cls);
  
}
