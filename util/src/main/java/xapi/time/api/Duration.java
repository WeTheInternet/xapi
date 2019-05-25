package xapi.time.api;

import java.io.Serializable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
public interface Duration extends Serializable {

    Moment millis();

    default Duration addMillisImmediate(double millis) {
        final Moment now = millis();
        final Moment later = now.plus(millis);
        return ()->later;
    }

    /**
     * @param millis a decimal number of milliseconds to add to this duration.
     *               Does NOT immediately call {@link #millis()};
     *               if this duration is immutable, we wait to add two numbers,
     *               if this duration tracks elapsed time
     * @return closure over whatever milli provider we wrap.
     */
    default Duration addMillisDeferred(double millis) {
        return ()->millis().plus(millis);
    }

}
