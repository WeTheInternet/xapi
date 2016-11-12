package xapi.server.api;

import com.github.javaparser.ast.expr.UiContainerExpr;
import xapi.model.api.Model;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/4/16.
 */
public interface Route extends Model {

    String getPath();

    Route setPath(String path);

    UiContainerExpr getResponse();

    Route setResponse(UiContainerExpr response);

    default boolean matches(String url) {
        final String path = getPath();
        if (url.equals(path)) {
            return true;
        }
        // Do some regex matching here...
        return false;
    }

}
