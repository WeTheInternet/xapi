package xapi.except;

/**
 * The user requested something stupid.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/10/18 @ 10:53 PM.
 */
public class InvalidRequest extends RuntimeException {

    public InvalidRequest() {
    }

    public InvalidRequest(String message) {
        super(message);
    }

    public InvalidRequest(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidRequest(Throwable cause) {
        super(cause);
    }

    public InvalidRequest(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
