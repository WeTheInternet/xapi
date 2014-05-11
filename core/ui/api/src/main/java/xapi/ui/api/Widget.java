package xapi.ui.api;


public interface Widget <T> {

  T getElement();

  void append(Widget<T> child);
  
}
