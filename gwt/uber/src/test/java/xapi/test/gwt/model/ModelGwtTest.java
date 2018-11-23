package xapi.test.gwt.model;

import xapi.annotation.model.GetterFor;
import xapi.gwt.model.service.ModelServiceGwt;
import xapi.model.X_Model;
import xapi.model.service.ModelService;
import xapi.model.user.ModelUser;
import xapi.test.Assert;

import com.google.gwt.junit.client.GWTTestCase;

public class ModelGwtTest extends GWTTestCase{

  static {
    new ModelServiceGwt();
  }
  private static final String EMAIL = "test@test.com";
  private static final String FIRST_NAME = "Admin";
  private static final String LAST_NAME = "Istrator";
  private static final String KEY = "myProp";
  private static final String VALUE = "myVal";

  public interface ModelGroup extends ModelUser {
    @GetterFor
    String groupName();
    void setGroupName(String groupName);

    ModelUser[] getAdministrators();
    ModelUser[] getUsers();
    ModelGroup setAdministrators(ModelUser[] users);
    void setUsers(ModelUser[] users);
  }

  @Override
  public String getModuleName() {
    return "xapi.X_Uber";
  }

  public void testUserModel() {
    ModelUser user = X_Model.create(ModelUser.class);
    userAssertions(user);
  }

  private void userAssertions(ModelUser user) {
    user.setEmail(EMAIL);
    Assert.assertEquals(EMAIL, user.getEmail());

    user.setFirstName(FIRST_NAME);
    Assert.assertEquals(FIRST_NAME, user.getFirstName());

    user.setLastName(LAST_NAME);
    Assert.assertEquals(LAST_NAME, user.getLastName());

    user.setProperty(KEY, VALUE);
    Assert.assertEquals(user.getProperty(KEY), VALUE);


  }

  private void groupAssertions(ModelGroup group) {
    userAssertions(group);

    ModelUser [] users = new ModelUser[] {
      X_Model.create(ModelUser.class),
      X_Model.create(ModelUser.class),
      X_Model.create(ModelGroup.class)
    };

    group.setUsers(users);
    Assert.assertArrayEquals(group.getUsers(), users);
  }

  public void testGroupModel() {
    ModelGroup group = X_Model.create(ModelGroup.class);
    groupAssertions(group);
  }

}
