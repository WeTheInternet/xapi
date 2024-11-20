package xapi.elemental.impl;

import elemental.dom.Element;
import xapi.ui.api.StyleService;
import xapi.util.api.ConvertsValue;
import xapi.util.api.MergesValues;

import javax.inject.Provider;

public class LazyHtmlConverter <T, S extends StyleService<?, ?>, E extends Element>
implements MergesValues<T, S, E> {

  protected final LazyHtmlClone<E> cloner;
  private final MergesValues<T, S, E> converter;

  public LazyHtmlConverter(ConvertsValue<T, String> serializer) {
    class Merger implements MergesValues<T, S, E> {

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
      public E merge(T from, S service) {
        item = from;
        try {
          return cloner.get();
        } finally {
          item = null;
        }
      }
    };
    Merger merger = new Merger();
    converter = merger;
    cloner = merger.cloner;
  }

  public LazyHtmlConverter(final MergesValues<T, S, String> serializer) {
    class Merger implements MergesValues<T, S, E> {

      private T item;
      private S service;
      private final LazyHtmlClone<E> cloner = new LazyHtmlClone<E>(
        new Provider<String>() {
          @Override
          public String get() {
            return serializer.merge(item, service);
          }
        }
      );

      @Override
      public E merge(T from, S service) {
        this.service = service;
        item = from;
        try {
          return cloner.get();
        } finally {
          item = null;
          service = null;
        }
      }
    };

    Merger merger = new Merger();
    converter = merger;
    cloner = merger.cloner;
  }

  public LazyHtmlConverter(final MergesValues<T, S, String> serializer, final S service) {
    class Merger implements MergesValues<T, S, E> {

      private T item;
      private final LazyHtmlClone<E> cloner = new LazyHtmlClone<E>(
        new Provider<String>() {
          @Override
          public String get() {
            return serializer.merge(item, service);
          }
        }
      );

      @Override
      public E merge(T from, S service) {
        item = from;
        try {
          return cloner.get();
        } finally {
          item = null;
        }
      }
    };

    Merger merger = new Merger();
    converter = merger;
    cloner = merger.cloner;
  }

  @Override
  public E merge(T from, S service) {
    return converter.merge(from, service);
  }

  public LazyHtmlConverter<T, S, E> setInitializer(ConvertsValue<E, E> initializer) {
    cloner.setInitializer(initializer);
    return this;
  }

}
