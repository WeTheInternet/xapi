package xapi.ui.api;

public interface View <T, S extends StyleService<S>> {

  View<T, S> initialize(T data, S cssService);

}
