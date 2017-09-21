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
   * @see xapi.model.content.ModelComment#getContentId()
   */
  @Override
  public String getContentId() {
    return getProperty("contentId");
  }

  /**
   * @see xapi.model.content.ModelComment#setContentId(java.lang.String)
   */
  @Override
  public ModelComment setContentId(final String ContentId) {
    setProperty("contentId", ContentId);
    return this;
  }

  /**
   * @see xapi.test.model.content.ModelContentTest#getPropertyNames()
   */
  @Override
  public String[] getPropertyNames() {
    return new String[]{"downvotes", "contentId", "children", "related", "text", "time", "upvotes"};
  }

  /**
   * @see xapi.test.model.content.ModelContentTest#getPropertyType(java.lang.String)
   */
  @Override
  public Class<?> getPropertyType(final String key) {
    if ("ContentId".equals(key)) {
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
