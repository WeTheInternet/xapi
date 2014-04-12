package xapi.ui.html.api;

public @interface Style {

  public static @interface Unit {
    UnitType type() default UnitType.Px;
    double value() default 1;
  }

  public static enum AlignHorizontal {
    Left, Center, Right, Justify, Auto;
  }
  
  public static enum AlignVertical {
    Top, Middle, Bottom, Auto;
  }
  
  public static enum Overflow {
    None, Scroll, Auto, Inherit;
  }
  
  public static enum FontWeight {
    Bold, Bolder, Normal, Inherit;
  }
  
  public static enum UnitType {
    Px, Pct, Em, Auto;
  }
  
  public static enum Display {
    None, Block, Inline, InlineBlock, Inherit;
  }
  
  public static enum Position {
    Static, Relative, Absolute, Fixed, Sticky(Position.Fixed), Inherit;

    private final Position fallback;
    
    private Position(){fallback = this;}
    
    private Position(Position fallback) {
      this.fallback = fallback;
    }
    Position getFallback() {
      return fallback;
    }
  }
  
  String[] names() default {};
  
  Display display() default Display.Block;
  
  Position position() default Position.Static;
  
  FontWeight fontWeight() default FontWeight.Normal;
  
  AlignHorizontal textAlign() default AlignHorizontal.Justify;
  
  AlignVertical verticalAign() default AlignVertical.Top;
  
  double opacity() default 1;
  
  Class<? extends FontFamily> fontFamily() default FontFamily.Serif.class;
  
  Unit fontSize() default @Unit(value=1, type=UnitType.Em);

  Unit lineHeight() default @Unit(value=1.1, type=UnitType.Em);
  
  Unit[] padding() default {};
  
  Unit[] margin() default {};
  
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
  
}
