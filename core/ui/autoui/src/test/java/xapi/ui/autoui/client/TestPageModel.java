package xapi.ui.autoui.client;

public class TestPageModel implements TestPage {

  private final User author;
  private final String body;
  private final String id;
  private final String title;

  public TestPageModel(User author, String body, String id, String title) {
    this.author = author;
    this.body = body;
    this.id = title;
    this.title = title;
  }
  
  @Override
  public String id() {
    return id;
  }

  @Override
  public String title() {
    return title;
  }

  @Override
  public String body() {
    return body;
  }

  @Override
  public User author() {
    return author;
  }

}
