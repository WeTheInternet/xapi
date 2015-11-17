package xapi.gwt.junit.gui;

import xapi.elemental.X_Elemental;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.FontFamily.Monospace;
import xapi.ui.html.api.Style;
import xapi.ui.html.api.Style.AlignVertical;
import xapi.ui.html.api.Style.Display;
import xapi.ui.html.api.Style.Overflow;
import xapi.ui.html.api.Style.Unit;
import xapi.ui.html.api.Style.UnitType;

import com.google.gwt.core.client.EntryPoint;

/**
 * Created by james on 16/10/15.
 */
@Css(
    style ={
      @Style(
          names = ".junit",
          fontFamily = Monospace.class,
          fontSize = @Unit(value = 1.1, type = UnitType.Em)
      ),
      @Style(
          names = ".junit.root",
          display = Display.InlineBlock,
          verticalAign = AlignVertical.Top,
          maxHeight = @Unit(value= 90, type = UnitType.Pct),
          overflowY = Overflow.Auto,
          marginRight = @Unit(value = 2, type=UnitType.Em)
      )
    }
)
public abstract class JUnitEntryPoint extends JUnitGui implements EntryPoint {

  @Override
  public void onModuleLoad() {
    X_Elemental.injectCss(JUnitEntryPoint.class);
    runAllTests();
  }


}
