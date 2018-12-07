package xapi.dev.source;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/15/18.
 */
public interface HasIndent {

    default String getIndent() {
        return "  ";
    }
}
