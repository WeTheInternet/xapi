package xapi.elemental.impl;

import javax.inject.Provider;

import xapi.util.api.ConvertsValue;
import elemental.dom.Element;

public class LazyHtmlConverter <T, E extends Element> implements ConvertsValue<T, E> {

  private final ConvertsValue<T, E> converter;

  public LazyHtmlConverter(ConvertsValue<T, String> serializer) {
    converter = new ConvertsValue<T, E>() {

      private T item;
      private final LazyHtmlClone<E> cloner = new LazyHtmlClone<E>(
          new Provider<String>() {
            @Override
            public String get() {
              return serializer.convert(item);
            }
          }
      );
      @Override
      public E convert(final T from) {
        item = from;
        try {
          return cloner.get();
        } finally {
          item = null;
        }
      }
    };
  }

  @Override
  public E convert(T from) {
      return converter.convert(from);
  }
}
