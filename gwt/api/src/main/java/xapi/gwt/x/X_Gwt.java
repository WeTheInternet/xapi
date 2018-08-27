package xapi.gwt.x;

import jsinterop.base.Js;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/15/17.
 */
public class X_Gwt {

    private X_Gwt(){}

    @SuppressWarnings("all") // we have done terrible things, to make this slightly less bad
    public static <T> T getUnsafe(Object from, String key) {
        final Object result = Js.asPropertyMap(from).get(key);
        // this assert will always be false, except it does forcible cast the type,
        // which should be enough to grant you some sanity when assertions are enabled
        assert result == null || ((T)result).getClass() != null :
            "Cast failed trying to return " + key + " from " + from;
        // production we will always skip all casts
        return Js.uncheckedCast(result);
    }

    public static boolean hasUnsafe(Object from, String key) {
        return Js.asPropertyMap(from).has(key);
    }

    public static native Object getShadowRoot(Object element)
    /*-{
      if (element.shadowController) {
        return element.shadowController;
      }
      if (element.shadowRoot) {
        return element.shadowRoot;
      }
      if (element.attachShadow) {
        return element.attachShadow({mode: "open"});
      }
      if (element.createShadowRoot) {
        return element.createShadowRoot();
      }
      return element;

    }-*/;
}
