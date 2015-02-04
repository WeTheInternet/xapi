package xapi.polymer.core;

import static xapi.ui.html.api.Style.AlignVertical.Middle;
import static xapi.ui.html.api.Style.Display.Block;
import static xapi.ui.html.api.Style.Display.InlineBlock;
import static xapi.ui.html.api.Style.Position.Relative;
import static xapi.ui.html.api.Style.UnitType.Auto;
import static xapi.ui.html.api.Style.UnitType.Em;
import static xapi.ui.html.api.Style.UnitType.Px;

import xapi.ui.html.api.Css;
import xapi.ui.html.api.FontFamily;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.Style;
import xapi.ui.html.api.Style.Rgb;
import xapi.ui.html.api.Style.Unit;

@Html(
  css=@Css(
    style={
      @Style(
        names=".xapi",
        fontFamily=PolymerTheme.class,
        display=Block,
        position=Relative,
        width=@Unit(value=650, type=Px),
        marginBottom=@Unit(value=1, type=Em)
      ),
      @Style(
        names=".xapi .core-label"
      ),
      @Style(
        names=".xapi .label",
        width=@Unit(value=26, type=Em),
        marginRight=@Unit(value=1, type=Em),
        fontSize=@Unit(value=0.8, type=Em),
        display=InlineBlock,
        verticalAign=Middle
      ),
      @Style(
        names=".xapi .polymer",
        verticalAign=Middle,
        margin=@Unit(type=Auto)
      ),
      @Style(
        names=".xapi h3",
        fontSize=@Unit(value=1.1, type=Em),
        margin=@Unit(0)
      ),
      @Style(
        names=".xapi-button",
        backgroundColor=@Rgb(r=0,g=0,b=0xff),
        color=@Rgb(r=0xff,g=0xff,b=0xff)
      ),
    }
  )
)
public interface PolymerTheme extends FontFamily {

  default String name() {
    return "RobotoDraft, 'Helvetica Neue', Helvetica, Arial";
  }

}
