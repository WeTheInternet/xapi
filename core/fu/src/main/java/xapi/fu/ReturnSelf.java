package xapi.fu;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/9/16.
 */
public interface ReturnSelf <Self extends ReturnSelf<Self>> {

    default Self self() {
        return (Self) this;
    }
}
