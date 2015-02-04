package xapi.polymer.pickers;

import static com.google.gwt.regexp.shared.RegExp.quote;

import static xapi.components.impl.JsSupport.getString;
import static xapi.components.impl.JsSupport.newElement;
import static xapi.components.impl.JsSupport.not;
import static xapi.components.impl.JsSupport.removeFromParent;
import static xapi.components.impl.JsSupport.setObject;

import java.util.function.Consumer;
import java.util.function.Function;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;
import elemental.js.util.JsArrayOfString;
import elemental.js.util.JsMapFromStringTo;
import elemental.util.MapFromStringTo;

import xapi.components.api.IsWebComponent;
import xapi.components.api.OnWebComponentAttributeChanged;
import xapi.components.api.OnWebComponentCreated;
import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentFactory;
import xapi.components.api.WebComponentMethod;
import xapi.polymer.core.PolymerElement;


@JsType
@WebComponent(tagName=ListItemElement.TAG_NAME)
public interface ListItemElement extends
IsWebComponent<Element>,
OnWebComponentAttributeChanged,
OnWebComponentCreated<Element>,
AbstractPickerElement<Element> {

  String TAG_NAME = "xapi-item-list";
  WebComponentFactory<ListItemElement> NEW_LIST_ITEM = GWT.create(ListItemElement.class);

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  JsArrayOfString getValue();

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  void setValue(JsArrayOfString property);

  default void setValueFromString(String properties) {
    element().setAttribute("value", properties);
  }

  default String[] valueArray() {
    JsArrayOfString arr = getValue();
    String[] value = new String[arr.length()];
    MapFromStringTo<Element> map = getItemMap();
    for (int i = arr.length(); i-->0; ){
      value[i] = getString(map.get(arr.get(i)), "originalValue");
    }
    return value;
  }

  @JsProperty
  String getJoiner();

  @JsProperty
  ListItemElement setJoiner(String joiner);

  @JsProperty
  boolean getRemovable();

  @JsProperty
  ListItemElement setRemovable(boolean allowRemove);

  @JsProperty
  MapFromStringTo<Element> getItemMap();

  @JsProperty
  ListItemElement setItemMap(MapFromStringTo<Element> itemMap);

  default MapFromStringTo<Element> initItemMap() {
    MapFromStringTo<Element> map = getItemMap();
    if (map == null) {
      map = JsMapFromStringTo.create();
      setItemMap(map);
    }
    return map;
  }

  @Override
  default void onCreated(Element element) {
    initializePolymer("paper-shadow");
  }

  @JsProperty
  Function<String, Element> itemBuilder();

  @JsProperty
  ListItemElement itemBuilder(Function<String, Element> builder);

  @JsProperty
  Consumer<Element> onItemBuilt();

  @JsProperty
  ListItemElement onItemBuilt(Consumer<Element> callback);

  default Element newItemElement(String bit) {
    Function<String, Element> builder = itemBuilder();
    Element item;
    if (builder == null) {
      item = newElement("paper-item");
      item.setInnerText(bit);
    } else {
      item = builder.apply(bit);
    }
    if (getRemovable()) {
      PolymerElement remover = PolymerElement.newIcon("close");
      remover.onClick(e->{
        removeFromParent(item);
        Element el = element();
        String joiner = getJoiner();
        if (joiner == null) {
          joiner = "  ";
        }
        String oldVal = el.getAttribute("value");
        String newVal = (joiner+oldVal+joiner).replaceFirst(joiner+bit+joiner, "");
        el.setAttribute("value", newVal);
      });
      item.appendChild(remover.element());
    }
    setObject(item, "originalValue", bit);
    Consumer<Element> callback = onItemBuilt();
    if (callback != null) {
      callback.accept(item);
    }
    return item;
  }

  default void addValue(String item) {
    String joiner = getJoiner();
    if (joiner == null) {
      joiner = "  ";
      String escaped = item.replaceAll("([ ][ ]+)", " ");

      MapFromStringTo<Element> map = initItemMap();
      map.put(escaped, newItemElement(item));
      item = escaped;
    }
    Element ele = element();
    String before = ele.getAttribute("value");
    if (not(before)) {
      ele.setAttribute("value", item);
    } else {
      ele.setAttribute("value", before+joiner+item);
    }
  }

  @Override
  default void onAttributeChanged(String name, String oldVal, String newVal) {
    MapFromStringTo<Element> map;
    switch (name) {
    case "value":
      map = getItemMap();
      if (newVal == null) {
        setItemMap(null);
        getPolymer().element().setInnerHTML("");
        return;
      }
      if (map == null) {
        map = JsMapFromStringTo.create();
        setItemMap(map);
      }
      String joiner = getJoiner();
      if (joiner == null) {
        joiner = "  ";
      }
      String[] bits = newVal.split(quote(joiner));

      JsArrayOfString value = JsArrayOfString.create();
      getPolymer().setInnerHTML("");
      for (String bit : bits) {
        if (bit.length() == 0) {
          continue;
        }
        value.push(bit);
        Element existing = map.get(bit);
        if (existing == null) {
          existing = newItemElement(bit);
        }
        getPolymer().appendChild(existing);
      }
      // TODO maintain selection state
      break;
    }
  }

}
