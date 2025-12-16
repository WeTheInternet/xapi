package xapi.dev.ui.api;

import net.wti.lang.parser.ast.Node;
import xapi.source.X_Source;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/22/17.
 */
public interface UiConstants {
    String EXTRA_SYNTHENTIC = "__synthed__";
    String EXTRA_GENERATE_MODE = "__gen_mode__";
    String EXTRA_MODEL_INFO = "__model_info__";
    String EXTRA_ASSEMBLED_ELEMENT = "__ae__";
    String EXTRA_RESOURCE_PATH = "__path__";
    String EXTRA_FILE_NAME = "__file_name__";
    String EXTRA_FOR_LOOP_VAR = "__for_var__";
    String EXTRA_LOCATION = "location";
    String EXTRA_INSTALL_METHOD = "__install__";

    static Object location(Node el, Class<?> cls) {
        final Node parent = el.getParentNode();
        String location = el.<String>findExtra(EXTRA_LOCATION)
            .ifAbsentSupply(()-> {
                String loc = parent.getExtra(EXTRA_FILE_NAME);
                if (loc != null) {
                    loc += ".xapi";
                    String path = parent.getExtra(EXTRA_RESOURCE_PATH);
                    if (path != null) {
                        return path.replace('.', '/') + "/" + loc;
                    }
                }
                return null;
            });
        return location == null ? cls : X_Source.pathToLogLink(location, el.getBeginLine());
    }
}
