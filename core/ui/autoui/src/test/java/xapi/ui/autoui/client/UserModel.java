package xapi.ui.autoui.client;

public class UserModel implements User {

  private final String email;
  private final String id;
  private final String name;

  public UserModel(String email, String id, String name) {
    this.email = email;
    this.id = id;
    this.name = name;
  }
  
  @Override
  public String id() {
    return id;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public String email() {
    return email;
  }

}
