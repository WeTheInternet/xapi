/**
 *
 */
package xapi.test.model.content;

import xapi.model.content.ModelComment;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelCommentTest extends ModelContentTest implements ModelComment {

  /**
   * @see xapi.model.content.ModelComment#getHref()
   */
  @Override
  public String getHref() {
    return getProperty("href");
  }

  /**
   * @see xapi.model.content.ModelComment#setHref(java.lang.String)
   */
  @Override
  public ModelComment setHref(final String href) {
    setProperty("href", href);
    return this;
  }

  /**
   * @see xapi.test.model.content.ModelContentTest#getPropertyNames()
   */
  @Override
  public String[] getPropertyNames() {
    return new String[]{"downvotes", "href", "related", "text", "time", "upvotes"};
  }

  /**
   * @see xapi.test.model.content.ModelContentTest#getPropertyType(java.lang.String)
   */
  @Override
  public Class<?> getPropertyType(final String key) {
    if ("href".equals(key)) {
      return String.class;
    }
    return super.getPropertyType(key);
  }

  /**
   * @see xapi.model.impl.AbstractModel#getType()
   */
  @Override
  public String getType() {
    return "comment";
  }
}
