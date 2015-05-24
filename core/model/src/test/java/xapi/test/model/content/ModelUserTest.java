/**
 *
 */
package xapi.test.model.content;

import xapi.model.impl.AbstractModel;
import xapi.model.user.ModelUser;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public class ModelUserTest extends AbstractModel implements ModelUser {

  public ModelUserTest() {
    super("user");
  }

  /**
   * @see xapi.model.user.ModelUser#id()
   */
  @Override
  public String id() {
    return getKey().getId();
  }

  /**
   * @see xapi.model.user.ModelUser#getEmail()
   */
  @Override
  public String getEmail() {
    return getProperty("email");
  }

  /**
   * @see xapi.model.user.ModelUser#getFirstName()
   */
  @Override
  public String getFirstName() {
    return getProperty("firstName");
  }

  /**
   * @see xapi.model.user.ModelUser#getLastName()
   */
  @Override
  public String getLastName() {
    return getProperty("lastName");
  }

  /**
   * @see xapi.model.user.ModelUser#setEmail(java.lang.String)
   */
  @Override
  public ModelUser setEmail(final String email) {
    setProperty("email", email);
    return this;
  }

  /**
   * @see xapi.model.user.ModelUser#setFirstName(java.lang.String)
   */
  @Override
  public ModelUser setFirstName(final String firstName) {
    setProperty("firstName", firstName);
    return this;
  }

  /**
   * @see xapi.model.user.ModelUser#setLastName(java.lang.String)
   */
  @Override
  public ModelUser setLastName(final String lastName) {
    setProperty("lastName", lastName);
    return this;
  }

  /**
   * @see xapi.model.user.ModelUser#setId(java.lang.String)
   */
  @Override
  public ModelUser setId(final String id) {
    getKey().setId(id);
    return this;
  }

  /**
   * @see xapi.model.user.ModelUser#isValid()
   */
  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public String[] getPropertyNames() {
    return new String[]{"email", "firstName", "lastName"};
  }

  @Override
  public Class<?> getPropertyType(final String key) {
    switch (key) {
      case "email":
      case "firstName":
      case "lastName":
        return String.class;
    }
    return super.getPropertyType(key);
  }

}
