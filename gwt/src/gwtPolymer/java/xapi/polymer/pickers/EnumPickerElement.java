package xapi.polymer.pickers;

import elemental.dom.Element;
import elemental.html.InputElement;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.components.api.IsWebComponent;
import xapi.components.api.OnWebComponentAttributeChanged;
import xapi.components.api.ToStringIsName;
import xapi.components.api.WebComponent;
import xapi.components.api.WebComponentMethod;
import xapi.components.impl.JsFunctionSupport;

import static xapi.components.impl.JsSupport.newElement;
import static xapi.components.impl.JsSupport.setAttr;

import java.util.function.Consumer;

@JsType
@WebComponent(tagName=EnumPickerElement.TAG_NAME)
public interface EnumPickerElement <E extends Enum<E>> extends
IsWebComponent<Element>,
AbstractPickerElement<Element>,
OnWebComponentAttributeChanged<Element>
{

  String TAG_NAME = "xapi-enum-picker";
  // This type does not have a default factory, since we want to let subinterfaces define specific factories

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  E getValue();

  @JsProperty
  @WebComponentMethod(mapToAttribute = true)
  void setValue(E property);

  @Override
  default void onCreated(Element e) {
    initializePolymer("paper-radio-group");
  }

  @SuppressWarnings("unchecked")
  @WebComponentMethod(useJsniWildcard=true)
  default void render(E selected, E ... all) {
    Element group = getPolymer().element();
    for (E item : all) {
      // element is not actually an <input/>, but javascript doesn't care
      // and neither do we, since we can treat it just like an input
      InputElement radio = newElement("paper-radio-button");
      radio.setAttribute("name", item instanceof ToStringIsName ? item.toString() : item.name());
      radio.setAttribute("label", item.name());
      setAttr(radio, "toggles");
      group.appendChild(radio);
      radio.addEventListener("core-change", e-> maybeUpdate(radio, item), false);
    }
    setValue(selected);// Initialized the provided value
  }

  @WebComponentMethod(useJsniWildcard=true)
  default void maybeUpdate(InputElement radio, E item) {
    if (radio.isChecked()) {
      setValue(item);
    } else if (item == getValue()) {
      setValue(null);
    }
  }

  default void addAttributeChangeHandler(Consumer<E> handler) {
    Consumer<E> existing = getAttributeChangeHandler();
    if (existing == null) {
      setAttributeChangeHandler(handler);
    } else {
      setAttributeChangeHandler(JsFunctionSupport.mergeConsumer(existing, handler));
    }
  }

  @JsProperty
  Consumer<E> getAttributeChangeHandler();

  @JsProperty
  void setAttributeChangeHandler(Consumer<E> listener);

  @Override
  default void onAttributeChanged(Element e, String name, String oldVal, String newVal) {
    // Used when the user manually sets <xapi-enum-picker>.value="newVal"
    // This will also be called whenever the radio group is updated via clicks,
    // however, this has no effect as the selected variable already equals newVal
    switch (name) {
    case "value":
      getPolymer().element().setAttribute("selected", newVal);
      Consumer<E> handler = getAttributeChangeHandler();
      if (handler != null) {
        handler.accept(getValue());
      }
      break;
    }
  }
}
