package xapi.ui.api;

public interface View <T> {

  View<T> initialize(T data, StyleService<?> cssService);

}
