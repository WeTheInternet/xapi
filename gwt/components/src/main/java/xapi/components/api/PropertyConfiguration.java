package xapi.components.api;

import elemental2.core.JsObject;
import elemental2.core.Reflect;
import jsinterop.base.Js;
import xapi.fu.In1Out1;
import xapi.gwt.api.JsObjectDescriptor;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/13/17.
 */
public class PropertyConfiguration {

    private final String name;
    private final JsObject target;
    private JsObjectDescriptor definition;

    public PropertyConfiguration(JsObject proto, String name) {
        this(proto, proto, name);
    }

    public PropertyConfiguration(JsObject sourceProto, JsObject targetProto, String name) {
        this.name = name;
        this.target = targetProto;
        this.definition = Js.uncheckedCast(Reflect.getOwnPropertyDescriptor(sourceProto, name));
    }

    public String getName() {
        return name;
    }

    public void mutate(In1Out1<JsObjectDescriptor, JsObjectDescriptor> mapper) {
        Object res = mapper.io(definition);
        final JsObjectDescriptor result = Js.uncheckedCast(res);
        if (result != definition) {
            definition = result;
//            JsObject.defineProperty(target, name, Js.uncheckedCast(result));
            Reflect.defineProperty(target, name, Js.uncheckedCast(result));
        }
    }

}
