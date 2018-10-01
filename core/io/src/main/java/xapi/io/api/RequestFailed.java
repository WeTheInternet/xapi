package xapi.io.api;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 9/28/18 @ 3:54 AM.
 */
public class RequestFailed extends Throwable {
    private final IOMessage<String> msg;

    public RequestFailed(IOMessage<String> msg) {
        super("Request " + msg.url() + " failed:\n" + msg.body());
        this.msg = msg;
    }
}
