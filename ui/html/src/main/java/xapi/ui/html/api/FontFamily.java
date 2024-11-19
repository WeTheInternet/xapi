package xapi.ui.html.api;

public interface FontFamily {

  String name();

  interface HasGoogleFont {
    String googleFont();
  }

  class Cursive implements FontFamily{
    @Override
    public String name() {
      return "Fondamento, cursive";
    }
  }

  class Monospace implements FontFamily{
    @Override
    public String name() {
      return "monospace, courier new, courier";
    }
  }

  class Serif implements FontFamily{
    @Override
    public String name() {
      return "Raleway, Junge, times, times new roman";
    }
  }

  class SansSerif implements FontFamily{
    @Override
    public String name() {
      return "Work Sans, arial, helvetica";
    }
  }

  class Impact implements FontFamily{
    @Override
    public String name() {
      return "Junge, times, bookman, arial black";
//      return "Merienda One, intro, impact, bookman, arial black";
    }
  }

}
