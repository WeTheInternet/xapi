package xapi.gwt.model;

import xapi.fu.itr.MappedIterable;
import xapi.model.impl.AbstractModel;

import java.util.Map.Entry;

public class ModelGwt extends AbstractModel {

  @Override
  public MappedIterable<Entry<String, Object>> getProperties() {
    // Because we know all generated Gwt modules MUST implement property names,
    // We can safely enforce serialization ordering using .getPropertyNames()
    final String[] names = getPropertyNames();
    return new Itr(names);
  }

  @Override
  public Class<?> getPropertyType(final String key) {
    throw new UnsupportedOperationException("Type "+getClass()+" does not handle property type "+key);
  }

  @Override
  public String[] getPropertyNames() {
    return new String[0];
  }
}
