/**
 *
 */
package xapi.test.model.content;

import xapi.model.content.HasAuthor;
import xapi.model.content.ModelContent;
import xapi.model.content.ModelRating;
import xapi.model.user.ModelUser;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelContentTest extends ModelTextTest implements ModelContent {

  /**
   * @see xapi.model.content.HasVotes#getUpvotes()
   */
  @Override
  public ModelRating[] getUpvotes() {
    return getProperty("upvotes");
  }

  /**
   * @see xapi.model.content.HasVotes#setUpvotes(xapi.model.content.ModelRating[])
   */
  @Override
  public ModelContent setUpvotes(final ModelRating[] upvotes) {
    setProperty("upvotes", upvotes);
    return this;
  }

  /**
   * @see xapi.model.content.HasVotes#getDownvotes()
   */
  @Override
  public ModelRating[] getDownvotes() {
    return getProperty("downvotes");
  }

  /**
   * @see xapi.model.content.HasVotes#setDownvotes(xapi.model.content.ModelRating[])
   */
  @Override
  public ModelContent setDownvotes(final ModelRating[] upvotes) {
    setProperty("upvotes", upvotes);
    return this;
  }

  /**
   * @see xapi.model.content.ModelContent#getRelated()
   */
  @Override
  public ModelContent[] getRelated() {
    return getProperty("related");
  }

  /**
   * @see xapi.model.content.ModelContent#setRelated(xapi.model.content.ModelContent[])
   */
  @Override
  public ModelContent setRelated(final ModelContent[] related) {
    setProperty("related", related);
    return this;
  }

  /**
   * @see xapi.model.content.ModelContent#getChildren()
   */
  @Override
  public ModelContent[] getChildren() {
    return getProperty("children");
  }

  /**
   * @see xapi.model.content.ModelContent#setChildren(xapi.model.content.ModelContent[])
   */
  @Override
  public ModelContent setChildren(final ModelContent[] children) {
    setProperty("children", children);
    return this;
  }

  @Override
  public String getPermaLink() {
    return getProperty("permalink");
  }

  @Override
  public ModelContent setPermaLink(String permalink) {
    setProperty("permalink", permalink);
    return this;
  }


  @Override
  public Class<?> getPropertyType(final String key) {
    switch (key) {
      case "downvotes":
      case "upvotes":
        return ModelRating[].class;
      case "related":
        return ModelContent[].class;
    }
    return super.getPropertyType(key);
  }

  @Override
  public String[] getPropertyNames() {
    return new String[]{"related", "text", "time", "downvotes", "upvotes"};
  }

  @Override
  public ModelUser getAuthor() {
    return null;
  }

  @Override
  public HasAuthor setAuthor(ModelUser user) {
    return null;
  }
}
