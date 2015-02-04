/**
 *
 */
package xapi.polymer.core;

import static xapi.components.impl.JsSupport.addClassName;
import static xapi.components.impl.JsSupport.findInShadowRoot;
import static xapi.components.impl.JsSupport.newDivWithHtml;
import static xapi.components.impl.JsSupport.setAttr;
import static xapi.elemental.X_Elemental.getElementalService;
import static xapi.ui.html.api.Style.Overflow.Auto;
import static xapi.ui.html.api.Style.Position.Relative;
import static xapi.ui.html.api.Style.UnitType.Px;

import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;
import com.google.gwt.core.shared.GWT;

import elemental.dom.Element;
import elemental.html.DivElement;

import xapi.annotation.common.Property;
import xapi.components.api.IsWebComponent;
import xapi.components.api.OnWebComponentCreated;
import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentFactory;
import xapi.components.impl.WebComponentWithCallbacks;
import xapi.ui.html.X_Html;
import xapi.ui.html.api.Css;
import xapi.ui.html.api.El;
import xapi.ui.html.api.Html;
import xapi.ui.html.api.Style;
import xapi.ui.html.api.Style.Display;
import xapi.ui.html.api.Style.Rgb;
import xapi.ui.html.api.Style.Unit;
import xapi.ui.html.api.Style.UnitType;

@JsType
@WebComponent(tagName=PaperHeaderPanel.TAG_NAME)
@Css(
  style= {
  @Style(
    names="."+PaperHeaderPanel.CLASS_NAME+" core-header-panel",
    display=Display.InlineBlock,
    position=Relative,
    overflow=Auto,
    width=@Unit(value=700),
    minHeight=@Unit(value=700, type=Px)
  )
  }
)
@Html(
  document="core-header-panel",
  renderOrder= {
    "getHeaderPanel", "getContentPanel"
  }
)
public interface PaperHeaderPanel extends
IsWebComponent<Element>,
OnWebComponentCreated<Element>,
WebComponentWithCallbacks<Element> {

  WebComponentFactory<PaperHeaderPanel> NEW_HEADER_PANEL = GWT.create(PaperHeaderPanel.class);

  String TAG_NAME = "xapi-header-panel";
  String CLASS_NAME = "xapi-header";

  @JsProperty
  @El(
    className="core-header",
    properties= {
      @Property(name="horizontal"),
      @Property(name="layout"),
      @Property(name="center"),
    },
    style= {
    @Style(
      names=".core-header",
      properties=@Property(name="background", value="radial-gradient(ellipse at center, #1e5799 0%,#2989d8 57%,#7db9e8 100%)"),
      borderRadius= {@Unit(value=3), @Unit(value=3), @Unit(value=0), @Unit(value=0)}
    ),
    @Style(
      names=".core-header paper-button",
      marginRight=@Unit(value=0)
    ),
    @Style(
      names=".core-header .header-text",
      color=@Rgb(r=0xff,g=0xff,b=0xff),
      marginLeft=@Unit(value=1, type=UnitType.Em)
    )
    }
  )
  Element getHeaderPanel();

  @JsProperty
  void setHeaderPanel(Element header);

  @JsProperty
  Element getContainerPanel();

  @JsProperty
  void setContainerPanel(Element container);

  @JsProperty
  @El(
    className="content",
    style=@Style(
        names="."+CLASS_NAME+ " .content",
        overflowX=Auto
    )
  )
  Element getContentPanel();

  @JsProperty
  void setContentPanel(Element content);

  default void setHeaderText(String text) {
    DivElement header = newDivWithHtml(text);
    addClassName(header, "header-text");
    setAttr(header, "flex");
    getHeaderPanel().appendChild(header);
  }

  default Element getScrollContainer() {
    Element container = getContainerPanel();
    return findInShadowRoot(container, "mainContainer");
  }

  @Override
  default void onCreated(Element e) {
    addClassName(e, CLASS_NAME);
    e.setInnerHTML(X_Html.toHtml(PaperHeaderPanel.class, this, getElementalService()));
    setContentPanel(e.querySelector(".content"));
    setHeaderPanel(e.querySelector(".core-header"));
    setContainerPanel(e.getFirstElementChild());
  }

}
