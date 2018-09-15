package xapi.server.errors;

import xapi.util.X_String;

/**
 * Used to redirect user to another resource.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/11/18 @ 12:03 AM.
 */
public class RedirectionException extends RuntimeException {

    private final String location;
    private final String body;
    private boolean updateUrl;

    public RedirectionException (String location) {
        this(location, "");
    }
    public RedirectionException (String location, boolean updateUrl) {
        this(location, "");
        this.updateUrl = updateUrl;
    }
    public RedirectionException (String location, String tempBody) {
        this.location = location;
        this.body = tempBody == null ? X_String.EMPTY_STRING : tempBody;
    }

    public String getLocation() {
        return location;
    }

    public String getBody() {
        return body;
    }

    public boolean isUpdateUrl() {
        return updateUrl;
    }

    public RedirectionException setUpdateUrl(boolean updateUrl) {
        this.updateUrl = updateUrl;
        return this;
    }
}
