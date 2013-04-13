package xapi.source.api;

public interface HasFields {

  Iterable<IsField> getFields();
  IsField getField(String name);

}
