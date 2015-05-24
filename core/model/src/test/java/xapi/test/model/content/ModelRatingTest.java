/**
 *
 */
package xapi.test.model.content;

import xapi.model.content.ModelRating;
import xapi.model.impl.AbstractModel;
import xapi.model.user.ModelUser;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelRatingTest extends AbstractModel implements ModelRating {

  /**
   * @see xapi.model.content.HasAuthor#getAuthor()
   */
  @Override
  public ModelUser getAuthor() {
    return getProperty("author");
  }

  /**
   * @see xapi.model.content.HasAuthor#setAuthor(xapi.model.user.ModelUser)
   */
  @Override
  public ModelRating setAuthor(final ModelUser user) {
    setProperty("author", user);
    return this;
  }

  /**
   * @see xapi.model.content.ModelRating#getRating()
   */
  @Override
  public double getRating() {
    if (map.containsKey("rating")) {
      final Double d = (Double) map.get("rating");
      return d.doubleValue();
    } else {
      return 0;
    }
  }

  /**
   * @see xapi.model.content.ModelRating#setRating(double)
   */
  @Override
  public ModelRating setRating(final double rating) {
    setProperty("rating", rating);
    return this;
  }

  /**
   * @see xapi.model.impl.AbstractModel#getPropertyNames()
   */
  @Override
  public String[] getPropertyNames() {
    return new String[] {"author", "rating"};
  }

  /**
   * @see xapi.model.impl.AbstractModel#getPropertyType(java.lang.String)
   */
  @Override
  public Class<?> getPropertyType(final String key) {
    switch (key) {
      case "author":
        return ModelUser.class;
      case "rating":
        return double.class;
    }
    return super.getPropertyType(key);
  }

}
