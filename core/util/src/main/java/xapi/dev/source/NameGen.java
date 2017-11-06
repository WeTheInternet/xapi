package xapi.dev.source;

import xapi.fu.Lazy;
import xapi.time.X_Time;
import xapi.time.api.Moment;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Need a unique name?
 *
 * Don't want to be bothered to manage state across multiple compiles?
 *
 * If so, then NameGen is for you!
 *
 * If you create an instance with a given seed,
 * you will get deterministic names when called in a deterministic manner.
 *
 * If you are lazy and just want globally unique identifiers across a given
 * run of a jvm, then use the static factory method to get a shared NamePool.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 8/20/16.
 */
public class NameGen {

    private static final Lazy<NameGen> GLOBAL = Lazy.deferSupplier(NameGen::new, 0xcafebabe);
    private String idPrefix = "id";

    public static NameGen getGlobal() {
        return GLOBAL.out1();
    }

    private final AtomicInteger cnt;

    public NameGen() {
        this((int)(Math.random() * Integer.MAX_VALUE / 2));
    }

    public NameGen(int seed) {
        cnt = new AtomicInteger(Math.abs(seed));
    }

    public String newName(NameType type) {
        switch (type) {
            case CSS_CLASS:
                return newClass();
            case ID:
                return newId();
            case ENCODED_TIMESTAMP:
                return newTimestamp();
            default:
                throw new UnsupportedOperationException("Type " + type + " not supported.");
        }
    }

    protected int nextInt() {
        return cnt.getAndUpdate(i->i==Integer.MAX_VALUE ? 1 : i+1);
    }

    protected String nextString() {
        return Integer.toString(nextInt(), 36);
    }

    protected String classPrefix() {
        return "cls-";
    }

    protected String idPrefix() {
        return idPrefix;
    }

    public NameGen setIdPrefix(String prefix) {
        this.idPrefix = prefix;
        return this;
    }

    public String newClass() {
        return classPrefix() + nextString();
    }

    public String newId() {
        return idPrefix() + nextString();
    }

    public String newName(String prefix) {
        return prefix + nextString();
    }

    public String newTimestamp() {
        // nowPlusOne uses a double that is a fractional millisecond count;
        // when called in rapid succession, it will create the "minimally largest" double
        // that can fit within the precision used by the current timestamp.
        // If the timestamp should become larger than the current time in millis,
        // this will actually park
        final Moment now = X_Time.nowPlusOne();
        long time = Double.doubleToLongBits(now.millis());
        return Long.toString(time, 36);
    }

    public enum NameType {
        CSS_CLASS,
        ID,
        ENCODED_TIMESTAMP
    }

    public static NameGen notNull(NameGen id) {
        return id == null ? GLOBAL.out1() : id;
    }
}
