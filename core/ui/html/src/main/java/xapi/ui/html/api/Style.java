package xapi.ui.html.api;

public @interface Style {

  public static @interface Unit {
    UnitType type() default UnitType.Px;
    double value() default 1;
  }

  public static @interface Transition {
    double time() default 0.25;
    String unit() default "s";
    String value();
  }

  public static @interface Rgb {
    int r();
    int g();
    int b();
    int opacity() default 0xff;
  }

  public static enum AlignHorizontal {
    Left("left"), Center("center"), Right("right"), Justify("justify"), Auto("auto");
    String styleName;
    private AlignHorizontal(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  public static enum AlignVertical {
    Top("top"), Middle("middle"), Bottom("bottom"), Auto("auto");
    String styleName;
    private AlignVertical(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  public static enum Cursor {
    Pointer("pointer"), Progress("progress"), Crosshair("crosshair"),
    ResizeN("n-resize"), ResizeE("e-resize"), ResizeS("s-resize"), ResizeW("w-resize"),
    ResizeNE("ne-resize"), ResizeSE("se-resize"), ResizeSW("sw-resize"), ResizeNW("nw-resize"),
    ResizeEW("ew-resize"), ResizeNS("ns-resize"),
    Help("help"), Move("move"), Default("default"), Inherit("inherit");
    String styleName;
    private Cursor(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  public static enum Overflow {
    Hidden("hidden"), Scroll("scroll"), Auto("auto"), Inherit("inherit");
    String styleName;
    private Overflow(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  public static enum FontStyle {
    Italic("italic"), Oblique("oblique"), Normal("normal"), Inherit("inherit");
    String styleName;
    private FontStyle(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  public static enum FontWeight {
    Bold("bold"), Bolder("bolder"), Normal("normal"), Inherit("inherit");
    String styleName;
    private FontWeight(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  public static enum Floats {
    Left("left"), Right("right"), None("none"), Auto("auto");
    String styleName;
    private Floats(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  public static enum Clear {
    Left("left"), Right("right"), None("none"), Both("both"), Auto("auto");
    String styleName;
    private Clear(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  public static enum UnitType {
    Px, Pct, Em, Auto;
  }

  public static enum BoxSizing {
    BorderBox("border-box"), ContentBox("content-box"), PaddingBox("padding-box"), Inherit("inherit");
    String styleName;
    private BoxSizing(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  public static enum Display {
    None("none"), Block("block"), Inline("inline"),
    InlineBlock("inline-block"), Inherit("inherit");
    String styleName;
    private Display(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  public static enum Position {
    Static("static"), Relative("relative"), Absolute("absolte"),
    Fixed("fixed"), Sticky("sticky",Position.Fixed), Inherit("inheritd");

    private final Position fallback;
    private final String styleName;

    private Position(String styleName){
      this.styleName = styleName;
      fallback = this;}

    private Position(String styleName, Position fallback) {
      this.styleName = styleName;
      this.fallback = fallback;
    }
    public String styleName() {
      return styleName;
    }
    Position getFallback() {
      return fallback;
    }
  }

  String[] names() default {};

  BoxSizing boxSizing() default BoxSizing.Inherit;

  Display display() default Display.Inherit;

  Position position() default Position.Inherit;

  FontStyle fontStyle() default FontStyle.Inherit;

  FontWeight fontWeight() default FontWeight.Inherit;

  AlignHorizontal textAlign() default AlignHorizontal.Auto;

  AlignVertical verticalAign() default AlignVertical.Auto;

  double opacity() default 1;

  Class<? extends FontFamily>[] fontFamily() default {};

  Floats floats() default Floats.Auto;

  Clear clear() default Clear.Auto;

  Cursor cursor() default Cursor.Inherit;

  Rgb[] color() default {};
  Rgb[] backgroundColor() default {};

  Transition[] transition() default {};

  Unit fontSize() default @Unit(type=UnitType.Auto);

  Unit lineHeight() default @Unit(type=UnitType.Auto);

  Unit[] padding() default {};
  Unit paddingLeft() default @Unit(type=UnitType.Auto);
  Unit paddingRight() default @Unit(type=UnitType.Auto);
  Unit paddingTop() default @Unit(type=UnitType.Auto);
  Unit paddingBottom() default @Unit(type=UnitType.Auto);

  Unit[] margin() default {};
  Unit marginLeft() default @Unit(type=UnitType.Auto);
  Unit marginRight() default @Unit(type=UnitType.Auto);
  Unit marginTop() default @Unit(type=UnitType.Auto);
  Unit marginBottom() default @Unit(type=UnitType.Auto);

  Unit left() default @Unit(type=UnitType.Auto);
  Unit right() default @Unit(type=UnitType.Auto);
  Unit top() default @Unit(type=UnitType.Auto);
  Unit bottom() default @Unit(type=UnitType.Auto);
  Unit width() default @Unit(type=UnitType.Auto);
  Unit height() default @Unit(type=UnitType.Auto);
  Unit maxWidth() default @Unit(type=UnitType.Auto);
  Unit maxHeight() default @Unit(type=UnitType.Auto);
  Unit minWidth() default @Unit(type=UnitType.Auto);
  Unit minHeight() default @Unit(type=UnitType.Auto);

  Overflow overflow() default Overflow.Inherit;
  Overflow overflowX() default Overflow.Inherit;
  Overflow overflowY() default Overflow.Inherit;

  Class<? extends Stylizer<?>>[] stylizers() default {};
}
