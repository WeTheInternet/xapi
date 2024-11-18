package xapi.ui.api;

public interface Stylizer <E> {

  Stylizer<E> applyStyle(E element, String key, String value);

}
