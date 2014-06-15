package xapi.ui.html.api;

public interface FontFamily {

  String name();

  public static class Cursive implements FontFamily{
    @Override
    public String name() {
      return "Fondamento, cursive";
    }
  }

  public static class Monospace implements FontFamily{
    @Override
    public String name() {
      return "monospace, courier new, courier";
    }
  }

  public static class Serif implements FontFamily{
    @Override
    public String name() {
      return "Junge, times, times new roman";
    }
  }

  public static class SansSerif implements FontFamily{
    @Override
    public String name() {
      return "arial, helvetica";
    }
  }

  public static class Impact implements FontFamily{
    @Override
    public String name() {
      return "Merienda One, intro, impact, bookman, arial black";
    }
  }

}
