package xapi.ui.html.api;

import xapi.annotation.common.Property;
import xapi.ui.api.Stylizer;

public @interface Style {

  @interface Unit {
    UnitType type() default UnitType.Px;
    double value() default 1;
    boolean important() default false;
  }

  @interface Transition {
    double time() default 0.25;
    String unit() default "s";
    String value();
  }

  @interface Color {
    String asString() default "";
    Rgb asRgb() default @Rgb(r=0,g=0,b=0);
  }

  @interface Rgb {
    int r();
    int g();
    int b();
    int opacity() default 0xff;
  }

  enum AlignHorizontal {
    Left("left"), Center("center"), Right("right"), Justify("justify"), Auto("auto");
    String styleName;
    AlignHorizontal(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  enum AlignVertical {
    Top("top"), Middle("middle"), Bottom("bottom"), Auto("auto");
    String styleName;
    AlignVertical(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  enum Cursor {
    Pointer("pointer"), Progress("progress"), Crosshair("crosshair"),
    ResizeN("n-resize"), ResizeE("e-resize"), ResizeS("s-resize"), ResizeW("w-resize"),
    ResizeNE("ne-resize"), ResizeSE("se-resize"), ResizeSW("sw-resize"), ResizeNW("nw-resize"),
    ResizeEW("ew-resize"), ResizeNS("ns-resize"),
    Help("help"), Move("move"), Default("default"), Inherit("inherit");
    String styleName;
    Cursor(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  enum Overflow {
    Hidden("hidden"), Scroll("scroll"), Auto("auto"), Inherit("inherit");
    String styleName;
    Overflow(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  enum FontStyle {
    Italic("italic"), Oblique("oblique"), Normal("normal"), Inherit("inherit");
    String styleName;
    FontStyle(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  enum FontWeight {
    Bold("bold"), Bolder("bolder"), Normal("normal"), Inherit("inherit");
    String styleName;
    FontWeight(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  enum Floats {
    Left("left"), Right("right"), None("none"), Auto("auto");
    String styleName;
    Floats(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  enum Clear {
    Left("left"), Right("right"), None("none"), Both("both"), Auto("auto");
    String styleName;
    Clear(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  enum UnitType {
    Px, Pct, Em, Auto, Unset
  }

  enum BoxSizing {
    BorderBox("border-box"), ContentBox("content-box"), PaddingBox("padding-box"), Inherit("inherit");
    String styleName;
    BoxSizing(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  enum BorderStyle {
    None, Hidden, Dotted, Dashed, Solid, Double,
    Groove, Ridge, Inset, Outset, Initial, Inherit;
    public String styleName() {
      return name().toLowerCase();
    }
  }

  enum Display {
    None("none"), Block("block"), Inline("inline"),
    InlineBlock("inline-block"), Inherit("inherit"),
    Table("table"), TableRow("table-row"), TableColumn("table-column"),
    TableCaption("table-caption"), TableCell("table-cell"),
    TableHeaderGroup("table-header-group"), TableFooterGroup("table-footer-group"),
    TableRowGroup("table-row-group"), TableColumnGroup("table-column-group"),
    Flex("flex"), InlineFlex("inline-flex")
    ;
    String styleName;
    Display(String styleName) {
      this.styleName = styleName;
    }
    public String styleName() {
      return styleName;
    }
  }

  enum Position {
    Static("static"), Relative("relative"), Absolute("absolute"),
    Fixed("fixed"), Sticky("sticky",Position.Fixed), Inherit("inherited");

    private final Position fallback;
    private final String styleName;

    Position(String styleName){
      this.styleName = styleName;
      fallback = this;}

    Position(String styleName, Position fallback) {
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

  Unit fontSize() default @Unit(type=UnitType.Unset);

  Unit lineHeight() default @Unit(type=UnitType.Unset);

  Unit[] padding() default {};
  Unit paddingLeft() default @Unit(type=UnitType.Unset);
  Unit paddingRight() default @Unit(type=UnitType.Unset);
  Unit paddingTop() default @Unit(type=UnitType.Unset);
  Unit paddingBottom() default @Unit(type=UnitType.Unset);

  Unit[] margin() default {};
  Unit marginLeft() default @Unit(type=UnitType.Unset);
  Unit marginRight() default @Unit(type=UnitType.Unset);
  Unit marginTop() default @Unit(type=UnitType.Unset);
  Unit marginBottom() default @Unit(type=UnitType.Unset);

  Unit left() default @Unit(type=UnitType.Unset);
  Unit right() default @Unit(type=UnitType.Unset);
  Unit top() default @Unit(type=UnitType.Unset);
  Unit bottom() default @Unit(type=UnitType.Unset);
  Unit width() default @Unit(type=UnitType.Unset);
  Unit height() default @Unit(type=UnitType.Unset);
  Unit maxWidth() default @Unit(type=UnitType.Unset);
  Unit maxHeight() default @Unit(type=UnitType.Unset);
  Unit minWidth() default @Unit(type=UnitType.Unset);
  Unit minHeight() default @Unit(type=UnitType.Unset);

  Rgb[] borderColor() default {};
  BorderStyle[] borderStyle() default {};
  Unit[] borderWidth() default {};
  Unit borderWidthLeft() default @Unit(type=UnitType.Unset);
  Unit borderWidthRight() default @Unit(type=UnitType.Unset);
  Unit borderWidthTop() default @Unit(type=UnitType.Unset);
  Unit borderWidthBottom() default @Unit(type=UnitType.Unset);
  Unit[] borderRadius() default {};

  Overflow overflow() default Overflow.Inherit;
  Overflow overflowX() default Overflow.Inherit;
  Overflow overflowY() default Overflow.Inherit;

  Property[] properties() default {};
  Class<? extends Stylizer<?>>[] stylizers() default {};
  /**
   * The insertion-order priority in which to insert the style.
   *
   * A best effort will be made to ensure style injected at different priorities will be
   * appended to an ordered set of style tags in the document's head,
   * but the only solid guarantee is that all style inserted in a single javascript
   * event loop will be in prioritized order.
   *
   */
  int priority() default 0;
}
