/**
 *
 */
package xapi.test.model.content;

import java.util.TreeMap;

import xapi.collect.api.StringTo;
import xapi.collect.impl.StringToAbstract;
import xapi.model.content.ModelText;
import xapi.model.impl.AbstractModel;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelTextTest extends AbstractModel implements ModelText {

  @Override
  public String getText() {
    return (String) map.get("text");
  }

  @Override
  public ModelText setText(final String text) {
    map.put("text", text);
    return this;
  }

  /**
   * @see xapi.model.content.ModelText#getTime()
   */
  @Override
  public double getTime() {
    final Double time = (Double) map.get("time");
    if (time == null) {
      return 0;
    }
    return time.doubleValue();
  }

  /**
   * @see xapi.model.content.ModelText#setTime(double)
   */
  @Override
  public ModelText setTime(final double time) {
    map.put("time", time);
    return this;
  }

  @Override
  public Class<?> getPropertyType(final String key) {
    switch (key) {
      case "time":
        return double.class;
      case "text":
        return String.class;
    }
    // will throw an exception, unless our supertype was replaced / enhanced
    return super.getPropertyType(key);
  }

  @Override
  public String[] getPropertyNames() {
    return new String[]{"text", "time"};
  }

  @Override
  protected StringTo<Object> newStringMap() {
    // We need to manually enforce ordering for our tests.
    // The order of the iterable of this map must match the order of the keys returned by getPropertyNames();
    return new StringToAbstract<Object>(new TreeMap<String, Object>());
  }

}
